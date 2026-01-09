<template>
  <div class="global-captcha-container" v-if="visible">
    <captcha-wrapper
      :visible="visible"
      :action="captchaAction"
      :is-reply-comment="isReplyComment"
      @success="onCaptchaSuccess"
      @fail="onCaptchaFail"
      @close="onCaptchaClose"
    ></captcha-wrapper>
  </div>
</template>

<script>
import CaptchaWrapper from './CaptchaWrapper.vue';
import { useMainStore } from '@/stores/main';

export default {
  name: 'CaptchaContainer',
  components: {
    CaptchaWrapper
  },
  computed: {
    mainStore() {
      return useMainStore();
    },
    visible() {
      return this.mainStore.captcha.show;
    },
    captchaAction() {
      return this.mainStore.captcha.action;
    },
    captchaParams() {
      return this.mainStore.captcha.params;
    },
    isReplyComment() {
      return this.mainStore.captcha.isReplyComment;
    }
  },
  methods: {
    // 验证码成功回调
    onCaptchaSuccess(token) {
      this.mainStore.executeCaptchaCallback(token);
    },
    
    // 验证码失败回调
    onCaptchaFail() {
    },
    
    // 关闭验证码
    onCaptchaClose() {
      this.mainStore.showCaptcha(false);
    }
  }
}
</script>

<style scoped>
.global-captcha-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 3000;
}
</style> 