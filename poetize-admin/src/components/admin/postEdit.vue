<template>
  <div>
    <el-tag effect="dark" class="my-tag">
      <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -3px;">
        <path d="M0 0h1024v1024H0V0z" fill="#202425" opacity=".01"></path>
        <path
          d="M682.666667 204.8h238.933333a34.133333 34.133333 0 0 1 34.133333 34.133333v648.533334a68.266667 68.266667 0 0 1-68.266666 68.266666h-204.8V204.8z"
          fill="#FFAA44"></path>
        <path
          d="M68.266667 921.6a34.133333 34.133333 0 0 0 34.133333 34.133333h785.066667a68.266667 68.266667 0 0 1-68.266667-68.266666V102.4a34.133333 34.133333 0 0 0-34.133333-34.133333H102.4a34.133333 34.133333 0 0 0-34.133333 34.133333v819.2z"
          fill="#11AA66"></path>
        <path
          d="M238.933333 307.2a34.133333 34.133333 0 0 0 0 68.266667h136.533334a34.133333 34.133333 0 1 0 0-68.266667H238.933333z m0 204.8a34.133333 34.133333 0 1 0 0 68.266667h409.6a34.133333 34.133333 0 1 0 0-68.266667H238.933333z m0 204.8a34.133333 34.133333 0 1 0 0 68.266667h204.8a34.133333 34.133333 0 1 0 0-68.266667H238.933333z"
          fill="#FFFFFF"></path>
      </svg>
      文章信息
    </el-tag>
    <el-form :model="article" :rules="rules" ref="ruleForm" label-width="120px"
             class="demo-ruleForm mobile-responsive-form">
      <el-form-item label="标题" prop="articleTitle">
        <el-input v-model="article.articleTitle" maxlength="500" show-word-limit></el-input>
      </el-form-item>

      <el-form-item label="视频链接" prop="videoUrl">
        <el-input maxlength="1000" v-model="article.videoUrl"></el-input>
      </el-form-item>

      <el-form-item label="内容" prop="articleContent">
        <div v-if="!editorReady" class="editor-loading-wrapper">
          <div class="editor-skeleton">
            <div class="skeleton-toolbar"></div>
            <div class="skeleton-content">
              <i class="el-icon-loading"></i>
              <p>编辑器加载中...</p>
            </div>
          </div>
        </div>
        
        <!-- 延迟渲染编辑器，数据加载完成后再初始化 -->
        <ArticleEditor 
          v-if="shouldRenderEditor"
          ref="md" 
          v-model="article.articleContent"
          :height="600"
          mode="ir"
          placeholder="请输入文章内容..."
          @image-add="imgAdd"
          @change="handleEditorChange"
          @ready="onMainEditorReady"
        />
      </el-form-item>

      <!-- 翻译编辑按钮和跳过开关 -->
      <el-form-item>
        <div style="display: flex; align-items: center; gap: 20px;">
          <div>
            <el-button type="info" icon="el-icon-edit" @click="openTranslationEditor">编辑翻译</el-button>
            <span style="margin-left: 10px; color: #909399; font-size: 12px;">
              编辑文章的翻译版本
            </span>
          </div>

          <div style="display: flex; align-items: center;">
            <el-switch
              v-model="skipAiTranslation"
              active-text="跳过AI自动翻译"
              inactive-text="启用AI自动翻译"
              active-color="#F56C6C"
              inactive-color="#13CE66"
              class="skip-translation-switch"
              style="margin-right: 10px;">
            </el-switch>
            <el-tooltip content="开启后保存文章时不会执行AI自动翻译" placement="top">
              <i class="el-icon-question" style="color: #909399; cursor: help;"></i>
            </el-tooltip>
          </div>
        </div>

        <!-- 暂存翻译提示 -->
        <div v-if="hasPendingTranslation" style="margin-top: 8px;">
          <el-tag type="warning" size="mini">
            <i class="el-icon-edit"></i>
            有未保存的翻译内容 ({{ getLanguageName(pendingTranslation.language) }})
          </el-tag>
        </div>
      </el-form-item>

      <el-form-item label="是否启用评论" prop="commentStatus">
        <el-tag :type="article.commentStatus === false ? 'danger' : 'success'"
                disable-transitions>
          {{article.commentStatus === false ? '否' : '是'}}
        </el-tag>
        <el-switch v-model="article.commentStatus"></el-switch>
      </el-form-item>

      <el-form-item label="是否推荐" prop="recommendStatus">
        <el-tag :type="article.recommendStatus === false ? 'danger' : 'success'"
                disable-transitions>
          {{article.recommendStatus === false ? '否' : '是'}}
        </el-tag>
        <el-switch v-model="article.recommendStatus"></el-switch>
      </el-form-item>

      <el-form-item label="是否可见" prop="viewStatus">
        <el-tag :type="article.viewStatus === false ? 'danger' : 'success'"
                disable-transitions>
          {{article.viewStatus === false ? '否' : '是'}}
        </el-tag>
        <el-switch v-model="article.viewStatus"></el-switch>
      </el-form-item>

      <el-form-item label="推送至搜索引擎" prop="submitToSearchEngine">
        <el-tag :type="article.submitToSearchEngine === false ? 'info' : 'success'"
                disable-transitions>
          {{article.submitToSearchEngine === false ? '否' : '是'}}
        </el-tag>
        <el-switch v-model="article.submitToSearchEngine"></el-switch>
        <div class="tip-text">
          <i class="el-icon-info"></i> 
          是否在保存后自动推送文章到搜索引擎（百度、谷歌等）以便提高收录速度
        </div>
      </el-form-item>

      <el-form-item v-if="article.viewStatus === false" label="不可见时的访问密码" prop="password">
        <el-input maxlength="30" v-model="article.password"></el-input>
      </el-form-item>

      <el-form-item v-if="article.viewStatus === false" label="密码提示" prop="tips">
        <el-input maxlength="60" v-model="article.tips"></el-input>
      </el-form-item>

      <el-form-item label="封面" prop="articleCover">
        <div class="cover-input-container">
          <el-input 
            v-model="article.articleCover" 
            placeholder="请输入图片链接或使用下方上传功能，如果不想设置封面可以留空"></el-input>
          <el-image 
            class="table-td-thumb"
            lazy
            :preview-src-list="[article.articleCover]"
            :src="article.articleCover"
            fit="cover">
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline"></i>
              <div class="image-placeholder-text">封面预览</div>
            </div>
          </el-image>
        </div>
        <uploadPicture :isAdmin="true" :prefix="'articleCover'" class="cover-upload" @addPicture="addArticleCover"
                       :maxSize="5"
                       :maxNumber="1"></uploadPicture>
      </el-form-item>
      <el-form-item label="分类" prop="sortId">
        <el-select v-model="article.sortId" placeholder="请选择分类" @change="handleSortChange">
          <el-option
            v-for="item in sorts"
            :key="item.id"
            :label="item.sortName"
            :value="item.id">
          </el-option>
          <el-option
            key="new-sort"
            label="+ 新建分类"
            value="new-sort"
            style="color: #409EFF; font-weight: bold;">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="标签" prop="labelId">
        <el-select v-model="article.labelId" placeholder="请选择标签" @change="handleLabelChange">
          <el-option
            v-for="item in labelsTemp"
            :key="item.id"
            :label="item.labelName"
            :value="item.id">
          </el-option>
          <el-option
            v-if="article.sortId && article.sortId !== 'new-sort'"
            key="new-label"
            label="+ 新建标签"
            value="new-label"
            style="color: #409EFF; font-weight: bold;">
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 文章付费设置 -->
      <template v-if="paymentPluginActive">
        <el-divider content-position="left">
          <span style="font-size: 14px; color: #606266;">
            <i class="el-icon-money"></i> 文章付费设置
          </span>
        </el-divider>

        <el-form-item label="付费类型">
          <el-select v-model="article.payType" placeholder="请选择付费类型">
            <el-option :value="0" label="免费"></el-option>
            <el-option :value="1" label="按文章付费"></el-option>
            <el-option :value="2" label="会员专属"></el-option>
            <el-option :value="3" label="赞赏解锁"></el-option>
            <el-option :value="4" label="固定金额解锁"></el-option>
          </el-select>
          <div class="tip-text">
            <i class="el-icon-info"></i>
            当前已启用付费插件：{{ paymentPluginName }}
          </div>
        </el-form-item>

        <el-form-item v-if="article.payType === 4" label="付费金额（元）">
          <el-input-number
            v-model="article.payAmount"
            :min="0.01"
            :max="99999"
            :precision="2"
            :step="1"
            placeholder="请输入金额">
          </el-input-number>
        </el-form-item>

        <el-form-item v-if="article.payType && article.payType !== 0" label="免费预览比例(%)">
          <el-slider
            v-model="article.freePercent"
            :min="0"
            :max="100"
            :step="5"
            show-input
            style="max-width: 500px;">
          </el-slider>
          <div class="tip-text">
            <i class="el-icon-info"></i>
            读者可免费阅读文章前 {{ article.freePercent || 30 }}% 的内容，其余部分需付费解锁。也可在文章内容中插入 <code>&lt;!--paywall--&gt;</code> 标记来精确控制截断位置。
          </div>
        </el-form-item>
      </template>

      <div v-else class="payment-not-enabled-tip" style="margin: 10px 0 20px 120px; color: #909399; font-size: 12px;">
        <i class="el-icon-info"></i>
        如需设置文章付费，请先在 <b>插件管理 → 文章付费</b> 中启用并配置付费插件。
      </div>
    </el-form>
    <div class="myCenter" style="margin-bottom: 22px">
      <el-button type="primary" @click="submitForm('ruleForm')">保存并等待</el-button>
      <el-button type="success" @click="submitFormAsync('ruleForm')" :loading="asyncSaveLoading">
        <i class="el-icon-loading" v-if="asyncSaveLoading"></i>
        <i class="el-icon-check" v-else></i>
        保存并离开
      </el-button>
      <el-button type="danger" @click="resetForm('ruleForm')">重置所有修改</el-button>
    </div>

    <!-- 新建分类对话框 -->
    <el-dialog title="新建分类" :visible.sync="newSortDialog" width="500px" :close-on-click-modal="false" custom-class="centered-dialog">
      <el-form ref="newSortForm" :model="newSortForm" :rules="newSortRules" label-width="100px">
        <el-form-item label="分类类型" prop="sortType">
          <el-radio-group v-model="newSortForm.sortType">
            <el-radio-button :label="0">导航栏分类</el-radio-button>
            <el-radio-button :label="1">普通分类</el-radio-button>
          </el-radio-group>
          <div class="tip-text">
            <i class="el-icon-info"></i> 
            导航栏分类会显示在侧边栏"速览"模块中
          </div>
        </el-form-item>
        <el-form-item label="分类名称" prop="sortName">
          <el-input v-model="newSortForm.sortName" placeholder="请输入分类名称" maxlength="32" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="分类描述" prop="sortDescription">
          <el-input v-model="newSortForm.sortDescription" placeholder="请输入分类描述" maxlength="256" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="newSortForm.priority" :min="1" :max="999" placeholder="数字越小越靠前"></el-input-number>
          <div class="tip-text">
            <i class="el-icon-info"></i> 
            数字越小的分类在前端显示时越靠前
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelNewSort">取 消</el-button>
        <el-button type="primary" @click="createNewSort" :loading="newSortLoading">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 新建标签对话框 -->
    <el-dialog title="新建标签" :visible.sync="newLabelDialog" width="500px" :close-on-click-modal="false" custom-class="centered-dialog">
      <el-form ref="newLabelForm" :model="newLabelForm" :rules="newLabelRules" label-width="100px">
        <el-form-item label="所属分类">
          <el-input :value="getCurrentSortName()" disabled></el-input>
        </el-form-item>
        <el-form-item label="标签名称" prop="labelName">
          <el-input v-model="newLabelForm.labelName" placeholder="请输入标签名称" maxlength="32" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="标签描述" prop="labelDescription">
          <el-input v-model="newLabelForm.labelDescription" placeholder="请输入标签描述" maxlength="256" show-word-limit></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="createNewLabel" :loading="newLabelLoading">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 翻译编辑弹窗 -->
    <el-dialog
      title="编辑文章翻译"
      :visible.sync="translationDialogVisible"
      width="80%"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
      custom-class="centered-dialog translation-dialog"
    >
      <el-form :model="translationForm" ref="translationForm" label-width="120px">
        <!-- 目标语言选择 -->
        <el-form-item label="目标语言">
          <el-select v-model="translationForm.targetLanguage" @change="onTargetLanguageChange">
            <el-option label="English (英文)" value="en"></el-option>
            <el-option label="日本語 (日文)" value="ja"></el-option>
            <el-option label="繁體中文 (繁体中文)" value="zh-TW"></el-option>
            <el-option label="한국어 (韩文)" value="ko"></el-option>
            <el-option label="Français (法文)" value="fr"></el-option>
            <el-option label="Deutsch (德文)" value="de"></el-option>
            <el-option label="Español (西班牙文)" value="es"></el-option>
            <el-option label="Русский (俄文)" value="ru"></el-option>
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            修改后将同时更新系统默认目标语言
          </span>
        </el-form-item>

        <!-- 翻译标题 -->
        <el-form-item label="翻译标题" prop="translatedTitle">
          <el-input
            v-model="translationForm.translatedTitle"
            maxlength="500"
            show-word-limit
            placeholder="请输入翻译后的文章标题">
          </el-input>
        </el-form-item>

        <!-- 翻译内容 -->
        <el-form-item label="翻译内容" prop="translatedContent">
          <div v-if="translationDialogVisible && !translationEditorReady" class="editor-loading-wrapper">
            <div class="editor-skeleton">
              <div class="skeleton-toolbar"></div>
              <div class="skeleton-content">
                <i class="el-icon-loading"></i>
                <p>翻译编辑器加载中...</p>
              </div>
            </div>
          </div>
          <ArticleEditor
            v-if="shouldRenderTranslationEditor"
            ref="translationMd"
            class="translation-editor"
            v-model="translationForm.translatedContent"
            :height="500"
            mode="ir"
            placeholder="请输入翻译后的文章内容"
            @change="handleTranslationEditorChange"
            @ready="onTranslationEditorReady"
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="closeTranslationDialog">取 消</el-button>
        <el-button type="primary" @click="saveTranslation" :loading="translationSaving">
          保 存
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
    import { useMainStore } from '@/stores/main';

