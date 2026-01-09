<template>
  <div>
    <!-- 框 -->
    <textarea
      class="comment-textarea"
      v-model="commentContent"
      placeholder="写下点什么..."
      maxlength="1000"
    />
    <!-- 按钮 -->
    <div class="myBetween" style="margin-bottom: 10px">
      <div style="display: flex">
        <div
          :class="{ 'emoji-active': showEmoji }"
          @click="showEmoji = !showEmoji"
        >
          <el-icon class="myEmoji"><el-icon-orange /></el-icon>
        </div>
        <div @click="openPicture()">
          <el-icon class="myPicture"><el-icon-picture /></el-icon>
        </div>
      </div>

      <div style="display: flex">
        <!--        <proButton :info="'涂鸦'"-->
        <!--                   v-show="!$common.mobile() && !disableGraffiti"-->
        <!--                   @click.native="showGraffiti()"-->
        <!--                   :before="$constant.before_color_1"-->
        <!--                   :after="$constant.after_color_1"-->
        <!--                   style="margin-right: 6px">-->
        <!--        </proButton>-->
        <proButton
          :info="'提交'"
          @click="submitComment()"
          :before="$constant.before_color_2"
          :after="$constant.after_color_2"
        >
        </proButton>
      </div>
    </div>
    <!-- 表情 -->
    <emoji @addEmoji="addEmoji" :showEmoji="showEmoji"></emoji>

    <el-dialog
      title="图片"
      v-model="showPicture"
      width="25%"
      :append-to-body="true"
      custom-class="centered-dialog"
      :close-on-click-modal="false"
      destroy-on-close
      center
    >
      <div>
        <uploadPicture
          :prefix="'commentPicture'"
          @addPicture="addPicture"
          :maxSize="2"
          :maxNumber="1"
        ></uploadPicture>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { $on, $off, $once, $emit } from '../../utils/gogocodeTransfer'
import {
  Orange as ElIconOrange,
  Picture as ElIconPicture,
} from '@element-plus/icons-vue'
import { useMainStore } from '@/stores/main'


export default {
  components: {
    emoji: defineAsyncComponent(() => import('../common/emoji')),
    proButton: defineAsyncComponent(() => import('../common/proButton')),
    uploadPicture: defineAsyncComponent(() => import('../common/uploadPicture')),
    ElIconOrange,
    ElIconPicture,
  },
  props: {
    disableGraffiti: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      commentContent: '',
      showEmoji: false,
      showPicture: false,
      picture: {
        name: '',
        url: '',
      },
    }
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
  },
  mounted() {
    // 监听恢复评论的事件
    $on(this.$bus, 'restore-comment', (comment) => {
      if (comment) {
        this.commentContent = comment
      }
    })

    // 初始化图片名称为当前用户名
    if (this.mainStore.currentUser && this.mainStore.currentUser.username) {
      this.picture.name = this.mainStore.currentUser.username
    }
  },
  beforeUnmount() {
    // 清除事件监听
    $off(this.$bus, 'restore-comment')
  },
  methods: {
    openPicture() {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        this.$message({
          message: '请先登录！',
          type: 'error',
        })
        return
      }

      this.showPicture = true
    },

    addPicture(res) {
      this.picture.url = res
      this.savePicture()
    },
    savePicture() {
      // 确保有用户名，如果没有则使用当前用户名
      const username =
        this.picture.name ||
        (this.mainStore.currentUser && this.mainStore.currentUser.username) ||
        '匿名'
      let img = '[' + username + ',' + this.picture.url + ']'
      this.commentContent += img
      this.picture.url = ''
      this.showPicture = false
    },
    addEmoji(key) {
      this.commentContent += key
    },
    showGraffiti() {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        this.$message({
          message: '请先登录！',
          type: 'error',
        })
        return
      }

      this.commentContent = ''
      $emit(this, 'showGraffiti')
    },
    submitComment() {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        // 保存评论内容和当前页面URL到localStorage
        const articleId = this.$route.params.id
        const tempComment = {
          content: this.commentContent.trim(),
          timestamp: Date.now(),
          articleUrl: window.location.href,
        }
        localStorage.setItem(
          `tempComment_${articleId}`,
          JSON.stringify(tempComment)
        )

        // 使用统一的登录跳转函数
        this.$common.redirectToLogin(
          this.$router,
          {
            extraQuery: { hasComment: 'true' },
            message: '请先登录！评论内容已保存，登录后将自动恢复',
          },
          this
        )
        return
      }

      if (this.commentContent.trim() === '') {
        this.$message({
          message: '你还没写呢~',
          type: 'warning',
        })
        return
      }
      $emit(this, 'submitComment', this.commentContent.trim())
      // 注意：不在这里清空评论内容，由父组件根据验证码流程决定何时清空
    },

    // 清空评论内容（由父组件调用）
    clearComment() {
      this.commentContent = ''
    },

    // 恢复评论内容（验证码取消时调用）
    restoreComment(content) {
      if (content) {
        this.commentContent = content
      }
    },
  },
  emits: ['submitComment', 'showGraffiti'],
}
</script>

<style scoped>
.comment-textarea {
  border: 1px solid var(--lightGray);
  width: 100%;
  font-size: 14px;
  padding: 15px;
  min-height: 180px;
  resize: none;
  outline: none;
  border-radius: 4px;
  background-color: var(--inputBackground);
  background-image: var(--commentURL);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: 100%;
  margin-bottom: 10px;
  color: var(--fontColor);
}
.comment-textarea:focus {
  border-color: var(--themeBackground);
}
body.dark-mode .comment-textarea {
  border-color: var(--borderColor);
}
body.dark-mode .comment-textarea:focus {
  border-color: var(--themeBackground);
}
.myEmoji {
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.5s ease, color 0.5s ease;
  margin-right: 12px;
  will-change: transform;
  transform: translateZ(0);
}
.myEmoji:hover {
  transform: rotate(360deg);
  font-size: 22px;
}
.myPicture {
  font-size: 18px;
  cursor: pointer;
}
.emoji-active {
  color: var(--red);
}
</style>
