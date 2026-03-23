FROM mariadb:11.8


# 安装 RocksDB 插件并清理
RUN set -eux; \
    apt-get update && apt-get install -y mariadb-plugin-rocksdb && \
    rm -rf /var/lib/apt/lists/*

# 首次启动自动执行
COPY ./poetize-server/sql/poetry.sql /docker-entrypoint-initdb.d/init.sql

# 创建配置文件，启动时自动加载 RocksDB 引擎
RUN echo "[mysqld]\nplugin_load_add=rocksdb=ha_rocksdb.so" > /etc/mysql/conf.d/rocksdb.cnf

EXPOSE 3306 