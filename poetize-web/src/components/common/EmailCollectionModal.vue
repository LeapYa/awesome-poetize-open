<template>
  <div class="email-collection-modal" v-if="visible">
    <div class="modal-overlay" @click="handleSkip"></div>
    <div class="modal-content">
      <div class="modal-header">
        <h3>完善个人信息</h3>
        <el-icon class="close-btn"><el-icon-close /></el-icon>
      </div>

      <div class="modal-body">
        <div class="welcome-info">
          <div class="avatar">
            <img
              :src="$common.getAvatarUrl(userInfo.avatar)"
              :alt="userInfo.username"
            />
          </div>
          <div class="user-info">
            <h4>欢迎，{{ userInfo.username }}！</h4>
            <p class="platform-info">通过 {{ platformName }} 登录成功</p>
          </div>
        </div>

        <div class="email-section">
          <div class="section-title">
            <el-icon><el-icon-message /></el-icon>
            <span>邮箱地址（可选）</span>
          </div>
          <p class="email-description">
            我们仅使用您的邮箱发送重要通知，如文章订阅、评论回复、安全验证等。
            <br />为保证账号安全，需要验证邮箱所有权。您也可以稍后在个人设置中添加。
          </p>

          <el-input
            v-model="email"
            placeholder="请输入您的邮箱地址（可选）"
            :prefix-icon="ElIconMessage"
            :class="{ error: emailError }"
            @input="clearEmailError"
            @keyup.enter="handleConfirm"
          />
          <div class="error-message" v-if="emailError">{{ emailError }}</div>

          <div class="code-input-group" v-if="email">
            <el-input
              v-model="verificationCode"
              placeholder="请输入验证码"
              :prefix-icon="ElIconKey"
              :class="{ error: codeError }"
              @input="clearCodeError"
              @keyup.enter="handleConfirm"
              class="code-input"
            />
            <el-button
              @click="sendCode"
              :disabled="
                countdown > 0 || !email || !validateEmail(email) || sendingCode
              "
              :loading="sendingCode"
              class="send-code-btn"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
            </el-button>
          </div>
          <div class="error-message" v-if="codeError">{{ codeError }}</div>
        </div>
      </div>

      <div class="modal-footer">
        <el-button @click="handleSkip" class="skip-btn">
          跳过，稍后设置
        </el-button>
        <el-button type="primary" @click="handleConfirm" :loading="submitting">
          {{ email ? '保存并继续' : '直接进入' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { $on, $off, $once, $emit } from '../../utils/gogocodeTransfer'
import {
  Close as ElIconClose,
  Message as ElIconMessage,
  Key as ElIconKey,
} from '@element-plus/icons-vue'
export default {
  data() {
    return {
      email: '',
      verificationCode: '',
      emailError: '',
      codeError: '',
      submitting: false,
      sendingCode: false,
      countdown: 0,
      countdownTimer: null,
      ElIconMessage,
      ElIconKey,
    }
  },
  components: {
    ElIconClose,
    ElIconMessage,
  },
  name: 'EmailCollectionModal',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    userInfo: {
      type: Object,
      default: () => ({}),
    },
    provider: {
      type: String,
      default: '',
    },
  },
  computed: {
    platformName() {
      const platformNames = {
        gitee: 'Gitee',
        github: 'GitHub',
        google: 'Google',
        yandex: 'Yandex',
        x: 'Twitter',
        qq: 'QQ',
        baidu: 'Baidu',
      }
      return platformNames[this.provider] || this.provider
    },
  },
  methods: {
    validateEmail(email) {
      if (!email) return true // 邮箱是可选的
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      return emailRegex.test(email)
    },

    clearEmailError() {
      this.emailError = ''
    },

    clearCodeError() {
      this.codeError = ''
    },

    async sendCode() {
      // 验证邮箱格式
      if (!this.email) {
        this.emailError = '请先输入邮箱地址'
        return
      }

      if (!this.validateEmail(this.email)) {
        this.emailError = '请输入有效的邮箱地址'
        return
      }

      this.sendingCode = true
      this.emailError = ''

      try {
        const response = await this.$http.get(
          this.$constant.baseURL + '/user/getCodeForBind',
          {
            place: this.email,
            flag: 2, // 2表示邮箱
          }
        )

        if (response.code === 200) {
          this.$message.success('验证码已发送，请注意查收！')

          // 开始倒计时
          this.countdown = 60
          this.countdownTimer = setInterval(() => {
            this.countdown--
            if (this.countdown <= 0) {
              clearInterval(this.countdownTimer)
              this.countdownTimer = null
            }
          }, 1000)
        } else {
          this.$message.error(response.message || '发送验证码失败')
        }
      } catch (error) {
        console.error('发送验证码失败:', error)
        this.$message.error(error.message || '发送验证码失败，请稍后重试')
      } finally {
        this.sendingCode = false
      }
    },

    async handleConfirm() {
      // 如果用户没有输入邮箱，直接跳过
      if (!this.email) {
        this.handleSkip()
        return
      }

      // 验证邮箱格式
      if (!this.validateEmail(this.email)) {
        this.emailError = '请输入有效的邮箱地址'
        return
      }

      // 验证验证码
      if (!this.verificationCode || this.verificationCode.trim().length === 0) {
        this.codeError = '请输入验证码'
        return
      }

      if (this.verificationCode.trim().length !== 6) {
        this.codeError = '验证码格式错误，应为6位数字'
        return
      }

      this.submitting = true
      this.emailError = ''
      this.codeError = ''

      try {
        // 调用 updateSecretInfo API 验证并保存邮箱
        const response = await this.$http.post(
          this.$constant.baseURL + '/user/updateSecretInfo',
          null,
          {
            place: this.email,
            flag: 2, // 2表示邮箱
            code: this.verificationCode,
            password: '', // 邮箱绑定不需要密码
          }
        )

        if (response.code === 200) {
          this.$message.success('邮箱验证成功！')

          // 完成登录流程
          $emit(this, 'complete', {
            email: this.email,
            skipped: false,
          })
        } else {
          this.codeError = response.message || '验证码错误'
        }
      } catch (error) {
        console.error('邮箱验证失败:', error)
        this.codeError = error.message || '验证失败，请检查验证码'
      } finally {
        this.submitting = false
      }
    },

    handleSkip() {
      $emit(this, 'complete', {
        email: '',
        skipped: true,
      })
    },
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        // 模态框显示时重置状态
        this.email = ''
        this.verificationCode = ''
        this.emailError = ''
        this.codeError = ''
        this.submitting = false
        this.sendingCode = false
        this.countdown = 0
      } else {
        // 模态框关闭时清理倒计时器
        if (this.countdownTimer) {
          clearInterval(this.countdownTimer)
          this.countdownTimer = null
        }
      }
    },
  },
  beforeUnmount() {
    // 组件销毁前清理倒计时器
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },
  emits: ['complete'],
}
</script>

