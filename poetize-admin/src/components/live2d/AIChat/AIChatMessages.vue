<template>
  <!-- 外层容器：作为浮动按钮的定位基准。
       按钮不能放进 .chat-messages 内部 —— 那是滚动容器，absolute 的定位基准是
       内容盒而非视口，按钮会被定位到内容最底部（滚到底才看得见），
       起不到「回到底部」的作用。 -->
  <div class="chat-messages-wrap">
    <div ref="messagesContainer" class="chat-messages" @scroll="handleScroll">
      <AIChatMessage
        v-for="message in messages"
        :key="message.id"
        :message="message"
        @rendered="scrollToBottom"
      />

      <!-- 打字指示器：等待首 token 时只显示三点动画。
           工具参数撰写进度已改由 AI 气泡内的工具胶囊展示（见 AIChatMessage.vue），
           这里不再承载任何文案。 -->
      <div v-if="typing" class="typing-indicator">
        <div class="typing-dots">
          <div class="typing-dot"></div>
          <div class="typing-dot"></div>
          <div class="typing-dot"></div>
        </div>
      </div>
    </div>

    <!-- 回到底部：用户向上翻阅历史时浮出，点击恢复自动跟随。
         高速流式下内容持续增长，用户很难靠手动滚动落进底部阈值，
         这里提供一个确定性的恢复入口。 -->
    <button
      v-show="!stickToBottom"
      class="scroll-bottom-btn"
      :class="{ 'btn-meteor': streaming }"
      type="button"
      title="回到底部"
      aria-label="回到底部"
      :style="{ color: themeColor }"
      @click="handleScrollToBottomClick"
    >
      <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <path d="M12 5v13M6 12l6 6 6-6" />
      </svg>
    </button>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useAIChatStore } from '@/stores/aiChat'
import AIChatMessage from './AIChatMessage.vue'

