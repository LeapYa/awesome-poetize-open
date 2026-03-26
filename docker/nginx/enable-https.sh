#!/bin/sh

# 检测系统类型
detect_os() {
    if [ -f /etc/debian_version ]; then
        echo "debian"
    elif [ -f /etc/alpine-release ]; then
        echo "alpine"
    else
        echo "unknown"
    fi
}

OS_TYPE=$(detect_os)
echo "检测到系统类型: $OS_TYPE"

# 定义文件操作函数（nginx用户已有必要权限）
safe_cp() {
    src="$1"
    dest="$2"
    echo "正在复制: $src -> $dest"
    
    # nginx用户有必要权限，直接复制
    cp "$src" "$dest" && return 0
    
    # 尝试创建父目录
    mkdir -p "$(dirname "$dest")" 2>/dev/null
    cp "$src" "$dest" && return 0
    
    return 1
}

safe_mv() {
    src="$1"
    dest="$2"
    echo "正在移动: $src -> $dest"
    
    # nginx用户有必要权限，直接移动
    mv "$src" "$dest" && return 0
    
    return 1
}

safe_mkdir() {
    dir="$1"
    echo "正在创建目录: $dir"
    
    # nginx用户有必要权限，直接创建
    mkdir -p "$dir" && return 0
    
    return 1
}

# 错误处理函数
handle_error() {
    echo "错误：$1"
    echo "回退到HTTP配置..."
    cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template /usr/local/openresty/nginx/conf/conf.d/default.conf 2>/dev/null || true
    return 0
}

# 安装软件包的跨平台函数
install_package() {
    pkg_name="$1"
    echo "尝试安装 $pkg_name（通常应该已在构建时安装）..."
    
    # 在容器中，通常以nginx用户运行，可能没有安装权限
    # 如果OpenSSL等工具缺失，这表明Dockerfile需要更新
    echo "警告：$pkg_name 缺失，请检查Dockerfile中是否已正确安装此包"
    return 1
}

# 注意：默认配置文件已在构建时创建，这里只需要在需要时切换到HTTPS
echo "默认HTTP配置已在构建时设置，当前将检查是否需要切换到HTTPS..."