<style scoped>
.email-collection-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}
.modal-content {
  position: relative;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  animation: modalSlideIn 0.3s ease-out;
}
@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}
.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.close-btn {
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s ease, color 0.2s ease;
}
.close-btn:hover {
  color: #666;
  background: #f5f5f5;
}
.modal-body {
  padding: 0 24px 24px;
}
.welcome-info {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: linear-gradient(
    -45deg,
    #e8d8b9,
    #eccec5,
    #a3e9eb,
    #bdbdf0,
    #eec1ea
  );
  background-size: 400% 400%;
  animation: gradientBG 10s ease infinite;
  border-radius: 8px;
  color: white;
}
.avatar {
  margin-right: 16px;
}
.avatar img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
}
.user-info h4 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.platform-info {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}
.email-section {
  margin-bottom: 24px;
}
.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.section-title i {
  margin-right: 8px;
  color: #409eff;
}
.email-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}
.code-input-group {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.code-input {
  flex: 1;
}
.send-code-btn {
  min-width: 120px;
  white-space: nowrap;
}
.el-input.error :deep(.el-input__inner){
  border-color: #f56c6c;
}
.error-message {
  margin-top: 8px;
  font-size: 12px;
  color: #f56c6c;
}
.modal-footer {
  display: flex;
  justify-content: space-between;
  padding: 20px 24px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}
.skip-btn {
  color: #666;
  border-color: #ddd;
}
.skip-btn:hover {
  color: #409eff;
  border-color: #409eff;
}
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  .modal-header,
  .modal-body {
    padding-left: 16px;
    padding-right: 16px;
  }
  .modal-footer {
    padding: 16px;
    flex-direction: column;
    gap: 12px;
  }
  .welcome-info {
    flex-direction: column;
    text-align: center;
  }
  .avatar {
    margin-right: 0;
    margin-bottom: 12px;
  }
}
</style>
