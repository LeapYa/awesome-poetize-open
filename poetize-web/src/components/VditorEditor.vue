<template>
  <div class="vditor-editor-isolate">
    <div ref="vditorContainer" class="vditor-wrapper"></div>
  </div>
</template>

<script>
import { $on, $off, $once, $emit } from '../utils/gogocodeTransfer'
import Vditor from 'vditor'
import { loadEChartsResources } from '@/utils/resourceLoaders/resourceLoader'
import {
  parseEChartsOption,
  extractEChartsChartTypes,
} from '@/utils/echartsOptionParser'
// 注意：Vditor CSS 动态加载，只在需要时引入
let vditorStyleLoaded = false

function loadVditorStyle() {
  if (!vditorStyleLoaded && typeof document !== 'undefined') {
    import('vditor/dist/index.css')
    vditorStyleLoaded = true
  }
}

export default {
  name: 'VditorEditor',
  props: {
    value: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: '请输入内容...',
    },
    height: {
      type: [String, Number],
      default: 600,
    },
    mode: {
      type: String,
      default: 'ir', // ir: 即时渲染, sv: 分屏预览, wysiwyg: 所见即所得
      validator: (value) => ['ir', 'sv', 'wysiwyg'].includes(value),
    },
    toolbarConfig: {
      type: Object,
      default: () => ({}),
    },
    upload: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      editor: null,
      isComposing: false,
      isDarkMode: false,
      isInternalUpdate: false, // 标记是否为内部更新，避免循环
    }
  },
  watch: {
    value(newVal) {
      // 如果是内部更新触发的，不处理
      if (this.isInternalUpdate) {
        return
      }

      if (this.editor && !this.isComposing) {
        const currentValue = this.editor.getValue()
        const displayValue = this.upgradeMarkdownHeadings(newVal || '')

        // 只有当内容真正不同时才更新编辑器
        // 这样可以避免用户输入时触发不必要的 setValue
        if (currentValue !== displayValue) {
          this.editor.setValue(displayValue)
        }
      }
    },
  },
  mounted() {
    // 动态加载 Vditor 样式
    loadVditorStyle()
    // 检测初始暗色模式状态
    this.checkDarkMode()
    this.initEditor()
    // 监听暗色模式变化
    this.setupThemeListener()
  },
  beforeUnmount() {
    // 清理全屏监听器
    if (this._fullscreenObserver) {
      this._fullscreenObserver.disconnect()
      this._fullscreenObserver = null
    }

    // 清理代码块监听器
    if (this._codeBlockObserver) {
      this._codeBlockObserver.disconnect()
      this._codeBlockObserver = null
    }

    // 清理代码块计时器
    if (this._codeBlockTimer) {
      clearTimeout(this._codeBlockTimer)
      this._codeBlockTimer = null
    }

    // 清理预览观察器
    if (this._previewObserver) {
      this._previewObserver.disconnect()
      this._previewObserver = null
    }

    // 清理 ECharts 渲染计时器
    if (this._echartsRenderTimer) {
      clearTimeout(this._echartsRenderTimer)
      this._echartsRenderTimer = null
    }

    // 清理所有 ECharts 实例
    if (this._echartsInstances) {
      this._echartsInstances.forEach((chart) => {
        try {
          chart.dispose()
        } catch (e) {}
      })
      this._echartsInstances = []
    }

    // 移除主题监听
    if (this._themeListener) {
      $off(this.$root, 'theme-changed', this._themeListener)
      window.removeEventListener('storage', this._storageListener)
    }

    if (this.editor) {
      this.editor.destroy()
      this.editor = null
    }
  },
  methods: {
    initEditor() {
      // 检查 window.hljs 是否可用（优先使用项目的 hljs）
      if (typeof window.hljs !== 'undefined') {
      } else {
      }

      const toolbar = [
        'emoji',
        'headings',
        'bold',
        'italic',
        'strike',
        'link',
        '|',
        'list',
        'ordered-list',
        'check',
        'outdent',
        'indent',
        '|',
        'quote',
        'line',
        'code',
        'inline-code',
        'insert-before',
        'insert-after',
        '|',
        'upload',
        {
          name: 'insert-image-link',
          tip: '插入图片链接',
          icon: '<svg viewBox="0 0 1024 1024"><path d="M959.877 128l0.123 0.123v767.775l-0.123 0.122H64.102l-0.122-0.122V128.123l0.122-0.123h895.775zM960 64H64C28.795 64 0 92.795 0 128v768c0 35.205 28.795 64 64 64h896c35.205 0 64-28.795 64-64V128c0-35.205-28.795-64-64-64zM832 288.01c0 53.023-42.988 96.01-96.01 96.01s-96.01-42.987-96.01-96.01S682.967 192 735.99 192 832 234.988 832 288.01zM896 832H128V704l224.01-384 256 320h64l224.01-192z"></path></svg>',
          click: () => {
            // 直接插入图片Markdown模板
            this.editor.insertValue('![图片描述](url)')
            this.editor.focus()
          },
        },
        'table',
        '|',
        'undo',
        'redo',
        '|',
        'fullscreen',
        'edit-mode',
        {
          name: 'more',
          toolbar: [
            'both',
            'code-theme',
            'content-theme',
            'export',
            'outline',
            'preview',
            'devtools',
            'info',
            'help',
          ],
        },
      ]

      const uploadConfig = this.upload || {
        accept: 'image/*',
        handler: (files) => {
          // 触发自定义上传事件
          $emit(this, 'image-add', files[0])
          return null
        },
      }

      this.editor = new Vditor(this.$refs.vditorContainer, {
        height:
          typeof this.height === 'number' ? this.height : parseInt(this.height),
        placeholder: this.placeholder,
        mode: this.mode,
        lang: 'zh_CN',
        i18nPath: '/static/vditor/i18n',
        theme: this.isDarkMode ? 'dark' : 'classic', // 编辑器整体主题
        toolbar,
        toolbarConfig: {
          pin: true,
          ...this.toolbarConfig,
        },
        cache: {
          enable: false,
        },
        preview: {
          markdown: {
            toc: true,
            mark: true,
            footnotes: true,
            autoSpace: true,
          },
          math: {
            inlineDigit: true,
            engine: 'KaTeX',
          },
          hljs: {
            enable: true,
            lineNumber: true, // 启用行号
            // 固定使用 GitHub 主题，与文章显示页保持一致
            style: 'github',
          },
          theme: {
            current: this.isDarkMode ? 'dark' : 'light',
          },
        },
        hint: {
          emojiPath:
            'https://cdn.jsdelivr.net/npm/vditor@3.9.0/dist/images/emoji',
        },
        upload: uploadConfig,
        // 所见即所得模式下的自定义工具栏配置，返回空数组表示不自定义
        customWysiwygToolbar: () => {
          return []
        },
        input: (value) => {
          if (!this.isComposing) {
            // 用户输入时，降级标题后发送给父组件（保存到数据库）
            const downgradedValue = this.downgradeMarkdownHeadings(value)

            // 设置标志，防止 watch 触发 setValue 导致光标跳转
            this.isInternalUpdate = true
            $emit(this, 'update:value', downgradedValue)
            $emit(this, 'change', downgradedValue)

            // 下一个 tick 后重置标志
            this.$nextTick(() => {
              this.isInternalUpdate = false
            })

            // 内容变化后应用自定义代码样式
            this.applyCustomCodeStyle()

            // 手动触发工具栏添加（用于 IR/WYSIWYG 模式）
            this.addCodeLanguageLabels()
          }
        },
        focus: () => {
          $emit(this, 'focus')
        },
        blur: () => {
          $emit(this, 'blur')
        },
        after: () => {
          if (this.value) {
            // 初始化时也需要升级标题
            const displayValue = this.upgradeMarkdownHeadings(this.value)
            this.editor.setValue(displayValue)
          }
          // 应用自定义代码高亮样式
          this.applyCustomCodeStyle()
          // 添加代码块语言标签
          this.addCodeLanguageLabels()
          // 设置代码块监听器
          this.setupCodeBlockObserver()
          // 渲染 ECharts 图表（预览区域）
          this.renderEChartsInPreview()
          $emit(this, 'ready', this.editor)

          // 监听全屏事件，将编辑器移到 body
          this.setupFullscreenHandler()

          // 监听预览内容变化，实时渲染 ECharts
          const previewElement =
            this.$refs.vditorContainer.querySelector('.vditor-preview')
          if (previewElement) {
            const observer = new MutationObserver(() => {
              // 使用防抖避免频繁渲染
              clearTimeout(this._echartsRenderTimer)
              this._echartsRenderTimer = setTimeout(() => {
                this.renderEChartsInPreview()
              }, 300)
            })
            observer.observe(previewElement, {
              childList: true,
              subtree: true,
            })
            this._previewObserver = observer
          }
        },
      })

      // 监听中文输入
      const editArea = this.$refs.vditorContainer.querySelector(
        '.vditor-ir, .vditor-sv, .vditor-wysiwyg'
      )
      if (editArea) {
        editArea.addEventListener('compositionstart', () => {
          this.isComposing = true
        })
        editArea.addEventListener('compositionend', () => {
          this.isComposing = false
          // 中文输入结束后，降级标题后发送给父组件
          const downgradedValue = this.downgradeMarkdownHeadings(
            this.editor.getValue()
          )

          // 设置标志，防止 watch 触发 setValue 导致光标跳转
          this.isInternalUpdate = true
          $emit(this, 'update:value', downgradedValue)
          $emit(this, 'change', downgradedValue)

          // 下一个 tick 后重置标志
          this.$nextTick(() => {
            this.isInternalUpdate = false
          })
        })
      }
    },
    getValue() {
      // 获取编辑器内容时，降级标题后返回（保存到数据库）
      const editorValue = this.editor ? this.editor.getValue() : ''
      return this.downgradeMarkdownHeadings(editorValue)
    },
    setValue(value) {
      if (this.editor) {
        // 设置编辑器内容时，升级标题后显示
        const displayValue = this.upgradeMarkdownHeadings(value || '')
        this.editor.setValue(displayValue)
      }
    },
    insertValue(value) {
      if (this.editor) {
        this.editor.insertValue(value)
      }
    },
    focus() {
      if (this.editor) {
        this.editor.focus()
      }
    },
    blur() {
      if (this.editor) {
        this.editor.blur()
      }
    },
    disabled() {
      if (this.editor) {
        this.editor.disabled()
      }
    },
    enable() {
      if (this.editor) {
        this.editor.enable()
      }
    },
    getHTML() {
      return this.editor ? this.editor.getHTML() : ''
    },
    // Markdown 标题降级（保存到数据库时用）
    // # → ##, ## → ###, ### → ####, #### → #####, ##### → ######
    downgradeMarkdownHeadings(markdown) {
      if (!markdown) return ''

      // 按行处理，避免代码块中的 # 被误处理
      const lines = markdown.split('\n')
      let inCodeBlock = false

      const processedLines = lines.map((line) => {
        // 检测代码块
        if (line.trim().startsWith('```')) {
          inCodeBlock = !inCodeBlock
          return line
        }

        // 代码块内不处理
        if (inCodeBlock) {
          return line
        }

        // 处理标题行：在开头的 # 前面添加一个 #
        // 匹配行首的标题标记（支持空格）
        if (/^\s*#{1,5}\s/.test(line)) {
          return line.replace(/^(\s*)(#{1,5})(\s)/, '$1#$2$3')
        }

        return line
      })

      return processedLines.join('\n')
    },
    // Markdown 标题升级（从数据库读取时用于显示）
    // ## → #, ### → ##, #### → ###, ##### → ####, ###### → #####
    upgradeMarkdownHeadings(markdown) {
      if (!markdown) return ''

      // 按行处理，避免代码块中的 # 被误处理
      const lines = markdown.split('\n')
      let inCodeBlock = false

      const processedLines = lines.map((line) => {
        // 检测代码块
        if (line.trim().startsWith('```')) {
          inCodeBlock = !inCodeBlock
          return line
        }

        // 代码块内不处理
        if (inCodeBlock) {
          return line
        }

        // 处理标题行：移除开头的一个 #
        // 匹配行首的标题标记（2-6个#）
        if (/^\s*#{2,6}\s/.test(line)) {
          return line.replace(/^(\s*)#{1}(#{1,5}\s)/, '$1$2')
        }

        return line
      })

      return processedLines.join('\n')
    },
    // 应用自定义代码高亮样式
    applyCustomCodeStyle() {},
    // 添加代码块语言标签和复制按钮（使用 flex 布局）
    addCodeLanguageLabels() {
      // WYSIWYG 模式下不添加自定义工具栏，保持原生编辑体验
      // 这样用户可以直接点击代码块进入编辑状态
      if (this.mode === 'wysiwyg') {
        return
      }

      // 增加延迟时间，确保 Vditor 已经完全渲染代码块
      // IR 模式需要更长的渲染时间
      setTimeout(() => {
        if (!this.$refs.vditorContainer) return

        // 查找所有预览区域的 pre 元素
        let preElements = []

        // SV 分屏模式
        const svPre = this.$refs.vditorContainer.querySelectorAll(
          '.vditor-preview .vditor-reset pre:not(.vditor-ir__marker):not(.vditor-ir__marker--pre)'
        )
        if (svPre.length > 0) {
          preElements = preElements.concat(Array.from(svPre))
        }

        // IR 即时渲染模式 - pre 本身有 vditor-ir__preview 类名
        const irPre = this.$refs.vditorContainer.querySelectorAll(
          'pre.vditor-ir__preview'
        )
        if (irPre.length > 0) {
          preElements = preElements.concat(Array.from(irPre))
        }

        preElements.forEach((pre, i) => {
          // 检查是否已经被包装
          if (
            pre.parentNode &&
            pre.parentNode.classList.contains('code-block-wrapper')
          ) {
            return
          }

          // 跳过数学公式块（检查多种可能的结构）
          // 1. 检查父元素是否有 data-type="math-block"
          if (
            pre.parentNode &&
            pre.parentNode.getAttribute &&
            pre.parentNode.getAttribute('data-type') === 'math-block'
          ) {
            return
          }

          // 2. 检查 pre 内是否包含 .language-math
          if (
            pre.querySelector('.language-math') ||
            pre.querySelector('[data-type="math-block"]')
          ) {
            return
          }

          // 3. 检查 pre 元素是否包含 KaTeX 相关的类名或属性
          if (
            pre.classList.contains('katex-display') ||
            pre.classList.contains('mathjax-display') ||
            pre.querySelector('.katex') ||
            pre.querySelector('.katex-display') ||
            pre.querySelector('.katex-html')
          ) {
            return
          }

          const code = pre.querySelector('code')
          if (!code) return

          // 从 className 中提取语言名称
          let lang = ''
          const classNameStr = code.className || ''
          const classNameArr = classNameStr.split(' ')

          classNameArr.some((className) => {
            if (className.indexOf('language-') > -1) {
              lang = className.substring(className.indexOf('-') + 1)
              return true
            }
            return false
          })

          // 跳过 Mermaid 代码块和数学公式
          if (lang === 'mermaid' || lang === 'math' || lang === 'katex') {
            return
          }

          // 设置唯一ID
          code.id = 'vditor-hljs-' + i

          // 创建顶部工具栏（使用 flex 布局）
          const toolbar = document.createElement('div')
          toolbar.className = 'code-header-toolbar'

          // 创建三个圆点装饰
          const dots = document.createElement('div')
          dots.className = 'code-dots'
          toolbar.appendChild(dots)

          // 创建语言标签（直接使用用户填写的原始名称）
          if (lang) {
            const langLabel = document.createElement('div')
            langLabel.className = 'code-language-label'
            langLabel.textContent = lang
            toolbar.appendChild(langLabel)
          }

          // 创建复制按钮
          const copyButton = document.createElement('a')
          copyButton.className = 'copy-code'
          copyButton.href = 'javascript:;'
          copyButton.setAttribute('data-clipboard-target', '#vditor-hljs-' + i)
          copyButton.innerHTML =
            '<i class="fa fa-clipboard" aria-hidden="true"></i>'
          toolbar.appendChild(copyButton)

          // 创建包装容器
          try {
            // 创建包装容器
            const wrapper = document.createElement('div')
            wrapper.className = 'code-block-wrapper'

            // 用包装器替换 pre 的位置
            pre.parentNode.insertBefore(wrapper, pre)

            // 将工具栏和 pre 都放入包装器
            wrapper.appendChild(toolbar)
            wrapper.appendChild(pre)
          } catch (e) {
            console.error('创建包装器失败:', e)
          }
        })

        // 初始化剪贴板功能（使用 ClipboardJS）
        if (typeof ClipboardJS !== 'undefined') {
          const that = this // 保存Vue实例引用
          const clipboard = new ClipboardJS('.copy-code')

          // 复制成功回调
          clipboard.on('success', (e) => {
            that.$message({
              message: '代码已复制到剪贴板',
              type: 'success',
              duration: 2000,
            })
            e.clearSelection()
          })

          // 复制失败回调
          clipboard.on('error', (e) => {
            that.$message({
              message: '复制失败，请手动复制',
              type: 'error',
              duration: 2000,
            })
          })
        }
      }, 500)
    },
    // 检查暗色模式状态
    checkDarkMode() {
      // 从localStorage读取主题设置
      const theme = localStorage.getItem('theme')
      if (theme === 'dark') {
        this.isDarkMode = true
      } else if (theme === 'light') {
        this.isDarkMode = false
      } else {
        // 如果没有设置，检查body的class
        this.isDarkMode = document.body.classList.contains('dark-mode')
      }
    },
    // 监听主题变化
    setupThemeListener() {
      // 监听全局主题变化事件（由 admin.vue 触发）
      this._themeListener = (isDark) => {
        this.isDarkMode = isDark
        this.switchEditorTheme(isDark)
      }
      $on(this.$root, 'theme-changed', this._themeListener)

      // 监听 storage 事件（跨标签页）
      this._storageListener = (e) => {
        if (e.key === 'theme') {
          const isDark = e.newValue === 'dark'
          this.isDarkMode = isDark
          this.switchEditorTheme(isDark)
        }
      }
      window.addEventListener('storage', this._storageListener)
    },
    // 渲染 ECharts 图表（预览区域）
    async renderEChartsInPreview() {
      // 防止重复执行
      if (this._isRenderingECharts) {
        return
      }

      // 获取预览容器（支持所有模式）
      const previewElement =
        this.$refs.vditorContainer.querySelector('.vditor-preview') ||
        this.$refs.vditorContainer.querySelector('.vditor-ir__preview') ||
        this.$refs.vditorContainer.querySelector('.vditor-wysiwyg__preview')

      if (!previewElement) return

      // 查找所有 echarts 代码块
      const echartsBlocks = previewElement.querySelectorAll(
        'pre code.language-echarts'
      )

      if (echartsBlocks.length === 0) return

      // 初始化实例数组
      if (!this._echartsInstances) {
        this._echartsInstances = []
      }

      const parsedConfigs = new Map()
      const requiredChartTypes = new Set()

      for (let i = 0; i < echartsBlocks.length; i++) {
        const codeBlock = echartsBlocks[i]
        const pre = codeBlock.parentElement

        if (
          !pre ||
          pre.classList.contains('echarts-rendered') ||
          pre.hasAttribute('data-echarts-rendered')
        ) {
          continue
        }

        try {
          const code = codeBlock.textContent
          const config = await parseEChartsOption(code)
          parsedConfigs.set(pre, config)

          const chartTypes = await extractEChartsChartTypes(config)
          chartTypes.forEach((type) => requiredChartTypes.add(type))
        } catch (error) {
          console.error('Vditor 预览中 ECharts 配置解析失败:', error)
        }
      }

      if (parsedConfigs.size === 0) {
        return
      }

      await loadEChartsResources(Array.from(requiredChartTypes))

      const echarts = window.echarts
      if (!echarts) {
        return
      }

      this._isRenderingECharts = true

      try {
        echartsBlocks.forEach((codeBlock, index) => {
          try {
            const pre = codeBlock.parentElement

            // 跳过已经渲染过的
            if (
              pre.classList.contains('echarts-rendered') ||
              pre.hasAttribute('data-echarts-rendered')
            ) {
              return
            }

            // 标记为已渲染（在替换前标记）
            pre.classList.add('echarts-rendered')
            pre.setAttribute('data-echarts-rendered', 'true')

            // 解析 JSON 配置
            const code = codeBlock.textContent
            const config = parsedConfigs.get(pre)
            if (!config) {
              return
            }

            // 创建容器
            const container = document.createElement('div')
            container.className = 'echarts-container vditor-echarts'
            container.style.width = '100%'
            container.style.height = config.height || '400px'
            container.style.marginBottom = '20px'

            // 保存原始配置
            container.setAttribute('data-echarts-config', code)

            // 替换代码块
            pre.parentNode.replaceChild(container, pre)

            // 初始化图表
            const chart = echarts.init(
              container,
              this.isDarkMode ? 'dark' : 'light'
            )

            // 设置配置
            const finalConfig = {
              animation: true,
              animationDuration: 1000,
              animationEasing: 'cubicOut',
              animationDelay: 0,
              backgroundColor: 'transparent',
              ...config,
            }

            chart.setOption(finalConfig)

            // 保存实例
            this._echartsInstances.push(chart)
            container._echartsInstance = chart
          } catch (error) {
            console.error('Vditor 预览中 ECharts 渲染失败:', error)
          }
        })
      } finally {
        this._isRenderingECharts = false
      }
    },
    // 切换编辑器主题
    switchEditorTheme(isDark) {
      if (!this.editor) return

      try {
        const echarts = window.echarts
        if (!echarts) {
          return
        }

        // 切换 ECharts 主题
        if (this._echartsInstances && this._echartsInstances.length > 0) {
          const previewElement =
            this.$refs.vditorContainer.querySelector('.vditor-preview')
          if (previewElement) {
            const echartsContainers =
              previewElement.querySelectorAll('.vditor-echarts')
            echartsContainers.forEach((container, index) => {
              const chart = this._echartsInstances[index]
              if (chart) {
                // 销毁旧实例
                chart.dispose()

                // 用新主题重新初始化
                const newChart = echarts.init(
                  container,
                  isDark ? 'dark' : 'light'
                )

                // 获取原始配置（从容器的 textContent 获取）
                const config = chart.getOption()
                if (config) {
                  newChart.setOption(config, true)
                }

                // 更新实例引用
                this._echartsInstances[index] = newChart
              }
            })
          }
        }

        // 使用 Vditor 官方的 setTheme 方法
        // 参数：编辑器主题, 内容主题, 代码高亮主题
        const editorTheme = isDark ? 'dark' : 'classic'
        const contentTheme = isDark ? 'dark' : 'light'
        // 固定使用 GitHub 代码高亮主题，与文章显示页保持一致
        const codeTheme = 'github'

        if (this.editor.setTheme) {
          this.editor.setTheme(editorTheme, contentTheme, codeTheme)
        } else {
        }
      } catch (error) {}
    },
    // 设置代码块观察器，监听 DOM 变化并自动添加工具栏
    setupCodeBlockObserver() {
      // WYSIWYG 模式下不启动观察器，避免干扰原生编辑体验
      if (this.mode === 'wysiwyg') {
        return
      }

      this.$nextTick(() => {
        setTimeout(() => {
          if (!this.$refs.vditorContainer) return

          // 创建观察器
          const observer = new MutationObserver((mutations) => {
            // 使用防抖，避免频繁调用
            clearTimeout(this._codeBlockTimer)
            this._codeBlockTimer = setTimeout(() => {
              this.addCodeLanguageLabels()
            }, 300)
          })

          // 观察包含预览区域的父容器（不包括 WYSIWYG）
          const contentAreas = [
            this.$refs.vditorContainer.querySelector('.vditor-preview'),
            this.$refs.vditorContainer.querySelector('.vditor-ir'),
          ]

          contentAreas.forEach((area) => {
            if (area) {
              observer.observe(area, {
                childList: true,
                subtree: true,
                attributes: false,
              })
            }
          })

          // 保存观察器引用以便清理
          this._codeBlockObserver = observer
        }, 100)
      })
    },
    setupFullscreenHandler() {
      // 保存原始父节点和位置信息
      let originalParent = null
      let originalNextSibling = null

      // 延迟查找元素，确保 DOM 已渲染
      this.$nextTick(() => {
        setTimeout(() => {
          // 监听全屏变化
          const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
              if (
                mutation.type === 'attributes' &&
                mutation.attributeName === 'class'
              ) {
                const vditor = mutation.target
                const isFullscreen =
                  vditor.classList.contains('vditor--fullscreen')

                if (isFullscreen && !originalParent) {
                  // 进入全屏：移到 body
                  originalParent = this.$refs.vditorContainer.parentNode
                  originalNextSibling = this.$refs.vditorContainer.nextSibling

                  document.body.appendChild(this.$refs.vditorContainer)
                } else if (!isFullscreen && originalParent) {
                  // 退出全屏：移回原位置

                  if (
                    originalNextSibling &&
                    originalNextSibling.parentNode === originalParent
                  ) {
                    originalParent.insertBefore(
                      this.$refs.vditorContainer,
                      originalNextSibling
                    )
                  } else {
                    originalParent.appendChild(this.$refs.vditorContainer)
                  }

                  originalParent = null
                  originalNextSibling = null
                }
              }
            })
          })

          // vditorContainer 本身就是带有 vditor 类的元素
          const vditorElement = this.$refs.vditorContainer

          if (vditorElement) {
            observer.observe(vditorElement, {
              attributes: true,
              attributeFilter: ['class'],
            })

            // 保存 observer 以便销毁时清理
            this._fullscreenObserver = observer
          } else {
            console.error('未找到 vditorContainer')
          }
        }, 100)
      })
    },
  },
  emits: ['image-add', 'update:value', 'change', 'ready', 'focus', 'blur'],
}
</script>

<style scoped src="@/assets/css/vditoreditor.css"></style>
<style src="@/assets/css/vditoreditor-global.css"></style>
