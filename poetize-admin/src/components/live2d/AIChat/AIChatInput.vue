<template>
  <div class="chat-input-wrapper">
    <!-- 编辑模式提示条 -->
    <div v-if="isEditing" class="edit-mode-bar">
      <span class="edit-mode-text">✂️ 编辑消息中...</span>
      <button class="cancel-edit-btn" @click="handleCancelEdit">取消</button>
    </div>
    
    <!-- 已附加页面提示条 -->
    <div v-if="attachedPage" class="attached-page-bar">
      <div class="attached-page-info">
        <svg class="page-icon" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M854.6 288.6L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.7-9.4-22.7zM790.2 326H602V137.8L790.2 326z m1.8 562H232V136h302v216c0 23.2 18.8 42 42 42h216v494z" fill="currentColor"/>
        </svg>
        <span class="page-title">{{ attachedPage.title }}</span>
      </div>
      <button class="remove-page-btn" @click="handleRemovePage" title="移除附加页面">
        ×
      </button>
    </div>
    
    <!-- 附加页面按钮区域 -->
    <div v-if="!attachedPage" class="attach-page-container">
      <button
        class="attach-page-btn"
        :disabled="sending"
        @click="handleAttachPage"
        title="附加当前页面内容"
      >
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M750.933333 648.567467V256a153.6 153.6 0 1 0-307.2 0V682.666667a68.266667 68.266667 0 1 0 136.533334 0V341.333333a34.133333 34.133333 0 1 1 68.266666 0v341.333334a136.533333 136.533333 0 0 1-273.066666 0V256a221.866667 221.866667 0 1 1 443.733333 0V682.666667a307.2 307.2 0 1 1-614.4 0V273.066667a34.133333 34.133333 0 1 1 68.266667 0v409.6a238.933333 238.933333 0 1 0 477.866666 0v-34.0992z" fill="currentColor"/>
        </svg>
        <span class="attach-text">附加</span>
      </button>
    </div>
    
    <div class="chat-input-container">
      <textarea
        ref="inputRef"
        v-model="localValue"
        class="chat-input"
        :placeholder="placeholder"
        :disabled="sending"
        rows="1"
        @keydown.enter.exact="handleKeyDown"
        @input="handleInput"
      />
    
    <!-- 发送按钮（正常状态） -->
    <button
      v-if="!streaming"
      class="send-btn"
      :disabled="!canSend"
      @click="handleSend"
    >
      发送
    </button>
    
    <!-- 停止按钮（AI生成时） -->
    <button
      v-else
      class="stop-btn"
      @click="handleStop"
    >
      <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
        <path d="M722.9375 933.875H301.0625a210.9375 210.9375 0 0 1-210.9375-210.9375V301.0625a210.9375 210.9375 0 0 1 210.9375-210.9375h421.875a210.9375 210.9375 0 0 1 210.9375 210.9375v421.875a210.9375 210.9375 0 0 1-210.9375 210.9375z" />
      </svg>
      <span>停止</span>
    </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick } from 'vue'
import { useAIChatStore } from '@/stores/aiChat'

export default {
  name: 'AIChatInput',
  
  props: {
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '输入消息...'
    },
    sending: {
      type: Boolean,
      default: false
    },
    streaming: {
      type: Boolean,
      default: false
    },
    isEditing: {
      type: Boolean,
      default: false
    }
  },
  
  setup(props, { emit }) {
    const aiChatStore = useAIChatStore()
    const inputRef = ref(null)
    const localValue = ref(props.value)
    
    // 检测是否为移动端
    const isMobile = computed(() => window.innerWidth <= 768)
    
    // 已附加的页面
    const attachedPage = computed(() => aiChatStore.attachedPageContext)
    
    // 是否可发送
    const canSend = computed(() => {
      return localValue.value.trim().length > 0 && !props.sending
    })
    
    /**
     * 输入处理
     */
    const handleInput = () => {
      emit('input', localValue.value)
      adjustTextareaHeight()
    }
    
    /**
     * 处理键盘事件
     */
    const handleKeyDown = (e) => {
      // 移动端不处理回车发送，允许换行
      if (isMobile.value) {
        return
      }
      
      // PC端：回车发送
      e.preventDefault()
      handleSend()
    }
    
    /**
     * 发送消息
     */
    const handleSend = () => {
      if (canSend.value) {
        emit('send')
      } else {
      }
    }
    
    /**
     * 停止生成
     */
    const handleStop = () => {
      emit('stop')
    }
    
    /**
     * 取消编辑
     */
    const handleCancelEdit = () => {
      emit('cancel-edit')
    }
    
    /**
     * 附加当前页面
     */
    const handleAttachPage = () => {
      const success = aiChatStore.attachCurrentPage()
      if (success) {
        // 可以发射事件通知父组件显示提示
        emit('page-attached', aiChatStore.attachedPageContext)
      } else {
      }
    }
    
    /**
     * 移除附加的页面
     */
    const handleRemovePage = () => {
      aiChatStore.removeAttachedPage()
      emit('page-removed')
    }
    
    /**
     * 自动调整输入框高度
     */
    const adjustTextareaHeight = () => {
      nextTick(() => {
        const textarea = inputRef.value
        if (!textarea) return
        
        // 重置高度
        textarea.style.height = 'auto'
        
        // 计算新高度（最多5行）
        const maxHeight = 120 // 约5行
        const newHeight = Math.min(textarea.scrollHeight, maxHeight)
        
        textarea.style.height = `${newHeight}px`
      })
    }
    
    // 监听外部值变化
    watch(() => props.value, (newVal) => {
      localValue.value = newVal
      adjustTextareaHeight()
    })
    
    return {
      inputRef,
      localValue,
      isMobile,
      canSend,
      attachedPage,
      handleInput,
      handleKeyDown,
      handleSend,
      handleStop,
      handleCancelEdit,
      handleAttachPage,
      handleRemovePage
    }
  }
}
</script>

