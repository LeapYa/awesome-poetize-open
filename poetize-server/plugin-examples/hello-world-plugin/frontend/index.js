// ========================================================
// Hello World 示例插件 - frontend/index.js
// 依赖：window.PoetizePlugin（由 plugin-sdk.js 提供）
// ========================================================

(function () {
    'use strict'

    var PLUGIN_KEY = 'hello-world-plugin'

    // ──────────────────────────────────────────
    // 工具函数
    // ──────────────────────────────────────────
    function getConfig(key, defaultVal) {
        if (window.PoetizePlugin && window.PoetizePlugin.api) {
            return window.PoetizePlugin.api.getConfig(PLUGIN_KEY, key, defaultVal)
        }
        return defaultVal
    }

    // ──────────────────────────────────────────
    // onArticleRender：文章内容渲染完成
    // ctx = { articleId, title, element }
    // ──────────────────────────────────────────
    if (window.PoetizePlugin) {
        window.PoetizePlugin.on('onArticleRender', function (ctx) {
            var greeting = getConfig('greeting', '👋 感谢阅读！')
            var bgColor = getConfig('bgColor', '#f0f4ff')

            // 防重复注入
            if (document.getElementById('hw-plugin-banner')) return

            var banner = document.createElement('div')
            banner.id = 'hw-plugin-banner'
            banner.className = 'hw-plugin-banner'
            banner.innerHTML =
                '<span class="hw-icon">🔌</span>' +
                '<span class="hw-text">' + greeting + '</span>' +
                '<span class="hw-badge">Hello World 插件</span>'
            banner.style.background = bgColor

            if (ctx.element) {
                ctx.element.appendChild(banner)
            }

            console.log('[HelloWorldPlugin] 文章渲染钩子已触发，标题:', ctx && ctx.title)
        })
    }

    // ──────────────────────────────────────────
    // 插件加载日志
    // ──────────────────────────────────────────
    console.log('[HelloWorldPlugin] 前端插件已加载 v1.0.0')
})()
