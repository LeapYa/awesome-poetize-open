<p align="center">
  <a href="#">
    <img src="poetize_picture/首页1.jpg" alt="Logo" width="100%">
  </a>

<h1 align="center">POETIZE 最美博客（AGPL 分支 · LeapYa 维护）</h1>
  <p align="center">
    让内容创作与社交体验更美好
    <br />
    <br />
    <a href="#-快速开始">快速部署</a>
    ·
    <a href="#-部署文档">部署文档</a>
    ·
    <a href="#-开发指南">二次开发</a>
  </p>
  <p align="center">
   <img src="https://img.shields.io/badge/license-AGPL--3.0-%3CCOLOR%3E.svg" alt="AGPL License">
   <img src="https://img.shields.io/badge/language-java-%23B07219.svg" alt="Java">
   <img src="https://img.shields.io/badge/language-python-%233572A5.svg" alt="Python">
   <img src="https://img.shields.io/badge/language-dockerfile-%23384D54.svg" alt="Dockerfile">
  </p>
</p>

## 📑 目录

- [项目简介](#-项目简介)
- [快速开始](#-快速开始)
- [部署文档](#-部署文档)
- [贡献与许可](#-贡献与许可)
- [开发指南](#-开发指南)
- [技术栈](#️-技术栈)
- [联系方式](#-联系方式)
- [版权说明](#-版权说明)

## 📖 项目简介

本项目**Awesome-poetize-open**是基于开源项目 [POETIZE最美博客](https://gitee.com/littledokey/poetize) 功能扩展和定制化开发，历时半年，这是一个集内容创作、社交互动与技术优化于一体的现代化博客系统，非常适合个人建站和内容创作者使用。

<p align="center">
  <img src="poetize_picture/首页.png" alt="首页" width="100%">
</p>

<p align="center">博客首页 - 展示个人创作与生活点滴</p>

<p align="center">
  <img src="poetize_picture/首页1.jpg" alt="文章展示" width="49%">
  <img src="poetize_picture/首页2.jpg" alt="社交功能" width="49%">
</p>

<p align="center">左：内容布局展示 | 右：社交功能体验</p>

#### **本分支新增/优化功能**

1. ✅ 一键部署脚本 —— 一行命令自动完成环境配置、HTTPS配置和服务启动
2. ✅ 后台权限管理 —— 支持多角色分级管理，提升安全性
3. ✅ 多邮箱服务支持 —— 可配置多邮箱，提升邮件送达率
4. ✅ 第三方登录集成 —— 支持GitHub、Google、Twitter、Yandex、Gitee平台登录
5. ✅ 机器人验证功能 —— 集成点选、滑动验证码，防止恶意注册
6. ✅ SEO优化与预渲染 —— 自动生成sitemap、robots.txt及页面预渲染，极大提升搜索引擎收录与SEO效果
7. ✅ 看板娘优化 —— Live2D看板娘可自定义、支持AI互动
8. ✅ 导航栏优化 —— 支持自定义导航栏，布局更美观
9. ✅ 评论体验优化 —— 评论内容自动保存，未登录也不丢失
10. ✅ 增加兰空图床、简单图床的存储支持 —— 支持多种图片上传方式
11. ✅ AI翻译 —— 支持中英互译，可用本地或API模型
12. ✅ 页脚优化 —— 页脚信息更丰富、可自定义
13. ✅ 图片压缩和转换WebP格式 —— 自动压缩图片，提升网站加载速度
14. ✅ 智能摘要 —— 自动生成文章摘要，提升阅读体验
15. ✅ 暗色模式优化、定时暗色模式 —— 支持夜间自动切换暗色主题，优化暗色模式
16. ✅ 灰色模式 —— 支持全站灰色纪念模式
17. ✅ 自定义错误页面 —— 提供友好的404、403等错误页面
18. ✅ 字体文件CDN化 —— 支持字体文件外部化存储与动态加载，可配置单一/分块字体模式，自定义Unicode范围，大幅减少网站带宽占用
19. ✅ 将MD5密码哈希升级为BCrypt算法 —— 修复密码安全漏洞
20. ✅ 评论区重构、优化其楼层计算算法优化 —— 对评论区进行重构，引入懒加载机制以提升页面加载速度，并使用深度优先遍历算法优化评论楼层计算逻辑，提高渲染性能
21. ✅ Redis缓存优化 —— 大部分接口使用Redis缓存，提升性能
22. ✅ 实现token签名算法HMAC-SHA256认证 —— 完全替换简单UUID token，新增防伪造、防篡改、防重放攻击能力

更多功能...

## 🚀 快速开始

```bash
# 你只需要输入域名邮箱即可
bash <(curl -sL install.leapya.com)
```

脚本将自动完成所有配置，包括Docker安装、数据库初始化和HTTPS配置。

## 📋 部署文档

### 1.准备服务器+域名解析

#### 服务器选择指南

选择可靠的云服务商即可，根据价格和需求自行决定。

**地域选择：**

- **香港云服务器** - 免备案，即买即用，推荐不想备案的用户
- **国内云服务器** - 需要备案，约需3-7个工作日，适合面向国内用户的站点

#### 服务器配置要求

**基础配置：**

- **操作系统**：Ubuntu 18.04+、Debian 10+ 或 CentOS 7/8+
- **CPU/内存**：2核+ / 2GB+
- **硬盘空间**：15GB+
- **带宽选择**：建议5M以上
- **网络配置**：将域名解析到服务器IP，并开放80和443端口

> 部署时请确保服务器内存充足，内存较低时部署脚本会自动进行内存优化并开启交换空间，经过测试，1核1G内存可以成功部署，但性能较差，也可能会部署失败，推荐2核2GB内存，4核4GB内存更佳

#### 系统兼容性测试结果

| 操作系统类型          | CPU  | 内存 | 存储 | 测试结果  |
| --------------------- | ---- | ---- | ---- | --------- |
| Ubuntu 18.04+ x64     | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| Debian 10+ x64        | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| CentOS 7/8+ x64       | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| Windows Server/桌面版 | -    | -    | -    | ❌ 不支持 |

> **其他支持的系统**：RHEL、Rocky Linux、AlmaLinux、Fedora、Amazon Linux、阿里云/腾讯云 Linux、麒麟、统信UOS、Deepin、openEuler、Alpine、Arch Linux、openSUSE等主流Linux发行版均已测试通过。

### 2.运行一键安装脚本

```bash
# 以下方式任选其一即可
# 方式一：交互模式
bash <(curl -sL install.leapya.com)

# 方式二：非交互模式(替换成自己的域名，每个域名使用-d隔开)
bash <(curl -sL install.leapya.com) -d 域名.com -d www.域名.com

# 方式三：克隆本仓库部署（交互模式）
git clone https://github.com/LeapYa/Awesome-poetize-open.git && sudo chmod +x deploy.sh && sudo ./deploy.sh

# 方式四：克隆本仓库部署（非交互模式）
git clone https://github.com/LeapYa/Awesome-poetize-open.git && sudo chmod +x deploy.sh && sudo ./deploy.sh -d 域名.com -d www.域名.com
```

> 部署脚本已经做好了错误处理和重试机制，如果仍然部署失败，请查看[常见问题](#6常见问题)

### 3.访问方式

部署完成后，可通过以下地址访问系统功能：

* 主站：`http(s)://域名/`
* 聊天室：`http(s)://域名/im`
* 管理后台：`http(s)://域名/admin`

**默认管理员凭证**：

- 用户名：`Sara`
- 密码：`aaa`

### 4.可选配置

#### 更换字体

如需更换网站字体，提供两种方法：

**方法1：分块字体模式**

1. 将新字体文件（TTF格式）放入 `split_font/` 文件夹
2. 重命名为 `font.ttf`
3. 安装依赖：`pip install -r requirements.txt`
4. 执行：`python font_subset.py`
5. 将生成的 `font_chunks` 目录复制到：
   - `poetize-ui/public/assets/`
   - `poetize-ui/public/static/assets/`
6. 重启前端服务

**方法2：单一字体模式**

1. 在后台管理 → 配置管理中，设置"使用单一字体文件"为 `true`
2. 将新字体文件（WOFF2格式）重命名为 `font.woff2`
3. 复制到：
   - `poetize-ui/public/assets/`
   - `poetize-ui/public/static/assets/`
4. 重启前端服务

#### OAuth代理

若需支持国外第三方登录平台（GitHub、Google等），请配置海外代理服务器，详见[OAuth代理配置说明文档](docs/OAuth代理配置说明.md)。

#### Ollama本地翻译模型

如需启用本地AI翻译功能，编辑 `docker-compose.yml` 找到"Ollama翻译模型服务"部分取消注释即可。默认使用 `qwen3:0.6b` 轻量级模型。更多模型选择和配置详见 [Ollama官方模型库](https://ollama.com/library)。

### 5.常用命令

```bash
# 容器状态
docker ps -a

# 查看日志
docker logs poetize-nginx

# 服务管理（docker-compose旧版本的命令是docker compose）
docker compose restart
docker compose down
docker compose up -d

# HTTPS手动配置
docker exec poetize-nginx /enable-https.sh

# 升级项目（全量更新，不管项目有没有更新）
poetize -update

# 迁移博客
poetize -qy
```

### 6.常见问题

#### 项目部署失败

项目在部署时可能因任何原因（网络波动、资源不足等）导致部署失败，在1核1G服务器较常见，如果部署失败，可执行以下命令清理并重新部署：

```bash
docker system prune -af && rm -rf Awesome-poetize-open && bash <(curl -sL install.leapya.com)
```

更多详见[开发排障指南](#开发排障指南)

### 7.高级功能

本项目提供三个管理脚本，使用 `poetize -h`、`./deploy.sh -h` 或 `./migrate.sh` 查看详细用法。

#### 国内环境部署

`deploy.sh` 脚本已内置国内镜像源加速。~~若网络受限，可从Release下载离线资源包，包含Docker安装包和所有镜像文件。~~

## 🤝 贡献与许可

* 原作者：Sara (POETIZE最美博客)
* Fork版本开发：LeapYa
* 开源协议：遵循原项目AGPL协议

## 💻 开发指南

### 环境要求

* **Node.js 14+** - 前端开发
* **JDK 25** - Java后端开发（支持 JDK 21+）
* **Maven 3.9+** - Java项目构建
* **Python 3.9+** - Python后端开发
* **Docker & Compose** - 容器化部署
* **Git** - 版本控制
* **数据库客户端** - 开发调试

### 项目结构

<details>
<summary>
项目主要目录结构（点击展开完整结构）

```
├── poetize                  # 全局管理命令（升级、迁移、日志等）
├── deploy.sh                # 一键部署脚本
├── migrate.sh               # 博客迁移脚本
├── docker-compose.yml       # Docker服务编排文件
├── docs/                    # 项目文档
├── docker/                  # Docker构建配置目录
├── poetize-server/          # Java后端（Spring Boot 3.5.5 + Java 25）
├── poetize-ui/              # 博客前端（Vue2）
├── poetize-im-ui/           # 聊天室前端（Vue3）
├── py/                      # Python后端服务（FastAPI）
├── split_font/              # 字体分割工具
└── README.md                # 项目文档
```

</summary>

```
.
├── poetize                  # 全局管理命令（安装后可全局使用）
├── deploy.sh                # 一键部署脚本
├── migrate.sh               # 博客迁移脚本
├── docker-compose.yml       # Docker服务编排配置
├── docs/                    # 项目文档
│   └── OAuth代理配置说明.md # OAuth代理配置文档
├── docker/                  # Docker构建文件
│   ├── java/                # Java服务Docker配置
│   ├── mysql/               # MariaDB配置
│   │   ├── Dockerfile
│   │   └── conf/my.cnf
│   ├── nginx/               # Nginx配置
│   │   ├── Dockerfile
│   │   ├── nginx.conf
│   │   └── default.conf
│   ├── python/              # Python服务Docker配置
│   ├── redis/               # Redis配置
│   ├── prerender/           # SEO预渲染服务配置
│   │   ├── Dockerfile
│   │   └── worker.js        # 预渲染worker脚本
│   ├── translation_model/   # AI翻译模型服务配置（基于Ollama）
│   │   └── Dockerfile
│   ├── poetize-ui/          # 前端UI Docker配置
│   └── poetize-im-ui/       # 聊天室UI Docker配置
├── poetize-server/          # Java后端服务（Spring Boot 3.5.5 + Java 25）
│   ├── pom.xml              # Maven主配置文件
│   ├── settings.xml         # Maven仓库配置
│   ├── sql/                 # 数据库脚本
│   │   ├── poetry.sql       # 生产环境初始化脚本（多引擎优化）
│   │   ├── poetry_old.sql   # 开发环境初始化脚本（InnoDB）
│   │   └── *.sql            # 其他数据库迁移脚本
│   └── poetry-web/          # Web模块
│       ├── pom.xml          # Web模块Maven配置
│       └── src/             # Java源代码
│           ├── main/
│           │   ├── java/    # Java源文件
│           │   └── resources/ # 配置文件
│           └── test/        # 测试代码
├── poetize-ui/              # 博客前端（Vue2）
│   ├── package.json         # npm依赖配置
│   ├── vue.config.js        # Vue CLI配置
│   ├── public/              # 静态资源
│   └── src/                 # 源代码
│       ├── components/      # Vue组件
│       ├── router/          # 路由配置
│       ├── store/           # Vuex状态管理
│       ├── utils/           # 工具类
│       └── views/           # 页面视图
├── poetize-im-ui/           # 聊天室前端（Vue3）
│   ├── package.json         # npm依赖配置
│   ├── vue.config.js        # Vue CLI配置
│   ├── public/              # 静态资源
│   └── src/                 # 源代码
│       ├── components/      # Vue组件
│       ├── router/          # 路由配置
│       ├── store/           # Vuex状态管理
│       └── utils/           # 工具类
├── py/                      # Python后端服务（FastAPI）
│   ├── main.py              # FastAPI主应用入口
│   ├── requirements.txt     # Python依赖列表
│   ├── config.py            # 配置管理
│   ├── ai_chat_api.py       # AI聊天接口
│   ├── ai_config_client.py  # AI配置客户端
│   ├── translation_api.py   # 翻译服务
│   ├── article_rag_mcp_server.py  # 文章检索增强服务
│   ├── auth_decorator.py    # 认证装饰器
│   ├── cache_helper.py      # 缓存辅助工具
│   ├── redis_client.py      # Redis客户端
│   ├── oauth/               # OAuth第三方登录
│   │   ├── factory.py       # OAuth工厂类
│   │   ├── providers/       # 各平台OAuth实现
│   │   └── ...              # 其他OAuth相关模块
│   └── data/                # 配置数据文件
├── split_font/              # 字体分割工具
│   ├── font_subset.py       # 字体分割脚本
│   ├── font.ttf             # 源字体文件
│   ├── level-1.txt          # 一级常用字表
│   ├── level-2.txt          # 二级常用字表
│   └── font_chunks/         # 生成的分块字体文件
└── README.md                # 项目文档
```

</details>

### 博客前端开发

1. **更换测试环境的访问API(生产环境中需要更改回去)**

   修改poetize-ui/src/utils/constant.js

   ```
   // 测试环境
   baseURL: "http://localhost:8081",
   pythonBaseURL: "http://localhost:5000", // Python服务URL
   imBaseURL: "http://localhost:81/im",
   webURL: "http://localhost",

   // 生产环境(本地开发需要注释掉)
   // webURL: location.protocol + "//" + location.hostname + (location.port ? ':' + location.port : ''),
   // baseURL: location.protocol + "//" + location.hostname + (location.port ? ':' + location.port : '') + "/api",
   // pythonBaseURL: location.protocol + "//" + location.hostname + (location.port ? ':' + location.port : '') + "/python",
   // imBaseURL: location.protocol + "//" + location.hostname + (location.port ? ':' + location.port : '') + "/im",
   ```
2. **依赖安装**

   ```bash
   cd poetize-ui
   npm install
   ```
3. **开发服务**

   ```bash
   npm run serve
   ```

### 聊天室前端开发

1. **安装依赖**

   ```bash
   cd poetize-im-ui
   npm install
   ```
2. **配置修改**

   编辑 `poetize-im-ui/src/utils/constant.js`，切换到测试环境配置：

   ```javascript
   // 测试环境
   baseURL: "http://localhost:8081",
   webBaseURL: "http://localhost",
   imURL: "http://localhost:81/im",
   imBaseURL: "localhost",
   wsProtocol: "ws",
   wsPort: "9324",

   // 生产环境（注释掉）
   // baseURL: location.protocol + "//" + location.hostname...
   ```
3. **开发服务**

   ```bash
   npm run serve
   ```

   访问地址：`http://localhost:81/im`
4. **日志配置（可选）**

   如需减少控制台日志，可编辑 `babel.config.js`，取消注释以下内容：

   ```javascript
   env: {
     production: {
       plugins: [
         ['transform-remove-console', { exclude: ['error', 'warn'] }]
       ]
     }
   }
   ```

   或修改 `.eslintrc.js` 中的规则：

   ```javascript
   rules: {
     'no-console': 'warn',  // 或 'error'
     'no-debugger': 'warn'
   }
   ```

### Java后端开发

1. **导入项目** - 使用IntelliJ IDEA或Eclipse导入poetize-server目录
2. **主要模块**

   - `poetry-web`: 核心业务模块，含控制器、服务和实体类
   - `sql`: 数据库初始化和更新脚本
3. **配置修改**

   - `poetry-web/src/main/resources/application.yml`
4. **开发运行**

   使用Maven命令运行：

   ```bash
   cd poetize-server
   mvn spring-boot:run
   ```

   或在IDE中直接运行主类 `com.ld.poetry.PoetryApplication`

### Python后端开发

Python服务提供以下关键功能（在开发环境，也可不启动此服务）：

- 第三方登录集成（GitHub、Google等）
- 机器翻译服务
- SEO优化（站点地图生成）
- 内容管理API

1. **虚拟环境**

   ```bash
   python -m venv venv
   source venv/bin/activate  # Linux/macOS
   venv\Scripts\activate     # Windows
   ```
2. **依赖安装**

   ```bash
   pip install -r requirements.txt
   # 如果安装依赖失败了，请删除虚拟环境后，使用清华镜像源安装依赖
   ```
3. **启动服务** - 运行主入口脚本

   ```bash
   uvicorn main:app --port 5000 --reload
   ```

### 数据库

系统默认使用 **MariaDB 11**（完全兼容 MySQL 5.7），采用多种存储引擎优化不同场景下的性能表现。

#### 数据库初始化

本项目在 `poetize-server/sql/` 目录提供两个初始化脚本，**表结构和数据完全相同**，仅存储引擎配置不同：

- **poetry.sql** - 使用 InnoDB + Aria + RocksDB 多引擎优化，性能更优，需要 MariaDB 11+ 环境和安装存储引擎插件
- **poetry_old.sql** - 仅使用 InnoDB 引擎，兼容 MySQL 5.7+ 和所有 MariaDB 版本

**快速选择：**

- 使用本项目 Docker 或 部署脚本 部署 → `poetry.sql`（默认，已配置好所有插件）
- Windows 本地开发 + Docker Desktop → `poetry.sql`（推荐，环境一致）
- 使用本地 MySQL/MariaDB → `poetry_old.sql`（无需插件，兼容性最好）

**初始化命令：**

```bash
# Docker 环境（推荐）
# 先获取数据库密码
ROOT_PWD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
docker exec -i poetize-mariadb mariadb -uroot -p${ROOT_PWD} poetize < poetize-server/sql/poetry.sql

# 本地数据库环境（使用自己设置的密码）
mysql -uroot -p poetize < poetize-server/sql/poetry_old.sql
```

#### MariaDB 存储引擎插件安装

如果你使用的是自己搭建的 MariaDB 环境（非本项目 Docker），需要手动安装存储引擎插件才能使用 `poetry.sql`。

**1. RocksDB 存储引擎安装**

RocksDB 是一个高性能的键值存储引擎，适合读写密集型应用（如聊天记录表）。

```bash
# Ubuntu/Debian 系统
sudo apt-get update
sudo apt-get install mariadb-plugin-rocksdb

# CentOS/RHEL 系统
sudo yum install MariaDB-rocksdb-engine

# 或使用 dnf（较新的系统）
sudo dnf install MariaDB-rocksdb-engine
```

安装后，在 MariaDB 配置文件中启用：

```ini
# /etc/mysql/conf.d/rocksdb.cnf 或 /etc/my.cnf.d/rocksdb.cnf
[mysqld]
plugin_load_add=rocksdb=ha_rocksdb.so
```

重启 MariaDB 服务：

```bash
sudo systemctl restart mariadb
```

验证安装：

```sql
-- 登录 MariaDB 后执行
SHOW ENGINES;
-- 应该能看到 ROCKSDB 引擎状态为 YES 或 DEFAULT
```

**2. Aria 存储引擎**

Aria 是 MariaDB 的默认存储引擎之一（MyISAM 的改进版），**无需额外安装**，MariaDB 10.0+ 自带。

**3. Docker 环境配置参考**

本项目的 `docker/mysql/Dockerfile` 已经配置好所有存储引擎：

```dockerfile
FROM mariadb:11.8.2

# 安装 RocksDB 插件
RUN apt-get update && apt-get install -y mariadb-plugin-rocksdb && \
    rm -rf /var/lib/apt/lists/*

# 自动加载 RocksDB 引擎
RUN echo "[mysqld]\nplugin_load_add=rocksdb=ha_rocksdb.so" > /etc/mysql/conf.d/rocksdb.cnf
```

**4. 存储引擎说明**

| 存储引擎          | 用途                             | 特点                 | 是否需要安装          |
| ----------------- | -------------------------------- | -------------------- | --------------------- |
| **InnoDB**  | 通用表（用户、文章等）           | 事务支持、外键、行锁 | ❌ MariaDB/MySQL 自带 |
| **Aria**    | 静态数据表（分类、标签、配置等） | 高速读取、崩溃恢复   | ❌ MariaDB 10.0+ 自带 |
| **RocksDB** | 高并发写入表（聊天记录、历史等） | LSM树结构、压缩存储  | ✅ 需手动安装插件     |

**5. Windows/Linux 开发环境连接数据库**

无论使用什么操作系统，开发环境推荐以下方式：

```bash
# 方案A：完整 Docker 环境（推荐，环境与生产一致）
docker compose up -d mysql redis
# 使用 poetry.sql - 包含所有存储引擎优化

# 方案B：仅使用本地数据库（快速开发）
# 本地安装 MariaDB/MySQL，使用 poetry_old.sql
# 优点：数据库管理工具（如 Navicat）连接更方便
# 缺点：缺少性能优化的存储引擎
```

#### 数据库选择说明

本项目默认采用MariaDB 11作为数据库，而非MySQL 5.7。这是基于以下考虑：

1. **性能优势** - MariaDB在高并发读写场景下通常表现更佳
2. **开源承诺** - MariaDB保持完全开源，没有商业许可限制
3. **兼容性保证** - 完全兼容MySQL的SQL语法和连接协议
4. **额外特性** - 提供更多存储引擎选项和优化器改进

虽然项目可以使用MySQL运行，但我们推荐使用MariaDB以获得更好的性能和未来的可扩展性。若必须使用MySQL，请参考下方切换步骤进行配置调整。两种数据库的数据文件和表结构完全兼容，可以无缝迁移。

#### 从MariaDB切换到MySQL的详细步骤

如需在开发或部署环境中使用MySQL替代MariaDB，按以下步骤操作：

1. **修改Java项目依赖（pom.xml）**

   ```xml
   <!-- 移除MariaDB依赖 -->
   <dependency>
     <groupId>org.mariadb.jdbc</groupId>
     <artifactId>mariadb-java-client</artifactId>
     <version>3.1.4</version>
     <!-- ... -->
   </dependency>

   <!-- 添加MySQL依赖 -->
   <dependency>
     <groupId>mysql</groupId>
     <artifactId>mysql-connector-java</artifactId>
     <version>8.0.33</version>
   </dependency>
   ```
2. **更新Java应用配置（application.yml）**

   ```yaml
   spring:
     datasource:
       # 从这个
       driver-class-name: org.mariadb.jdbc.Driver
       url: jdbc:mariadb://localhost:3306/poetize?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai

       # 改为这个
       driver-class-name: com.mysql.cj.jdbc.Driver
       url: jdbc:mysql://localhost:3306/poetize?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
   ```
3. **修改Docker镜像构建文件（docker/java/Dockerfile）**

   ```Dockerfile
   # 将
   RUN wget -O /app/libs/mariadb-java-client-3.1.4.jar https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar

   # 改为
   RUN wget -O /app/libs/mysql-connector-java-8.0.33.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
   ```
4. **更新Docker Compose配置（docker-compose.yml）**

   ```yaml
   # MariaDB服务配置
   mysql:
     # 从这个
     image: mariadb:11.1
     # 改为这个
     image: mysql:8.0

     environment:
       # 从这些环境变量
       - MARIADB_ROOT_PASSWORD=root123
       - MARIADB_DATABASE=poetize 
       - MARIADB_USER=poetize
       - MARIADB_PASSWORD=poetize123

       # 改为这些
       - MYSQL_ROOT_PASSWORD=root123
       - MYSQL_DATABASE=poetize
       - MYSQL_USER=poetize
       - MYSQL_PASSWORD=poetize123
   ```

   还需要修改Java服务环境变量部分：

   ```yaml
   poetize-java:
     # ...
     environment:
       # 修改驱动类名
       - SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
       # 修改URL前缀
       - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/poetize?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
   ```
5. **Python服务配置修改**（如果使用Python服务连接数据库）

   ```yaml
   poetize-python:
     # ...
     environment:
       # 修改数据库类型
       - DB_TYPE=mysql
       # 可能需要添加或修改连接参数
       - DB_CHARSET=utf8mb4
   ```
6. **修改my.cnf配置**（MySQL 8.0和MariaDB 11虽然兼容，但有些特定配置可能不同）

   ```ini
   # my.cnf
   [mysqld]
   default-authentication-plugin=mysql_native_password  # MySQL 8.0需要此配置以支持旧密码认证
   # 其他配置...
   ```
7. **将poetry_old.sql覆盖poetry.sql（poetize-server/sql/）**

   ```bash
   rm -f poetry.sql
   # 使用旧脚本（数据表一样，但mysql只能使用InnoDB存储引擎）
   cp poetry_old.sql poetry.sql
   ```

这些修改完成后，系统将使用MySQL而非MariaDB作为数据库引擎。注意，MariaDB对MySQL的某些语法有扩展，如果您的SQL使用了这些扩展特性，切换时可能需要调整。

### 配置说明

* **docker-compose.yml** - 服务编排与环境变量
* **nginx/\*.conf** - 反向代理与负载均衡
* **mysql/conf/my.cnf** - 数据库引擎设置
* **应用配置** - Java和Python各自配置文件
* **敏感数据** - 密码、密钥不应提交，使用.gitignore或环境变量

### 部署方式

本地开发完成后，修改代码并重建Docker镜像，使用 `deploy.sh` 脚本进行自动化部署。

### 开发排障指南

#### 前端常见问题

**1. npm install 依赖安装失败**

```bash
# 问题：依赖冲突或版本不兼容
# 解决：使用 --legacy-peer-deps 参数
npm install --legacy-peer-deps

# 或清除缓存重试
npm cache clean --force
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

**2. 前端 docker 部署启动后 API 请求失败（跨域/404）**

```bash
# 检查 constant.js 中的 API 地址配置
# poetize-ui/src/utils/constant.js

# 确保后端服务已启动
docker ps | grep poetize-java
docker ps | grep poetize-python

# 检查服务内存占用（检查后端服务或者数据库是否因资源不足导致异常）
docker stats
```

**3. WebSocket 连接失败（聊天室无法连接）**

```bash
# 检查 WebSocket 地址配置
# 确保 imBaseURL 配置正确
# 本地开发: ws://localhost:8081
# 生产环境: wss://你的域名

# 检查 Nginx WebSocket 代理配置
docker exec poetize-nginx cat /etc/nginx/conf.d/default.conf | grep -A 5 "websocket"
```

#### Java后端常见问题

**1. Maven 依赖下载缓慢或失败**

```bash
# 检查 settings.xml 镜像源配置
# 已配置华为云镜像，如仍失败可尝试阿里云镜像

# 清除 Maven 本地仓库重新下载
rm -rf ~/.m2/repository
mvn clean install
```

**2. Spring Boot 启动失败**

```bash
# 检查 JDK 版本（必须是 JDK 25，或兼容 JDK 21+）
java -version

# 检查数据库连接
# 查看 application.yml 中数据库配置是否正确
# 确保数据库服务已启动
docker ps | grep mysql

# 查看详细错误日志
tail -f poetry-web/target/logs/spring.log
```

**3. 数据库连接密码错误**

部署脚本会自动生成随机数据库密码，密码保存在 `.config/db_credentials.txt` 文件中。

```bash
# 查看数据库密码
cat .config/db_credentials.txt

# 使用正确的密码连接数据库
# 假设 ROOT 密码为 abc123xyz（实际以文件中为准）
docker exec -it poetize-mariadb mariadb -uroot -p
# 提示输入密码时，粘贴从文件中获取的 ROOT 密码

# 或使用 poetize 用户连接
docker exec -it poetize-mariadb mariadb -upoetize -p poetize
# 输入文件中的 poetize 用户密码
```

**4. 数据库初始化失败或表不存在**

```bash
# 先获取数据库密码
ROOT_PWD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')

# 检查是否已执行初始化脚本
docker exec -i poetize-mariadb mariadb -uroot -p${ROOT_PWD} -e "USE poetize; SHOW TABLES;"

# 重新初始化数据库
docker exec -i poetize-mariadb mariadb -uroot -p${ROOT_PWD} poetize < poetize-server/sql/poetry.sql

# 如果 RocksDB 引擎报错，使用兼容性脚本
docker exec -i poetize-mariadb mariadb -uroot -p${ROOT_PWD} poetize < poetize-server/sql/poetry_old.sql
```

#### Python服务常见问题

**1. Python 依赖安装失败**

```bash
# 升级 pip
pip install --upgrade pip

# 使用国内镜像源
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 如遇特定包失败，单独安装
pip install 包名 --no-deps
```

**2. FastAPI 服务启动端口冲突**

```bash
# 检查端口占用
lsof -i :5000  # Linux/macOS
netstat -ano | findstr :5000  # Windows

# 修改启动端口
uvicorn main:app --port 5001 --reload
```

**3. 第三方登录 OAuth 回调失败**

```bash
# 检查配置文件
cat py/data/oauth_config.json

# 确保回调地址配置正确
# 本地开发: http://localhost:5000/callback/{provider}
# 生产环境: https://你的域名/callback/{provider}

# 检查 OAuth 应用配置的回调地址是否一致
```

#### Docker 环境问题

**1. Docker 容器无法启动**

```bash
# 查看容器状态
docker ps -a

# 查看容器日志
docker logs poetize-java
docker logs poetize-python
docker logs poetize-mariadb

# 重启特定容器
docker restart poetize-java
```

**2. 容器启动后立即退出**

```bash
# 查看退出原因
docker logs --tail 50 容器名

# 检查资源限制（内存不足）
docker stats

# 检查配置文件语法
docker compose config
```

**3. 数据库容器健康检查失败**

```bash
# 获取数据库密码
ROOT_PWD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')

# 手动检查数据库连接
docker exec -it poetize-mariadb mariadb -uroot -p${ROOT_PWD}

# 检查存储引擎插件
docker exec -it poetize-mariadb mariadb -uroot -p${ROOT_PWD} -e "SHOW ENGINES;"

# 查看数据库错误日志
docker logs poetize-mariadb
```

#### 网络与访问问题

**1. 无法访问后台管理页面**

```bash
# 检查 Nginx 配置
docker exec poetize-nginx nginx -t

# 重启 Nginx
docker restart poetize-nginx

# 检查路由配置
# 后台地址: http://域名/admin
```

**2. 静态资源 404**

```bash
# 检查前端构建产物
ls poetize-ui/dist/
ls poetize-im-ui/dist/

# 检查 Nginx 静态文件映射
docker exec poetize-nginx cat /etc/nginx/conf.d/default.conf | grep "location"
```

**3. HTTPS 证书问题**

```bash
# 重新申请证书
docker exec poetize-nginx /enable-https.sh

# 检查证书有效期
docker exec poetize-nginx certbot certificates

# 查看证书续期日志
docker logs poetize-nginx | grep certbot
```

#### 性能与调试

**1. 接口响应慢**

```bash
# 检查 Redis 缓存状态
docker exec -it poetize-redis redis-cli
> INFO stats
> DBSIZE

# 查看 Java 应用 JVM 状态
docker exec poetize-java jstack 1

# 检查数据库慢查询
ROOT_PWD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
docker exec -it poetize-mariadb mariadb -uroot -p${ROOT_PWD} -e "SHOW FULL PROCESSLIST;"
```

**2. 查看实时日志**

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker logs -f poetize-java
docker logs -f poetize-python

# 查看最近 100 行日志
docker logs --tail 100 poetize-java
```

**3. 内存占用过高**

```bash
# 查看容器资源使用
docker stats

# 调整 Java 堆内存（修改 docker-compose.yml）
JAVA_OPTS: "-Xms512m -Xmx1g"

# 重启服务生效
docker compose restart poetize-java
```

#### 常用调试命令

```bash
# 进入容器内部调试
docker exec -it poetize-java sh
docker exec -it poetize-python bash
docker exec -it poetize-mariadb bash

# 检查网络连通性
docker exec poetize-java ping mysql
docker exec poetize-python curl http://poetize-java:8081/actuator/health

# 导出容器配置
docker inspect poetize-java > java-config.json

# 备份数据库
ROOT_PWD=$(grep "数据库ROOT密码:" .config/db_credentials.txt | cut -d':' -f2 | tr -d ' ')
docker exec poetize-mariadb mysqldump -uroot -p${ROOT_PWD} poetize > backup.sql
```

## 🛠️ 技术栈

* **前端** - Vue2/Vue3、Element UI、Socket.io、Live2D
* **后端** - Spring Boot 3.5.5、Java 25、FastAPI、Python
* **数据库** - MariaDB 11、Redis
* **部署** - Docker、Docker Compose、Nginx、Shell脚本

## 📧 联系方式

* **邮箱** - enable_lazy@qq.com 或 hi@leapya.com
* **问题反馈** - [GitHub Issues](https://github.com/LeapYa/Awesome-poetize-open/issues)

所有项目贡献者信息请参阅[贡献者](#-贡献与许可)部分。

## 📜 版权说明

本项目遵循GNU Affero General Public License v3.0 (AGPL-3.0)开源许可协议，详情请参阅[LICENSE](LICENSE)文件。
