<template>
  <div class="oauth-callback-container">
    <div class="callback-content">
      <div v-if="loading" class="loading-section">
        <el-icon style="font-size: 48px; color: var(--themeBackground)"
          ><el-icon-loading
        /></el-icon>
        <p class="loading-text">正在处理授权信息...</p>
      </div>

      <div v-else-if="success" class="success-section">
        <el-icon style="font-size: 48px; color: var(--green)"
          ><el-icon-success
        /></el-icon>
        <h3>{{ operationType === 'bind' ? '绑定成功！' : '登录成功！' }}</h3>
        <p>{{ successMessage }}</p>
        <el-button type="primary" @click="goToUserCenter"
          >返回个人中心</el-button
        >
      </div>

      <div v-else class="error-section">
        <el-icon style="font-size: 48px; color: var(--red)"
          ><el-icon-error
        /></el-icon>
        <h3>{{ getErrorTitle() }}</h3>
        <p>{{ errorMessage }}</p>
        <el-button type="primary" @click="goToLogin">{{
          getErrorButtonLabel()
        }}</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import {
  Loading as ElIconLoading,
  SuccessFilled as ElIconSuccess,
  CircleCloseFilled as ElIconError,
} from '@element-plus/icons-vue'
export default {
  components: {
    ElIconLoading,
    ElIconSuccess,
    ElIconError,
  },
  name: 'OAuthCallback',
  data() {
    return {
      loading: true,
      success: false,
      successMessage: '',
      errorMessage: '',
      operationType: 'login', // Default to login
    }
  },
  mounted() {
    this.handleCallback()
  },
  methods: {
    handleCallback() {
      try {
        // 获取URL参数
        const urlParams = new URLSearchParams(window.location.search)
        const code = urlParams.get('code')
        const state = urlParams.get('state')
        const error = urlParams.get('error')
        const success = urlParams.get('success')
        const message = urlParams.get('message')
        const platformType =
          urlParams.get('platform') || this.$route.query.platform
        const context = (
          urlParams.get('action') ||
          urlParams.get('context') ||
          this.$route.query.action ||
          'login'
        ).toLowerCase()
        this.operationType = ['bind', 'binding'].includes(context)
          ? 'bind'
          : 'login'

        // 检查是否已经处理完成（来自后端的直接结果）
        if (success === 'true') {
          this.handleSuccess(
            message || '绑定成功！现在您可以使用第三方账号登录了'
          )
          return
        }

        // 检查是否有错误
        if (error) {
          const friendlyMessage = this.translateError(error, platformType)
          this.handleError(friendlyMessage)
          return
        }

        // 检查必要参数（旧的流程，用于向后兼容）
        if (!code || !state) {
          this.handleError('缺少必要的授权参数')
          return
        }

        if (!platformType) {
          this.handleError('缺少平台类型参数')
          return
        }

        // 调用后端绑定接口（旧的流程，用于向后兼容）
        this.bindAccount(platformType, code, state)
      } catch (error) {
        console.error('处理OAuth回调失败:', error)
        this.handleError('处理授权信息失败')
      }
    },

    bindAccount(platformType, code, state) {
      const params = new URLSearchParams()
      params.append('platformType', platformType)
      params.append('code', code)
      params.append('state', state)

      this.$http
        .post(this.$constant.baseURL + '/user/bindThirdPartyAccount', params, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        })
        .then((res) => {
          this.loading = false
          if (res.code === 200) {
            this.success = true
            this.successMessage =
              res.message || '账号绑定成功！现在您可以使用第三方账号登录了'
          } else {
            this.handleError(res.message || '绑定失败，请稍后再试')
          }
        })
        .catch((error) => {
          console.error('绑定第三方账号失败:', error)
          this.handleError('绑定失败，请稍后再试')
        })
    },

    handleSuccess(message) {
      this.loading = false
      this.success = true
      this.successMessage = message
    },

    handleError(message) {
      this.loading = false
      this.success = false
      this.errorMessage = message
    },

    goToUserCenter() {
      // 获取原始重定向路径
      const redirectPath =
        this.$route.query.redirect ||
        sessionStorage.getItem('oauthRedirectPath') ||
        '/user'

      // 清除保存的重定向路径
      sessionStorage.removeItem('oauthRedirectPath')

      // 检查是否需要邮箱收集
      const tempUserDataStr = localStorage.getItem('tempUserData')
      if (tempUserDataStr) {
        try {
          const tempUserData = JSON.parse(tempUserDataStr)
          if (tempUserData.needsEmailCollection) {
            // 重定向到原始页面，并添加showEmailCollection参数
            this.$router.push({
              path: redirectPath,
              query: { showEmailCollection: 'true' },
              replace: true,
            })
            return
          }
        } catch (error) {
          console.error('解析临时用户数据失败:', error)
          localStorage.removeItem('tempUserData')
        }
      }

      // 重定向到原始页面
      this.$router.push({
        path: redirectPath,
        replace: true,
      })
    },

    goToLogin() {
      this.$router.push('/user')
    },

    getErrorTitle() {
      return this.operationType === 'bind' ? '绑定失败' : '登录失败'
    },

    getErrorButtonLabel() {
      return this.operationType === 'bind' ? '返回个人中心' : '返回登录页'
    },

    translateError(error, platformType) {
      const normalizedError = (error || '').toLowerCase()
      const providerName = (platformType || '').toLowerCase()
      const actionText = this.operationType === 'bind' ? '绑定' : '登录'

      if (
        normalizedError.includes('oauth_error') ||
        normalizedError.includes('timeout')
      ) {
        if (
          ['github', 'google', 'x', 'twitter', 'yandex'].includes(providerName)
        ) {
          return `${this.formatProviderName(
            providerName
          )} ${actionText}服务暂时不可用，可能由于网络限制导致授权失败，请稍后再试或选择 Gitee。`
        }
        return `第三方${actionText}服务暂时不可用，请稍后再试。`
      }

      if (
        normalizedError.includes('config_error') ||
        normalizedError.includes('not configured')
      ) {
        return `该第三方账号暂未配置${actionText}，请联系管理员。`
      }

      return `授权失败: ${error}`
    },

    formatProviderName(provider) {
      const mapping = {
        github: 'GitHub',
        google: 'Google',
        gitee: 'Gitee',
        qq: 'QQ',
        x: 'Twitter',
        twitter: 'Twitter',
        yandex: 'Yandex',
        baidu: 'Baidu',
      }
      return mapping[provider] || provider || '第三方'
    },
  },
}
</script>

<style scoped>
.oauth-callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--background);
}
.callback-content {
  text-align: center;
  padding: 40px;
  background: var(--whiteMask);
  border-radius: 10px;
  box-shadow: 0 4px 20px var(--miniMask);
  backdrop-filter: blur(10px);
  border: 1px solid var(--lightGray);
  max-width: 400px;
  width: 90%;
}
.loading-section,
.success-section,
.error-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.loading-text {
  color: var(--fontColor);
  font-size: 16px;
  margin: 0;
}
.success-section h3 {
  color: var(--green);
  margin: 0;
  font-size: 24px;
}
.error-section h3 {
  color: var(--red);
  margin: 0;
  font-size: 24px;
}
.success-section p,
.error-section p {
  color: var(--fontColor);
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}
@media screen and (max-width: 480px) {
  .callback-content {
    padding: 30px 20px;
  }
  .success-section h3,
  .error-section h3 {
    font-size: 20px;
  }
  .loading-text,
  .success-section p,
  .error-section p {
    font-size: 13px;
  }
}
</style>
