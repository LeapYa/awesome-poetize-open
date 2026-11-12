# ⚠️ OpenResty 版本锁定在 1.29.2.5，升级前请先读完这段：
#    v5.2.12 把基础镜像升到 1.31.8.1 后全站不可访问（CDN 侧 550/524，浏览器 net::ERR_HTTP2_PROTOCOL_ERROR）。
#    其中 worker 持续 signal 11 已确认来自 ip2region 的 lua_c 扩展（现已改纯 Lua，见 lua/xdb_searcher.lua），
#    但换成纯 Lua 之后 1.31.8.1 仍复现 HTTP2 错误 —— 说明 1.31.x 在本部署形态下还有另一处未定位的问题。
#    因此基线退回 1.29.2.5。不要在未复现并解决该问题的前提下重新上调版本。
#    代价：1.29.x 已停止 backport，后续新增 CVE 不再修复，需自行评估或打补丁。
FROM openresty/openresty:1.29.2.5-alpine AS builder
# 国内镜像会在每个 "FROM openresty" 行后自动插入 apk 镜像加速（由 poetize 脚本 patch_dockerfile_mirror 动态查找 FROM 行完成），无需再手动维护行号

RUN apk update

# 安装构建依赖
RUN apk add --no-cache \
    build-base \
    git \
    cmake \
    brotli-dev \
    pcre-dev \
    zlib-dev \
    openssl-dev \
    curl \
    wget \
    busybox-extras

# 下载ngx_brotli模块
WORKDIR /usr/src
RUN set -e; \
    echo "🔧 尝试下载ngx_brotli模块..."; \
    # 首先尝试GitHub官方
    if timeout -s TERM 240 \
        git clone --depth=1 --shallow-submodules --recursive https://github.com/google/ngx_brotli.git; then \
        echo "✅ 从GitHub下载ngx_brotli成功"; \
    # 如果GitHub失败，尝试AtomGit国内镜像
    elif timeout -s TERM 240 \
        rm -rf ngx_brotli && \
        git clone --depth=1 https://atomgit.com/gh_mirrors/ng/ngx_brotli.git; then \
        echo "🔄 AtomGit 主仓库成功，开始处理子模块…"; \
        cd ngx_brotli && \
        # 把子模块地址改到 AtomGit 镜像
        git submodule set-url deps/brotli https://atomgit.com/gh_mirrors/brotl/brotli.git && \
        # 浅克隆子模块
        git submodule update --init --depth=1 --recommend-shallow && \
        echo "✅ 子模块 brotli 已就位"; \
        cd ..; \
    else \
        echo "❌ 所有ngx_brotli下载方法都失败，请检查网络连接"; \
        exit 1; \
    fi

# 获取Nginx版本与OpenResty版本
RUN nginx -v 2>&1 | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+' > /tmp/nginx_version
RUN openresty -v 2>&1 | grep -o 'openresty/[0-9]\+\(\.[0-9]\+\)*' | cut -d/ -f2 > /tmp/openresty_version

# 编译Brotli模块
# ⚠️ 必须用 OpenResty 源码包里的 bundle/nginx-<ver> 编译，不能用 nginx.org 的官方源码。
#    OpenResty 对 nginx 打了补丁，ngx_http_request_t 的尾部布局与官方源码不同。
#    --with-compat 只放宽签名校验，不解决结构体 ABI 差异：用官方源码编出的模块虽然能加载，
#    访问 main_filter_need_in_memory 这类尾部位域时会写到错误偏移，运行时永远读到 0。
#    于是 copy filter 不把文件 buf 读进内存，brotli 拿到 pos=NULL 的 buf，在 libbrotlienc
#    内部的 memcpy 上段错误。1.31.8.1 上必现（sendfile on + 带 br 的请求），
#    1.29.2.5 上因两套源码的布局恰好一致而未暴露。
WORKDIR /usr/src/ngx_brotli
RUN set -e; \
    if [ -f "config" ]; then \
        echo "🔧 开始编译Brotli模块（使用 OpenResty bundle 源码）..."; \
        NGINX_VERSION=$(cat /tmp/nginx_version); \
        OPENRESTY_VERSION=$(cat /tmp/openresty_version); \
        echo "   nginx=${NGINX_VERSION}  openresty=${OPENRESTY_VERSION}"; \
        if ! wget --timeout=180 --tries=3 -O /usr/src/openresty-src.tar.gz \
                https://openresty.org/download/openresty-${OPENRESTY_VERSION}.tar.gz; then \
            echo "❌ OpenResty 源码下载失败。不要退回 nginx.org 源码：两者结构体布局不同，编出的模块会踩内存偏移错误"; \
            exit 1; \
        fi; \
        tar -xzf /usr/src/openresty-src.tar.gz -C /usr/src && \
        cd /usr/src/openresty-${OPENRESTY_VERSION}/bundle/nginx-${NGINX_VERSION} && \
        ./configure --with-compat --add-dynamic-module=/usr/src/ngx_brotli && \
        make modules && \
        mkdir -p /usr/local/openresty/nginx/modules/ && \
        cp objs/ngx_http_brotli_filter_module.so /usr/local/openresty/nginx/modules/ 2>/dev/null || true && \
        cp objs/ngx_http_brotli_static_module.so /usr/local/openresty/nginx/modules/ 2>/dev/null || true; \
        echo "🎉 Brotli模块编译完成"; \
    else \
        echo "⏭️ ngx_brotli未正确下载"; \
        exit 1; \
    fi

