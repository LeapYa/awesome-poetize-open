# OAuth代理配置说明

为了解决国内服务器无法访问Google、GitHub、X(Twitter)、Yandex等海外OAuth服务的问题，系统提供了两种代理方式：**域名级反向代理**（推荐）和 **HTTP正向代理**。通过境外反向代理服务器来访问这些服务，但需要准备一台海外vps。

## 方案原理与网络封锁类型

### 能解决哪些封锁？

OAuth 登录流程分为两段网络连接，封锁的影响各不相同：

| 阶段 | 连接路径 | 说明 |
|------|---------|------|
| **① 用户授权** | 用户浏览器 → github.com | 用户浏览器直接访问官方授权页 |
| **② 后端API调用** | 国内服务器 → 代理VPS → github.com | Token交换、获取用户信息等 |

**阶段②（后端API调用）** 是本方案解决的核心目标。对各类封锁都有效：

| 封锁类型 | 阶段② 能否解决 | 原理 |
|---------|:-----------:|------|
| **DNS 污染** | ✅ | 国内服务器解析的是代理域名 `auth.example.com`，不涉及被污染的域名 |
| **IP 封锁** | ✅ | 国内服务器连接的是境外VPS的IP，被封的是 github.com 的IP |
| **SNI 阻断** | ✅ | TLS握手时发送的SNI是 `auth.example.com`，GFW看不到 `github.com` |

> [!IMPORTANT]
> **阶段①（用户授权页）** 需要用户浏览器直接访问 `github.com` 等官方域名，无法通过服务端代理解决，需要用户自行解决不能访问官方授权页的问题（如使用科学上网）。

## 代理方式

系统支持两种代理方式，**任选其一**即可：

### 方式一：域名级反向代理（推荐）

通过境外VPS上的Nginx反向代理，将API请求转发到目标服务。

**优点**：无需在国内服务器配置额外软件，只需一个域名和VPS。

```bash
# docker-compose.yml 环境变量
OAUTH_PROXY_DOMAIN=https://auth.example.com
```

### 方式二：HTTP正向代理

通过 HTTP 代理服务器（如 Squid、3proxy 等）转发请求。

**优点**：不需要额外域名，配置简单。

```bash
# docker-compose.yml 环境变量
OAUTH_PROXY_HOST=your-proxy-host
OAUTH_PROXY_PORT=8080
```

> [!NOTE]
> 方式一通过 URL 路径替换实现代理（代码中 `getProxiedUrl()` 方法），方式二通过 Java `RestTemplate` 的 HTTP 代理设置实现（`OAuthRestTemplateConfig` 类）。两者可以同时配置，但建议只用一种。

## 支持的平台

**需要代理的海外平台：**

- GitHub
- Google
- X (Twitter)
- Yandex

**不需要代理的国内平台：**

- Gitee
- QQ
- 百度
- 爱发电

## 海外VPS配置（方式一）

### Nginx配置示例

部署Nginx后，配置如下路径映射：

```nginx
server {
    listen 443 ssl http2;
    server_name auth.example.com;
  
    # SSL配置
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
  
    # 通用代理设置
    proxy_http_version 1.1;
    proxy_ssl_server_name on;
    proxy_set_header Connection "";
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_redirect off;

    # ============================================================================
    # GitHub API代理
    # ============================================================================
  
    # Token交换端点
    location = /github/login/oauth/access_token {
        proxy_set_header Host github.com;
        proxy_pass https://github.com/login/oauth/access_token;
    }
  
    # 用户信息和邮箱API
    location /github/api/ {
        proxy_set_header Host api.github.com;
        proxy_pass https://api.github.com/;
    }

    # ============================================================================
    # Google API代理
    # ============================================================================
  
    # Token交换端点
    location /google/oauth2/ {
        proxy_set_header Host oauth2.googleapis.com;
        proxy_pass https://oauth2.googleapis.com/;
    }
  
    # 用户信息端点（统一使用v2接口）
    location = /google/oauth2/v2/userinfo {
        proxy_set_header Host www.googleapis.com;
        proxy_pass https://www.googleapis.com/oauth2/v2/userinfo;
    }

    # ============================================================================
    # X (Twitter) API代理
    # ============================================================================
  
    # OAuth 1.0和2.0的API端点
    location /x/api/ {
        proxy_set_header Host api.twitter.com;
        proxy_pass https://api.twitter.com/;
    }

    # ============================================================================
    # Yandex API代理
    # ============================================================================
  
    # Token交换端点
    location = /yandex/token {
        proxy_set_header Host oauth.yandex.com;
        proxy_pass https://oauth.yandex.com/token;
    }
  
    # 用户信息端点
    location /yandex/login/ {
        proxy_set_header Host login.yandex.ru;
        proxy_pass https://login.yandex.ru/;
    }
}
```

## HTTP正向代理配置（方式二）

方式二不需要额外域名，在境外VPS上搭建一个HTTP正向代理即可。后端的 `OAuthRestTemplateConfig` 会自动通过该代理发送所有OAuth请求。

### 以 Squid 为例

1. **安装 Squid**：

   ```bash
   # Ubuntu/Debian
   apt install squid

   # CentOS/RHEL
   yum install squid
   ```

