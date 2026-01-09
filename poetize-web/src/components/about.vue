<template>
  <div>
    <!-- 两句诗 -->
    <div class="my-animation-slide-top">
      <twoPoem :isHitokoto="false"></twoPoem>
    </div>

    <div
      style="background: var(--background)"
      class="my-animation-slide-bottom"
    >
      <div class="about-wrap">
        <h1 style="font-size: 40px; font-weight: 500; letter-spacing: 5px">
          两只毛驴鸣翠柳
        </h1>
        <!-- 对话框 -->
        <div class="about-box">
          <h4>与 {{ mainStore.webInfo.webName }} 对话中...</h4>
          <div v-if="sayShow" id="say-container"></div>
        </div>
      </div>

      <!-- 页脚 -->
      <myFooter></myFooter>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { useMainStore } from '@/stores/main'


export default {
  components: {
    twoPoem: defineAsyncComponent(() => import('./common/twoPoem')),
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
  },
  data() {
    return {
      sayShow: false,
      sayContent: [
        {
          talk: ['Hi, there👋', '这是一个 Vue2 Vue3 与 SpringBoot 结合的产物~'],
          reply: ['然后呢？ 😃', '少废话！ 🙄'],
        },
        {
          talk: [
            '😘',
            '本站平时仅用于交流和学习新知识',
            '如涉及侵权请联系站长删除对应资源，谢谢！！！',
          ],
          reply: ['这个网站有什么用吗？ 😂'],
        },
        {
          talk: [
            '拥有自己的独立网站难道不酷吗🚀',
            '那就摸鱼吧👋',
            '摸鱼大军请在聊天室集合🥝',
          ],
          reply: [],
        },
      ],
      sayIndex: 0,
    }
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
  },

  watch: {},

  created() {
    setTimeout(() => {
      this.sayShow = true
      // 使用 $nextTick 确保 DOM 更新完成后再调用 say()
      this.$nextTick(() => {
        this.say()
      })
    }, 2000)
  },

  mounted() {},

  methods: {
    answer(index, value) {
      // 移除所有 say-select 元素
      const saySelectElements = document.querySelectorAll('.say-select')
      saySelectElements.forEach((element) => {
        element.remove()
      })

      const container = document.getElementById('say-container')
      if (!container) {
        return
      }

      let htmlStr = `<div class="say-right my-animation-slide-bottom"><span class="say-item-right">${value}</span></div>`
      let frag = document.createRange().createContextualFragment(htmlStr)
      // 安全地添加fragment到container
      if (container && frag && typeof container.appendChild === 'function') {
        try {
          // 安全地添加fragment到container
          if (
            container &&
            frag &&
            typeof container.appendChild === 'function'
          ) {
            try {
              // 安全地添加fragment到container
              if (
                container &&
                frag &&
                typeof container.appendChild === 'function'
              ) {
                try {
                  // 安全地添加fragment到container
                  if (
                    container &&
                    frag &&
                    typeof container.appendChild === 'function'
                  ) {
                    try {
                      container.appendChild(frag)
                    } catch (e) {}
                  }
                } catch (e) {}
              }
            } catch (e) {}
          }
        } catch (e) {}
      }
      if (index === 0) {
        setTimeout(() => {
          this.say()
        }, 500)
      } else {
        const container = document.getElementById('say-container')
        if (container) {
          let htmlStr = `<div class="say-left my-animation-slide-bottom"><span class="say-item-left">👋 👋 👋</span></div>`
          let frag = document.createRange().createContextualFragment(htmlStr)
          container.appendChild(frag)
        }
      }
    },
    say() {
      const container = document.getElementById('say-container')
      if (!container) {
        return
      }

      if (
        !this.$common.isEmpty(this.sayContent[this.sayIndex]) &&
        !this.$common.isEmpty(this.sayContent[this.sayIndex].talk)
      ) {
        this.sayContent[this.sayIndex].talk.forEach((value, index, talk) => {
          setTimeout(() => {
            let htmlStr = `<div class="say-left my-animation-slide-bottom"><span class="say-item-left">${value}</span></div>`
            let frag = document.createRange().createContextualFragment(htmlStr)
            container.appendChild(frag)
            if (talk.length === index + 1) {
              if (!this.$common.isEmpty(this.sayContent[this.sayIndex].reply)) {
                setTimeout(() => {
                  if (this.sayContent[this.sayIndex].reply.length === 2) {
                    let reply0 = this.sayContent[this.sayIndex].reply[0]
                    let reply1 = this.sayContent[this.sayIndex].reply[1]
                    let htmlStr = `<div class="say-left my-animation-slide-bottom"><span class="say-select">${reply0}</span><span class="say-select">${reply1}</span></div>`
                    let frag = document
                      .createRange()
                      .createContextualFragment(htmlStr)
                    if (container) {
                      // 安全地添加fragment到container
                      if (
                        container &&
                        frag &&
                        typeof container.appendChild === 'function'
                      ) {
                        try {
                          // 安全地添加fragment到container
                          if (
                            container &&
                            frag &&
                            typeof container.appendChild === 'function'
                          ) {
                            try {
                              container.appendChild(frag)
                            } catch (e) {}
                          }
                        } catch (e) {}
                      }
                    }
                    document.getElementsByClassName('say-select')[0].onclick =
                      () => {
                        this.answer(0, reply0)
                      }
                    document.getElementsByClassName('say-select')[1].onclick =
                      () => {
                        this.answer(1, reply1)
                      }
                  } else if (
                    this.sayContent[this.sayIndex].reply.length === 1
                  ) {
                    let reply0 = this.sayContent[this.sayIndex].reply[0]
                    let htmlStr = `<div class="say-left my-animation-slide-bottom"><span class="say-select">${reply0}</span></div>`
                    let frag = document
                      .createRange()
                      .createContextualFragment(htmlStr)
                    if (container) {
                      container.appendChild(frag)
                    }
                    document.getElementsByClassName('say-select')[0].onclick =
                      () => {
                        this.answer(0, reply0)
                      }
                  }
                  this.sayIndex += 1
                }, 500)
              }
            }
          }, index * 500)
        })
      }
    },
  },
}
</script>

<style>
.about-wrap {
  text-align: center;
  width: 95%;
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px 80px;
}
.about-box {
  min-height: 450px;
  padding: 5px;
  background-color: var(--maxMaxLightGray);
  border-radius: 10px;
}
.say-item-left {
  padding: 5px 12px;
  border-radius: 1rem;
  color: var(--maxGreyFont);
  background-color: var(--lightGray);
}
.say-item-right {
  padding: 5px 12px;
  border-radius: 1rem;
  color: var(--white);
  background-color: var(--translucent);
}
.say-left {
  display: flex;
  justify-content: left;
  margin: 15px;
}
.say-right {
  display: flex;
  justify-content: right;
  margin: 15px;
}
.say-select {
  cursor: pointer;
  background: var(--black);
  border-radius: 5px;
  padding: 5px 10px;
  margin-right: 12px;
  margin-top: 20px;
  color: var(--white);
  border: 1px solid var(--black);
}
.say-select:hover {
  border: 1px solid var(--themeBackground);
  color: var(--themeBackground);
  box-shadow: 0 0 5px var(--themeBackground);
}
</style>
