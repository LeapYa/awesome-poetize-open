/**
 * 事件监听 Composable
 * Vue2.7 Composition API
 */
import { onMounted, onUnmounted } from 'vue'
import { useLive2DStore } from '@/stores/live2d'

export function useEvents() {
  const store = useLive2DStore()

  /**
   * 复制事件
   */
  const handleCopy = () => {
    store.showMessage('复制成功！希望这些内容对你有帮助～', 6000, 9)
  }

  /**
   * 页面可见性变化
   */
  const handleVisibilityChange = () => {
    if (!document.hidden) {
      store.showMessage('欢迎回来！我一直在这里等你哦～', 6000, 9)
    }
  }

  /**
   * 欢迎消息
   */
  const showWelcomeMessage = () => {
    const hour = new Date().getHours()
    let text = ''

    if (location.pathname === '/') {
      // 主页问候
      if (hour > 5 && hour <= 7) {
        text = '早上好！一日之计在于晨，美好的一天就要开始了。'
      } else if (hour > 7 && hour <= 11) {
        text = '上午好！工作顺利嘛，不要久坐，多起来走动走动哦！'
      } else if (hour > 11 && hour <= 13) {
        text = '中午了，工作了一个上午，现在是午餐时间！'
      } else if (hour > 13 && hour <= 17) {
        text = '午后很容易犯困呢，今天的运动目标完成了吗？'
      } else if (hour > 17 && hour <= 19) {
        text = '傍晚了！窗外夕阳的景色很美丽呢，最美不过夕阳红～'
      } else if (hour > 19 && hour <= 21) {
        text = '晚上好，今天过得怎么样？'
      } else if (hour > 21 && hour <= 23) {
        text = ['已经这么晚了呀，早点休息吧，晚安～', '深夜时要爱护眼睛呀！']
      } else {
        text = '你是夜猫子呀？这么晚还不睡觉，明天起的来嘛？'
      }
    } else if (document.referrer !== '') {
      // 来自其他页面
      try {
        const referrer = new URL(document.referrer)
        const domain = referrer.hostname.split('.')[1]

        if (location.hostname === referrer.hostname) {
          const title = document.title.split(' - ')[0]
          text = `欢迎阅读<span>「${title}」</span>`
        } else if (domain === 'baidu') {
          const query = referrer.search.split('&wd=')[1]?.split('&')[0]
          text = `Hello！来自 百度搜索 的朋友<br>你是搜索 <span>${decodeURIComponent(
            query || ''
          )}</span> 找到的我吗？`
        } else if (domain === 'so') {
          const query = referrer.search.split('&q=')[1]?.split('&')[0]
          text = `Hello！来自 360搜索 的朋友<br>你是搜索 <span>${decodeURIComponent(
            query || ''
          )}</span> 找到的我吗？`
        } else if (domain === 'google') {
          const title = document.title.split(' - ')[0]
          text = `Hello！来自 谷歌搜索 的朋友<br>欢迎阅读<span>「${title}」</span>`
        } else {
          text = `Hello！来自 <span>${referrer.hostname}</span> 的朋友`
        }
      } catch (e) {
        const title = document.title.split(' - ')[0]
        text = `欢迎阅读<span>「${title}」</span>`
      }
    } else {
      const title = document.title.split(' - ')[0]
      text = `欢迎阅读<span>「${title}」</span>`
    }

    store.showMessage(text, 7000, 8)
  }

  /**
   * 用户空闲检测
   */
  const setupIdleDetection = () => {
    let userAction = false
    let userActionTimer = null

    const defaultMessages = [
      '欢迎来到这个温馨的小站呢～🌟',
      '今天也要保持好心情哦！',
      '今天又学到了什么新知识吗？',
      '不如写篇博客记录一下今天的想法吧～',
      '记得多喝水，保护好眼睛哦！',
      '你的每一次访问都让我很开心呢 ✨',
      '发现了什么有趣的内容吗？',
      '要不要试试和我聊天呢？我很乐意陪你～',
      '这个博客真是个宝藏网站呢！',
      '点击我可以切换不同的造型哦～',
      '静静地陪伴是我最喜欢的事情了 💕',
      '今天心情怎么样？要不要分享给我听？',
      '学而时习之，不亦说乎～',
      '偶尔放松一下也是很重要的哦！'
    ]

    const resetUserAction = () => {
      userAction = true
    }

    window.addEventListener('mousemove', resetUserAction)
    window.addEventListener('keydown', resetUserAction)

    const checkInterval = setInterval(() => {
      if (userAction) {
        userAction = false
        if (userActionTimer) {
          clearInterval(userActionTimer)
          userActionTimer = null
        }
      } else if (!userActionTimer) {
        userActionTimer = setInterval(() => {
          // 获取当前模型的闲置消息配置
          const messageConfig = store.currentModelMessages
          let messages = defaultMessages

          if (messageConfig && messageConfig.idle && messageConfig.idle.length > 0) {
            messages = messageConfig.idle
          }

          const randomMessage = messages[Math.floor(Math.random() * messages.length)]
          store.showMessage(randomMessage, 6000, 9)
        }, 20000)
      }
    }, 1000)

    // 返回清理函数
    return () => {
      window.removeEventListener('mousemove', resetUserAction)
      window.removeEventListener('keydown', resetUserAction)
      clearInterval(checkInterval)
      if (userActionTimer) {
        clearInterval(userActionTimer)
      }
    }
  }

  /**
   * 绑定所有事件
   */
  const bindAllEvents = () => {
    // 复制事件
    window.addEventListener('copy', handleCopy)

    // 可见性变化
    document.addEventListener('visibilitychange', handleVisibilityChange)

    // 显示欢迎消息
    setTimeout(() => {
      showWelcomeMessage()
    }, 1000)

    // 用户空闲检测
    const cleanupIdle = setupIdleDetection()

    return cleanupIdle
  }

  /**
   * 解绑所有事件
   */
  const unbindAllEvents = (cleanupIdle) => {
    window.removeEventListener('copy', handleCopy)
    document.removeEventListener('visibilitychange', handleVisibilityChange)

    if (cleanupIdle) {
      cleanupIdle()
    }
  }

  // 组件挂载时绑定
  let cleanup = null
  onMounted(() => {
    cleanup = bindAllEvents()
  })

  // 组件卸载时解绑
  onUnmounted(() => {
    unbindAllEvents(cleanup)
  })

  return {
    showWelcomeMessage,
  }
}
