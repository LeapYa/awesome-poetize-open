import { getArticlePath } from './article-url'

export function setupLanguageSwitchEventDelegation() {
  if (this.languageSwitchHandler) {
    document.removeEventListener('click', this.languageSwitchHandler, true)
    document.removeEventListener('touchend', this.languageSwitchHandler, true)
    document.removeEventListener('mousedown', this.languageSwitchHandler, true)
    document.removeEventListener('touchstart', this.languageSwitchHandler, true)
  }

  this.languageSwitchHandler = (event) => {
    const button = event.target.closest(
      '.article-language-switch .el-button[data-lang]'
    )
    if (button && !button.disabled) {
      event.preventDefault()
      event.stopPropagation()
      event.stopImmediatePropagation()

      const langCode = button.getAttribute('data-lang')
      if (langCode) {
        this.handleLanguageSwitch(langCode)
      }
      return false
    }
  }

  document.addEventListener('click', this.languageSwitchHandler, true)
  document.addEventListener('touchend', this.languageSwitchHandler, true)
  document.addEventListener('mousedown', this.languageSwitchHandler, true)
  document.addEventListener('touchstart', this.languageSwitchHandler, true)

  this.$nextTick(() => {
    const buttons = document.querySelectorAll(
      '.article-language-switch .el-button[data-lang]'
    )
    buttons.forEach((button) => {
      button.addEventListener(
        'click',
        (e) => {
          e.preventDefault()
          e.stopPropagation()
          const langCode = button.getAttribute('data-lang')
          if (langCode) {
            this.handleLanguageSwitch(langCode)
          }
        },
        true
      )
    })
  })
}

export function handleMouseDown(event) {
  event.preventDefault()
  event.stopPropagation()
  const langCode = event.target.closest('[data-lang]')?.getAttribute('data-lang')
  if (langCode) {
    this.handleLanguageSwitch(langCode)
  }
}

export function handleTouchStart(event) {
  event.preventDefault()
  event.stopPropagation()
  const langCode = event.target.closest('[data-lang]')?.getAttribute('data-lang')
  if (langCode) {
    this.handleLanguageSwitch(langCode)
  }
}

export async function handleLanguageSwitch(lang) {
  if (lang === this.currentLang) {
    return
  }

  const isLanguageAvailable = this.availableLanguageButtons.some(
    (btn) => btn.code === lang
  )
  if (!isLanguageAvailable) {
    this.$message.warning('该语言版本暂不可用')
    return
  }

  await this.switchLanguage(lang)
}

export async function switchLanguage(lang) {
  if (lang === this.currentLang) return

  const isLanguageAvailable = this.availableLanguageButtons.some(
    (btn) => btn.code === lang
  )
  if (!isLanguageAvailable) {
    this.$message.warning('该语言版本暂不可用')
    return
  }

  this.currentLang = lang
  this.tocbotRefreshed = false

  const articleLangKey = `article_${this.id}_preferredLanguage`
  if (lang !== this.sourceLanguage) {
    localStorage.setItem(articleLangKey, lang)
  } else {
    localStorage.removeItem(articleLangKey)
  }

  localStorage.removeItem('preferredLanguage')
  this.updateUrlWithLanguage(lang)
  document.documentElement.setAttribute('lang', lang)

  if (lang !== this.sourceLanguage) {
    if (this.translatedContent) {
      await this.renderArticleBody(this.translatedContent)
    } else {
      await this.fetchTranslation()
    }
  } else if (lang === this.sourceLanguage) {
    await this.renderArticleBody(this.article.articleContent)
  }
}

/**
 * 只取译文内容 —— 不渲染、不改语言状态、不弹提示
 *
 * 给「页面初始化时才发现需要译文」的场景用：那种场景拿到内容后必须继续走完后续初始化
 * （主题 / 语言按钮 / SEO 等），不能像 fetchTranslation() 那样自己渲染完就结束。
 *
 * @param {string} [language] - 目标语言，缺省取当前语言
 * @returns {Promise<{status: 'ok'|'not_found'|'error', title: string, content: string}>}
 */
export async function fetchTranslationContent(language) {
  const targetLang = language || this.currentLang
  if (!this.article || !this.article.id || !targetLang) {
    return { status: 'error', title: '', content: '' }
  }

  try {
    const response = await this.$http.get(
      this.$constant.baseURL + '/article/getTranslation',
      {
        id: this.article.id,
        language: targetLang,
      }
    )

    if (
      response.code === 200 &&
      response.data &&
      response.data.status === 'not_found'
    ) {
      return { status: 'not_found', title: '', content: '' }
    }

    if (response.code === 200 && response.data) {
      return {
        status: 'ok',
        title: response.data.title,
        content: response.data.content,
      }
    }

    console.error('获取翻译失败，服务器返回:', response)
    return { status: 'error', title: '', content: '' }
  } catch (error) {
    console.error('Translation error:', error)
    return { status: 'error', title: '', content: '' }
  }
}

