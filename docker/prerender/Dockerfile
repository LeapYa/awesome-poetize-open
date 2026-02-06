FROM node:20-alpine

WORKDIR /app

# 安装curl用于健康检查
RUN apk add --no-cache curl

RUN npm config set registry https://registry.npmmirror.com

# 安装依赖 puppeteer & express
RUN npm set progress=false && npm install express@4.19.2 body-parser@1.20.2 axios@1.6.7 markdown-it@14.0.0 markdown-it-multimd-table@4.2.3 html-entities@2.3.4 --save

# 复制 worker 源码
COPY worker.js .

CMD ["node", "worker.js"] 