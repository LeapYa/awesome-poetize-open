# nginx CVE 补丁（针对 OpenResty 1.29.2.5）

基础镜像固定在 `openresty/openresty:1.29.2.5-alpine`，原因写在上一级 `Dockerfile` 头部。
1.29.x 分支已停止 backport，后续 nginx 安全修复不会再由上游提供，所以本目录存放我们自行回补的补丁。

## 判定原则

**只对本项目配置下真实可达的 CVE 打补丁。** 判定依据两条：

1. nginx 官方 security advisories 里该 CVE 的 Vulnerable 区间**包含 1.29.2**；
2. 本项目 `nginx.conf` / `default.http.conf.template` / `default.https.conf.template` 里
   确实启用了受影响模块所需的**前置条件**。

两条缺一，就不打。理由：每多一条补丁就多一份和上游分叉的成本，而这个分叉要在每次基础镜像变动时重新验证。

## 现状：从 1.29.2 到今天的全量审计

**先纠正一个容易误判的事实**：`openresty/openresty:1.29.2.5-alpine` 的发布日期是 2026-05-25，
但它内部的 nginx 内核是 **1.29.2（2025-10-07）**。所以要统计的不是 5 月到 11 月，
而是 2025-10 到 2026-11 这一年多里 nginx 修了什么，再减去 OpenResty 自己 backport 过的。

基线：nginx 1.29.2 = 2025-10-07；OpenResty 1.29.2.5 = 2026-05-25（backport 到 nginx 1.31.1 为止的修复）。

### 一、OpenResty 已经替我们回补的（无需处理，共 13 条）

| nginx 修复版本 | 日期 | CVE | OpenResty 回补于 |
|---|---|---|---|
| 1.29.5 | 2026-02-04 | CVE-2026-1642（SSL upstream injection） | 1.29.2.3 |
| 1.29.7 | 2026-03-24 | CVE-2026-27654（dav 溢出）、27784 / 32647（mp4 溢出）、27651（CRAM-MD5 空指针）、28753（auth_http 注入）、28755（stream OCSP 绕过） | 1.29.2.3 |
| 1.31.0 | 2026-05-13 | CVE-2026-42945（rewrite 堆溢出，CVSS 9.2）、42946（scgi/uwsgi 越界读）、42934（charset 越界读）、40460（HTTP/3 地址伪造）、40701（OCSP resolver UAF） | 1.29.2.4 |
| 1.31.1 | 2026-05-22 | CVE-2026-9256（rewrite 缓冲区溢出） | 1.29.2.5 |

### 二、1.29.2 受影响且上游**没有**回补的（共 10 条，逐条判定）

| nginx 修复版本 | 日期 | CVE | 严重度 | 本项目是否可达 | 处理 |
|---|---|---|---|---|---|
| 1.31.2 | 2026-06-17 | CVE-2026-42055 | medium | 否：无 `grpc_pass`、无 `proxy_protocol` | 不打 |
| 1.31.2 | 2026-06-17 | CVE-2026-48142 | low | 否：无 `charset_map`（修复只是该指令的配置期校验） | 不打 |
| 1.31.3 | 2026-07-15 | **CVE-2026-42533** | **major** | 否：`map` 带正则 **0** 处 | 不打 |
| 1.31.3 | 2026-07-15 | CVE-2026-56434 | medium | 否：`ssi on` **0** 处 | 不打 |
| 1.31.3 | 2026-07-15 | CVE-2026-60005 | medium | 否：`slice` 指令 **0** 处 | 不打 |
| 1.31.6 | 2026-09-15 | CVE-2026-90439 | medium | 否，见下节 | 不适用 |
| 1.31.7 | 2026-10-14 | CVE-2026-15176 | medium | 否：16 个 alias location，**0** 个同时含 rewrite | 不打 |
| 1.31.7 | 2026-10-14 | **CVE-2026-95101** | medium | **是**：HTTPS 模板 `http3 on` | **已打补丁** |
| 1.31.7 | 2026-10-14 | CVE-2026-97629 | medium | 否：`ssl_session_tickets off` 已显式关闭 | 不打 |
| 1.31.7 | 2026-10-14 | CVE-2026-97205 | medium | 否：`aio` 指令 **0** 处 | 不打 |

### 三、1.29.2 根本不在影响区间的（3 条，此前故障报告误列入）

| CVE | 实际影响范围 | 为什么 1.29.2 不受影响 |
|---|---|---|
| CVE-2026-93479 | 1.30.0–1.30.5、1.31.2–1.31.6 | 1.29.2 早于引入该功能的 1.30.0 |
| CVE-2026-42530 | 1.31.0–1.31.1 | 1.29.2 没有对应代码 |
| CVE-2026-42926 | 1.29.4–1.30.0 | 1.29.2 早于 1.29.4 |

### 判定所依据的配置实测（三个配置文件合计，2026-11-11）

```
map 带正则 (~ 或 *~)                  0
ssi on                               0
slice 指令                            0
aio 指令                              0
grpc_pass                            0
proxy_protocol                       0
charset_map                          0
ssl_session_tickets off              1     ← 已关闭
ssl_session_tickets on               0
http3 on                             1     ← 仅 HTTPS 模板
listen ... quic                      1     ← 仅 HTTPS 模板
alias location 总数                  16
其中同时含 rewrite                    0
```

## 结论一句话

命中面只有一条：**CVE-2026-95101**，而且只在自建 HTTPS 并开启 HTTP/3 的部署形态下可达
（CDN 回源、nginx 不做 TLS 的场景不触碰）。已打补丁，其余 9 条全部不可达，
另 13 条由 OpenResty 上游回补。

