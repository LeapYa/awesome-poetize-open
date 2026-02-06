const express = require('express');
const bodyParser = require('body-parser');
const fs = require('fs');
const path = require('path');
const axios = require('axios');
const MarkdownIt = require('markdown-it');
const { JSDOM } = require('jsdom');
const { decode: decodeHtmlEntities } = require('html-entities');

const app = express();
app.use(bodyParser.json());

// ===== 日志系统和监控 =====
class Logger {
  constructor() {
    this.logLevel = process.env.LOG_LEVEL || 'info';
    this.levels = { error: 0, warn: 1, info: 2, debug: 3 };
    
    // 日志文件配置
    this.logDir = '/app/dist/logs';
    this.ensureLogDirectory();
    
    // 日志清理配置
    this.logRetentionDays = parseInt(process.env.LOG_RETENTION_DAYS) || 30; // 默认保留30天
    this.cleanupInterval = 24 * 60 * 60 * 1000; // 每天清理一次
    
    // 启动定时清理
    this.startLogCleanup();
  }

  ensureLogDirectory() {
    try {
      if (!fs.existsSync(this.logDir)) {
        fs.mkdirSync(this.logDir, { recursive: true });
      }
    } catch (error) {
      console.error('创建日志目录失败:', error);
    }
  }

  formatLog(level, message, meta = {}) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message,
      service: 'prerender-worker',
      requestId: meta.requestId || 'unknown',
      ...meta
    };
    return logEntry;
  }

  writeToFile(logEntry) {
    try {
      const today = new Date().toISOString().split('T')[0];
      const logFile = path.join(this.logDir, `prerender-${today}.log`);
      const logLine = JSON.stringify(logEntry) + '\n';
      fs.appendFileSync(logFile, logLine);
    } catch (error) {
      console.error('写入日志文件失败:', error);
    }
  }

  shouldLog(level) {
    return this.levels[level] <= this.levels[this.logLevel];
  }

  log(level, message, meta = {}) {
    if (!this.shouldLog(level)) return;
    
    const logEntry = this.formatLog(level, message, meta);
    
    // 写入文件
    this.writeToFile(logEntry);
    
    // 控制台输出
    const logString = JSON.stringify(logEntry);
    switch (level) {
      case 'error':
        console.error(logString);
        break;
      case 'warn':
        console.warn(logString);
        break;
      case 'info':
        console.info(logString);
        break;
      case 'debug':
        console.log(logString);
        break;
    }
  }

  error(message, meta = {}) {
    this.log('error', message, meta);
  }

  warn(message, meta = {}) {
    this.log('warn', message, meta);
  }

  info(message, meta = {}) {
    this.log('info', message, meta);
  }

  debug(message, meta = {}) {
    this.log('debug', message, meta);
  }

  // 启动定时日志清理
  startLogCleanup() {
    // 立即执行一次清理
    this.cleanupOldLogs();
    
    // 设置定时清理
    setInterval(() => {
      this.cleanupOldLogs();
    }, this.cleanupInterval);
    
    console.log(`日志清理已启动，保留${this.logRetentionDays}天的日志文件，每天清理一次`);
  }

  // 清理过期日志文件
  cleanupOldLogs() {
    try {
      const files = fs.readdirSync(this.logDir)
        .filter(file => file.startsWith('prerender-') && file.endsWith('.log'));

      const now = new Date();
      const cutoffDate = new Date(now.getTime() - (this.logRetentionDays * 24 * 60 * 60 * 1000));
      
      let deletedCount = 0;
      let totalSize = 0;

      files.forEach(file => {
        const filePath = path.join(this.logDir, file);
        const stats = fs.statSync(filePath);
        
        // 检查文件是否过期
        if (stats.mtime < cutoffDate) {
          try {
            totalSize += stats.size;
            fs.unlinkSync(filePath);
            deletedCount++;
            console.log(`已删除过期日志文件: ${file} (${(stats.size / 1024).toFixed(1)}KB)`);
          } catch (deleteError) {
            console.error(`删除日志文件失败 ${file}:`, deleteError.message);
          }
        }
      });

      if (deletedCount > 0) {
        console.log(`日志清理完成: 删除了${deletedCount}个文件，释放${(totalSize / 1024 / 1024).toFixed(1)}MB空间`);
        
        // 记录清理日志
        this.log('info', '定时日志清理完成', {
          deletedFiles: deletedCount,
          freedSpace: `${(totalSize / 1024 / 1024).toFixed(1)}MB`,
          retentionDays: this.logRetentionDays
        });
      }
    } catch (error) {
      console.error('清理日志文件时发生错误:', error);
      this.log('error', '日志清理失败', { error: error.message });
    }
  }
}

const logger = new Logger();

// ===== 服务状态监控 =====
class ServiceMonitor {
  constructor() {
    this.stats = {
      startTime: new Date(),
      totalRequests: 0,
      successfulRenders: 0,
      failedRenders: 0,
      articlesRendered: 0,
      pagesRendered: 0,
      averageRenderTime: 0,
      lastRenderTime: null,
      errors: [],
      currentTasks: new Map(),
      recentTasks: [], // 添加最近完成的任务历史
      systemHealth: {
        memoryUsage: {},
        uptime: 0,
        templateStatus: 'unknown'
      }
    };
    
    // 定期更新系统信息
    setInterval(() => {
      this.updateSystemHealth();
    }, 30000); // 每30秒更新一次
    
    this.updateSystemHealth();
  }

  updateSystemHealth() {
    this.stats.systemHealth.memoryUsage = process.memoryUsage();
    this.stats.systemHealth.uptime = process.uptime();
    
    // 检查模板状态
    const templatePath = path.resolve('/app/dist/index.html');
    this.stats.systemHealth.templateStatus = fs.existsSync(templatePath) ? 'available' : 'missing';
  }

  recordRequest(type = 'unknown') {
    this.stats.totalRequests++;
    logger.debug('请求已记录', { type, total: this.stats.totalRequests });
  }

  recordRenderStart(taskId, type, params = {}) {
    const task = {
      id: taskId,
      type,
      params,
      startTime: new Date(),
      status: 'running'
    };
    this.stats.currentTasks.set(taskId, task);
    logger.info('渲染任务已开始', { 
      taskId, 
      type, 
      params, 
      currentTaskCount: this.stats.currentTasks.size,
      totalRequests: this.stats.totalRequests 
    });
  }

  recordRenderSuccess(taskId, details = {}) {
    const task = this.stats.currentTasks.get(taskId);
    if (task) {
      const duration = new Date() - task.startTime;
      this.stats.successfulRenders++;
      this.stats.lastRenderTime = new Date();
      
      // 更新平均渲染时间
      const totalTime = this.stats.averageRenderTime * (this.stats.successfulRenders - 1) + duration;
      this.stats.averageRenderTime = Math.round(totalTime / this.stats.successfulRenders);
      
      if (task.type === 'article') {
        this.stats.articlesRendered += details.count || 1;
      } else {
        // 页面渲染应该记录实际成功的页面数量
        this.stats.pagesRendered += details.count || 1;
      }
      
      // 添加到任务历史
      const completedTask = {
        taskId: task.id,
        type: task.type,
        status: 'completed',
        startTime: task.startTime.toISOString(),
        endTime: new Date().toISOString(),
        duration: `${duration}ms`,
        params: task.params,
        details
      };
      
      // 确保recentTasks数组存在
      if (!this.stats.recentTasks) {
        this.stats.recentTasks = [];
      }
      this.stats.recentTasks.push(completedTask);
      
      // 只保留最近20个完成的任务
      if (this.stats.recentTasks.length > 20) {
        this.stats.recentTasks = this.stats.recentTasks.slice(-20);
      }
      
      this.stats.currentTasks.delete(taskId);
      logger.info('渲染任务成功完成', { 
        taskId, 
        duration: `${duration}ms`, 
        type: task.type,
        currentTaskCount: this.stats.currentTasks.size,
        recentTaskCount: this.stats.recentTasks.length,
        ...details 
      });
      
      // 调试：打印任务完成信息
      console.log('=== 任务完成 ===');
      console.log(`任务ID: ${taskId}`);
      console.log(`类型: ${task.type}`);
      console.log(`耗时: ${duration}ms`);
      console.log(`当前运行中任务数: ${this.stats.currentTasks.size}`);
      console.log(`历史任务数: ${this.stats.recentTasks.length}`);
      console.log(`成功渲染总数: ${this.stats.successfulRenders}`);
      console.log('详情:', details);
      console.log('==================');
    }
  }

  recordRenderFailure(taskId, error) {
    const task = this.stats.currentTasks.get(taskId);
    if (task) {
      const duration = new Date() - task.startTime;
      this.stats.failedRenders++;
      
      const errorRecord = {
        timestamp: new Date(),
        taskId,
        type: task.type,
        error: error.message || error.toString(),
        duration: `${duration}ms`,
        params: task.params
      };
      
      // 确保errors数组存在
      if (!this.stats.errors) {
        this.stats.errors = [];
      }
      this.stats.errors.push(errorRecord);
      
      // 只保留最近50个错误记录
      if (this.stats.errors.length > 50) {
        this.stats.errors = this.stats.errors.slice(-50);
      }
      
      this.stats.currentTasks.delete(taskId);
      logger.error('渲染任务失败', errorRecord);
    }
  }

  clearStats() {
    this.stats = {
      startTime: new Date(),
      totalRequests: 0,
      successfulRenders: 0,
      failedRenders: 0,
      articlesRendered: 0,
      pagesRendered: 0,
      averageRenderTime: 0,
      lastRenderTime: null,
      errors: [],
      currentTasks: new Map(),
      systemHealth: {
        memoryUsage: {},
        uptime: 0,
        templateStatus: 'unknown'
      }
    };
  }
}

const monitor = new ServiceMonitor();

// ===== 任务ID生成器 =====
function generateTaskId() {
  return `task_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

const JAVA_BACKEND_URL = process.env.JAVA_BACKEND_URL || 'http://poetize-java:8081';
const PYTHON_BACKEND_URL = process.env.PYTHON_BACKEND_URL || 'http://poetize-python:5000';

const md = new MarkdownIt({breaks: true}).use(require('markdown-it-multimd-table'));

/**
 * 获取系统配置的源语言
 * @returns {Promise<string>} 源语言代码，默认为'zh'
 */
async function getSourceLanguage() {
  try {
    logger.debug('从Java后端获取源语言配置');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/ai/config/articleAi/defaultLang`, {
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });

    if (res.data && res.data.code === 200 && res.data.data) {
      const sourceLanguage = res.data.data.default_source_lang || 'zh';

      logger.debug('已从Java后端获取源语言配置', {
        sourceLanguage,
        responseCode: res.data.code,
        fullConfig: res.data.data
      });

      return sourceLanguage;
    } else {
      logger.warn('Java翻译配置API响应格式无效', {
        responseCode: res.data?.code,
        hasData: !!res.data?.data
      });
    }
  } catch (error) {
    logger.warn('从Java后端获取源语言配置失败，使用默认配置', {
      error: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: `${JAVA_BACKEND_URL}/webInfo/ai/config/articleAi/defaultLang`
    });
  }

  // 返回默认源语言
  const defaultSourceLanguage = 'zh';
  logger.info('使用默认源语言', { sourceLanguage: defaultSourceLanguage });
  return defaultSourceLanguage;
}

