<template>
  <div class="ai-appearance-config">
    <el-form :model="appearanceConfig" label-width="120px">
      <el-form-item label="机器人名称">
        <el-input v-model="appearanceConfig.botName" placeholder="例如: 小助手"></el-input>
      </el-form-item>

      <el-form-item label="主题颜色">
        <el-color-picker v-model="appearanceConfig.themeColor"></el-color-picker>
        <span style="margin-left: 10px; color: #909399; font-size: 12px;">用于用户消息气泡颜色</span>
      </el-form-item>

      <el-form-item label="显示打字动效">
        <el-switch v-model="appearanceConfig.typingAnimation"></el-switch>
      </el-form-item>

      <el-form-item label="显示时间戳">
        <el-switch v-model="appearanceConfig.showTimestamp"></el-switch>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'AiAppearanceConfig',
  props: {
    value: {
      type: Object,
      default: () => ({
        botName: 'AI助手',
        themeColor: '#409EFF',
        typingAnimation: true,
        showTimestamp: true
      })
    }
  },
  
  data() {
    return {
      appearanceConfig: { ...this.value }
    }
  },
  
  watch: {
    value: {
      handler(newVal) {
        if (JSON.stringify(newVal) !== JSON.stringify(this.appearanceConfig)) {
          this.appearanceConfig = { ...newVal };
        }
      },
      deep: true
    },
    
    appearanceConfig: {
      handler(newVal) {
        if (JSON.stringify(newVal) !== JSON.stringify(this.value)) {
          this.$emit('input', newVal);
        }
      },
      deep: true
    }
  }
}
</script>

<style scoped>
.ai-appearance-config {
  max-height: 500px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 10px;
}

/* 移动端对话框中不限制高度 */
@media screen and (max-width: 768px) {
  .ai-appearance-config {
    max-height: none;
    overflow-y: visible;
  }
}

/* PC端样式 - 768px以上 */
@media screen and (min-width: 769px) {
  ::v-deep .el-form-item__label {
    float: left !important;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .ai-appearance-config {
    padding: 0;
  }

  .ai-appearance-config .el-form-item {
    margin-bottom: 15px;
  }

  /* 标签适配 - 垂直布局 */
  .ai-appearance-config .el-form-item__label {
    float: none !important;
    width: 100% !important;
    text-align: left !important;
    font-size: 13px;
    line-height: 1.4;
    margin-bottom: 8px !important;
    padding-bottom: 0 !important;
  }

  .ai-appearance-config .el-form-item__content {
    margin-left: 0 !important;
    width: 100% !important;
  }

  /* 提示文本 */
  .ai-appearance-config .el-form-item__content span {
    font-size: 11px;
  }
}

@media screen and (max-width: 480px) {
  .ai-appearance-config .el-form-item {
    margin-bottom: 12px;
  }

  .ai-appearance-config .el-form-item__label {
    font-size: 12px;
  }

  .ai-appearance-config .el-form-item__content span {
    font-size: 10px;
  }
}
</style> 