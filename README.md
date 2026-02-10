<!--
  POETIZE 最美博客系统 - LeapYa维护版
  
  Keywords: 博客系统, blog system, 个人博客, personal blog, Vue3, Spring Boot, 
  Java, Docker, 一键部署, 开源博客, open source blog, 聊天室, chatroom, 
  Live2D, SEO优化, 自托管, self-hosted, MariaDB, Redis, WebSocket, 
  OAuth2登录, 第三方登录, 看板娘, 最美博客, poetize, 表白墙, 相册, 
  markdown博客, 博客模板, blog template, docker-compose
  
  GitHub: https://github.com/LeapYa/Awesome-poetize-open
  Demo: https://leapya.com
-->

<p align="center">
  <a href="#">
    <img src="poetize_picture/logo.png" alt="POETIZE Open Fork Logo" width="180px">
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
   <img src="https://img.shields.io/github/license/LeapYa/Awesome-poetize-open" alt="License">
   <img src="https://img.shields.io/badge/Vue.js-35495e.svg?logo=vue.js&logoColor=4FC08D" alt="Vue.js">
   <img src="https://img.shields.io/badge/Java-%23ED8B00.svg?logo=openjdk&logoColor=white" alt="Java">
   <img src="https://img.shields.io/badge/Spring-6DB33F.svg?logo=spring&logoColor=white" alt="Spring">
   <img src="https://img.shields.io/badge/Python-14354C.svg?logo=python&logoColor=white" alt="Python">
   <img src="https://img.shields.io/badge/MariaDB-003545?logo=mariadb&logoColor=white" alt="MariaDB">
   <img src="https://img.shields.io/badge/Redis-DC382D.svg?logo=redis&logoColor=white" alt="Redis">
   <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker">
   <img src="https://img.shields.io/github/stars/LeapYa/Awesome-poetize-open?style=social" alt="GitHub Stars">
  </p>
</p>

## 📑 目录

