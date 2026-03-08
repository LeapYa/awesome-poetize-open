<template>
  <div class="handle-box">
    <el-select :value="value" placeholder="请选择插件类型" class="handle-select mr10" @input="$emit('input', $event)" @change="$emit('change', $event)">
      <el-option label="鼠标点击效果" value="mouse_click_effect"></el-option>
      <el-option label="全屏飘落特效" value="particle_effect"></el-option>
      <el-option label="看板娘模型" value="waifu_model"></el-option>
      <el-option label="文章编辑器" value="editor"></el-option>
      <el-option label="文章主题" value="article_theme"></el-option>
      <el-option label="文章付费" value="payment"></el-option>
      <el-option label="AI工具" value="ai_tool"></el-option>
    </el-select>
    <el-button type="primary" icon="el-icon-plus" @click="$emit('create')">新增插件</el-button>
    <el-button type="success" icon="el-icon-upload2" @click="$emit('install')">安装插件包</el-button>
  </div>
</template>

<script>
export default {
  name: 'PluginTypeToolbar',
  props: {
    value: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped>
.handle-box {
  margin: 20px 0;
}
.handle-select {
  width: 150px;
}
.mr10 {
  margin-right: 10px;
}
</style>
