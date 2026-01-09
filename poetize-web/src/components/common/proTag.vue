<template>
  <div class="myTag" :style="bg">
    <span class="name">🔖 {{ info }}</span>
  </div>
</template>

<script>
export default {
  props: {
    info: {
      type: String,
      default: '一只小毛驴',
    },
    color: {
      type: String,
    },
  },
  data() {
    return {
      bg: { background: this.color },
    }
  },
}
</script>

<style scoped>
.myTag {
  cursor: pointer;
  color: var(--white);
  border-radius: 5px;
  padding: 8px;
}
.myTag:hover {
  background: var(--gradualRed) !important;
}
</style>
