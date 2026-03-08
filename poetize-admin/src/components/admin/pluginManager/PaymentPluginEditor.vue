<!-- eslint-disable vue/no-mutating-props -->
<template>
  <div>
    <template v-if="hasManifestSchema">
      <el-divider content-position="left">支付配置</el-divider>
      <div class="payment-config-section">
        <el-form-item v-for="(schema, key) in paymentConfigSchema" :key="key" :label="schema.label || key">
          <template v-if="schema.type === 'string'">
            <el-input
              v-model="paymentConfig[key]"
              :placeholder="getSchemaFieldPlaceholder(key, schema)"
              :type="isSecretField(key) ? 'password' : 'text'"
              :show-password="isSecretField(key)">
            </el-input>
          </template>
          <template v-else-if="schema.type === 'number'">
            <el-input-number
              v-model="paymentConfig[key]"
              :min="schema.min || 0"
              :max="schema.max || 999999"
              :precision="2"
              :step="schema.step || 1">
            </el-input-number>
          </template>
          <template v-else-if="schema.type === 'boolean'">
            <el-switch v-model="paymentConfig[key]"></el-switch>
          </template>
          <template v-else-if="schema.type === 'select' && schema.options">
            <el-select v-model="paymentConfig[key]" :placeholder="getSchemaFieldPlaceholder(key, schema)">
              <el-option v-for="opt in schema.options" :key="opt.value" :label="opt.label" :value="opt.value"></el-option>
            </el-select>
          </template>
          <template v-else>
            <el-input v-model="paymentConfig[key]" :placeholder="getSchemaFieldPlaceholder(key, schema)"></el-input>
          </template>
          <div v-if="schema.description" class="sub-title">{{ schema.description }}</div>
        </el-form-item>
      </div>
    </template>

    <el-form-item v-else label="JSON配置" prop="pluginConfig">
      <el-input type="textarea" :rows="6" v-model="form.pluginConfig" placeholder="请输入JSON格式配置"></el-input>
      <div class="sub-title">此插件未提供配置项定义，请直接编辑 JSON 配置</div>
    </el-form-item>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import { getSchemaFieldPlaceholder, isSecretField } from '@/components/admin/pluginManager/pluginManagerTransforms';

export default {
  name: 'PaymentPluginEditor',
  props: {
    form: { type: Object, required: true },
    paymentConfigSchema: { type: Object, required: true },
    paymentConfig: { type: Object, required: true }
  },
  computed: {
    hasManifestSchema() {
      return Object.keys(this.paymentConfigSchema).length > 0;
    }
  },
  methods: {
    getSchemaFieldPlaceholder,
    isSecretField
  }
}
</script>

<style scoped>
.payment-config-section {
  padding: 0 10px;
}
.payment-config-section .el-form-item {
  margin-bottom: 18px;
}
.payment-config-section .el-input,
.payment-config-section .el-select {
  max-width: 400px;
}
.payment-config-section .el-input-number {
  width: 180px;
}
.sub-title {
  font-size: 12px;
  color: #999;
  line-height: 20px;
}
</style>
