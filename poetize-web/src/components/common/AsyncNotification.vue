<template>
  <div class="async-notification-container">
    <!-- 通知列表 -->
    <transition-group name="notification" tag="div" class="notification-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="['notification-item', notification.type]"
        @click="removeNotification(notification.id)"
      >
        <div class="notification-content">
          <div class="notification-icon">
            <el-icon v-if="notification.type === 'loading'" class="loading-icon"><el-icon-loading /></el-icon>
            <el-icon v-else-if="notification.type === 'success'"><el-icon-success /></el-icon>
            <el-icon v-else-if="notification.type === 'error'"><el-icon-error /></el-icon>
            <el-icon v-else><el-icon-info /></el-icon>
          </div>
          <div class="notification-text">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-message">{{ notification.message }}</div>
          </div>
          <div class="notification-close">
            <el-icon><el-icon-close /></el-icon>
          </div>
        </div>
        <!-- 进度条 -->
        <div
          v-if="notification.type === 'loading'"
          class="notification-progress"
        >
          <div
            class="progress-bar"
            :style="{ width: notification.progress + '%' }"
          ></div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script>
import {
  Loading as ElIconLoading,
  SuccessFilled as ElIconSuccess,
  CircleCloseFilled as ElIconError,
  InfoFilled as ElIconInfo,
  Close as ElIconClose,
} from '@element-plus/icons-vue'
export default {
  components: {
    ElIconLoading,
    ElIconSuccess,
    ElIconError,
    ElIconInfo,
    ElIconClose,
  },
  name: 'AsyncNotification',
  data() {
    return {
      notifications: [],
      pollTimers: {}, // 存储每个任务的轮询定时器
    }
  },
  mounted() {
    // 确保组件能访问到全局常量
    if (!this.$constant && this.$parent && this.$parent.$constant) {
      this.$constant = this.$parent.$constant
    }
  },
  methods: {
    /**
     * 添加通知
     * @param {Object} notification - 通知对象
     * @param {string} notification.title - 标题
     * @param {string} notification.message - 消息
     * @param {string} notification.type - 类型：loading, success, error, info
     * @param {number} notification.duration - 持续时间（毫秒），0表示不自动消失
     * @param {string} notification.taskId - 任务ID（用于更新状态）
     */
    addNotification(notification) {
      const id = Date.now() + Math.random()
      const newNotification = {
        id,
        title: notification.title || '通知',
        message: notification.message || '',
        type: notification.type || 'info',
        duration:
          notification.duration !== undefined ? notification.duration : 3000,
        taskId: notification.taskId,
        progress: 0,
        createTime: Date.now(),
      }

      this.notifications.unshift(newNotification)

      // 自动移除（除非duration为0）
      if (newNotification.duration > 0) {
        setTimeout(() => {
          this.removeNotification(id)
        }, newNotification.duration)
      }

      return id
    },

    /**
     * 更新通知
     */
    updateNotification(id, updates) {
      const notification = this.notifications.find((n) => n.id === id)
      if (notification) {
        Object.assign(notification, updates)

        // 如果状态变为成功或失败，设置自动移除
        if (
          (updates.type === 'success' || updates.type === 'error') &&
          notification.duration === 0
        ) {
          notification.duration = updates.type === 'success' ? 2000 : 5000
          setTimeout(() => {
            this.removeNotification(id)
          }, notification.duration)
        }
      }
    },

    /**
     * 根据任务ID更新通知
     */
    updateNotificationByTaskId(taskId, updates) {
      const notification = this.notifications.find((n) => n.taskId === taskId)
      if (notification) {
        this.updateNotification(notification.id, updates)
      }
    },

    /**
     * 移除通知
     */
    removeNotification(id) {
      const index = this.notifications.findIndex((n) => n.id === id)
      if (index > -1) {
        this.notifications.splice(index, 1)
      }
    },

    /**
     * 清空所有通知
     */
    clearAllNotifications() {
      this.notifications = []

      // 清理所有轮询定时器
      Object.values(this.pollTimers).forEach((timer) => {
        if (timer) clearInterval(timer)
      })
      this.pollTimers = {}
    },

    /**
     * 开始轮询任务状态
     */
    startPolling(taskId) {
      if (!taskId) {
        console.error('startPolling: taskId为空')
        return
      }

      // 只在开始轮询时输出一次日志

      // 清理之前的定时器（如果存在）
      if (this.pollTimers[taskId]) {
        clearInterval(this.pollTimers[taskId])
      }

      // 立即执行一次检查
      this.checkTaskStatus(taskId)

      // 创建新的轮询定时器
      this.pollTimers[taskId] = setInterval(() => {
        this.checkTaskStatus(taskId)
      }, 2000)
    },

    /**
     * 停止轮询任务状态
     */
    stopPolling(taskId) {
      if (this.pollTimers[taskId]) {
        clearInterval(this.pollTimers[taskId])
        delete this.pollTimers[taskId]
      }
    },

    /**
     * 检查任务状态
     */
    checkTaskStatus(taskId) {
      // 获取baseURL（通过多种方式尝试）
      let baseURL = ''
      if (this.$constant && this.$constant.baseURL) {
        baseURL = this.$constant.baseURL
      } else if (window.VueAppConfig && window.VueAppConfig.baseURL) {
        baseURL = window.VueAppConfig.baseURL
      } else {
        baseURL = window.location.protocol + '//' + window.location.host
      }

      const url = `${baseURL}/article/getArticleSaveStatus`

      // Cookie-based auth: no need to inject Authorization header
      const headers = {
        'Content-Type': 'application/json',
      }

      fetch(url + '?taskId=' + encodeURIComponent(taskId), {
        method: 'GET',
        headers: headers,
        credentials: 'include',
      })
        .then((response) => response.json())
        .then((res) => {
          if (res.code === 200 && res.data) {
            const status = res.data

            // 更新通知状态
            this.updateNotificationByTaskId(taskId, {
              message: status.message,
            })

            // 如果完成（成功或失败），停止轮询
            if (status.status === 'success') {
              this.stopPolling(taskId)

              this.updateNotificationByTaskId(taskId, {
                type: 'success',
                title: '保存成功',
                message: '文章保存成功！',
              })
            } else if (status.status === 'failed') {
              console.error('任务失败：', status.message)
              this.stopPolling(taskId)

              this.updateNotificationByTaskId(taskId, {
                type: 'error',
                title: '保存失败',
                message: status.message || '文章保存失败',
              })
            } else if (status.status === 'processing') {
              // 进行中的状态不输出日志，减少噪音
            } else {
            }
          } else {
            // 任务不存在或已过期
            this.stopPolling(taskId)

            this.updateNotificationByTaskId(taskId, {
              type: 'error',
              title: '状态查询失败',
              message: '无法查询保存状态，任务可能已完成或过期',
            })
          }
        })
        .catch((error) => {
          console.error('轮询请求失败:', error)
          // 网络错误时不再输出额外日志，避免刷屏
        })
    },
  },
}
</script>

