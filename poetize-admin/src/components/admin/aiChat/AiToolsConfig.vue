<template>
  <div class="ai-tools-config">
    <!-- 原生内置工具组 -->
    <div class="pro-max-section">
      <div class="section-header">
        <div class="section-title">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" class="title-icon">
            <polyline points="16 18 22 12 16 6"></polyline>
            <polyline points="8 6 2 12 8 18"></polyline>
          </svg>
          系统内置原生能力
        </div>
        <div class="section-subtitle">无需额外配置，已默认集成到大模型运行时中。</div>
      </div>
      
      <div class="tools-grid">
        <div v-for="(tool, index) in nativeTools" :key="'native-' + index" class="glass-card tool-card">
          <div class="tool-icon-wrapper native-icon-bg">
            <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" v-html="tool.svgIcon"></svg>
          </div>
          <div class="tool-info">
            <div class="tool-header">
              <div style="display: flex; align-items: center; gap: 6px;">
                <span class="tool-name">{{ tool.name }}</span>
                <span class="tool-badge native-badge">原生支持</span>
              </div>
              <el-button v-if="tool.configurable" size="mini" type="text" icon="el-icon-setting" style="padding: 0; margin-left: auto;" @click.stop="openConfigDialog(tool.id)">配置</el-button>
            </div>
            <div class="tool-desc">{{ tool.description }}</div>
            <div class="tool-features">
              <span v-for="feat in tool.features" :key="feat" class="feature-tag">{{ feat }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 动态扩展插件组 -->
    <div class="pro-max-section" style="margin-top: 20px;">
      <div class="section-header" style="display: flex; justify-content: space-between; align-items: flex-start;">
        <div>
          <div class="section-title">
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" class="title-icon">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
              <line x1="12" y1="22.08" x2="12" y2="12"></line>
            </svg>
            扩展插件 (第三方接口)
          </div>
          <div class="section-subtitle">需前往插件管理配置接口凭证后才能生效，例如联网搜索等 API。</div>
        </div>
        <el-button size="small" @click="navigateToPluginManager">
          插件管理
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" style="margin-left: 4px;">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </el-button>
      </div>
      
      <div v-if="loading" class="loading-state">
        <i class="el-icon-loading"></i>
        <span>正在加载插件列表...</span>
      </div>
      
      <div v-else-if="externalTools.length === 0" class="empty-state glass-card">
        <svg viewBox="0 0 24 24" width="48" height="48" stroke="currentColor" stroke-width="1" fill="none" class="empty-icon text-slate-300">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
          <line x1="3" y1="9" x2="21" y2="9"></line>
          <line x1="9" y1="21" x2="9" y2="9"></line>
        </svg>
        <div class="empty-text">暂未安装或启用任何扩展工具插件</div>
        <el-button size="small" type="primary" plain class="mt-4" @click="navigateToPluginManager">去插件中心看看</el-button>
      </div>

      <div v-else class="tools-grid">
        <div v-for="tool in externalTools" :key="tool.id" class="glass-card tool-card interactive-card">
          <div class="tool-icon-wrapper ext-icon-bg">
            <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none">
              <!-- Default external API icon -->
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
          </div>
          <div class="tool-info">
            <div class="tool-header">
              <span class="tool-name">{{ tool.pluginName }}</span>
              <span :class="['tool-badge', tool.enabled ? 'active-badge' : 'inactive-badge']">
                {{ tool.enabled ? '已启用' : '未启用' }}
              </span>
            </div>
            <div class="tool-desc">{{ tool.pluginDescription }}</div>
            <div class="tool-meta">
              <span class="meta-item"><i class="el-icon-info"></i> {{ tool.pluginKey }}</span>
            </div>
          </div>
        </div>
        
        <!-- 引导安装更多卡片 -->
        <div class="glass-card tool-card dashed-card" @click="navigateToPluginManager">
          <div class="tool-icon-wrapper add-icon-bg">
            <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
          </div>
          <div class="tool-info justify-center">
            <div class="tool-name text-primary">安装更多扩展</div>
            <div class="tool-desc">安装、配置或管理更多如联网搜索之类的外部 API 工具。</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内置工具配置弹窗 -->
    <el-dialog
      title="配置 AI 上下文记忆 (Mem0)"
      :visible.sync="memoryDialogVisible"
      width="500px"
      custom-class="pro-max-dialog"
      :close-on-click-modal="false"
      append-to-body>
      
      <el-form v-if="memoryDialogVisible" label-width="120px" class="compact-form">
        <el-form-item id="field-ai-mem0-enable" label="开启记忆">
          <el-switch v-model="localConfig.enableMemory" @change="emitChange"></el-switch>
          <div class="form-tip">是否开启跨会话长期记忆能力。</div>
        </el-form-item>
        
        <el-form-item id="field-ai-mem0-key" label="Mem0 密钥" v-if="localConfig.enableMemory">
          <el-input v-model="localConfig.mem0ApiKey" @change="emitChange" placeholder="输入 Mem0 平台提供的 API Key" size="small" show-password></el-input>
          <div class="form-tip">
            Mem0 API Key，可前往 <a href="https://mem0.ai/" target="_blank" class="link-primary">mem0.ai</a> 获取。
          </div>
        </el-form-item>

        <el-form-item id="field-ai-mem0-autosave" label="自动保存记忆" v-if="localConfig.enableMemory">
          <el-switch v-model="localConfig.memoryAutosave" @change="emitChange"></el-switch>
          <div class="form-tip">聊天过程中，自动总结并保存有用的知识点到记忆库。</div>
        </el-form-item>

        <el-form-item id="field-ai-mem0-autorecall" label="自动提取记忆" v-if="localConfig.enableMemory">
          <el-switch v-model="localConfig.memoryAutorecall" @change="emitChange"></el-switch>
          <div class="form-tip">每次对话前，自动检索并注入相关的历史记忆。</div>
        </el-form-item>

        <el-form-item id="field-ai-mem0-limit" label="注入提取上限" v-if="localConfig.enableMemory && localConfig.memoryAutorecall">
          <el-input-number v-model="localConfig.memoryRecallLimit" @change="emitChange" :min="1" :max="20" size="small" style="width: 120px;"></el-input-number>
          <div class="form-tip">单次对话最多检索并合并多少条相关记忆。</div>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="memoryDialogVisible = false">关闭</el-button>
        <el-button size="small" type="primary" @click="memoryDialogVisible = false">完成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'AiToolsConfig',
  props: {
    value: {
      type: Object,
      default: () => ({
        enableMemory: false,
        mem0ApiKey: '',
        memoryAutosave: true,
        memoryAutorecall: true,
        memoryRecallLimit: 5
      })
    }
  },
  data() {
    return {
      localConfig: { ...this.value },
      loading: true,
      memoryDialogVisible: false,
      externalTools: [],
      // 硬编码的系统内置原生工具 (基于 ArticleTools.java、TimeTools.java、CalculatorTools.java 提取)
      nativeTools: [
        {
          id: 'article_search',
          name: '文章资源检索',
          description: '允许大语言模型搜索、阅读和归纳分析当前站点的博客文章资源。',
          svgIcon: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>',
          features: ['全文读取', '热门文章', '分类目录', '搜索摘要']
        },
        {
          id: 'time_wizard',
          name: '时间与节气向导',
          description: '赋予 AI 当前时间、农历/节气/节日、最近节日倒计时，以及中国法定放假/调休查询能力；法定安排会明确区分 official 与 predicted 口径。',
          svgIcon: '<circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline>',
          features: ['当前时间/时区', '农历/节气/节日', '最近节日倒计时', '放假/调休']
        },
        {
          id: 'calculator',
          name: '数学计算器',
          description: '允许 AI 直接进行数学表达式计算，适合四则运算、括号表达式、幂运算，以及常见数学函数求值。',
          svgIcon: '<rect x="5" y="2" width="14" height="20" rx="2"></rect><line x1="8" y1="6" x2="16" y2="6"></line><line x1="8" y1="10" x2="8" y2="10"></line><line x1="12" y1="10" x2="12" y2="10"></line><line x1="16" y1="10" x2="16" y2="10"></line><line x1="8" y1="14" x2="8" y2="14"></line><line x1="12" y1="14" x2="12" y2="14"></line><line x1="16" y1="14" x2="16" y2="14"></line><line x1="8" y1="18" x2="16" y2="18"></line>',
          features: ['四则运算', '括号/负数', '幂与取模', 'sqrt/pow/max']
        },
        {
          id: 'ai_memory',
          name: 'AI 上下文记忆',
          description: '为 AI 提供长记忆能力，记住关键事实，并在未来对话中进行提取回忆。',
          svgIcon: '<path d="M12 5a3 3 0 1 0-5.997.125 4 4 0 0 0-2.526 5.77 4 4 0 0 0 .556 6.588A4 4 0 1 0 12 18Z"></path><path d="M12 5a3 3 0 1 1 5.997.125 4 4 0 0 1 2.526 5.77 4 4 0 0 1-.556 6.588A4 4 0 1 1 12 18Z"></path><path d="M15 13a4.5 4.5 0 0 1-3-4 4.5 4.5 0 0 1-3 4"></path><path d="M17.599 6.5a3 3 0 0 0 .399-1.375"></path><path d="M6.003 5.125A3 3 0 0 0 6.401 6.5"></path><path d="M3.477 10.896a4 4 0 0 1 .585-.396"></path><path d="M19.938 10.5a4 4 0 0 1 .585.396"></path><path d="M6 18a4 4 0 0 1-1.967-.516"></path><path d="M19.967 17.484A4 4 0 0 1 18 18"></path>',
          features: ['跨会话记忆', '自动提炼', '长短期管理', '个性化事实'],
          configurable: true
        }
      ]
    }
  },
  watch: {
    value: {
      handler(val) {
        this.localConfig = { ...val };
      },
      deep: true
    }
  },
  mounted() {
    this.fetchExternalTools();
  },
  methods: {
    fetchExternalTools() {
      this.loading = true;
      this.$http.get(this.$constant.baseURL + "/sysPlugin/listPlugins", { pluginType: 'ai_tool' }, true)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.externalTools = res.data;
          }
        })
        .catch(err => {
          console.error("Failed to load ai tool plugins:", err);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    navigateToPluginManager() {
      this.$router.push({ path: '/pluginManager', query: { type: 'ai_tool' } });
      this.$emit('close-dialog'); // 如果在移动端弹窗中点击，则关闭弹窗
    },
    openConfigDialog(id) {
      if (id === 'ai_memory') {
        this.memoryDialogVisible = true;
      }
    },
    emitChange() {
      this.$emit('input', { ...this.localConfig });
    }
  }
}
</script>

