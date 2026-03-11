<template>
  <div class="article-editor">
    <component
      :is="currentComponent"
      ref="inner"
      :value="value"
      :placeholder="placeholder"
      :height="height"
      :mode="mode"
      :toolbar-config="toolbarConfig"
      :upload="upload"
      @input="forwardInput"
      @change="forwardChange"
      @ready="forwardReady"
      @image-add="forwardImageAdd"
      @focus="forwardFocus"
      @blur="forwardBlur"
      @save="forwardSave"
    />
  </div>
</template>

<script>
const VditorEditor = () => import('@/components/VditorEditor.vue')
const SplitPreviewMarkdownEditor = () => import('@/components/editors/SplitPreviewMarkdownEditor.vue')
const IRMarkdownEditor = () => import('@/components/editors/IRMarkdownEditor.vue')
const WYSIWYGMarkdownEditor = () => import('@/components/editors/WYSIWYGMarkdownEditor.vue')

export default {
  name: 'ArticleEditor',
  components: {
    VditorEditor,
    SplitPreviewMarkdownEditor,
    IRMarkdownEditor,
    WYSIWYGMarkdownEditor,
  },
  props: {
    value: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: '请输入内容...',
    },
    height: {
      type: [String, Number],
      default: 600,
    },
    mode: {
      type: String,
      default: 'ir',
    },
    toolbarConfig: {
      type: Object,
      default: () => ({}),
    },
    upload: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      activeKey: 'vditor',
      // 支持的编辑器类型
      // vditor: Vditor 编辑器
      // split_preview: 分屏预览 Markdown 编辑器（左编辑右预览）
      // ir: 自实现即时渲染编辑器
      // wysiwyg: 自实现所见即所得编辑器
    }
  },
  computed: {
    currentComponent() {
      switch (this.activeKey) {
        case 'split_preview':
          return 'SplitPreviewMarkdownEditor'
        case 'ir':
          return 'IRMarkdownEditor'
        case 'wysiwyg':
          return 'WYSIWYGMarkdownEditor'
        default:
          return 'VditorEditor'
      }
    },
  },
  created() {
    this.loadActiveFromCache()
    this.fetchActivePlugin()
  },
  methods: {
    loadActiveFromCache() {
      let cached = localStorage.getItem('activeEditorPluginKey')
      if (['vditor', 'split_preview', 'ir', 'wysiwyg'].includes(cached)) {
        this.activeKey = cached
      }
    },
    cacheActive() {
      localStorage.setItem('activeEditorPluginKey', this.activeKey)
    },
    fetchActivePlugin() {
      const base = this.$constant.baseURL
      this.$http.get(base + '/sysPlugin/getActivePlugin', { pluginType: 'editor' }, true)
        .then((res) => {
          const active = res && res.code === 200 ? res.data : null
          if (active && ['vditor', 'split_preview', 'ir', 'wysiwyg'].includes(active.pluginKey)) {
            this.activeKey = active.pluginKey
            this.cacheActive()
          }
        })
        .catch(() => {})
    },
    forwardInput(val) {
      this.$emit('input', val)
    },
    forwardChange(val) {
      this.$emit('change', val)
    },
    forwardReady(editor) {
      this.$emit('ready', editor)
    },
    forwardImageAdd(file) {
      this.$emit('image-add', file)
    },
    forwardFocus() {
      this.$emit('focus')
    },
    forwardBlur() {
      this.$emit('blur')
    },
    forwardSave() {
      this.$emit('save')
    },
    /**
     * 切换编辑器类型
     * @param {string} editorKey - 编辑器类型：vditor, split_preview, ir, wysiwyg
     */
    switchEditor(editorKey) {
      if (['vditor', 'split_preview', 'ir', 'wysiwyg'].includes(editorKey)) {
        this.activeKey = editorKey
        this.cacheActive()
      }
    },
    /**
     * 获取当前编辑器类型
     * @returns {string}
     */
    getEditorType() {
      return this.activeKey
    },
    insertValue(text) {
      const inner = this.$refs.inner
      if (inner && typeof inner.insertValue === 'function') {
        inner.insertValue(text)
      }
    },
    resolveImageUpload(uploadId, text) {
      const inner = this.$refs.inner
      if (inner && typeof inner.resolveImageUpload === 'function') {
        const result = inner.resolveImageUpload(uploadId, text)
        return result !== false
      }
      if (inner && typeof inner.insertValue === 'function') {
        inner.insertValue(text)
        return true
      }
      return false
    },
    rejectImageUpload(uploadId) {
      const inner = this.$refs.inner
      if (inner && typeof inner.rejectImageUpload === 'function') {
        return inner.rejectImageUpload(uploadId)
      }
      return false
    },
    focus() {
      const inner = this.$refs.inner
      if (inner && typeof inner.focus === 'function') {
        inner.focus()
      }
    },
    blur() {
      const inner = this.$refs.inner
      if (inner && typeof inner.blur === 'function') {
        inner.blur()
      }
    },
    getValue() {
      const inner = this.$refs.inner
      if (inner && typeof inner.getValue === 'function') {
        return inner.getValue()
      }
      return this.value || ''
    },
    setValue(val) {
      const inner = this.$refs.inner
      if (inner && typeof inner.setValue === 'function') {
        inner.setValue(val)
      } else {
        this.$emit('input', val)
        this.$emit('change', val)
      }
    },
  },
}
</script>

<style scoped>
.article-editor {
  width: 100%;
}
</style>
