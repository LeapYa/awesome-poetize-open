/**
 * @file Markdown 文件导入工具
 * @description 解析 Markdown 文件的 front-matter 元数据和正文内容，
 * 将其转换为 ArticleVO 格式，供文章导入功能使用。
 * 同时支持 JSON 格式的文章批量导入。
 * 包含智能标题降级判断逻辑：仅在正文中存在 # 一级标题时才进行降级处理。
 */

/**
 * 解析 YAML front-matter
 * 支持格式：
 * ---
 * title: 文章标题
 * cover: 封面URL
 * sort: 分类名称
 * label: 标签名称
 * date: 2025-01-01
 * tags: [标签1, 标签2]
 * password: 访问密码
 * video: 视频链接
 * ---
 *
 * @param {string} yamlStr - front-matter 原始字符串（不含 --- 分隔符）
 * @returns {Object} 解析后的键值对
 */
function parseFrontMatter(yamlStr) {
  var result = {};
  if (!yamlStr || !yamlStr.trim()) {
    return result;
  }

  var lines = yamlStr.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim();
    if (!line || line.startsWith('#')) {
      continue;
    }

    var colonIndex = line.indexOf(':');
    if (colonIndex === -1) {
      continue;
    }

    var key = line.substring(0, colonIndex).trim();
    var value = line.substring(colonIndex + 1).trim();

    // 处理引号包裹的值
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      value = value.substring(1, value.length - 1);
    }

    // 处理数组格式 [item1, item2]
    if (value.startsWith('[') && value.endsWith(']')) {
      var arrStr = value.substring(1, value.length - 1);
      value = arrStr.split(',').map(function(item) {
        var s = item.trim();
        if ((s.startsWith('"') && s.endsWith('"')) ||
            (s.startsWith("'") && s.endsWith("'"))) {
          s = s.substring(1, s.length - 1);
        }
        return s;
      }).filter(function(s) { return s.length > 0; });
    }

    // 处理布尔值
    if (value === 'true') value = true;
    else if (value === 'false') value = false;

    if (key) {
      result[key] = value;
    }
  }

  return result;
}

/**
 * 从文件名中提取文章标题
 * 去除扩展名、日期前缀等
 *
 * @param {string} fileName - 文件名
 * @returns {string} 提取的标题
 */
function extractTitleFromFileName(fileName) {
  if (!fileName) return '未命名文章';

  // 去掉扩展名
  var name = fileName.replace(/\.(md|markdown|txt)$/i, '');

  // 去掉常见的日期前缀格式：2025-01-01- 或 20250101-
  name = name.replace(/^\d{4}-\d{2}-\d{2}[-_]?/, '');
  name = name.replace(/^\d{8}[-_]?/, '');

  // 去掉序号前缀：01- 或 01_
  name = name.replace(/^\d{1,3}[-_]/, '');

  return name.trim() || '未命名文章';
}

/**
 * 解析单个 Markdown 文件内容
 *
 * @param {string} content - 文件的文本内容
 * @param {string} fileName - 文件名（用作 fallback 标题）
 * @returns {Object} ArticleVO 格式的文章数据
 */