2. **配置 `/etc/squid/squid.conf`**：

   ```conf
   # 监听端口
   http_port 8080

   # === ACL 定义 ===
   # 仅允许国内服务器IP访问（替换为你的实际IP）
   acl allowed_servers src YOUR_SERVER_IP/32
   # 允许HTTPS代理（CONNECT方法）
   acl SSL_ports port 443
   acl CONNECT method CONNECT
   # 仅允许代理OAuth相关域名（可选，增强安全性）
   acl oauth_domains dstdomain .github.com .googleapis.com .twitter.com .yandex.com .yandex.ru

   # === 访问规则（按顺序匹配，先匹配先生效） ===
   # 1. 只允许指定IP访问
   http_access deny !allowed_servers
   # 2. 允许CONNECT到443端口（HTTPS代理必需）
   http_access allow CONNECT SSL_ports
   # 3. 允许访问OAuth相关域名
   http_access allow oauth_domains
   # 4. 拒绝其他所有请求
   http_access deny all
   ```

3. **启动服务**：

   ```bash
   systemctl enable squid
   systemctl start squid
   ```

4. **配置环境变量**：

   ```bash
   # docker-compose.yml
   OAUTH_PROXY_HOST=your-vps-ip
   OAUTH_PROXY_PORT=8080
   ```

> [!WARNING]
> HTTP正向代理必须限制访问来源IP，否则可能被滥用为开放代理。务必在 `squid.conf` 中配置 `acl allowed_servers` 或使用防火墙规则限制。

## 工作流程

以 GitHub 为例：

```
用户浏览器                    国内服务器                    境外VPS (auth.example.com)       GitHub
    |                            |                              |                            |
    |-- 1. 点击GitHub登录 ------>|                              |                            |
    |<- 2. 返回授权URL ----------|                              |                            |
    |                            |                              |                            |
    |-- 3. 直接访问授权页 ------------------------------------------------>  授权页面          |
    |<- 4. 用户授权，GitHub回调 --|--------- 回调到国内 -------->|                            |
    |                            |                              |                            |
    |                            |-- 5. Token交换 ------------->|-- 转发 ------------------>|
    |                            |<- 6. 返回access_token -------|<- 返回 -------------------|
    |                            |                              |                            |
    |                            |-- 7. 获取用户信息 ----------->|-- 转发 ------------------>|
    |                            |<- 8. 返回用户信息 ------------|<- 返回 -------------------|
    |                            |                              |                            |
    |<- 9. 登录成功 -------------|                              |                            |
```

- **步骤3**：用户浏览器直接访问 `https://github.com/login/oauth/authorize`（需要用户自行解决网络问题）
- **步骤5-8**：后端通过代理完成，**用户无需任何额外配置**

### 后端代码架构

系统通过两层架构处理 OAuth 代理：

- **Provider层**：`OAuth2Provider`（OAuth 2.0）和 `OAuth1Provider`（OAuth 1.0）基类提供 `getProxiedUrl()` 方法，各平台 Provider（如 `GitHubOAuthProvider`）在 `getTokenUrl()` 和 `getUserInfoUrl()` 中调用
- **兼容层**：`OAuthClientServiceImpl` 中的 `getTokenUrl()` 和 `getUserInfoUrl()` 也独立支持代理URL映射
- **网络层**：`OAuthRestTemplateConfig` 配置 HTTP 正向代理（方式二）

## 注意事项

1. **域名证书**: 境外代理域名需要配置有效的SSL证书
2. **简化配置**: 只需要代理API端点，不需要Cookie重写和HTML处理
3. **回调配置**: OAuth应用的回调URL直接配置为国内域名
4. **网络稳定**: 境外代理服务器需要稳定的网络连接
5. **路径精确**: 确保代理路径与代码中的URL映射完全一致
6. **安全建议**: 建议在Nginx中限制代理域名的访问来源IP，仅允许你的国内服务器访问

## 测试验证

配置完成后，可以通过以下方式测试：

1. **测试后端API代理**:

   ```bash
   # 方式一：域名级反代
   curl https://auth.example.com/github/api/user
   curl https://auth.example.com/google/oauth2/token

   # 方式二：HTTP正向代理（从国内服务器上测试）
   curl -x http://your-proxy-host:8080 https://api.github.com/user
   ```
2. **测试完整OAuth流程**:

   - 访问你的登录页面，点击GitHub/Google登录
   - 用户浏览器会直接跳转到官方授权页面
   - 授权后回调到你的国内服务器
   - 检查后端日志，确认token和用户信息获取成功

## 故障排除

| 问题 | 可能原因 | 排查方法 |
|------|---------|---------|
| 用户无法访问授权页 | 用户网络被封锁（DNS/SNI/IP） | 用户需自行解决（VPN等），这不是服务端能解决的 |
| Token获取失败 | 代理路径配置错误或VPS不通 | 检查 `OAUTH_PROXY_DOMAIN` 配置和Nginx路径映射 |
| 用户信息获取失败 | 用户信息API代理路径不正确 | 用 `curl` 直接测试代理端点是否可访问 |
| 代理连接超时 | VPS网络不稳定或SSL证书问题 | 检查VPS连通性和证书有效期 |
| 配置了代理但未生效 | 环境变量未正确传递 | 检查后端启动日志中的 `OAuth RestTemplate配置完成` 输出 |