export async function fetchTranslation() {
  if (!this.article || !this.article.id) {
    return
  }

  // 译文取不到时统一退回原文视图（原来三处重复，收敛到这里）
  const fallbackToSource = async (level) => {
    this.currentLang = this.sourceLanguage
    const articleLangKey = `article_${this.id}_preferredLanguage`
    localStorage.removeItem(articleLangKey)
    this.updateUrlWithLanguage(this.sourceLanguage)
    await this.renderArticleBody(this.article.articleContent)
    if (level === 'info') {
      this.$message.info('该语言版本不存在，已切换到原文显示')
    } else {
      this.$message.error('翻译加载失败，已切换到原文显示')
    }
  }

  this.isLoading = true
  try {
    const result = await this.fetchTranslationContent(this.currentLang)

    if (result.status === 'ok') {
      this.translatedTitle = result.title
      this.translatedContent = result.content
      await this.renderArticleBody(this.translatedContent)
    } else {
      await fallbackToSource(result.status === 'not_found' ? 'info' : 'error')
    }
  } catch (error) {
    // renderArticleBody 自身抛错也走降级，保持与原实现一致的兜底
    console.error('Translation error:', error)
    await fallbackToSource('error')
  } finally {
    this.isLoading = false
    this.$nextTick(() => {
      this.normalizeTaskListCheckboxes()
    })
  }
}

export function updateUrlWithLanguage(lang) {
  let newPath
  const articleForPath = this.article && this.article.id
    ? this.article
    : { id: this.id, articleSlug: this.articlePathToken }

  if (lang === this.sourceLanguage) {
    newPath = getArticlePath(articleForPath)
  } else {
    newPath = getArticlePath(articleForPath, lang)
  }

  const query = { ...this.$route.query }

  this.$router
    .replace({
      path: newPath,
      query: query,
    })
    .catch((err) => {
      if (err.name !== 'NavigationDuplicated') {
      }
    })
}

export async function initializeLanguageSettings() {
  try {
    await this.getDefaultTargetLanguage()

    const langParam = this.$route.params.lang
    const articleLangKey = `article_${this.id}_preferredLanguage`
    const savedLang = localStorage.getItem(articleLangKey)

    this.currentLang = this.sourceLanguage || 'zh'

    if (langParam && this.languageMap[langParam]) {
      this.currentLang = langParam
    } else if (
      savedLang &&
      this.languageMap[savedLang] &&
      savedLang !== this.sourceLanguage
    ) {
      this.currentLang = savedLang
    } else {
      this.currentLang = this.sourceLanguage
    }

    document.documentElement.setAttribute('lang', this.currentLang)
  } catch (error) {
    console.error('语言设置初始化失败:', error)
    this.currentLang = 'zh'
    this.sourceLanguage = 'zh'
    this.targetLanguage = 'en'
    document.documentElement.setAttribute('lang', this.currentLang)
  }
}

/**
 * 语言方向的初始默认值
 *
 * ⚠️ 这里只是**文章响应到达之前**的占位值。源语言的权威来源是文章接口带回的
 * `defaultSourceLang`（后端 `ArticleController.enrichArticleResponse()` 注入，随文章响应一起到达），
 * 由 `article.vue` 的 `syncSourceLanguageFromArticle()` 校正。
 *
 * 别在这里另接一套配置读取（比如读 bootstrap 的 articleDefaultLanguages）——
 * 那是给同一个事实造第二个来源，会带来「哪个为准」的歧义，而它换来的只是
 * 极少数情况下少发一次请求。源语言是站点级配置（DB sys_ai_config.default_source_lang），
 * 不是逐篇文章的字段。
 */
export async function getDefaultTargetLanguage() {
  this.targetLanguage = 'en'
  this.targetLanguageName = 'English'
  this.sourceLanguage = 'zh'
  this.sourceLanguageName = '中文'
}

export function generateLanguageButtons() {
  this.availableLanguageButtons = []

  this.availableLanguageButtons.push({
    code: this.sourceLanguage,
    name: this.sourceLanguageName,
  })

  if (this.availableLanguages && this.availableLanguages.length > 0) {
    this.availableLanguages.forEach((langCode) => {
      if (langCode !== this.sourceLanguage) {
        const langName = this.languageMap[langCode] || langCode
        this.availableLanguageButtons.push({
          code: langCode,
          name: langName,
        })
      }
    })
  }

  const currentLangAvailable = this.availableLanguageButtons.some(
    (btn) => btn.code === this.currentLang
  )
  if (!currentLangAvailable) {
    this.currentLang = this.sourceLanguage
    const articleLangKey = `article_${this.id}_preferredLanguage`
    localStorage.removeItem(articleLangKey)
    this.updateUrlWithLanguage(this.sourceLanguage)
  }
}
