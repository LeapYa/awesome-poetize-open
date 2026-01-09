<template>
  <div class="danmaku-container" ref="container">
    <div
      v-for="item in renderList"
      :key="item.uniqueId"
      class="danmaku-item"
      :style="getDanmakuStyle(item)"
      @click="handleDanmakuClick(item)"
      @mouseenter="handleDanmakuMouseEnter(item)"
      @mouseleave="handleDanmakuMouseLeave(item)"
    >
      <img
        :src="item.avatarUrl"
        class="danmaku-avatar"
        @error="handleImageError"
        alt="avatar"
      />
      <span class="danmaku-msg">{{ item.msg }}</span>
    </div>
  </div>
</template>

<script>
import { $on, $off, $once, $emit } from '../../utils/gogocodeTransfer'
import { useMainStore } from '@/stores/main'

/**
 * 弹幕组件
 *
 * Props:
 * @param {Array} list - 弹幕数据列表 [{ id, avatar, msg, time }]
 * @param {Boolean} loop - 是否循环播放，默认 true
 * @param {Number} channels - 弹幕轨道数量，默认 5
 * @param {Boolean} pauseOnHover - 鼠标悬停时暂停，默认 false
 * @param {Number} speed - 弹幕速度倍率，默认 1
 * @param {Boolean} preventOverlap - 是否避免弹幕重叠，默认 true
 */
import { getDefaultAvatar } from '@/utils/default-avatar'

