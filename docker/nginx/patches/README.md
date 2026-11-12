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

### 二、1.29.2 受影响且上游**没有**回补的（共 11 条，逐条判定）

| nginx 修复版本 | 日期 | CVE | 严重度 | 本项目是否可达 | 处理 |
|---|---|---|---|---|---|
| 1.29.8 | 2026-04-07 | **CVE-2026-49975**（HTTP/2 Bomb，官方 changelog 记为 Feature，见下节） | major（社区评级） | **是**：HTTPS 模板 `http2 on`，真实域名部署默认生效 | **已打补丁**（max_headers） |
| 1.31.2 | 2026-06-17 | CVE-2026-42055 | medium | 否：无 `grpc_pass`、无 `proxy_protocol` | 不打 |
| 1.31.2 | 2026-06-17 | CVE-2026-48142 | low | 否：无 `charset_map`（修复只是该指令的配置期校验） | 不打 |
| 1.31.3 | 2026-07-15 | **CVE-2026-42533** | **major** | 当前配置已规避：正则 `map` 存在，但相关数字捕获先由 `set` 保存，未在同一表达式中先引用捕获再触发 map | 不打，保留配置级规避 |
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

### 判定所依据的配置实测（三个配置文件合计）

此前将正则 map 记为 0 处不准确：`nginx.conf` 有访客 IP、日志筛选等正则 map，两套站点模板也有 `$label_id`、`$is_api_request` 等正则 map。CVE-2026-42533 的配置级规避在模板：article/sort 的 `$1`、`$2` 先单独 `set` 到普通变量，再与 map 输出变量组合；当前表达式没有先引用数字捕获、再触发 map 覆盖捕获的用法。后续修改这类表达式必须重新复核。

```
map 带正则 (~ 或 ~*)                  存在（相关数字捕获已用 set 保存）
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
http2 on                             1     ← 仅 HTTPS 模板（default.https.conf:131）
alias location 总数                  16
其中同时含 rewrite                    0
```

## 结论一句话

命中面有两条：**CVE-2026-95101**（仅自建 HTTPS + HTTP/3 时可达）与 **CVE-2026-49975**
（只要 HTTPS 模板的 `http2 on` 生效就可达，即所有真实域名部署，影响面比 95101 大得多）。
均已打补丁，其余 9 条全部不可达，另 13 条由 OpenResty 上游回补。

**注意**：这个结论绑定当前配置。若将来去掉 map 相关数字捕获的 `set` 隔离、开启 `ssi` / `slice` / `aio`、
改用 gRPC 上游，或把 `ssl_session_tickets` 打开，对应的 CVE 前置条件可能成立，必须重新评估。

## 为什么 CVE-2026-49975 会被审计漏掉

nginx 官方**没有把它当 CVE 处理**：

1. advisories 页面至今没有这个条目；
2. nginx 1.29.8（2026-04-07）的 changelog 把 `max_headers` 记为 `Feature:`，不是 `Security:`；
3. CVE-2026-49975 这个编号由 CNA 分配给了 Apache httpd 变体，nginx 只是社区用它统一指称。

攻击本体（HTTP/2 Bomb，oss-security 2026-06-03 披露）：滥用 HPACK 动态表索引，
一个请求携带数千个指向同一头部的单字节索引，每个索引在服务端产生约 70 字节的元数据分配
（放大比约 5700:1），再借流控停滞（零窗口 + 周期性 WINDOW_UPDATE）把已分配的内存锁死不释放。
单连接、小 VPS 上几十秒可耗尽数十 GB，worker 直接 OOM，且不需要认证。

我们的修复：把 1.29.8 引入的 `max_headers` 指令（默认 1000 个请求头）回补到 1.29.2。
官方 commit `365694160a85229a7cb006738de9260d49ff5fa2` 在 OpenResty bundle 的 1.29.2 源码上
全部 hunk 干净应用（OpenResty 只改过同文件的 `no_error_pages` 相关段，不与补丁 hunk 重叠）。

