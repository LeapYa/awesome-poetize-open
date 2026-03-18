/**
 * 插件 SDK
 * 前端插件通过此 SDK 注册钩子和调用平台 API
 *
 * 使用方式（在插件的 frontend/index.js 中）：
 *   PoetizePlugin.on('onArticleRender', function(ctx) {
 *     ctx.element.insertAdjacentHTML('beforeend', '<div>自定义内容</div>')
 *   })
 */
(function (global) {
    'use strict'

    // ===== 内部状态 =====
    const _hooks = {}
    const _configs = {}
    const _pluginKeys = []

    function createPluginSourceUrl(pluginKey) {
        return 'poetize-plugin-' + String(pluginKey || 'unknown').replace(/[^a-zA-Z0-9_-]/g, '_') + '.js'
    }

    // ===== 公共 API =====
    const PoetizePlugin = {
        // ----------------------------------------------------------------
        // 钩子系统
        // ----------------------------------------------------------------

        /**
         * 注册钩子监听
         * @param {string} hookName 钩子名称
         * @param {Function} callback 回调函数，接收 context 对象
         */
        on(hookName, callback) {
            if (!_hooks[hookName]) _hooks[hookName] = []
            _hooks[hookName].push(callback)
        },

        /**
         * 触发钩子（内部调用，插件无需调用此方法）
         * @param {string} hookName
         * @param {object} context
         */
        emit(hookName, context) {
            const handlers = _hooks[hookName] || []
            handlers.forEach((fn) => {
                try {
                    fn(context)
                } catch (e) {
                    console.error('[PoetizePlugin] 钩子 ' + hookName + ' 执行错误:', e)
                }
            })
        },

        // ----------------------------------------------------------------
        // 平台 API（插件可调用）
        // ----------------------------------------------------------------
        api: {
            /**
             * 获取当前文章数据（仅文章页有效）
             */
            getArticle() {
                return global.__poetize_article__ || null
            },

            /**
             * 获取当前登录用户（未登录返回 null）
             */
            getUser() {
                return global.__poetize_user__ || null
            },

            /**
             * 获取插件配置
             * @param {string} pluginKey 插件 key
             * @param {string} configKey 配置项 key
             * @param {*} defaultValue 默认值
             */
            getConfig(pluginKey, configKey, defaultValue) {
                const config = _configs[pluginKey] || {}
                return config[configKey] !== undefined ? config[configKey] : defaultValue
            },

            /**
             * 显示 Toast 提示
             * @param {string} message
             * @param {'success'|'warning'|'error'|'info'} type
             */
            showToast(message, type = 'info') {
                if (global.__poetize_toast__) {
                    global.__poetize_toast__(message, type)
                } else {
                    console.log('[PoetizePlugin Toast]', type, message)
                }
            },

            /**
             * 在指定位置插入 HTML
             * @param {Element} targetElement 目标元素
             * @param {'beforebegin'|'afterbegin'|'beforeend'|'afterend'} position
             * @param {string} html HTML 字符串
             */
            insertHtml(targetElement, position, html) {
                if (targetElement && typeof targetElement.insertAdjacentHTML === 'function') {
                    targetElement.insertAdjacentHTML(position, html)
                }
            },

            /**
             * 注册侧边栏小组件（返回挂载点元素）
             * @param {string} widgetId 唯一 ID
             * @param {string} title 标题
             */
            registerSidebarWidget(widgetId, title) {
                const mountId = 'plugin-sidebar-' + widgetId
                const sidebar = document.querySelector('.sidebar-wrapper, .right-panel, [class*="sidebar"]')
                if (!sidebar) return null
                let el = document.getElementById(mountId)
                if (!el) {
                    el = document.createElement('div')
                    el.id = mountId
                    el.className = 'plugin-sidebar-widget'
                    el.innerHTML = title ? '<div class="plugin-widget-title">' + title + '</div><div class="plugin-widget-body"></div>' : ''
                    sidebar.appendChild(el)
                }
                return el.querySelector('.plugin-widget-body') || el
            },
        },

        // ----------------------------------------------------------------
        // 内部方法（框架调用，插件不应直接使用）
        // ----------------------------------------------------------------
        _internal: {
            /** 注册插件配置 */
            setPluginConfig(pluginKey, config) {
                _configs[pluginKey] = config || {}
                if (!_pluginKeys.includes(pluginKey)) {
                    _pluginKeys.push(pluginKey)
                }
            },

            /** 加载并执行插件前端 JS 代码 */
            loadPluginCode(pluginKey, jsCode, config) {
                PoetizePlugin._internal.setPluginConfig(pluginKey, config)

                if (!jsCode || typeof jsCode !== 'string') {
                    return
                }

                try {
                    const runner = new Function(
                        'PoetizePlugin',
                        'window',
                        'document',
                        'config',
                        `${jsCode}\n//# sourceURL=${createPluginSourceUrl(pluginKey)}`
                    )
                    runner(PoetizePlugin, global, global.document, config || {})
                } catch (error) {
                    console.error('[PoetizePlugin] 插件前端代码执行失败 (' + pluginKey + '):', error)
                }
            },

            /** 加载插件 CSS */
            loadPluginCss(pluginKey, cssCode) {
                if (!cssCode) return
                const styleId = 'plugin-style-' + pluginKey
                if (document.getElementById(styleId)) return
                const style = document.createElement('style')
                style.id = styleId
                style.textContent = cssCode
                document.head.appendChild(style)
            },
        },
    }

    // 挂载到全局
    global.PoetizePlugin = PoetizePlugin

})(typeof window !== 'undefined' ? window : globalThis)