# 下载 ip2region 源码（仅用于提供 fallback xdb 数据文件）
# 查询实现走 lua/xdb_searcher.lua（官方纯 Lua 版，随 lua/ 目录进镜像），不编译任何 C 扩展
WORKDIR /usr/src
RUN set -e; \
    echo "🔧 尝试下载 ip2region 源码..."; \
    # 首先尝试GitHub官方
    if timeout -s TERM 240 \
        git clone --depth=1 https://github.com/lionsoul2014/ip2region.git; then \
        echo "✅ 从GitHub下载ip2region成功"; \
    # 如果GitHub失败，尝试作者维护的Gitee官方镜像
    elif timeout -s TERM 240 \
        rm -rf ip2region && \
        git clone --depth=1 https://gitee.com/lionsoul/ip2region.git; then \
        echo "🔄 从Gitee下载ip2region成功"; \
    # 如果都失败，尝试AtomGit国内镜像
    elif timeout -s TERM 240 \
        rm -rf ip2region && \
        git clone --depth=1 https://atomgit.com/gh_mirrors/ip/ip2region.git; then \
        echo "🔄 从AtomGit下载ip2region成功"; \
    else \
        echo "❌ ip2region下载失败，请检查网络连接"; \
        exit 1; \
    fi

# ===== 已停用：Lua 运行库安装留档 =====
# 当前 Nginx/SEO 链路已经迁移到后端预渲染 + Nginx 原生 try_files，不再执行 Lua 脚本。
# 下面这段是历史上为 OpenResty Lua SEO fallback 准备的依赖安装逻辑，先保留为注释，便于以后参考或回退。
#
# # 在构建阶段安装lua模块
# WORKDIR /usr/src
#
# # 安装lua-resty-openssl
# RUN set -e; \
#     echo "🔧 尝试安装lua-resty-openssl..."; \
#     # 首先尝试GitHub
#     if timeout -s TERM 240 \
#         git clone --depth=1 https://github.com/fffonion/lua-resty-openssl.git; then \
#         echo "✅ 从GitHub下载lua-resty-openssl成功"; \
#     # 如果GitHub失败，尝试Gitee
#     elif timeout -s TERM 240 \
#         rm -rf lua-resty-openssl && \
#         git clone --depth=1 https://gitee.com/mirrors_fffonion/lua-resty-openssl.git; then \
#         echo "🔄 从Gitee下载lua-resty-openssl成功"; \
#     # 如果都失败，尝试直接下载
#     elif wget -O lua-resty-openssl.tar.gz https://github.com/fffonion/lua-resty-openssl/archive/refs/heads/master.tar.gz && \
#          tar -xzf lua-resty-openssl.tar.gz && mv lua-resty-openssl-master lua-resty-openssl; then \
#         echo "🔄 从tar包下载lua-resty-openssl成功"; \
#     else \
#         echo "❌ lua-resty-openssl下载失败"; \
#         exit 1; \
#     fi; \
#     mkdir -p /usr/local/openresty/lualib/resty/openssl && \
#     cd lua-resty-openssl && \
#     cp -r lib/resty/openssl/* /usr/local/openresty/lualib/resty/openssl/ 2>/dev/null || true && \
#     cp lib/resty/openssl.lua /usr/local/openresty/lualib/resty/ 2>/dev/null || true; \
#     echo "✅ lua-resty-openssl安装完成"
#
# # 安装lua-resty-http模块
# RUN set -e; \
#     mkdir -p /usr/local/openresty/lualib/resty; \
#     echo "🔧 尝试安装lua-resty-http..."; \
#     # 首先尝试GitHub
#     if timeout -s TERM 240 \
#         git clone --depth=1 https://github.com/ledgetech/lua-resty-http.git; then \
#         echo "✅ 从GitHub下载lua-resty-http成功"; \
#     # 如果GitHub失败，尝试AtomGit国内镜像
#     elif timeout -s TERM 240 \
#         rm -rf lua-resty-http && \
#         git clone --depth=1 https://atomgit.com/gh_mirrors/lu/lua-resty-http.git; then \
#         echo "🔄 从AtomGit下载lua-resty-http成功"; \
#     # 如果都失败，尝试直接下载
#     elif wget -O lua-resty-http.tar.gz https://github.com/ledgetech/lua-resty-http/archive/refs/heads/master.tar.gz && \
#          tar -xzf lua-resty-http.tar.gz && mv lua-resty-http-master lua-resty-http; then \
#         echo "🔄 从tar包下载lua-resty-http成功"; \
#     else \
#         echo "❌ lua-resty-http下载失败"; \
#         exit 1; \
#     fi; \
#     cd lua-resty-http && \
#     cp lib/resty/http.lua /usr/local/openresty/lualib/resty/ 2>/dev/null || true && \
#     cp lib/resty/http_headers.lua /usr/local/openresty/lualib/resty/ 2>/dev/null || true && \
#     cp lib/resty/http_connect.lua /usr/local/openresty/lualib/resty/ 2>/dev/null || true && \
#     cp -r lib/ngx /usr/local/openresty/lualib/ 2>/dev/null || true; \
#     echo "✅ lua-resty-http安装完成"