- [项目简介](#-项目简介)
- [快速开始](#-快速开始)
- [部署文档](#-部署文档)
- [贡献与许可](#-贡献与许可)
- [开发指南](#-开发指南)
- [排障指南](#-排障指南)
- [技术栈](#️-技术栈)
- [联系方式](#-联系方式)
- [版权说明](#-版权说明)

## 📖 项目简介

本项目**Awesome-poetize-open**是基于开源项目 [POETIZE最美博客](https://gitee.com/littledokey/poetize) 功能扩展和定制化开发，改造历时一年，这是一个集内容创作、社交互动与技术优化于一体的现代化博客系统。本项目主要用于自用，可能有些地方比不上商业化的最美博客，我当初也是被它的颜值吸引，想着自己也改一个自己满意的分享出来，我每个月都会进行不定时更新，如果你有什么好的想法或者功能建议，欢迎提issue或pr。

<p align="center">
  <img src="poetize_picture/首页.png" alt="首页" width="100%">
</p>

<p align="center">博客首页 - 展示个人创作与生活点滴</p>

<p align="center">
  <img src="poetize_picture/首页1.jpg" alt="文章展示" width="49%">
  <img src="poetize_picture/聊天室.png" alt="社交功能" width="49%">
</p>

<p align="center">左：内容布局展示 | 右：社交功能体验</p>

#### **本分支相较于开源版最美博客新增/优化功能：**

1. ✅ 一键部署脚本 —— 一行命令自动完成环境配置、HTTPS配置和服务启动
1. ✅ 专业SEO优化与预渲染 —— 预渲染静态HTML、结构化数据、多语言hreflang、社交分享元数据、Sitemap与搜索引擎推送，全方位提升搜索引擎收录
1. ✅ 多邮箱服务支持 —— 可配置多邮箱，提升邮件送达率
1. ✅ 第三方登录集成 —— 支持GitHub、Google、Twitter、Yandex、Gitee等平台登录
1. ✅ 行为验证码功能 —— 集成点选、滑动验证码，防止恶意注册
1. ✅ 看板娘优化 —— Live2D看板娘可自定义、支持AI互动
1. ✅ 导航栏优化 —— 支持自定义导航栏，可以自定义隐藏任何导航栏项，布局更美观
1. ✅ 评论体验优化 —— 评论内容自动保存，未登录也不丢失
1. ✅ 增加兰空图床、简单图床的存储支持 —— 支持多种图片上传和存储方式
1. ✅ AI翻译 —— 支持中英互译，可用本地或API模型
1. ✅ 页脚优化 —— 页脚信息更丰富、可自定义
1. ✅ 图片压缩和转换WebP格式 —— 自动压缩图片，提升网站加载速度
1. ✅ 智能摘要 —— 自动生成文章摘要，提升阅读体验
1. ✅ 暗色模式优化、定时暗色模式 —— 支持夜间自动切换暗色主题，优化暗色模式
1. ✅ 灰色模式 —— 支持全站灰色纪念模式
1. ✅ 自定义错误页面 —— 提供友好的404、403等错误页面
1. ✅ 字体文件CDN化 —— 支持字体文件外部化存储与动态加载，可配置单一/分块字体模式，自定义Unicode范围，大幅减少网站带宽占用
1. ✅ 将MD5密码哈希升级为BCrypt算法 —— 修复密码安全漏洞
1. ✅ 评论区重构、优化其楼层计算算法优化 —— 对评论区进行重构，引入懒加载机制以提升页面加载速度，并使用深度优先遍历算法优化评论楼层计算逻辑，提高渲染性能
1. ✅ 实现token签名算法HMAC-SHA256认证 —— 完全替换简单UUID token，新增防伪造、防篡改、防重放攻击能力
1. ✅ 彻底重构 IM 系统 —— 移除 t-io 依赖，迁移至原生 Spring WebSocket，降低架构复杂度和维护成本，配合Java 25 虚拟线程，性能也能媲美 t-io
1. ✅ 支持账号多端登录 —— 实现多设备同时在线登录，并加强了账号安全，改密码后强制下线所有设备
1. ✅ 核心技术栈升级 —— 主站前端升级到 Vue3.5，后端升级到 Java 25 LTS + Spring Boot 3.5，全面启用虚拟线程，并使用 Redis缓存，性能与响应速度显著提升

> 更多功能，就不一一列举了...

## 🚀 快速开始

```bash
# 你只需要输入域名邮箱即可
bash <(curl -sL install.leapya.com)
```

脚本将自动完成所有配置，包括Docker安装、数据库初始化和HTTPS配置。

## 📋 部署文档

### 1.准备服务器、域名、域名解析

#### 1.1 准备服务器

选择可靠的云服务商即可，根据价格和需求自行决定，常用平台：

- [阿里云](https://www.aliyun.com/)、[腾讯云](https://www.tencent.com/)、[华为云](https://www.huaweicloud.com/)、[UCloud](https://www.ucloud.cn/)、[雨云](https://www.rainyun.com/) - 国内主流
- [AWS](https://aws.amazon.com/)、[DigitalOcean](https://www.digitalocean.com/)、[Linode](https://www.linode.com/)、[Heroku](https://www.heroku.com/)、[Vultr](https://www.vultr.com/) - 海外主流

**配置要求：**

- **操作系统**：Ubuntu 20.04+、Debian 11+ 或 CentOS 7/8+（[更多系统测试结果](#14-系统兼容性测试结果)）
- **CPU/内存**：2核2GB起步（1核1GB可运行但较卡，2核2GB足够了）
- **硬盘空间**：15GB+
- **带宽**：建议5Mbps+（1Mbps≈128KB/s，一个普通页面至少200KB）

**地域与线路选择：**

| 地域 | 优点 | 注意事项 |
|------|------|---------|
| 国内 | 访问速度最快，国内搜索引擎收录更友好 | 需要ICP备案（约3-7个工作日） |
| 香港/海外 | 免备案，即买即用 | 国内访问可能稍慢 |

> **避坑提醒：**
> - **香港/海外服务器**：优先选择**CN2线路**等有回国优化的线路，避免无线路优化，否则国内访问会很慢
> - **国内服务器**：大部分云服务商的国内服务器没有海外出口线路，海外用户访问会很慢甚至无法访问，一定要问清楚再购买，优先选择**BGP多线**，避免单线路（如仅电信/联通），否则国内跨运营商访问会很慢
> - 如果需要备案，云服务器至少需要购买3个月以上的使用时长
> - 记得在安全组/防火墙中 **开放80和443端口**
> - 内存较低时部署脚本会自动开启swap交换空间进行优化

#### 1.2 购买域名

在任意域名注册商购买域名即可，常用平台：

- [阿里云万网](https://wanwang.aliyun.com/)、[腾讯云DNSPod](https://dnspod.cloud.tencent.com/)、[华为云](https://www.huaweicloud.com/product/domain.html) - 国内主流
- [Cloudflare](https://www.cloudflare.com/products/registrar/)、[Namesilo](https://www.namesilo.com/)、[Namecheap](https://www.namecheap.com/) - 海外注册商

> 如果使用国内服务器，域名需要完成ICP备案才能正常访问，如果需要备案，域名不要选择 **.tk**、**.ml**、**.ga**、**.cf** 等后缀，这些后缀的域名无法备案，选择 **.com**、**.net**、**.org**、**.top**、**.xyz**、**.cn**、**.online** 等后缀的域名，备案流程可参考[ICP备案](https://help.aliyun.com/zh/icp-filing/basic-icp-service/user-guide/icp-filing-application-overview)，其他云服务商也类似，都是向各自的购买云服务器的云服务商申请备案。

#### 1.3 配置域名解析

购买域名后，需要将域名解析到服务器IP：

1. 登录域名注册商的DNS管理控制台
2. 添加以下解析记录：

| 记录类型 | 主机记录 | 记录值 | TTL |
|---------|---------|--------|-----|
| A | @ | 你的服务器IP | 600 |
| A | www | 你的服务器IP | 600 |

> **说明**：`@` 表示根域名（如 `example.com`），`www` 表示带www前缀（如 `www.example.com`）。解析生效通常需要几分钟到几小时不等。

#### 1.4 系统兼容性测试结果

| 操作系统类型          | CPU  | 内存 | 存储 | 测试结果  |
| --------------------- | ---- | ---- | ---- | --------- |
| Ubuntu 20.04+ x64     | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| Debian 11+ x64        | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| CentOS 7/8+ x64       | 1核+ | 1G+  | 30GB | ✅ 推荐   |
| Windows Server/桌面版 | 2核+ | 2G+  | 30GB | ✅ 使用wsl   |

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

> 首次登录后进入"网站设置"完成基础信息配置（网站名称、头像、背景、发件邮箱等），然后到个人中心配置自己的信息和邮箱，之后退出登录，点击登录页面的"忘记密码"修改默认密码。

### 4.可选配置

#### （1）SEO优化配置

SEO（搜索引擎优化）可以让你的博客更容易被 Google、百度等搜索引擎收录，让更多人通过搜索找到你的文章。

本项目内置了完整的 SEO 功能，包括智能摘要生成、站点地图、搜索引擎推送等。如果你希望网站被更多人发现，建议阅读 [SEO优化指南](docs/SEO优化指南.md)。

#### （2）更换字体

  **默认字体说明**

  本项目默认使用「**萌趣体**」字体（来源：[字体视界](https://www.17font.com/)），该字体风格圆润可爱，非常适合个人博客的温馨氛围。

  > **注意**：萌趣体是**单字重字体**，不支持粗体变体。这意味着 Markdown 中的 `**粗体**` 语法虽然会被正确解析，但视觉上加粗效果不明显。如需明显的粗体效果，建议更换为支持多字重的字体。

  **推荐字体**

  如需更换字体，以下是一些支持**多字重**的免费商用字体推荐：

  | 字体名称 | 风格特点 | 字重数量 | 下载地址 |
  | :--- | :--- | :--- | :--- |
  | HarmonyOS Sans | 圆润现代 | 6种 | [官网下载](https://developer.huawei.com/consumer/cn/design/resource/) |
  | MiSans | 圆润友好 | 6种 | [官网下载](https://hyperos.mi.com/font) |
  | OPlusSans | 圆角柔和 | 5种 | [GitHub](https://github.com/nicemicrosun/OPPO_Sans) |
  | 思源黑体 | 专业现代 | 7种 | [GitHub](https://github.com/adobe-fonts/source-han-sans) |
  | 思源宋体 | 优雅衬线 | 7种 | [GitHub](https://github.com/adobe-fonts/source-han-serif) |

  **更换方法**

  如需更换网站字体，提供两种方法：

  **方法1：分块字体模式**

  1. 将新字体文件（TTF格式）放入 `split_font/` 文件夹
  2. 重命名为 `font.ttf`
  3. 安装依赖：`pip install -r requirements.txt`
  4. 执行：`python font_subset.py`
  5. 将生成的 `font_chunks` 目录复制到：
      - `poetize-web/public/assets/`
      - `poetize-web/public/static/assets/`
  6. 重启前端服务

  **方法2：单一字体模式**

  1. 在后台管理 → 配置管理中，设置"使用单一字体文件"为 `true`
  2. 将新字体文件（WOFF2格式）重命名为 `font.woff2`
  3. 复制到：
      - `poetize-web/public/assets/`
      - `poetize-web/public/static/assets/`
  4. 重启前端服务

#### （3）OAuth代理

若需支持国外第三方登录平台（GitHub、Google等），请配置海外代理服务器，详见[OAuth代理配置说明文档](docs/OAuth代理配置说明.md)。

#### （4）Ollama本地翻译模型

如需启用本地AI翻译功能，编辑 `docker-compose.yml` 找到"Ollama翻译模型服务"部分取消注释即可。默认使用 `qwen3:0.6b` 轻量级模型。更多模型选择和配置详见 [Ollama官方模型库](https://ollama.com/library)。

#### （5）使用外部数据库或Redis

如果你已有 MariaDB/MySQL 或 Redis 服务，可在部署时直接使用，无需再启动内置容器：

```bash
# 使用外部数据库
bash <(curl -sL install.leapya.com) -d 域名.com --db-host 数据库地址 --db-pwd 密码

# 使用外部Redis
bash <(curl -sL install.leapya.com) -d 域名.com --redis-host Redis地址 --redis-pwd 密码

# 完整参数（可按需省略,也可以使用 -h 查看完整参数）
--db-host HOST       # 数据库主机地址
--db-port PORT       # 端口（默认3306）
--db-name NAME       # 数据库名（默认poetize）
--db-user USER       # 用户名（默认poetize）
--db-pwd PASSWORD    # 密码
--redis-host HOST    # Redis主机地址
--redis-port PORT    # 端口（默认6379）
--redis-pwd PASSWORD # 密码
--redis-db NUMBER    # 数据库编号（默认0）
```

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
sudo docker system prune -af && rm -rf Awesome-poetize-open && bash <(curl -sL install.leapya.com)
```

更多详见[排障指南](#-排障指南)

### 7.高级功能

本项目提供三个管理脚本，使用 `poetize -h`、`./deploy.sh -h` 或 `./migrate.sh` 查看详细用法。

#### 国内环境部署

`deploy.sh` 脚本已内置国内镜像源加速。~~若网络受限，可从Release下载离线资源包，包含Docker安装包和所有镜像文件。~~

## 🤝 贡献与许可

* 原作者：Sara (POETIZE最美博客)
* Fork版本开发：LeapYa
* 开源协议：遵循原项目AGPL协议

### 贡献者

感谢所有为本项目做出贡献的人！

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<table>
  <tr>
    <td align="center">
      <a href="https://github.com/mikutea">
        <img src="https://github.com/mikutea.png" width="100px;" alt="mikutea"/>
        <br />
        <sub><b>mikutea</b></sub>
      </a>
      <br />
      <a href="https://github.com/LeapYa/Awesome-poetize-open/issues?q=author%3Amikutea+label%3Aenhancement" title="Ideas & suggestions">🤔</a>
      <a href="https://github.com/LeapYa/Awesome-poetize-open/issues?q=author%3Amikutea+label%3Abug" title="Bug reports">🐛</a>
    </td>
  </tr>
</table>
<!-- ALL-CONTRIBUTORS-LIST:END -->

## 💻 开发指南

详细的开发环境配置、项目结构说明和各模块开发指南，请参阅 **[开发指南文档](docs/开发指南.md)**。

包含内容：
- 环境要求（Node.js、JDK、Maven、Python、Docker）
- 项目目录结构
- 前端开发（poetize-web、poetize-admin）
- Java 后端开发
- Python 后端开发
- 数据库配置（MariaDB/MySQL）
- 从 MariaDB 切换到 MySQL 的步骤

其他文档：
- **[SEO优化指南](docs/SEO优化指南.md)** - 发布策略、收录技巧、技术配置
- **[数据库设计文档](docs/数据库设计.md)** - 表结构、字段说明、ER图
- **[架构设计文档](docs/架构设计.md)** - 系统架构、技术栈、部署架构

### 快速开发启动

**环境准备**：JDK 25 LTS、Node.js 14+、Maven 3.9+、MariaDB + Redis（详见 [开发指南](docs/开发指南.md#数据库环境准备)）

```bash
# 1. 启动后端
cd poetize-server && mvn spring-boot:run

# 2. 启动前端（另开终端）
cd poetize-web && npm install && npm run dev

# 3. 启动后台管理系统前端（另开终端）
cd poetize-admin && npm install && npm run dev
```

访问地址：
- 前台：`http://localhost:5173`
- 后台：`http://localhost:5174/admin`


## 🔧 排障指南

遇到问题？请参阅 **[排障指南文档](docs/排障指南.md)**。

包含常见问题解决方案：
- 前端问题（npm 安装失败、API 请求失败、WebSocket 连接失败）
- Java 后端问题（Maven 依赖、Spring Boot 启动、数据库连接）
- Python 服务问题（依赖安装、端口冲突、OAuth 回调）
- Docker 环境问题（容器启动、健康检查、日志查看）
- 网络与访问问题（HTTPS 证书、静态资源 404）
- 性能调试命令

## 🛠️ 技术栈

* **前端** - Vue3（前台+聊天室）、Vue2（后台管理）、Element Plus/Element UI、WebSocket、Live2D
* **后端** - Spring Boot 3.5.5、Java 25 LTS、FastAPI、Python 3.9+
* **数据库** - MariaDB 11、Redis 7
* **部署** - Docker、Docker Compose、OpenResty（Nginx）、Shell 脚本

## 📧 联系方式

* **邮箱** - enable_lazy@qq.com 或 hi@leapya.com
* **问题反馈** - [GitHub Issues](https://github.com/LeapYa/Awesome-poetize-open/issues)

所有项目贡献者信息请参阅[贡献者](#-贡献与许可)部分。

## 📜 版权说明

本项目遵循GNU Affero General Public License v3.0 (AGPL-3.0)开源许可协议，详情请参阅[LICENSE](LICENSE)文件。
