#!/bin/bash
## 作者: LeapYa
## 修改时间: 2026-01-06
## 描述: POETIZE 博客系统自动迁移脚本
## 版本: 1.5.0

# 定义颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 函数
info() { echo -e "${BLUE}[信息]${NC} $1"; }
success() { echo -e "${GREEN}[成功]${NC} $1"; }
error() { echo -e "${RED}[失败]${NC} $1"; }
warning() { echo -e "${YELLOW}[警告]${NC} $1"; }

# 全局变量
NGINX_DOMAINS=""
TARGET_IP=""
TARGET_USER=""
TARGET_PASSWORD=""
TARGET_SSH_KEY=""  # SSH私钥文件路径，如果设置则优先使用密钥认证
TARGET_PORT="22"
DB_ROOT_PASSWORD=""
DB_USER_PASSWORD=""
BACKUP_DIR=""
IS_CHINA_ENV=false
CURRENT_DIR=$(dirname "$(pwd)" | sed 's:/*$::')  # 当前目录，去除末尾的一个或多个斜杠
MIGRATE_UPLOADS="yes"    # 是否迁移用户上传文件，默认为yes
extract_dir="Awesome-poetize-open"  # 项目提取目录
USE_EXTERNAL_DB=false  # 是否使用外部数据库
USE_EXTERNAL_REDIS=false  # 是否使用外部Redis
UPDATE_VERSION=""  # 迁移到指定版本（空则使用最新稳定版本）
USE_LATEST_TAG=true  # 是否使用最新稳定标签（false则使用main分支）

# 动态生成volume名称的函数
get_volume_name() {
    # 从extract_dir生成对应的volume名称
    # 例如: Awesome-poetize-open -> awesome-poetize-open_poetize_uploads
    # 例如: Awesome-poetize-open-blog2 -> awesome-poetize-open-blog2_poetize_uploads
    local dir_name=$(echo "$extract_dir" | tr '[:upper:]' '[:lower:]')
    echo "${dir_name}_poetize_uploads"
}

# 断点续传和重试配置
STATE_FILE=".migrate_state"
DATA_FILE=".migrate_data"
MAX_RETRIES=3
RETRY_DELAY=10
SSH_TIMEOUT=30
CONNECT_TIMEOUT=10

# 迁移步骤状态
STEP_PREREQUISITES="prerequisites"
STEP_READ_CREDENTIALS="read_credentials"
STEP_USER_INPUT="user_input"
STEP_EXTRACT_DOMAINS="extract_domains"
STEP_BACKUP_DB="backup_db"
STEP_TEST_SSH="test_ssh"
STEP_DETECT_ENV="detect_env"
STEP_PULL_CODE="pull_code"
STEP_TRANSFER_FILES="transfer_files"
STEP_DEPLOY="deploy"
STEP_EXECUTE_SQL="execute_sql"
STEP_CLEANUP="cleanup"

# 状态管理函数
save_state() {
    local step="$1"
    local status="$2"  # completed, failed, in_progress
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "$step:$status:$timestamp" >> "$STATE_FILE"
    info "状态已保存: $step -> $status"
}

get_step_status() {
    local step="$1"
    if [ ! -f "$STATE_FILE" ]; then
        echo "not_started"
        return
    fi
    
    local status=$(grep "^$step:" "$STATE_FILE" | tail -1 | cut -d':' -f2)
    if [ -z "$status" ]; then
        echo "not_started"
    else
        echo "$status"
    fi
}

is_step_completed() {
    local step="$1"
    local status=$(get_step_status "$step")
    [ "$status" = "completed" ]
}

show_migration_progress() {
    info "迁移进度状态:"
    local steps=("$STEP_PREREQUISITES" "$STEP_READ_CREDENTIALS" "$STEP_USER_INPUT" "$STEP_EXTRACT_DOMAINS" "$STEP_BACKUP_DB" "$STEP_TEST_SSH" "$STEP_DETECT_ENV" "$STEP_PULL_CODE" "$STEP_TRANSFER_FILES" "$STEP_DEPLOY" "$STEP_EXECUTE_SQL" "$STEP_CLEANUP")
    local step_names=("前置条件检查" "读取数据库凭据" "用户输入收集" "域名提取" "数据库备份" "SSH连接测试" "环境检测" "代码拉取" "文件传输" "部署执行" "执行SQL脚本" "清理工作")
    
    for i in "${!steps[@]}"; do
        local step="${steps[$i]}"
        local name="${step_names[$i]}"
        local status=$(get_step_status "$step")
        
        case "$status" in
            "completed")
                printf "  ${GREEN}✓${NC} %s\n" "$name"
                ;;
            "failed")
                printf "  ${RED}✗${NC} %s\n" "$name"
                ;;
            "in_progress")
                printf "  ${YELLOW}⚠${NC} %s (进行中)\n" "$name"
                ;;
            *)
                printf "  ${GRAY}○${NC} %s (未开始)\n" "$name"
                ;;
        esac
    done
}

clean_state() {
    if [ -f "$STATE_FILE" ]; then
        rm -f "$STATE_FILE"
        info "状态文件已清理"
    fi
    if [ -f "$DATA_FILE" ]; then
        rm -f "$DATA_FILE"
        info "数据文件已清理"
    fi
}

# 增量保存单个变量到数据文件
save_variable() {
    local var_name="$1"
    local var_value="$2"
    
    # 如果数据文件不存在，创建它
    if [ ! -f "$DATA_FILE" ]; then
        touch "$DATA_FILE"
    fi
    
    # 删除已存在的同名变量行
    if [ -f "$DATA_FILE" ]; then
        grep -v "^$var_name=" "$DATA_FILE" > "$DATA_FILE.tmp" 2>/dev/null || true
        mv "$DATA_FILE.tmp" "$DATA_FILE"
    fi
    
    # 添加新的变量值
    echo "$var_name=\"$var_value\"" >> "$DATA_FILE"
}

# 保存用户输入的目标服务器信息
save_target_server_data() {
    save_variable "TARGET_IP" "$TARGET_IP"
    save_variable "TARGET_USER" "$TARGET_USER"
    save_variable "TARGET_PASSWORD" "$TARGET_PASSWORD"
    save_variable "TARGET_SSH_KEY" "$TARGET_SSH_KEY"
    save_variable "TARGET_PORT" "$TARGET_PORT"
    info "目标服务器信息已保存"
}

# 保存数据库凭据
save_db_credentials() {
    save_variable "DB_ROOT_PASSWORD" "$DB_ROOT_PASSWORD"
    save_variable "DB_USER_PASSWORD" "$DB_USER_PASSWORD"
    info "数据库凭据已保存"
}

# 保存提取的域名
save_nginx_domains() {
    save_variable "NGINX_DOMAINS" "$NGINX_DOMAINS"
    info "提取的域名已保存"
}

# 保存环境检测结果
save_environment_info() {
    save_variable "IS_CHINA_ENV" "$IS_CHINA_ENV"
    save_variable "USE_EXTERNAL_DB" "$USE_EXTERNAL_DB"
    save_variable "USE_EXTERNAL_REDIS" "$USE_EXTERNAL_REDIS"
    info "环境信息已保存"
}

# 保存备份目录信息
save_backup_info() {
    save_variable "BACKUP_DIR" "$BACKUP_DIR"
    info "备份信息已保存"
}

# 加载用户输入数据
load_user_data() {
    if [ -f "$DATA_FILE" ]; then
        source "$DATA_FILE"
        info "用户数据已从 $DATA_FILE 加载"
        return 0
    else
        warning "数据文件 $DATA_FILE 不存在"
        return 1
    fi
}

# 重试机制函数
retry_command() {
    local max_attempts="$1"
    local delay="$2"
    local description="$3"
    shift 3
    local cmd="$@"
    
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        info "$description (尝试 $attempt/$max_attempts)"
        
        if eval "$cmd"; then
            success "$description 成功"
            return 0
        fi
        
        if [ $attempt -lt $max_attempts ]; then
            warning "$description 失败，等待 ${delay} 秒后重试..."
            sleep "$delay"
        else
            error "$description 在 $max_attempts 次尝试后仍然失败"
        fi
        
        ((attempt++))
    done
    
    return 1
}