# ===== nginx 安全补丁构建 =====
# 1.29.x 已停止 backport，新 CVE 只能自行回补。判定表、可达性分析与新增补丁的规矩见 patches/README.md。
# 实现方式：下载官方源码包 → 应用 patches/*.patch → 以基础镜像同款参数重编 nginx → 只把 objs/nginx
# 二进制替换进最终镜像。
# 有意去掉 geoip/image_filter/xslt 三个从未 load_module 的动态模块（减审计面与构建时间）；
# 基础镜像层自带的三个 .so 无 load 指令、保持惰性。本二进制的 -V 不再与基础镜像逐字一致，
# 动态模块加载兼容由 --with-compat 保证（brotli 已实测加载正常）。
# 复用 builder 阶段的编译环境，避免再装一遍 build-base（国内网络下这一步要好几分钟）。
FROM builder AS nginx-patch

# 先放一份基础镜像自带的二进制兜底：当构建环境拿不到源码/装不上编译依赖时，降级为原版继续构建，
# 避免为了一个补丁把整站升级卡死
RUN mkdir -p /out && cp /usr/local/openresty/nginx/sbin/nginx /out/nginx

COPY patches/ /tmp/patches/

# ngx_lua 的 config 脚本靠这两个环境变量定位 LuaJIT（镜像路径与默认探测路径不同）
ENV LUAJIT_INC=/usr/local/openresty/luajit/include/luajit-2.1 \
    LUAJIT_LIB=/usr/local/openresty/luajit/lib

