FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim



# 安装系统依赖 
RUN apt-get update && apt-get install -y \
    gcc \
    curl \
    git \
    ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

ARG UV_DEFAULT_INDEX=https://pypi.org/simple

RUN set -e; \
    echo "测试PyPI官方源连接..."; \
    if curl -fsSL --connect-timeout 10 --max-time 30 https://pypi.org/simple/ >/dev/null 2>&1; then \
        echo "✅ PyPI官方源可用，保持官方配置"; \
        echo "https://pypi.org/simple" > /tmp/pypi_index.txt; \
    else \
        echo "⚠️ PyPI 官方源不可用，使用清华镜像"; \
        echo "https://pypi.tuna.tsinghua.edu.cn/simple" > /tmp/pypi_index.txt; \
    fi


# 复制所有Python服务代码
COPY --chown=root:root ./py/ /app/

# 创建虚拟环境并安装Python依赖
RUN UV_DEFAULT_INDEX=$(cat /tmp/pypi_index.txt) && \
    uv venv && \
    . .venv/bin/activate && \
    uv pip install --no-cache-dir --upgrade pip -i ${UV_DEFAULT_INDEX} && \
    uv pip install --no-cache-dir wheel -i ${UV_DEFAULT_INDEX} && \
    uv pip install --no-cache-dir -r requirements.txt -i ${UV_DEFAULT_INDEX} && \
    # 清理pip缓存
    rm -rf /root/.cache/pip

# 清理Python字节码文件和临时文件
RUN find /app -name "*.pyc" -delete && \
    find /app -name "__pycache__" -type d -exec rm -rf {} + && \
    find /app -name "*.log" -delete

# 验证安装
RUN (uv --version || echo "uv not available") && \
    (uvx --version || echo "uvx not available")

# 暴露端口
EXPOSE 5000

# 使用Uvicorn启动FastAPI应用
CMD ["uv", "run", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000", "--workers", "4", "--no-access-log"]