export default {
  name: 'Danmaku',
  props: {
    list: {
      type: Array,
      default: () => [],
    },
    loop: {
      type: Boolean,
      default: true,
    },
    channels: {
      type: Number,
      default: 5,
    },
    pauseOnHover: {
      type: Boolean,
      default: false,
    },
    speed: {
      type: Number,
      default: 1,
    },
    preventOverlap: {
      type: Boolean,
      default: true,
    },
    interval: {
      type: Number,
      default: 800, // 默认发送间隔（毫秒）
    },
  },
  data() {
    return {
      renderList: [], // 当前渲染的弹幕列表
      currentIndex: 0, // 当前发送索引
      isPaused: false, // 是否全局暂停（通过 pause() 方法控制）
      animationFrameId: null, // 动画帧ID
      channelStatus: [], // 轨道状态
      containerWidth: 0, // 容器宽度
      containerHeight: 0, // 容器高度
      uniqueCounter: 0, // 唯一ID计数器
      lastSendTime: 0, // 上次发送弹幕的时间
      randomColors: [
        // 随机背景色列表
        'rgba(255, 107, 129)', // 粉红
        'rgba(100, 181, 246)', // 蓝色
        'rgba(129, 199, 132)', // 绿色
        'rgba(255, 167, 38)', // 橙色
        'rgba(171, 71, 188)', // 紫色
        'rgba(38, 198, 218)', // 青色
        'rgba(255, 193, 7)', // 黄色
        'rgba(233, 30, 99)', // 玫红
        'rgba(121, 134, 203)', // 靛蓝
        'rgba(102, 187, 106)', // 浅绿
      ],
    }
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
    // 使用全局默认头像
    defaultAvatar() {
      return getDefaultAvatar()
    },
  },
  watch: {
    list: {
      deep: true,

      handler(newList) {
        if (newList && newList.length > 0) {
          // 列表更新时，重置索引继续播放
          if (this.currentIndex >= newList.length) {
            this.currentIndex = 0
          }
        }
      },

      immediate: true,
    },
  },
  mounted() {
    this.init()
  },
  beforeUnmount() {
    this.stop()
    // 清理 resize 监听
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
    }
  },
  methods: {
    /**
     * 初始化弹幕系统
     */
    init() {
      this.updateContainerSize()
      this.initChannels()
      this.startAnimation()

      // 监听容器大小变化
      this.observeResize()
    },

    /**
     * 更新容器尺寸
     */
    updateContainerSize() {
      if (this.$refs.container) {
        const rect = this.$refs.container.getBoundingClientRect()
        this.containerWidth = rect.width
        this.containerHeight = rect.height
      }
    },

    /**
     * 初始化轨道状态
     */
    initChannels() {
      this.channelStatus = Array(this.channels)
        .fill(null)
        .map(() => ({
          lastDanmaku: null, // 最后一条弹幕
          lastSendTime: 0, // 最后发送时间
        }))
    },

    /**
     * 监听容器大小变化
     */
    observeResize() {
      if (window.ResizeObserver) {
        this.resizeObserver = new ResizeObserver(() => {
          this.updateContainerSize()
        })
        this.resizeObserver.observe(this.$refs.container)
      } else {
        // 降级方案
        window.addEventListener('resize', this.updateContainerSize)
      }
    },

    /**
     * 开始动画循环
     */
    startAnimation() {
      const animate = () => {
        // 只有全局暂停时才停止更新（通过 pause() 方法触发）
        // 单条弹幕的悬停暂停不影响整体动画
        if (!this.isPaused) {
          this.update()
        }
        this.animationFrameId = requestAnimationFrame(animate)
      }
      animate()
    },

    /**
     * 停止动画
     */
    stop() {
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId)
        this.animationFrameId = null
      }
    },

    /**
     * 更新弹幕状态
     */
    update() {
      const now = Date.now()

      // 发送新弹幕
      this.sendDanmaku(now)

      // 清理已完成的弹幕
      this.cleanupDanmaku()
    },

    /**
     * 发送弹幕
     */
    sendDanmaku(now) {
      if (!this.list || this.list.length === 0) return

      // 检查发送间隔，避免同一条弹幕被重复发送
      if (now - this.lastSendTime < this.interval) {
        return
      }

      // 如果已经播放完且不循环，则停止
      if (this.currentIndex >= this.list.length && !this.loop) {
        return
      }

      const danmaku = this.list[this.currentIndex]

      // 检查这条弹幕是否已经在屏幕上显示了（避免同一条弹幕重复出现）
      const isAlreadyShowing = this.renderList.some(
        (item) => item.id === danmaku.id
      )
      if (isAlreadyShowing) {
        // 如果这条弹幕已经在显示，跳过它，移动到下一条
        this.currentIndex++
        if (this.currentIndex >= this.list.length) {
          if (this.loop) {
            this.currentIndex = 0
          }
        }
        return
      }

      // 找到可用的轨道
      const availableChannel = this.findAvailableChannel(now)

      if (availableChannel !== -1) {
        // 创建弹幕实例
        const danmakuItem = {
          ...danmaku,
          uniqueId: `danmaku-${this.uniqueCounter++}-${Date.now()}`,
          channel: availableChannel,
          left: this.containerWidth,
          startTime: now,
          duration: ((danmaku.time || 10) * 1000) / this.speed, // 转换为毫秒
          backgroundColor: this.getRandomColor(), // 随机背景色
          avatarUrl: this.getAvatarUrl(danmaku), // 预处理头像URL
        }

        this.renderList.push(danmakuItem)

        // 更新轨道状态
        this.channelStatus[availableChannel].lastDanmaku = danmakuItem
        this.channelStatus[availableChannel].lastSendTime = now

        // 更新上次发送时间
        this.lastSendTime = now

        // 移动到下一条
        this.currentIndex++

        // 循环播放
        if (this.currentIndex >= this.list.length) {
          if (this.loop) {
            this.currentIndex = 0
          }
        }
      }
    },

    /**
     * 查找可用的轨道（随机选择）
     */
    findAvailableChannel(now) {
      if (!this.preventOverlap) {
        // 不检测重叠，随机选择轨道
        return Math.floor(Math.random() * this.channels)
      }

      // 收集所有可用的轨道
      const availableChannels = []

      for (let i = 0; i < this.channels; i++) {
        const channel = this.channelStatus[i]

        // 如果轨道为空，可用
        if (!channel.lastDanmaku) {
          availableChannels.push(i)
          continue
        }

        // 计算上一条弹幕的当前位置和宽度
        const lastDanmaku = channel.lastDanmaku
        const elapsed = now - lastDanmaku.startTime
        const progress = elapsed / lastDanmaku.duration

        // 估算弹幕宽度（头像30px + 间距8px + 文字长度）
        const estimatedWidth = 30 + 8 + (lastDanmaku.msg?.length || 10) * 14
        const currentLeft =
          this.containerWidth -
          (this.containerWidth + estimatedWidth) * progress

        // 如果上一条弹幕已经移出足够距离，该轨道可用
        // 安全间距：确保新弹幕出现时，旧弹幕已经移出至少 200px
        if (currentLeft < this.containerWidth - 200) {
          availableChannels.push(i)
        }
      }

      // 如果有可用轨道，随机选择一个
      if (availableChannels.length > 0) {
        const randomIndex = Math.floor(Math.random() * availableChannels.length)
        return availableChannels[randomIndex]
      }

      // 所有轨道都繁忙，返回-1
      return -1
    },

    /**
     * 清理已完成的弹幕
     */
    cleanupDanmaku() {
      const now = Date.now()

      this.renderList = this.renderList.filter((item) => {
        const elapsed = now - item.startTime
        const isFinished = elapsed >= item.duration

        // 如果弹幕结束，清理轨道状态
        if (isFinished) {
          const channel = this.channelStatus[item.channel]
          if (channel.lastDanmaku === item) {
            channel.lastDanmaku = null
          }
        }

        return !isFinished
      })
    },

    /**
     * 获取随机颜色
     */
    getRandomColor() {
      const index = Math.floor(Math.random() * this.randomColors.length)
      return this.randomColors[index]
    },

    /**
     * 获取随机头像列表（从 Vuex store 读取后端配置）
     */
    getRandomAvatarList() {
      try {
        const webInfo = this.mainStore.webInfo
        if (webInfo && webInfo.randomAvatar) {
          // randomAvatar 可能是字符串或数组
          if (Array.isArray(webInfo.randomAvatar)) {
            return webInfo.randomAvatar
          } else if (typeof webInfo.randomAvatar === 'string') {
            // 尝试解析 JSON 字符串
            try {
              const parsed = JSON.parse(webInfo.randomAvatar)
              if (Array.isArray(parsed) && parsed.length > 0) {
                return parsed
              }
            } catch (e) {
              // 解析失败，返回空数组
            }
          }
        }
      } catch (e) {
        console.error('获取随机头像列表失败:', e)
      }
      return []
    },

    /**
     * 获取随机头像
     * 优先级：后端配置的随机头像 > 默认灰色头像
     */
    getRandomAvatar() {
      const randomAvatars = this.getRandomAvatarList()
      if (randomAvatars && randomAvatars.length > 0) {
        const index = Math.floor(Math.random() * randomAvatars.length)
        return randomAvatars[index]
      }
      return this.defaultAvatar
    },

    /**
     * 获取头像URL
     * 优先级：用户头像 > 随机头像 > 默认灰色头像
     */
    getAvatarUrl(item) {
      // 1. 如果有用户头像且不为空，使用用户头像
      if (
        item.avatar &&
        item.avatar.trim() &&
        item.avatar !== this.defaultAvatar
      ) {
        return item.avatar
      }
      // 2. 否则使用随机头像
      return this.getRandomAvatar()
    },

    /**
     * 获取弹幕样式
     */
    getDanmakuStyle(item) {
      let elapsed

      // 如果这条弹幕被悬停暂停，使用暂停时的位置
      if (item.isPaused && item.pausedElapsed !== undefined) {
        elapsed = item.pausedElapsed
      } else {
        elapsed = Date.now() - item.startTime
      }

      const progress = Math.min(elapsed / item.duration, 1)

      // 估算弹幕宽度（头像24px + 间距8px + padding10px*2 + 文字长度）
      const estimatedWidth = 24 + 8 + 20 + (item.msg?.length || 10) * 14

      // 计算位置
      const left =
        this.containerWidth - (this.containerWidth + estimatedWidth) * progress
      const channelHeight = this.containerHeight / this.channels
      const top = item.channel * channelHeight + channelHeight / 2 - 20 // 20px为弹幕高度的一半

      return {
        transform: `translate(${left}px, ${top}px)`,
        transition: 'none',
        backgroundColor: item.backgroundColor || 'transparent',
        zIndex: item.isPaused ? 100 : 1, // 悬停的弹幕层级提高（但低于对话框）
      }
    },

    /**
     * 处理图片加载错误
     */
    handleImageError(e) {},

    /**
     * 鼠标悬停到某条弹幕
     */
    handleDanmakuMouseEnter(item) {
      if (this.pauseOnHover) {
        // 标记这条弹幕为暂停状态
        item['isPaused'] = true
        // 记录暂停时的已运行时间
        item['pausedElapsed'] = Date.now() - item.startTime
        item['pauseStartTime'] = Date.now()
      }
    },

    /**
     * 鼠标离开某条弹幕
     */
    handleDanmakuMouseLeave(item) {
      if (this.pauseOnHover && item.isPaused) {
        // 计算暂停时长
        const pauseDuration = Date.now() - item.pauseStartTime
        // 调整弹幕的开始时间，补偿暂停的时间
        item.startTime += pauseDuration
        // 清除暂停状态
        item['isPaused'] = false
        item['pausedElapsed'] = undefined
        item['pauseStartTime'] = undefined
      }
    },

    /**
     * 点击弹幕
     */
    handleDanmakuClick(item) {
      $emit(this, 'danmaku-click', item)
    },

    /**
     * 暂停弹幕
     */
    pause() {
      this.isPaused = true
    },

    /**
     * 继续播放
     */
    resume() {
      this.isPaused = false
    },

    /**
     * 清空弹幕
     */
    clear() {
      this.renderList = []
      this.currentIndex = 0
      this.initChannels()
    },

    /**
     * 添加弹幕到列表末尾
     */
    add(danmaku) {
      if (!this.list) {
        this.list = []
      }
      this.list.push(danmaku)
    },
  },
  emits: ['danmaku-click'],
}
</script>