<style scoped>
.justify-center {
  justify-content: center;
}
.mt-auto {
  margin-top: auto;
}
.pt-3 {
  padding-top: 12px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
.link-primary {
  color: #409EFF;
  text-decoration: none;
}
.link-primary:hover {
  text-decoration: underline;
}
.ai-tools-config {
  padding: 8px 10px 24px 10px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 块划分 */
.pro-max-section {
  display: flex;
  flex-direction: column;
}

.section-header {
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
  gap: 6px;
}

.title-icon {
  color: #64748b;
}

.section-subtitle {
  font-size: 12px;
  color: #64748b;
}

/* Grid 网格 */
.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

/* 基础卡片 */
.glass-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

/* 卡片容器 */
.tool-card {
  display: flex;
  padding: 12px;
  gap: 12px;
}

.interactive-card {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.interactive-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 图标容器区 */
.tool-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  flex-shrink: 0;
}

.native-icon-bg {
  background-color: #f0fdf4;
  color: #16a34a;
  box-shadow: inset 0 0 0 1px rgba(22, 163, 74, 0.1);
}

.ext-icon-bg {
  background-color: #eff6ff;
  color: #2563eb;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.1);
}

.add-icon-bg {
  background-color: #f8fafc;
  color: #94a3b8;
  border: 1px dashed #cbd5e1;
}

/* 卡片内容区 */
.tool-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.tool-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.tool-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 徽章与标签 */
.tool-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
  transform: scale(0.9);
  transform-origin: right top;
}

.native-badge {
  background-color: #f0fdf4;
  color: #16a34a;
}

.active-badge {
  background-color: #eff6ff;
  color: #2563eb;
}

.inactive-badge {
  background-color: #f1f5f9;
  color: #64748b;
}

.tool-features {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: auto;
}

.feature-tag {
  font-size: 10px;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 1px 6px;
  border-radius: 4px;
  transform: scale(0.95);
  transform-origin: left bottom;
}

.tool-meta {
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  margin-top: auto;
}

/* 引导安装虚线卡片 */
.dashed-card {
  border-style: dashed;
  cursor: pointer;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.dashed-card:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.dashed-card:hover .add-icon-bg {
  color: #3b82f6;
  border-color: #93c5fd;
}

.dashed-card:hover .text-primary {
  color: #2563eb;
}

/* 空状态与加载状态 */
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #64748b;
  font-size: 14px;
}

.empty-state {
  background-color: #f8fafc;
  border-radius: 12px;
}

.empty-icon {
  margin-bottom: 16px;
  color: #cbd5e1;
}

.empty-text {
  color: #64748b;
  font-size: 13px;
}

.mt-4 {
  margin-top: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .tools-grid {
    grid-template-columns: 1fr;
  }
}
</style>