# 获取当前使用的配置文件中的主站域名信息，忽略默认块的 server_name _
ALL_DOMAINS=$(awk '
    /^[[:space:]]*server_name[[:space:]]+/ {
        line = $0
        sub(/^[[:space:]]*server_name[[:space:]]+/, "", line)
        sub(/;.*/, "", line)
        if (line != "_" && line != "") {
            print line
            exit
        }
    }
' /usr/local/openresty/nginx/conf/conf.d/default.conf 2>/dev/null || echo "localhost")
# 获取第一个域名作为主域名
CURRENT_DOMAIN=$(echo "$ALL_DOMAINS" | awk '{print $1}' 2>/dev/null || echo "localhost")

echo "当前配置的域名: $CURRENT_DOMAIN"
echo "所有域名: $ALL_DOMAINS"

# 本地环境检测逻辑
IS_LOCAL=0
if [ "$CURRENT_DOMAIN" = "localhost" ]; then
    IS_LOCAL=1
elif [ "$CURRENT_DOMAIN" = "127.0.0.1" ]; then
    IS_LOCAL=1
elif echo "$CURRENT_DOMAIN" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then
    # IP地址格式
    IS_LOCAL=1
fi

# 显式打印检测结果
if [ $IS_LOCAL -eq 1 ]; then
    echo "检测到本地开发环境 ($CURRENT_DOMAIN)"
else
    echo "检测到生产环境，域名: $CURRENT_DOMAIN"
fi

# 如果明确指定了强制模式，则覆盖自动检测结果
if [ "$1" = "--force-http" ]; then
    echo "强制使用HTTP模式"
    IS_LOCAL=1
elif [ "$1" = "--force-https" ]; then
    echo "强制使用HTTPS模式"
    IS_LOCAL=0
fi

# 如果是本地环境，可以使用自签名证书进行HTTPS
if [ $IS_LOCAL -eq 1 ]; then
    echo "检测到本地环境，将创建自签名证书..."
    
    # 确保目录存在
    safe_mkdir "/etc/letsencrypt/live/$CURRENT_DOMAIN" || {
        handle_error "无法创建证书目录"
        exit 0
    }
    
    # 检查证书是否已存在
    if [ ! -f "/etc/letsencrypt/live/$CURRENT_DOMAIN/fullchain.pem" ] || [ ! -f "/etc/letsencrypt/live/$CURRENT_DOMAIN/privkey.pem" ]; then
        echo "为 $CURRENT_DOMAIN 创建自签名证书..."
        
        # 安装OpenSSL (如果没有)
        if ! command -v openssl > /dev/null; then
            install_package "openssl" || {
                handle_error "无法安装OpenSSL"
                exit 0
            }
        fi
        
        # 创建自签名证书
        openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
            -keyout "/tmp/privkey.pem" \
            -out "/tmp/fullchain.pem" \
            -subj "/CN=$CURRENT_DOMAIN" || {
                handle_error "创建自签名证书失败"
                exit 0
            }
        
        # 移动生成的证书到目标目录
        safe_cp "/tmp/privkey.pem" "/etc/letsencrypt/live/$CURRENT_DOMAIN/privkey.pem" || {
            handle_error "复制privkey.pem失败"
            exit 0
        }
        
        safe_cp "/tmp/fullchain.pem" "/etc/letsencrypt/live/$CURRENT_DOMAIN/fullchain.pem" || {
            handle_error "复制fullchain.pem失败"
            exit 0
        }
        
        # 创建chain.pem (复制fullchain.pem)
        safe_cp "/etc/letsencrypt/live/$CURRENT_DOMAIN/fullchain.pem" "/etc/letsencrypt/live/$CURRENT_DOMAIN/chain.pem" || {
            handle_error "复制chain.pem失败"
            exit 0
        }
        
        echo "自签名证书创建完成"
    else
        echo "已存在自签名证书，将继续使用"
    fi
    
    # 读取HTTPS配置模板并替换域名
    sed "s/example.com www.example.com/$ALL_DOMAINS/g" /usr/local/openresty/nginx/conf/conf.d/default.https.conf.template > /tmp/new_default.conf || {
        handle_error "替换HTTPS配置中的域名失败"
        exit 0
    }
    
    # 替换证书路径中的域名
    sed -i "s|/etc/letsencrypt/live/example.com/|/etc/letsencrypt/live/$CURRENT_DOMAIN/|g" /tmp/new_default.conf || {
        handle_error "替换HTTPS配置中的证书路径失败"
        exit 0
    }
    
    # 检查生成的配置文件是否有效
    echo "验证Nginx配置有效性..."
    nginx -t -c /usr/local/openresty/nginx/conf/nginx.conf -p /usr/local/openresty/nginx
    
    if [ $? -eq 0 ]; then
        # 配置文件有效，替换现有配置
        safe_mv /tmp/new_default.conf /usr/local/openresty/nginx/conf/conf.d/default.conf
        
        # 如果nginx已经运行，使用reload，否则跳过（初始启动时会由主命令启动nginx）
        if pidof nginx >/dev/null; then
            echo "应用HTTPS配置（重新加载）..."
                nginx -s reload || {
                    handle_error "Nginx重新加载配置失败"
                    exit 0
                }
        else
            echo "Nginx尚未运行，跳过重新加载，将在启动时应用配置"
        fi
        
        echo "本地HTTPS已成功启用！访问 https://$CURRENT_DOMAIN 查看您的网站"
        echo "注意：因为使用自签名证书，浏览器可能会显示安全警告，这是正常的"
    else
        echo "错误：生成的Nginx配置文件无效，使用HTTP配置..."
        cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template /usr/local/openresty/nginx/conf/conf.d/default.conf
        echo "HTTP配置已应用"
    fi
    
    exit 0
fi

# 首先检查当前域名的证书目录是否存在
if [ -d "/etc/letsencrypt/live/$CURRENT_DOMAIN" ]; then
    CERT_DIR="/etc/letsencrypt/live/$CURRENT_DOMAIN"
    DOMAIN="$CURRENT_DOMAIN"
    echo "找到与当前域名匹配的证书目录: $CERT_DIR"
else
    # 获取证书目录（通常是域名）
    CERT_DIRS=$(find /etc/letsencrypt/live -maxdepth 1 -type d -not -path "/etc/letsencrypt/live" 2>/dev/null || echo "")

    if [ -z "$CERT_DIRS" ]; then
        echo "错误：找不到任何SSL证书目录。使用HTTP配置..."
        cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template /usr/local/openresty/nginx/conf/conf.d/default.conf
        echo "HTTP配置已应用"
        exit 0
    fi

    # 使用第一个找到的证书目录
    CERT_DIR=$(echo "$CERT_DIRS" | head -n 1)
    DOMAIN=$(basename "$CERT_DIR")

    echo "未找到与当前域名($CURRENT_DOMAIN)匹配的证书目录，将使用: $CERT_DIR"
fi

echo "证书域名: $DOMAIN"

# 权限检查和修复函数（与certbot脚本保持一致）
fix_cert_permissions_if_needed() {
    # 检查是否能以nginx用户权限读取证书文件
    if [ -f "$CERT_DIR/fullchain.pem" ] && [ ! -r "$CERT_DIR/fullchain.pem" ]; then
        echo "警告：检测到证书文件权限问题，尝试修复..."
        
        # 以root权限修复权限（如果当前有root权限）
        if [ "$(id -u)" = "0" ]; then
            echo "以root权限修复证书文件权限..."
            find /etc/letsencrypt/live -type d -exec chmod 755 {} \; 2>/dev/null || true
            find /etc/letsencrypt/archive -type d -exec chmod 755 {} \; 2>/dev/null || true
            find /etc/letsencrypt/live -name "*.pem" -exec chmod 644 {} \; 2>/dev/null || true
            find /etc/letsencrypt/archive -name "*.pem" -exec chmod 644 {} \; 2>/dev/null || true
            chmod 755 /etc/letsencrypt/live 2>/dev/null || true
            chmod 755 /etc/letsencrypt/archive 2>/dev/null || true
            echo "证书文件权限修复完成"
        else
            echo "当前用户不是root，无法修复权限"
            echo "请确保certbot容器已正确设置证书文件权限"
        fi
    fi
}

# 修复证书权限（如果需要）
fix_cert_permissions_if_needed

# 检查证书文件是否存在
if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
    echo "找到SSL证书文件，启用HTTPS配置..."

    # 构建正确的域名配置
    if [ -n "$ALL_DOMAINS" ]; then
        DOMAIN_CONFIG="$ALL_DOMAINS"
    else
        # 使用证书中的域名作为配置
        DOMAIN_CONFIG="$DOMAIN"
    fi
    
    echo "将使用以下server_name配置: $DOMAIN_CONFIG"
    
    # 读取HTTPS配置模板并替换域名
    sed "s/example.com www.example.com/$DOMAIN_CONFIG/g" /usr/local/openresty/nginx/conf/conf.d/default.https.conf.template > /tmp/new_default.conf || {
        handle_error "替换HTTPS配置中的域名失败"
        exit 0
    }
    
    # 替换证书路径中的域名
    sed -i "s|/etc/letsencrypt/live/example.com/|/etc/letsencrypt/live/$DOMAIN/|g" /tmp/new_default.conf || {
        handle_error "替换HTTPS配置中的证书路径失败"
        exit 0
    }
    
    # 检查生成的配置文件是否有效
    echo "验证Nginx配置有效性..."
    nginx -t -c /usr/local/openresty/nginx/conf/nginx.conf -p /usr/local/openresty/nginx
    
    if [ $? -eq 0 ]; then
        # 配置文件有效，替换现有配置
        safe_mv /tmp/new_default.conf /usr/local/openresty/nginx/conf/conf.d/default.conf
        
        # 如果nginx已经运行，使用reload，否则跳过（初始启动时会由主命令启动nginx）
        if pidof nginx >/dev/null; then
            echo "应用HTTPS配置（重新加载）..."
                nginx -s reload || {
                    handle_error "Nginx重新加载配置失败"
                    exit 0
                }
        else
            echo "Nginx尚未运行，跳过重新加载，将在启动时应用配置"
        fi
        
        echo "HTTPS已成功启用！访问 https://$DOMAIN 查看您的网站"
    else
        echo "错误：生成的Nginx配置文件无效，使用HTTP配置..."
        cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template /usr/local/openresty/nginx/conf/conf.d/default.conf
        echo "HTTP配置已应用"
    fi
else
    echo "错误：在 $CERT_DIR 中找不到完整的SSL证书文件。"
    echo "需要的文件: fullchain.pem, privkey.pem"
    echo "使用HTTP配置..."
    cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template /usr/local/openresty/nginx/conf/conf.d/default.conf
    echo "HTTP配置已应用"
fi 