export function parseMarkdownFile(content, fileName) {
  var article = {
    articleTitle: '',
    articleContent: '',
    articleCover: '',
    sortName: '',
    labelName: '',
    viewStatus: null,       // null 表示文件未指定，导入时使用默认配置
    commentStatus: null,    // null 表示文件未指定，导入时使用默认配置
    recommendStatus: null,  // null 表示文件未指定，导入时使用默认配置
    submitToSearchEngine: false,
    videoUrl: '',
    password: '',
    tips: '',
    needManualTitle: false,  // 标记是否需要用户手动输入标题
    h1ExtractedAsTitle: false,  // 标记是否自动将唯一的 # 一级标题提取为文章标题
    extractedH1: '',  // 被提取的一级标题文本（用于撤销提取时恢复）
    originalContent: ''  // 提取标题前的完整正文（用于撤销提取时恢复）
  };

  if (!content) {
    article.articleTitle = extractTitleFromFileName(fileName);
    return article;
  }

  // 检测是否有 front-matter（以 --- 开头）
  var frontMatterRegex = /^---\s*\n([\s\S]*?)\n---\s*\n?/;
  var match = content.match(frontMatterRegex);

  if (match) {
    // 有 front-matter
    var meta = parseFrontMatter(match[1]);
    var body = content.substring(match[0].length).trim();

    article.articleTitle = meta.title || meta.Title || extractTitleFromFileName(fileName);
    article.articleContent = body;
    article.articleCover = meta.cover || meta.Cover || meta.image || meta.banner || '';
    article.sortName = meta.sort || meta.Sort || meta.category || meta.Category || '';
    article.labelName = meta.label || meta.Label || meta.tag || meta.Tag || '';
    article.videoUrl = meta.video || meta.Video || meta.videoUrl || '';
    article.password = meta.password || meta.Password || '';
    article.tips = meta.tips || meta.Tips || meta.summary || meta.description || '';

    // 处理 tags 数组 → 取第一个作为 labelName
    if (!article.labelName && meta.tags) {
      if (Array.isArray(meta.tags) && meta.tags.length > 0) {
        article.labelName = meta.tags[0];
      } else if (typeof meta.tags === 'string') {
        article.labelName = meta.tags;
      }
    }

    // 处理 categories 数组 → 取第一个作为 sortName
    if (!article.sortName && meta.categories) {
      if (Array.isArray(meta.categories) && meta.categories.length > 0) {
        article.sortName = meta.categories[0];
      } else if (typeof meta.categories === 'string') {
        article.sortName = meta.categories;
      }
    }

    // 处理布尔字段
    if (meta.commentStatus !== undefined) article.commentStatus = !!meta.commentStatus;
    if (meta.recommendStatus !== undefined) article.recommendStatus = !!meta.recommendStatus;
    if (meta.viewStatus !== undefined) article.viewStatus = !!meta.viewStatus;
    if (meta.draft === true) article.viewStatus = false;
  } else {
    // 没有 front-matter
    var bodyContent = content.trim();

    // 统计正文中一级标题的数量（排除代码块内的）
    var h1Count = countH1Headings(bodyContent);

    if (h1Count === 1) {
      // 只有一个 # 一级标题 → 默认视为文章标题，提取并从正文移除
      // 同时保存原始数据，供用户确认或撤销
      var headingMatch = bodyContent.match(/^#\s+(.+)/m);
      if (headingMatch) {
        article.articleTitle = headingMatch[1].trim();
        article.articleContent = bodyContent.replace(/^#\s+.+\n?/, '').trim();
        article.h1ExtractedAsTitle = true;
        article.extractedH1 = headingMatch[1].trim();
        article.originalContent = bodyContent;
      } else {
        article.articleTitle = extractTitleFromFileName(fileName);
        article.articleContent = bodyContent;
      }
    } else if (h1Count > 1) {
      // 多个 # 一级标题 → 这些不是文章标题，是正文内容
      // 正文原样保留（后续 smartDowngradeHeadings 会自动降级）
      // 标题用文件名，并标记需要用户手动输入
      article.articleTitle = extractTitleFromFileName(fileName);
      article.articleContent = bodyContent;
      article.needManualTitle = true;
    } else {
      // 没有 # 一级标题
      article.articleTitle = extractTitleFromFileName(fileName);
      article.articleContent = bodyContent;
    }
  }

  return article;
}

/**
 * 解析 JSON 文件内容（支持批量导入）
 *
 * 支持格式：
 * 1. { "articles": [ {...}, {...} ] }
 * 2. [ {...}, {...} ]
 * 3. { "articleTitle": "...", "articleContent": "..." }  （单篇）
 *
 * @param {string} content - JSON 文本内容
 * @returns {Array} ArticleVO 格式的文章数据数组
 */
export function parseJsonFile(content) {
  var data;
  try {
    data = JSON.parse(content);
  } catch (e) {
    throw new Error('JSON 文件格式错误：' + e.message);
  }

  var rawArticles = [];

  if (Array.isArray(data)) {
    rawArticles = data;
  } else if (data && Array.isArray(data.articles)) {
    rawArticles = data.articles;
  } else if (data && (data.articleTitle || data.title)) {
    rawArticles = [data];
  } else {
    throw new Error('无法识别的 JSON 格式，请使用数组格式或 { "articles": [...] } 格式');
  }

  return rawArticles.map(function(raw, index) {
    return {
      articleTitle: raw.articleTitle || raw.title || ('导入文章 ' + (index + 1)),
      articleContent: raw.articleContent || raw.content || '',
      articleCover: raw.articleCover || raw.cover || '',
      sortName: raw.sortName || raw.sort || raw.category || '',
      labelName: raw.labelName || raw.label || raw.tag || '',
      viewStatus: raw.viewStatus !== undefined ? !!raw.viewStatus : true,
      commentStatus: raw.commentStatus !== undefined ? !!raw.commentStatus : true,
      recommendStatus: raw.recommendStatus !== undefined ? !!raw.recommendStatus : false,
      submitToSearchEngine: raw.submitToSearchEngine !== undefined ? !!raw.submitToSearchEngine : false,
      videoUrl: raw.videoUrl || raw.video || '',
      password: raw.password || '',
      tips: raw.tips || raw.summary || raw.description || '',
      needManualTitle: false,
      h1ExtractedAsTitle: false,
      extractedH1: '',
      originalContent: ''
    };
  });
}

/**
 * 读取文件内容为文本
 *
 * @param {File} file - 文件对象
 * @returns {Promise<string>} 文件文本内容
 */
export function readFileAsText(file) {
  return new Promise(function(resolve, reject) {
    var reader = new FileReader();
    reader.onload = function(e) {
      resolve(e.target.result);
    };
    reader.onerror = function(e) {
      reject(new Error('读取文件失败: ' + file.name));
    };
    reader.readAsText(file, 'UTF-8');
  });
}

/**
 * 获取文件扩展名（小写）
 *
 * @param {string} fileName
 * @returns {string}
 */
export function getFileExtension(fileName) {
  if (!fileName) return '';
  var parts = fileName.split('.');
  return parts.length > 1 ? parts.pop().toLowerCase() : '';
}

/**
 * 验证文件是否为支持的导入格式
 *
 * @param {File} file
 * @returns {{ valid: boolean, message: string }}
 */
export function validateImportFile(file) {
  var MAX_SIZE = 10 * 1024 * 1024; // 10MB
  var ALLOWED_EXTENSIONS = ['md', 'markdown', 'json', 'txt'];

  var ext = getFileExtension(file.name);

  if (ALLOWED_EXTENSIONS.indexOf(ext) === -1) {
    return {
      valid: false,
      message: '不支持的文件格式: .' + ext + '，支持: .md, .markdown, .json, .txt'
    };
  }

  if (file.size > MAX_SIZE) {
    return {
      valid: false,
      message: '文件 ' + file.name + ' 超过 10MB 大小限制'
    };
  }

  if (file.size === 0) {
    return {
      valid: false,
      message: '文件 ' + file.name + ' 内容为空'
    };
  }

  return { valid: true, message: '' };
}

/**
 * 统计 Markdown 内容中 # 一级标题的数量（排除代码块内的）
 *
 * @param {string} content - Markdown 内容
 * @returns {number} 一级标题的数量
 */
function countH1Headings(content) {
  if (!content) return 0;

  var lines = content.split('\n');
  var inCodeBlock = false;
  var count = 0;

  for (var i = 0; i < lines.length; i++) {
    var trimmed = lines[i].trim();

    if (trimmed.startsWith('```') || trimmed.startsWith('~~~')) {
      inCodeBlock = !inCodeBlock;
      continue;
    }

    if (inCodeBlock) continue;

    // 一级标题：以 # 开头且后面紧跟空格（不是 ## 等）
    if (/^\s*#\s/.test(lines[i]) && !/^\s*##/.test(lines[i])) {
      count++;
    }
  }

  return count;
}

/**
 * 检测正文内容中是否存在 # 一级标题（排除代码块内的）
 *
 * @param {string} content - 文章正文 Markdown 内容
 * @returns {boolean} 是否包含一级标题
 */
export function contentHasH1Headings(content) {
  if (!content) return false;

  var lines = content.split('\n');
  var inCodeBlock = false;

  for (var i = 0; i < lines.length; i++) {
    var trimmed = lines[i].trim();

    // 检测代码块边界
    if (trimmed.startsWith('```') || trimmed.startsWith('~~~')) {
      inCodeBlock = !inCodeBlock;
      continue;
    }

    // 代码块内跳过
    if (inCodeBlock) continue;

    // 检测一级标题：以 # 开头且后面紧跟空格（不是 ## 等）
    if (/^\s*#\s/.test(lines[i])) {
      return true;
    }
  }

  return false;
}

/**
 * 智能标题降级处理（用于导入文章）
 *
 * 逻辑：
 * - 如果正文中存在 # 一级标题 → 说明导入的文章把一级标题当作正文内容使用，
 *   需要整体降级以匹配系统约定（h1 留给文章标题，正文从 h2 开始）
 * - 如果正文中没有 # 一级标题（只有 ##、### 等）→ 说明标题级别已经正确，
 *   不需要降级
 *
 * @param {string} content - 文章正文 Markdown 内容
 * @param {Function} downgradeFunc - 降级函数（downgradeMarkdownHeadings）
 * @returns {string} 处理后的正文内容
 */
export function smartDowngradeHeadings(content, downgradeFunc) {
  if (!content) return '';

  if (contentHasH1Headings(content)) {
    // 正文中有一级标题，需要降级
    return downgradeFunc(content);
  }

  // 正文中没有一级标题，标题级别已正确，原样返回
  return content;
}
