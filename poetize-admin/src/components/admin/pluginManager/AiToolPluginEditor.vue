<!-- eslint-disable vue/no-mutating-props -->
<template>
  <div>
    <div class="ai-tool-guide-card">
      <div class="ai-tool-guide-header">
        <div>
          <div class="ai-tool-guide-title">先看这里：新手建议先用 AI 帮你生成配置</div>
          <div class="ai-tool-guide-subtitle">如果你手上还没有整理好的接口参数，先复制下面的提示词给外部 AI，再回来照着填。</div>
        </div>
        <el-button type="primary" size="small" @click="copyAiToolPrompt">复制AI配置提示词</el-button>
      </div>
      <div class="ai-tool-guide-steps">
        <div class="ai-tool-guide-step"><span>1</span> 把提示词发给外部 AI</div>
        <div class="ai-tool-guide-step"><span>2</span> 让它先问你工具用途和接口资料</div>
        <div class="ai-tool-guide-step"><span>3</span> 再让它按后台输入框逐项给出填写结果</div>
      </div>
      <div class="ai-tool-prompt-box is-primary">
        <pre>{{ assistantPrompt }}</pre>
      </div>
    </div>

    <el-divider content-position="left">再填写 HTTP 工具配置</el-divider>
    <el-alert title="通过配置直接生成可调用工具" type="info" :closable="false" show-icon style="margin-bottom: 15px;">
      <div>模板变量支持 <code v-pre>{{args.xxx}}</code> 和 <code v-pre>{{config.xxx}}</code>。</div>
      <div>例如：<code v-pre>{"q":"{{args.query}}","api_key":"{{config.apiKey}}"}</code></div>
    </el-alert>

    <el-form-item label="工具参数Schema">
      <el-input type="textarea" :rows="8" v-model="aiToolForm.inputSchemaText"></el-input>
      <div class="sub-title">填写 JSON Schema，定义模型可传入的工具参数</div>
    </el-form-item>
    <el-form-item label="配置Schema">
      <el-input type="textarea" :rows="8" v-model="aiToolForm.configSchemaText"></el-input>
      <div class="sub-title">定义后台可编辑的配置结构，例如 apiKey、engine、region</div>
    </el-form-item>
    <el-form-item label="HTTP方法">
      <el-select v-model="aiToolForm.method">
        <el-option label="GET" value="GET"></el-option>
        <el-option label="POST" value="POST"></el-option>
        <el-option label="PUT" value="PUT"></el-option>
        <el-option label="DELETE" value="DELETE"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="请求地址">
      <el-input v-model="aiToolForm.url" placeholder="例如: https://serpapi.com/search.json"></el-input>
    </el-form-item>

    <el-form-item label="请求头模板">
      <template v-if="aiToolEditorMode.headers === 'json'">
        <el-input type="textarea" :rows="4" v-model="aiToolForm.headersText" :placeholder="getAiToolJsonPlaceholder('headers')"></el-input>
        <div class="sub-title">可直接粘贴 JSON，请保持对象格式。</div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" plain @click="setAiToolEditorMode('headers', 'kv')">返回键值对</el-button>
        </div>
      </template>
      <div v-else class="ai-tool-kv-list">
        <div class="ai-tool-kv-row" v-for="(item, index) in aiToolForm.headersList" :key="`header-${index}`">
          <el-input v-model="item.key" placeholder="Header名，例如 Authorization"></el-input>
          <el-input v-model="item.value" :placeholder="'值，例如 Bearer {{config.apiKey}}'"></el-input>
          <el-button type="text" class="red-text" @click="removeAiToolKvRow('headersList', index)">删除</el-button>
        </div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" @click="addAiToolKvRow('headersList')">新增Header</el-button>
          <el-button size="mini" plain @click="setAiToolEditorMode('headers', 'json')">转JSON输入</el-button>
        </div>
      </div>
    </el-form-item>

    <el-form-item label="Query模板">
      <template v-if="aiToolEditorMode.query === 'json'">
        <el-input type="textarea" :rows="4" v-model="aiToolForm.queryText" :placeholder="getAiToolJsonPlaceholder('query')"></el-input>
        <div class="sub-title">可直接粘贴 JSON，请保持对象格式。</div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" plain @click="setAiToolEditorMode('query', 'kv')">返回键值对</el-button>
        </div>
      </template>
      <div v-else class="ai-tool-kv-list">
        <div class="ai-tool-kv-row" v-for="(item, index) in aiToolForm.queryList" :key="`query-${index}`">
          <el-input v-model="item.key" placeholder="参数名，例如 q"></el-input>
          <el-input v-model="item.value" :placeholder="'值，例如 {{args.query}}'"></el-input>
          <el-button type="text" class="red-text" @click="removeAiToolKvRow('queryList', index)">删除</el-button>
        </div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" @click="addAiToolKvRow('queryList')">新增Query参数</el-button>
          <el-button size="mini" plain @click="setAiToolEditorMode('query', 'json')">转JSON输入</el-button>
        </div>
      </div>
    </el-form-item>

    <el-form-item v-if="!['GET', 'DELETE'].includes(aiToolForm.method)" label="Body模板">
      <template v-if="aiToolEditorMode.body === 'json'">
        <el-input type="textarea" :rows="6" v-model="aiToolForm.bodyText" :placeholder="getAiToolJsonPlaceholder('body')"></el-input>
        <div class="sub-title">可直接粘贴 JSON，请保持对象格式。</div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" plain @click="setAiToolEditorMode('body', 'kv')">返回键值对</el-button>
        </div>
      </template>
      <div v-else class="ai-tool-kv-list">
        <div class="ai-tool-kv-row" v-for="(item, index) in aiToolForm.bodyList" :key="`body-${index}`">
          <el-input v-model="item.key" placeholder="字段名，例如 query"></el-input>
          <el-input v-model="item.value" :placeholder="'值，例如 {{args.query}}'"></el-input>
          <el-button type="text" class="red-text" @click="removeAiToolKvRow('bodyList', index)">删除</el-button>
        </div>
        <div class="ai-tool-inline-actions">
          <el-button size="mini" @click="addAiToolKvRow('bodyList')">新增Body字段</el-button>
          <el-button size="mini" plain @click="setAiToolEditorMode('body', 'json')">转JSON输入</el-button>
        </div>
      </div>
    </el-form-item>

    <el-form-item label="结果路径">
      <el-input v-model="aiToolForm.responsePath" placeholder="例如: data.items 或 organic_results"></el-input>
    </el-form-item>
    <el-form-item label="超时时间">
      <el-input-number v-model="aiToolForm.timeoutMs" :min="1000" :max="60000" :step="500"></el-input-number>
    </el-form-item>
    <el-form-item label="工具配置">
      <el-input type="textarea" :rows="6" v-model="aiToolForm.pluginConfigText"></el-input>
      <div class="sub-title">这里填写实际运行配置，比如 API Key、默认引擎、地区参数。敏感字段为空时，表示你还需要手动填入自己的真实密钥。</div>
    </el-form-item>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import {
  AI_TOOL_ASSISTANT_PROMPT,
  getAiToolJsonPlaceholder,
  getAiToolSectionMeta,
  kvListToObject,
  objectToKvList,
  parseJsonText
} from '@/components/admin/pluginManager/pluginManagerTransforms';

