<template>
  <div>
    <div class="message-hero-section">
      <el-image
        style="animation: header-effect 2s"
        class="background-image"
        v-once
        lazy
        :src="
          mainStore.webInfo.randomCover[
            Math.floor(Math.random() * mainStore.webInfo.randomCover.length)
          ]
        "
        fit="cover"
      >
        <template v-slot:error>
          <div class="image-slot background-image-error"></div>
        </template>
      </el-image>
      <!-- 输入框 -->
      <div class="message-in" style="text-align: center">
        <h2 class="message-title">树洞</h2>
        <div>
          <input
            class="message-input"
            type="text"
            style="outline: none; width: 70%"
            placeholder="留下点什么啦~"
            v-model="messageContent"
            @click="show = true"
            maxlength="60"
          />
          <button
            v-show="show"
            style="margin-left: 12px; cursor: pointer; width: 20%"
            @click="submitMessage"
            class="message-input"
          >
            发射
          </button>
        </div>
      </div>
      <!-- 向下滑动提示 -->
      <div class="scroll-down-hint" @click="scrollToComments">
        <el-icon class="el-icon-arrow-down scroll-down-arrow"><el-icon-arrow-down /></el-icon>
        <div class="scroll-down-text">向下滑动查看更多</div>
      </div>

      <!-- 弹幕 -->
      <div class="barrage-container">
        <danmaku
          ref="danmaku"
          :list="barrageList"
          :loop="true"
          :pauseOnHover="true"
        ></danmaku>
      </div>
    </div>
    <div class="comment-wrap">
      <div class="comment-content">
        <comment
          :source="$constant.source"
          :type="'message'"
          :userId="$constant.userId"
        ></comment>
      </div>
      <myFooter></myFooter>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { ArrowDown as ElIconArrowDown } from '@element-plus/icons-vue'
import { useMainStore } from '@/stores/main'


export default {
  components: {
    comment: defineAsyncComponent(() => import('./comment/comment')),
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
    danmaku: defineAsyncComponent(() => import('./common/Danmaku')),
    ElIconArrowDown,
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
  },
  data() {
    return {
      show: false,
      messageContent: '',
      // background: {"background": "url(" + this.mainStore.webInfo.backgroundImage + ") center center / cover no-repeat"},
      barrageList: [],
    }
  },
  created() {
    this.getTreeHole()
  },
  methods: {
    scrollToComments() {
      // 平滑滚动到评论区
      window.scrollTo({
        top: window.innerHeight,
        behavior: 'smooth',
      })
    },
    getTreeHole() {
      this.$http
        .get(this.$constant.baseURL + '/webInfo/listTreeHole')
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            res.data.forEach((m) => {
              this.barrageList.push({
                id: m.id,
                avatar: m.avatar, // 后端已处理随机头像
                msg: m.message,
                time: Math.floor(Math.random() * 5 + 10),
              })
            })
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })
    },
    submitMessage() {
      if (this.messageContent.trim() === '') {
        this.$message({
          message: '你还没写呢~',
          type: 'warning',
        })
        return
      }

      let treeHole = {
        message: this.messageContent.trim(),
      }

      // 如果用户已登录且有头像，使用用户头像
      // 未登录或无头像时，不设置 avatar 字段，后端会自动分配随机头像
      if (
        !this.$common.isEmpty(this.mainStore.currentUser) &&
        !this.$common.isEmpty(this.mainStore.currentUser.avatar)
      ) {
        treeHole.avatar = this.mainStore.currentUser.avatar
      }

      this.$http
        .post(this.$constant.baseURL + '/webInfo/saveTreeHole', treeHole)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.barrageList.push({
              id: res.data.id,
              avatar: res.data.avatar, // 后端已处理随机头像
              msg: res.data.message,
              time: Math.floor(Math.random() * 5 + 10),
            })
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })

      this.messageContent = ''
      this.show = false
    },
  },
}
</script>

<style scoped>
.message-in {
  position: absolute;
  left: 50%;
  top: 40%;
  transform: translate(-50%, -50%);
  color: var(--white);
  animation: hideToShow 2.5s;
  width: 360px;
  z-index: 10;
}
.message-title {
  user-select: none;
  text-align: center;
}
.message-input {
  border-radius: 1.2rem;
  border: var(--white) 1px solid;
  color: var(--white);
  background: var(--transparent);
  padding: 10px 10px;
}
.message-input::-webkit-input-placeholder {
  color: var(--white);
}
.barrage-container {
  position: absolute;
  top: 50px;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(100% - 50px);
  width: 100%;
  user-select: none;
  overflow: hidden;
}
.comment-wrap {
  background: var(--background);
  position: absolute;
  top: 100vh;
  width: 100%;
}
.comment-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
.message-hero-section {
  position: relative;
  height: 100vh;
  overflow: hidden;
}
.scroll-down-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  color: var(--white);
  z-index: 15;
  cursor: pointer;
  transition: transform 0.3s ease, opacity 0.3s ease, background-color 0.3s ease;
}
.scroll-down-hint:hover {
  transform: translateX(-50%) translateY(-5px);
  opacity: 0.8;
}
.scroll-down-arrow {
  font-size: 24px;
  animation: bounce 2s infinite;
  display: block;
  margin-bottom: 5px;
  width: 100%;
}
.scroll-down-text {
  font-size: 12px;
  opacity: 0.8;
  white-space: nowrap;
}
@keyframes bounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-8px);
  }
  60% {
    transform: translateY(-4px);
  }
}
@media screen and (max-width: 768px) {
  .scroll-down-hint {
    bottom: 20px;
  }
  .scroll-down-arrow {
    font-size: 20px;
  }
  .scroll-down-text {
    font-size: 11px;
  }
}
</style>