**教训：审计的输入不能只有 advisories 页面。** nginx 把协议滥用类防御记为 Feature 时，
advisories 不会出现条目，按页面清单对账就会结构性漏掉。审计输入改为：
advisories 全量 + CHANGES 全量（Security 与 Feature 都要过）+ HTTP/2 生态披露（oss-security）。

### 补丁行为实测（真实容器 + 真实 HTTP/2 连接）

两个容器：打补丁的完整镜像 vs `openresty/openresty:1.29.2.5-alpine` 原版（无补丁），
同一份最小配置（`listen 443 ssl` + `http2 on` + 自签证书），同一 h2 客户端脚本
（真实 ALPN 协商 + 真实 HEADERS 帧，非 curl 模拟）：

| 请求形态 | 无补丁原版 | 打补丁版 |
|---|---|---|
| 1 个头（正常请求） | 200 | **200**（功能无回归） |
| 100 个头 | 200 | 200 |
| 1100 个头 | **200（放行）** | **400（拒绝）** |
| 5000 个 4KB Cookie 索引（炸弹形态） | 连接终止 | 连接终止 |
| 8000 个同上 | 连接终止 | 连接终止 |

关键差异在第 3 行：1100 头落在旧版 `large_client_header_buffers`（默认 4×8k ≈ 32KB）
之下、新 1000 头上限之上——这正是 HTTP/2 Bomb 的攻击窗口：每个头在服务端解码都要分配
约 70 字节元数据，攻击者保持连接数在"能过缓冲区限制"的区间内反复打，配合流控停滞锁内存。
补丁把硬上限压到 1000，服务端在解码计数处提前拒绝，单请求元数据分配被限死在约 70KB 内。

注意本测试是 max_headers 行为验证 + 双版本对照，不是完整 Bomb PoC 复现
（完整攻击还需要 WINDOW_UPDATE 流控停滞与多连接并发，属于攻击复现范畴）；
防线存在性与生效点已由 1100 头的对照差异证明。

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
5. 构建后必须核对三件事：`nginx -V` 与基础镜像一致（**例外**：geoip/image_filter/xslt
   三个从未 load_module 的动态模块已从自编译参数中移除，核对时排除这三项，动态模块加载兼容由
   `--with-compat` 保证，brotli 已实测）、`nginx -t` 通过、Lua 与动态模块可正常加载；
6. 审计输入是三份材料合起来：advisories 全量、CHANGES 全量（Security 与 Feature 都要过，
   CVE-2026-49975 就是只看 advisories 漏掉的）、HTTP/2 生态的跨软件披露（oss-security）。

## 动态模块必须用 OpenResty 源码编译

这一节记录一个曾经导致全站不可访问、排查了一整天的构建缺陷。

**结论：任何第三方 nginx 动态模块，都必须用 OpenResty 源码包里的 `bundle/nginx-<ver>` 编译，不能用 nginx.org 的官方源码。**

brotli 只是第一个踩到的，不是它特殊：它恰好访问了被移动的位域字段。换成别的模块，只要访问结构体尾部，一样会中招。

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

哪些模块容易中招，取决于它访问结构体的哪一部分：

| 模块访问的字段 | 风险 | 例子 |
|---|---|---|
| 结构体前部的指针（`headers_out`、`connection`、`pool`） | 低 | echo、headers-more |
| 变量表、请求参数（`variables`、`args`） | 低 | set-misc、redis2 |
| 结构体尾部的位域（`main_filter_need_in_memory`、各类 flag） | **高** | brotli |

OpenResty 的补丁改动集中在结构体后半部分，所以只有访问尾部位域的模块才会踩到。
不崩不代表没问题：偏移错了但恰好读到 0 且该字段不影响逻辑时，就是长期的静默错误。

### 验证时容易漏的一步

本地复现必须显式写 `sendfile on`。nginx 默认是 `off`，此时 copy filter 会把文件 buf
强制读进内存，`pos` 不为 NULL，怎么压都不崩——这正是这个问题被误判成"只在生产出现"
的原因。

### 现状

Dockerfile 已改为从 `openresty.org` 下载源码包、在 `bundle/nginx-<ver>` 里编译模块；
下载失败时直接失败，不退回 nginx.org 源码。修复后 1.31.8.1 也可用，是否升级另议。

