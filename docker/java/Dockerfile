# 使用Maven作为构建环境
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

WORKDIR /app

# 复制整个poetize-server目录到容器中
COPY ./poetize-server /app

# 在容器中运行Maven构建（启用Java 25预览特性）
RUN mvn -f /app/pom.xml clean package -DskipTests -Darguments="--enable-preview"

# 最终运行环境
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# 安装健康检查所需工具和WebP支持库
RUN apk add --no-cache netcat-openbsd curl libwebp libwebp-tools

# 预下载Ip2Region数据库（自动尝试官方源，失败则使用国内镜像）
# 提前下载避免容器启动时因网络问题导致超时
RUN mkdir -p /root/.ip2region && \
    # 下载 IPv4 库
    (curl -fsSL -o /root/.ip2region/ip2region_v4.xdb "https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v4.xdb" --connect-timeout 10 --retry 2 || \
     curl -fsSL -o /root/.ip2region/ip2region_v4.xdb "https://gh-proxy.org/https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v4.xdb") && \
    # 下载 IPv6 库
    (curl -fsSL -o /root/.ip2region/ip2region_v6.xdb "https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v6.xdb" --connect-timeout 10 --retry 2 || \
     curl -fsSL -o /root/.ip2region/ip2region_v6.xdb "https://gh-proxy.org/https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v6.xdb")

# 从构建阶段复制JAR文件
COPY --from=builder /app/poetry-web/target/poetize-server-exec.jar /app/app.jar

# 暴露端口
EXPOSE 8081

# 创建健康检查脚本
RUN echo '#!/bin/sh\nset -e\nif nc -z localhost 8081; then\n  curl -f http://localhost:8081/actuator/health || exit 1\n  exit 0\nfi\nexit 1' > /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh

# 启动命令
CMD ["sh", "-c", "ls -la /app && java --enable-preview $JAVA_OPTS -jar /app/app.jar"] 