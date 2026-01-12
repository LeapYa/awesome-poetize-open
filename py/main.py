"""
POETIZE博客系统 - FastAPI后端服务
支持AI聊天、多语言翻译等功能

主要功能模块：
- 多语言翻译
- AI聊天
- 多语言摘要
"""

import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import RedirectResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from starlette.middleware.sessions import SessionMiddleware
from config import SECRET_KEY, JAVA_BACKEND_URL, PYTHON_SERVICE_PORT, get_frontend_url, init_frontend_url

from ai_chat_api import register_ai_chat_api  # 处理AI聊天功能（非配置管理）
from translation_api import register_translation_api  # 处理翻译功能（非配置管理）

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 定义应用生命周期管理
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # ========== 启动时执行 ==========
    logger.info("应用启动中，开始初始化...")
    
    # 初始化前端URL
    await init_frontend_url()
    
    logger.info("应用启动完成")
    
    yield  # 应用运行中
    
    # ========== 关闭时执行 ==========
    logger.info("应用正在关闭...")
    logger.info("应用已关闭")

# 创建FastAPI应用实例
# 安全配置：禁用自动文档功能，避免在生产环境暴露API结构
app = FastAPI(
    title="POETIZE博客系统API",
    description="基于FastAPI的博客系统后端服务，提供AI聊天、翻译等功能",
    version="2.0.0",
    docs_url=None,        # 禁用Swagger UI文档 (/docs)
    redoc_url=None,       # 禁用ReDoc文档 (/redoc)
    openapi_url=None,     # 禁用OpenAPI schema (/openapi.json)
    lifespan=lifespan     # 使用新的生命周期管理
)

# 添加Session中间件（必须在其他中间件之前添加）
app.add_middleware(
    SessionMiddleware,
    secret_key=SECRET_KEY,
    max_age=3600,  # 1小时过期
    same_site='lax',  # 允许跨站点请求携带cookie
    https_only=False  # 开发环境允许HTTP
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
    max_age=3600
)

# 添加GZip压缩中间件，压缩响应数据以节省带宽
app.add_middleware(GZipMiddleware, minimum_size=1000)

@app.get("/")
async def index():
    frontend_url = await get_frontend_url()
    return RedirectResponse(url=frontend_url)

# 健康检查接口
@app.get("/api/health")
async def health_check():
    """API健康检查端点，返回系统状态和版本信息"""
    return {
        'status': 'ok',
        'api_version': os.environ.get('VERSION', 'dev'),
        'app_version': os.environ.get('VERSION', 'dev'),
        'translation_model': 'integrated',
    }

# 健康检查接口
@app.get("/health")
async def health_check_old():
    """兼容旧的健康检查路径"""
    return await health_check()



def register_all_apis(app):
    """注册所有API模块"""
    # 注册各个API模块
    register_ai_chat_api(app)  # 注册AI聊天功能API（非配置管理，配置管理已迁移到Java）
    register_translation_api(app)  # 注册翻译功能API（非配置管理，配置管理已迁移到Java）

    logger.info("所有API模块已注册完成")

# 注册所有API
register_all_apis(app)

if __name__ == '__main__':
    import uvicorn
    
    port = int(os.environ.get('PORT', PYTHON_SERVICE_PORT))
    debug = os.environ.get('DEBUG', 'False').lower() == 'true'
    
    logger.info("==========================================")
    logger.info("POETIZE博客系统 - FastAPI后端服务启动中...")
    logger.info("==========================================")
    logger.info(f"服务端口: {port}")
    logger.info(f"调试模式: {debug}")
    logger.info(f"Java后端: {JAVA_BACKEND_URL}")
    logger.info("前端地址: 将在首次请求时动态获取")
    logger.info("==========================================")
    
    uvicorn.run(
        "main:app", 
        host='0.0.0.0', 
        port=port, 
        reload=debug,
        log_level="info",
        access_log=False  # 禁用访问日志，不再显示每个请求
    )