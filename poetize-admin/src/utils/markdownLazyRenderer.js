/**
 * @file Markdown 懒加载渲染器工具
 * @description 封装 MarkdownIt 及其插件的动态导入，通过代码分割减少首页 Bundle 体积。
 */

let mdInstance = null;

/**
 * 异步预热 Markdown 渲染引擎核心
 * @description 仅加载 markdown-it 核心包，不执行插件加载，建议在应用空闲或鼠标悬浮前期执行。
 * @returns {Promise<void>}
 */
export async function warmupMarkdown() {
    if (mdInstance) return;
    try {
        await import('markdown-it');
    } catch (e) {
        console.error('Markdown 核心预热失败:', e);
    }
}

/**
 * 解析并渲染 Markdown 内容为 HTML 字符串
 * @description 若库尚未加载，将通过动态导入并发获取核心引擎及其常用插件（表格、数学公式等）。
 * @param {string} content - 待渲染的原始 Markdown 文本字符串
 * @returns {Promise<string>} 渲染后的 HTML 字符串
 */
export async function renderMarkdown(content) {
    if (!mdInstance) {
        try {
            const [
                { default: MarkdownIt },
                { default: markdownItMultimdTable },
                { default: markdownItKatex }
            ] = await Promise.all([
                import('markdown-it'),
                import('markdown-it-multimd-table'),
                import('@iktakahiro/markdown-it-katex')
            ]);

            mdInstance = new MarkdownIt({ breaks: true })
                .use(markdownItMultimdTable)
                .use(markdownItKatex);
        } catch (error) {
            console.error('Markdown 渲染器加载失败:', error);
            return content || '';
        }
    }

    return mdInstance.render(content || '');
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