RUN set -e; \
    # geoip-dev/libxslt-dev/gd-dev 仅服务于已移除的三个动态模块，不再安装
    if ! apk add --no-cache perl linux-headers; then \
        echo "⚠️  无法安装编译依赖（网络受限），跳过 nginx CVE 补丁，沿用基础镜像自带 nginx"; \
        exit 0; \
    fi; \
    if ! wget -q -T 180 -t 3 -O /usr/src/openresty-src.tar.gz \
            https://openresty.org/download/openresty-1.29.2.5.tar.gz; then \
        echo "⚠️ OpenResty 源码下载失败，跳过 nginx CVE 补丁，沿用基础镜像自带 nginx"; \
        exit 0; \
    fi; \
    tar -xzf /usr/src/openresty-src.tar.gz -C /usr/src; \
    cd /usr/src/openresty-1.29.2.5/bundle/nginx-1.29.2; \
    for p in /tmp/patches/*.patch; do \
        echo "→ 应用安全补丁 $(basename "$p")"; \
        patch -p1 --no-backup-if-mismatch < "$p"; \
    done; \
    cat /tmp/patches/lua-default-path.snippet >> ../ngx_lua-0.10.31rc2/config; \
    ./configure \
        --prefix=/usr/local/openresty/nginx \
        --with-cc-opt='-O2 -DNGX_LUA_ABORT_AT_PANIC -I/usr/local/openresty/pcre2/include -I/usr/local/openresty/openssl3/include' \
        --add-module=../ngx_devel_kit-0.3.4 \
        --add-module=../echo-nginx-module-0.64 \
        --add-module=../xss-nginx-module-0.07 \
        --add-module=../ngx_coolkit-0.2 \
        --add-module=../set-misc-nginx-module-0.33 \
        --add-module=../form-input-nginx-module-0.12 \
        --add-module=../encrypted-session-nginx-module-0.09 \
        --add-module=../srcache-nginx-module-0.33 \
        --add-module=../ngx_lua-0.10.31rc2 \
        --add-module=../ngx_lua_upstream-0.08 \
        --add-module=../headers-more-nginx-module-0.39 \
        --add-module=../array-var-nginx-module-0.06 \
        --add-module=../memc-nginx-module-0.20 \
        --add-module=../redis2-nginx-module-0.15 \
        --add-module=../redis-nginx-module-0.41 \
        --add-module=../ngx_stream_lua-0.0.19rc3 \
        --with-ld-opt='-Wl,-rpath,/usr/local/openresty/luajit/lib -L/usr/local/openresty/pcre2/lib -L/usr/local/openresty/openssl3/lib -Wl,-rpath,/usr/local/openresty/pcre2/lib:/usr/local/openresty/openssl3/lib' \
        --with-pcre \
        --with-compat \
        --without-mail_pop3_module \
        --without-mail_imap_module \
        --without-mail_smtp_module \
        --with-http_addition_module \
        --with-http_auth_request_module \
        --with-http_dav_module \
        --with-http_flv_module \
        --with-http_gunzip_module \
        --with-http_gzip_static_module \
        --with-http_mp4_module \
        --with-http_random_index_module \
        --with-http_realip_module \
        --with-http_secure_link_module \
        --with-http_slice_module \
        --with-http_ssl_module \
        --with-http_stub_status_module \
        --with-http_sub_module \
        --with-http_v2_module \
        --with-http_v3_module \
        --with-ipv6 \
        --with-mail \
        --with-mail_ssl_module \
        --with-md5-asm \
        --with-sha1-asm \
        --with-stream \
        --with-stream_ssl_module \
        --with-stream_ssl_preread_module \
        --with-threads \
        --with-pcre-jit \
        --with-stream; \
    make -j"$(nproc)"; \
    cp objs/nginx /out/nginx; \
    echo "✅ nginx CVE 补丁已编入镜像"

# 最终镜像
FROM openresty/openresty:1.29.2.5-alpine

RUN apk update

# 只安装运行时必需的依赖
RUN apk add --no-cache \
    ca-certificates \
    brotli \
    pcre \
    zlib \
    openssl

# 创建nginx用户和组
RUN addgroup -g 101 -S nginx && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx

# 创建必要的目录并设置权限
RUN mkdir -p /var/log/nginx /var/cache/nginx /var/run /tmp && \
    chown -R nginx:nginx /var/log/nginx /var/cache/nginx /var/run && \
    chmod -R 755 /var/log/nginx /var/cache/nginx /var/run && \
    chmod 777 /tmp

# 从builder中复制Brotli模块
COPY --from=builder /usr/local/openresty/nginx/modules/ /usr/local/openresty/nginx/modules/

# 用回补了 CVE 的 nginx 二进制覆盖基础镜像自带版本
# （补丁构建阶段降级时，/out/nginx 就是基础镜像的原版二进制，行为不变）
COPY --from=nginx-patch /out/nginx /usr/local/openresty/nginx/sbin/nginx

# 复制 fallback xdb 数据文件（运行时优先用共享卷里 Java 更新的版本，共享卷不存在时用此 fallback）
COPY --from=builder /usr/src/ip2region/data/ip2region_v4.xdb /usr/local/openresty/xdb/ip2region_v4.xdb
COPY --from=builder /usr/src/ip2region/data/ip2region_v6.xdb /usr/local/openresty/xdb/ip2region_v6.xdb

# 创建配置目录和 Lua 脚本目录
RUN mkdir -p /usr/local/openresty/nginx/conf/conf.d /usr/local/openresty/nginx/lua

# 复制自定义配置
COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# 复制 Lua 脚本（IP 黑名单检查，依赖 OpenResty 自带的 lua-resty-redis；含访客 IP 可信链解析）
COPY lua/ /usr/local/openresty/nginx/lua/

# 容器启动入口脚本（启动前探测"服务器自身公网 IP"写入共享运行时目录，详见脚本头注释）
COPY entrypoint.sh /docker-entrypoint.sh

# 复制配置模板文件
COPY default.http.conf /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template
COPY default.https.conf /usr/local/openresty/nginx/conf/conf.d/default.https.conf.template

# 在构建时就处理配置文件，默认使用HTTP配置
RUN cp /usr/local/openresty/nginx/conf/conf.d/default.http.conf.template \
       /usr/local/openresty/nginx/conf/conf.d/default.conf

# 生成默认自签名证书（用于拒绝IP直接访问HTTPS的default_server块）
RUN mkdir -p /etc/nginx/ssl && \
    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
        -keyout /etc/nginx/ssl/default.key \
        -out /etc/nginx/ssl/default.crt \
        -subj "/CN=_" && \
    chmod 644 /etc/nginx/ssl/default.crt /etc/nginx/ssl/default.key

# 已停用：Nginx 不再读取 /etc/nginx/lua 下的脚本，历史命令留档。
# mkdir -p /etc/nginx/lua && \
# chown nginx:nginx /etc/nginx/lua && \
# chmod 755 /etc/nginx/lua && \

# 设置nginx相关目录的权限
RUN chown -R nginx:nginx /usr/local/openresty/nginx/ /usr/local/openresty/xdb/ && \
    chmod -R 755 /usr/local/openresty/nginx/conf/ && \
    # 给nginx用户配置目录的写权限
    chmod 775 /usr/local/openresty/nginx/conf/conf.d/ && \
    # 创建并设置nginx目录权限
    mkdir -p /usr/share/nginx/html && \
    chown -R nginx:nginx /usr/share/nginx && \
    chmod -R 755 /usr/share/nginx && \
    # 创建并设置Let's Encrypt验证目录权限
    mkdir -p /usr/share/nginx/html/.well-known/acme-challenge && \
    chown -R nginx:nginx /usr/share/nginx/html/.well-known && \
    chmod -R 777 /usr/share/nginx/html/.well-known && \
    # 创建并设置SSL证书目录权限（nginx需要读取证书文件）
    mkdir -p /etc/letsencrypt /var/lib/letsencrypt && \
    chown -R nginx:nginx /etc/letsencrypt /var/lib/letsencrypt && \
    chmod -R 755 /etc/letsencrypt /var/lib/letsencrypt && \
    # 创建并设置数据目录权限（nginx需要读取sitemap.xml和robots.txt）
    mkdir -p /app/data && \
    chown nginx:nginx /app/data && \
    chmod 755 /app/data && \
    # 运行时共享目录（启动期产物：自身公网 IP；Java 容器只读挂载同一卷）
    mkdir -p /app/runtime && \
    chown nginx:nginx /app/runtime && \
    chmod 755 /app/runtime && \
    # 入口脚本可执行
    chmod 755 /docker-entrypoint.sh && \
    # 设置默认SSL证书目录权限
    chown -R nginx:nginx /etc/nginx/ssl && \
    chmod 755 /etc/nginx/ssl

# 暴露端口
EXPOSE 80 443/tcp 443/udp

# 切换到nginx用户
USER nginx

# 启动入口：先探测"服务器自身公网 IP"写入 /app/runtime/self_ips（失败静默跳过），再 exec CMD
ENTRYPOINT ["/docker-entrypoint.sh"]

# 启动命令
CMD ["openresty", "-g", "daemon off;"]
