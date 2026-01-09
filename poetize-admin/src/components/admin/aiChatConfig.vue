<template>
  <div class="ai-chat-management">
    <!-- 页面标题 -->
    <div style="margin-bottom: 30px;">
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        AI聊天配置
      </el-tag>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- AI模型配置 -->
      <el-tab-pane label="AI模型配置" name="model">
        <el-card class="config-card">
          <div slot="header" class="card-header">
            <span>AI模型设置</span>
            <el-button type="primary" @click="saveModelConfig" :loading="saving">保存配置</el-button>
          </div>
          
          <el-form :model="modelConfig" label-width="120px">
            <el-form-item label="AI服务商">
              <el-select 
                v-model="modelConfig.provider" 
                placeholder="请选择AI服务商" 
                @change="onProviderChange">
                <el-option label="OpenAI" value="openai"></el-option>
                <el-option label="Claude (Anthropic)" value="anthropic"></el-option>
                <el-option label="DeepSeek" value="deepseek"></el-option>
                <el-option label="硅基流动 SiliconFlow" value="siliconflow"></el-option>
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
                💡 API密钥保存后会自动隐藏敏感信息，这是正常的安全保护措施
              </div>
            </el-form-item>

            <el-form-item label="模型名称">
              <el-select 
                v-model="modelConfig.model" 
                :placeholder="getModelPlaceholder" 
                filterable 
                allow-create
                :class="{'custom-model-select': modelConfig.provider === 'custom'}">
                <el-option 
                  v-for="model in availableModels" 
                  :key="model.value" 
                  :label="model.label" 
                  :value="model.value">
                </el-option>
              </el-select>
              <small class="help-text" v-if="modelConfig.provider === 'siliconflow'">
                💡 支持自定义：可选择预设模型或直接输入硅基流动平台任意可用模型名称
              </small>
              <small class="help-text" v-else-if="modelConfig.provider === 'custom'">
                💡 自定义API：请输入您的模型名称，支持任何兼容OpenAI格式的模型
              </small>
              <small class="help-text" v-else>
                根据所选服务商自动显示可用模型
              </small>
              <small class="help-text thinking-hint" v-if="isThinkingModelSelected">
                🧠 此模型支持思考模式，可在高级设置中启用以获得更深入的分析
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

            <el-form-item label="启用AI聊天">
              <el-switch v-model="modelConfig.enabled"></el-switch>
            </el-form-item>

            <el-form-item label="启用流式响应">
              <el-switch v-model="modelConfig.enableStreaming"></el-switch>
              <small class="help-text">启用后AI回复将实时显示，提供更流畅的对话体验，包括工具调用过程可视化</small>
            </el-form-item>

            <el-form-item label="启用思考模式">
              <el-switch v-model="modelConfig.enableThinking"></el-switch>
              <small class="help-text">启用后AI会先思考再回答，提供更深入的分析（仅部分模型支持，如o1系列）</small>
            </el-form-item>

            <el-form-item label="启用工具调用">
              <el-switch v-model="modelConfig.enableTools"></el-switch>
              <small class="help-text">启用后AI可以调用MCP工具（如搜索、计算等），提供更强大的功能。关闭后AI仅进行纯对话</small>
            </el-form-item>

            <el-form-item label="连接测试">
              <el-button @click="testConnection" :loading="testing">测试连接</el-button>
              <span v-if="isApiKeyMasked" class="help-text" style="margin-left: 10px;">
                🔒 将使用已保存的配置进行测试
              </span>
              <span v-else class="help-text" style="margin-left: 10px;">
                🔧 将使用当前输入的配置进行测试
              </span>
              <span v-if="testResult" :class="testResult.success ? 'test-success' : 'test-error'">
                {{ testResult.message }}
              </span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 聊天设置 -->
      <el-tab-pane label="聊天设置" name="chat">
        <el-card class="config-card">
          <div slot="header" class="card-header">
            <span>聊天功能设置</span>
            <el-button type="primary" @click="saveChatConfig" :loading="saving">保存配置</el-button>
          </div>
          
          <el-form :model="chatConfig" label-width="120px">
            <el-form-item label="系统提示词">
              <el-input 
                v-model="chatConfig.systemPrompt" 
                type="textarea" 
                :rows="4"
                placeholder="请输入AI的角色设定和行为指导">
              </el-input>
              <small class="help-text">定义AI的角色和回复风格</small>
            </el-form-item>

            <el-form-item label="欢迎消息">
              <el-input 
                v-model="chatConfig.welcomeMessage" 
                type="textarea" 
                :rows="2"
                placeholder="AI首次对话时的欢迎消息">
              </el-input>
            </el-form-item>

            <el-form-item label="对话历史数">
              <el-input-number 
                v-model="chatConfig.historyCount" 
                :min="0" 
                :max="20">
              </el-input-number>
              <small class="help-text">保留多少条历史对话用于上下文理解</small>
            </el-form-item>

            <el-form-item label="速率限制">
              <el-input-number 
                v-model="chatConfig.rateLimit" 
                :min="1" 
                :max="100"
                :precision="0">
              </el-input-number>
              <small class="help-text">每分钟最多允许的消息数量</small>
            </el-form-item>

            <el-form-item label="最大消息长度">
              <el-input-number 
                v-model="chatConfig.maxMessageLength" 
                :min="100" 
                :max="2000"
                :precision="0">
              </el-input-number>
              <small class="help-text">用户单条消息的最大字符数限制</small>
            </el-form-item>

            <el-form-item label="需要登录">
              <el-switch v-model="chatConfig.requireLogin"></el-switch>
              <small class="help-text">是否需要用户登录后才能使用AI聊天</small>
            </el-form-item>

            <el-form-item label="保存聊天记录">
              <el-switch v-model="chatConfig.saveHistory"></el-switch>
              <small class="help-text">是否保存用户的聊天历史记录</small>
            </el-form-item>

            <el-form-item label="内容过滤">
              <el-switch v-model="chatConfig.contentFilter"></el-switch>
              <small class="help-text">启用内容安全过滤</small>
            </el-form-item>

            <!-- 记忆管理配置 -->
            <el-divider content-position="left">
              <i class="el-icon-data-analysis"></i> 记忆管理设置
            </el-divider>

            <el-form-item label="启用记忆功能">
              <el-switch v-model="chatConfig.enableMemory"></el-switch>
              <small class="help-text">使用 Mem0 服务为 AI 提供长期记忆能力</small>
            </el-form-item>

            <el-form-item label="Mem0 API密钥" v-if="chatConfig.enableMemory">
              <el-input 
                v-model="chatConfig.mem0ApiKey" 
                type="password" 
                show-password
                placeholder="输入新密钥以更新，留空保持原值"
                @input="onMem0ApiKeyInput">
                <template slot="append">
                  <el-button @click="testMem0Connection" :loading="testingMem0" type="primary">
                    测试连接
                  </el-button>
                </template>
              </el-input>
              <div v-if="isMem0KeyMasked" class="api-key-status">
                <i class="el-icon-success"></i>
                <span>Mem0 密钥已保存（部分隐藏）</span>
                <el-button type="text" size="small" @click="showFullMem0Key" v-if="!showingFullMem0Key">重新输入密钥</el-button>
              </div>
              <div v-else class="help-text" style="margin-top: 5px;">
                💡 免费版每月 1000 次调用。
                <a href="https://mem0.dev/pd-api" target="_blank" style="color: #409EFF;">获取 API 密钥</a>
              </div>
              <span v-if="mem0TestResult" :class="mem0TestResult.success ? 'test-success' : 'test-error'">
                {{ mem0TestResult.message }}
              </span>
            </el-form-item>

            <el-form-item label="自动保存记忆" v-if="chatConfig.enableMemory">
              <el-switch v-model="chatConfig.memoryAutoSave"></el-switch>
              <small class="help-text">每次对话后自动保存到 Mem0</small>
            </el-form-item>

            <el-form-item label="自动检索记忆" v-if="chatConfig.enableMemory">
              <el-switch v-model="chatConfig.memoryAutoRecall"></el-switch>
              <small class="help-text">根据用户消息自动检索相关记忆</small>
            </el-form-item>

            <el-form-item label="记忆检索数量" v-if="chatConfig.enableMemory && chatConfig.memoryAutoRecall">
              <el-slider 
                v-model="chatConfig.memoryRecallLimit" 
                :min="1" 
                :max="10" 
                :step="1"
                show-stops
                :marks="{ 1: '1', 3: '3', 5: '5', 10: '10' }">
              </el-slider>
              <small class="help-text">每次最多检索 {{ chatConfig.memoryRecallLimit }} 条相关记忆（建议 3-5 条）</small>
            </el-form-item>

            <el-alert 
              v-if="chatConfig.enableMemory"
              title="用量提醒"
              type="info"
              :closable="false"
              style="margin-bottom: 15px;">
              <template>
                <p style="margin: 0; font-size: 13px;">
                  免费版每月 1000 次 API 调用。每次对话消耗：
                  <br>
                  • 保存记忆：1 次调用
                  <br>
                  • 检索记忆：1 次调用
                  <br>
                  如果同时开启保存和检索，每次对话约消耗 2 次调用。
                </p>
              </template>
            </el-alert>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 外观设置 -->
      <el-tab-pane label="外观设置" name="appearance">
        <el-card class="config-card">
          <div slot="header" class="card-header">
            <span>聊天界面外观</span>
            <el-button type="primary" @click="saveAppearanceConfig" :loading="saving">保存配置</el-button>
          </div>
          
          <el-form :model="appearanceConfig" label-width="120px">
            <el-form-item label="机器人头像">
              <div class="avatar-upload">
                <el-upload
                  class="avatar-uploader"
                  :action="uploadUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload">
                  <img v-if="appearanceConfig.botAvatar" :src="appearanceConfig.botAvatar" class="avatar">
                  <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                </el-upload>
              </div>
            </el-form-item>

            <el-form-item label="机器人名称">
              <el-input v-model="appearanceConfig.botName" placeholder="例如: 小助手"></el-input>
            </el-form-item>

            <el-form-item label="主题颜色">
              <el-color-picker v-model="appearanceConfig.themeColor"></el-color-picker>
            </el-form-item>

            <el-form-item label="聊天窗口位置">
              <el-radio-group v-model="appearanceConfig.position">
                <el-radio label="bottom-right">右下角</el-radio>
                <el-radio label="bottom-left">左下角</el-radio>
                <el-radio label="center">居中</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="聊天气泡样式">
              <el-radio-group v-model="appearanceConfig.bubbleStyle">
                <el-radio label="modern">现代风格</el-radio>
                <el-radio label="classic">经典风格</el-radio>
                <el-radio label="minimal">简约风格</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="显示打字动效">
              <el-switch v-model="appearanceConfig.typingAnimation"></el-switch>
            </el-form-item>

            <el-form-item label="显示时间戳">
              <el-switch v-model="appearanceConfig.showTimestamp"></el-switch>
            </el-form-item>

            <el-form-item label="配置管理">
              <el-button @click="exportConfig">导出配置</el-button>
              <el-button @click="showImportDialog">导入配置</el-button>
              <small class="help-text">可将所有配置导出为JSON文件，或从文件导入配置</small>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 导入配置对话框 -->
    <el-dialog title="导入配置" :visible.sync="importDialogVisible" width="500px" custom-class="centered-dialog">
      <el-upload
        drag
        :action="uploadUrl"
        :before-upload="beforeConfigUpload"
        :on-success="handleConfigImport"
        accept=".json">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将配置文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">只能上传json格式的配置文件</div>
      </el-upload>
    </el-dialog>

    <!-- 聊天测试面板 -->
    <el-drawer
      title="聊天测试"
      :visible.sync="testChatVisible"
      direction="rtl"
      size="400px">
      <div class="chat-test-panel">
        <div class="chat-messages" ref="chatMessages">
          <div v-for="(message, index) in testMessages" :key="index" :class="['message', message.type]">
            <div class="message-content">{{ message.content }}</div>
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input 
            v-model="testInput" 
            @keyup.enter="sendTestMessage"
            placeholder="输入测试消息..."
            :disabled="testSending">
          </el-input>
          <el-button @click="sendTestMessage" :loading="testSending">发送</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 浮动测试按钮 -->
    <el-button 
      type="primary" 
      class="test-chat-btn"
      @click="testChatVisible = true"
      v-if="modelConfig.enabled">
      <i class="el-icon-chat-line-round"></i>
      测试聊天
    </el-button>
  </div>
