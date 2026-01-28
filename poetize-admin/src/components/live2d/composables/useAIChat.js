/**
 * AI聊天逻辑 Composable
 * Vue2.7 Composition API
 * 
 * 注意：使用延迟加载模式，避免静态导入 @/stores/aiChat
 * 这样可以防止 mermaid 等大型依赖在首页被预加载
 */
import { ref, computed, watch, shallowRef } from 'vue'

// 缓存 store 实例（所有 useAIChat 调用共享）
let _storeInstance = null
let _storePromise = null

/**
 * 异步获取 store 实例（单例模式 + 懒加载）
 */
async function getStore() {
  if (_storeInstance) {
    return _storeInstance
  }

  // 避免并发重复加载
  if (!_storePromise) {
    _storePromise = import('@/stores/aiChat').then(({ useAIChatStore }) => {
      _storeInstance = useAIChatStore()
      return _storeInstance
    })
  }

  return _storePromise
}

export function useAIChat() {
  // 使用 shallowRef 存储 store 引用，避免深度响应式追踪
  const storeRef = shallowRef(null)

  // 响应式状态
  const inputText = ref('')
  const sending = ref(false)
  const error = ref(null)

  // 计算属性（通过 storeRef 访问，store 加载前返回默认值）
  const messages = computed(() => storeRef.value?.messages || [])
  const config = computed(() => storeRef.value?.config || null)
  const streaming = computed(() => storeRef.value?.streaming || false)
  const typing = computed(() => storeRef.value?.typing || false)
  const requireLogin = computed(() => storeRef.value?.requireLogin || false)
  const currentUser = computed(() => storeRef.value?.currentUser || null)
  const themeColor = computed(() => storeRef.value?.themeColor || '#4facfe')
  const editingMessageId = computed(() => storeRef.value?.editingMessageId || null)
  const isEditing = computed(() => !!storeRef.value?.editingMessageId)

  // 监听编辑状态，自动填充输入框
  watch(editingMessageId, (newId) => {
    if (newId && storeRef.value) {
      inputText.value = storeRef.value.editingContent
    }
  })

  /**
   * 初始化聊天（懒加载 store 和 Markdown 资源）
   */
  const init = async () => {
    try {
      // 懒加载 store
      const store = await getStore()
      storeRef.value = store

      // 加载配置
      await store.init()

      // 懒加载 Markdown 渲染资源
      const { loadMarkdownResources } = await import('@/utils/resourceLoaders/resourceLoader')
      await loadMarkdownResources()

      return true
    } catch (err) {
      console.error('AI聊天初始化失败:', err)
      error.value = err.message
      return false
    }
  }

  /**
   * 发送消息
   */
  const sendMessage = async (content = null) => {
    const store = storeRef.value
    if (!store) {
      console.error('Store 未初始化')
      return
    }

    // 使用传入的content或inputText
    const messageContent = (content || inputText.value).trim()

    if (!messageContent) {
      return
    }

    sending.value = true
    error.value = null

    try {
      let result

      // 如果是编辑模式，先更新编辑内容
      if (isEditing.value) {
        store.editingContent = messageContent
        result = await store.saveEditAndResend()
      } else {
        result = await store.sendMessage(messageContent)
      }

      if (result.success) {
        // 清空输入框
        inputText.value = ''
      } else if (result.cancelled) {
        // 用户主动取消，不显示错误消息
        inputText.value = ''
      } else {
        // 清空输入框（即使失败也要清空，因为用户消息已经显示了）
        inputText.value = ''

        // 显示错误
        error.value = result.message

        // 添加系统提示消息
        if (result.error === 'require_login') {
          store.addMessage(
            `💡 提示：这个功能需要登录后才能使用哦～ [点击这里登录](/user) 就能体验所有功能啦！✨`,
            'assistant'
          )
        } else if (result.error === 'rate_limit' || result.error === 'content_filter') {
          // 速率限制和内容过滤的错误，用系统消息显示
          store.addMessage(`⚠️ ${result.message}`, 'system')
        } else {
          store.addMessage(`⚠️ ${result.message}`, 'system')
        }
      }
    } catch (err) {
      console.error('发送消息失败:', err)
      error.value = '网络错误，请稍后重试'

      if (store) {
        store.addMessage('抱歉，我现在有点累了，请稍后再试试吧～', 'assistant')
      }
    } finally {
      sending.value = false
    }
  }

  /**
   * 清空聊天记录
   */
  const clearHistory = () => {
    if (!storeRef.value) return false

    if (confirm('确定要清空所有聊天记录吗？此操作不可恢复。')) {
      storeRef.value.clearHistory()
      return true
    }
    return false
  }

  /**
   * 重新加载配置
   */
  const reloadConfig = async () => {
    if (!storeRef.value) return false

    try {
      await storeRef.value.loadConfig()
      return true
    } catch (err) {
      console.error('重新加载配置失败:', err)
      return false
    }
  }

  /**
   * 添加系统消息
   */
  const addSystemMessage = (content) => {
    if (storeRef.value) {
      storeRef.value.addMessage(content, 'system')
    }
  }

  /**
   * 重新加载用户状态
   */
  const reloadUserStatus = () => {
    if (storeRef.value) {
      storeRef.value.checkUserLogin()
    }
  }

  /**
   * 停止AI生成
   */
  const stopGeneration = () => {
    if (storeRef.value) {
      storeRef.value.stopGeneration()
    }
  }

  /**
   * 取消编辑
   */
  const cancelEdit = () => {
    if (storeRef.value) {
      storeRef.value.cancelEdit()
    }
    inputText.value = ''
  }

  return {
    // 状态
    inputText,
    sending,
    error,
    messages,
    config,
    streaming,
    typing,
    requireLogin,
    currentUser,
    themeColor,
    editingMessageId,
    isEditing,

    // 方法
    init,
    sendMessage,
    clearHistory,
    reloadConfig,
    addSystemMessage,
    reloadUserStatus,
    stopGeneration,
    cancelEdit
  }
}
