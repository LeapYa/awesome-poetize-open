<template>
  <div class="ai-model-config">
    <el-form :model="modelConfig" label-width="120px">
      <el-form-item label="AI服务商">
        <el-select 
          v-model="modelConfig.provider" 
          placeholder="请选择AI服务商" 
          @change="onProviderChange">
          <el-option label="OpenAI" value="openai"></el-option>
          <el-option label="Claude (Anthropic)" value="anthropic"></el-option>
          <el-option label="DeepSeek" value="deepseek"></el-option>
          <el-option label="硅基流动" value="siliconflow"></el-option>
          <el-option label="自定义API" value="custom"></el-option>
        </el-select>
        <small class="help-text">其他服务商（如通义千问、文心一言等）请使用"自定义API"选项</small>
      </el-form-item>

      <el-form-item label="API密钥">
        <el-input 
          v-model="modelConfig.apiKey" 
          type="password" 
          show-password
          placeholder="请输入API密钥"
          @input="onApiKeyInput">
        </el-input>
        <div v-if="isApiKeyMasked" class="api-key-status">
          <i class="el-icon-success"></i>
          <span>密钥已保存（出于安全考虑部分隐藏）</span>
          <el-button type="text" size="small" @click="showFullApiKey" v-if="!showingFullKey">重新输入密钥</el-button>
        </div>
        <div v-else class="help-text" style="margin-top: 5px;">
          API密钥保存后会自动隐藏敏感信息，这是正常的安全保护措施
        </div>
      </el-form-item>

      <el-form-item label="模型名称">
        <el-select 
          v-model="modelConfig.model" 
          placeholder="请输入模型名称（如：gpt-5、claude-3-5-sonnet-20241022、deepseek-chat等）" 
          filterable 
          allow-create
          class="custom-model-select">
          <el-option 
            v-for="model in availableModels" 
            :key="model.value" 
            :label="model.label" 
            :value="model.value">
          </el-option>
        </el-select>
        <small class="help-text">
          支持任何模型名称，请根据您选择的服务商输入对应的模型标识符
        </small>
        <small class="help-text thinking-hint" v-if="isThinkingModelSelected">
          此模型支持思考模式，可在高级设置中启用以获得更深入的分析
        </small>
      </el-form-item>

      <el-form-item label="API基础URL" v-if="!['openai', 'anthropic'].includes(modelConfig.provider)">
        <el-input 
          v-model="modelConfig.baseUrl" 
          placeholder="例如: https://api.example.com/v1">
        </el-input>
      </el-form-item>

      <el-form-item label="温度参数">
        <el-slider 
          v-model="modelConfig.temperature" 
          :min="0" 
          :max="2" 
          :step="0.1"
          show-tooltip>
        </el-slider>
        <small class="help-text">控制回复的随机性，0表示最确定，2表示最随机</small>
      </el-form-item>

      <el-form-item label="最大令牌数">
        <el-input-number 
          v-model="modelConfig.maxTokens" 
          :min="100" 
          :max="8000" 
          :step="100">
        </el-input-number>
        <small class="help-text">单次回复的最大长度</small>
      </el-form-item>

      <el-form-item label="Top P">
        <el-slider 
          v-model="modelConfig.topP" 
          :min="0" 
          :max="1" 
          :step="0.01"
          show-tooltip>
        </el-slider>
        <small class="help-text">核采样参数，控制输出多样性（0-1），默认1.0</small>
      </el-form-item>

      <el-form-item label="频率惩罚">
        <el-slider 
          v-model="modelConfig.frequencyPenalty" 
          :min="0" 
          :max="2" 
          :step="0.1"
          show-tooltip>
        </el-slider>
        <small class="help-text">降低重复词汇的频率（0-2），默认0</small>
      </el-form-item>

      <el-form-item label="存在惩罚">
        <el-slider 
          v-model="modelConfig.presencePenalty" 
          :min="0" 
          :max="2" 
          :step="0.1"
          show-tooltip>
        </el-slider>
        <small class="help-text">鼓励谈论新话题（0-2），默认0</small>
      </el-form-item>

      <el-form-item label="启用AI聊天">
        <el-switch v-model="modelConfig.enabled"></el-switch>
      </el-form-item>

      <el-form-item label="启用流式响应">
        <el-switch v-model="modelConfig.enableStreaming"></el-switch>
        <small class="help-text">启用后AI回复将实时显示，提供更流畅的对话体验，包括工具调用过程可视化</small>
      </el-form-item>

      <el-form-item label="连接测试">
        <el-button @click="testConnection" :loading="testing">测试连接</el-button>
        <span v-if="isApiKeyMasked" class="help-text" style="margin-left: 10px;">
          将使用已保存的配置进行测试
        </span>
        <span v-else class="help-text" style="margin-left: 10px;">
          将使用当前输入的配置进行测试
        </span>
        <span v-if="testResult" :class="testResult.success ? 'test-success' : 'test-error'">
          {{ testResult.message }}
        </span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'AiModelConfig',
  props: {
    value: {
      type: Object,
      default: () => ({
        provider: 'openai',
        apiKey: '',
        model: 'gpt-3.5-turbo',
        baseUrl: '',
        temperature: 0.7,
        maxTokens: 1000,
        topP: 1.0,
        frequencyPenalty: 0,
        presencePenalty: 0,
        enabled: false,
        enableStreaming: false
      })
    }
  },
  
  data() {
    return {
      modelConfig: { ...this.value },
      testing: false,
      testResult: null,
      isApiKeyMasked: true,
      showingFullKey: false,
      originalMaskedKey: ''
    }
  },
  
  computed: {
    availableModels() {
      // 返回空数组，允许用户自由输入任何模型名称
      return [];
    },
    
    isThinkingModelSelected() {
      const thinkingModels = ['o1-preview', 'o1-mini', 'deepseek-reasoner'];
      return thinkingModels.includes(this.modelConfig.model) || 
             this.modelConfig.model.includes('o1') ||
             this.modelConfig.model.includes('reasoner') ||
             this.modelConfig.model.includes('DeepSeek-R1') ||
             this.modelConfig.model.includes('thinking');
    }
  },
  
  watch: {
    value: {
      handler(newVal) {
        // 避免无限循环：只在值真正变化时更新
        if (JSON.stringify(newVal) !== JSON.stringify(this.modelConfig)) {
          this.modelConfig = { ...newVal };
          this.isApiKeyMasked = this.modelConfig.apiKey && this.modelConfig.apiKey.includes('*');
          this.originalMaskedKey = this.isApiKeyMasked ? this.modelConfig.apiKey : '';
        }
      },
      deep: true
    },
    
    modelConfig: {
      handler(newVal) {
        // 避免无限循环：只在值真正变化时 emit
        if (JSON.stringify(newVal) !== JSON.stringify(this.value)) {
          this.$emit('input', newVal);
        }
      },
      deep: true
    }
  },
  
  methods: {
    onProviderChange() {
      // 清除测试结果
      this.testResult = null;
    },
    
    async testConnection() {
      this.testing = true;
      this.testResult = '';

      try {
        if (this.isApiKeyMasked || (this.modelConfig.apiKey && this.modelConfig.apiKey.includes('*'))) {
          const response = await this.$http.post(this.$constant.baseURL + '/webInfo/ai/config/chat/test', {
            provider: this.modelConfig.provider,
            api_base: this.modelConfig.baseUrl,
            model: this.modelConfig.model,
            use_saved_config: true
          }, true);

          if (response.flag) {
            this.testResult = {
              success: true,
              message: response.message || '连接测试成功（使用已保存的配置）'
            };
            this.$message.success('连接测试成功（使用已保存的配置）');
          } else {
            this.testResult = {
              success: false,
              message: response.message || '连接测试失败'
            };
            this.$message.error('连接测试失败: ' + response.message);
          }
        } else {
          const testData = {
            provider: this.modelConfig.provider,
            api_key: this.modelConfig.apiKey,
            api_base: this.modelConfig.baseUrl,
            model: this.modelConfig.model
          };

          const response = await this.$http.post(this.$constant.baseURL + '/webInfo/ai/config/chat/test', testData, true);

          if (response.flag) {
            this.testResult = {
              success: true,
              message: response.message || '连接测试成功'
            };
            this.$message.success('连接测试成功');
          } else {
            this.testResult = {
              success: false,
              message: response.message || '连接测试失败'
            };
            this.$message.error('连接测试失败: ' + response.message);
          }
        }
      } catch (error) {
        this.testResult = {
          success: false,
          message: error.message
        };
        this.$message.error('连接测试失败: ' + error.message);
      } finally {
        this.testing = false;
      }
    },
    
    onApiKeyInput() {
      if (this.modelConfig.apiKey && !this.modelConfig.apiKey.includes('*')) {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
      }
      if (!this.modelConfig.apiKey) {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
      }
    },

    async showFullApiKey() {
      this.$confirm('要重新输入API密钥吗？当前密钥将被清空。', '重新输入密钥', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
        this.modelConfig.apiKey = '';
        this.$message.info('请重新输入您的API密钥');
      }).catch(() => {
        // 用户取消操作
      });
}
  }
}
</script>

