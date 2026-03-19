<template>
  <div v-if="article.paywalled" class="paywall-block">
    <div class="paywall-fade"></div>
    <div class="paywall-card">
      <div class="paywall-badge">
        <span v-if="article.payType === 2">VIP专享</span>
        <span v-else>付费解锁</span>
      </div>
      <div class="paywall-hidden-prompt">
        <svg
          viewBox="0 0 1024 1024"
          width="16"
          height="16"
          style="vertical-align: -2px; margin-right: 6px"
        >
          <path
            d="M512 42.666667A469.333333 469.333333 0 1 0 981.333333 512 469.333333 469.333333 0 0 0 512 42.666667z m0 768a53.333333 53.333333 0 1 1 53.333333-53.333334 53.333333 53.333333 0 0 1-53.333333 53.333334z m53.333333-234.666667a53.333333 53.333333 0 0 1-106.666666 0v-256a53.333333 53.333333 0 0 1 106.666666 0z"
            fill="#ff4d79"
          ></path>
        </svg>
        此处内容已隐藏
      </div>
      <div v-if="article.payType === 1" class="paywall-desc">
        支付 <strong>¥{{ article.payAmount }}</strong> 即可解锁全文
      </div>
      <div v-else-if="article.payType === 2" class="paywall-desc">
        本文为会员专属内容，成为会员即可阅读
      </div>
      <div v-else-if="article.payType === 3" class="paywall-desc">
        赞赏 <strong>¥{{ article.payAmount }}</strong> 后可解锁全文
      </div>
      <div v-else-if="article.payType === 4" class="paywall-desc">
        支付 <strong>¥{{ article.payAmount }}</strong> 即可解锁全文
      </div>
      <div class="paywall-actions">
        <el-button
          type="primary"
          size="large"
          round
          :loading="paymentLoading"
          @click="emit('pay')"
        >
          {{ article.payType === 2 ? '成为会员' : '立即解锁' }}
        </el-button>
      </div>
      <div v-if="article.paidCount > 0" class="paywall-meta">
        已有 {{ article.paidCount }} 人解锁
      </div>
      <div class="paywall-verify">
        <a
          href="javascript:void(0)"
          :class="{ checking: verifyingPayment }"
          @click="emit('verify')"
        >
          {{ verifyingPayment ? '验证中...' : '已支付？点击刷新状态' }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  article: {
    type: Object,
    required: true,
  },
  paymentLoading: {
    type: Boolean,
    default: false,
  },
  verifyingPayment: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['pay', 'verify'])
</script>
