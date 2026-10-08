/**
 * @file Markdown 懒加载渲染器工具
 * @description 封装 MarkdownIt 及其插件的动态导入，通过代码分割减少首页 Bundle 体积。
 */

import { transformAttachmentLinks } from './attachmentCard';

// 渲染器按语言缓存实例：GitHub Alert 提示框的标题文案跟随内容语言，
// 不同语言必须用不同实例，否则后建的会覆盖先建的。
// （主编辑器与翻译弹窗是两个语言不同、但可能同时挂载的编辑器，不能只用一个全局语言。）
// ⚠️ 实例本身是 getInstance 内的局部 const，下面的 renderer 规则靠闭包引用它 ——
// 不要把它提回模块级，否则多语言并存时规则会指到「最后创建的那个」实例上。
// key 为语言码，取值受站点语言配置约束（后端白名单 14 种 + '' 表示未指定），不会无限增长。
const mdInstances = new Map();
const mdPending = new Map();

/**
 * 异步预热 Markdown 渲染引擎核心
 * @description 仅加载 markdown-it 核心包，不执行插件加载，建议在应用空闲或鼠标悬浮前期执行。
 * @returns {Promise<void>}
 */
export async function warmupMarkdown() {
    if (mdInstances.size) return;
    try {
        await import('markdown-it');
    } catch (e) {
        console.error('Markdown 核心预热失败:', e);
    }
}

/**
 * 创建（并按语言缓存）渲染器实例
 * @param {string} [lang] - 内容语言码，决定 GitHub Alert 标题文案；不传表示不指定
 * @returns {Promise<Object|null>} MarkdownIt 实例，创建失败返回 null
 */
