# ========================================
# 阶段 1: 构建阶段 - 编译后台管理前端
# ========================================
FROM node:24-bookworm-slim AS builder

# 声明构建时环境变量
ARG VUE_APP_POETIZE_AES_KEY
ARG VUE_APP_PRODUCTION_MODE
ARG VITE_APP_VERSION=dev
ENV VUE_APP_POETIZE_AES_KEY=$VUE_APP_POETIZE_AES_KEY
ENV VUE_APP_PRODUCTION_MODE=$VUE_APP_PRODUCTION_MODE
ENV VITE_APP_VERSION=$VITE_APP_VERSION
# 为 Vite 增加 VITE_ 前缀环境变量映射
ENV VITE_APP_POETIZE_AES_KEY=$VUE_APP_POETIZE_AES_KEY
ENV VITE_APP_PRODUCTION_MODE=$VUE_APP_PRODUCTION_MODE

# 安装必要的系统依赖
RUN apt-get update && \
    apt-get install -y git curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 配置 npm 镜像源（加速构建）
RUN npm config set registry https://registry.npmmirror.com && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000 && \
    npm config set fetch-retries 5

# 先复制 package.json 和 package-lock.json（利用 Docker 缓存层）
COPY poetize-admin/package*.json ./

# 复制 scripts 目录和 public 目录结构（postinstall 脚本需要）
COPY poetize-admin/scripts ./scripts
COPY poetize-admin/public ./public

# 安装依赖
# npm ci：严格按 package-lock.json 安装（lock 已入库），失败即中止构建。
# 禁止回退 npm install --force：它会重新解析依赖版本造成依赖漂移——v5.2.8 白屏事故
# （rollup treeshake + CJS alias 丢 getTagNamespace 实参）正是漂移依赖触发的。
# lock 与 package.json 失同步时，正确做法是本地更新 lock 并提交，而不是在生产构建里绕过它。
RUN echo "开始安装依赖..." && \
    npm cache clean --force || true && \
    npm ci --legacy-peer-deps --no-fund --no-audit --maxsockets 1 --network-timeout=600000 && \
    echo "依赖安装完成（npm ci 严格按 lock 安装）"

# 安装全局工具（Brotli 压缩）
RUN npm install -g brotli --no-fund --no-audit --maxsockets 1 || echo '警告: brotli 安装失败，将跳过 .br 文件生成'

# 复制所有源代码
COPY poetize-admin/ ./

# 构建前端项目（限制内存使用以适应低配服务器）
RUN NODE_OPTIONS="--max-old-space-size=1536 --max-semi-space-size=64" npm run build

# ========================================
# 阶段 2: 运行阶段 - 只保留构建产物
# ========================================
FROM alpine:3.19 AS runtime

# 设置时区
ENV TZ=Asia/Shanghai

# 安装必要工具（用于复制文件到 volume）
RUN apk add --no-cache bash coreutils

# 创建工作目录
WORKDIR /app

# 从构建阶段复制构建产物到临时目录
COPY --from=builder /app/dist /build/dist

# 启动脚本：将构建产物从镜像复制到 volume 挂载点
CMD ["sh", "-c", "\
    echo '=== 后台管理前端部署：复制构建产物到 volume ===' && \
    mkdir -p /app/dist && \
    cp -rf /build/dist/* /app/dist/ && \
    echo '=== 静态文件部署完成 ===' && \
    touch /app/dist/.admin_ready && \
    echo '=== 后台管理前端启动成功 ===' \
"]
