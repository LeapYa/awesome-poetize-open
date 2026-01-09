<template>
  <div
    class="message"
    :class="messageClass"
  >
    <!-- 用户消息的行容器（编辑按钮+消息内容） -->
    <div v-if="isUser" class="message-row">
      <!-- 编辑按钮（用户消息左侧，悬停显示） -->
      <button
        class="message-edit-btn"
        @click="handleEdit"
        title="编辑消息"
      >
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M319.184 567.24c-0.665 0.886-1.329 1.8-1.633 2.907l-51.532 195.307c-2.99 11.375 0.083 23.635 8.22 32.27 6.088 6.144 14.114 9.52 22.666 9.52 2.823 0 5.645-0.332 8.413-1.107l187.558-52.888c0.305 0 0.443 0.277 0.664 0.277 2.159 0 4.29-0.803 5.868-2.519l501.538-518.448c14.89-15.416 23.054-36.421 23.054-59.282 0-25.904-10.627-51.78-29.226-70.988l-47.353-49.041c-18.598-19.235-43.672-30.25-68.69-30.25-22.113 0-42.427 8.442-57.372 23.83L319.93 565.441c-0.526 0.498-0.388 1.217-0.747 1.799M951.85 181.774l-49.817 51.477-80.73-84.826L870.4 97.667c7.75-8.08 22.805-6.89 31.716 2.353l47.409 49.041c4.926 5.12 7.749 11.9 7.749 18.626-0.028 5.507-1.91 10.517-5.424 14.087M408.355 575.377l361.887-374.175 80.813 84.881L489.832 659.54l-81.477-84.162zM342.43 727.095l26.154-99.245 69.77 72.123-95.924 27.122z m641.19-339.857c-18.985 0-34.54 15.969-34.622 35.868V906.46c0 25.351-19.899 45.942-44.447 45.942h-790.86c-24.521 0-44.503-20.59-44.503-45.942v-788.95c0-25.379 19.982-45.97 44.503-45.97h509.343c19.096 0 34.594-16.051 34.594-35.784C657.63 16.052 642.131 0 623.035 0h-514.63C48.655 0 0 50.259 0 112.086v799.855C0 973.77 48.654 1024 108.406 1024h801.376c59.808 0 108.406-50.231 108.406-112.059V422.857c-0.055-19.65-15.581-35.619-34.567-35.619" />
        </svg>
      </button>
      
      <div 
        class="message-content"
        :style="{ background: themeColor }"
      >
        <!-- 附加页面标识 -->
        <div v-if="message.attachedPage" class="attached-page-badge">
          <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
            <path d="M750.933333 648.567467V256a153.6 153.6 0 1 0-307.2 0V682.666667a68.266667 68.266667 0 1 0 136.533334 0V341.333333a34.133333 34.133333 0 1 1 68.266666 0v341.333334a136.533333 136.533333 0 0 1-273.066666 0V256a221.866667 221.866667 0 1 1 443.733333 0V682.666667a307.2 307.2 0 1 1-614.4 0V273.066667a34.133333 34.133333 0 1 1 68.266667 0v409.6a238.933333 238.933333 0 1 0 477.866666 0v-34.0992z" fill="currentColor"/>
          </svg>
          <span class="badge-text">{{ message.attachedPage.title }}</span>
        </div>
        
        <div class="message-text" v-text="message.content" />
      </div>
    </div>
    
    <!-- AI消息的内容 -->
    <div 
      v-else
      class="message-content"
      :style="!isUser ? { '--link-color': themeColor } : {}"
    >
      <!-- Markdown渲染 -->
      <MarkdownRenderer
        v-if="isAssistant"
        :content="message.content"
        :streaming="message.streaming || false"
        :enable-typewriter="message.isNew !== false"
        @rendered="handleRendered"
      />
      
      <!-- 系统消息纯文本 -->
      <div
        v-else
        class="message-text"
        v-text="message.content"
      />
    </div>
    
    <div class="message-footer">
      <div class="message-time">
        {{ formattedTime }}
      </div>
      <button 
        v-if="!isUser"
        class="message-copy-btn"
        @click="handleCopy"
        title="复制消息"
      >
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M833.33 767.96h-91.9c-21.73 0-39.34-17.6-39.34-39.34s17.62-39.34 39.34-39.34h91.9c8.82 0 15.98-7.18 15.98-15.98V193.8c0-8.8-7.17-15.98-15.98-15.98H353.84c-8.82 0-15.98 7.18-15.98 15.98v90.86c0 21.75-17.62 39.34-39.34 39.34s-39.34-17.6-39.34-39.34V193.8c0-52.21 42.47-94.67 94.67-94.67h479.49c52.19 0 94.67 42.45 94.67 94.67v479.49c-0.01 52.21-42.49 94.67-94.68 94.67z"></path>
          <path d="M675.96 925.33H196.47c-52.19 0-94.67-42.45-94.67-94.67V351.17c0-52.21 42.47-94.67 94.67-94.67h479.49c52.19 0 94.67 42.45 94.67 94.67v479.49c-0.01 52.22-42.48 94.67-94.67 94.67zM196.47 335.19c-8.82 0-15.98 7.18-15.98 15.98v479.49c0 8.8 7.17 15.98 15.98 15.98h479.49c8.82 0 15.98-7.18 15.98-15.98V351.17c0-8.8-7.17-15.98-15.98-15.98H196.47z"></path>
        </svg>
      </button>
    </div>
  </div>