<style scoped>
.ai-model-config {
  max-height: 500px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 10px;
}

/* 移动端对话框中不限制高度 */
@media screen and (max-width: 768px) {
  .ai-model-config {
    max-height: none;
    overflow-y: visible;
  }
}

.help-text {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 5px;
}

.api-key-status {
  margin-top: 5px;
  color: #67c23a;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.thinking-hint {
  color: #e6a23c;
}

.test-success {
  color: #67c23a;
  margin-left: 10px;
  font-size: 12px;
}

.test-error {
  color: #f56c6c;
  margin-left: 10px;
  font-size: 12px;
}

.custom-model-select {
  width: 100%;
}

/* PC端样式 - 768px以上 */
@media screen and (min-width: 769px) {
  ::v-deep .el-form-item__label {
    float: left !important;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .ai-model-config {
    padding: 0;
  }

  .ai-model-config .el-form-item {
    margin-bottom: 15px;
  }

  /* 标签适配 - 垂直布局 */
  .ai-model-config .el-form-item__label {
    float: none !important;
    width: 100% !important;
    text-align: left !important;
    font-size: 13px;
    line-height: 1.4;
    margin-bottom: 8px !important;
    padding-bottom: 0 !important;
  }

  .ai-model-config .el-form-item__content {
    margin-left: 0 !important;
    width: 100% !important;
  }

  /* 帮助文本字号优化 */
  .help-text {
    font-size: 11px;
    line-height: 1.3;
    margin-top: 3px;
  }

  .api-key-status {
    font-size: 11px;
  }

  .test-success,
  .test-error {
    font-size: 11px;
  }

  /* 滑块容器 */
  .ai-model-config .el-slider {
    padding: 0 10px;
  }
}

@media screen and (max-width: 480px) {
  .ai-model-config .el-form-item {
    margin-bottom: 12px;
  }

  .ai-model-config .el-form-item__label {
    font-size: 12px;
  }

  .help-text,
  .api-key-status,
  .test-success,
  .test-error {
    font-size: 10px;
  }
}
</style> 