<style scoped>
.async-notification-container {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 3000;
  max-width: 350px;
  pointer-events: none;
}
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.notification-item {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  cursor: pointer;
  pointer-events: auto;
  transition: transform 0.3s ease, opacity 0.3s ease, box-shadow 0.3s ease;
  max-width: 350px;
  border-left: 4px solid;
  transform: translateZ(0);
}
.notification-item:hover {
  transform: translateX(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}
.notification-item.loading {
  border-left-color: #409eff;
}
.notification-item.success {
  border-left-color: #67c23a;
}
.notification-item.error {
  border-left-color: #f56c6c;
}
.notification-item.info {
  border-left-color: #909399;
}
.notification-content {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  gap: 12px;
}
.notification-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}
.notification-icon i {
  font-size: 18px;
}
.loading .notification-icon i {
  color: #409eff;
  animation: rotate 1s linear infinite;
}
.success .notification-icon i {
  color: #67c23a;
}
.error .notification-icon i {
  color: #f56c6c;
}
.info .notification-icon i {
  color: #909399;
}
.notification-text {
  flex: 1;
  min-width: 0;
}
.notification-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1.4;
}
.notification-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  word-break: break-word;
}
.notification-close {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
}
.notification-close:hover {
  background-color: #f5f7fa;
}
.notification-close i {
  font-size: 12px;
  color: #909399;
}
.notification-progress {
  height: 3px;
  background-color: #f5f7fa;
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  transition: width 0.3s ease;
  animation: shimmer 2s ease-in-out infinite;
}
@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: 200px 0;
  }
}
.notification-enter-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
  will-change: transform, opacity;
  transform: translateZ(0);
}
.notification-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
  will-change: transform, opacity;
  transform: translateZ(0);
}
.notification-enter {
  transform: translateX(100%);
  opacity: 0;
}
.notification-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
.notification-move {
  transition: transform 0.3s ease;
}
body.dark-mode .notification-item {
  background: #2d2d2d !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
body.dark-mode .notification-item:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}
body.dark-mode .notification-title {
  color: #e0e0e0 !important;
}
body.dark-mode .notification-message {
  color: #b0b0b0 !important;
}
body.dark-mode .notification-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
}
body.dark-mode .notification-close i {
  color: #b0b0b0;
}
body.dark-mode .notification-progress {
  background-color: rgba(255, 255, 255, 0.1);
}
@media screen and (max-width: 768px) {
  .async-notification-container {
    top: 60px;
    right: 10px;
    left: 10px;
    max-width: none;
  }
  .notification-item {
    max-width: none;
  }
  .notification-content {
    padding: 12px;
  }
  .notification-title {
    font-size: 13px;
  }
  .notification-message {
    font-size: 12px;
  }
}
</style>