# SSH重试执行函数
ssh_retry() {
    local description="$1"
    local ssh_cmd="$2"
    local use_sudo="${3:-false}"
    
    local full_cmd
    local ssh_auth_options=""
    
    # 检查是否使用SSH密钥认证
    if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
        ssh_auth_options="-i '$TARGET_SSH_KEY'"
        info "使用SSH密钥认证: $TARGET_SSH_KEY"
    elif [ -n "$TARGET_PASSWORD" ]; then
        ssh_auth_options="sshpass -p '$TARGET_PASSWORD'"
        info "使用SSH密码认证"
    else
        error "未设置SSH认证方式，请提供密钥文件路径或密码"
        return 1
    fi
    
    if [ "$use_sudo" = "true" ] && [ "$TARGET_USER" != "root" ]; then
        if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
            # 使用密钥认证时，sudo需要密码
            if [ -n "$TARGET_PASSWORD" ]; then
                full_cmd="ssh $ssh_auth_options -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT -o ServerAliveInterval=60 '$TARGET_USER@$TARGET_IP' \"echo '$TARGET_PASSWORD' | sudo -S bash -c '$ssh_cmd'\""
            else
                warning "使用密钥认证但未提供sudo密码，尝试无密码sudo"
                full_cmd="ssh $ssh_auth_options -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT -o ServerAliveInterval=60 '$TARGET_USER@$TARGET_IP' \"sudo bash -c '$ssh_cmd'\""
            fi
        else
            # 使用密码认证
            full_cmd="$ssh_auth_options ssh -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT -o ServerAliveInterval=60 '$TARGET_USER@$TARGET_IP' \"echo '$TARGET_PASSWORD' | sudo -S bash -c '$ssh_cmd'\""
        fi
    else
        if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
            full_cmd="ssh $ssh_auth_options -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT -o ServerAliveInterval=60 '$TARGET_USER@$TARGET_IP' '$ssh_cmd'"
        else
            full_cmd="$ssh_auth_options ssh -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT -o ServerAliveInterval=60 '$TARGET_USER@$TARGET_IP' '$ssh_cmd'"
        fi
    fi
    
    retry_command "$MAX_RETRIES" "$RETRY_DELAY" "$description" "$full_cmd"

    local rc=$?
    return $rc
}