<style scoped>
.danmaku-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}
.danmaku-item {
  position: absolute;
  left: 0;
  top: 0;
  height: 35px;
  padding: 5px 10px;
  border-radius: 25px;
  margin: 5px;
  border: 1px solid #fff;
  display: flex;
  align-items: center;
  white-space: nowrap;
  pointer-events: auto;
  cursor: pointer;
  color: var(--white);
  will-change: transform;
  transform: translateZ(0);
  backface-visibility: hidden;
  transition: background-color 0.3s ease, border-color 0.3s ease;
}
.danmaku-item:hover {
  background-color: rgba(255, 255, 255, 0.25) !important;
  border-color: rgba(255, 255, 255, 0.8);
}
.danmaku-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
  object-fit: cover;
  flex-shrink: 0;
  user-select: none;
  -webkit-user-drag: none;
}
.danmaku-msg {
  font-size: 14px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8), -1px -1px 2px rgba(0, 0, 0, 0.8),
    1px -1px 2px rgba(0, 0, 0, 0.8), -1px 1px 2px rgba(0, 0, 0, 0.8);
  user-select: none;
}
@media screen and (max-width: 768px) {
  .danmaku-avatar {
    width: 20px;
    height: 20px;
    margin-right: 6px;
  }
  .danmaku-msg {
    font-size: 12px;
  }
}
</style>
