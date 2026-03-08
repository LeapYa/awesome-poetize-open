<!-- eslint-disable vue/no-mutating-props -->
<template>
  <div>
    <el-form-item label="插件类型">
      <el-input v-model="form.pluginType" disabled></el-input>
    </el-form-item>
    <el-form-item label="插件名称" prop="pluginName">
      <el-input v-model="form.pluginName"></el-input>
    </el-form-item>
    <el-form-item label="标识符(Key)" prop="pluginKey">
      <el-input v-model="form.pluginKey" :disabled="isEdit"></el-input>
      <div class="sub-title">唯一标识，不可重复，创建后不可修改</div>
    </el-form-item>
    <el-form-item label="描述" prop="pluginDescription">
      <el-input type="textarea" v-model="form.pluginDescription"></el-input>
    </el-form-item>
    <el-form-item v-if="showJsonConfig" label="JSON配置" prop="pluginConfig">
      <el-input type="textarea" :rows="4" v-model="form.pluginConfig" placeholder="请输入JSON格式配置"></el-input>
      <div class="sub-title">用于配置插件参数，必须是合法的JSON格式。例如：{"colors": ["#FF0000", "#00FF00"]}</div>
    </el-form-item>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
export default {
  name: 'PluginEditorBaseFields',
  props: {
    form: { type: Object, required: true },
    isEdit: { type: Boolean, default: false },
    showJsonConfig: { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.sub-title {
  font-size: 12px;
  color: #999;
  line-height: 20px;
}
</style>