</template>

<script>
export default {
  name: 'AiChatConfig',
  data() {
    return {
      activeTab: 'model',
      saving: false,
      testing: false,
      testResult: null,
      importDialogVisible: false,
      testChatVisible: false,
      testSending: false,
      testInput: '',
      testMessages: [],
      
      // AI模型配置
      modelConfig: {
        provider: 'openai',
        apiKey: '',
        model: 'gpt-3.5-turbo',
        baseUrl: '',
        temperature: 0.7,
        maxTokens: 1000,
        enabled: false,
        enableStreaming: false,
        enableThinking: false,
        enableTools: true  // 默认启用工具调用
      },
      
      // 聊天设置
      chatConfig: {
        systemPrompt: "AI assistant. Respond in Chinese naturally.",
        welcomeMessage: "你好！有什么可以帮助你的吗？",
        historyCount: 10,
        rateLimit: 20,
        requireLogin: false,
        saveHistory: true,
        contentFilter: true,
        maxMessageLength: 500,
        // 记忆管理设置
        enableMemory: false,
        mem0ApiKey: '',
        memoryAutoSave: true,
        memoryAutoRecall: true,
        memoryRecallLimit: 3
      },
      
      // 外观设置
      appearanceConfig: {
        botAvatar: '',
        botName: 'AI助手',
        themeColor: '#409EFF',
        position: 'bottom-right',
        bubbleStyle: 'modern',
        typingAnimation: true,
        showTimestamp: true
      },
      
      isApiKeyMasked: true,
      showingFullKey: false,
      originalMaskedKey: '',
      
      // Mem0 API密钥状态
      isMem0KeyMasked: false,
      showingFullMem0Key: false,
      originalMaskedMem0Key: '',
      testingMem0: false,
      mem0TestResult: null
    }
  },
  
  computed: {
    uploadUrl() {
      return this.$constant.baseURL + "/admin/upload";
    },
    
    uploadHeaders() {
      return {
        'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
      };
    },
    
    availableModels() {
      const modelMap = {
        openai: [
          { label: 'GPT-5', value: 'gpt-5' },
          { label: 'GPT-5 Codex', value: 'gpt-5-codex' },
          { label: 'GPT-5 Nano', value: 'gpt-5-nano' },
          { label: 'GPT-4.1', value: 'gpt-4.1' },
          { label: 'GPT-4o', value: 'gpt-4o' },
          { label: 'GPT-4o Mini', value: 'gpt-4o-mini' },
          { label: 'GPT-4 Turbo', value: 'gpt-4-turbo' },
          { label: 'GPT-4', value: 'gpt-4' },
          { label: 'o1-preview (思考模式)', value: 'o1-preview' },
          { label: 'o1-mini (思考模式)', value: 'o1-mini' },
          { label: 'GPT-3.5 Turbo', value: 'gpt-3.5-turbo' }
        ],
        anthropic: [
          { label: 'Claude Sonnet 4.5 (最新)', value: 'claude-sonnet-4-5-20250929' },
          { label: 'Claude Haiku 4.5 (快速)', value: 'claude-haiku-4-5-20251015' },
          { label: 'Claude Opus 4.1 (强大)', value: 'claude-opus-4-1-20250805' },
          { label: 'Claude Sonnet 4', value: 'claude-sonnet-4-20250514' },
          { label: 'Claude-3.5 Sonnet', value: 'claude-3-5-sonnet-20241022' },
          { label: 'Claude-3.5 Haiku', value: 'claude-3-5-haiku-20241022' },
          { label: 'Claude-3 Opus', value: 'claude-3-opus-20240229' },
          { label: 'Claude-3 Sonnet', value: 'claude-3-sonnet-20240229' },
          { label: 'Claude-3 Haiku', value: 'claude-3-haiku-20240307' }
        ],
        deepseek: [
          { label: 'DeepSeek Chat', value: 'deepseek-chat' },
          { label: 'DeepSeek Reasoner (思考模式)', value: 'deepseek-reasoner' }
        ],
        siliconflow: [
          { label: 'DeepSeek-R1 (推理模型)', value: 'deepseek-ai/DeepSeek-R1' },
          { label: 'DeepSeek-V3', value: 'deepseek-ai/DeepSeek-V3' },
          { label: 'DeepSeek-V3.2-Exp', value: 'deepseek-ai/DeepSeek-V3.2-Exp' },
          { label: 'Qwen2.5-72B-Instruct', value: 'Qwen/Qwen2.5-72B-Instruct' },
          { label: 'Qwen2.5-32B-Instruct', value: 'Qwen/Qwen2.5-32B-Instruct' },
          { label: 'GLM-4.6', value: 'zai-org/GLM-4.6' },
          { label: 'Kimi-K2-Instruct', value: 'Pro/moonshotai/Kimi-K2-Instruct-0905' },
          { label: 'Meta-Llama-3.1-70B', value: 'meta-llama/Meta-Llama-3.1-70B-Instruct' }
        ],
        custom: [
          { label: 'GPT-3.5 Turbo (OpenAI兼容)', value: 'gpt-3.5-turbo' },
          { label: 'GPT-4 (OpenAI兼容)', value: 'gpt-4' },
          { label: 'GPT-4o (OpenAI兼容)', value: 'gpt-4o' },
          { label: 'DeepSeek Chat', value: 'deepseek-chat' },
          { label: 'Qwen2.5-72B (ModelScope)', value: 'Qwen/Qwen2.5-72B-Instruct' },
          { label: 'Qwen Turbo (阿里云)', value: 'qwen-turbo' },
          { label: 'Qwen Plus (阿里云)', value: 'qwen-plus' },
          { label: 'GLM-4 (智谱)', value: 'glm-4' },
          { label: 'ERNIE-Bot (百度)', value: 'ernie-bot' },
          { label: '自定义模型', value: 'custom-model' }
        ]
      };
      return modelMap[this.modelConfig.provider] || [];
    },
    
    isThinkingModelSelected() {
      // 检查是否选择了支持思考模式的模型
      const thinkingModels = ['o1-preview', 'o1-mini', 'deepseek-reasoner'];
      return thinkingModels.includes(this.modelConfig.model) || 
             this.modelConfig.model.includes('o1') ||
             this.modelConfig.model.includes('reasoner') ||
             this.modelConfig.model.includes('DeepSeek-R1') ||
             this.modelConfig.model.includes('thinking');
    },
    
    getModelPlaceholder() {
      if (this.modelConfig.provider === 'siliconflow') {
        return '选择预设模型或直接输入任意模型名称';
      } else if (this.modelConfig.provider === 'custom') {
        return '请输入自定义模型名称';
      }
      return '请选择模型';
    }
  },
  
  created() {
    // 移除此处的 loadConfigs 调用，统一在 mounted 中加载
  },
  
  methods: {
    // 加载所有配置
    async loadConfigs() {
      try {
        const response = await this.$http.get(this.$constant.baseURL + "/webInfo/ai/config/chat/get", {}, true);
        if (response.code === 200 && response.data) {
          // 从后端API获取配置数据（Java驼峰格式）
          const config = response.data;
          
          // 映射基础配置
          this.modelConfig.provider = config.provider || 'openai';
          this.modelConfig.apiKey = config.apiKey || '';
          this.modelConfig.model = config.model || 'gpt-3.5-turbo';
          this.modelConfig.baseUrl = config.apiBase || '';
          this.modelConfig.temperature = config.temperature || 0.7;
          this.modelConfig.maxTokens = config.maxTokens || 1000;
          this.modelConfig.enabled = config.enabled || false;
          this.modelConfig.enableStreaming = config.enableStreaming || false;
          
          // 检查API密钥是否被隐藏（包含星号表示已保存但被隐藏）
          this.isApiKeyMasked = this.modelConfig.apiKey && this.modelConfig.apiKey.includes('*');
          this.originalMaskedKey = this.isApiKeyMasked ? this.modelConfig.apiKey : '';
          
          // 映射聊天配置（Java驼峰格式）
          this.chatConfig = {
            systemPrompt: config.customInstructions || "AI assistant. Respond in Chinese naturally.",
            welcomeMessage: config.welcomeMessage || "你好！有什么可以帮助你的吗？",
            historyCount: config.maxConversationLength || 10,
            rateLimit: config.rateLimit || 20,
            requireLogin: config.requireLogin || false,
            saveHistory: config.enableChatHistory !== false,
            contentFilter: config.enableContentFilter !== false,
            maxMessageLength: config.maxMessageLength || 500,
            // 记忆管理配置
            enableMemory: config.enableMemory || false,
            mem0ApiKey: config.mem0ApiKey || '',
            memoryAutoSave: config.memoryAutoSave !== false,
            memoryAutoRecall: config.memoryAutoRecall !== false,
            memoryRecallLimit: config.memoryRecallLimit || 3
          };
          
          // 检查 Mem0 API密钥是否被隐藏
          this.isMem0KeyMasked = this.chatConfig.mem0ApiKey && this.chatConfig.mem0ApiKey.includes('*');
          this.originalMaskedMem0Key = this.isMem0KeyMasked ? this.chatConfig.mem0ApiKey : '';
          
          // 映射外观配置
          this.appearanceConfig.botAvatar = config.chatAvatar || '';
          this.appearanceConfig.botName = config.chatName || 'AI助手';
          this.appearanceConfig.themeColor = config.themeColor || '#409EFF';
          this.appearanceConfig.typingAnimation = config.enableTypingIndicator || true;
          this.appearanceConfig.showTimestamp = true; // 固定值
          
          // 映射思考模式和工具调用配置
          this.modelConfig.enableThinking = config.enableThinking || false;
          this.modelConfig.enableTools = config.enableTools !== false; // 默认为true
        }
      } catch (error) {
        console.error('加载AI配置失败:', error);
        this.$message.error('加载AI配置失败: ' + (error.message || '未知错误'));
      }
    },
    
    // 服务商变更处理
    onProviderChange(newProvider) {
      // 防止重复触发
      if (this._providerChanging) {
        return;
      }
      this._providerChanging = true;
      
      try {
        const models = this.availableModels;
        if (models.length > 0) {
          // 如果是自定义API且当前已有模型名称，保持不变
          if (this.modelConfig.provider === 'custom' && this.modelConfig.model) {
            // 保持现有模型名称不变
          } else {
            // 其他情况选择第一个可用模型
            this.modelConfig.model = models[0].value;
          }
        } else if (this.modelConfig.provider === 'custom') {
          // 如果是自定义API但没有预设模型，设置一个常用的默认值
          if (!this.modelConfig.model) {
            this.modelConfig.model = 'gpt-3.5-turbo';
          }
        }
        this.testResult = null;
      } finally {
        // 使用 nextTick 确保 DOM 更新完成后才重置标志
        this.$nextTick(() => {
          this._providerChanging = false;
        });
      }
    },
    
    // 保存模型配置
    async saveModelConfig() {
      try {
        // 构建保存请求数据（Java驼峰格式）
        const saveData = {
          configType: 'ai_chat',
          configName: 'default',
          provider: this.modelConfig.provider,
          apiBase: this.modelConfig.baseUrl,
          model: this.modelConfig.model,
          temperature: this.modelConfig.temperature,
          maxTokens: this.modelConfig.maxTokens,
          enabled: this.modelConfig.enabled,
          enableStreaming: this.modelConfig.enableStreaming,
          // 聊天配置
          customInstructions: this.chatConfig.systemPrompt,
          welcomeMessage: this.chatConfig.welcomeMessage,
          maxConversationLength: this.chatConfig.historyCount,
          rateLimit: this.chatConfig.rateLimit,
          requireLogin: this.chatConfig.requireLogin,
          enableChatHistory: this.chatConfig.saveHistory,
          enableContentFilter: this.chatConfig.contentFilter,
          maxMessageLength: this.chatConfig.maxMessageLength || 500,
          // 外观配置
          chatAvatar: this.appearanceConfig.botAvatar,
          chatName: this.appearanceConfig.botName,
          themeColor: this.appearanceConfig.themeColor,
          enableTypingIndicator: this.appearanceConfig.typingAnimation,
          // 思考模式和工具调用配置
          enableThinking: this.modelConfig.enableThinking,
          enableTools: this.modelConfig.enableTools,
          // 记忆管理配置
          enableMemory: this.chatConfig.enableMemory,
          memoryAutoSave: this.chatConfig.memoryAutoSave,
          memoryAutoRecall: this.chatConfig.memoryAutoRecall,
          memoryRecallLimit: this.chatConfig.memoryRecallLimit
        };

        // 只有当API密钥不是隐藏格式时才发送
        if (this.modelConfig.apiKey && !this.modelConfig.apiKey.includes('*')) {
          saveData.apiKey = this.modelConfig.apiKey;
        }
        // 如果API密钥是隐藏格式，不发送apiKey字段，让后端保持原有密钥不变
        
        // 处理 Mem0 API 密钥
        if (this.chatConfig.mem0ApiKey && !this.chatConfig.mem0ApiKey.includes('*')) {
          saveData.mem0ApiKey = this.chatConfig.mem0ApiKey;
        }
        // 如果 Mem0 密钥是隐藏格式，不发送该字段，让后端保持原有密钥不变

        const response = await this.$http.post(this.$constant.baseURL + '/webInfo/ai/config/chat/save', saveData, true);
        
        if (response.code === 200) {
          this.$message.success('配置保存成功');
          // 保存成功后重新加载配置，获取最新的隐藏密钥格式
          await this.loadConfigs();
        } else {
          this.$message.error(response.message || '保存失败');
        }
      } catch (error) {
        console.error('保存配置失败:', error);
        this.$message.error('保存失败，请检查网络连接');
      }
    },
    
    // 保存聊天设置
    async saveChatConfig() {
      // 复用模型配置保存方法，因为后端是统一保存的
      await this.saveModelConfig();
    },
    
    // 保存外观配置
    async saveAppearanceConfig() {
      // 复用模型配置保存方法，因为后端是统一保存的
      await this.saveModelConfig();
    },
    
    // 测试连接
    async testConnection() {
      this.testing = true;
      this.testResult = '';

      try {
        // 检查是否使用的是隐藏的密钥
        if (this.isApiKeyMasked || (this.modelConfig.apiKey && this.modelConfig.apiKey.includes('*'))) {
          // 如果密钥被隐藏，使用保存的配置进行测试（不发送密钥）
          const response = await this.$http.post(this.$constant.baseURL + '/webInfo/ai/config/chat/test', {
            provider: this.modelConfig.provider,
            apiBase: this.modelConfig.baseUrl,
            model: this.modelConfig.model,
            use_saved_config: true  // 告诉后端使用已保存的配置
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
          // 使用当前输入的密钥进行测试
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
    
    // 头像上传成功
    handleAvatarSuccess(res) {
      if (res.flag) {
        this.appearanceConfig.botAvatar = res.data;
        this.$message.success('头像上传成功');
      } else {
        this.$message.error('头像上传失败');
      }
    },
    
    // 头像上传前验证
    beforeAvatarUpload(file) {
      const isImage = file.type.indexOf('image/') === 0;
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isImage) {
        this.$message.error('只能上传图片文件!');
        return false;
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB!');
        return false;
      }
      return true;
    },
    
    // 导出配置
    exportConfig() {
      const config = {
        model: this.modelConfig,
        chat: this.chatConfig,
        appearance: this.appearanceConfig
      };
      
      const blob = new Blob([JSON.stringify(config, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'ai-chat-config.json';
      link.click();
      URL.revokeObjectURL(url);
    },
    
    // 显示导入对话框
    showImportDialog() {
      this.importDialogVisible = true;
    },
    
    // 配置文件上传前验证
    beforeConfigUpload(file) {
      const isJson = file.type === 'application/json' || file.name.endsWith('.json');
      if (!isJson) {
        this.$message.error('只能上传JSON格式的配置文件!');
        return false;
      }
      return true;
    },
    
    // 处理配置导入
    handleConfigImport(res) {
      if (res.flag) {
        try {
          const config = JSON.parse(res.data);
          Object.assign(this.modelConfig, config.model || {});
          Object.assign(this.chatConfig, config.chat || {});
          Object.assign(this.appearanceConfig, config.appearance || {});
          this.$message.success('配置导入成功');
          this.importDialogVisible = false;
        } catch (error) {
          this.$message.error('配置文件格式错误');
        }
      } else {
        this.$message.error('配置导入失败');
      }
    },
    
    // 发送测试消息
    async sendTestMessage() {
      if (!this.testInput.trim()) return;
      
      const userMessage = {
        type: 'user',
        content: this.testInput,
        time: new Date().toLocaleTimeString()
      };
      
      this.testMessages.push(userMessage);
      this.testSending = true;
      
      try {
        // 使用正确的API端点进行测试
        const response = await this.$http.post(this.$constant.pythonBaseURL + "/ai/chat/sendMessage", {
          message: this.testInput,
          conversationId: `test_${Date.now()}`
        }, false); // 聊天API不需要管理员权限
        
        const botMessage = {
          type: 'bot',
          content: response.data ? response.data.response : '测试回复',
          time: new Date().toLocaleTimeString()
        };
        
        this.testMessages.push(botMessage);
      } catch (error) {
        const errorMessage = {
          type: 'bot',
          content: '发送失败: ' + error.message + '（提示：请先保存AI配置并确保配置正确）',
          time: new Date().toLocaleTimeString()
        };
        this.testMessages.push(errorMessage);
      }
      
      this.testInput = '';
      this.testSending = false;
      
      // 滚动到底部
      this.$nextTick(() => {
        const chatMessages = this.$refs.chatMessages;
        if (chatMessages) {
          chatMessages.scrollTop = chatMessages.scrollHeight;
        }
      });
    },

    onApiKeyInput() {
      // 当用户修改API密钥时，重置隐藏状态
      if (this.modelConfig.apiKey && !this.modelConfig.apiKey.includes('*')) {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
      }
      // 如果输入框被清空，也重置状态
      if (!this.modelConfig.apiKey) {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
      }
    },

    async showFullApiKey() {
      // 直接清空输入框，让用户重新输入
      this.$confirm('要重新输入API密钥吗？当前密钥将被清空。', '重新输入密钥', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.isApiKeyMasked = false;
        this.showingFullKey = false;
        this.modelConfig.apiKey = ''; // 清空输入框，让用户重新输入
        this.$message.info('请重新输入您的API密钥');
      }).catch(() => {
        // 用户取消操作
      });
    },

    hideFullApiKey() {
      this.isApiKeyMasked = true;
      this.showingFullKey = false;
      this.modelConfig.apiKey = this.originalMaskedKey;
    },
    
    // Mem0 API密钥输入处理
    onMem0ApiKeyInput() {
      // 当用户修改 Mem0 API密钥时，重置隐藏状态
      if (this.chatConfig.mem0ApiKey && !this.chatConfig.mem0ApiKey.includes('*')) {
        this.isMem0KeyMasked = false;
        this.showingFullMem0Key = false;
      }
      // 如果输入框被清空，也重置状态
      if (!this.chatConfig.mem0ApiKey) {
        this.isMem0KeyMasked = false;
        this.showingFullMem0Key = false;
      }
      // 清除之前的测试结果
      this.mem0TestResult = null;
    },
    
    // 显示完整的 Mem0 API密钥
    async showFullMem0Key() {
      this.$confirm('要重新输入 Mem0 API密钥吗？当前密钥将被清空。', '重新输入密钥', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.isMem0KeyMasked = false;
        this.showingFullMem0Key = false;
        this.chatConfig.mem0ApiKey = ''; // 清空输入框，让用户重新输入
        this.$message.info('请重新输入您的 Mem0 API密钥');
      }).catch(() => {
        // 用户取消操作
      });
    },
    
    // 测试 Mem0 连接
    async testMem0Connection() {
      if (!this.chatConfig.mem0ApiKey) {
        this.$message.warning('请先输入 Mem0 API 密钥');
        return;
      }
      
      this.testingMem0 = true;
      this.mem0TestResult = null;
      
      try {
        const testData = {
          mem0_api_key: this.chatConfig.mem0ApiKey
        };
        
        const response = await this.$http.post(
          this.$constant.pythonBaseURL + '/ai/memory/testConnection', 
          testData, 
          true
        );
        
        if (response.flag) {
          this.mem0TestResult = {
            success: true,
            message: response.message || 'Mem0 API 连接测试成功！'
          };
          this.$message.success('Mem0 API 连接测试成功！');
        } else {
          this.mem0TestResult = {
            success: false,
            message: response.message || '连接测试失败'
          };
          this.$message.error('连接测试失败: ' + response.message);
        }
      } catch (error) {
        this.mem0TestResult = {
          success: false,
          message: error.message || '测试失败'
        };
        this.$message.error('测试失败: ' + error.message);
      } finally {
        this.testingMem0 = false;
      }
    }
  },
  
  // 组件挂载时加载配置
  async mounted() {
    await this.loadConfigs();
  }
}
</script>

<style scoped>

.my-tag {
    margin-bottom: 20px !important;
    width: 100%;
    text-align: left;
    background: var(--lightYellow);
    border: none;
    height: 40px;
    line-height: 40px;
    font-size: 16px;
    color: var(--black);
  }

  .el-tag {
    margin: 10px;
  }

/* 页面容器 */
.ai-chat-management {
  padding: 24px;
  min-height: calc(100vh - 60px);
}

/* 页面标题区域 */
.page-header {
  margin-bottom: 24px;
}

.title-section {
  background: #ffffff;
  padding: 24px;
  border-radius: 8px;
  border-left: 4px solid #2d3748;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2d3748;
}

.page-title i {
  font-size: 24px;
  color: #4a5568;
}

.page-description {
  font-size: 14px;
  margin: 0;
  color: #718096;
  line-height: 1.5;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ========== 暗色模式适配 ========== */

/* config-card 暗色模式 */
.dark-mode .config-card {
  background-color: #2c2c2c !important;
  border-color: #404040 !important;
}

.dark-mode .config-card .el-card__header {
  background-color: #2c2c2c !important;
  border-bottom-color: #404040 !important;
}

.dark-mode .config-card .card-header span {
  color: #e0e0e0 !important;
}

.dark-mode .config-card .el-card__body {
  background-color: #2c2c2c !important;
  color: #b0b0b0 !important;
}

.help-text {
  color: #909399;
  margin-left: 10px;
}

.test-success {
  color: #67C23A;
  margin-left: 10px;
}

.test-error {
  color: #F56C6C;
  margin-left: 10px;
}

.avatar-upload .avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-upload .avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
}

.test-chat-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  border-radius: 50px;
  padding: 15px 20px;
}

.chat-test-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background: #f5f5f5;
}

.message {
  margin-bottom: 15px;
}

.message.user {
  text-align: right;
}

.message.bot {
  text-align: left;
}

.message-content {
  display: inline-block;
  padding: 10px 15px;
  border-radius: 10px;
  max-width: 80%;
  word-wrap: break-word;
}

.message.user .message-content {
  background: #409EFF;
  color: white;
}

.message.bot .message-content {
  background: white;
  color: #333;
  border: 1px solid #e4e7ed;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.chat-input {
  display: flex;
  padding: 10px;
  border-top: 1px solid #e4e7ed;
}

.chat-input .el-input {
  margin-right: 10px;
}

.custom-model-select {
  border: 2px dashed #67C23A !important;
}

.custom-model-select .el-input__inner {
  border-color: #67C23A;
  background-color: #f0f9ff;
}

.thinking-hint {
  color: #E6A23C !important;
  font-weight: 500;
  margin-top: 5px;
  display: block;
}

.api-key-status {
  margin-top: 8px;
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #67C23A;
}

.api-key-status .el-icon-success {
  margin-right: 5px;
  color: #67C23A;
}

.api-key-status span {
  margin-right: 10px;
}

.api-key-status .el-button {
  padding: 0;
  font-size: 12px;
  color: #409EFF;
}

/* ===========================================
   表单移动端样式 - PC端和移动端响应式
   =========================================== */

/* PC端样式 - 768px以上 */
@media screen and (min-width: 769px) {
  ::v-deep .el-form-item__label {
    float: left !important;
  }
}

/* 移动端样式 - 768px及以下 */
@media screen and (max-width: 768px) {
  /* 表单标签 - 垂直布局 */
  ::v-deep .el-form-item__label {
    float: none !important;
    width: 100% !important;
    text-align: left !important;
    margin-bottom: 8px !important;
    font-weight: 500 !important;
    font-size: 14px !important;
    padding-bottom: 0 !important;
    line-height: 1.5 !important;
  }

  ::v-deep .el-form-item__content {
    margin-left: 0 !important;
    width: 100% !important;
  }

  ::v-deep .el-form-item {
    margin-bottom: 20px !important;
  }

  /* 输入框移动端优化 */
  ::v-deep .el-input__inner {
    font-size: 16px !important;
    height: 44px !important;
    border-radius: 8px !important;
  }

  ::v-deep .el-textarea__inner {
    font-size: 16px !important;
    border-radius: 8px !important;
  }

  /* 选择器移动端优化 */
  ::v-deep .el-select {
    width: 100% !important;
  }

  ::v-deep .el-select .el-input__inner {
    height: 44px !important;
    line-height: 44px !important;
  }

  /* 按钮移动端优化 */
  ::v-deep .el-button {
    min-height: 40px !important;
    border-radius: 8px !important;
  }

  /* 页面容器移动端优化 */
  .ai-chat-management {
    padding: 15px !important;
  }

  .page-title {
    font-size: 20px !important;
  }

  .title-section {
    padding: 16px !important;
  }

  /* 对话框移动端优化 */
  ::v-deep .el-dialog {
    width: 95% !important;
    margin-top: 5vh !important;
  }

  ::v-deep .el-dialog__body {
    padding: 15px !important;
  }

  /* 测试按钮移动端优化 */
  .test-chat-btn {
    bottom: 20px !important;
    right: 20px !important;
    padding: 12px 16px !important;
  }
}

/* 极小屏幕优化 - 480px及以下 */
@media screen and (max-width: 480px) {
  ::v-deep .el-form-item__label {
    font-size: 13px !important;
  }

  ::v-deep .el-input__inner,
  ::v-deep .el-select .el-input__inner {
    height: 40px !important;
    line-height: 40px !important;
    font-size: 15px !important;
  }

  ::v-deep .el-button {
    min-height: 38px !important;
    font-size: 14px !important;
  }

  .page-title {
    font-size: 18px !important;
  }

  .ai-chat-management {
    padding: 10px !important;
  }
}
</style> 