export default {
  name: 'AIChatMessages',

  components: {
    AIChatMessage,
  },

  props: {
    messages: {
      type: Array,
      required: true,
    },
    streaming: {
      type: Boolean,
      default: false,
    },
    typing: {
      type: Boolean,
      default: false,
    },
  },

  setup(props) {
    const messagesContainer = ref(null)
    const scrollTimer = ref(null) // 滚动节流定时器
    const lastScrollTime = ref(0) // 上次滚动时间

    // 「粘底跟随」开关：只有用户处在底部附近时才自动滚动。
    // 否则流式回复期间用户想往上翻看没读完的内容，会被每次自动滚动立刻拽回底部。
    const BOTTOM_THRESHOLD = 40 // px：距底部在此范围内仍视为“在底部”
    const stickToBottom = ref(true)
    // 上一次 scroll 事件的 scrollTop：用于判定滚动方向（见 handleScroll）。
    // 模块级普通变量即可 —— 每次滚动事件同步更新，无需响应式。
    let lastScrollTop = 0

    /**
     * 根据当前滚动位置更新「粘底」状态（方向感知）。
     *
     * 为什么不能只看距离：流式期间内容持续长高，与 scroll 事件存在竞态 ——
     * 用户明明已滚到最底部，事件触发瞬间 scrollHeight 又长了，distance 会被
     * 推过阈值，单看距离就误判成「用户在上方」→ 按钮到了底部也不消失，
     * 且 stickToBottom=false 会短路所有自动滚动，内容越涨越追不上。
     *
     * 因此分两个信号：
     * - 触及底部（distance ≤ 阈值）→ 无条件恢复粘底（最强信号）；
     * - 只有 scrollTop 真的减小（用户向上滚）才解除粘底；
     *   内容增长 / 程序向下滚动（scrollTop 不变或增大）不改变状态。
     */
    const handleScroll = () => {
      const el = messagesContainer.value
      if (!el) return
      const st = el.scrollTop
      const distance = el.scrollHeight - st - el.clientHeight
      if (distance <= BOTTOM_THRESHOLD) {
        stickToBottom.value = true
      } else if (st < lastScrollTop - 1) {
        // 1px 容差：过滤触控板惯性滚动中的抖动
        stickToBottom.value = false
      }
      lastScrollTop = st
    }

    /**
     * 点击「回到底部」：恢复粘底跟随并立即滚到底。
     * 高速流式下内容持续增长，手动滚动很难落进 40px 阈值，故提供这个确定性入口。
     */
    const handleScrollToBottomClick = () => {
      stickToBottom.value = true
      scrollToBottom(true)
    }

    // 模型撰写工具参数（如创建技能的正文）的进度已迁移到 AI 气泡内的
    // 工具胶囊（见 AIChatMessage.vue 的 toolDraftLabel + .tool-pill），
    // 本组件的 typing 指示器只负责「等待首 token」这一件事，因此不再需要
    // 文案计算 —— 原先那行「正在思考中...」既无信息量，也已被胶囊取代。
    const aiChatStore = useAIChatStore()
    // 主题色：与 AIChatMessage 保持同一取法。
    // 注：不能依赖 --ai-chat-theme-color 变量 —— 它只定义在 AIChatInput 的
    // .chat-input-wrapper 上（兄弟组件子树），本组件取不到。
    const themeColor = computed(
      () => aiChatStore.config?.theme_color || '#4facfe'
    )

    /**
     * 滚动到底部（带节流）
     * <p>
     * 受「粘底跟随」约束：用户已向上翻阅历史时不再自动滚动，
     * 直到他滚回底部、或又有新消息加入（见下方 messages.length 监听）。
     * 需要无条件滚到底的场景（发送新消息、初次挂载）请传 immediate=true。
     */
    const scrollToBottom = (immediate = false) => {
      if (!immediate && !stickToBottom.value) {
        return
      }
      const now = Date.now()
      // 流式时的滚动节流粒度：50ms ≈ 20 次/秒。
      // 高速模型（如 1000 token/s）下，150ms 意味着每次滚动要一次吞掉约 150 token，
      // 视觉上是"一截一截往下跳"；50ms 时单次增量降到约 50 token，接近连续跟随。
      // 非流式场景无需节流。
      const throttleTime = props.streaming ? 50 : 0

      // 如果是立即滚动，或者距离上次滚动超过节流时间，则执行滚动
      if (immediate || now - lastScrollTime.value > throttleTime) {
        // 清除之前的定时器
        if (scrollTimer.value) {
          clearTimeout(scrollTimer.value)
          scrollTimer.value = null
        }

        lastScrollTime.value = now
        nextTick(() => {
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop =
              messagesContainer.value.scrollHeight
          }
        })
      } else {
        // 节流期间，使用定时器延迟执行最后一次滚动
        if (scrollTimer.value) {
          clearTimeout(scrollTimer.value)
        }
        scrollTimer.value = setTimeout(() => {
          lastScrollTime.value = Date.now()
          nextTick(() => {
            const el = messagesContainer.value
            // 执行时刻再校验一次：从调度到执行间隔了 throttleTime，
            // 期间用户可能已向上翻阅 —— 不校验会把刚翻上去的人拽回底部。
            if (!el || !stickToBottom.value) return
            el.scrollTop = el.scrollHeight
          })
        }, throttleTime)
      }
    }

    // 有新消息加入（用户发出提问 / 收到完整回复）：恢复粘底跟随并强制滚到底部。
    // 与下面监听「内容变化」的 deep watch 区分开：长度变化是低频的节点事件，
    // 内容变化是流式期间的高频事件（后者必须服从粘底开关，否则用户翻不上去）。
    watch(
      () => props.messages.length,
      () => {
        stickToBottom.value = true
        scrollToBottom(true)
      }
    )

    // 监听流式状态
    watch(
      () => props.streaming,
      (newVal) => {
        if (newVal) {
          scrollToBottom()
        }
      }
    )

    // 监听打字指示器状态
    watch(
      () => props.typing,
      (newVal) => {
        if (newVal) {
          scrollToBottom()
        }
      }
    )

    // 监听消息内容变化（用于打字机效果实时滚动）
    watch(
      () => props.messages,
      () => {
        scrollToBottom()
      },
      { deep: true }
    )

    // 组件挂载后滚动到底部（确保历史消息加载后在底部）
    onMounted(() => {
      // 延迟一下，确保DOM完全渲染
      setTimeout(() => {
        scrollToBottom()
      }, 100)
    })

    return {
      messagesContainer,
      themeColor,
      // ⚠️ 必须返回给模板：v-show="!stickToBottom" 依赖它。
      // 此前漏掉 → 模板解析到 undefined → 按钮永远显示（生产构建下无告警，静默失效）。
      stickToBottom,
      scrollToBottom,
      handleScroll,
      handleScrollToBottomClick,
    }
  },
}
</script>

