/**
 * AI聊天 - Pinia Store
 * Vue2.7版本
 */
import { defineStore } from 'pinia'
import constant from '@/utils/constant'

export const useAIChatStore = defineStore('aiChat', {
  state: () => ({
    // 消息列表
    messages: [],

    // AI配置
    config: null,
    configLoaded: false,

    // 状态
    streaming: false,
    typing: false,
    connected: false,

    // 用户信息
    currentUser: null,

    // 速率限制
    rateLimitData: {
      count: 0,
      resetTime: 0,
    },

    // 中断控制
    abortController: null,
    shouldStop: false,

    // 编辑状态
    editingMessageId: null,
    editingContent: '',
    editingOriginalAttachedPage: null, // 编辑时保存的原始附加页面

    // 页面上下文
    attachedPageContext: null, // 附加的页面内容
  }),

  getters: {
    /**
     * 是否需要登录
     */
    requireLogin: (state) => {
      const requireLogin = state.config?.require_login || false
      return requireLogin
    },

    /**
     * 是否启用流式响应
     */
    isStreamingEnabled: (state) => {
      return state.config?.streaming_enabled === true
    },

    /**
     * 主题颜色
     */
    themeColor: (state) => {
      return state.config?.theme_color || '#4facfe'
    },

    /**
     * 消息历史（用于API发送）
     */
    messageHistory: (state) => {
      const maxLength = state.config?.max_conversation_length || 20
      return state.messages.slice(-maxLength).map((msg) => ({
        role: msg.role,
        content: msg.content,
      }))
    },
  },

  actions: {
    /**
     * 初始化AI聊天（延迟加载，仅在打开聊天窗口时调用）
     */
    async init() {
      // 加载配置
      await this.loadConfig()

      // 恢复聊天历史
      this.restoreHistory()

      // 检查用户登录状态
      this.checkUserLogin()

      // 如果是首次打开聊天（没有历史记录），添加欢迎消息
      if (this.messages.length === 0) {
        this.addWelcomeMessage()
      }
    },

    /**
     * 轻量级初始化（仅恢复历史，不加载配置）
     */
    lightInit() {
      // 仅恢复历史和检查登录状态
      this.restoreHistory()
      this.checkUserLogin()
    },

    /**
     * 添加欢迎消息
     */
    addWelcomeMessage() {
      // 使用配置中的欢迎消息
      const welcomeText =
        this.config?.welcome_message ||
        this.config?.welcomeMessage ||
        '你好！我是你的AI助手，有什么可以帮助你的吗？'

      this.addMessage(welcomeText, 'assistant', { isWelcome: true })
    },

    /**
     * 加载AI配置（延迟加载）
     */
    async loadConfig() {
      // 如果已加载，直接返回
      if (this.configLoaded) {
        return
      }

      try {
        const response = await fetch(
          `${constant.baseURL}/webInfo/ai/config/chat/getStreamingConfig?configName=default`
        )

        if (response.ok) {
          const result = await response.json()
          // Java 接口返回格式：{ code: 200, message: "", data: {...} }
          if (result.code === 200 && result.data) {
            this.config = result.data
            this.configLoaded = true

            // 缓存配置
            localStorage.setItem('ai_chat_config', JSON.stringify(this.config))
          } else {
            throw new Error(result.message || '配置加载失败')
          }
        } else {
          throw new Error('配置加载失败')
        }
      } catch (error) {
        // 使用缓存配置
        const cached = localStorage.getItem('ai_chat_config')
        if (cached) {
          this.config = JSON.parse(cached)
          this.configLoaded = true
        } else {
          // 使用默认配置
          this.config = {
            chat_name: 'AI助手',
            welcome_message: '你好！我是你的AI助手，有什么可以帮助你的吗？',
            theme_color: '#4facfe',
            enable_streaming: false,
            require_login: false,
            max_message_length: 500,
            rate_limit: 20,
          }
        }
      }
    },

    /**
     * 检查用户登录状态
     */
    checkUserLogin() {
      try {
        const userStr =
          localStorage.getItem('currentUser') ||
          sessionStorage.getItem('currentUser')
        if (userStr) {
          this.currentUser = JSON.parse(userStr)
        } else {
          this.currentUser = null
        }
      } catch (error) {
        this.currentUser = null
      }
    },

    /**
     * 添加消息
     */
    addMessage(content, role = 'user', metadata = {}) {
      const message = {
        id: Date.now() + Math.random(),
        role,
        content,
        timestamp: Date.now(),
        isNew: true, // 标记为新消息，启用打字机效果
        ...metadata,
      }

      this.messages.push(message)

      // 保存到localStorage
      this.saveHistory()

      return message
    },

    /**
     * 更新消息（用于流式响应）
     */
    updateMessage(messageId, content) {
      const message = this.messages.find((m) => m.id === messageId)
      if (message) {
        message.content = content
        this.saveHistory()
      }
    },

    /**
     * 发送消息
     */
    async sendMessage(content) {
      // 验证消息长度
      const maxLength = this.config?.max_message_length || 500
      if (content.length > maxLength) {
        return {
          success: false,
          error: 'too_long',
          message: `消息太长了，请控制在${maxLength}个字符以内`,
        }
      }

      // 检查速率限制
      if (!this.checkRateLimit()) {
        const remainingTime = Math.ceil(
          (this.rateLimitData.resetTime - Date.now()) / 1000
        )
        return {
          success: false,
          error: 'rate_limit',
          message: `发送频率太快了，请等待${remainingTime}秒后再试`,
        }
      }

      // 内容过滤
      if (this.config?.enable_content_filter) {
        const filtered = this.filterContent(content)
        if (!filtered.pass) {
          return {
            success: false,
            error: 'content_filter',
            message: '请文明聊天，避免使用不当词汇',
          }
        }
      }

      // 先添加用户消息（如果有附加页面，保存到metadata中）
      const messageMetadata = {}
      if (this.attachedPageContext) {
        messageMetadata.attachedPage = {
          title: this.attachedPageContext.title,
          type: this.attachedPageContext.type,
          url: this.attachedPageContext.url,
        }
      }
      this.addMessage(content, 'user', messageMetadata)

      // 检查localStorage中的用户信息
      const userInStorage = localStorage.getItem('currentUser')

      // 重新检查登录状态（用户可能在打开聊天窗口后才登录）
      this.checkUserLogin()

      if (this.requireLogin && !this.currentUser) {
        return {
          success: false,
          error: 'require_login',
          message: '需要登录后才能使用聊天功能',
        }
      }

      // 发送到后端
      try {
        if (this.isStreamingEnabled) {
          return await this.sendStreamingMessage(content)
        } else {
          return await this.sendNormalMessage(content)
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        return {
          success: false,
          error: 'network',
          message: '网络错误，请稍后重试',
        }
      }
    },

    /**
     * 提取当前页面内容
     */
    extractCurrentPageContent() {
      try {
        const route = window.location.pathname

        // 文章页面
        if (route.includes('/article/')) {
          const title =
            document.querySelector('.article-title')?.innerText || ''
          const content =
            document.querySelector('.entry-content')?.innerText || ''
          const author =
            document.querySelector('.article-info span')?.innerText || ''

          // 提取语言信息
          const languageInfo = this.extractArticleLanguageInfo()

          // 限制内容长度，避免token浪费
          const maxChars = 8000
          const trimmedContent =
            content.length > maxChars
              ? content.substring(0, maxChars) + '\n...(内容已截断)'
              : content

          return {
            type: 'article',
            title: title.trim(),
            content: trimmedContent.trim(),
            author: author.trim(),
            url: window.location.href,
            ...languageInfo, // 添加语言信息
          }
        }

        // 其他页面 - 提取主要内容区域
        const mainContent =
          document.querySelector('main')?.innerText ||
          document.querySelector('.content')?.innerText ||
          document.querySelector('article')?.innerText ||
          document.body.innerText

        const maxChars = 5000
        const trimmedContent =
          mainContent?.length > maxChars
            ? mainContent.substring(0, maxChars) + '\n...(内容已截断)'
            : mainContent

        return {
          type: 'page',
          title: document.title,
          content: trimmedContent?.trim() || '',
          url: window.location.href,
        }
      } catch (error) {
        console.error('提取页面内容失败:', error)
        return null
      }
    },

    /**
     * 提取文章的语言信息
     */
    extractArticleLanguageInfo() {
      try {
        const languageInfo = {}

        // 1. 从HTML元素的lang属性获取当前显示语言
        const htmlLang = document.documentElement.getAttribute('lang')
        if (htmlLang) {
          languageInfo.currentLanguage = htmlLang
        }

        // 2. 从语言切换按钮获取可用语言列表和源语言
        // 尝试多种选择器以确保能找到按钮
        let languageButtons = document.querySelectorAll(
          '.article-language-switch button[data-lang]'
        )

        // 如果第一种方式没找到，尝试更宽松的选择器
        if (!languageButtons || languageButtons.length === 0) {
          languageButtons = document.querySelectorAll('button[data-lang]')
        }

        // 如果还是没找到，尝试通过class查找
        if (!languageButtons || languageButtons.length === 0) {
          const allButtons = document.querySelectorAll('.el-button--mini')
          languageButtons = Array.from(allButtons).filter((btn) =>
            btn.hasAttribute('data-lang')
          )
        }

        if (languageButtons && languageButtons.length > 0) {
          const availableLanguages = []
          let sourceLanguage = null
          let currentLanguageButton = null

          languageButtons.forEach((btn, index) => {
            const langCode = btn.getAttribute('data-lang')
            const langName = btn.textContent?.trim()
            const isPrimary = btn.classList.contains('el-button--primary')

            if (langCode && langName) {
              availableLanguages.push({
                code: langCode,
                name: langName,
              })

              // 第一个按钮通常是源语言
              if (!sourceLanguage) {
                sourceLanguage = {
                  code: langCode,
                  name: langName,
                }
              }

              // 标记为primary的按钮是当前语言
              if (isPrimary) {
                currentLanguageButton = {
                  code: langCode,
                  name: langName,
                }
              }
            }
          })

          if (availableLanguages.length > 0) {
            languageInfo.availableLanguages = availableLanguages
            languageInfo.sourceLanguage = sourceLanguage

            // 如果找到了当前语言按钮，覆盖之前的currentLanguage
            if (currentLanguageButton) {
              languageInfo.currentLanguage = currentLanguageButton.code
              languageInfo.currentLanguageName = currentLanguageButton.name
            }
          }
        }

        // 3. 从URL参数获取语言（如果有）
        const urlParams = new URLSearchParams(window.location.search)
        const urlLang = urlParams.get('lang')
        if (urlLang) {
          languageInfo.urlLanguage = urlLang
        }

        return languageInfo
      } catch (error) {
        console.error('提取文章语言信息失败:', error)
        return {}
      }
    },

    /**
     * 附加当前页面
     */
    attachCurrentPage() {
      const pageContext = this.extractCurrentPageContent()
      if (pageContext) {
        this.attachedPageContext = pageContext
        return true
      }
      return false
    },

    /**
     * 移除附加的页面
     */
    removeAttachedPage() {
      this.attachedPageContext = null
    },

    /**
     * 发送普通消息
     */
    async sendNormalMessage(content) {
      // 显示打字指示器
      this.typing = true
      this.shouldStop = false

      // 创建AbortController
      this.abortController = new AbortController()

      try {
        const response = await fetch(
          `${constant.pythonBaseURL}/ai/chat/sendMessage`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              message: content,
              conversationId: 'default',
              history: this.messageHistory,
              user_id: this.currentUser?.id || 'anonymous',
              pageContext: this.attachedPageContext, // 携带页面上下文
            }),
            signal: this.abortController.signal,
          }
        )

        const result = await response.json()

        // 隐藏打字指示器
        this.typing = false

        if (result.flag && result.data?.response) {
          // 添加AI回复
          this.addMessage(result.data.response, 'assistant')

          // 发送成功后清除附加的页面上下文
          if (this.attachedPageContext) {
            this.attachedPageContext = null
          }

          return {
            success: true,
            response: result.data.response,
          }
        } else {
          throw new Error(result.message || '未知错误')
        }
      } catch (error) {
        // 出错时也要隐藏打字指示器
        this.typing = false

        // 如果是用户中断，不显示错误
        if (error.name === 'AbortError') {
          return {
            success: false,
            cancelled: true,
            message: '已停止生成',
          }
        }

        console.error('发送消息失败:', error)
        throw error
      }
    },

    /**
     * 发送流式消息
     */
    async sendStreamingMessage(content) {
      // 先显示打字指示器
      this.typing = true
      this.streaming = true
      this.shouldStop = false

      // 创建AbortController
      this.abortController = new AbortController()

      try {
        const response = await fetch(
          `${constant.pythonBaseURL}/ai/chat/sendMessageStream`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              message: content,
              conversationId: 'default',
              history: this.messageHistory,
              user_id: this.currentUser?.id || 'anonymous',
              pageContext: this.attachedPageContext, // 携带页面上下文
            }),
            signal: this.abortController.signal,
          }
        )

        // 先不隐藏打字提示器，也不创建消息框
        // 等接收到第一个字符后，再隐藏打字提示器并创建消息框

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let fullText = ''
        let buffer = ''
        let aiMessage = null // 延迟创建消息
        let firstChunkReceived = false // 标记是否已收到第一个字符

        while (true) {
          // 检查是否需要停止
          if (this.shouldStop) {
            reader.cancel()
            break
          }

          const { value, done } = await reader.read()
          if (done) break

          // 解码数据
          buffer += decoder.decode(value, { stream: true })

          // 处理 SSE 格式的数据（每条消息以 \n\n 分隔）
          const lines = buffer.split('\n\n')
          buffer = lines.pop() || '' // 保留最后未完成的部分

          for (const line of lines) {
            if (!line.trim() || !line.startsWith('data: ')) continue

            try {
              const jsonStr = line.replace(/^data: /, '')
              const eventData = JSON.parse(jsonStr)

              // 处理不同类型的事件
              if (eventData.event === 'message' && eventData.content) {
                // 普通消息内容
                fullText += eventData.content

                // 收到第一个字符时：隐藏打字提示器并创建消息框
                if (!firstChunkReceived) {
                  this.typing = false
                  aiMessage = this.addMessage('', 'assistant', {
                    streaming: true,
                  })
                  firstChunkReceived = true
                }

                if (aiMessage) {
                  this.updateMessage(aiMessage.id, fullText)
                }
              } else if (eventData.event === 'tool_call') {
                // 工具调用开始
                const toolData = eventData.data || {}

                // 如果还没创建消息，先创建
                if (!aiMessage) {
                  this.typing = false
                  aiMessage = this.addMessage('', 'assistant', {
                    streaming: true,
                  })
                  firstChunkReceived = true
                }

                // 只显示 executing 状态（有参数的那次）
                if (toolData.status === 'executing') {
                  fullText += `\n\n🔧 **正在调用工具**: ${
                    toolData.tool || '未知工具'
                  }\n`
                  if (toolData.arguments) {
                    fullText += `参数: \`${JSON.stringify(
                      toolData.arguments
                    )}\`\n`
                  }
                  this.updateMessage(aiMessage.id, fullText)
                }
              } else if (eventData.event === 'tool_result') {
                // 工具调用结果
                if (aiMessage) {
                  fullText += `\n✅ **工具调用完成**\n\n`
                  this.updateMessage(aiMessage.id, fullText)
                }
              } else if (eventData.event === 'error') {
                // 错误事件
                console.error('流式响应错误:', eventData.message)

                // 如果还没创建消息，先创建
                if (!aiMessage) {
                  this.typing = false
                  aiMessage = this.addMessage('', 'assistant', {
                    streaming: true,
                  })
                  firstChunkReceived = true
                }

                this.updateMessage(
                  aiMessage.id,
                  fullText + '\n\n❌ 错误: ' + eventData.message
                )
                break
              }
              // start 事件可以忽略
            } catch (e) {
              console.error('解析 SSE 数据失败:', e, line)
            }
          }
        }

        this.streaming = false
        // 流结束时确保打字提示器已隐藏（保底逻辑）
        this.typing = false

        // 流式结束后，更新消息的 streaming 状态为 false，触发重新渲染（添加代码行号等）
        if (aiMessage) {
          const message = this.messages.find((m) => m.id === aiMessage.id)
          if (message) {
            message.streaming = false
          }
        }

        // 发送成功后清除附加的页面上下文（避免下次误用）
        if (this.attachedPageContext) {
          this.attachedPageContext = null
        }

        return {
          success: true,
          response: fullText,
        }
      } catch (error) {
        this.typing = false
        this.streaming = false

        // 如果是用户中断，不显示错误
        if (error.name === 'AbortError' || this.shouldStop) {
          return {
            success: false,
            cancelled: true,
            message: '已停止生成',
          }
        }

        console.error('流式消息失败:', error)

        throw error
      }
    },

    /**
     * 检查速率限制
     */
    checkRateLimit() {
      const now = Date.now()
      const limit = this.config?.rate_limit || 20

      // 重置计数器
      if (now > this.rateLimitData.resetTime) {
        this.rateLimitData = {
          count: 0,
          resetTime: now + 60000, // 1分钟后重置
        }
      }

      // 检查是否超限
      if (this.rateLimitData.count >= limit) {
        return false
      }

      // 增加计数
      this.rateLimitData.count++

      // 保存到localStorage
      const userId = this.currentUser?.id || 'anonymous'
      localStorage.setItem(
        `chat_rate_limit_${userId}`,
        JSON.stringify(this.rateLimitData)
      )

      return true
    },

    /**
     * 内容过滤
     */
    filterContent(content) {
      const badWords = ['垃圾', '傻逼', '废物', '妈的', '草泥马']

      for (const word of badWords) {
        if (content.includes(word)) {
          return { pass: false, word }
        }
      }

      return { pass: true }
    },

    /**
     * 保存聊天历史
     */
    saveHistory() {
      try {
        const maxMessages = 100 // 最多保存100条
        const toSave = this.messages.slice(-maxMessages)
        localStorage.setItem('ai_chat_history', JSON.stringify(toSave))
      } catch (error) {
        console.error('保存聊天历史失败:', error)
      }
    },

    /**
     * 恢复聊天历史
     */
    restoreHistory() {
      try {
        const saved = localStorage.getItem('ai_chat_history')
        if (saved) {
          this.messages = JSON.parse(saved)
          // 标记所有历史消息为旧消息，禁用打字机效果
          this.messages.forEach((msg) => {
            msg.isNew = false
          })
        }
      } catch (error) {
        console.error('恢复聊天历史失败:', error)
        this.messages = []
      }
    },

    /**
     * 清空聊天历史
     */
    clearHistory() {
      this.messages = []
      localStorage.removeItem('ai_chat_history')

      // 清空后添加欢迎消息
      this.addWelcomeMessage()
    },

    /**
     * 开始编辑消息
     */
    startEditMessage(messageId, content) {
      this.editingMessageId = messageId
      this.editingContent = content

      // 查找原消息
      const message = this.messages.find((m) => m.id === messageId)
      if (message && message.attachedPage) {
        // 保存原始附加页面信息
        this.editingOriginalAttachedPage = message.attachedPage
        // 恢复附加页面到当前状态
        this.attachedPageContext = {
          title: message.attachedPage.title,
          type: message.attachedPage.type,
          url: message.attachedPage.url,
          content: '', // 内容不需要恢复，因为已经发送过了
          author: message.attachedPage.author || '',
        }
      } else {
        this.editingOriginalAttachedPage = null
      }
    },

    /**
     * 取消编辑
     */
    cancelEdit() {
      this.editingMessageId = null
      this.editingContent = ''

      // 清除恢复的附加页面（如果是从编辑恢复的）
      if (this.editingOriginalAttachedPage) {
        this.attachedPageContext = null
        this.editingOriginalAttachedPage = null
      }
    },

    /**
     * 更新消息内容
     */
    updateMessageContent(messageId, newContent) {
      const message = this.messages.find((m) => m.id === messageId)
      if (message) {
        message.content = newContent
        this.saveHistory()
      }
    },

    /**
     * 保存编辑并重新发送
     */
    async saveEditAndResend() {
      if (!this.editingMessageId) return

      const messageIndex = this.messages.findIndex(
        (m) => m.id === this.editingMessageId
      )
      if (messageIndex === -1) return

      // 更新消息内容
      this.messages[messageIndex].content = this.editingContent

      // 如果有附加页面，更新到消息中
      if (this.attachedPageContext) {
        this.messages[messageIndex].attachedPage = {
          title: this.attachedPageContext.title,
          type: this.attachedPageContext.type,
          url: this.attachedPageContext.url,
          author: this.attachedPageContext.author,
        }
      }

      // 删除该消息之后的所有消息（包括AI回复）
      this.messages = this.messages.slice(0, messageIndex + 1)

      // 保存历史
      this.saveHistory()

      // 保存内容用于发送
      const content = this.editingContent

      // 清空编辑状态
      this.editingMessageId = null
      this.editingContent = ''
      this.editingOriginalAttachedPage = null

      // 检查速率限制
      if (!this.checkRateLimit()) {
        const remainingTime = Math.ceil(
          (this.rateLimitData.resetTime - Date.now()) / 1000
        )
        return {
          success: false,
          error: 'rate_limit',
          message: `发送频率太快了，请等待${remainingTime}秒后再试`,
        }
      }

      // 内容过滤
      if (this.config?.enable_content_filter) {
        const filtered = this.filterContent(content)
        if (!filtered.pass) {
          return {
            success: false,
            error: 'content_filter',
            message: '请文明聊天，避免使用不当词汇',
          }
        }
      }

      // 重新检查登录状态
      this.checkUserLogin()

      // 验证登录
      if (this.requireLogin && !this.currentUser) {
        return {
          success: false,
          error: 'require_login',
          message: '需要登录后才能使用聊天功能',
        }
      }

      // 直接发送到后端（不添加新的用户消息）
      try {
        if (this.isStreamingEnabled) {
          return await this.sendStreamingMessage(content)
        } else {
          return await this.sendNormalMessage(content)
        }
      } catch (error) {
        console.error('重新发送消息失败:', error)
        return {
          success: false,
          error: 'network',
          message: '网络错误，请稍后重试',
        }
      }
    },

    /**
     * 停止AI生成
     */
    stopGeneration() {
      this.shouldStop = true

      // 取消fetch请求
      if (this.abortController) {
        this.abortController.abort()
        this.abortController = null
      }

      // 重置状态
      this.typing = false
      this.streaming = false
    },
  },
})