# SCP重试传输函数
scp_retry() {
    local description="$1"
    local source="$2"
    local destination="$3"
    local options="${4:-}"
    
    local scp_cmd
    
    # 检查是否使用SSH密钥认证
    if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
        scp_cmd="scp -i '$TARGET_SSH_KEY' -P $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT $options '$source' '$TARGET_USER@$TARGET_IP:$destination'"
    elif [ -n "$TARGET_PASSWORD" ]; then
        scp_cmd="sshpass -p '$TARGET_PASSWORD' scp -P $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT $options '$source' '$TARGET_USER@$TARGET_IP:$destination'"
    else
        error "未设置SSH认证方式，请提供密钥文件路径或密码"
        return 1
    fi
    
    retry_command "$MAX_RETRIES" "$RETRY_DELAY" "$description" "$scp_cmd"
}
# 检查必要工具
check_prerequisites() {
    # 检查是否已完成
    if is_step_completed "$STEP_PREREQUISITES"; then
        success "前置条件检查已完成，跳过此步骤"
        return 0
    fi
    
    save_state "$STEP_PREREQUISITES" "in_progress"
    info "检查迁移前置条件..."
    CURRENT_DIR=$(dirname "$(pwd)")
    # 检查必要命令
    local missing_tools=()
    
    if ! command -v sshpass &> /dev/null; then
        missing_tools+=("sshpass")
    fi
    
    if ! command -v ssh &> /dev/null; then
        missing_tools+=("ssh")
    fi
    
    if ! command -v scp &> /dev/null; then
        missing_tools+=("scp")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        warning "缺少必要工具: ${missing_tools[*]}"
        info "正在尝试自动安装缺少的工具..."
        
        # 自动安装缺少的工具
        if command -v apt-get &> /dev/null; then
            info "检测到Ubuntu/Debian系统，使用apt-get安装..."
            sudo apt-get update -qq
            sudo apt-get install -y sshpass openssh-client
        elif command -v yum &> /dev/null; then
            info "检测到CentOS/RHEL系统，使用yum安装..."
            sudo yum install -y sshpass openssh-clients
        elif command -v dnf &> /dev/null; then
            info "检测到Fedora系统，使用dnf安装..."
            sudo dnf install -y sshpass openssh-clients
        elif command -v pacman &> /dev/null; then
            info "检测到Arch Linux系统，使用pacman安装..."
            sudo pacman -S --noconfirm openssh sshpass
        elif command -v zypper &> /dev/null; then
            info "检测到openSUSE系统，使用zypper安装..."
            sudo zypper install -y openssh sshpass
        elif command -v apk &> /dev/null; then
            info "检测到Alpine Linux系统，使用apk安装..."
            sudo apk add --update-cache openssh sshpass
        else
            error "无法识别的包管理器，请手动安装以下工具: ${missing_tools[*]}"
            info "安装命令参考:"
            info "Ubuntu/Debian: sudo apt-get install sshpass openssh-client"
            info "CentOS/RHEL: sudo yum install sshpass openssh-clients"
            info "Fedora: sudo dnf install sshpass openssh-clients"
            info "Arch Linux: sudo pacman -S openssh sshpass"
            info "openSUSE: sudo zypper install openssh sshpass"
            exit 1
        fi
        
        # 重新检查工具是否安装成功
        local still_missing=()
        for tool in "${missing_tools[@]}"; do
            if ! command -v "$tool" &> /dev/null; then
                still_missing+=("$tool")
            fi
        done
        
        if [ ${#still_missing[@]} -gt 0 ]; then
            error "以下工具安装失败: ${still_missing[*]}"
            error "请手动安装这些工具后重新运行脚本"
            exit 1
        else
            success "所有必要工具安装成功"
        fi
    fi
    
    # 检查数据库凭据文件
    if [ ! -f ".config/db_credentials.txt" ]; then
        error "数据库凭据文件 .config/db_credentials.txt 不存在"
        error "请确保在项目根目录运行此脚本，并且数据库已正确配置"
        exit 1
    fi
    
    # 检查是否使用外部数据库模式
    if grep -q "数据库外部模式: true" .config/db_credentials.txt 2>/dev/null; then
        USE_EXTERNAL_DB=true
        info "检测到外部数据库模式"
    fi
    
    # 检查是否使用外部Redis模式
    if grep -q "Redis外部模式: true" .config/redis_config.txt 2>/dev/null; then
        USE_EXTERNAL_REDIS=true
        info "检测到外部Redis模式"
    fi
    
    # 检查py/data目录
    if [ ! -d "py/data" ]; then
        error "配置目录 py/data 不存在"
        exit 1
    fi
    
    # 如果不是外部数据库模式，检查docker-compose是否运行
    if [ "$USE_EXTERNAL_DB" = false ]; then
        # 检查docker-compose是否运行（假设源服务器上只有一个要迁移的MariaDB容器）
        local running_container=$(sudo docker ps --format "{{.Names}}" | grep "mariadb" | head -1)
        if [ -z "$running_container" ]; then
            error "数据库容器未运行，请先启动服务: docker-compose up -d"
            exit 1
        else
            info "检测到运行中的MariaDB容器: $running_container"
        fi
    else
        info "外部数据库模式，跳过容器检查"
    fi
    # 保存环境检测结果
    save_environment_info
    
    save_state "$STEP_PREREQUISITES" "completed"
    success "前置条件检查通过"
}

# 获取用户输入
get_user_input() {
    # 检查是否已完成
    if is_step_completed "$STEP_USER_INPUT"; then
        success "用户输入收集已完成，跳过此步骤"
        # 从数据文件中恢复用户输入
        if ! load_user_data; then
            error "无法加载用户输入数据，请重新运行迁移"
            clean_state
            exit 1
        fi
        return 0
    fi
    
    save_state "$STEP_USER_INPUT" "in_progress"
    info "请输入目标服务器信息:"
    
    # 获取目标服务器IP
    while [ -z "$TARGET_IP" ]; do
        read -p "目标服务器IP地址: " TARGET_IP
        if [ -z "$TARGET_IP" ]; then
            warning "IP地址不能为空，请重新输入"
        fi
    done
    
    # 获取目标服务器用户名
    read -p "目标服务器用户名 (默认: root): " TARGET_USER
    if [ -z "$TARGET_USER" ]; then
        TARGET_USER="root"
        info "使用默认用户名: $TARGET_USER"
    fi
    
    # 选择认证方式
    echo
    info "请选择SSH认证方式:"
    echo "1) SSH密钥认证（更安全）"
    echo "2) 密码认证"
    read -p "请选择 (1-2, 默认: 1): " auth_choice
    
    if [ -z "$auth_choice" ] || [ "$auth_choice" = "1" ]; then
        # SSH密钥认证
        while [ -z "$TARGET_SSH_KEY" ]; do
            read -p "SSH私钥文件路径 (默认: ~/.ssh/id_rsa): " TARGET_SSH_KEY
            if [ -z "$TARGET_SSH_KEY" ]; then
                TARGET_SSH_KEY="~/.ssh/id_rsa"
            fi
            
            # 展开波浪号
            TARGET_SSH_KEY=$(eval echo "$TARGET_SSH_KEY")
            
            # 检查密钥文件是否存在
            if [ ! -f "$TARGET_SSH_KEY" ]; then
                warning "SSH密钥文件不存在: $TARGET_SSH_KEY"
                TARGET_SSH_KEY=""
            else
                # 检查密钥文件权限
                if [ "$(stat -c %a "$TARGET_SSH_KEY" 2>/dev/null || stat -f %A "$TARGET_SSH_KEY" 2>/dev/null)" != "600" ]; then
                    warning "SSH密钥文件权限不安全，建议设置为600"
                    read -p "是否继续使用此密钥? (y/N): " continue_choice
                    if [ "$continue_choice" != "y" ] && [ "$continue_choice" != "Y" ]; then
                        TARGET_SSH_KEY=""
                    fi
                fi
            fi
        done
        
        # 如果用户不是root，可能需要密码用于sudo
        if [ "$TARGET_USER" != "root" ]; then
            read -p "sudo密码 (如果需要): " TARGET_PASSWORD
            echo
        fi
        
        info "将使用SSH密钥认证: $TARGET_SSH_KEY"
    else
        # 密码认证
        while [ -z "$TARGET_PASSWORD" ]; do
            read -p "目标服务器密码: " TARGET_PASSWORD
            echo
            if [ -z "$TARGET_PASSWORD" ]; then
                warning "密码不能为空，请重新输入"
            fi
        done
        info "将使用密码认证"
    fi
    
    # 获取SSH端口
    read -p "SSH端口 (默认: 22): " TARGET_PORT
    if [ -z "$TARGET_PORT" ]; then
        TARGET_PORT="22"
    fi
    
    # 验证端口号
    if ! [[ "$TARGET_PORT" =~ ^[0-9]+$ ]] || [ "$TARGET_PORT" -lt 1 ] || [ "$TARGET_PORT" -gt 65535 ]; then
        warning "端口号无效，使用默认端口22"
        TARGET_PORT="22"
    fi
    
    save_state "$STEP_USER_INPUT" "completed"
    save_target_server_data
    success "目标服务器信息获取完成"
    info "目标服务器: $TARGET_USER@$TARGET_IP:$TARGET_PORT"
}

# 读取数据库凭据
read_db_credentials() {
    # 检查是否已完成
    if is_step_completed "$STEP_READ_CREDENTIALS"; then
        success "数据库凭据读取已完成，跳过此步骤"
        # 从数据文件中恢复数据库凭据
        if ! load_user_data; then
            # 如果数据文件不存在，重新读取凭据
            DB_ROOT_PASSWORD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
            DB_USER_PASSWORD=$(grep "数据库poetize用户密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
            save_db_credentials
        fi
        return 0
    fi
    
    save_state "$STEP_READ_CREDENTIALS" "in_progress"
    info "读取数据库凭据..."
    
    DB_ROOT_PASSWORD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
    DB_USER_PASSWORD=$(grep "数据库poetize用户密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
    
    if [ -z "$DB_ROOT_PASSWORD" ] || [ -z "$DB_USER_PASSWORD" ]; then
        error "无法读取数据库密码"
        exit 1
    fi
    
    save_state "$STEP_READ_CREDENTIALS" "completed"
    save_db_credentials
    success "数据库凭据读取成功"
}

# 从nginx配置文件中提取域名
extract_domains_from_nginx() {
    # 检查是否已完成
    if is_step_completed "$STEP_EXTRACT_DOMAINS"; then
        success "域名提取已完成，跳过此步骤"
        # 从数据文件中恢复提取的域名
        if ! load_user_data; then
            # 如果数据文件不存在，重新提取域名
            local nginx_config_file="docker/nginx/default.https.conf"
            if [ -f "$nginx_config_file" ]; then
                local domains=$(grep "server_name" "$nginx_config_file" | sed 's/server_name \(.*\);/\1/' | tr -d ' ' | tr ';' '\n' | grep -v "example.com" | grep -v "^$" | sort -u)
                if [ -n "$domains" ]; then
                    NGINX_DOMAINS="$domains"
                    save_nginx_domains
                fi
            fi
        fi
        return 0
    fi
    
    save_state "$STEP_EXTRACT_DOMAINS" "in_progress"
    info "从nginx配置文件中提取域名..."
    
    local nginx_config_file="docker/nginx/default.https.conf"
    
    if [ ! -f "$nginx_config_file" ]; then
        warning "nginx配置文件不存在: $nginx_config_file"
        return 1
    fi
    
    # 提取server_name行中的域名，排除example.com
    local domains=$(grep "server_name" "$nginx_config_file" | sed 's/server_name \(.*\);/\1/' | tr ';' '\n' | grep -v "example.com" | grep -v "^$" | sort -u)
    
    if [ -n "$domains" ]; then
        NGINX_DOMAINS="$domains"
        save_state "$STEP_EXTRACT_DOMAINS" "completed"
        save_nginx_domains
        success "成功提取到域名: $(echo "$domains" | tr '\n' ' ')"
        return 0
    else
        save_state "$STEP_EXTRACT_DOMAINS" "completed"
        warning "未找到有效域名或只有example.com默认域名"
        return 1
    fi
}

# 备份数据库
backup_database() {
    # 检查是否已完成
    if is_step_completed "$STEP_BACKUP_DB"; then
        success "数据库备份已完成，跳过此步骤"
        # 从数据文件中恢复备份目录信息
        load_user_data || true
        return 0
    fi
    
    save_state "$STEP_BACKUP_DB" "in_progress"
    
    # 如果使用外部数据库模式，跳过备份
    if [ "$USE_EXTERNAL_DB" = true ]; then
        info "检测到外部数据库模式，跳过数据库备份"
        save_state "$STEP_BACKUP_DB" "completed"
        success "数据库备份步骤已跳过（外部数据库模式）"
        return 0
    fi
    
    info "开始备份数据库..."
    
    # 创建临时备份目录
    BACKUP_DIR="migration_temp_$(date +%Y%m%d_%H%M%S)"
    sudo mkdir -p "$BACKUP_DIR"
    
    # 备份数据库
    info "正在导出数据库到 $BACKUP_DIR/poetry.sql..."
    
    # 动态获取实际的MariaDB容器名称（假设源服务器上只有一个要备份的MariaDB容器）
    local actual_container=$(sudo docker ps --format "{{.Names}}" | grep "mariadb" | head -1)
    if [ -z "$actual_container" ]; then
        # 如果没有找到运行中的容器，尝试查找所有容器（包括停止的）
        actual_container=$(sudo docker ps -a --format "{{.Names}}" | grep "mariadb" | head -1)
        if [ -z "$actual_container" ]; then
            error "未找到MariaDB容器，请确保数据库服务正在运行"
            exit 1
        else
            error "MariaDB容器 $actual_container 未运行，请先启动数据库服务"
            exit 1
        fi
    fi
    
    info "使用MariaDB容器: $actual_container"
    local backup_cmd="sudo docker exec $actual_container mariadb-dump -u root -p'$DB_ROOT_PASSWORD' --single-transaction --routines --triggers --databases poetize > '$BACKUP_DIR/poetry.sql'"
    
    if retry_command "$MAX_RETRIES" "$RETRY_DELAY" "数据库备份" "$backup_cmd"; then
        # 备份端口配置
        info "备份端口配置..."
        local http_port=""
        if [ -f "docker-compose.yml" ]; then
            # 提取HTTP端口配置（支持多种格式）
            http_port=$(grep -E '^\s*-\s*["'\'']*[0-9]+:80' docker-compose.yml 2>/dev/null | head -1 | grep -oE '[0-9]+' | head -1)
            if [ -n "$http_port" ]; then
                echo "$http_port" > "$BACKUP_DIR/http_port.txt"
                success "HTTP端口配置备份完成: $http_port"
            else
                info "使用默认80端口"
            fi
        fi
        
        save_state "$STEP_BACKUP_DB" "completed"
        save_backup_info
        success "数据库备份成功: $BACKUP_DIR/poetry.sql"
    else
        save_state "$STEP_BACKUP_DB" "failed"
        error "数据库备份失败"
        exit 1
    fi
}

# 测试SSH连接
test_ssh_connection() {
    # 检查是否已完成
    if is_step_completed "$STEP_TEST_SSH"; then
        success "SSH连接测试已完成，跳过此步骤"
        # 从数据文件中恢复extract_dir变量
        if ! load_user_data; then
            # 如果数据文件不存在，使用默认值
            extract_dir="Awesome-poetize-open"
            warning "无法加载extract_dir变量，使用默认值: $extract_dir"
        fi
        return 0
    fi
    
    save_state "$STEP_TEST_SSH" "in_progress"
    info "测试SSH连接到目标服务器..."

    # 清理旧指纹
    ssh-keygen -R "$TARGET_IP" 2>/dev/null

    # 测试基本SSH连接
    if ssh_retry "SSH连接测试" "echo 'SSH连接测试成功'" "false"; then
        success "SSH连接测试成功"
    else
        save_state "$STEP_TEST_SSH" "failed"
        error "SSH连接失败，请检查IP地址、用户名和密码"
        exit 1
    fi
    
    # 检查sudo权限（如果不是root用户）
    if [ "$TARGET_USER" != "root" ]; then
        info "检查sudo权限..."
        if ssh_retry "sudo权限检查" "echo 'sudo权限检查成功'" "true"; then
            success "sudo权限检查通过"
        else
            save_state "$STEP_TEST_SSH" "failed"
            error "用户 $TARGET_USER 没有sudo权限，请使用root用户或具有sudo权限的用户"
            exit 1
        fi
    fi
    
    # 检查目标服务器上的项目目录并设置extract_dir
    info "检查目标迁移服务器上的项目目录..."
    local base_dir="Awesome-poetize-open"
    local blog_number=1
    
    # 直接检查基础目录是否存在（不使用重试机制，因为目录不存在是正常情况）
    info "检查基础目录是否存在..."
    local check_cmd
    if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
        check_cmd="ssh -i '$TARGET_SSH_KEY' -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' '[ -d \"${CURRENT_DIR}/${base_dir}\" ]'"
    elif [ -n "$TARGET_PASSWORD" ]; then
        check_cmd="sshpass -p '$TARGET_PASSWORD' ssh -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' '[ -d \"${CURRENT_DIR}/${base_dir}\" ]'"
    else
        error "未设置SSH认证方式"
        exit 1
    fi
    
    if eval "$check_cmd" 2>/dev/null; then
        # 基础目录存在，需要找到可用的blog目录
        info "检测到目标服务器已存在 $base_dir 目录"
        
        while true; do
            local test_dir="${base_dir}-blog${blog_number}"
            local test_cmd
            if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
                test_cmd="ssh -i '$TARGET_SSH_KEY' -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' '[ -d \"${CURRENT_DIR}/${test_dir}\" ]'"
            else
                test_cmd="sshpass -p '$TARGET_PASSWORD' ssh -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' '[ -d \"${CURRENT_DIR}/${test_dir}\" ]'"
            fi
            
            if eval "$test_cmd" 2>/dev/null; then
                # 目录存在，尝试下一个编号
                blog_number=$((blog_number + 1))
            else
                # 目录不存在，使用这个名称
                extract_dir="$test_dir"
                break
            fi
        done
        
        info "将使用目录名称: $extract_dir"
        save_variable "extract_dir" "$extract_dir"
    else
        # 基础目录不存在，使用默认名称
        extract_dir="$base_dir"
        info "目标服务器无冲突目录，使用默认名称: $extract_dir"
        save_variable "extract_dir" "$extract_dir"
    fi
    
    save_state "$STEP_TEST_SSH" "completed"
}

# 检测目标服务器环境
detect_target_environment() {
    # 检查是否已完成
    if is_step_completed "$STEP_DETECT_ENV"; then
        success "环境检测已完成，跳过此步骤"
        # 从数据文件中恢复环境信息
        if ! load_user_data; then
            # 如果数据文件不存在，从状态文件读取环境信息
            local env_info=$(grep "^$STEP_DETECT_ENV:completed:" "$STATE_FILE" | tail -1 | cut -d':' -f4-)
            if [[ "$env_info" == *"china"* ]]; then
                IS_CHINA_ENV=true
                info "读取到国内网络环境配置"
            else
                IS_CHINA_ENV=false
                info "读取到国外网络环境配置"
            fi
        fi
        return 0
    fi
    
    save_state "$STEP_DETECT_ENV" "in_progress"
    info "检测目标服务器网络环境..."
    
    # 直接检测是否能访问Google（判断是否为国内环境），不使用重试机制
    info "测试Google连接性..."
    local network_test_cmd
    if [ -n "$TARGET_SSH_KEY" ] && [ -f "$TARGET_SSH_KEY" ]; then
        network_test_cmd="ssh -i '$TARGET_SSH_KEY' -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' 'curl -s --connect-timeout 5 --max-time 10 https://www.google.com >/dev/null 2>&1'"
    elif [ -n "$TARGET_PASSWORD" ]; then
        network_test_cmd="sshpass -p '$TARGET_PASSWORD' ssh -p $TARGET_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=$CONNECT_TIMEOUT '$TARGET_USER@$TARGET_IP' 'curl -s --connect-timeout 5 --max-time 10 https://www.google.com >/dev/null 2>&1'"
    else
        error "未设置SSH认证方式"
        exit 1
    fi
    
    if eval "$network_test_cmd" 2>/dev/null; then
        IS_CHINA_ENV=false
        save_state "$STEP_DETECT_ENV" "completed:foreign"
        save_environment_info
        success "检测到国外网络环境，将使用GitHub仓库"
    else
        IS_CHINA_ENV=true
        save_state "$STEP_DETECT_ENV" "completed:china"
        save_environment_info
        success "检测到国内网络环境，将使用Gitee仓库"
    fi
}

# 在目标服务器上拉取代码
pull_code_on_target() {
    # 检查是否已完成
    if is_step_completed "$STEP_PULL_CODE"; then
        success "项目代码拉取已完成，跳过此步骤"
        return 0
    fi
    
    save_state "$STEP_PULL_CODE" "in_progress"
    info "在目标服务器上拉取项目代码..."
    
    local git_url
    if [ "$IS_CHINA_ENV" = true ]; then
        git_url="https://gitee.com/leapya/poetize.git"
    else
        git_url="https://github.com/LeapYa/Awesome-poetize-open.git"
    fi
    
    info "使用仓库地址: $git_url"
    
    # 确定目标版本
    local target_version=""
    local branch_opt=""
    
    if [ -n "$UPDATE_VERSION" ]; then
        target_version="$UPDATE_VERSION"
        info "使用指定版本: $target_version"
    elif [ "$USE_LATEST_TAG" = "true" ]; then
        # 获取最新稳定版本
        info "获取最新稳定版本..."
        target_version=$(git ls-remote --tags "$git_url" 2>/dev/null | grep -o 'refs/tags/v[0-9.]*$' | sed 's|refs/tags/||' | sort -V | tail -1)
        if [ -n "$target_version" ]; then
            info "使用最新稳定版本: $target_version"
        else
            warning "无法获取最新版本标签，将使用main分支"
        fi
    else
        info "使用main分支（最新开发版本）"
    fi
    
    # 如果有目标版本，添加 --branch 参数
    if [ -n "$target_version" ]; then
        branch_opt="--branch $target_version"
    fi
    
    # 在目标服务器上执行命令
    local ssh_cmd="
        # 安装必要工具
        if command -v apt-get &>/dev/null; then
            sudo apt-get update && sudo apt-get install -y git curl
        elif command -v yum &>/dev/null; then
            sudo yum install -y git curl
        elif command -v dnf &>/dev/null; then
            sudo dnf install -y git curl
        elif command -v zypper &>/dev/null; then
            sudo zypper install -y git curl
        elif command -v pacman &>/dev/null; then
            sudo pacman -S --needed git curl
        elif command -v apk &>/dev/null; then
            sudo apk add --update-cache git curl
        else
            echo 'ERROR: 不支持的包管理器，请手动安装git和curl'
            exit 1
        fi

        # 进入项目目录
        if [ -d $CURRENT_DIR ]; then
            cd $CURRENT_DIR
        else
            mkdir -p $CURRENT_DIR
            cd $CURRENT_DIR
        fi
        
        # 克隆项目
        git clone --depth 1 $branch_opt $git_url $extract_dir
        
        # 检查是否成功
        if [ -d '$extract_dir' ] && [ -f '$extract_dir/deploy.sh' ]; then
            echo 'SUCCESS: 项目代码拉取成功${target_version:+: $target_version}'
        else
            echo 'ERROR: 项目代码拉取失败'
            exit 1
        fi
    "
    
    # 使用重试机制执行代码拉取
    if ssh_retry "项目代码拉取" "$ssh_cmd" "true"; then
        save_state "$STEP_PULL_CODE" "completed"
        success "项目代码拉取成功${target_version:+: $target_version}"
    else
        save_state "$STEP_PULL_CODE" "failed"
        error "项目代码拉取失败"
        exit 1
    fi
}

# 传输文件到目标服务器
transfer_files() {
    # 检查是否已完成
    if is_step_completed "$STEP_TRANSFER_FILES"; then
        success "文件传输已完成，跳过此步骤"
        return 0
    fi
    
    save_state "$STEP_TRANSFER_FILES" "in_progress"
    info "传输备份文件到目标服务器..."
    
    local target_path
    target_path="$CURRENT_DIR/$extract_dir"
    
    # 如果不是外部数据库模式，传输数据库备份文件
    if [ "$USE_EXTERNAL_DB" = false ]; then
        info "传输数据库备份文件..."
        if ! scp_retry "数据库备份文件" "$BACKUP_DIR/poetry.sql" "$target_path/poetize-server/sql/poetry.sql"; then
            save_state "$STEP_TRANSFER_FILES" "failed"
            error "数据库备份文件传输失败"
            exit 1
        fi
    else
        info "外部数据库模式，跳过数据库备份文件传输"
    fi
    
    # 传输整个.config目录（包含db_credentials.txt和redis_config.txt等所有配置）
    info "传输配置目录..."
    if ! ssh_retry "创建配置目录" "mkdir -p $target_path/.config" "true"; then
        save_state "$STEP_TRANSFER_FILES" "failed"
        error "创建配置目录失败"
        exit 1
    fi
    
    # 传输整个.config目录中的所有文件
    if ! scp_retry "配置文件" ".config/" "$target_path/" "-r"; then
        save_state "$STEP_TRANSFER_FILES" "failed"
        error "配置文件传输失败"
        exit 1
    fi
    
    # 传输py/data配置目录
    info "传输Python配置文件..."
    if ! scp_retry "Python配置文件" "py/data" "$target_path/py/" "-r"; then
        save_state "$STEP_TRANSFER_FILES" "failed"
        error "Python配置文件传输失败"
        exit 1
    fi
    
    # 如果不是root用户，需要确保文件权限正确
    if [ "$TARGET_USER" != "root" ]; then
        info "设置文件权限..."
        if ! ssh_retry "设置文件权限" "chown -R $TARGET_USER:$TARGET_USER $target_path" "true"; then
            warning "文件权限设置失败，可能需要手动调整"
        else
            success "文件权限设置成功"
        fi
    fi
    
    save_state "$STEP_TRANSFER_FILES" "completed"
    success "文件传输完成"
}

# 在目标服务器上执行部署
deploy_on_target() {
    # 检查是否已完成
    if is_step_completed "$STEP_DEPLOY"; then
        success "项目部署已完成，跳过此步骤"
        return 0
    fi
    
    save_state "$STEP_DEPLOY" "in_progress"
    info "在目标服务器上开始部署..."
    
    local target_path
    target_path="$CURRENT_DIR/$extract_dir"
    
    info "正在目标服务器上执行部署脚本，这可能需要一些时间..."
    info "部署过程中可能需要您的交互输入（如域名配置、HTTPS设置等）"
    
    # 设置部署脚本执行权限
    if ! ssh_retry "设置部署脚本权限" "cd $target_path && chmod +x deploy.sh" "true"; then
        save_state "$STEP_DEPLOY" "failed"
        error "设置部署脚本权限失败"
        exit 1
    fi
    
    # 执行部署脚本（支持交互式操作和实时输出）
    echo -e "${YELLOW}[提示]${NC} 即将连接到目标服务器执行部署脚本，请根据提示进行交互操作"
    echo -e "${YELLOW}[提示]${NC} 部署过程可能需要较长时间（国内服务器通常需要30-60分钟）"
    echo -e "${YELLOW}[提示]${NC} 您将看到实时的部署进度输出，如果需要退出，请按 Ctrl+C"
    echo -e "${BLUE}[信息]${NC} 正在连接目标服务器并开始部署..."
    echo ""
    
    # 显示部署开始时间
    local start_time=$(date '+%Y-%m-%d %H:%M:%S')
    info "部署开始时间: $start_time"
    echo ""
    
    # 构建deploy.sh命令参数
    local deploy_cmd="./deploy.sh"
    
    # 如果提取到了域名，自动添加域名参数
    if [ -n "$NGINX_DOMAINS" ]; then
        info "使用提取到的域名参数: $(echo "$NGINX_DOMAINS" | tr '\n' ' ')"
        
        # 从备份中读取端口配置
        local http_port=""
        local backup_port_file="$BACKUP_DIR/http_port.txt"
        if [ -f "$backup_port_file" ]; then
            http_port=$(cat "$backup_port_file" 2>/dev/null | tr -d '[:space:]')
            if [ -n "$http_port" ] && [ "$http_port" != "80" ]; then
                info "从备份中读取到HTTP端口: $http_port"
            else
                http_port=""
            fi
        else
            info "未找到端口配置备份，将使用默认80端口"
        fi
        
        # 添加域名参数（支持 域名:端口 格式）
        for domain in $NGINX_DOMAINS; do
            if [ -n "$http_port" ] && [ "$http_port" != "80" ]; then
                # 为域名添加自定义端口
                deploy_cmd="$deploy_cmd -d ${domain}:${http_port}"
                info "使用域名和端口: ${domain}:${http_port}"
            else
                deploy_cmd="$deploy_cmd -d $domain"
                info "使用域名: $domain"
            fi
        done
        info "完整的部署命令: $deploy_cmd"
    else
        info "未提取到域名，将使用交互式域名输入"
    fi
    
    # 执行部署脚本，保持实时输出和交互性
    if [ "$TARGET_USER" = "root" ]; then
        sshpass -p "$TARGET_PASSWORD" ssh -t -p $TARGET_PORT -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 "$TARGET_USER@$TARGET_IP" "
            cd $target_path && \
            echo '========================================' && \
            echo '开始执行部署脚本...' && \
            echo '部署过程中请耐心等待，不要中断连接' && \
            echo '========================================' && \
            echo '' && \
            $deploy_cmd
        "
    else
        sshpass -p "$TARGET_PASSWORD" ssh -t -p $TARGET_PORT -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 "$TARGET_USER@$TARGET_IP" "
            sudo cd $target_path && \
            echo '========================================' && \
            echo '开始执行部署脚本...' && \
            echo '部署过程中请耐心等待，不要中断连接' && \
            echo '========================================' && \
            echo '' && \
            sudo $deploy_cmd
        "
    fi
    
    local exit_code=$?
    local end_time=$(date '+%Y-%m-%d %H:%M:%S')
    echo ""
    info "部署结束时间: $end_time"
    
    if [ $exit_code -eq 0 ]; then
        save_state "$STEP_DEPLOY" "completed"
        success "目标服务器部署完成"
        echo -e "${GREEN}[成功]${NC} 部署脚本执行成功，服务应该已经启动"
    elif [ $exit_code -eq 130 ]; then
        save_state "$STEP_DEPLOY" "failed"
        warning "部署被用户中断"
        echo -e "${YELLOW}[警告]${NC} 如需继续部署，请重新运行迁移脚本"
    else
        save_state "$STEP_DEPLOY" "failed"
        warning "部署过程中出现了问题（退出码: $exit_code）"
        echo -e "${YELLOW}[建议]${NC} 请检查目标服务器状态，或重新运行迁移脚本继续部署"
    fi
}

# 在目标服务器上执行新增的SQL脚本
execute_sql_scripts_on_target() {
    # 检查是否已完成
    if is_step_completed "$STEP_EXECUTE_SQL"; then
        success "SQL脚本执行已完成，跳过此步骤"
        return 0
    fi
    
    # 如果目标服务器使用外部数据库模式，跳过SQL脚本执行
    if [ "$USE_EXTERNAL_DB" = true ]; then
        save_state "$STEP_EXECUTE_SQL" "in_progress"
        info "目标服务器使用外部数据库模式，跳过SQL脚本执行"
        save_state "$STEP_EXECUTE_SQL" "completed"
        success "SQL脚本执行步骤已跳过（外部数据库模式）"
        return 0
    fi
    
    save_state "$STEP_EXECUTE_SQL" "in_progress"
    info "在目标服务器上执行新增的SQL脚本..."
    
    local target_path
    target_path="$CURRENT_DIR/$extract_dir"
    
    # 在目标服务器上执行SQL脚本（支持版本跟踪）
    local ssh_cmd="
        cd $target_path
        
        # 获取数据库连接信息
        db_root_password=\"\"
        if [ -f \".config/db_credentials.txt\" ]; then
            db_root_password=\$(grep \"数据库ROOT密码:\" .config/db_credentials.txt | cut -d':' -f2 | xargs)
        fi
        
        if [ -z \"\$db_root_password\" ]; then
            echo \"WARNING: 无法获取数据库密码，跳过额外SQL脚本执行\"
            exit 1
        fi
        
        # 等待数据库容器启动
        echo \"INFO: 等待数据库容器启动...\"
        sleep 15
        
        # 根据项目目录名确定对应的MariaDB容器名
        mariadb_container=\"\"
        
        # 根据extract_dir确定容器名
        if [[ \"$extract_dir\" == *\"-blog\"* ]]; then
            # 提取blog编号，例如: Awesome-poetize-open-blog2 -> blog2
            blog_suffix=\$(echo \"$extract_dir\" | sed 's/.*-blog/blog/')
            mariadb_container=\"poetize-mariadb-\$blog_suffix\"
        else
            # 默认实例使用poetize-mariadb
            mariadb_container=\"poetize-mariadb\"
        fi
        
        # 检查容器是否存在并运行
        if ! sudo docker ps --format \"{{.Names}}\" | grep -q \"^\$mariadb_container\$\"; then
            mariadb_container=\"\"
        fi
        
        if [ -z \"\$mariadb_container\" ]; then
            echo \"WARNING: 未找到对应的MariaDB容器，跳过额外SQL脚本执行\"
            echo \"项目目录: $extract_dir\"
            if [[ \"$extract_dir\" == *\"-blog\"* ]]; then
                expected_name=\$(echo \"$extract_dir\" | sed 's/.*-blog/blog/')
                echo \"期望的容器名: poetize-mariadb-\$expected_name\"
            else
                echo \"期望的容器名: poetize-mariadb\"
            fi
            echo \"运行中的容器:\"
            sudo docker ps --format \"table {{.Names}}\\t{{.Image}}\\t{{.Status}}\"
            exit 1
        fi
        
        echo \"INFO: 使用MariaDB容器: \$mariadb_container\"
        
        # 查找poetize-server/sql目录下除poetry.sql和poetry_old.sql外的所有.sql文件
        if [ -d \"poetize-server/sql\" ]; then
            sql_files=\$(find poetize-server/sql -name \"*.sql\" -not -name \"poetry.sql\" -not -name \"poetry_old.sql\" -type f | sort)
            
            if [ -z \"\$sql_files\" ]; then
                echo \"INFO: 未找到需要执行的额外SQL脚本\"
                exit 0
            fi
            
            # 检查 db_migrations 表是否存在
            has_migrations_table=false
            if sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize -N -e \"SELECT 1 FROM db_migrations LIMIT 1\" 2>/dev/null | grep -q \"1\"; then
                has_migrations_table=true
                echo \"INFO: 检测到版本跟踪表，将只执行未记录的脚本\"
            fi
            
            # 统计
            total_count=0
            skip_count=0
            exec_count=0
            fail_count=0
            
            # 统计总数
            while IFS= read -r sql_file; do
                [ -z \"\$sql_file\" ] && continue
                total_count=\$((total_count + 1))
            done <<< \"\$sql_files\"
            
            echo \"INFO: 发现 \$total_count 个SQL脚本\"
            
            # 执行每个SQL脚本
            while IFS= read -r sql_file; do
                [ -z \"\$sql_file\" ] && continue
                [ ! -f \"\$sql_file\" ] && continue
                
                # 提取版本号
                version=\$(basename \"\$sql_file\" .sql)
                
                # 如果有版本跟踪表，检查是否已执行
                if [ \"\$has_migrations_table\" = true ]; then
                    already_executed=\$(sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize -N -e \"SELECT COUNT(*) FROM db_migrations WHERE version='\$version' AND success=1\" 2>/dev/null | tr -d '[:space:]')
                    if [ \"\$already_executed\" = \"1\" ]; then
                        skip_count=\$((skip_count + 1))
                        continue
                    fi
                fi
                
                # 执行SQL脚本
                echo \"INFO: 执行SQL脚本: \$sql_file\"
                start_time=\$(date +%s%3N 2>/dev/null || date +%s)
                
                if sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize < \"\$sql_file\" 2>/dev/null; then
                    end_time=\$(date +%s%3N 2>/dev/null || date +%s)
                    exec_time=\$((end_time - start_time))
                    
                    # 如果刚执行的是版本表初始化脚本，重新检查表是否存在
                    if [ \"\$version\" = \"000000000000\" ] && [ \"\$has_migrations_table\" = false ]; then
                        if sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize -N -e \"SELECT 1 FROM db_migrations LIMIT 1\" 2>/dev/null | grep -q \"1\"; then
                            has_migrations_table=true
                            echo \"INFO: 版本跟踪表已创建，后续脚本执行将被记录\"
                        fi
                    fi
                    
                    # 记录到版本表
                    if [ \"\$has_migrations_table\" = true ]; then
                        sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize -e \"INSERT IGNORE INTO db_migrations (version, description, execution_time_ms, success) VALUES ('\$version', '迁移执行', \$exec_time, 1)\" 2>/dev/null
                    fi
                    
                    echo \"SUCCESS: SQL脚本执行成功: \$sql_file (\${exec_time}ms)\"
                    exec_count=\$((exec_count + 1))
                else
                    # 记录失败
                    if [ \"\$has_migrations_table\" = true ]; then
                        sudo docker exec -i \"\$mariadb_container\" mariadb -u root -p\"\$db_root_password\" poetize -e \"INSERT IGNORE INTO db_migrations (version, description, success) VALUES ('\$version', '执行失败', 0)\" 2>/dev/null
                    fi
                    
                    echo \"WARNING: SQL脚本执行失败或已存在: \$sql_file（这通常是正常的，如果表已存在）\"
                    fail_count=\$((fail_count + 1))
                fi
            done <<< \"\$sql_files\"
            
            echo \"INFO: SQL脚本执行统计: 总计=\$total_count, 执行=\$exec_count, 跳过=\$skip_count, 失败/已存在=\$fail_count\"
            echo \"SUCCESS: 额外SQL脚本执行完成\"
            
            # 重启所有服务以确保数据库结构变更被正确识别
            echo \"INFO: 重启服务以应用数据库结构变更...\"
            
            if sudo docker compose restart; then
                echo \"SUCCESS: 服务重启成功\"
                
                # 等待服务启动完成
                echo \"INFO: 等待服务启动完成...\"
                sleep 45
                
                # 检查服务状态
                echo \"INFO: 检查服务状态...\"
                sudo docker compose ps
                echo \"SUCCESS: 服务重启完成\"
            else
                echo \"WARNING: 服务重启失败，请手动重启\"
                echo \"可以使用以下命令手动重启:\"
                echo \"  cd $target_path\"
                echo \"  sudo docker compose restart\"
            fi
        else
            echo \"WARNING: poetize-server/sql目录不存在，跳过额外SQL脚本执行\"
        fi
    "
    
    # 使用重试机制执行SQL脚本
    if ssh_retry "执行新增SQL脚本" "$ssh_cmd" "true"; then
        save_state "$STEP_EXECUTE_SQL" "completed"
        success "新增SQL脚本执行成功"
    else
        save_state "$STEP_EXECUTE_SQL" "failed"
        warning "新增SQL脚本执行失败，但系统仍可正常使用"
        # 不要退出，因为这不是致命错误
    fi
}

# 用户上传文件迁移函数
migrate_uploads() {
    if [ "$MIGRATE_UPLOADS" != "yes" ]; then
        warning "跳过用户上传文件迁移"
        return 0
    fi
    
    # 动态获取volume名称
    local volume_name=$(get_volume_name)
    local volume_description="用户上传文件"
    local backup_prefix="uploads"
    local data_path="."
    
    info "开始迁移${volume_description}..."
    info "使用volume名称: $volume_name"
    
    local backup_file="${backup_prefix}_backup_$(date +%Y%m%d_%H%M%S).tar.gz"
    local has_data=false
    
    # 检查Docker volume中的数据
    local actual_volume=$(sudo docker volume ls --format "{{.Name}}" | grep "$volume_name")
    if [ -n "$actual_volume" ]; then
        info "检查Docker volume中的${volume_description}..."
        info "找到volume: $actual_volume"
        
        # 创建临时容器来访问volume并检查数据
        if sudo docker run --rm -v "$actual_volume":/data alpine sh -c "[ -d /data/$data_path ] && [ \"\$(ls -A /data/$data_path 2>/dev/null)\" ]"; then
            success "发现${volume_description}"
            has_data=true
            
            # 导出数据
            info "导出${volume_description}到 $backup_file..."
            if sudo docker run --rm -v "$actual_volume":/data -v "$(pwd):/backup" alpine tar -czf "/backup/$backup_file" -C /data $data_path; then
                success "${volume_description}导出成功"
            else
                error "${volume_description}导出失败"
                return 1
            fi
        else
            warning "未发现${volume_description}或数据为空"
        fi
    else
        warning "未发现$volume_name volume"
    fi
    
    if [ "$has_data" = true ]; then
        # 传输数据到目标服务器
        info "传输${volume_description}到目标服务器..."
        if scp_retry "${volume_description}" "$backup_file" "/tmp/"; then
            success "${volume_description}传输成功"
            
            # 在目标服务器上导入数据
            info "在目标服务器上导入${volume_description}..."
            if ssh_retry "导入${volume_description}" "
                cd /tmp && 
                # 确保Docker volume存在
                local target_volume=\$(sudo docker volume ls --format \"{{.Name}}\" | grep \"$volume_name\" | head -1)
                if [ -z \"\$target_volume\" ]; then
                    # 如果没有找到，直接创建动态生成的volume名称
                    target_volume=\"$volume_name\"
                    sudo docker volume create \$target_volume 2>/dev/null || true
                fi && 
                echo \"使用volume: \$target_volume\" && 
                # 导入数据
                sudo docker run --rm -v \"\$target_volume\":/data -v /tmp:/backup alpine sh -c '
                    mkdir -p /data/$data_path && 
                    cd /data && 
                    tar -xzf /backup/$backup_file && 
                    echo \"${volume_description}导入完成\"' && 
                # 清理临时文件
                rm -f /tmp/$backup_file
            " "true"; then
                success "${volume_description}导入成功"
            else
                error "${volume_description}导入失败"
                return 1
            fi
        else
            error "${volume_description}传输失败"
            return 1
        fi
        
        # 清理本地备份文件
        rm -f "$backup_file"
        success "已清理本地${volume_description}备份文件"
    fi
    
    success "${volume_description}迁移完成"
    return 0
}

# 清理临时文件
cleanup() {
    info "清理临时文件..."
    
    # 删除临时备份目录
    if [ -n "$BACKUP_DIR" ] && [ -d "$BACKUP_DIR" ]; then
        rm -rf "$BACKUP_DIR"
        success "临时备份目录已清理"
    fi
    
    # 清理所有volume备份文件
    for pattern in "uploads_backup_*.tar.gz" "prerender_backup_*.tar.gz"; do
        for file in $pattern; do
            if [ -f "$file" ]; then
                rm -f "$file"
                success "已清理备份文件: $file"
            fi
        done
    done
    
    success "临时文件清理完成"
}

# 显示迁移总结
show_summary() {
    printf "\n"
    printf "${GREEN}%80s${NC}\n" | tr ' ' '='
    printf "${GREEN}%s${NC}\n" "$(printf '%*s' $(((80-20)/2)) '')POETIZE 迁移完成！$(printf '%*s' $(((80-20)/2)) '')"
    printf "${GREEN}%80s${NC}\n" | tr ' ' '='
    printf "\n"
    
    printf "${BLUE}迁移信息${NC}\n"
    printf "${BLUE}%s${NC}\n" "$(printf '%*s' 8 '' | tr ' ' '-')"
    printf "  源服务器: %s\n" "$(hostname)"
    printf "  目标服务器: %s@%s:%s\n" "$TARGET_USER" "$TARGET_IP" "$TARGET_PORT"
    printf "  网络环境: %s\n" "$([ "$IS_CHINA_ENV" = true ] && echo '国内环境 (使用Gitee)' || echo '国外环境 (使用GitHub)')"
    printf "\n"
    
    printf "${BLUE}迁移内容${NC}\n"
    printf "${BLUE}%s${NC}\n" "$(printf '%*s' 8 '' | tr ' ' '-')"
    printf "  ✓ 数据库数据\n"
    printf "  ✓ 数据库凭据\n"
    printf "  ✓ Python配置文件\n"
    printf "  ✓ 项目代码\n"
    if [ "$MIGRATE_UPLOADS" = "yes" ]; then
        printf "  ✓ 用户上传文件\n"
    fi
    printf "\n"
    
    printf "${BLUE}访问信息${NC}\n"
    printf "${BLUE}%s${NC}\n" "$(printf '%*s' 8 '' | tr ' ' '-')"
    printf "  网站地址: ${GREEN}http://%s${NC}\n" "$TARGET_IP"
    printf "  管理后台: ${GREEN}http://%s/admin${NC}\n" "$TARGET_IP"
    printf "  聊天室: ${GREEN}http://%s/im${NC}\n" "$TARGET_IP"
    printf "\n"
    
    printf "${YELLOW}注意事项${NC}\n"
    printf "${YELLOW}%s${NC}\n" "$(printf '%*s' 8 '' | tr ' ' '-')"
    printf "  1. 如需配置域名和HTTPS，请在目标服务器上重新运行部署脚本\n"
    printf "  2. 默认管理员账号: Sara, 密码: aaa\n"
    printf "  3. 请及时修改管理员密码确保安全\n"
    printf "  4. 如有问题，请检查目标服务器的Docker容器状态\n"
    printf "\n"
    
    printf "${GREEN}%80s${NC}\n" | tr ' ' '='
}

# 主函数
main() {
    # 解析命令行参数
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --version)
                UPDATE_VERSION="$2"
                shift 2
                ;;
            --latest|--use-main|--use-latest)
                USE_LATEST_TAG=false
                UPDATE_VERSION=""
                shift
                ;;
            *)
                shift
                ;;
        esac
    done

    echo ""
  printf "${GREEN}██████╗  ██████╗ ███████╗████████╗██╗███████╗███████╗${NC}\n"
  printf "${GREEN}██╔══██╗██╔═══██╗██╔════╝╚══██╔══╝██║╚══███╔╝██╔════╝${NC}\n"
  printf "${GREEN}██████╔╝██║   ██║█████╗     ██║   ██║  ███╔╝ █████╗${NC}\n"
  printf "${GREEN}██╔═══╝ ██║   ██║██╔══╝     ██║   ██║ ███╔╝  ██╔══╝${NC}\n"
  printf "${GREEN}██║     ╚██████╔╝███████╗   ██║   ██║███████╗███████╝${NC}\n"
  printf "${GREEN}╚═╝      ╚═════╝ ╚══════╝   ╚═╝   ╚═╝╚══════╝╚══════╝${NC}\n"
    echo -e "${BLUE}博客迁移工具====================================================${NC}"
    echo ""
    
    # 显示版本信息
    if [ -n "$UPDATE_VERSION" ]; then
        info "迁移到指定版本: $UPDATE_VERSION"
    elif [ "$USE_LATEST_TAG" = "false" ]; then
        info "使用main分支（最新开发版本）"
    fi
    
    # 检测降级操作
    if [ -n "$UPDATE_VERSION" ]; then
        # 读取当前部署版本
        local current_version=""
        if [ -f ".config/version.txt" ]; then
            current_version=$(grep "^VERSION=" .config/version.txt 2>/dev/null | cut -d'=' -f2)
        fi
        
        if [ -n "$current_version" ] && [ "$current_version" != "main" ]; then
            # 比较版本（去掉v前缀，使用sort -V比较）
            local target_v=$(echo "$UPDATE_VERSION" | sed 's/^v//')
            local current_v=$(echo "$current_version" | sed 's/^v//')
            
            # 使用sort -V比较版本
            local lower_version=$(printf "%s\n%s" "$target_v" "$current_v" | sort -V | head -1)
            
            if [ "$lower_version" = "$target_v" ] && [ "$target_v" != "$current_v" ]; then
                error "不支持版本降级操作！"
                printf "\n"
                printf "${YELLOW}当前版本: ${NC}${GREEN}$current_version${NC}\n"
                printf "${YELLOW}目标版本: ${NC}${RED}$UPDATE_VERSION${NC}\n"
                printf "\n"
                printf "${RED}降级可能导致数据库兼容性问题，请使用当前版本或更高版本进行迁移。${NC}\n"
                printf "\n"
                exit 1
            fi
        fi
    fi

    # 初始化状态管理
    info "初始化迁移状态管理..."
    
    # 检查是否有未完成的迁移
    if [ -f "$STATE_FILE" ]; then
        warning "检测到未完成的迁移任务"
        show_migration_progress
        echo
        read -p "是否继续之前的迁移? (y/n): " continue_migration
        if [[ ! "$continue_migration" =~ ^[Yy]$ ]]; then
            info "清理之前的迁移状态..."
            clean_state
        else
            # 尝试加载之前保存的用户数据
            load_user_data || true
        fi
    fi
    
    # 设置错误处理
    set -e
    trap cleanup EXIT
    
    # 执行初始化步骤（支持断点续传）
    check_prerequisites
    read_db_credentials
    get_user_input
    
    # 尝试从nginx配置文件中提取域名
    if extract_domains_from_nginx; then
        info "已从nginx配置文件中提取域名，将在部署时自动使用"
    else
        info "未能从nginx配置文件中提取域名，部署时将使用交互式输入"
    fi
    
    # 显示当前进度
    show_migration_progress
    
    backup_database
    test_ssh_connection
    detect_target_environment
    pull_code_on_target
    transfer_files
    deploy_on_target
    execute_sql_scripts_on_target
    
    # 执行volume数据迁移
    info "开始volume数据迁移..."
    
    # 执行用户上传文件迁移
    if ! migrate_uploads; then
        error "用户上传文件迁移失败"
        exit 1
    fi
    
    # 显示总结
    show_migration_summary
    
    # 清理临时文件和状态
    cleanup
    clean_state
}

# 显示迁移总结
show_migration_summary() {
    echo -e ""
    echo -e "${GREEN}===========================================${NC}"
    echo -e "${GREEN}           迁移完成总结${NC}"
    echo -e "${GREEN}===========================================${NC}"
    echo -e ""
    
    # 显示各步骤状态
    local step_status
    echo -e "${BLUE}迁移步骤完成情况:${NC}"
    
    step_status=$(get_step_status "$STEP_PREREQUISITES")
    echo -e "  ✓ 前置条件检查: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_READ_CREDENTIALS")
    echo -e "  ✓ 读取数据库凭据: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_USER_INPUT")
    echo -e "  ✓ 用户输入收集: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_EXTRACT_DOMAINS")
    echo -e "  ✓ 域名提取: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_BACKUP_DB")
    echo -e "  ✓ 数据库备份: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_TEST_SSH")
    echo -e "  ✓ SSH连接测试: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_DETECT_ENV")
    echo -e "  ✓ 环境检测: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_PULL_CODE")
    echo -e "  ✓ 代码拉取: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_TRANSFER_FILES")
    echo -e "  ✓ 文件传输: ${GREEN}$step_status${NC}"
    
    if [ "$MIGRATE_UPLOADS" = "yes" ]; then
        echo -e "  ✓ 用户上传文件迁移: ${GREEN}completed${NC}"
    else
        echo -e "  ⏭ 用户上传文件迁移: ${YELLOW}skipped${NC}"
    fi
    
    echo -e "  ✓ 预渲染文件迁移: ${GREEN}completed${NC}"
    
    step_status=$(get_step_status "$STEP_DEPLOY")
    echo -e "  ✓ 项目部署: ${GREEN}$step_status${NC}"
    
    step_status=$(get_step_status "$STEP_EXECUTE_SQL")
    echo -e "  ✓ 执行SQL脚本: ${GREEN}$step_status${NC}"
    
    echo -e ""
    echo -e "${GREEN}目标服务器信息:${NC}"
    echo -e "  IP地址: $TARGET_IP"
    echo -e "  端口: $TARGET_PORT"
    echo -e "  用户名: $TARGET_USER"
    echo -e "  项目路径: $CURRENT_DIR/$extract_dir"
    echo -e ""
    
    # 检查是否所有步骤都完成
    local all_completed=true
    for step in "$STEP_PREREQUISITES" "$STEP_READ_CREDENTIALS" "$STEP_USER_INPUT" "$STEP_EXTRACT_DOMAINS" "$STEP_BACKUP_DB" "$STEP_TEST_SSH" "$STEP_DETECT_ENV" "$STEP_PULL_CODE" "$STEP_TRANSFER_FILES" "$STEP_DEPLOY" "$STEP_EXECUTE_SQL"; do
        if ! is_step_completed "$step"; then
            all_completed=false
            break
        fi
    done
    
    if [ "$all_completed" = true ]; then
        echo -e "${GREEN}🎉 迁移已成功完成！${NC}"
        echo -e "${YELLOW}请访问目标服务器验证服务是否正常运行。${NC}"
    else
        echo -e "${YELLOW}⚠️  迁移未完全完成，请检查失败的步骤。${NC}"
        echo -e "${YELLOW}可以重新运行脚本继续未完成的步骤。${NC}"
    fi
    echo -e ""
}

# 运行主函数
main "$@"