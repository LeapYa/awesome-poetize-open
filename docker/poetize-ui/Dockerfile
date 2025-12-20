# ========================================
# 阶段 1: 构建阶段 - 编译前端代码
# ========================================
FROM node:20-bookworm-slim AS builder

# 声明构建时环境变量
ARG VUE_APP_POETIZE_AES_KEY
ARG VUE_APP_PRODUCTION_MODE
ENV VUE_APP_POETIZE_AES_KEY=$VUE_APP_POETIZE_AES_KEY
ENV VUE_APP_PRODUCTION_MODE=$VUE_APP_PRODUCTION_MODE
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
COPY poetize-ui/package*.json ./

# 复制 scripts 目录和 public 目录结构（postinstall 脚本需要）
COPY poetize-ui/scripts ./scripts
COPY poetize-ui/public ./public

# 安装依赖
RUN echo "开始安装依赖..." && \
    npm cache clean --force || true && \
    (npm install --force --legacy-peer-deps --no-fund --no-audit --prefer-offline --maxsockets 1 --network-timeout=600000 || \
     npm install --force --legacy-peer-deps --no-fund --no-audit --maxsockets 1 --network-timeout=600000) && \
    echo "依赖安装完成"

# 安装全局工具（Brotli 压缩）
RUN npm install -g brotli --no-fund --no-audit --maxsockets 1 || echo '警告: brotli 安装失败，将跳过 .br 文件生成'

# 复制所有源代码
COPY poetize-ui/ ./

# 构建前端项目
RUN npm run build

# 处理静态资源复制（Vite 构建时会自动将 public 内容搬运至 dist，此处仅作安全兼容性通过）
# RUN if [ -d "public/static" ]; then \
#       mkdir -p dist/static && \
#       cp -rf public/static/* dist/static/ 2>/dev/null || true; \
#     fi

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
# volume 挂载在 /app/dist，需要在启动时复制文件
CMD ["sh", "-c", "\
    echo '=== 博客前端部署：复制构建产物到 volume ===' && \
    mkdir -p /app/dist && \
    cp -rf /build/dist/* /app/dist/ && \
    echo '=== 静态文件部署完成 ===' && \
    touch /app/dist/.ui_ready && \
    echo '=== 博客前端启动成功 ===' \
"]