export default {
  name: 'AiToolPluginEditor',
  props: {
    aiToolForm: { type: Object, required: true },
    aiToolEditorMode: { type: Object, required: true },
    assistantPrompt: { type: String, default: AI_TOOL_ASSISTANT_PROMPT }
  },
  methods: {
    getAiToolJsonPlaceholder,
    addAiToolKvRow(field) {
      this.aiToolForm[field].push({ key: '', value: '' });
    },
    removeAiToolKvRow(field, index) {
      this.aiToolForm[field].splice(index, 1);
    },
    syncAiToolJsonFromKv(section) {
      const meta = getAiToolSectionMeta(section);
      const objectValue = kvListToObject(this.aiToolForm[meta.listField]);
      this.aiToolForm[meta.textField] = JSON.stringify(objectValue, null, 2);
    },
    setAiToolEditorMode(section, mode) {
      if (mode === 'json') {
        this.syncAiToolJsonFromKv(section);
      } else {
        const meta = getAiToolSectionMeta(section);
        const parsed = parseJsonText(this.aiToolForm[meta.textField], meta.label, {});
        this.aiToolForm[meta.textField] = JSON.stringify(parsed, null, 2);
        this.aiToolForm[meta.listField] = objectToKvList(parsed);
      }
      this.aiToolEditorMode[section] = mode;
    },
    copyAiToolPrompt() {
      navigator.clipboard.writeText(this.assistantPrompt).then(() => {
        this.$message.success('AI提示词已复制到剪贴板');
      }).catch(() => {
        this.$message.error('复制失败，请手动复制');
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
.red-text {
  color: #ff0000;
}
.ai-tool-guide-card {
  margin-bottom: 18px;
  padding: 18px;
  border: 1px solid #d9ecff;
  border-radius: 10px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef7ff 100%);
}
.ai-tool-guide-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}
.ai-tool-guide-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.ai-tool-guide-subtitle {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}
.ai-tool-guide-steps {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.ai-tool-guide-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
  font-size: 12px;
}
.ai-tool-guide-step span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}
.ai-tool-kv-list {
  width: 100%;
}
.ai-tool-kv-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.ai-tool-kv-row .el-input {
  flex: 1;
}
.ai-tool-prompt-box {
  max-height: 260px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
.ai-tool-prompt-box.is-primary {
  border-color: #c6e2ff;
  background: rgba(255, 255, 255, 0.9);
}
.ai-tool-prompt-box pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-size: 12px;
  color: #606266;
}
.ai-tool-inline-actions {
  margin-top: 10px;
}
@media (max-width: 900px) {
  .ai-tool-guide-header {
    flex-direction: column;
  }
  .ai-tool-kv-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