<style scoped>
.chat-input-wrapper {
  display: flex;
  flex-direction: column;
}

/* 编辑模式提示条 */
.edit-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  background: linear-gradient(90deg, #fff3cd 0%, #ffe8a1 100%);
  border-top: 1px solid rgba(255, 193, 7, 0.3);
  font-size: 13px;
  color: #856404;
}

.edit-mode-text {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cancel-edit-btn {
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(108, 117, 125, 0.3);
  border-radius: 4px;
  color: #6c757d;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-edit-btn:hover {
  background: white;
  border-color: #6c757d;
  color: #495057;
}

/* 已附加页面提示条 */
.attached-page-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  background: linear-gradient(90deg, #d1ecf1 0%, #bee5eb 100%);
  border-top: 1px solid rgba(23, 162, 184, 0.3);
  font-size: 13px;
  color: #0c5460;
}

.attached-page-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.page-icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
  flex-shrink: 0;
}

.page-title {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-page-btn {
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(23, 162, 184, 0.3);
  border-radius: 4px;
  color: #0c5460;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.remove-page-btn:hover {
  background: white;
  border-color: #17a2b8;
  color: #0056b3;
}

/* 附加按钮容器 */
.attach-page-container {
  padding: 10px 20px 5px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.chat-input-container {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 20px 15px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

/* 附加页面按钮 */
.attach-page-btn {
  flex-shrink: 0;
  padding: 0 12px;
  height: 32px;
  border: 1px solid rgba(79, 172, 254, 0.4);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  color: #4facfe;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.attach-page-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.9);
  border-color: #4facfe;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.3);
}

.attach-page-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.attach-page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.attach-page-btn svg {
  width: 14px;
  height: 14px;
  fill: currentColor;
  flex-shrink: 0;
}

.attach-text {
  color: currentColor;
}

.chat-input {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  padding: 10px 15px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
  overflow-y: hidden;
  background: rgba(255, 255, 255, 0.6);
  color: #2c3e50;
}

.chat-input::-webkit-scrollbar {
  display: none;
}

.chat-input:focus {
  border-color: rgba(79, 172, 254, 0.6);
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 0 0 3px rgba(79, 172, 254, 0.1);
}

.chat-input:disabled {
  background: rgba(245, 245, 245, 0.6);
  cursor: not-allowed;
}

.chat-input::placeholder {
  color: #999;
}

.send-btn {
  flex-shrink: 0;
  padding: 0 20px;
  height: 40px;
  border: none;
  border-radius: 20px;
  background: #4facfe;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.3);
}

.send-btn:hover:not(:disabled) {
  background: #3498db;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.4);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 停止按钮 */
.stop-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 18px;
  height: 40px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 59, 48, 0.1);
  border: 1px solid rgba(255, 59, 48, 0.3);
  color: #ff3b30;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.stop-btn:hover {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.5);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3);
}

.stop-btn:active {
  transform: scale(0.95);
}

.stop-btn svg {
  width: 14px;
  height: 14px;
  fill: currentColor;
}

/* 深色模式 */
.dark-mode .attach-page-container {
  background: rgba(0, 0, 0, 0.2);
  border-top-color: rgba(255, 255, 255, 0.1);
}

.dark-mode .chat-input-container {
  background: rgba(0, 0, 0, 0.2);
  border-top-color: rgba(255, 255, 255, 0.1);
}

.dark-mode .chat-input {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.2);
  color: #e0e0e0;
}

.dark-mode .chat-input:focus {
  border-color: #764ba2;
  background: rgba(0, 0, 0, 0.4);
}

.dark-mode .chat-input:disabled {
  background: #252525;
}

.dark-mode .chat-input::placeholder {
  color: #888;
}

.dark-mode .edit-mode-bar {
  background: linear-gradient(90deg, #856404 0%, #aa7a04 100%);
  border-top-color: rgba(255, 193, 7, 0.2);
  color: #ffd54f;
}

.dark-mode .cancel-edit-btn {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.2);
  color: #e0e0e0;
}

.dark-mode .cancel-edit-btn:hover {
  background: rgba(0, 0, 0, 0.5);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.dark-mode .attached-page-bar {
  background: linear-gradient(90deg, #17a2b8 0%, #138496 100%);
  border-top-color: rgba(23, 162, 184, 0.2);
  color: #d1ecf1;
}

.dark-mode .remove-page-btn {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.2);
  color: #d1ecf1;
}

.dark-mode .remove-page-btn:hover {
  background: rgba(0, 0, 0, 0.5);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.dark-mode .attach-page-btn {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(118, 75, 162, 0.4);
  color: #a78bfa;
}

.dark-mode .attach-page-btn:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.5);
  border-color: #a78bfa;
}
</style>
