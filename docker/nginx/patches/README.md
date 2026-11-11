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

## 为什么锁在 OpenResty 1.29.2.5

这一节只回答一个问题：1.31.8.1 为什么不采用。

**现象**：1.31.8.1 上，凡是经过 lua 异步 socket 回调（ban_check 连 Redis）的请求，只要带 `Accept-Encoding: br`，nginx worker 必然 SIGSEGV。直连源站、不带 br 时完全正常，所以本地很难复现。

**链路**：

1. copy filter 的 `ctx->need_in_memory` 只在创建 ctx 时计算一次（`ngx_http_copy_filter_module.c:117`），之后不再更新
2. ngx_lua 0.10.32 起，协程 resume 后 body 链先于 header 链被拉起，ctx 以 `need_in_memory = 0` 建好
3. 响应文件 buf 因此没有被读进内存，以 `in_file` 形态直达 body filter
4. ngx_brotli 的 body filter 不检查 `in_file`，直接取 `buf->pos`（文件 buf 下为 NULL），交给 libbrotlienc 内部的 memcpy

**版本边界**（同配置、同一份 brotli 模块）：

| OpenResty | ngx_lua | 建 ctx 时的 main_filter_need | 结果 |
|---|---|---|---|
| 1.29.2.5 | 0.10.31rc2 | 1 | 正常返回 br |
| 1.31.1.1 | 0.10.31rc5 | 1 | 正常返回 br |
| 1.31.8.1 | 0.10.32rc7 | 0 | SIGSEGV |

边界落在 ngx_lua 0.10.31 与 0.10.32 之间。1.31 内核本身没有问题，1.31.1.1 就是 1.31 内核且工作正常。

**已排除的假设**，不必重复验证：

- musl 版本：1.2.5 与 1.2.6 的 memcpy 机器码逐字节相同
- brotli 库版本：1.2.1 在 1.31.8.1 上压 800 多次带 br 请求零崩溃
- brotli 模块产物：三个 1.31 镜像的 `.text` 段 sha256 一致

**上游**：已提 google/ngx_brotli #180，即模块缺少 `in_file` 防御。

**重新评估 1.31 的前置条件**：除 Dockerfile 头部已有的三条外，还要确认 brotli on + 带 br + 经 lua 异步 socket 这条路径不崩。

**附：遇到 ld-musl 段错误时怎么定位**

```bash
dmesg -T | grep -i segfault | tail -5
# segfault at 0 ip 00007f724e0a7ad4 ... in ld-musl-x86_64.so.1[7f724e098000+59000]
```

`[基址+长度]` 是内核记的 VMA 起始，不是文件偏移 0。真实偏移 = 该段 vaddr + (ip - 基址)：

```
0x14000 + (0x7f724e0a7ad4 - 0x7f724e098000) = 0x14000 + 0xfad4 = 0x23ad4
```

用 `readelf -lW` 确认段 vaddr（RX 段覆盖 0x14000~0x6d000，长度正好 0x59000），再用 `readelf -sW` 按地址前缀筛函数，得到 `memcpy + 0x94`。反汇编该处是 `movzbl (%rsi),%eax`，配合 `segfault at 0` 即可断定源指针为 NULL。
