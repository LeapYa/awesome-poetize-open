<!-- eslint-disable vue/no-mutating-props -->
<template>
  <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="50%" :close-on-click-modal="false" custom-class="centered-dialog">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <plugin-editor-base-fields
        :form="form"
        :is-edit="isEdit"
        :show-json-config="showBaseJsonConfig" />

      <waifu-plugin-editor
        v-if="currentPluginType === 'waifu_model'"
        :waifu-config="waifuConfig"
        :preview-image="previewImage" />

      <article-theme-editor
        v-if="currentPluginType === 'article_theme'"
        :theme-config="themeConfig"
        @apply-preset="$emit('apply-theme-preset', $event)" />

      <ai-tool-plugin-editor
        v-if="currentPluginType === 'ai_tool'"
        :ai-tool-form="aiToolForm"
        :ai-tool-editor-mode="aiToolEditorMode"
        :assistant-prompt="aiToolAssistantPrompt" />

      <payment-plugin-editor
        v-if="isSchemaDrivenPluginType"
        :form="form"
        :payment-config-schema="paymentConfigSchema"
        :payment-config="paymentConfig" />

      <effect-code-editor
        v-if="showEffectCodeEditor"
        :form="form"
        :plugin-code-editor-meta="pluginCodeEditorMeta"
        :ai-prompt-dialog="aiPromptDialog"
        :ai-prompt-visible="aiPromptVisible"
        @open-ai-prompt="$emit('update:ai-prompt-visible', true)"
        @update:ai-prompt-visible="$emit('update:ai-prompt-visible', $event)" />

      <el-form-item label="是否启用">
        <el-switch v-model="form.enabled"></el-switch>
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0"></el-input-number>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="validateAndSubmit">确 定</el-button>
    </span>
  </el-dialog>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import PluginEditorBaseFields from '@/components/admin/pluginManager/PluginEditorBaseFields.vue';
import WaifuPluginEditor from '@/components/admin/pluginManager/WaifuPluginEditor.vue';
import ArticleThemeEditor from '@/components/admin/pluginManager/ArticleThemeEditor.vue';
import PaymentPluginEditor from '@/components/admin/pluginManager/PaymentPluginEditor.vue';
import AiToolPluginEditor from '@/components/admin/pluginManager/AiToolPluginEditor.vue';
import EffectCodeEditor from '@/components/admin/pluginManager/EffectCodeEditor.vue';

export default {
  name: 'PluginEditDialog',
  components: {
    PluginEditorBaseFields,
    WaifuPluginEditor,
    ArticleThemeEditor,
    PaymentPluginEditor,
    AiToolPluginEditor,
    EffectCodeEditor
  },
  props: {
    visible: { type: Boolean, default: false },
    dialogTitle: { type: String, required: true },
    form: { type: Object, required: true },
    rules: { type: Object, required: true },
    isEdit: { type: Boolean, default: false },
    currentPluginType: { type: String, required: true },
    isSchemaDrivenPluginType: { type: Boolean, default: false },
    waifuConfig: { type: Object, required: true },
    themeConfig: { type: Object, required: true },
    paymentConfig: { type: Object, required: true },
    paymentConfigSchema: { type: Object, required: true },
    aiToolForm: { type: Object, required: true },
    aiToolEditorMode: { type: Object, required: true },
    aiToolAssistantPrompt: { type: String, required: true },
    pluginCodeEditorMeta: { type: Object, required: true },
    aiPromptDialog: { type: Object, required: true },
    aiPromptVisible: { type: Boolean, default: false },
    previewImage: { type: String, default: '' }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible;
      },
      set(value) {
        this.$emit('update:visible', value);
      }
    },
    showBaseJsonConfig() {
      return this.currentPluginType !== 'waifu_model'
        && this.currentPluginType !== 'article_theme'
        && this.currentPluginType !== 'ai_tool'
        && !this.isSchemaDrivenPluginType;
    },
    showEffectCodeEditor() {
      return this.currentPluginType !== 'waifu_model'
        && this.currentPluginType !== 'article_theme'
        && this.currentPluginType !== 'ai_tool'
        && !this.isSchemaDrivenPluginType;
    }
  },
  methods: {
    validateAndSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.$emit('save');
        }
      });
    },
    clearValidate() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate();
      }
    }
  }
}
</script>
