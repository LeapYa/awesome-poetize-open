<template>
  <div class="payment-return-page">
    <div class="payment-card">
      <div v-if="status === 'checking'" class="payment-status">
        <div class="payment-spinner"></div>
        <h2>正在确认支付状态...</h2>
        <p>请稍候，正在为您验证支付结果</p>
      </div>

      <div v-else-if="status === 'success'" class="payment-status success">
        <div class="payment-icon">✅</div>
        <h2>支付成功</h2>
        <p>内容已解锁，正在跳转...</p>
      </div>

      <div v-else-if="status === 'pending'" class="payment-status pending">
        <div class="payment-icon">⏳</div>
        <h2>支付处理中</h2>
        <p>支付平台正在处理您的订单，请稍后查看</p>
        <el-button type="primary" round @click="goBack" style="margin-top: 20px">
          返回文章
        </el-button>
      </div>

      <div v-else class="payment-status error">
        <div class="payment-icon">❌</div>
        <h2>支付未完成</h2>
        <p>{{ errorMessage }}</p>
        <el-button type="primary" round @click="goBack" style="margin-top: 20px">
          返回重试
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      status: 'checking',
      errorMessage: '如果您已完成支付，请稍后刷新文章页面',
      articleId: null,
      returnPath: '/'
    }
  },
  created() {
    // 从 URL 参数获取信息
    const query = this.$route.query
    this.articleId = query.articleId || query.out_trade_no
    this.returnPath = query.returnPath || '/'

    // 从 out_trade_no 解析 articleId（格式: u{userId}_a{articleId}_{ts}）
    if (!this.articleId && query.out_trade_no) {
      const parts = query.out_trade_no.split('_')
      if (parts.length >= 2 && parts[1].startsWith('a')) {
        this.articleId = parseInt(parts[1].substring(1))
      }
    }

    this.checkPaymentStatus()
  },
  methods: {
    async checkPaymentStatus() {
      if (!this.articleId || this.articleId === 0) {
        // 会员购买，等 3 秒后提示
        await this.delay(3000)
        this.status = 'pending'
        return
      }

      let attempts = 0
      const maxAttempts = 12 // 最多检查 1 分钟

      while (attempts < maxAttempts) {
        try {
          const res = await this.$http.get(
            this.$constant.baseURL + '/payment/checkPayment',
            { articleId: this.articleId }
          )
          if (res.code === 200 && res.data === true) {
            this.status = 'success'
            // 2 秒后跳转回文章
            setTimeout(() => {
              if (this.articleId) {
                this.$router.push('/article/' + this.articleId)
              } else {
                this.$router.push(this.returnPath)
              }
            }, 2000)
            return
          }
        } catch (e) {
          // 忽略
        }
        attempts++
        await this.delay(5000)
      }

      // 超时未确认
      this.status = 'pending'
    },

    delay(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },

    goBack() {
      if (this.articleId && this.articleId !== 0) {
        this.$router.push('/article/' + this.articleId)
      } else {
        this.$router.push(this.returnPath || '/')
      }
    }
  }
}
</script>

<style scoped>
.payment-return-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.payment-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 50px 40px;
  max-width: 420px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

:root[data-theme='dark'] .payment-card,
.dark .payment-card {
  background: rgba(30, 30, 40, 0.95);
  color: #e0e0e0;
}

.payment-status h2 {
  margin: 16px 0 8px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.5;
}

.payment-status p {
  color: #888;
  font-size: 14px;
  line-height: 1.6;
}

.payment-icon {
  font-size: 56px;
  margin-bottom: 0.5em;
  line-height: 1.2;
}

.payment-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