**注意**：这个结论绑定当前配置。若将来引入 `map` + 正则、开启 `ssi` / `slice` / `aio`、
改用 gRPC 上游，或把 `ssl_session_tickets` 打开，对应的 CVE 就会变成可达，必须重新评估。

## 为什么 CVE-2026-90439 不适用

1.30.4 → 1.30.5 针对它的修复落在 `src/event/quic/ngx_event_quic_openssl_compat.c`，
该文件仅在 OpenSSL < 3.5（`NGX_QUIC_OPENSSL_COMPAT`）时参与编译。基础镜像链接的是
OpenSSL 3.5.6，走原生 QUIC API，**受影响的代码根本不在二进制里**。

复核办法（在补丁构建阶段执行）：

```bash
grep -c NGX_QUIC_OPENSSL_COMPAT objs/Makefile   # 期望 0
```

## 构建阶段怎么用这些补丁

`Dockerfile` 的 `nginx-patch` 阶段：

1. 下载官方源码包 `openresty-1.29.2.5.tar.gz`（含 `bundle/nginx-1.29.2` 与全部 18 个 add-module）
2. 在 `bundle/nginx-1.29.2` 内依次 `patch -p1` 应用本目录的 `*.patch`
3. 用与基础镜像 `openresty -V` **逐项一致**的 configure 参数重编 nginx（`--with-compat` 保证
   与镜像内已有动态模块签名兼容）
4. 只把 `objs/nginx` 覆盖进最终镜像的 `/usr/local/openresty/nginx/sbin/nginx`

## 目录里除了补丁还有什么

`lua-default-path.snippet` —— 不是 CVE 补丁，是编译期必需的一段拼接：OpenResty 官方
`./configure` 会往 `ngx_lua/config` 末尾追加一段脚本，生成 `LUA_DEFAULT_PATH` /
`LUA_DEFAULT_CPATH`，也就是 `nginx.conf` 里 `lua_package_path "...;;"` 中 `;;` 展开的内容。
我们直接调 nginx 的 configure（而非 openresty 的），少了它会导致
`require "resty.redis"` / `require "cjson"` 全部找不到——站点看似正常，但封禁、真实 IP
解析这些 Lua 链路会成片失效。构建阶段会把它 `cat` 到 `ngx_lua/config` 末尾。

## 新增补丁的规矩

1. 先在 nginx 官方 advisories 确认 1.29.2 落在 Vulnerable 区间内；
2. 在本项目配置里找出真实前置条件，找不到就不打；
3. 补丁**从上游源码 diff 原样生成，不要手写**——优先挑 1.29.2 与修复版本之间该文件逐字节相同的，
   这样应用零风险；文件已分叉的需要单独评估并写进本文件；
4. 提交前先 `patch -p1 --dry-run` 验证；
5. 构建后必须核对三件事：`nginx -V` 与基础镜像一致、`nginx -t` 通过、Lua 与动态模块可正常加载。

## Brotli 模块必须用 OpenResty 源码编译

2026-11-12 定稿。这一节记录一个曾经导致全站不可访问、排查了一整天的构建缺陷。

**结论：动态模块必须用 OpenResty 源码包里的 `bundle/nginx-<ver>` 编译，不能用 nginx.org 的官方源码。**

OpenResty 对 nginx 打了补丁，1.31 这一代的补丁改变了 `ngx_http_request_t` 的尾部布局。
`--with-compat` 只放宽模块签名校验，**不解决结构体 ABI 差异**，所以模块能正常加载、
平时也看不出异常，直到访问到被移动的位域字段。

### 症状

`sendfile on` + `brotli on`，任何带 `Accept-Encoding: br` 的静态文件请求都返回空响应，
`error.log` 里是 `worker process exited on signal 11`。直连源站且不带 br 时完全正常，
所以只看功能测试很容易漏掉。

### 先后顺序（错了会白查很久）

brotli 的 body filter 拿到 `in_file` 的 buf、`pos` 为 NULL，而 `ngx_buf_size()` 对
非内存 buf 走 `file_last - file_pos` 分支，返回非零长度，绕过 `input_size == 0` 的检查，
NULL 直接进了 libbrotlienc 内部的 memcpy。

`in_file` 的 buf 之所以没被读进内存，是因为 copy filter 的 `ctx->need_in_memory` 为 0。
而它本该由 brotli header filter 设置的 `main_filter_need_in_memory` 来决定——这个位域
写错了位置，运行时永远读到 0。

### 实测对照（同一份 ngx_brotli 源码，只换编译用的 nginx 源码）

| OpenResty | 编译用源码 | `sendfile on` + 带 br 压测 |
|---|---|---|
| 1.29.2.5 | nginx.org 1.29.2 | 正常（生产长期运行） |
| 1.29.2.5 | bundle 1.29.2 | 正常 |
| 1.31.8.1 | nginx.org 1.31.8 | 6 次请求全部 signal 11 |
| 1.31.8.1 | bundle 1.31.8 | 100 次请求全部 200，零崩溃，`Content-Encoding: br` |

反汇编可见两个版本模块写入的位域偏移不同，1.29 上与运行时读的位置一致，1.31 上不一致。
1.29 那一代 OpenResty 的补丁没有移动这段布局，所以一直没暴露——不是设计正确，是恰好对齐。

### 验证时容易漏的一步

本地复现必须显式写 `sendfile on`。nginx 默认是 `off`，此时 copy filter 会把文件 buf
强制读进内存，`pos` 不为 NULL，怎么压都不崩——这正是这个问题被误判成"只在生产出现"
的原因。

### 现状

Dockerfile 已改为从 `openresty.org` 下载源码包、在 `bundle/nginx-<ver>` 里编译模块；
下载失败时直接失败，不退回 nginx.org 源码。修复后 1.31.8.1 也可用，是否升级另议。