const uploadPicture = () => import("../common/uploadPicture");
  const ArticleEditor = () => import('@/components/ArticleEditor.vue');
  import { getAdminLanguageName } from '@/utils/languageUtils';

  const createDefaultArticle = () => ({
    articleTitle: "",
    articleContent: "",
    commentStatus: true,
    recommendStatus: false,
    viewStatus: true,
    submitToSearchEngine: true,
    password: "",
    tips: "",
    articleCover: "",
    videoUrl: "",
    sortId: null,
    labelId: null,
    payType: 0,
    payAmount: null,
    freePercent: 30
  });

  export default {
    components: {
      uploadPicture,
      ArticleEditor
    },
    data() {
      return {
        id: this.$route.query.id ? parseInt(this.$route.query.id) : null,
        loading: null,
        asyncSaveLoading: false,
        currentTaskId: null,
        newSortLoading: false,
        newLabelLoading: false,
        currentStoreType: null, // 添加currentStoreType属性
        // 编辑器加载优化相关
        editorReady: false, // 编辑器是否准备好
        shouldRenderEditor: false, // 是否应该渲染编辑器
        shouldRenderTranslationEditor: false,
        mainEditor: null,
        deferredTaskHandles: [],
        article: createDefaultArticle(),
        // 付费插件状态
        paymentPluginActive: false,
        paymentPluginName: '',
        sorts: [],
        labels: [],
        labelsTemp: [],
        // 新建分类对话框
        newSortDialog: false,
        newSortForm: {
          sortName: '',
          sortDescription: '',
          priority: 1,
          sortType: 0
        },
        newSortRules: {
          sortName: [
            { required: true, message: '请输入分类名称', trigger: 'blur' },
            { max: 32, message: '分类名称长度不能超过32个字符', trigger: 'blur' }
          ],
          sortDescription: [
            { required: true, message: '请输入分类描述', trigger: 'blur' },
            { max: 256, message: '分类描述长度不能超过256个字符', trigger: 'blur' }
          ],
          priority: [
            { required: true, message: '请输入优先级', trigger: 'blur' },
            { type: 'number', message: '优先级必须为数字值', trigger: 'blur' }
          ]
        },
        // 新建标签对话框
        newLabelDialog: false,
        newLabelForm: {
          labelName: '',
          labelDescription: '',
          sortId: null
        },
        newLabelRules: {
          labelName: [
            { required: true, message: '请输入标签名称', trigger: 'blur' },
            { max: 32, message: '标签名称长度不能超过32个字符', trigger: 'blur' }
          ],
          labelDescription: [
            { required: true, message: '请输入标签描述', trigger: 'blur' },
            { max: 256, message: '标签描述长度不能超过256个字符', trigger: 'blur' }
          ]
        },
        // 翻译编辑相关数据
        translationDialogVisible: false,
        translationEditorReady: false,
        translationSaving: false,
        translationForm: {
          targetLanguage: 'en',
          translatedTitle: '',
          translatedContent: ''
        },
        // 跳过AI翻译开关
        skipAiTranslation: false,
        // 暂存的翻译数据
        pendingTranslation: {
          title: '',
          content: '',
          language: ''
        },
        // 响应式布局相关
        resizeTimer: null,
        rules: {
          articleTitle: [
            {required: true, message: '请输入标题', trigger: 'change'},
            {max: 500, message: '标题长度不能超过500个字符', trigger: 'change'}
          ],
          articleContent: [
            {required: true, message: '请输入内容', trigger: 'change'}
          ],
          commentStatus: [
            {required: true, message: '是否启用评论', trigger: 'change'}
          ],
          recommendStatus: [
            {required: true, message: '是否推荐', trigger: 'change'}
          ],
          viewStatus: [
            {required: true, message: '是否可见', trigger: 'change'}
          ],
          articleCover: [
            {required: true, message: '封面', trigger: 'change'}
          ],
          sortId: [
            {required: true, message: '分类', trigger: 'change'}
          ],
          labelId: [
            {required: true, message: '标签', trigger: 'blur'}
          ]
        }
      };
    },

    computed: {
      mainStore() {
        return useMainStore();
      },
      // 检查是否有暂存的翻译数据
      hasPendingTranslation() {
        return this.pendingTranslation.title &&
               this.pendingTranslation.content &&
               this.pendingTranslation.language;
      }
    },

    watch: {
      'article.sortId'(newVal, oldVal) {
        if (oldVal !== null) {
          this.article.labelId = null;
        }
        if (!this.$common.isEmpty(newVal) && !this.$common.isEmpty(this.labels)) {
          this.labelsTemp = this.labels.filter(l => l.sortId === newVal);
        }
      },
      
      // 监听路由变化，更新文章 ID
      '$route.query.id'(newId, oldId) {
        if (newId === oldId) {
          return;
        }
        this.id = newId ? parseInt(newId) : null;
        this.initializePageData();
      }
    },

    created() {
      // 优化加载流程：先加载分类标签，再延迟初始化编辑器
      this.initializePageData();
      
      // 监听系统配置更新事件
      this.$bus.$on('sysConfigUpdated', this.handleSysConfigUpdate);
      
      // 初始化移动端表单适配
      this.initMobileFormLayout();
      
      // 监听窗口大小变化
      window.addEventListener('resize', this.handleWindowResize);
    },
    
    mounted() {
      // 编辑器将在数据加载完成后延迟初始化，提升用户体验
    },
    
    beforeDestroy() {
      // 移除事件监听，避免内存泄漏
      this.$bus.$off('sysConfigUpdated', this.handleSysConfigUpdate);
      
      // 移除窗口大小变化监听
      window.removeEventListener('resize', this.handleWindowResize);
      this.clearDeferredTasks();
    },



    methods: {
      syncMainEditorContent() {
        const editor = this.mainEditor || this.$refs.md
        if (editor && typeof editor.getValue === 'function') {
          this.article.articleContent = editor.getValue()
        }
      },
      // 初始化页面数据（优化后的加载流程）
      async initializePageData() {
        this.editorReady = false;
        this.shouldRenderEditor = false;
        this.mainEditor = null;
        this.clearDeferredTasks();
        try {
          const tasks = [this.getSortAndLabel(false)];
          if (!this.$common.isEmpty(this.id)) {
            tasks.push(this.getArticleById({ checkTranslationStatus: false }));
          } else {
            this.article = createDefaultArticle();
          }

          await Promise.all(tasks);
          this.scheduleEditorMount();
          this.scheduleNonCriticalStartupTasks();
        } catch (error) {
          console.error('初始化页面数据失败:', error);
          // 即使失败也要显示编辑器
          this.scheduleEditorMount();
        }
      },

      scheduleEditorMount() {
        this.$nextTick(() => {
          const mountEditor = () => {
            this.shouldRenderEditor = true;
          };

          if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
            window.requestAnimationFrame(mountEditor);
          } else {
            setTimeout(mountEditor, 16);
          }
        });
      },

      scheduleDeferredTask(task, delay = 300) {
        const removeHandle = (handle) => {
          this.deferredTaskHandles = this.deferredTaskHandles.filter((item) => item !== handle);
        };
        let handle = null;

        if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
          const idleId = window.requestIdleCallback(() => {
            removeHandle(handle);
            task();
          }, { timeout: Math.max(800, delay) });
          handle = { type: 'idle', id: idleId };
          this.deferredTaskHandles.push(handle);
          return;
        }

        const timeoutId = window.setTimeout(() => {
          removeHandle(handle);
          task();
        }, delay);
        handle = { type: 'timeout', id: timeoutId };
        this.deferredTaskHandles.push(handle);
      },

      clearDeferredTasks() {
        this.deferredTaskHandles.forEach((handle) => {
          if (handle.type === 'idle' && typeof window !== 'undefined' && typeof window.cancelIdleCallback === 'function') {
            window.cancelIdleCallback(handle.id);
            return;
          }
          clearTimeout(handle.id);
        });
        this.deferredTaskHandles = [];
      },

      scheduleNonCriticalStartupTasks() {
        this.scheduleDeferredTask(() => {
          this.checkPaymentPlugin();
        }, 400);

        if (!this.$common.isEmpty(this.id)) {
          this.scheduleDeferredTask(() => {
            this.checkAndSetTranslationMode();
          }, 900);
        }
      },
      
      // 检查是否有已启用的付费插件
      checkPaymentPlugin() {
        this.$http.get(this.$constant.baseURL + "/sysPlugin/getActivePlugin", { pluginType: 'payment' }, true)
          .then((res) => {
            if (res.data && res.data.enabled) {
              this.paymentPluginActive = true;
              this.paymentPluginName = res.data.pluginName || res.data.pluginKey || '未知';
            } else {
              this.paymentPluginActive = false;
              this.paymentPluginName = '';
            }
          })
          .catch(() => {
            this.paymentPluginActive = false;
            this.paymentPluginName = '';
          });
      },

      // 主编辑器就绪回调
      onMainEditorReady(editor) {
        this.mainEditor = editor;
        this.editorReady = true;
      },
      
      // 翻译编辑器就绪回调
      onTranslationEditorReady() {
        this.translationEditorReady = true;
      },
      
      // 主编辑器内容变化处理
      handleEditorChange(value) {
        // Vditor 内置了 Mermaid 支持，无需手动渲染
      },
      
      // 翻译编辑器内容变化处理
      handleTranslationEditorChange(value) {
        // Vditor 内置了 Mermaid 支持，无需手动渲染
      },

      resetTranslationForm() {
        this.translationForm.translatedTitle = '';
        this.translationForm.translatedContent = '';
      },

      restorePendingTranslation(language = this.translationForm.targetLanguage) {
        if (!this.hasPendingTranslation || this.pendingTranslation.language !== language) {
          return false;
        }

        this.translationForm.translatedTitle = this.pendingTranslation.title || '';
        this.translationForm.translatedContent = this.pendingTranslation.content || '';
        return true;
      },

      buildArticleRequestPayload(article) {
        const payload = {
          ...article,
          skipAiTranslation: this.skipAiTranslation
        };

        if (this.hasPendingTranslation) {
          payload.pendingTranslationTitle = this.pendingTranslation.title;
          payload.pendingTranslationContent = this.pendingTranslation.content;
          payload.pendingTranslationLanguage = this.pendingTranslation.language;
        } else {
          payload.pendingTranslationTitle = null;
          payload.pendingTranslationContent = null;
          payload.pendingTranslationLanguage = null;
        }

        return payload;
      },
      
      // 打开翻译编辑器对话框
      async openTranslationEditor() {
        try {
          // 加载默认目标语言
          await this.loadDefaultTargetLanguage();

          if (this.hasPendingTranslation) {
            this.translationForm.targetLanguage = this.pendingTranslation.language;
          }

          if (!this.restorePendingTranslation()) {
            // 只有在文章已保存（有ID）时才加载已有翻译
            if (!this.$common.isEmpty(this.id)) {
              await this.loadExistingTranslation();
            } else {
              // 新文章，以空白状态打开
              this.resetTranslationForm();
            }
          }

          // 显示弹窗
          this.translationEditorReady = false;
          this.translationDialogVisible = true;
          this.$nextTick(() => {
            this.shouldRenderTranslationEditor = true;
          });
          
          // Vditor 编辑器将在 ready 事件中自动初始化
        } catch (error) {
          console.error('打开翻译编辑器失败:', error);
          this.$message.error('打开翻译编辑器失败: ' + error.message);
        }
      },
      

      async loadDefaultTargetLanguage() {
        try {
          // 从Java API获取默认语言
          const response = await this.$http.get(this.$constant.baseURL + "/webInfo/ai/config/articleAi/defaultLang");
          if (response.code === 200 && response.data) {
            this.translationForm.targetLanguage = response.data.default_target_lang || 'en';
          }
        } catch (error) {
          this.translationForm.targetLanguage = 'en';
        }
      },

      async loadExistingTranslation() {
        // 确保有文章ID才执行数据库查询
        if (this.$common.isEmpty(this.id)) {
          this.resetTranslationForm();
          return;
        }

        try {
          const response = await this.$http.get(this.$constant.baseURL + "/article/getTranslation", {
            id: this.id,
            language: this.translationForm.targetLanguage
          });

          if (response.code === 200 && response.data && response.data.status === 'success') {
            this.translationForm.translatedTitle = response.data.title || '';
            this.translationForm.translatedContent = response.data.content || '';
          } else {
            // 该语言没有翻译内容，清空表单
            this.resetTranslationForm();
          }
        } catch (error) {
          // 加载失败，清空表单
          this.resetTranslationForm();
        }
      },

      async onTargetLanguageChange(newLanguage) {
        try {
          // 更新系统默认目标语言
          await this.updateDefaultTargetLanguage(newLanguage);

          if (this.restorePendingTranslation(newLanguage)) {
            return;
          }

          // 只有在文章已保存（有ID）时才加载翻译内容
          if (!this.$common.isEmpty(this.id)) {
            await this.loadExistingTranslation();
          } else {
            // 新文章或无ID，清空翻译表单
            this.resetTranslationForm();
          }
        } catch (error) {
          console.error('切换目标语言失败:', error);
          this.$message.error('切换目标语言失败: ' + error.message);
        }
      },

      async updateDefaultTargetLanguage(targetLanguage) {
        try {
          // 获取当前完整配置
          const getResponse = await this.$http.get(this.$constant.baseURL + "/webInfo/ai/config/translation/get");
          if (getResponse.code !== 200 || !getResponse.data) {
            this.$message.warning('获取翻译配置失败');
            return;
          }
          
          // 更新目标语言
          const config = getResponse.data;
          config.defaultTargetLang = targetLanguage;
          
          // 保存配置
          const response = await this.$http.post(this.$constant.baseURL + "/webInfo/ai/config/translation/save", config);

          if (response.code === 200) {
            this.$message.success('默认目标语言已更新为: ' + this.getLanguageName(targetLanguage));
          }
        } catch (error) {
        }
      },

      // 使用统一的后台管理语言映射工具（中文）
      getLanguageName: getAdminLanguageName,

      async saveTranslation() {
        // 验证表单
        if (!this.translationForm.translatedTitle.trim()) {
          this.$message.warning('请输入翻译标题');
          return;
        }

        if (!this.translationForm.translatedContent.trim()) {
          this.$message.warning('请输入翻译内容');
          return;
        }

        // 暂存翻译数据
        this.pendingTranslation = {
          title: this.translationForm.translatedTitle.trim(),
          content: this.translationForm.translatedContent.trim(),
          language: this.translationForm.targetLanguage
        };

        // 自动开启跳过AI翻译开关
        this.skipAiTranslation = true;

        // 显示成功消息
        this.$message.success('翻译内容已暂存，请保存文章以应用翻译');

        // 关闭弹窗
        this.closeTranslationDialog();
      },

      closeTranslationDialog() {
        this.translationDialogVisible = false;
        this.shouldRenderTranslationEditor = false;
        this.translationEditorReady = false;
        this.resetTranslationForm();
      },

      // 清空暂存的翻译数据
      clearPendingTranslation() {
        this.pendingTranslation = {
          title: '',
          content: '',
          language: ''
        };
      },

      // 移动端表单布局适配相关方法
      initMobileFormLayout() {
        this.$nextTick(() => {
          this.updateFormLabelPosition();
        });
      },

      handleWindowResize() {
        // 防抖处理
        if (this.resizeTimer) {
          clearTimeout(this.resizeTimer);
        }
        this.resizeTimer = setTimeout(() => {
          this.updateFormLabelPosition();
        }, 300);
      },

      updateFormLabelPosition() {
        const form = this.$refs.ruleForm;
        if (!form || !form.$el) return;

        const isMobile = window.innerWidth <= 768;
        
        if (isMobile) {
          form.$el.classList.add('el-form--label-top');
          form.$el.classList.remove('el-form--label-left');
        } else {
          form.$el.classList.add('el-form--label-left');
          form.$el.classList.remove('el-form--label-top');
        }
      },
      
      // 获取分类和标签
      // reloadArticle: 是否重新加载文章（默认true，创建分类/标签时传false）
      getSortAndLabel(reloadArticle = true) {
        return this.$http.get(this.$constant.baseURL + "/webInfo/listSortAndLabel")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.sorts = res.data.sorts;
              this.labels = res.data.labels;
              
              // 只在初始加载时重新获取文章，创建分类/标签时不重新加载
              if (reloadArticle && !this.$common.isEmpty(this.id)) {
                this.getArticleById();
              }
              return res.data;
            }
          })
          .catch((error) => {
            this.showError("获取分类和标签失败", error);
            throw error;
          });
      },
      
      // 获取当前分类名称
      getCurrentSortName() {
        if (this.article.sortId && this.sorts.length > 0) {
          const sort = this.sorts.find(s => s.id === this.article.sortId);
          return sort ? sort.sortName : '';
        }
        return '';
      },
      
      // 根据ID获取文章
      getArticleById(options = {}) {
        const { checkTranslationStatus = true } = options;
        return this.$http.get(this.$constant.baseURL + "/admin/article/getArticleById", {id: this.id})
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.article = res.data;
              // 检查文章是否有手动编辑的翻译，如果有则自动进入编辑翻译模式
              if (checkTranslationStatus) {
                this.checkAndSetTranslationMode();
              }
            }
            return res.data;
          })
          .catch((error) => {
            this.showError("获取文章失败", error);
            throw error;
          });
      },
      
      // 检查并设置翻译模式
      checkAndSetTranslationMode() {
        // 检查文章是否有可用的翻译语言
        this.$http.get(this.$constant.baseURL + "/article/getAvailableLanguages", {id: this.id})
          .then((res) => {
            if (res.code === 200 && res.data && res.data.length > 0) {
              // 如果文章有翻译，自动开启跳过AI翻译
              this.skipAiTranslation = true;
              
              // 显示提示信息
              this.$message({
                message: `检测到文章已有翻译版本（${res.data.join(', ')}），已自动开启跳过AI翻译`,
                type: 'info',
                duration: 3000
              });
            }
          })
          .catch((error) => {
            // 检查失败不影响正常编辑
          });
      },
      
      // 保存并等待（同步版本，阻塞等待所有任务完成）
      submitForm(formName) {
        this.syncMainEditorContent()
        this.$refs[formName].validate((valid) => {
          if (valid) {
            if (!this.$common.isEmpty(this.id)) {
              this.article.id = this.id;
            }
            
            // 使用同步接口，在当前页面等待所有任务完成
            this.saveArticleAndWait(this.article);
          }
        });
      },
      
      // 保存并离开（异步版本）
      submitFormAsync(formName) {
        this.syncMainEditorContent()
        this.$refs[formName].validate((valid) => {
          if (valid) {
            if (!this.$common.isEmpty(this.id)) {
              this.article.id = this.id;
            }
            
            this.saveArticleAsync(this.article);
          }
        });
      },
      
      // 同步保存文章
      saveArticle(article, url) {
        this.$confirm('确认保存文章？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success',
          center: true
        }).then(() => {
          // 显示加载中
          this.startLoading("保存文章中...");
          const payload = this.buildArticleRequestPayload(article);

          // 发送保存请求
          this.$http.post(this.$constant.baseURL + url, payload, true)
            .then(res => {
              this.stopLoading();
              
              // 记录完整响应用于调试
              
              // 检查保存是否成功
              if (res.code === 200 || res.data === true) {
                // 显示成功通知
                this.$message({
                  message: '文章保存成功，翻译将在后台自动完成',
                  type: 'success',
                  duration: 3000,
                  offset: 20
                });
                
                // 发布全局事件，通知首页刷新文章列表
                this.$root.$emit('articleSaved');
                
                // SEO推送提示（现在由后端异步处理）
                if (article.viewStatus && article.submitToSearchEngine) {
                  this.$message({
                    message: '文章保存成功，搜索引擎推送将在后台自动处理',
                    type: 'info',
                    duration: 3000,
                    offset: 80
                  });
                }
                
                // 清空暂存翻译数据
                this.clearPendingTranslation();

                // 更新ID以便后续编辑
                if (!this.id && res.data) {
                  this.id = res.data;
                  this.$router.replace({
                    path: "/postEdit",
                    query: {id: this.id}
                  });
                }
              } else {
                // 处理保存失败的情况
                this.handleSaveError(res);
              }
            })
            .catch(error => {
              this.stopLoading();
              console.error('保存文章网络请求失败:', error);
              this.showError("保存失败", error.message || "网络请求错误");
            });
        }).catch(() => {
          // 用户取消保存，无需任何操作
        });
      },
      
      // 保存文章并等待完成（使用同步接口）
      saveArticleAndWait(article) {
        this.$confirm('文章将被保存，请等待所有处理完成后再进行其他操作。', '确认保存并等待', {
          confirmButtonText: '保存',
          cancelButtonText: '取消',
          type: 'info',
          center: true
        }).then(() => {
          // 显示加载状态
          this.startLoading("正在保存文章...");
          
          // 记录保存请求数据
          
          // 根据是否有id选择不同的同步接口
          let url = this.$common.isEmpty(this.id)
            ? "/article/saveArticle"
            : "/article/updateArticle";
          const payload = this.buildArticleRequestPayload(article);

          // 发送同步保存请求
          this.$http.post(this.$constant.baseURL + url, payload, true)
            .then(res => {
              // 记录响应
              
              // 检查保存是否成功
              if (res.code === 200) {
                this.stopLoading();
                
                // 显示成功通知
                this.$message({
                  message: '文章保存成功！所有任务已完成（翻译、摘要生成等）',
                  type: 'success',
                  duration: 3000,
                  offset: 20
                });
                
                // 发布全局事件，通知首页刷新文章列表
                this.$root.$emit('articleSaved');
                
                // 清空暂存翻译数据
                this.clearPendingTranslation();

                // 延迟跳转到文章列表，给用户时间看到成功提示
                setTimeout(() => {
                  this.$router.push({path: "/postList"});
                }, 1500);
              } else {
                this.stopLoading();
                // 处理保存失败的情况
                console.error('保存失败:', res);
                this.handleSaveError(res);
              }
            })
            .catch(error => {
              this.stopLoading();
              console.error('保存请求失败:', error);
              this.showError("保存失败", error.message || "网络请求错误");
            });
        }).catch(() => {
          // 用户取消
        });
      },
      
      // 更新 loading 提示文本
      updateLoadingText(text) {
        if (this.loading) {
          this.loading.text = text;
        }
      },
      
      // 显示 loading
      startLoading(text) {
        this.loading = this.$loading({
          lock: true,
          text: text || '加载中...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
      },
      
      // 停止 loading
      stopLoading() {
        if (this.loading) {
          this.loading.close();
          this.loading = null;
        }
      },
      
      // 显示错误消息
      showError(title, message) {
        this.$message({
          message: `${title}: ${message}`,
          type: 'error',
          duration: 5000
        });
      },
      
      // 处理保存错误
      handleSaveError(res) {
        const errorMessage = res.message || res.msg || '保存失败';
        this.$message({
          message: errorMessage,
          type: 'error',
          duration: 5000
        });
      },
      
      // 异步保存文章
      saveArticleAsync(article) {
        this.$confirm('文章将在后台保存，您可以立即返回文章列表，保存状态会显示在右侧通知中。', '确认异步保存', {
          confirmButtonText: '保存并离开',
          cancelButtonText: '取消',
          type: 'info',
          center: true
        }).then(() => {
          this.asyncSaveLoading = true;
          
          // 记录保存请求数据
          
          // 根据是否有id选择不同的异步接口
          let url = this.$common.isEmpty(this.id)
            ? "/article/saveArticleAsync"
            : "/article/updateArticleAsync";
          const payload = this.buildArticleRequestPayload(article);

          // 发送异步保存请求
          this.$http.post(this.$constant.baseURL + url, payload, true)
            .then(res => {
              this.asyncSaveLoading = false;
              
              // 记录响应
              
              if (res.code === 200 && res.data) {
                // 获取任务ID
                this.currentTaskId = res.data;
                
                // 发布全局事件，通知首页刷新文章列表
                this.$root.$emit('articleSaved');
                
                // 添加通知（会自动启动轮询）
                this.$notify.loading('保存文章', '正在保存文章，请稍候...', this.currentTaskId);

                // 清空暂存翻译数据
                this.clearPendingTranslation();

                // 延迟跳转，确保全局通知组件已接管轮询
                setTimeout(() => {
                  this.$router.push({path: "/postList"});
                }, 1000);
              } else {
                console.error('异步保存失败:', res);
                this.handleSaveError(res);
              }
            })
            .catch(error => {
              this.asyncSaveLoading = false;
              console.error('异步保存请求失败:', error);
              this.showError("启动异步保存失败", error.message || "网络请求错误");
            });
        }).catch(() => {
          // 用户取消
        });
      },
      
      // 图片上传处理（适配 Vditor）
      imgAdd(payload) {
        try {
          const uploadPayload = payload && payload.file ? payload : { file: payload, uploadId: null };
          const file = uploadPayload.file;
          const uploadId = uploadPayload.uploadId || null;
          if (!file) {
            this.showError("图片上传准备失败", "未获取到上传文件");
            return;
          }

          // 显示上传中提示
          const loadingMessage = this.$message({
            message: '正在上传图片...',
            type: 'info',
            iconClass: 'el-icon-loading',
            duration: 0, // 不自动关闭
            showClose: true
          });

          let suffix = file.name.lastIndexOf('.') !== -1 ? file.name.substring(file.name.lastIndexOf('.')) : "";
          let key = "articlePicture" + "/" + this.mainStore.currentAdmin.username.replace(/[^a-zA-Z]/g, '') 
                    + this.mainStore.currentAdmin.id + new Date().getTime() 
                    + Math.floor(Math.random() * 1000) + suffix;

          // 获取当前存储类型，优先使用更新后的配置
          let storeType = this.currentStoreType || this.mainStore.sysConfig['store.type'] || "local";

          let fd = new FormData();
          fd.append("file", file);
          fd.append("originalName", file.name);
          fd.append("key", key);
          fd.append("relativePath", key);
          fd.append("type", "articlePicture");
          fd.append("storeType", storeType);

          if (storeType === "local") {
            this.saveLocal(fd, loadingMessage, uploadId);
          } else if (storeType === "qiniu") {
            this.saveQiniu(fd, loadingMessage, uploadId);
          } else if (storeType === "lsky") {
            this.saveLsky(fd, loadingMessage, uploadId);
          } else if (storeType === "easyimage") {
            this.saveLsky(fd, loadingMessage, uploadId);
          }
        } catch (error) {
          this.showError("图片上传准备失败", error);
        }
      },
      
      // 本地保存图片
      saveLocal(fd, loadingMessage, uploadId) {
        this.$http.upload(this.$constant.baseURL + "/resource/upload", fd, true)
          .then((res) => {
            // 关闭上传中提示
            if (loadingMessage) loadingMessage.close();
            
            if (!this.$common.isEmpty(res.data)) {
              this.insertImage(res.data, fd.get("file").name, uploadId);
              this.$message.success('图片上传成功');
            } else {
              this.rejectImageUpload(uploadId);
              this.showError("图片上传失败", "服务器未返回有效的图片URL");
            }
          })
          .catch((error) => {
            // 关闭上传中提示
            if (loadingMessage) loadingMessage.close();
            this.rejectImageUpload(uploadId);
            this.showError("图片本地上传失败", error);
          });
      },
      
      // 插入图片到编辑器
      insertImage(url, filename, uploadId = null) {
        // 智能处理图片URL：开发环境使用完整URL，生产环境使用相对路径
        let fullUrl = url;
        if (url.startsWith('/')) {
          // 开发环境（非生产模式）：前后端端口不同，需要完整URL
          // 生产环境：前后端同域，使用相对路径由Nginx代理
          if (!import.meta.env.PROD) {
            fullUrl = this.$constant.baseURL + url;
          }
        }
        
        // 过滤文件名中的括号，防止破坏Markdown语法
        const safeFilename = (filename || '图片').replace(/[\[\]\(\)]/g, '');
        const markdown = `![${safeFilename}](${fullUrl})\n`;
        
        if (this.$refs.md) {
          if (uploadId && typeof this.$refs.md.resolveImageUpload === 'function') {
            const inserted = this.$refs.md.resolveImageUpload(uploadId, markdown);
            if (inserted) {
              return;
            }
          }
          this.$refs.md.insertValue(markdown);
        }
      },
      rejectImageUpload(uploadId) {
        if (!uploadId || !this.$refs.md || typeof this.$refs.md.rejectImageUpload !== 'function') {
          return;
        }
        this.$refs.md.rejectImageUpload(uploadId);
      },
      
      // 七牛云保存图片
      saveQiniu(fd, loadingMessage, uploadId) {
        this.$http.get(this.$constant.baseURL + "/qiniu/getUpToken", {key: fd.get("key")}, true)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              fd.append("token", res.data);

              this.$http.uploadQiniu(this.mainStore.sysConfig.qiniuUrl, fd)
                .then((res) => {
                  // 关闭上传中提示
                  if (loadingMessage) loadingMessage.close();
                  
                  if (!this.$common.isEmpty(res.key)) {
                    let url = this.mainStore.sysConfig['qiniu.downloadUrl'] + res.key;
                    let file = fd.get("file");
                    this.$common.saveResource(this, "articlePicture", url, file.size, file.type, file.name, "qiniu", true);
                    this.insertImage(url, file.name, uploadId);
                    this.$message.success('图片上传成功');
                  } else {
                    this.rejectImageUpload(uploadId);
                    this.showError("七牛云上传失败", "未返回有效的图片密钥");
                  }
                })
                .catch((error) => {
                  // 关闭上传中提示
                  if (loadingMessage) loadingMessage.close();
                  this.rejectImageUpload(uploadId);
                  this.showError("七牛云上传请求失败", error);
                });
            } else {
              // 关闭上传中提示
              if (loadingMessage) loadingMessage.close();
              this.rejectImageUpload(uploadId);
              this.showError("获取七牛云上传Token失败", "服务器未返回有效的Token");
            }
          })
          .catch((error) => {
            // 关闭上传中提示
            if (loadingMessage) loadingMessage.close();
            this.rejectImageUpload(uploadId);
            this.showError("获取七牛云上传Token失败", error);
          });
      },
      
      // 兰空图床保存图片
      saveLsky(fd, loadingMessage, uploadId) {
        this.$http.post(this.$constant.baseURL + "/resource/upload", fd, true)
          .then((res) => {
            // 关闭上传中提示
            if (loadingMessage) loadingMessage.close();
            
            if (!this.$common.isEmpty(res.data)) {
              // 获取返回的图片URL
              let url = res.data;
              let file = fd.get("file");
              let storeType = fd.get("storeType") || "lsky";
              this.$common.saveResource(this, "articlePicture", url, file.size, file.type, file.name, storeType, true);
              this.insertImage(url, file.name, uploadId);
              this.$message.success('图片上传成功');
            } else {
              this.rejectImageUpload(uploadId);
              this.showError("图床上传失败", "服务器未返回有效的图片URL");
            }
          })
          .catch((error) => {
            // 关闭上传中提示
            if (loadingMessage) loadingMessage.close();
            this.rejectImageUpload(uploadId);
            this.showError("图床上传失败", error);
          });
      },
      
      // 添加文章封面（兼容两种命名）
      addCover() {
        this.$refs.uploadPicture.change(1);
      },
      
      addArticleCover(res) {
        this.article.articleCover = res;
      },
      
      // 重置表单
      resetForm(formName) {
        this.$refs[formName].resetFields();
        if (!this.$common.isEmpty(this.id)) {
          this.getArticleById();
        }
      },
      
      // 处理系统配置更新事件
      handleSysConfigUpdate(config) {
        if (config && config['store.type']) {
          this.currentStoreType = config['store.type'];
        }
      },
      
      // 创建新分类
      createNewSort() {
        this.$refs.newSortForm.validate((valid) => {
          if (valid) {
            this.newSortLoading = true;
            this.$http.post(this.$constant.baseURL + "/webInfo/saveSort", this.newSortForm)
              .then((res) => {
                this.newSortLoading = false;
                if (res.code === 200) {
                  this.$message.success('分类创建成功');
                  
                  // 保存新分类名称
                  const newSortName = this.newSortForm.sortName;
                  
                  // 重新获取分类列表并自动选中新创建的分类
                  // 传入false，不重新加载文章
                  this.getSortAndLabel(false).then(() => {
                    // 自动选中新创建的分类
                    const newSort = this.sorts.find(sort => sort.sortName === newSortName);
                    if (newSort) {
                      // 使用$nextTick确保在下一个tick中设置，避免watch干扰
                      this.$nextTick(() => {
                        this.article.sortId = newSort.id;
                        // 手动更新labelsTemp
                        this.labelsTemp = this.labels.filter(l => l.sortId === newSort.id);
                      });
                    } else {
                    }
                  });
                  
                  // 关闭对话框
                  this.cancelNewSort();
                }
              })
              .catch((error) => {
                this.newSortLoading = false;
                this.showError("创建分类失败", error);
              });
          }
        });
      },
      
      // 创建新标签
      createNewLabel() {
        this.$refs.newLabelForm.validate((valid) => {
          if (valid) {
            this.newLabelLoading = true;
            const labelData = {
              ...this.newLabelForm,
              sortId: this.article.sortId
            };
            
            // 保存标签名称，用于后续查找（防止对话框关闭后数据丢失）
            const createdLabelName = labelData.labelName;
            const createdSortId = labelData.sortId;
            
            this.$http.post(this.$constant.baseURL + "/webInfo/saveLabel", labelData)
              .then((res) => {
                this.newLabelLoading = false;
                if (res.code === 200) {
                  this.$message.success('标签创建成功');
                  
                  // 关闭对话框（在重新加载数据之前关闭）
                  this.cancelNewLabel();
                  
                  // 重新获取分类和标签列表并自动选中新创建的标签
                  // 传入false，不重新加载文章
                  this.getSortAndLabel(false).then(() => {
                    
                    // 使用$nextTick确保DOM更新完成
                    this.$nextTick(() => {
                      // 强制刷新labelsTemp，确保下拉框显示新标签
                      this.labelsTemp = this.labels.filter(l => l.sortId === createdSortId);
                      
                      // 自动选中新创建的标签（使用保存的值）
                      const newLabel = this.labels.find(label => 
                        label.labelName === createdLabelName && 
                        label.sortId === createdSortId
                      );
                      
                      if (newLabel) {
                        // 延迟设置，确保不被watch干扰
                        setTimeout(() => {
                          this.article.labelId = newLabel.id;
                        }, 100);
                      } else {
                      }
                    });
                  });
                }
              })
              .catch((error) => {
                this.newLabelLoading = false;
                this.showError("创建标签失败", error);
              });
          }
        });
      },
      
      // 辅助方法：显示加载中
      startLoading(text = "加载中...") {
        if (this.loading) {
          this.loading.close();
        }
        this.loading = this.$loading({
          lock: true,
          text: text,
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
      },
      
      // 辅助方法：停止加载
      stopLoading() {
        if (this.loading) {
          this.loading.close();
          this.loading = null;
        }
      },
      
      // 辅助方法：显示成功通知
      showSuccess(title, message) {
        this.$message({
          message: message,
          type: 'success',
          offset: 50
        });
      },
      
      // 显示错误信息
      showError(title, error) {
        let errorMessage = typeof error === 'string' 
          ? error 
          : (error && error.message ? error.message : '未知错误');
        
        console.error(title + ':', errorMessage);
        
        this.$message({
          message: errorMessage,
          type: 'error',
          offset: 50
        });
      },
      
      // 处理分类选择变化
      handleSortChange(value) {
        if (value === 'new-sort') {
          // 重置分类选择
          this.article.sortId = null;
          // 打开新建分类对话框
          this.openNewSortDialog();
        }
      },
      
      // 处理标签选择变化
      handleLabelChange(value) {
        if (value === 'new-label') {
          // 重置标签选择
          this.article.labelId = null;
          // 打开新建标签对话框
          this.openNewLabelDialog();
        }
      },
      
      // 打开新建分类对话框
      openNewSortDialog() {
        this.newSortForm = {
          sortName: '',
          sortDescription: '',
          priority: 1,
          sortType: 0  // 默认为导航栏分类，会显示在侧边栏"速览"中
        };
        this.newSortDialog = true;
        // 清除表单验证
        this.$nextTick(() => {
          if (this.$refs.newSortForm) {
            this.$refs.newSortForm.clearValidate();
          }
        });
      },
      
      // 取消新建分类
      cancelNewSort() {
        this.newSortDialog = false;
        this.newSortForm = {
          sortName: '',
          sortDescription: '',
          priority: 1,
          sortType: 0
        };
      },
      
      // 打开新建标签对话框
      openNewLabelDialog() {
        if (!this.article.sortId) {
          this.$message({
            message: '请先选择分类',
            type: 'warning'
          });
          return;
        }
        
        this.newLabelForm = {
          labelName: '',
          labelDescription: '',
          sortId: this.article.sortId
        };
        this.newLabelDialog = true;
        // 清除表单验证
        this.$nextTick(() => {
          if (this.$refs.newLabelForm) {
            this.$refs.newLabelForm.clearValidate();
          }
        });
      },
      
      // 取消新建标签
      cancelNewLabel() {
        this.newLabelDialog = false;
        this.newLabelForm = {
          labelName: '',
          labelDescription: '',
          sortId: null
        };
      },
      
      // 获取当前选中分类的名称
      getCurrentSortName() {
        if (!this.article.sortId) return '';
        const sort = this.sorts.find(s => s.id === this.article.sortId);
        return sort ? sort.sortName : '';
      },
      
      // 统一的保存错误处理
      handleSaveError(res) {
        // 详细记录错误
        console.error('保存文章失败，响应:', JSON.stringify(res, null, 2));
        
        // 提取错误消息
        let errorMsg = '服务器返回未知错误';
        if (res.message) {
          errorMsg = res.message;
        } else if (typeof res.data === 'string') {
          errorMsg = res.data;
        } else if (res.data && res.data.message) {
          errorMsg = res.data.message;
        }
        
        this.showError("保存失败", errorMsg);
      }
    }
  }
</script>

<style scoped src="@/assets/css/postedit.css"></style>
<style src="@/assets/css/postedit-global.css"></style>