// 完整版：先尝试 manifest.json，失败则解析 index.html，结果缓存 10 分钟
const assetCache = { assets: null, lastFetch: 0 };
async function getFrontEndAssets(host = 'nginx') {
  const TEN_MIN = 10 * 60 * 1000;
  if (assetCache.assets && Date.now() - assetCache.lastFetch < TEN_MIN) {
    return assetCache.assets;
  }

  // 1. manifest.json
  try {
    const manifestRes = await axios.get(`http://${host}/manifest.json`, { timeout: 3000 });
    if (manifestRes.status === 200 && manifestRes.data) {
      const m = typeof manifestRes.data === 'string' ? JSON.parse(manifestRes.data) : manifestRes.data;
      assetCache.assets = {
        css: m['app.css'] || '/css/app.css',
        js: m['app.js'] || '/js/app.js',
        vendorJs: m['vendor.js'] || null,
        vendorCss: m['vendor.css'] || null
      };
      assetCache.lastFetch = Date.now();
      return assetCache.assets;
    }
  } catch (_) { /* ignore */ }

  // 2. 解析首页 HTML
  try {
    const htmlRes = await axios.get(`http://${host}`, { timeout: 5000 });
    const html = htmlRes.data || '';

    const cssMatch = html.match(/\/(css|static\/css)\/app[^"']+\.css/);
    const jsMatch = html.match(/\/(js|static\/js)\/app[^"']+\.js/);
    const vendorJsMatch = html.match(/\/js\/chunk-vendors[^"']+\.js/);
    const vendorCssMatch = html.match(/\/css\/npm[^"']+\.css/);

    assetCache.assets = {
      css: cssMatch ? cssMatch[0] : '/css/app.css',
      js: jsMatch ? jsMatch[0] : '/js/app.js',
      vendorJs: vendorJsMatch ? vendorJsMatch[0] : null,
      vendorCss: vendorCssMatch ? vendorCssMatch[0] : null
    };
    assetCache.lastFetch = Date.now();
  } catch (_) {
    assetCache.assets = {
      css: '/css/app.css',
      js: '/js/app.js',
      vendorJs: null,
      vendorCss: null
    };
  }

  return assetCache.assets;
}

// ===== 内部服务请求头配置 =====
const INTERNAL_SERVICE_HEADERS = {
  'X-Internal-Service': 'poetize-prerender',
  'User-Agent': 'poetize-prerender/1.0.0'
};

// ===== 文章相关函数 =====
async function fetchArticle(id) {
  try {
    logger.debug('获取文章', { id });
    const res = await axios.get(`${JAVA_BACKEND_URL}/article/getArticleByIdNoCount`, { 
      params: { id },
      timeout: 10000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const article = (res.data && res.data.data) || null;
    logger.debug('文章已获取', { id, found: !!article });
    return article;
  } catch (error) {
    logger.error('获取文章失败', { id, error: error.message, stack: error.stack });
    throw new Error(`获取文章${id}失败: ${error.message}`);
  }
}

async function fetchTranslation(id, lang) {
  // 动态获取源语言配置，而不是硬编码'zh'
  const sourceLanguage = await getSourceLanguage();

  // 如果请求的语言与源语言相同，不需要翻译
  if (lang === sourceLanguage) {
    logger.debug('请求语言与源语言匹配，无需翻译', {
      id,
      requestedLang: lang,
      sourceLanguage
    });
    return null;
  }

  try {
    logger.debug('获取翻译', {
      id,
      lang,
      sourceLanguage,
      needsTranslation: true
    });

    const res = await axios.get(`${JAVA_BACKEND_URL}/article/getTranslation`, {
      params: {
        id: id,
        language: lang
      },
      timeout: 8000,
      headers: INTERNAL_SERVICE_HEADERS
    });

    // 增强响应解析，检查status字段
    const translation = (res.data && res.data.code === 200 && res.data.data && res.data.data.status === 'success')
      ? res.data.data
      : null;

    logger.debug('翻译已获取', {
      id,
      lang,
      sourceLanguage,
      found: !!translation,
      responseCode: res.data?.code,
      responseStatus: res.data?.data?.status
    });
    return translation;
  } catch (error) {
    logger.warn('获取翻译失败，使用原始内容', {
      id,
      lang,
      sourceLanguage,
      error: error.message
    });
    return null;
  }
}

async function fetchMeta(id, lang) {
  try {
    logger.debug('获取元数据', { id, lang });
    
    // 并行获取文章元数据和SEO配置 - 改为调用Java端
    const [articleMetaRes, seoConfigRes] = await Promise.all([
      axios.get(`${JAVA_BACKEND_URL}/seo/getArticleMeta`, { 
        params: { articleId: id, lang },
        timeout: 5000,
        headers: INTERNAL_SERVICE_HEADERS
      }),
      axios.get(`${JAVA_BACKEND_URL}/seo/getSeoConfig/nginx`, { 
        timeout: 5000,
        headers: INTERNAL_SERVICE_HEADERS
      })
    ]);
    
    // 获取文章元数据 - 适配Java端返回格式
    const meta = (articleMetaRes.data && articleMetaRes.data.code === 200) ? (articleMetaRes.data.data || {}) : {};
    logger.debug('元数据已获取', { id, lang, keysCount: Object.keys(meta).length });
    
    // 获取SEO配置 - Java端直接返回配置对象
    const seoConfig = seoConfigRes.data || {};
    
    // 使用通用函数添加图标字段
    addSeoIconFieldsToMeta(meta, seoConfig);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    logger.debug('已添加图标字段到文章元数据', { 
      articleId: id, 
      lang,
      hasSiteIcon: !!meta.site_icon
    });
    
    return meta;
  } catch (error) {
    logger.warn('获取元数据失败，使用默认值', { 
      id, 
      lang, 
      error: error.message 
    });
    return {};
  }
}

// ===== 新增：其他页面数据获取函数 =====

async function fetchWebInfo() {
  try {
    logger.debug('获取网站信息');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/getWebInfo`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const webInfo = (res.data && res.data.data) || {};
    
    // 详细记录获取到的webInfo数据
    logger.info('网站信息获取成功', { 
      status: res.status,
      dataExists: !!res.data,
      webInfoExists: !!res.data?.data,
      keys: Object.keys(webInfo),
      webName: webInfo.webName,
      webTitle: webInfo.webTitle,
      avatar: webInfo.avatar,
      backgroundImage: webInfo.backgroundImage,
      footer: webInfo.footer
    });
    
    return webInfo;
  } catch (error) {
    logger.error('获取网站信息失败', { 
      error: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: `${JAVA_BACKEND_URL}/webInfo/getWebInfo`
    });
    return {};
  }
}

async function fetchSeoConfig() {
  try {
    logger.debug('从服务器获取SEO配置');
    const res = await axios.get(`${JAVA_BACKEND_URL}/seo/getSeoConfig/nginx`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const seoConfig = res.data || {};
    
    logger.info('SEO配置获取成功', { 
      status: res.status,
      responseCode: res.data?.code,
      dataExists: !!res.data?.data,
      keys: Object.keys(seoConfig),
      site_address: seoConfig.site_address,
      og_image: seoConfig.og_image,
      site_icon: seoConfig.site_icon ? '存在' : '不存在',
      apple_touch_icon: seoConfig.apple_touch_icon ? '存在' : '不存在',
      site_icon_192: seoConfig.site_icon_192 ? '存在' : '不存在',
      site_icon_512: seoConfig.site_icon_512 ? '存在' : '不存在',
      site_logo: seoConfig.site_logo ? '存在' : '不存在',
      default_author: seoConfig.default_author,
      custom_head_code: seoConfig.custom_head_code ? `存在(${seoConfig.custom_head_code.length}字符)` : '不存在',
      has_site_verification: !!(seoConfig.google_site_verification || seoConfig.baidu_site_verification)
    });
    
    return seoConfig;
  } catch (error) {
    logger.warn('获取SEO配置失败，使用默认值', { 
      error: error.message, 
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: `${JAVA_BACKEND_URL}/seo/getSeoConfig/nginx`
    });
    
    return {};
  }
}

async function fetchSortInfo() {
  try {
    logger.debug('获取分类信息');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/listSortForPrerender`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const sortInfo = (res.data && res.data.data) || [];
    logger.debug('分类信息已获取', { count: sortInfo.length });
    return sortInfo;
  } catch (error) {
    logger.warn('获取分类信息失败，使用空数组', { error: error.message });
    return [];
  }
}

async function fetchRecentArticles(limit = 5) {
  try {
    logger.debug('获取最新文章', { limit });
    const res = await axios.post(`${JAVA_BACKEND_URL}/article/listArticle`, {
      current: 1,
      size: limit
    }, { 
      timeout: 8000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const articles = (res.data && res.data.data && res.data.data.records) || [];
    logger.debug('最新文章已获取', { count: articles.length, limit });
    return articles;
  } catch (error) {
    logger.warn('获取最新文章失败，使用空数组', { 
      limit, 
      error: error.message 
    });
    return [];
  }
}

async function fetchRecommendArticles(limit = 5) {
  try {
    logger.debug('获取推荐文章', { limit });
    const res = await axios.post(`${JAVA_BACKEND_URL}/article/listArticle`, {
      current: 1,
      size: limit,
      recommendStatus: true
    }, { 
      timeout: 8000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const articles = (res.data && res.data.data && res.data.data.records) || [];
    logger.debug('推荐文章已获取', { count: articles.length, limit });
    return articles;
  } catch (error) {
    logger.warn('获取推荐文章失败，使用空数组', { 
      limit, 
      error: error.message 
    });
    return [];
  }
}

async function fetchCollects() {
  try {
    logger.debug('获取收藏信息');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/listCollect`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const collects = (res.data && res.data.data) || {};
    logger.debug('收藏信息已获取', { categories: Object.keys(collects).length });
    return collects;
  } catch (error) {
    logger.warn('获取收藏信息失败，使用空对象', { error: error.message });
    return {};
  }
}

async function fetchFriends() {
  try {
    logger.debug('获取友链信息');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/listFriend`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const friends = (res.data && res.data.data) || {};
    logger.debug('友链信息已获取', { categories: Object.keys(friends).length });
    return friends;
  } catch (error) {
    logger.warn('获取友链信息失败，使用空对象', { error: error.message });
    return {};
  }
}

async function fetchSiteInfo() {
  try {
    logger.debug('从资源聚合获取站点信息');
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/getSiteInfo`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const siteInfo = (res.data && res.data.data) || {};
    
    logger.info('Site info fetched successfully', { 
      status: res.status,
      dataExists: !!res.data?.data,
      title: siteInfo.title,
      url: siteInfo.url,
      cover: siteInfo.cover,
      introduction: siteInfo.introduction,
      remark: siteInfo.remark
    });
    
    return siteInfo;
  } catch (error) {
    logger.warn('Failed to fetch site info from resource aggregation, using defaults', { 
      error: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: `${JAVA_BACKEND_URL}/webInfo/getSiteInfo`
    });
    return {};
  }
}

async function fetchSortById(sortId) {
  try {
    logger.debug('根据ID获取分类', { sortId });
    // 修改为使用现有的API: /webInfo/getSortInfo 或 /webInfo/listSortForPrerender
    const res = await axios.get(`${JAVA_BACKEND_URL}/webInfo/listSortForPrerender`, { 
      timeout: 5000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    
    // 从返回的分类列表中找到指定ID的分类
    const sortList = (res.data && res.data.data) || [];
    const sort = Array.isArray(sortList) ? sortList.find(s => s.id === parseInt(sortId)) : null;
    
    logger.debug('根据ID获取分类完成', { sortId, found: !!sort, totalSorts: sortList.length });
    return sort;
  } catch (error) {
    logger.error('根据ID获取分类失败', { sortId, error: error.message });
    return null;
  }
}

async function fetchArticlesBySort(sortId, labelId = null, limit = 10) {
  try {
    logger.debug('根据分类获取文章', { sortId, labelId, limit });
    const params = { current: 1, size: limit, sortId };
    if (labelId) params.labelId = labelId;
    
    const res = await axios.post(`${JAVA_BACKEND_URL}/article/listArticle`, params, { 
      timeout: 8000,
      headers: INTERNAL_SERVICE_HEADERS
    });
    const articles = (res.data && res.data.data && res.data.data.records) || [];
    logger.debug('Articles fetched by sort', { 
      sortId, 
      labelId, 
      limit, 
      count: articles.length 
    });
    return articles;
  } catch (error) {
    logger.warn('Failed to fetch articles by sort, using empty array', { 
      sortId, 
      labelId, 
      limit, 
      error: error.message 
    });
    return [];
  }
}

// ===== 通用HTML构建函数 =====
function buildHtmlTemplate({ title, meta, content, lang, pageType = 'article' }) {
  const templatePath = path.resolve('/app/dist/index.html');
  let templateHtml;
  
  if (!fs.existsSync(templatePath)) {
    // 如果挂载路径不存在，尝试相对路径作为fallback
    const fallbackPath = path.resolve(__dirname, './dist/index.html');
    if (!fs.existsSync(fallbackPath)) {
      throw new Error(`在${templatePath}或${fallbackPath}找不到SPA模板。请确保poetize-ui已构建且卷已正确挂载。`);
    }
    console.warn(`使用备用模板路径: ${fallbackPath}`);
    templateHtml = fs.readFileSync(fallbackPath, 'utf8');
  } else {
    templateHtml = fs.readFileSync(templatePath, 'utf8');
  }
  
  // 使用 JSDOM 解析 HTML
  const dom = new JSDOM(templateHtml);
  const document = dom.window.document;
  const Node = dom.window.Node; // 获取Node对象

  // 设置语言属性
  document.documentElement.setAttribute('lang', lang);
  
  // 设置标题
  const titleElement = document.querySelector('head title');
  if (titleElement) {
    titleElement.textContent = title;
  }
  
  // 添加PWA manifest链接到title之前
  try {
    const manifestLink = document.createElement('link');
    manifestLink.setAttribute('rel', 'manifest');
    manifestLink.setAttribute('href', '/manifest.json'); // Java端动态生成，包含完整的PWA配置
    manifestLink.setAttribute('data-prerender-manifest', 'true');
    
    if (document.head && document.head.nodeType === Node.ELEMENT_NODE && titleElement) {
      document.head.insertBefore(manifestLink, titleElement);
      logger.debug('✅ PWA manifest链接已添加到title之前');
    } else if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
      // 如果没有title元素，添加到head末尾以避免破坏HTML结构
      document.head.appendChild(manifestLink);
      logger.debug('✅ PWA manifest链接已添加到head末尾）');
    }
  } catch (e) {
    logger.warn('❌ 添加PWA manifest链接失败', { error: e.message });
  }

  // 清理占位符/旧meta，更彻底
  const removeElements = (selector) => {
    const elements = document.querySelectorAll(selector);
    elements.forEach(el => el.remove());
  };
  
  removeElements('head meta[name="description"]');
  removeElements('head meta[name="keywords"]');
  removeElements('head meta[name="author"]');
  removeElements('head meta[property^="og:"]');
  removeElements('head meta[property^="twitter:"]');
  removeElements('head meta[property^="article:"]');
  removeElements('head meta[property="structured_data"]');
  removeElements('head script[type="application/ld+json"][data-prerender-structured-data="true"]');
  removeElements('head link[rel="canonical"]');
  removeElements('head link[rel="alternate"]');

  // 处理图标字段
  const iconMapping = {
    'site_icon': { rel: 'icon', id: 'seo-favicon' },
    'apple_touch_icon': { rel: 'apple-touch-icon' },
    'site_icon_192': { rel: 'icon', type: 'image/png', sizes: '192x192' },
    'site_icon_512': { rel: 'icon', type: 'image/png', sizes: '512x512' },
    'site_logo': { rel: 'icon', type: 'image/png', sizes: 'any' }
  };
  
  // 如果有site_icon，移除默认的favicon
  if (meta && meta.site_icon) {
    removeElements('head link[rel="icon"]');
    removeElements('head link[id="default-favicon"]');
    logger.info('已移除默认favicon以便替换');
  }
  
  // 添加各种图标
  if (meta) {
    Object.keys(iconMapping).forEach(field => {
      if (meta[field]) {
        const attrs = iconMapping[field];
        const linkElement = document.createElement('link');
        linkElement.href = meta[field];
        
        // 添加所有属性
        Object.keys(attrs).forEach(attr => {
          linkElement.setAttribute(attr, attrs[attr]);
        });
        
        // 安全地插入到title之前，避免appendChild在文本节点上的错误
        try {
          if (document.head && document.head.nodeType === Node.ELEMENT_NODE && titleElement) {
            document.head.insertBefore(linkElement, titleElement);
            logger.debug(`已添加${field}图标到title之前`, { url: meta[field] });
          } else if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
            // 如果没有title元素，添加到head末尾作为fallback
            document.head.appendChild(linkElement);
            logger.debug(`已添加${field}图标到head末尾（fallback）`, { url: meta[field] });
          } else {
            logger.warn(`无法添加${field}图标 - document.head不是元素节点`);
          }
        } catch (error) {
          logger.warn(`添加${field}图标失败`, { error: error.message, url: meta[field] });
        }
      }
    });
  }

  // 注入新的meta，一次一个，更安全
  if (typeof meta === 'object' && meta !== null) {
    for (const key in meta) {
      if (!meta.hasOwnProperty(key)) continue;

      if (key === 'structured_data') {
        continue;
      }

      const value = (meta[key] || '').toString().replace(/"/g, '&quot;');
      
      if (key === 'title') {
        // title已在上面处理
        continue;
      } else if (Object.keys(iconMapping).includes(key)) {
        // 图标已在上面处理
        continue;
      } else if (key.startsWith('hreflang_')) {
        // hreflang 链接跳过DOM处理，稍后在序列化后直接插入HTML字符串
        continue;
      } else if (key === 'canonical') {
        const canonicalLink = document.createElement('link');
        canonicalLink.rel = 'canonical';
        canonicalLink.href = value;
        
        // 安全地插入到title之前
        try {
          if (titleElement && titleElement.parentNode && titleElement.parentNode.nodeType === Node.ELEMENT_NODE) {
            titleElement.parentNode.insertBefore(canonicalLink, titleElement);
          } else if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
            document.head.appendChild(canonicalLink);
          } else {
            logger.warn('无法添加canonical链接 - 找不到合适的父元素节点');
          }
        } catch (error) {
          logger.warn('添加canonical链接失败', { error: error.message, href: value });
        }
      } else if (['description', 'keywords', 'author'].includes(key)) {
        // 跳过空值的 meta 标签
        if (value && value.trim() !== '') {
          const metaElement = document.createElement('meta');
          metaElement.name = key;
          metaElement.content = value;
          
          // 安全地添加到head
          try {
            if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
              document.head.appendChild(metaElement);
            } else {
              logger.warn(`无法添加meta标签${key} - document.head不是元素节点`);
            }
          } catch (error) {
            logger.warn(`添加meta标签${key}失败`, { error: error.message, value });
          }
        }
      } else if (key === 'custom_head_code') {
        // 跳过 custom_head_code，它将在后面的专门逻辑中处理
        logger.debug('跳过custom_head_code，将在专门的处理逻辑中处理');
      } else {
        // 处理 og:, twitter:, article: 等属性，但跳过空值
        if (value && value.trim() !== '') {
          const metaElement = document.createElement('meta');
          metaElement.setAttribute('property', key);
          metaElement.content = value;
          
          // 安全地添加到head
          try {
            if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
              document.head.appendChild(metaElement);
            } else {
              logger.warn(`无法添加property meta标签${key} - document.head不是元素节点`);
            }
          } catch (error) {
            logger.warn(`添加property meta标签${key}失败`, { error: error.message, value });
          }
        }
      }
    }

    if (meta.structured_data && meta.structured_data.toString().trim() !== '') {
      try {
        const structuredDataScript = document.createElement('script');
        structuredDataScript.setAttribute('type', 'application/ld+json');
        structuredDataScript.setAttribute('data-prerender-structured-data', 'true');
        structuredDataScript.textContent =
          typeof meta.structured_data === 'string'
            ? meta.structured_data
            : JSON.stringify(meta.structured_data);

        if (document.head && document.head.nodeType === Node.ELEMENT_NODE && titleElement) {
          document.head.insertBefore(structuredDataScript, titleElement);
        } else if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
          document.head.appendChild(structuredDataScript);
        }
      } catch (e) {
        logger.warn('注入JSON-LD结构化数据失败', { error: e.message });
      }
    }
    
    // 处理搜索引擎验证标签
    const verificationTags = [
      'google_site_verification',
      'baidu_site_verification', 
      'bing_site_verification',
      'yandex_site_verification',
      'sogou_site_verification',
      'so_site_verification',
      'shenma_site_verification',
      'yahoo_site_verification',
      'duckduckgo_site_verification'
    ];
    
    verificationTags.forEach(tagKey => {
      if (meta[tagKey] && meta[tagKey].trim() !== '') {
        try {
          const tagValue = meta[tagKey].trim();
          
          if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
            // 如果值是完整的HTML标签（以<开头），则原样插入
            if (tagValue.startsWith('<')) {
              // 使用临时容器解析HTML标签，确保安全插入
              const tempDiv = document.createElement('div');
              tempDiv.innerHTML = tagValue;
              const metaElement = tempDiv.firstElementChild;
              if (metaElement) {
                document.head.appendChild(metaElement);
              }
            } else {
              // 向后兼容：如果只是content值，则构造meta标签
              const verificationMeta = document.createElement('meta');
              verificationMeta.setAttribute('name', tagKey.replace(/_/g, '-'));
              verificationMeta.setAttribute('content', tagValue);
              document.head.appendChild(verificationMeta);
            }
            logger.debug('成功添加搜索引擎验证标签', { 
              platform: tagKey,
              isRawHtml: tagValue.startsWith('<'),
              content: tagValue.substring(0, 50) + (tagValue.length > 50 ? '...' : '')
            });
          }
        } catch (e) {
          logger.warn('添加搜索引擎验证标签失败', { 
            platform: tagKey,
            error: e.message 
          });
        }
      }
    });

    // 处理robots meta标签
    if (meta.robots && meta.robots.trim() !== '') {
      try {
        const robotsMeta = document.createElement('meta');
        robotsMeta.setAttribute('name', 'robots');
        robotsMeta.setAttribute('content', meta.robots.trim());
        robotsMeta.setAttribute('data-prerender-robots', 'true');
        
        if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
          document.head.appendChild(robotsMeta);
          logger.debug('成功添加robots meta标签', { content: meta.robots });
        }
      } catch (e) {
        logger.warn('添加robots meta标签失败', { error: e.message });
      }
    }

    // 处理社交媒体验证标签
    const socialMediaTags = {
      // Twitter标签
      'twitter_site': 'twitter:site',
      'twitter_creator': 'twitter:creator',
      
      // Facebook标签
      'fb_app_id': 'fb:app_id',
      'fb_page_url': 'fb:page_url',
      
      // Open Graph标签（property字段）
      'og_type': 'og:type',
      'og_site_name': 'og:site_name',
      
      // LinkedIn标签
      'linkedin_company_id': 'linkedin:company',
      
      // Pinterest标签
      'pinterest_verification': 'p:domain_verify',
      'pinterest_description': 'pinterest:description',
      
      // 小程序标签
      'wechat_miniprogram_id': 'wechat:miniprogram',
      'wechat_miniprogram_path': 'wechat:miniprogram:path',
      'qq_miniprogram_path': 'qq:miniprogram:path'
    };
    
    Object.entries(socialMediaTags).forEach(([configKey, metaName]) => {
      if (meta[configKey] && meta[configKey].trim() !== '') {
        try {
          const socialMeta = document.createElement('meta');
          
          // OpenGraph字段使用property，其他使用name
          if (metaName.startsWith('og:')) {
            socialMeta.setAttribute('property', metaName);
          } else {
            socialMeta.setAttribute('name', metaName);
          }
          
          socialMeta.setAttribute('content', meta[configKey].trim());
          socialMeta.setAttribute('data-prerender-social', 'true');
          
          if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
            document.head.appendChild(socialMeta);
            logger.debug('成功添加社交媒体标签', { field: configKey, metaName });
          }
        } catch (e) {
          logger.warn('添加社交媒体标签失败', { field: configKey, error: e.message });
        }
      }
    });


    // 处理自定义头部代码
    if (meta.custom_head_code && meta.custom_head_code.trim() !== '') {
      try {
        logger.info('处理自定义头部代码', { 
          codeLength: meta.custom_head_code.length,
          preview: meta.custom_head_code.substring(0, 100) + '...'
        });
        
        // 创建临时DOM容器来解析自定义头部代码
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = meta.custom_head_code;
        
        // 遍历解析的元素并添加到head
        Array.from(tempDiv.children).forEach(element => {
          if (element.nodeType === Node.ELEMENT_NODE) {
            // 安全地添加到head
            if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
              try {
                const clonedElement = element.cloneNode(true);
                document.head.appendChild(clonedElement);
                logger.debug('成功添加自定义头部元素到prerender HTML', { 
                  tagName: element.tagName,
                  id: element.id || '无',
                  className: element.className || '无'
                });
              } catch (e) {
                logger.warn('添加自定义头部元素失败', { 
                  error: e.message, 
                  tagName: element.tagName 
                });
              }
            }
          }
        });
        
        // 如果是纯文本/脚本内容（没有HTML标签），包装在script标签中
        const trimmedCode = meta.custom_head_code.trim();
        if (trimmedCode && !trimmedCode.includes('<') && !trimmedCode.includes('>')) {
          const scriptElement = document.createElement('script');
          scriptElement.textContent = trimmedCode;
          
          if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
            try {
              document.head.appendChild(scriptElement);
              logger.debug('成功添加自定义脚本到prerender HTML');
            } catch (e) {
              logger.warn('添加自定义脚本失败', { error: e.message });
            }
          }
        }
        
      } catch (error) {
        logger.error('处理自定义头部代码失败', { 
          error: error.message,
          codeLength: meta.custom_head_code.length 
        });
      }
    }
    
  } else {
    console.error('元数据不是有效对象:', meta);
  }

  // 添加页面类型标识
  document.body.setAttribute('data-prerender-type', pageType);
  document.body.setAttribute('data-prerender-lang', lang);

  // 添加防止FOUC的关键内联样式
  const criticalCSS = `
    <style>
      /* 防止FOUC的关键样式 */
      html.prerender #app {
        visibility: visible;
        opacity: 1;
      }
      
      html:not(.loaded) #app {
        visibility: hidden;
      }
      
      html.loaded #app {
        visibility: visible;
        opacity: 1;
        transition: opacity 0.3s ease-in-out;
      }
      
      /* 预渲染内容的样式保护 */
      .article-detail, .home-prerender, .favorite-prerender, .sort-prerender, .sort-list-prerender {
        min-height: 200px;
        position: relative;
        opacity: 1;
        transform: translateY(0);
        animation: fadeIn 0.5s ease-in-out;
      }
      
      @keyframes fadeIn {
        from { 
          opacity: 0; 
          transform: translateY(10px); 
        }
        to { 
          opacity: 1; 
          transform: translateY(0); 
        }
      }
      
      /* 骨架屏效果 */
      .article-detail::before, 
      .home-prerender::before, 
      .favorite-prerender::before, 
      .sort-prerender::before,
      .sort-list-prerender::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(90deg, 
          rgba(240, 240, 240, 0.1) 25%, 
          transparent 37%, 
          rgba(240, 240, 240, 0.1) 63%
        );
        animation: shimmer 1.5s ease-in-out infinite;
        z-index: 1;
        opacity: 0;
        transition: opacity 0.3s ease;
        pointer-events: none;
      }
      
      html:not(.loaded) .article-detail::before,
      html:not(.loaded) .home-prerender::before,
      html:not(.loaded) .favorite-prerender::before,
      html:not(.loaded) .sort-prerender::before,
      html:not(.loaded) .sort-list-prerender::before {
        opacity: 1;
      }
      
      @keyframes shimmer {
        0% { transform: translateX(-100%); }
        100% { transform: translateX(100%); }
      }
      
      /* 预渲染内容的响应式保护 */
      @media (max-width: 768px) {
        .article-detail, .home-prerender, .favorite-prerender, .sort-prerender, .sort-list-prerender {
          min-height: 150px;
          padding: 1rem;
        }
      }
    </style>
  `;
  
  // 安全地添加关键CSS
  try {
    if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
      document.head.insertAdjacentHTML('beforeend', criticalCSS);
    } else {
      logger.warn('无法添加关键CSS - document.head不是元素节点');
    }
  } catch (error) {
    logger.warn('添加关键CSS失败', { error: error.message });
  }

  // 添加资源预加载优化 - 在viewport meta标签之后插入
  const viewportMeta = document.querySelector('meta[name="viewport"]');
  if (viewportMeta) {
    try {
      if (viewportMeta.nodeType === Node.ELEMENT_NODE) {
        viewportMeta.insertAdjacentHTML('afterend', `<link rel="dns-prefetch" href="https://cdn.jsdelivr.net">`);
      } else {
        logger.warn('无法添加预加载链接 - viewport meta不是元素节点');
      }
    } catch (error) {
      logger.warn('添加预加载链接失败', { error: error.message });
    }
  }

  // 在修改DOM之前，提取原始HTML中body底部的所有script标签（字符串级别）
  const bodyScriptsMatch = templateHtml.match(/<body[^>]*>([\s\S]*)<\/body>/i);
  let originalBodyScripts = '';
  if (bodyScriptsMatch) {
    const bodyContent = bodyScriptsMatch[1];
    // 提取</div>（#app结束）之后的所有script标签
    const appEndMatch = bodyContent.match(/<div\s+id\s*=\s*["']?app["']?[^>]*>.*?<\/div>([\s\S]*)/i);
    if (appEndMatch) {
      originalBodyScripts = appEndMatch[1];
    }
  }

  // 注入渲染好的内容
  const appElement = document.getElementById('app');
  if (appElement) {
    appElement.innerHTML = content;
    
    // 根据页面类型给#app添加相应的CSS类
    if (pageType === 'article') {
      appElement.classList.add('article-detail');
    }
    // 其他页面类型(home, favorite, sort, sort-list)的包装div已经在HTML模板中处理
  }

  // 添加加载状态管理脚本
  const loadingScript = `
    <script>
      // 防止FOUC的立即执行脚本
      (function() {
        // 标记页面为预渲染状态
        document.documentElement.classList.add('prerender');
        
          // 图片加载状态处理 - 只处理预渲染容器内的图片
         function handleImageLoad() {
           const preRenderContainers = document.querySelectorAll('.article-detail, .home-prerender, .favorite-prerender, .sort-prerender, .sort-list-prerender');
           preRenderContainers.forEach(function(container) {
             const images = container.querySelectorAll('img');
             images.forEach(function(img) {
               // 立即处理已完成加载的图片
               if (img.complete && img.naturalWidth > 0) {
                 img.classList.add('loaded');
               } else if (img.src.startsWith('data:') || img.src.startsWith('blob:')) {
                 // data URL 和 blob URL 立即标记为已加载
                 img.classList.add('loaded');
               } else if (img.src) {
                 // 为未完成加载的图片添加事件监听
                 img.addEventListener('load', function() {
                   this.classList.add('loaded');
                 }, { once: true });
                 img.addEventListener('error', function() {
                   this.classList.add('loaded'); // 即使加载失败也要显示，避免一直隐藏
                   console.warn('图片加载失败，但仍显示:', this.src);
                 }, { once: true });
                 
                 // 设置一个超时，确保图片不会永远隐藏
                 setTimeout(function() {
                   if (!img.classList.contains('loaded')) {
                     img.classList.add('loaded');
                     console.warn('图片加载超时，强制显示:', img.src);
                   }
                 }, 5000); // 5秒超时
               } else {
                 // 没有src的图片直接标记为已加载
                 img.classList.add('loaded');
               }
             });
           });
         }
        
        // 监听资源加载完成
        function markAsLoaded() {
          requestAnimationFrame(function() {
            document.documentElement.classList.add('loaded');
            document.documentElement.classList.remove('prerender');
            handleImageLoad();
          });
        }
        
        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', markAsLoaded);
        } else {
          markAsLoaded();
        }
        
        // Vue应用挂载完成后的回调
        window.addEventListener('app-mounted', function() {
          const app = document.getElementById('app');
          if (app) {
            app.classList.add('loaded');
          }
          handleImageLoad();
        });
        
        // 字体加载完成后的优化
        if (document.fonts) {
          document.fonts.ready.then(function() {
            document.documentElement.classList.add('fonts-loaded');
          });
        }
      })();
    </script>
  `;
  
  // 安全地添加加载脚本
  try {
    if (document.body && document.body.nodeType === Node.ELEMENT_NODE) {
      document.body.insertAdjacentHTML('beforeend', loadingScript);
    } else {
      logger.warn('无法添加加载脚本 - document.body不是元素节点');
    }
  } catch (error) {
    logger.warn('添加加载脚本失败', { error: error.message });
  }
  
  // 确保生成的HTML具有良好的格式
  let html = dom.serialize();
  
  // 恢复原始的body script标签（直接在字符串级别替换，避免JSDOM丢失）
  if (originalBodyScripts.trim()) {
    html = html.replace('</body>', `${originalBodyScripts}</body>`);
    logger.debug('已恢复原始body script标签', { pageType });
  }
  
  // 处理 hreflang 链接（在序列化后直接操作字符串，避免转义问题）
  if (meta && typeof meta === 'object') {
    const hreflangLinks = [];
    Object.keys(meta).forEach(key => {
      if (key.startsWith('hreflang_')) {
        hreflangLinks.push(meta[key]); // 直接使用接口返回的完整HTML标签
      }
    });
    
    if (hreflangLinks.length > 0) {
      const titleMatch = html.match(/<title[^>]*>.*?<\/title>/i);
      if (titleMatch) {
        const allHreflangHTML = hreflangLinks.join('\n  ');
        html = html.replace(titleMatch[0], `${allHreflangHTML}\n  ${titleMatch[0]}`);
       }
    }
  }
  
  // 重排序webpack生成的CSS链接到title标签之前（符合HTML规范）
  const titleMatch = html.match(/<title[^>]*>.*?<\/title>/i);
  // 只匹配webpack生成的CSS文件（通常包含hash或chunk名称，且在/static/目录下）
  const webpackCssMatches = html.match(/<link[^>]*href=["'][^"']*\/static\/[^"']*\.css[^"']*["'][^>]*rel=["']stylesheet["'][^>]*>/gi) || [];
  
  if (titleMatch && webpackCssMatches.length > 0) {
    const titleTag = titleMatch[0];
    
    // 移除webpack生成的CSS链接
    webpackCssMatches.forEach(link => {
      html = html.replace(link, '');
    });
    
    // 在title标签前插入webpack CSS链接
    const cssLinks = webpackCssMatches.join('\n  ');
    html = html.replace(titleTag, `${cssLinks}\n  ${titleTag}`);
    
    logger.debug('已重排序CSS链接到title标签之前', { 
      cssLinksCount: webpackCssMatches.length 
    });
  }
  // 只格式化head部分，避免在body中引入文本节点导致Vue水合失败
  const headEnd = html.indexOf('</head>');
  if (headEnd > 0) {
    let headPart = html.substring(0, headEnd);
    const rest = html.substring(headEnd);
    
    // 格式化head部分
    headPart = headPart.replace(/<meta/g, '\n  <meta');
    headPart = headPart.replace(/<link/g, '\n  <link');
    headPart = headPart.replace(/<style/g, '\n  <style');
    headPart = headPart.replace(/<\/style>/g, '</style>\n');
    
    // 清理head部分的多余空行
    headPart = headPart.replace(/\n\s*\n/g, '\n');
    
    html = headPart + '\n</head>' + rest;
  }
  
  return html;
}

// ===== 文章页面渲染函数 =====
function buildHtml({ title, articleTitle, meta, content, lang }) {

  // 确保meta是一个有效的对象
  const safeMeta = (typeof meta === 'object' && meta !== null) ? meta : {};
  
  // 记录是否包含图标字段
  logger.debug('文章元数据包含图标字段:', {
    hasSiteIcon: !!safeMeta.site_icon,
    hasAppleTouchIcon: !!safeMeta.apple_touch_icon,
    hasSiteIcon192: !!safeMeta.site_icon_192,
    hasSiteIcon512: !!safeMeta.site_icon_512
  });
  
  // SEO优化：在内容前添加文章标题作为唯一的H1标签
  // title: 用于 <title> 标签（文章标题 - 网站名）
  // articleTitle: 用于 <h1> 标签（仅文章标题）
  const h1Title = articleTitle || title;  // 如果没有单独的 articleTitle，使用完整 title
  const contentWithTitle = `<h1 class="article-main-title">${h1Title}</h1>\n${content}`;
  
  return buildHtmlTemplate({ 
    title: title,  // <title> 标签：文章标题 - 网站名
    meta: safeMeta, 
    content: contentWithTitle,  // <h1> 标签：文章标题
    lang: lang || 'zh', 
    pageType: 'article' 
  });
}

// ===== 首页渲染函数 =====
async function renderHomePage(lang = 'zh') {
  try {
    const [webInfo, seoConfig, sortInfo, recentArticles, recommendArticles] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig(),
      fetchSortInfo(), 
      fetchRecentArticles(8),
      fetchRecommendArticles(5)
    ]);

    // 直接使用webInfo的标题数据，简化逻辑
    const title = webInfo.webTitle || webInfo.webName ;
    const description = seoConfig.site_description || `${webInfo.webName} - 个人博客网站，分享技术文章、生活感悟。`;
    const keywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const author = seoConfig.default_author || webInfo.webName || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl);
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': baseUrl,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName , // 优先使用webTitle
      'twitter:card': seoConfig.twitter_card || 'summary_large_image',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    // 使用通用函数添加图标字段
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    // 构建首页内容（压缩格式，避免产生文本节点导致Vue水合失败）
    const homeContent = `<div class="home-prerender"><div class="home-hero"><h1>${webInfo.webName || webInfo.webTitle}</h1><p>${description}</p></div><div class="home-categories"><h2>文章分类</h2><ul>${sortInfo.map(sort => `<li><a href="/sort/${sort.id}" title="${sort.sortDescription || sort.sortName}">${sort.sortName}</a></li>`).join('')}</ul></div>${recommendArticles.length > 0 ? `<div class="home-recommend-articles"><h2>🔥推荐文章</h2><ul>${recommendArticles.map(article => `<li><a href="/article/${article.id}" title="${article.articleTitle}">${article.articleCover ? `<img src="${article.articleCover}" alt="${article.articleTitle}" width="120" height="80" loading="lazy">` : ''}<div class="article-info"><h3>${article.articleTitle}</h3>${article.summary ? `<p>${article.summary}</p>` : ''}<time>${article.createTime}</time></div></a></li>`).join('')}</ul></div>` : ''}<div class="home-recent-articles"><h2>最新文章</h2><ul>${recentArticles.map(article => `<li><a href="/article/${article.id}" title="${article.articleTitle}"><h3>${article.articleTitle}</h3>${article.summary ? `<p>${article.summary}</p>` : ''}<time>${article.createTime}</time></a></li>`).join('')}</ul></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: homeContent, 
      lang, 
      pageType: 'home' 
    });
  } catch (error) {
    console.error('渲染首页失败:', error);
    throw error;
  }
}

// ===== 关于页面渲染函数 =====
async function renderAboutPage(lang = 'zh') {
  try {
    logger.info('开始渲染关于页面', { lang });
    
    // 获取网站基本信息
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `关于我们 - ${webInfo.webTitle || webInfo.webName }`;
    const description = webInfo.about || '了解更多关于我们的信息';
    const keywords = `关于,${webInfo.webName },博客,个人简介`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:image': ensureAbsoluteImageUrl(webInfo.avatar || '/poetize.jpg', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName , // 优先使用webTitle
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(webInfo.avatar || '/poetize.jpg', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const aboutContent = `<div class="about-prerender"><div class="about-hero"><h1>关于${ webInfo.webName || webInfo.webTitle }</h1><p>${description}</p></div><div class="about-content"><div class="about-info">${webInfo.about ? `<div class="about-text">${webInfo.about}</div>` : ''}<div class="contact-info"><h3>联系方式</h3><p>邮箱: ${webInfo.email || '暂未提供'}</p></div></div></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: aboutContent, lang, pageType: 'about' });
  } catch (error) {
    logger.error('渲染关于页面失败:', error);
    throw error;
  }
}

// ===== 留言板页面渲染函数 =====
async function renderMessagePage(lang = 'zh') {
  try {
    logger.info('开始渲染留言板页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `留言板 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '欢迎在这里留下您的宝贵意见和建议';
    const keywords = `留言,反馈,建议,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/message`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const messageContent = `<div class="message-prerender"><div class="message-hero"><h1>留言板</h1><p>${description}</p></div><div class="message-form-placeholder"><p>留言功能将在页面加载完成后可用</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: messageContent, lang, pageType: 'message' });
  } catch (error) {
    logger.error('渲染留言板页面失败:', error);
    throw error;
  }
}

// ===== 微言页面渲染函数 =====
async function renderWeiYanPage(lang = 'zh') {
  try {
    logger.info('开始渲染微言页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `微言 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '记录生活点滴，分享心情随笔';
    const keywords = `微言,动态,心情,随笔,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/weiYan`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const weiYanContent = `<div class="weiyan-prerender"><div class="weiyan-hero"><h1>微言</h1><p>${description}</p></div><div class="weiyan-list-placeholder"><p>动态内容将在页面加载完成后显示</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: weiYanContent, lang, pageType: 'weiyan' });
  } catch (error) {
    logger.error('渲染微言页面失败:', error);
    throw error;
  }
}

// ===== 恋爱记录页面渲染函数 =====
async function renderLovePage(lang = 'zh') {
  try {
    logger.info('开始渲染恋爱记录页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `恋爱记录 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '记录美好的爱情时光';
    const keywords = `恋爱,爱情,记录,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/love`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const loveContent = `<div class="love-prerender"><div class="love-hero"><h1>恋爱记录</h1><p>${description}</p></div><div class="love-timeline-placeholder"><p>爱情时光轴将在页面加载完成后显示</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: loveContent, lang, pageType: 'love' });
  } catch (error) {
    logger.error('渲染恋爱记录页面失败:', error);
    throw error;
  }
}

// ===== 旅行日记页面渲染函数 =====
async function renderTravelPage(lang = 'zh') {
  try {
    logger.info('开始渲染旅行日记页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `旅行日记 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '记录旅途中的美好时光和所见所闻';
    const keywords = `旅行,日记,游记,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/travel`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const travelContent = `<div class="travel-prerender"><div class="travel-hero"><h1>旅行日记</h1><p>${description}</p></div><div class="travel-list-placeholder"><p>旅行记录将在页面加载完成后显示</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: travelContent, lang, pageType: 'travel' });
  } catch (error) {
    logger.error('渲染旅行日记页面失败:', error);
    throw error;
  }
}

// ===== 隐私政策页面渲染函数 =====
async function renderPrivacyPage(lang = 'zh') {
  try {
    logger.info('开始渲染隐私政策页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `隐私政策 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '了解我们如何保护您的个人隐私信息';
    const keywords = `隐私政策,隐私保护,个人信息,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'article',
      'og:url': `${baseUrl}/privacy`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const privacyContent = `<div class="privacy-prerender"><div class="privacy-hero"><h1>隐私政策</h1><p>${description}</p></div><div class="privacy-content"><p>我们重视您的隐私，并致力于保护您的个人信息安全。</p><p>详细的隐私政策内容将在页面加载完成后显示。</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: privacyContent, lang, pageType: 'privacy' });
  } catch (error) {
    logger.error('渲染隐私政策页面失败:', error);
    throw error;
  }
}


// ===== 信件页面渲染函数 =====
async function renderLetterPage(lang = 'zh') {
  try {
    logger.info('开始渲染信件页面', { lang });
    
    const webInfo = await fetchWebInfo();
    const seoConfig = await fetchSeoConfig();
    
    const title = `信件 - ${webInfo.webTitle || webInfo.webName }`;
    const description = '查看和管理您的信件';
    const keywords = `信件,私信,消息,${webInfo.webName }`;
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    
    const meta = {
      description,
      keywords,
      author: webInfo.author ,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/letter`,
      'og:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl),
      'og:site_name': webInfo.webTitle || webInfo.webName ,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl)
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);
    
    const letterContent = `<div class="letter-prerender"><div class="letter-hero"><h1>信件</h1><p>${description}</p></div><div class="letter-list-placeholder"><p>信件内容将在页面加载完成后显示</p></div></div>`;
    
    return buildHtmlTemplate({ title, meta, content: letterContent, lang, pageType: 'letter' });
  } catch (error) {
    logger.error('渲染信件页面失败:', error);
    throw error;
  }
}


// ===== 友人帐页面渲染函数 =====
async function renderFriendsPage(lang = 'zh') {
  try {
    const [webInfo, seoConfig, friends, siteInfo] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig(),
      fetchFriends(),
      fetchSiteInfo()
    ]);

    const siteName = webInfo.webTitle || webInfo.webName;
    const title = `友人帐 - ${siteName}`;
    const description = '留下你的网站吧，让我们建立友谊的桥梁';
    const author = webInfo.webName || seoConfig.default_author || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(webInfo.avatar || seoConfig.og_image || '', baseUrl);
    
    const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const keywords = `${baseKeywords},友人帐,友链,朋友,网站交换`;
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/friends`,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    // 友链分类的标准key映射（兼容旧的emoji和新的emoji）
    const eliteFriendsKey = friends['🌟青出于蓝'] ? '🌟青出于蓝' : (friends['♥️青出于蓝'] || null);
    const regularFriendsKey = friends['🥇友情链接'] ? '🥇友情链接' : null;
    
    const friendsContent = `<div class="friends-prerender"><h1>友人帐</h1><p>留下你的网站吧，让我们建立友谊的桥梁</p>${eliteFriendsKey && friends[eliteFriendsKey] && friends[eliteFriendsKey].length > 0 ? `<h2>🌟青出于蓝</h2><ul>${friends[eliteFriendsKey].map(friend => `<li><a href="${friend.url}" target="_blank" rel="noopener" title="${friend.introduction}">${friend.title} - ${friend.introduction}</a></li>`).join('')}</ul>` : ''}${regularFriendsKey && friends[regularFriendsKey] && friends[regularFriendsKey].length > 0 ? `<h2>🥇友情链接</h2><ul>${friends[regularFriendsKey].map(friend => `<li><a href="${friend.url}" target="_blank" rel="noopener" title="${friend.introduction}">${friend.title} - ${friend.introduction}</a></li>`).join('')}</ul>` : ''}<h2>✉️ 申请方式</h2><div><p>1. 添加本站链接</p><p>首先将本站链接添加至您的网站，信息如下：</p><p>网站名称：${siteInfo.title || webInfo.webName}</p><p>网站地址：${baseUrl}</p><p>网站描述：${siteInfo.introduction || webInfo.webTitle}</p><p>网站封面：${siteInfo.remark || ''}</p></div><div><p>2. 提交申请</p><p>点击下方信封 📮 填写您的网站信息提交申请</p></div><div><p>3. 等待审核</p><p>审核通过后将会添加至该页面中，请耐心等待</p></div><h2>⚠️ 温馨提示</h2><ul><li>不会添加带有广告营销和没有实质性内容的友链</li><li>申请之前请将本网站添加为您的友链</li><li>审核时间一般在一周内，请耐心等待</li></ul>${!eliteFriendsKey && !regularFriendsKey ? '<p>暂无友链，欢迎交换友链</p>' : ''}<div id="dynamic-content-placeholder" style="display:none;"><script>window.PRERENDER_DATA = {type: 'friends',lang: '${lang}',timestamp: ${Date.now()}};</script></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: friendsContent, 
      lang, 
      pageType: 'friends' 
    });
  } catch (error) {
    console.error('渲染友人帐页面失败:', error);
    throw error;
  }
}

// ===== 曲乐页面渲染函数 =====
async function renderMusicPage(lang = 'zh') {
  try {
    const [webInfo, seoConfig] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig()
    ]);

    const siteName = webInfo.webTitle || webInfo.webName;
    const title = `曲乐 - ${siteName}`;
    const description = '一曲肝肠断，天涯何处觅知音';
    const author = webInfo.webName || seoConfig.default_author || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(webInfo.avatar || seoConfig.og_image || '', baseUrl);
    
    const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const keywords = `${baseKeywords},曲乐,音乐,娱乐,音频`;
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/music`,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    const musicContent = `<div class="music-prerender"><div class="music-hero"><h1>曲乐</h1><p>一曲肝肠断，天涯何处觅知音</p></div><div class="music-main"><div class="music-placeholder"><p>音乐内容将在页面加载完成后显示</p></div></div><div id="dynamic-content-placeholder" style="display:none;"><script>window.PRERENDER_DATA = {type: 'music',lang: '${lang}',timestamp: ${Date.now()}};</script></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: musicContent, 
      lang, 
      pageType: 'music' 
    });
  } catch (error) {
    console.error('渲染曲乐页面失败:', error);
    throw error;
  }
}

// ===== 收藏夹页面渲染函数 =====
async function renderFavoritesPage(lang = 'zh') {
  try {
    const [webInfo, seoConfig, collects] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig(),
      fetchCollects()
    ]);

    const siteName = webInfo.webTitle || webInfo.webName;
    const title = `收藏夹 - ${siteName}`;
    const description = '将本网站添加到您的收藏夹吧，发现更多精彩内容';
    const author = webInfo.webName || seoConfig.default_author || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(webInfo.avatar || seoConfig.og_image || '', baseUrl);
    
    const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const keywords = `${baseKeywords},收藏夹,书签,网站收藏,精选网站`;
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/favorites`,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName,
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    const favoritesContent = `<div class="favorites-prerender"><div class="favorites-hero"><h1>收藏夹</h1><p>将本网站添加到您的收藏夹吧，发现更多精彩内容</p></div><div class="favorites-main">${Object.keys(collects).length > 0 ? Object.keys(collects).map(category => `<div class="collect-category"><h3>${category}</h3><ul>${collects[category].map(item => `<li><a href="${item.url}" target="_blank" rel="noopener" title="${item.introduction}"><img src="${item.cover}" alt="${item.title}" width="32" height="32" loading="lazy"><span>${item.title}</span><small>${item.introduction}</small></a></li>`).join('')}</ul></div>`).join('') : '<p>暂无收藏夹</p>'}</div><div id="dynamic-content-placeholder" style="display:none;"><script>window.PRERENDER_DATA = {type: 'favorites',lang: '${lang}',timestamp: ${Date.now()}};</script></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: favoritesContent, 
      lang, 
      pageType: 'favorites' 
    });
  } catch (error) {
    console.error('渲染收藏夹页面失败:', error);
    throw error;
  }
}


// ===== 默认分类页面渲染函数（显示所有分类列表）=====
async function renderDefaultSortPage(lang = 'zh') {
  try {
    const [webInfo, seoConfig, sortList] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig(),
      fetchSortInfo() // 获取所有分类信息
    ]);

    const siteName = webInfo.webTitle || webInfo.webName;
    const title = `文章分类 - ${siteName}`;
    const description = '浏览所有文章分类，找到您感兴趣的内容主题';
    const author = seoConfig.default_author || webInfo.webName || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl);
    
    // 在基础关键词基础上添加页面特定关键词
    const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const keywords = `${baseKeywords},文章分类,分类列表,内容导航`;
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/sort`,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName, // 优先使用webTitle
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    // 使用通用函数添加图标字段
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    // 构建默认分类页面内容
    const defaultSortContent = `<div class="sort-list-prerender"><div class="sort-hero"><h1>文章分类</h1><p>探索不同主题的文章内容</p></div><div class="sort-categories">${Array.isArray(sortList) && sortList.length > 0 ? `<div class="categories-grid">${sortList.map(sort => `<div class="category-card"><a href="/sort/${sort.id}" title="${sort.sortDescription || sort.sortName}"><h3>${sort.sortName}</h3><p>${sort.sortDescription || '暂无描述'}</p><div class="category-stats"><span class="article-count">${sort.countOfSort || 0} 篇文章</span>${sort.labels && sort.labels.length > 0 ? `<span class="label-count">${sort.labels.length} 个标签</span>` : ''}</div>${sort.labels && sort.labels.length > 0 ? `<div class="category-labels">${sort.labels.slice(0, 3).map(label => `<span class="label-tag">${label.labelName}</span>`).join('')}${sort.labels.length > 3 ? '<span class="label-more">...</span>' : ''}</div>` : ''}</a></div>`).join('')}</div>` : '<p class="no-categories">暂无分类</p>'}</div><div id="dynamic-content-placeholder" style="display:none;"><script>window.PRERENDER_DATA = {type: 'sort-list',lang: '${lang}',timestamp: ${Date.now()}};</script></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: defaultSortContent, 
      lang, 
      pageType: 'sort-list' 
    });
  } catch (error) {
    console.error('渲染默认分类页失败:', error);
    throw error;
  }
}

// ===== 分类页面渲染函数 =====
async function renderSortPage(sortId, labelId = null, lang = 'zh') {
  try {
    // 并行获取多个数据源
    const [webInfo, seoConfig, sortData, articles] = await Promise.all([
      fetchWebInfo(),
      fetchSeoConfig(),
      fetchSortById(sortId),
      fetchArticlesBySort(sortId, labelId, 20)
    ]);

    if (!sortData) {
      throw new Error(`分类${sortId}未找到`);
    }

    const siteName = webInfo.webTitle || webInfo.webName ;
    const title = `${sortData.sortName} - ${siteName}`;
    const description = sortData.sortDescription || `${sortData.sortName}分类下的所有文章`;
    const author = seoConfig.default_author || webInfo.webName || 'Admin';
    const baseUrl = webInfo.siteAddress || process.env.SITE_URL || 'http://localhost';
    const ogImage = ensureAbsoluteImageUrl(seoConfig.og_image || webInfo.avatar || '', baseUrl);
    
    // 在基础关键词基础上添加分类特定关键词
    const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
    const keywords = `${baseKeywords},${sortData.sortName},文章分类,博客`;
    
    const meta = {
      description,
      keywords,
      author,
      'og:title': title,
      'og:description': description,
      'og:type': 'website',
      'og:url': `${baseUrl}/sort/${sortId}${labelId ? `?labelId=${labelId}` : ''}`,
      'og:image': ogImage,
      'og:site_name': webInfo.webTitle || webInfo.webName , // 优先使用webTitle
      'twitter:card': seoConfig.twitter_card || 'summary',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage
    };
    
    // 使用通用函数添加图标字段
    addSeoIconFieldsToMeta(meta, seoConfig, baseUrl);
    addSearchEngineVerificationTags(meta, seoConfig);

    // 构建分类页面内容
    const sortContent = `<div class="sort-prerender"><div class="sort-hero"><h1>${sortData.sortName}</h1><p>${sortData.sortDescription || ''}</p></div><div class="sort-articles"><h2>文章列表</h2>${articles.length > 0 ? `<ul class="article-list">${articles.map(article => `<li class="article-item"><a href="/article/${article.id}" title="${article.articleTitle}">${article.articleCover ? `<img src="${article.articleCover}" alt="${article.articleTitle}" loading="lazy">` : ''}<div class="article-info"><h3>${article.articleTitle}</h3>${article.summary ? `<p>${article.summary}</p>` : ''}<div class="article-meta"><time>${article.createTime}</time><span class="view-count">阅读 ${article.viewCount || 0}</span>${article.label ? `<span class="label">${article.label.labelName}</span>` : ''}</div></div></a></li>`).join('')}</ul>` : '<p>暂无文章</p>'}</div>${sortData.labels && sortData.labels.length > 0 ? `<div class="sort-labels"><h3>标签筛选</h3><ul>${sortData.labels.map(label => `<li><a href="/sort/${sortId}?labelId=${label.id}" title="${label.labelDescription || label.labelName}">${label.labelName} (${label.countOfLabel || 0})</a></li>`).join('')}</ul></div>` : ''}<div id="dynamic-content-placeholder" style="display:none;"><script>window.PRERENDER_DATA = {type: 'sort',sortId: ${sortId},labelId: ${labelId || 'null'},lang: '${lang}',timestamp: ${Date.now()}};</script></div></div>`;

    return buildHtmlTemplate({ 
      title, 
      meta, 
      content: sortContent, 
      lang, 
      pageType: 'sort' 
    });
  } catch (error) {
    console.error(`渲染分类页${sortId}失败:`, error);
    throw error;
  }
}

// ===== 文章渲染函数 =====
function resolveOutputRoot(outputRoot) {
  return outputRoot || process.env.PRERENDER_OUTPUT || path.resolve(__dirname, './dist/prerender');
}

function resolveIndexFilename(lang) {
  return lang === 'zh' ? 'index.html' : `index-${lang}.html`;
}

function writeRenderedHtml(outputPath, lang, html) {
  fs.mkdirSync(outputPath, { recursive: true });
  const filename = resolveIndexFilename(lang);
  const filePath = path.join(outputPath, filename);
  fs.writeFileSync(filePath, html, 'utf8');
  return { filename, filePath };
}

function normalizeLanguagesInput(languages, defaultLanguages = ['zh']) {
  return Array.isArray(languages) && languages.length > 0 ? languages : defaultLanguages;
}

function filterSupportedLanguages(languages, supportedLanguages) {
  const supported = new Set(supportedLanguages);
  return languages.filter(lang => supported.has(lang));
}

async function renderArticleVariant({ taskId, OUTPUT_ROOT, seoConfig, webInfo }, { articleId, article, lang }) {
  logger.debug('渲染文章', { taskId, articleId, lang });

  let contentHtml = article.articleContent || '';
  let articleTitle = article.articleTitle || '';

  const translation = await fetchTranslation(articleId, lang);
  if (translation) {
    if (translation.content) contentHtml = translation.content;
    if (translation.title) articleTitle = translation.title;
    logger.debug('翻译已应用', { taskId, articleId, lang });
  }

  contentHtml = decodeHtmlEntities(contentHtml);
  contentHtml = md.render(contentHtml);
  logger.debug('Markdown内容已渲染为HTML', { taskId, articleId, lang });

  const articleMeta = await fetchMeta(articleId, lang);

  const siteName = webInfo.webTitle || webInfo.webName;
  const baseKeywords = seoConfig.site_keywords || '博客,个人网站,技术分享';
  const author = seoConfig.default_author || webInfo.webName || 'Admin';
  const baseUrl = seoConfig.site_address || process.env.SITE_URL;

  const meta = {
    ...articleMeta,
    author: articleMeta.author || author,
    keywords: articleMeta.keywords || baseKeywords,
    'og:site_name': webInfo.webTitle || webInfo.webName,
    'og:url': articleMeta['og:url'],
    'og:image': ensureAbsoluteImageUrl(articleMeta['og:image'] || seoConfig.og_image || webInfo.avatar || '', baseUrl),
    'twitter:card': seoConfig.twitter_card || 'summary_large_image',
    'twitter:site': seoConfig.twitter_site || '',
    'twitter:image': ensureAbsoluteImageUrl(articleMeta['og:image'] || seoConfig.og_image || webInfo.avatar || '', baseUrl)
  };

  logger.info('buildHtml前的元数据对象', {
    taskId,
    articleId,
    lang,
    metaType: typeof meta,
    metaKeys: Object.keys(meta),
    metaSample: Object.keys(meta).slice(0, 3).reduce((obj, key) => {
      obj[key] = meta[key];
      return obj;
    }, {})
  });

  const pageTitle = `${meta.title || articleTitle} - ${siteName}`;

  const html = buildHtml({
    title: pageTitle,
    articleTitle: meta.title || articleTitle,
    meta,
    content: contentHtml,
    lang
  });

  const outputPath = path.join(OUTPUT_ROOT, 'article', articleId.toString());
  const { filename } = writeRenderedHtml(outputPath, lang, html);

  logger.debug('文章渲染成功', {
    taskId,
    articleId,
    lang,
    filePath: `${outputPath}/${filename}`,
    size: `${(html.length / 1024).toFixed(1)}KB`
  });

  return { outputPath, filename, htmlSizeKb: `${(html.length / 1024).toFixed(1)}KB` };
}

async function safeRenderArticleVariant(context, payload) {
  try {
    const result = await renderArticleVariant(context, payload);
    return { ok: true, result };
  } catch (error) {
    logger.error('文章渲染失败', {
      taskId: context.taskId,
      articleId: payload.articleId,
      lang: payload.lang,
      error: error.message,
      stack: error.stack
    });
    return { ok: false, error };
  }
}

async function renderIds(ids = [], options = {}) {
  if (!Array.isArray(ids) || ids.length === 0) {
    throw new Error('ids必须是非空数组');
  }

  const taskId = generateTaskId();
  monitor.recordRenderStart(taskId, 'article', { ids, options });

  const OUTPUT_ROOT = resolveOutputRoot(options.outputRoot);

  const requestedLanguages = normalizeLanguagesInput(options.languages);

  // 支持的语言列表（用于验证）
  const ALL_SUPPORTED_LANGUAGES = ['zh', 'en', 'ja', 'zh-TW', 'ko', 'fr', 'de', 'es', 'ru'];

  // 验证传入的语言是否支持
  const validLanguages = filterSupportedLanguages(requestedLanguages, ALL_SUPPORTED_LANGUAGES);
  if (validLanguages.length === 0) {
    throw new Error(`在以下语言中未找到支持的语言: ${requestedLanguages.join(', ')}。支持的语言: ${ALL_SUPPORTED_LANGUAGES.join(', ')}`);
  }

  try {
    logger.info('开始文章渲染', {
      taskId,
      articleCount: ids.length,
      requestedLanguages: requestedLanguages,
      validLanguages: validLanguages
    });

    const assets = await getFrontEndAssets(options.frontendHost || 'nginx');
    logger.debug('前端资源已加载', { taskId, assets });

    // 获取SEO配置和网站信息，所有文章共用
    const [seoConfig, webInfo] = await Promise.all([
      fetchSeoConfig(),
      fetchWebInfo()
    ]);
    
    // 调试：记录获取到的webInfo数据
    logger.debug('文章的网站信息数据', { 
      taskId, 
      webInfoKeys: Object.keys(webInfo),
      webName: webInfo.webName,
      webTitle: webInfo.webTitle,
      avatar: webInfo.avatar
    });

    // 调试：检查CSS文件是否存在
    const distPath = '/app/dist';
    const staticCssPath = path.join(distPath, 'static', 'css');
    logger.info('检查CSS文件可用性', {
      taskId,
      distPathExists: fs.existsSync(distPath),
      staticCssPathExists: fs.existsSync(staticCssPath),
      distContents: fs.existsSync(distPath) ? fs.readdirSync(distPath) : [],
      staticCssContents: fs.existsSync(staticCssPath) ? fs.readdirSync(staticCssPath).filter(f => f.endsWith('.css')) : []
    });

    let successCount = 0;
    let failCount = 0;
    const errors = [];

    for (const id of ids) {
      const article = await fetchArticle(id);
      if (!article) {
        logger.warn('文章未找到，跳过', { taskId, articleId: id });
        continue;
      }

      const context = { taskId, OUTPUT_ROOT, assets, seoConfig, webInfo };
      for (const lang of validLanguages) {
        const attempt = await safeRenderArticleVariant(context, { articleId: id, article, lang });
        if (attempt.ok) {
          successCount++;
        } else {
          failCount++;
          errors.push({ articleId: id, lang, error: attempt.error.message });
        }
      }
    }

    if (errors.length > 0 && successCount === 0) {
      throw new Error(`所有渲染都失败了。错误: ${JSON.stringify(errors)}`);
    }

    monitor.recordRenderSuccess(taskId, {
      count: ids.length,
      successCount,
      failCount,
      languages: validLanguages.length
    });

    logger.info('文章渲染已完成', {
      taskId,
      totalArticles: ids.length,
      renderedLanguages: validLanguages,
      successCount,
      failCount,
      errorCount: errors.length
    });

  } catch (error) {
    monitor.recordRenderFailure(taskId, error);
    throw error;
  }
}

// ===== 页面类型渲染函数 =====

// 辅助函数：渲染单个分类页面（用于批量渲染，不创建新任务）
async function renderSingleSortPage(sortId, parentTaskId = null) {
  const OUTPUT_ROOT = process.env.PRERENDER_OUTPUT || path.resolve(__dirname, './dist/prerender');
  // 分类页面只生成中文版
  const langs = ['zh'];
  
  for (const lang of langs) {
    const html = await renderSortPage(sortId, null, lang);
    
    const outputPath = path.join(OUTPUT_ROOT, 'sort', sortId.toString());
    fs.mkdirSync(outputPath, { recursive: true });
    
    const filename = lang === 'zh' ? 'index.html' : `index-${lang}.html`;
    const filePath = path.join(outputPath, filename);
    fs.writeFileSync(filePath, html, 'utf8');
    
    logger.debug('分类页面已渲染', { 
      parentTaskId, 
      sortId, 
      lang, 
      filePath: `${outputPath}/${filename}`,
      size: `${(html.length / 1024).toFixed(1)}KB`
    });
  }
}

async function resolvePageVariant({ type, lang, params, OUTPUT_ROOT }) {
  const renderers = {
    home: async () => ({ html: await renderHomePage(lang), outputPath: path.join(OUTPUT_ROOT, 'home') }),
    friends: async () => ({ html: await renderFriendsPage(lang), outputPath: path.join(OUTPUT_ROOT, 'friends') }),
    music: async () => ({ html: await renderMusicPage(lang), outputPath: path.join(OUTPUT_ROOT, 'music') }),
    favorites: async () => ({ html: await renderFavoritesPage(lang), outputPath: path.join(OUTPUT_ROOT, 'favorites') }),
    sort: async () => {
      const { sortId, labelId } = params || {};
      if (!sortId) {
        return { html: await renderDefaultSortPage(lang), outputPath: path.join(OUTPUT_ROOT, 'sort') };
      }
      let outputPath = path.join(OUTPUT_ROOT, 'sort', sortId.toString());
      if (labelId) outputPath = path.join(outputPath, labelId.toString());
      return { html: await renderSortPage(sortId, labelId, lang), outputPath };
    },
    about: async () => ({ html: await renderAboutPage(lang), outputPath: path.join(OUTPUT_ROOT, 'about') }),
    message: async () => ({ html: await renderMessagePage(lang), outputPath: path.join(OUTPUT_ROOT, 'message') }),
    weiYan: async () => ({ html: await renderWeiYanPage(lang), outputPath: path.join(OUTPUT_ROOT, 'weiYan') }),
    love: async () => ({ html: await renderLovePage(lang), outputPath: path.join(OUTPUT_ROOT, 'love') }),
    travel: async () => ({ html: await renderTravelPage(lang), outputPath: path.join(OUTPUT_ROOT, 'travel') }),
    privacy: async () => ({ html: await renderPrivacyPage(lang), outputPath: path.join(OUTPUT_ROOT, 'privacy') }),
    letter: async () => ({ html: await renderLetterPage(lang), outputPath: path.join(OUTPUT_ROOT, 'letter') })
  };

  const renderer = renderers[type];
  if (!renderer) throw new Error(`未知页面类型: ${type}`);
  return renderer();
}

async function safeRenderPageVariant({ taskId, type, lang, params, OUTPUT_ROOT }) {
  try {
    logger.debug('渲染页面', { taskId, type, lang, params });
    const { html, outputPath } = await resolvePageVariant({ type, lang, params, OUTPUT_ROOT });
    const { filename } = writeRenderedHtml(outputPath, lang, html);
    const size = `${(html.length / 1024).toFixed(1)}KB`;
    logger.debug('页面渲染成功', {
      taskId,
      type,
      lang,
      filePath: `${outputPath}/${filename}`,
      size
    });
    return { ok: true, result: { lang, path: `${outputPath}/${filename}`, size } };
  } catch (error) {
    logger.error('页面渲染失败', {
      taskId,
      type,
      lang,
      params,
      error: error.message,
      stack: error.stack
    });
    return { ok: false, error };
  }
}

async function renderPages(type, params = {}) {
  const taskId = generateTaskId();
  monitor.recordRenderStart(taskId, type, { type, params });

  const OUTPUT_ROOT = resolveOutputRoot();
  // 只有文章页面需要多语言，其他页面只生成中文版
  const langs = ['zh'];
  
  try {
    logger.info('开始页面渲染', { taskId, type, params, langs });

    let successCount = 0;
    let failCount = 0;
    const results = [];

    for (const lang of langs) {
      const attempt = await safeRenderPageVariant({ taskId, type, lang, params, OUTPUT_ROOT });
      if (attempt.ok) {
        successCount++;
        results.push(attempt.result);
      } else {
        failCount++;
      }
    }

    if (failCount > 0 && successCount === 0) {
      throw new Error(`类型${type}的所有页面渲染都失败了`);
    }

    monitor.recordRenderSuccess(taskId, { 
      type, 
      params, 
      successCount, 
      failCount,
      results,
      count: successCount // 页面渲染应该记录实际成功的页面数量
    });

    logger.info('Page rendering completed', { 
      taskId, 
      type, 
      params,
      successCount, 
      failCount,
      results
    });

  } catch (error) {
    monitor.recordRenderFailure(taskId, error);
    throw error;
  }
}

// ===== API路由 =====

// 中间件：记录请求
app.use((req, res, next) => {
  const requestId = generateTaskId();
  req.requestId = requestId;
  
  // 监控API前缀 - 仅保留health
  const monitoringPrefixes = ['/health'];
  const isMonitoringRequest = monitoringPrefixes.some(prefix => req.path.startsWith(prefix));
  
  if (!isMonitoringRequest) {
    monitor.recordRequest(req.method + ' ' + req.path);
  }
  
  // 只记录非监控API的请求，避免自动刷新产生大量日志
  if (!isMonitoringRequest) {
    logger.info('API request received', {
      requestId,
      method: req.method,
      path: req.path,
      ip: req.ip,
      userAgent: req.get('User-Agent')
    });
  } else {
    // 监控API请求只记录为DEBUG级别（通常不会输出）
    logger.debug('Monitoring API request', {
      requestId,
      method: req.method,
      path: req.path
    });
  }
  
  next();
});

// 原有文章渲染API - 增强版（支持指定语言）
app.post('/render', async (req, res) => {
  const requestId = req.requestId;
  const { ids, languages } = req.body;

  logger.info('收到渲染请求', { requestId, ids, languages });

  if (!Array.isArray(ids) || ids.length === 0) {
    logger.warn('无效的渲染请求 - 需要ids数组', { requestId, body: req.body });
    return res.status(400).json({
      message: '需要ids数组',
      requestId,
      timestamp: new Date().toISOString()
    });
  }

  if (ids.length > 50) {
    logger.warn('单次请求文章数量过多', { requestId, count: ids.length });
    return res.status(400).json({
      message: '文章数量过多。每次请求最多50篇。',
      requestId,
      received: ids.length,
      maximum: 50
    });
  }

  // 验证languages参数
  let languagesToRender = languages;
  if (!Array.isArray(languagesToRender) || languagesToRender.length === 0) {
    // 如果没有指定语言，默认渲染中文
    languagesToRender = ['zh'];
    logger.warn('No languages specified, defaulting to Chinese', {
      requestId,
      articleIds: ids
    });
  }

  try {
    const startTime = Date.now();
    await renderIds(ids, { languages: languagesToRender });
    const duration = Date.now() - startTime;

    logger.info('Render request completed successfully', {
      requestId,
      articleCount: ids.length,
      languages: languagesToRender,
      duration: `${duration}ms`
    });

    res.json({
      status: 'ok',
      rendered: ids.length,
      renderedLanguages: languagesToRender,
      requestId,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });
  } catch (e) {
    logger.error('Render request failed', {
      requestId,
      error: e.message,
      stack: e.stack,
      ids,
      languages: languagesToRender
    });
    res.status(500).json({
      message: e.message,
      requestId,
      timestamp: new Date().toISOString()
    });
  }
});

function isInternalServiceRequest(req) {
  const internalService = req.get('X-Internal-Service');
  return typeof internalService === 'string' && internalService.trim() !== '';
}

function requireInternalService(req, res) {
  if (isInternalServiceRequest(req)) return true;
  res.status(403).json({
    error: 'Forbidden',
    requestId: req.requestId,
    timestamp: new Date().toISOString()
  });
  return false;
}

function removePathIfExists(targetPath) {
  if (!fs.existsSync(targetPath)) return { existed: false, deletedFiles: 0 };
  const stat = fs.statSync(targetPath);
  if (!stat.isDirectory()) {
    fs.unlinkSync(targetPath);
    return { existed: true, deletedFiles: 1 };
  }

  const deletedFiles = clearDirectory(targetPath);
  try {
    fs.rmdirSync(targetPath);
  } catch (e) {
  }
  return { existed: true, deletedFiles };
}

function removeIndexFilesInDirectory(targetDir) {
  if (!fs.existsSync(targetDir)) return { existed: false, deletedFiles: 0 };
  const stat = fs.statSync(targetDir);
  if (!stat.isDirectory()) return { existed: false, deletedFiles: 0 };

  const files = fs.readdirSync(targetDir);
  let deletedFiles = 0;
  for (const file of files) {
    if (!/^index(-[a-zA-Z-]+)?\.html$/.test(file)) continue;
    try {
      fs.unlinkSync(path.join(targetDir, file));
      deletedFiles++;
    } catch (e) {
    }
  }
  return { existed: true, deletedFiles };
}

app.delete('/render/:id', (req, res) => {
  if (!requireInternalService(req, res)) return;

  const requestId = req.requestId;
  const id = req.params.id;
  if (!id || !/^\d+$/.test(id)) {
    return res.status(400).json({
      error: 'Invalid article id',
      requestId,
      timestamp: new Date().toISOString()
    });
  }

  const OUTPUT_ROOT = resolveOutputRoot();
  const articleDir = path.join(OUTPUT_ROOT, 'article', id.toString());

  try {
    const result = removePathIfExists(articleDir);
    res.json({
      success: true,
      requestId,
      articleId: parseInt(id, 10),
      existed: result.existed,
      deletedFiles: result.deletedFiles,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('删除文章预渲染文件失败', { requestId, articleId: id, error: error.message, stack: error.stack });
    res.status(500).json({
      success: false,
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// 专门的文章渲染API
app.post('/render/article', async (req, res) => {
  const requestId = req.requestId;
  const { id, languages } = req.body;

  logger.info('收到文章渲染请求', { requestId, articleId: id, languages });

  if (!id) {
    logger.warn('无效的文章渲染请求 - 需要id参数', { requestId, body: req.body });
    return res.status(400).json({
      message: '需要文章ID',
      requestId,
      timestamp: new Date().toISOString()
    });
  }

  // 验证languages参数
  let languagesToRender = languages;
  if (!Array.isArray(languagesToRender) || languagesToRender.length === 0) {
    // 如果没有指定语言，默认渲染中文
    languagesToRender = ['zh'];
    logger.warn('文章未指定语言，默认使用中文', {
      requestId,
      articleId: id
    });
  }

  try {
    const startTime = Date.now();
    await renderIds([id], { languages: languagesToRender });
    const duration = Date.now() - startTime;

    logger.info('文章渲染成功完成', {
      requestId,
      articleId: id,
      languages: languagesToRender,
      duration: `${duration}ms`
    });

    res.json({
      success: true,
      message: `文章${id}在以下语言中渲染成功: ${languagesToRender.join(', ')}`,
      articleId: id,
      renderedLanguages: languagesToRender,
      requestId,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });
  } catch (e) {
    logger.error('文章渲染失败', {
      requestId,
      articleId: id,
      languages: languagesToRender,
      error: e.message,
      stack: e.stack
    });

    res.status(500).json({
      success: false,
      message: e.message,
      requestId,
      timestamp: new Date().toISOString()
    });
  }
});

async function safeRenderAllSortVariant(batchTaskId, sortId) {
  try {
    await renderSingleSortPage(sortId, batchTaskId);
    logger.info('分类页面渲染成功', { batchTaskId, sortId });
    return { ok: true };
  } catch (error) {
    logger.error('分类页面渲染失败', { batchTaskId, sortId, error: error.message });
    return { ok: false, error };
  }
}

async function renderAllSortPages(sortIds) {
  const batchTaskId = generateTaskId();
  monitor.recordRenderStart(batchTaskId, 'allSorts', { sortIds, count: sortIds.length });

  let successCount = 0;
  let failCount = 0;
  const results = [];

  try {
    for (const sortId of sortIds) {
      const attempt = await safeRenderAllSortVariant(batchTaskId, sortId);
      if (attempt.ok) {
        successCount++;
        results.push({ sortId, status: 'success' });
      } else {
        failCount++;
        results.push({ sortId, status: 'failed', error: attempt.error.message });
      }
    }

    monitor.recordRenderSuccess(batchTaskId, {
      type: 'allSorts',
      sortIds,
      successCount,
      failCount,
      results,
      count: successCount
    });

    return { batchTaskId, successCount, failCount, results };
  } catch (error) {
    monitor.recordRenderFailure(batchTaskId, error);
    throw error;
  }
}

// 新增：页面渲染API - 增强版
app.post('/render/pages', async (req, res) => {
  const requestId = req.requestId;
  const { type, params = {} } = req.body;
  
  logger.info('收到页面渲染请求', { requestId, type, params });

  const supportedTypes = ['home', 'friends', 'music', 'favorites', 'sort', 'allSorts', 'about', 'message', 'weiYan', 'love', 'travel', 'privacy', 'letter', 'verify', '403', '404', 'oauth-callback'];
  
  if (!type) {
    logger.warn('无效的页面渲染请求 - 需要type参数', { requestId, body: req.body });
    return res.status(400).json({ 
      message: '需要type参数',
      requestId,
      supportedTypes: ['home', 'favorite', 'sort'],
      timestamp: new Date().toISOString()
    });
  }

  if (!supportedTypes.includes(type)) {
    logger.warn('无效的页面类型', { requestId, type });
    return res.status(400).json({ 
      message: '无效的页面类型',
      requestId,
      received: type,
      supported: supportedTypes,
      timestamp: new Date().toISOString()
    });
  }

  try {
    const startTime = Date.now();
    
    if (type === 'allSorts') {
      const { sortIds } = params;
      if (!Array.isArray(sortIds) || sortIds.length === 0) {
        return res.status(400).json({ 
          message: 'allSorts类型需要sortIds数组',
          requestId 
        });
      }
      
      logger.info('渲染所有分类页面', { requestId, sortIds });
      await renderAllSortPages(sortIds);
    } else {
      await renderPages(type, params);
    }
    
    const duration = Date.now() - startTime;
    
    logger.info('页面渲染请求成功完成', { 
      requestId, 
      type, 
      params, 
      duration: `${duration}ms` 
    });
    
    res.json({ 
      status: 'ok', 
      type, 
      params,
      requestId,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });
  } catch (e) {
    logger.error('页面渲染请求失败', { 
      requestId, 
      type, 
      params,
      error: e.message, 
      stack: e.stack 
    });
    res.status(500).json({ 
      message: e.message,
      requestId,
      type,
      params,
      timestamp: new Date().toISOString()
    });
  }
});

app.delete('/render/pages/sort/:sortId', (req, res) => {
  if (!requireInternalService(req, res)) return;

  const requestId = req.requestId;
  const sortId = req.params.sortId;
  const labelId = req.query.labelId;

  if (!sortId || !/^\d+$/.test(sortId)) {
    return res.status(400).json({
      error: 'Invalid sortId',
      requestId,
      timestamp: new Date().toISOString()
    });
  }
  if (labelId != null && labelId !== '' && !/^\d+$/.test(labelId)) {
    return res.status(400).json({
      error: 'Invalid labelId',
      requestId,
      timestamp: new Date().toISOString()
    });
  }

  const OUTPUT_ROOT = resolveOutputRoot();
  const targetPath = labelId
    ? path.join(OUTPUT_ROOT, 'sort', sortId.toString(), labelId.toString())
    : path.join(OUTPUT_ROOT, 'sort', sortId.toString());

  try {
    const result = removePathIfExists(targetPath);
    res.json({
      success: true,
      requestId,
      type: 'sort',
      sortId: parseInt(sortId, 10),
      labelId: labelId ? parseInt(labelId, 10) : null,
      existed: result.existed,
      deletedFiles: result.deletedFiles,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('删除分类预渲染文件失败', { requestId, sortId, labelId, error: error.message, stack: error.stack });
    res.status(500).json({
      success: false,
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

app.delete('/render/pages/:type', (req, res) => {
  if (!requireInternalService(req, res)) return;

  const requestId = req.requestId;
  const type = req.params.type;

  const supportedTypes = ['home', 'friends', 'music', 'favorites', 'sort', 'about', 'message', 'weiYan', 'love', 'travel', 'privacy', 'letter', 'verify', '403', '404', 'oauth-callback'];
  if (!supportedTypes.includes(type)) {
    return res.status(400).json({
      error: 'Invalid page type',
      requestId,
      received: type,
      supported: supportedTypes,
      timestamp: new Date().toISOString()
    });
  }

  const OUTPUT_ROOT = resolveOutputRoot();

  try {
    if (type === 'sort') {
      const dir = path.join(OUTPUT_ROOT, 'sort');
      const result = removeIndexFilesInDirectory(dir);
      return res.json({
        success: true,
        requestId,
        type,
        existed: result.existed,
        deletedFiles: result.deletedFiles,
        timestamp: new Date().toISOString()
      });
    }

    const targetDir = path.join(OUTPUT_ROOT, type);
    const result = removePathIfExists(targetDir);
    res.json({
      success: true,
      requestId,
      type,
      existed: result.existed,
      deletedFiles: result.deletedFiles,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('删除页面预渲染文件失败', { requestId, type, error: error.message, stack: error.stack });
    res.status(500).json({
      success: false,
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// 新增：服务状态监控API
app.get('/status', (req, res) => {
  const requestId = req.requestId;
  
  // 仅保留基础运行状态，移除monitor.getStats调用
  res.json({
    service: 'prerender-worker',
    version: '2.0.0',
    status: 'running',
    requestId,
    timestamp: new Date().toISOString()
  });
});

// 新增：详细的健康检查API
app.get('/health', (req, res) => {
  const requestId = req.requestId;
  
  try {
    // 检查模板文件
    const templatePath = path.resolve('/app/dist/index.html');
    const templateExists = fs.existsSync(templatePath);
    
    // 检查输出目录
    const outputPath = process.env.PRERENDER_OUTPUT || path.resolve(__dirname, './dist/prerender');
    const outputDirExists = fs.existsSync(outputPath);
    
    // 内存使用检查
    const memUsage = process.memoryUsage();
    const memoryHealthy = memUsage.heapUsed < 500 * 1024 * 1024; // 500MB
    
    // 运行时间检查
    const uptime = process.uptime();
    
    const health = {
      status: templateExists && outputDirExists && memoryHealthy ? 'healthy' : 'unhealthy',
      requestId,
      timestamp: new Date().toISOString(),
      checks: {
        template: {
          status: templateExists ? 'ok' : 'missing',
          path: templatePath
        },
        outputDirectory: {
          status: outputDirExists ? 'ok' : 'missing',
          path: outputPath
        },
        memory: {
          status: memoryHealthy ? 'ok' : 'high',
          usage: {
            heapUsed: `${(memUsage.heapUsed / 1024 / 1024).toFixed(1)}MB`,
            heapTotal: `${(memUsage.heapTotal / 1024 / 1024).toFixed(1)}MB`,
            rss: `${(memUsage.rss / 1024 / 1024).toFixed(1)}MB`
          }
        },
        uptime: `${Math.floor(uptime)}s`,
        runningTasks: monitor.stats.currentTasks.size
      }
    };
    
    // 移除健康检查完成日志，避免刷屏
    
    const statusCode = health.status === 'healthy' ? 200 : 503;
    res.status(statusCode).json(health);
    
  } catch (error) {
    logger.error('健康检查失败', { requestId, error: error.message });
    res.status(500).json({
      status: 'error',
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

app.post('/clear-cache', (req, res) => {
  if (!requireInternalService(req, res)) return;
  const requestId = req.requestId;

  try {
    assetCache.assets = null;
    assetCache.lastFetch = 0;
    res.json({
      success: true,
      requestId,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('清理缓存失败', { requestId, error: error.message, stack: error.stack });
    res.status(500).json({
      success: false,
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

app.post('/restart-routes', (req, res) => {
  if (!requireInternalService(req, res)) return;
  const requestId = req.requestId;

  try {
    assetCache.assets = null;
    assetCache.lastFetch = 0;
    res.json({
      success: true,
      requestId,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('重启路由失败', { requestId, error: error.message, stack: error.stack });
    res.status(500).json({
      success: false,
      requestId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// 工具函数：清理目录
function clearDirectory(dirPath) {
  let deletedCount = 0;
  
  if (!fs.existsSync(dirPath)) {
    return deletedCount;
  }
  
  function deleteRecursive(dir) {
    const files = fs.readdirSync(dir);
    
    files.forEach(file => {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      
      if (stat.isDirectory()) {
        deleteRecursive(filePath);
        fs.rmdirSync(filePath);
      } else {
        fs.unlinkSync(filePath);
        deletedCount++;
      }
    });
  }
  
  try {
    deleteRecursive(dirPath);
    return deletedCount;
  } catch (error) {
    logger.error('清理目录失败', { dirPath, error: error.message });
    return deletedCount;
  }
}

// Initialize output directory at startup
function initializeOutputDirectory() {
  const outputPath = process.env.PRERENDER_OUTPUT || path.resolve(__dirname, './dist/prerender');
  try {
    if (!fs.existsSync(outputPath)) {
      fs.mkdirSync(outputPath, { recursive: true });
      logger.info('输出目录已创建', { path: outputPath });
    } else {
      logger.info('输出目录已存在', { path: outputPath });
    }
  } catch (error) {
    logger.error('创建输出目录失败', { path: outputPath, error: error.message });
  }
}

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  // Initialize output directory on startup
  initializeOutputDirectory();
  
  logger.info('Prerender worker 启动', {
    port: PORT,
    nodeVersion: process.version,
    timestamp: new Date().toISOString(),
    environment: {
      JAVA_BACKEND_URL,
      PYTHON_BACKEND_URL,
      LOG_LEVEL: process.env.LOG_LEVEL || 'info',
      PRERENDER_OUTPUT: process.env.PRERENDER_OUTPUT || 'default'
    }
  });
  console.log(`Prerender worker 监听于端口 ${PORT}`);
});

// 添加一个通用函数来确保图片URL是绝对路径
function ensureAbsoluteImageUrl(url, baseUrl) {
  if (!url) return url;
  
  // 如果已经是绝对URL（包含协议），直接返回
  if (/^https?:\/\//.test(url)) {
    return url;
  }
  
  // 如果是相对路径，转换为绝对路径
  if (url.startsWith('/')) {
    const cleanBaseUrl = baseUrl ? baseUrl.replace(/\/$/, '') : '';
    return cleanBaseUrl ? `${cleanBaseUrl}${url}` : url;
  }
  
  // 如果是相对路径但不以/开头，添加/
  const cleanBaseUrl = baseUrl ? baseUrl.replace(/\/$/, '') : '';
  return cleanBaseUrl ? `${cleanBaseUrl}/${url}` : `/${url}`;
}

// 添加一个通用函数，用于将SEO配置中的图标字段添加到meta对象中
function addSeoIconFieldsToMeta(meta, seoConfig, baseUrl = '') {
  if (!meta || !seoConfig) return meta;
  
  // 定义需要从SEO配置中复制到meta的图标字段
  const iconFields = [
    'site_icon',
    'apple_touch_icon',
    'site_icon_192',
    'site_icon_512',
    'site_logo'
  ];
  
  // 复制字段，确保图片URL是绝对路径
  iconFields.forEach(field => {
    if (seoConfig[field] && !meta[field]) {
      meta[field] = ensureAbsoluteImageUrl(seoConfig[field], baseUrl);
    }
  });
  
  return meta;
}

// 通用函数：添加所有SEO相关标签
function addSearchEngineVerificationTags(meta, seoConfig) {
  if (!meta || !seoConfig) return meta;

  // 搜索引擎验证标签字段（基于Java接口返回的完整字段）
  const verificationFields = [
    'google_site_verification',
    'baidu_site_verification', 
    'bing_site_verification',
    'yandex_site_verification',
    'sogou_site_verification',
    'so_site_verification',
    'shenma_site_verification',
    'yahoo_site_verification',
    'duckduckgo_site_verification'
  ];

  let addedCount = 0;
  verificationFields.forEach(field => {
    if (seoConfig[field] && seoConfig[field].trim() !== '' && !meta[field]) {
      meta[field] = seoConfig[field].trim();
      addedCount++;
    }
  });

  // 添加社交媒体字段（高价值SEO字段）
  const socialMediaFields = [
    // Twitter相关
    'twitter_site', 
    'twitter_creator',
    
    // Facebook相关
    'fb_app_id',
    'fb_page_url',
    
    
    // LinkedIn支持
    'linkedin_company_id',
    
    // Pinterest增强
    'pinterest_verification',
    'pinterest_description',
    
    // 小程序支持
    'wechat_miniprogram_id',
    'wechat_miniprogram_path',
    'qq_miniprogram_path'
  ];
  socialMediaFields.forEach(field => {
    if (seoConfig[field] && seoConfig[field].trim() !== '' && !meta[field]) {
      meta[field] = seoConfig[field].trim();
      addedCount++;
    }
  });


  // 添加robots标签
  if (seoConfig.robots_default && !meta.robots) {
    meta.robots = seoConfig.robots_default;
  }

  // 添加自定义头部代码
  if (seoConfig.custom_head_code && seoConfig.custom_head_code.trim() !== '' && !meta.custom_head_code) {
    meta.custom_head_code = seoConfig.custom_head_code.trim();
    addedCount++;
  }

  // 添加其他基础SEO字段
  const basicSeoFields = [
    'default_author',
    'site_short_name', 
    'site_address',
    'site_description',
    'site_keywords'
  ];
  basicSeoFields.forEach(field => {
    if (seoConfig[field] && seoConfig[field].trim() !== '' && !meta[field]) {
      meta[field] = seoConfig[field].trim();
      addedCount++;
    }
  });

  if (addedCount > 0) {
    console.debug('已添加SEO相关标签到meta对象', { 
      verificationTagsCount: addedCount,
      hasRobots: !!meta.robots,
      hasCustomHeadCode: !!meta.custom_head_code
    });
  }

  return meta;
}