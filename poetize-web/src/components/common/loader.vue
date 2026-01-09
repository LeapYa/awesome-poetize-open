<template>
  <div>
    <!-- 加载动画 -->
    <transition name="loader">
      <div v-show="loaderVisible">
        <slot name="loader"></slot>
      </div>
    </transition>
    <!-- 内容 -->
    <transition name="body">
      <div v-show="bodyVisible">
        <slot name="body"></slot>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loaderVisible: this.loading,
      bodyVisible: !this.loading,
    }
  },
  watch: {
    loading(loading) {
      this.loaderVisible = loading
      setTimeout(() => {
        this.bodyVisible = !loading
      }, 300)
    },
  },
}
</script>

<style scoped>
.loader-enter-active,
.loader-leave-active,
.body-enter-active,
.body-leave-active {
  transition: transform 0.5s ease, opacity 0.5s ease;
  will-change: transform, opacity;
  transform: translateZ(0);
}
.loader-enter-from,
.loader-leave-to {
  opacity: 0;
}
.body-enter-from,
.body-leave-to {
  opacity: 0;
  transform: scale(0.5) translateY(50%);
}
</style>