async function getInstance(lang) {
    const key = lang || '';
    if (mdInstances.has(key)) return mdInstances.get(key);
    if (mdPending.has(key)) return mdPending.get(key);

    const task = (async () => {
        try {
            const [
                { default: MarkdownIt },
                { default: markdownItMultimdTable },
                { default: markdownItKatex },
                { default: markdownItTaskLists },
                { default: hljs },
                { default: multimdTableEscape },
                { default: githubAlerts }
            ] = await Promise.all([
                import('markdown-it'),
                import('markdown-it-multimd-table'),
                import('@iktakahiro/markdown-it-katex'),
                import('markdown-it-task-lists'),
                import('highlight.js'),
                import('./multimdTableEscape'),
                import('./githubAlerts')
            ]);

            const mdInstance = new MarkdownIt({ 
                breaks: true,
                html: true, // 允许 HTML 标签，以便渲染复杂的自定义结构
                linkify: true
            })
                .use(markdownItMultimdTable)
                .use(multimdTableEscape) // 还原单元格内 `\|`，见该文件头注释
                // 兼容 `> [!NOTE]` 系列 GitHub 私有扩展；标题文案跟随内容语言
                .use(githubAlerts, { lang })
                .use(markdownItKatex)
                .use(markdownItTaskLists, {
                    label: true,
                    labelAfter: true
                });

            const defaultTableOpen = mdInstance.renderer.rules.table_open;
            const defaultTableClose = mdInstance.renderer.rules.table_close;

            mdInstance.renderer.rules.table_open = function (tokens, idx, options, env, self) {
                const rendered = defaultTableOpen ? defaultTableOpen(tokens, idx, options, env, self) : self.renderToken(tokens, idx, options);
                return `<div class="table-wrapper">${rendered}`;
            };

            mdInstance.renderer.rules.table_close = function (tokens, idx, options, env, self) {
                const rendered = defaultTableClose ? defaultTableClose(tokens, idx, options, env, self) : self.renderToken(tokens, idx, options);
                return `${rendered}</div>`;
            };

            // 自定义 fence 渲染规则，避免外层被包裹 pre 和 code
            mdInstance.renderer.rules.fence = function (tokens, idx, options, env, self) {
                const token = tokens[idx];
                const info = token.info ? mdInstance.utils.unescapeAll(token.info).trim() : '';
                let lang = '';
                let langName = '';

                if (info) {
                    langName = info.split(/\s+/g)[0];
                    lang = langName;
                }

                let codeHTML = '';
                // 将代码按行分割，以便添加行号结构
                const lines = token.content.split(/\r?\n/);
                // markdown-it 的 fence content 通常末尾带有一个换行符，分割后会多出一个空串，需移除
                if (lines.length > 0 && lines[lines.length - 1] === '') {
                    lines.pop();
                }

                if (lang === 'mermaid') {
                    // Mermaid 图表容器
                    const encodedSource = mdInstance.utils.escapeHtml(encodeURIComponent(token.content));
                    return `<div class="mermaid" data-source="${encodedSource}">${mdInstance.utils.escapeHtml(token.content)}</div>`;
                }

                if (lang === 'echarts') {
                    // ECharts 图表容器
                    const encodedSource = mdInstance.utils.escapeHtml(encodeURIComponent(token.content));
                    return `<div class="echarts-render" data-source="${encodedSource}" style="width: 100%; height: 400px; margin: 10px 0;">${mdInstance.utils.escapeHtml(token.content)}</div>`;
                }

                if (lang && hljs.getLanguage(lang)) {
                    try {
                        // 逐行高亮以保持行结构安全（牺牲部分多行语法的准确性）
                        codeHTML = lines.map(line => {
                            const h = hljs.highlight(line, { language: lang, ignoreIllegals: true }).value;
                            return `<span class="code-line">${h || '&nbsp;'}</span>`;
                        }).join('');
                    } catch (__) {}
                }

                // 兜底或无高亮情况
                if (!codeHTML) {
                    codeHTML = lines.map(line => {
                        const escaped = mdInstance.utils.escapeHtml(line);
                        return `<span class="code-line">${escaped || '&nbsp;'}</span>`;
                    }).join('');
                }

                // 构造符合 editor-preview.css 要求的结构
                const langLabel = langName ? langName.toUpperCase() : '';
                const encodedCode = encodeURIComponent(token.content);
                const escapedLangLabel = mdInstance.utils.escapeHtml(langLabel);

                // 添加 css-line-numbers 类以启用 CSS 计数器
                return `<div class="highlight-wrap has-toolbar">
                    <div class="highlight-toolbar">
                        <span class="hl-dots" aria-hidden="true"></span>
                        <span class="hl-lang">${escapedLangLabel}</span>
                        <i class="el-icon-document-copy copy-code" data-code="${encodedCode}" title="复制"></i>
                    </div>
                    <pre><code class="hljs ${lang} css-line-numbers">${codeHTML}</code></pre>
                </div>`;
            };

            return mdInstance;
        } catch (error) {
            console.error('Markdown 渲染器加载失败:', error);
            return null;
        }
    })();

    mdPending.set(key, task);
    try {
        const md = await task;
        if (md) mdInstances.set(key, md);
        return md;
    } finally {
        mdPending.delete(key);
    }
}

/**
 * 解析并渲染 Markdown 内容为 HTML 字符串
 * @param {string} content - 待渲染的原始 Markdown 文本字符串
 * @param {string} [lang] - 内容语言码，决定 GitHub Alert 提示框标题文案
 * @returns {Promise<string>} 渲染后的 HTML 字符串
 */
export async function renderMarkdown(content, lang) {
    const md = await getInstance(lang);
    if (!md) return content || '';
    return transformAttachmentLinks(md.render(content || ''));
}

/**
 * 保持兼容性的简单 Markdown 渲染接口
 * @param {string} content - Markdown 文本
 * @returns {Promise<string>} HTML 字符串
 */
export async function renderSimpleMarkdown(content) {
    return renderMarkdown(content);
}

/**
 * 预加载接口（保持向下兼容，逻辑重连至核心预热）
 * @returns {Promise<void>}
 */
export async function preloadMarkdown() {
    return warmupMarkdown();
}