## 通用攻击事件：采集边界与实现

本框架按行为分类，不按 CVE 编号添加计数器。复用现有 Redis 与 `poetize:security:blacklist:<IP>`，由 `ban_check.lua` 的 access 检查拒绝后续正常请求。Java 的 `/admin/security/blockIp` 受站长鉴权保护，没有专供 nginx 调用的内部封禁入口；因此采用 Lua 直接写临时封禁，既有管理员/更长封禁通过 `SET NX EX` 保留。

| 层次 | 已有能力 | 本次补充 |
|---|---|---|
| Java 请求防护 | 恶意路径、路径遍历、URI 路径内 SQL/XSS 特征、缺参探测，共用 1 小时累计 3 次后封禁 24 小时 | 不重复统计后端响应 |
| 身份与频率 | 搜索引擎伪装验证、脚本/未验证蜘蛛高 301 守卫、接口 `@RateLimit` 限流 | 复用 never-ban 与最终黑名单键，接口限流本身不升级黑名单 |
| nginx 解析/协议 | max_headers 与 HTTP/3 补丁拒绝非法流量 | E1/E4/E5 计数；E2/E3 仅观测；E6 可选启用 |

设计：每 worker 用 1MiB 原生错误 ring 与 1024 项有界队列；`log_by_lua` 最多消费 128 条日志，按消息开头白名单及 connection/status 关联，热路径不扫描 URI/UA，也不做网络 IO。0.1 秒 timer 每批最多刷 128 个事件，Redis 以自己的 `TIME` 为跨实例时钟；单次 EVAL 完成时间戳 `LPUSH`、`LTRIM`、从尾部剔除窗口外条目、阈值判定和封禁。每个 IP/事件列表最多 threshold+1 项，入队/出队摊销 O(1)；配置阈值上限 4096，单次清理最坏 O(threshold)。正常请求新增工作固定有界，正常负载不产生任何 Redis EVAL（实测 0 次）；Redis 故障丢弃积压、退避 5 秒并放行。`NGINX_ATTACK_ENABLED=false` 时 `collection_enabled()` 直接短路，不再注册 timer、不进 log 阶段函数。

### 本地性能对照（同镜像三模式微基准）

对照方式：同一补丁镜像、2 worker/128MiB、保留真实 IP 解析 + Redis 黑名单 + xdb 路径，TLS HTTP/1.1 keepalive 1KiB 静态响应，16/64 并发各 5 次随机顺序 8 秒采样（30 样本、230911 请求零错误）。三模式：原版（无 hook/error 级日志）、总开关关闭、默认开启。这是本地微基准，不代表线上容量，也不覆盖 HTTP/2。

修复前（框架首次压测）发现两个回归并已修复：

1. **`error_log` 提到 info 后正常请求日志放大**：每个请求约 277 字节额外日志。根因是 `ban_check.lua` 的 vectorIndex 加载日志用 info 级——而这暴露了一个更早的缺陷：`access_by_lua_file` 每次请求重新执行整个文件块，文件顶层 `local v4_vindex` 等变量随请求重建，注释里宣称的"worker 级缓存"从未生效，等于**每个请求重载一次 512KiB vectorIndex**（原版 error 级日志只是把这个成本藏住了）。
2. **总开关关闭不等于回到原版**：hook/ring/共享字典/log 函数调用仍保留。

修复后状态：

- `ban_check.lua` 的 xdb 状态移入 `package.loaded["poetize.ban_check.xdb"]`，真正实现 worker 级缓存；实测集成日志中三个容器合计仅 4 条加载日志（每 worker 首载 1 条），修复前为每请求 1 条。这也顺带消除了每请求 512KiB 的重复分配。
- vectorIndex 加载日志降为 notice（只在真正加载/重载时出现）；`error_log` 保持 info——E1 的原生拒绝（`client sent too many header lines`）是 info 级，降回 error 会漏采 E1。info 级下正常流量的残余日志只有 keepalive 断开一条（约每连接 1 条，1000 个新连接实测 361KB）。
- `NGINX_ATTACK_ENABLED=false` 时 log 阶段与 init_worker 均被 `collection_enabled()` 短路，新增成本只剩配置声明。