<style scoped>
/* 外层容器承担原本 .chat-messages 的 flex 角色，并作为浮动按钮的定位基准。
   按钮相对它定位 → 固定在可视区右下角，不随消息内容滚动。 */
.chat-messages-wrap {
  position: relative;
  flex: 1;
  min-height: 0; /* flex 子项允许收缩，否则内容溢出时高度计算异常 */
  display: flex;
  flex-direction: column;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: transparent;
  scroll-behavior: auto;
}
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #b0b0b0;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}
.empty-state i {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.5;
}
.message-wrapper {
  margin-bottom: 15px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.typing-indicator {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 18px 18px 18px 4px;
  margin-bottom: 15px;
  animation: fadeInUp 0.3s ease-out;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
.typing-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}
.typing-dot {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: typingDot 1.4s infinite;
}
.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typingDot {
  0%,
  60%,
  100% {
    transform: scale(1);
    opacity: 0.5;
  }
  30% {
    transform: scale(1.2);
    opacity: 1;
  }
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.dark-mode .typing-indicator {
  background: rgba(44, 62, 80, 0.9);
}
.dark-mode .typing-dot {
  background: #666;
}
.dark-mode .empty-state {
  color: #b0b0b0;
}
.dark-mode .chat-messages::-webkit-scrollbar-thumb {
  background: #555;
}
.dark-mode .chat-messages::-webkit-scrollbar-thumb:hover {
  background: #666;
}

/* 「回到底部」浮动按钮：仅在用户向上翻阅历史时出现，水平居中于底部。
   白底 + 主题色箭头，与 AIChatInput 的内联 SVG 图标风格保持一致。 */
.scroll-bottom-btn {
  position: absolute;
  left: 50%;
  bottom: 12px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50%;
  background: #fff;
  /* 兜底色；实际由模板上的内联 :style="{ color: themeColor }" 提供 */
  color: #4facfe;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  animation: scroll-bottom-btn-in 0.18s ease;
  z-index: 3;
}
.scroll-bottom-btn:hover {
  transform: translate(-50%, -1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.16);
}
.scroll-bottom-btn:active {
  transform: translateX(-50%) scale(0.94);
}
.scroll-bottom-btn svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
/* 绕按钮流转的「流星」光弧 —— 仅在 AI 生成中出现（streaming 时挂 .btn-meteor）。
   动画本身携带信息：「下面还有内容在生成」；不在生成时按钮就是个干净的圆钮。
   贴合方式：inset: -1px + 细环遮罩，让光弧正好压在按钮边缘上（半径约 16px 处），
   视觉上就是「按钮的边框在流转」，而不是外面另套一个圈。
   实现：conic-gradient 造彗星拖尾（220° 前透明 → 300° 达到主题色 → 320° 消失），
   radial-gradient 遮罩只保留最外圈约 1.7px 的细环，再整体旋转 360°。
   颜色用 currentColor，自动跟随主题色。 */
.btn-meteor::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    transparent 0deg 220deg,
    currentColor 300deg,
    transparent 320deg,
    transparent 360deg
  );
  -webkit-mask: radial-gradient(closest-side, transparent 0 88%, #000 90% 100%);
  mask: radial-gradient(closest-side, transparent 0 88%, #000 90% 100%);
  animation: scroll-bottom-btn-meteor 1.8s linear infinite;
  pointer-events: none;
}
@keyframes scroll-bottom-btn-meteor {
  to {
    transform: rotate(360deg);
  }
}
/* v-show 从 display:none 切回时重放一次入场动画（需带上居中偏移） */
@keyframes scroll-bottom-btn-in {
  from {
    opacity: 0;
    transform: translate(-50%, 4px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}
.dark-mode .scroll-bottom-btn {
  background: rgba(44, 62, 80, 0.95);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}
.dark-mode .scroll-bottom-btn:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}
</style>
