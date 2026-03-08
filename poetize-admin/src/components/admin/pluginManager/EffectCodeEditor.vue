<!-- eslint-disable vue/no-mutating-props -->
<template>
  <div>
    <el-form-item label="JS代码" prop="pluginCode">
      <el-input
        type="textarea"
        :rows="10"
        v-model="form.pluginCode"
        :placeholder="pluginCodeEditorMeta.placeholder"
        style="font-family: 'Consolas', 'Monaco', monospace;"></el-input>
      <div class="sub-title">
        <span v-html="pluginCodeEditorMeta.description"></span><br/>
        <template v-if="pluginCodeEditorMeta.showAnimeDoc">
          <strong>anime</strong> 为 anime.js 动画库，可用于创建复杂动画效果。
        </template>
        <template v-else>
          建议自行做好重复挂载保护、窗口尺寸变化处理和 DOM 清理逻辑。
        </template>
        <a href="https://animejs.com/documentation/" target="_blank" style="color: #409EFF;">查看anime.js文档</a><br/>
        不会写代码？<a href="javascript:void(0)" @click="$emit('open-ai-prompt')" style="color: #409EFF;">使用AI生成效果代码</a>
      </div>
    </el-form-item>

    <el-dialog :title="aiPromptDialog.title" :visible.sync="dialogVisible" width="600px" custom-class="centered-dialog" append-to-body>
      <p style="margin-bottom: 15px; color: #666;">{{ aiPromptDialog.intro }}</p>
      <pre class="prompt-box">{{ aiPromptDialog.prompt }}</pre>
      <div style="margin-top: 15px;">
        <strong style="color: #409EFF;">{{ aiPromptDialog.examplesTitle }}</strong>
        <p style="margin: 8px 0; color: #909399; font-size: 12px;">{{ aiPromptDialog.examplesHint }}</p>
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <thead>
            <tr style="background: #f5f7fa;">
              <th style="padding: 10px; border: 1px solid #ebeef5; text-align: left; width: 100px;">想要的效果</th>
              <th style="padding: 10px; border: 1px solid #ebeef5; text-align: left;">描述</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="example in aiPromptDialog.examples" :key="example.name">
              <td style="padding: 8px 10px; border: 1px solid #ebeef5;">{{ example.name }}</td>
              <td style="padding: 8px 10px; border: 1px solid #ebeef5;">{{ example.description }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="copyAiPrompt">复制提示词</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
export default {
  name: 'EffectCodeEditor',
  props: {
    form: { type: Object, required: true },
    pluginCodeEditorMeta: { type: Object, required: true },
    aiPromptDialog: { type: Object, required: true },
    aiPromptVisible: { type: Boolean, default: false }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.aiPromptVisible;
      },
      set(value) {
        this.$emit('update:ai-prompt-visible', value);
      }
    }
  },
  methods: {
    copyAiPrompt() {
      navigator.clipboard.writeText(this.aiPromptDialog.prompt).then(() => {
        this.$message.success(this.aiPromptDialog.copySuccessMessage);
      }).catch(() => {
        this.$message.error('复制失败，请手动选择复制');
      });
    }
  }
}
</script>

<style scoped>
.sub-title {
  font-size: 12px;
  color: #999;
  line-height: 20px;
}
.prompt-box {
  white-space: pre-wrap;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.7;
  max-height: 350px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
}
</style>