修复前的中位数对照（供参考）：16 并发吞吐与原版持平（差异在噪声内）、CPU +4.9%；64 并发吞吐 −3.5%、CPU +9.4%、P95 92.71ms 对 91.96ms。修复后的复测数字见 `temp/nginx-events-perf/results.json`（修复前基线存档 `results-prefix.json`），判定以复测为准。

| 事件 | 已验证原生特征/状态 | 默认开关、阈值与动作 |
|---|---|---|
| E1 | `client sent too many header lines`，400 且 `$http2=h2`；不把所有 h2 400 当头超限 | 开；60 秒内第 11 次封 600 秒 |
| E2 | 本地 QUIC 数据报真实产生 `quic too short dcid in initial` | 开；仅日志观测，阈值 20 预留、不参与封禁 |
| E3 | 大 HTTP/2 头真实产生 `client sent too large header ... while processing HTTP/2 connection`，GOAWAY=11 | 开；仅日志观测，阈值 20 预留、不参与封禁 |
| E4 | `client sent invalid ...` 加 400；本地非法头名触发 | 开；60 秒内第 21 次封 600 秒 |
| E5 | `client sent too long URI`、`client sent too long header line`；414 或对外 400，内部 494 不直接出现在 access 状态 | 开；60 秒内第 11 次封 600 秒；身份不可得时仅观测 |
| E6 | 无 upstream 的本地 400/404/405，且未归入其他解析事件 | 关；启用后 60 秒内第 121 次封 600 秒 |

正常 1/100 头请求均为 200；默认容忍偶发畸形/超限，但共享公网出口仍可能聚合错误，需按运维日志调阈值。E6 易被过期资源触发且与已有后端防护重叠，默认关闭；SPA 改写为 200 的未知路径不可见。

归因安全边界：早期 400/414 可能在 rewrite 与身份头读取前结束，不能只读 `$real_client_ip` 的 remote 回退。采集器复用 `core.resolve` 重算并回写统一变量；代理模式缺身份头时仅记 `identity_unavailable`，不能封 CDN 对端。只有显式 `REAL_IP_HEADER=off` 的纯直连模式允许按 remote 计数。E2/E3 的原生日志只有连接对端，即使能够稳定触发也不具备可靠访客身份，因此始终仅观测。前置代理必须覆盖身份头并限制源站回源入口；本框架沿用现有信任链，不为客户端自报头增加新的可信保证。

兼容边界：Lua 与 Java 的 never-ban 均排除非公网、自身 IP/CIDR、可信 IP/CIDR。直接写黑名单的原因必须用 `cjson.encode(reason)` 保存为 JSON String；裸串会使 Java `RedisUtil.get()` 反序列化失败并删键。多实例必须连同一 Redis 数据库，共享 `poetize_runtime` 卷仅共享自身地址探测结果。

不新增以下专用埋点：CVE-2026-42055（grpc/proxy_protocol）、56434（ssi）、60005（slice）、15176（alias+rewrite）、97205（aio）、48142（charset_map）在当前配置无对应入口；42533 按上述捕获隔离规避，不能再以正则 map 为 0 判定；90439 受影响代码不在二进制内；97629 的 session tickets 已关闭；1642 是 nginx 到上游方向的问题，不能靠客户端攻击事件归因覆盖。已有二进制与补丁保持不变。

复现入口为 `scripts/test_nginx_attack_events.py`（本机 Docker、Python h2，`NGINX_EVENTS_TEST_IMAGE` 可指定现有补丁镜像）与 `scripts/test_nginx_ip_guard.lua`（LuaJIT）。集成入口先渲染生产 HTTP/HTTPS 模板跑 `nginx -t`，再测真实 ALPN h2、E1/E4/E5/E6、E2/E3 观测、跨实例阈值/拒绝、never-ban、TTL、开关和 Redis 故障；结果 JSON 与原始容器日志留在忽略的 `temp/nginx-events-test/`。模板校验只替换测试证书与必要的 referer 渲染，不把最小测试配置当作生产模板校验。部署者开关、日志与精确解封命令见 `docs/排障指南.md` 第 6 节。