</template>

<script>
import { computed, ref, onMounted, nextTick } from 'vue'
import { useAIChatStore } from '@/stores/aiChat'
import { useLive2DStore } from '@/stores/live2d'
import MarkdownRenderer from './MarkdownRenderer.vue'

export default {
  name: 'AIChatMessage',
  
  components: {
    MarkdownRenderer
  },
  
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  
  emits: ['rendered'],
  
  setup(props, { emit }) {
    const aiChatStore = useAIChatStore()
    const live2dStore = useLive2DStore()
    
    // 消息类型
    const isUser = computed(() => props.message.role === 'user')
    const isAssistant = computed(() => props.message.role === 'assistant')
    const isSystem = computed(() => props.message.role === 'system')
    
    // 主题颜色
    const themeColor = computed(() => aiChatStore.config?.theme_color || '#4facfe')
    
    /**
     * 复制消息内容
     */
    const handleCopy = async () => {
      try {
        await navigator.clipboard.writeText(props.message.content)
        live2dStore.showMessage('复制成功！', 3000, 9)
      } catch (error) {
        console.error('复制失败:', error)
        // 降级方案：使用传统方法
        try {
          const textarea = document.createElement('textarea')
          textarea.value = props.message.content
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
          live2dStore.showMessage('复制成功！', 3000, 9)
        } catch (err) {
          live2dStore.showMessage('复制失败，请手动复制', 3000, 9)
        }
      }
    }
    
    /**
     * 编辑消息
     */
    const handleEdit = () => {
      // 触发编辑事件，传递消息ID和内容
      aiChatStore.startEditMessage(props.message.id, props.message.content)
      live2dStore.showMessage('编辑消息中...', 2000, 9)
    }
    
    /**
     * Markdown渲染完成
     */
    const handleRendered = () => {
      // 向父组件传递渲染完成事件
      emit('rendered')
    }
    
    // 对于用户消息和系统消息（不经过 MarkdownRenderer），挂载后立即触发 rendered
    onMounted(() => {
      if (!isAssistant.value) {
        // 用户消息或系统消息，纯文本显示
        nextTick(() => {
          emit('rendered')
        })
      }
    })
    
    // 样式类
    const messageClass = computed(() => ({
      'message-user': isUser.value,
      'message-assistant': isAssistant.value,
      'message-system': isSystem.value
    }))
    
    // 格式化时间
    const formattedTime = computed(() => {
      if (!props.message.timestamp) return ''
      
      const date = new Date(props.message.timestamp)
      const now = new Date()
      const diff = now - date
      
      // 1分钟内
      if (diff < 60000) {
        return '刚刚'
      }
      
      // 1小时内
      if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`
      }
      
      // 今天
      if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      }
      
      // 其他
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    })
    
    return {
      isUser,
      isAssistant,
      isSystem,
      messageClass,
      formattedTime,
      themeColor,
      handleCopy,
      handleEdit,
      handleRendered
    }
  }
}
</script>

<style scoped>
.message {
  margin-bottom: 15px;
  animation: slideIn 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* AI消息左对齐 */
.message-assistant {
  align-items: flex-start;
}

/* 用户消息右对齐 */
.message-user {
  align-items: flex-end;
}

/* 用户消息的行容器（编辑按钮+消息内容） */
.message-row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: flex-end;
  max-width: 90%;
}

/* 用户消息的message-content在row中可以自适应 */
.message-row .message-content {
  flex: 1;
  max-width: 100%;
}

/* 系统消息居中 */
.message-system {
  align-items: center;
}

.message-content {
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 85%;
  word-wrap: break-word;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* AI消息样式 */
.message-assistant .message-content {
  background: rgba(255, 255, 255, 0.9);
  color: #333;
  border-radius: 18px 18px 18px 4px;
}

/* 用户消息样式 */
.message-user .message-content {
  color: white;
  border-radius: 18px 18px 4px 18px;
  max-width: none; /* 移除max-width限制，由message-row控制 */
}

/* 系统消息样式 */
.message-system .message-content {
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 18px;
  font-size: 13px;
  padding: 8px 12px;
}

/* 附加页面标识 */
.attached-page-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.attached-page-badge svg {
  width: 12px;
  height: 12px;
  fill: currentColor;
  flex-shrink: 0;
}

.badge-text {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 消息底部区域 */
.message-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding: 0 4px;
}

.message-time {
  font-size: 11px;
  color: #999;
}

/* 复制按钮 */
.message-copy-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  opacity: 1;
  transition: transform 0.1s;
}

.message-copy-btn:hover {
  transform: scale(1.15);
}

.message-copy-btn:active {
  transform: scale(0.95);
}

.message-copy-btn svg {
  width: 14px;
  height: 14px;
  fill: #999;
  transition: fill 0.2s;
}

.message-copy-btn:hover svg {
  fill: #667eea;
}

/* 编辑按钮 */
.message-edit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  margin-right: 8px;
  margin-top: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, transform 0.1s;
  flex-shrink: 0;
}

.message-row:hover .message-edit-btn {
  opacity: 1;
}

.message-edit-btn:hover {
  transform: scale(1.1);
}

.message-edit-btn:active {
  transform: scale(0.95);
}

.message-edit-btn svg {
  width: 16px;
  height: 16px;
  fill: #999;
  transition: fill 0.2s;
}

.message-edit-btn:hover svg {
  fill: #667eea;
}

.message-text {
  white-space: pre-wrap;
}

/* 深色模式 */
.dark-mode .message-assistant .message-content {
  background: rgba(44, 62, 80, 0.9);
  color: #ecf0f1;
}

.dark-mode .message-user .message-content {
  background: #3498db;
}

.dark-mode .attached-page-badge {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.95);
}

.dark-mode .message-system .message-content {
  background: #1e3a5f;
  color: #64b5f6;
}

.dark-mode .message-time {
  color: #8e8ea0;
}

.dark-mode .message-copy-btn svg {
  fill: #a0a0b0;
}

.dark-mode .message-copy-btn:hover svg {
  fill: #a29bfe;
}

.dark-mode .message-edit-btn svg {
  fill: #a0a0b0;
}

.dark-mode .message-edit-btn:hover svg {
  fill: #a29bfe;
}
</style>
