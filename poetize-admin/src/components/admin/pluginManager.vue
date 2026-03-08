<template>
  <div>
    <div class="crumbs">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item><i class="el-icon-menu"></i> 插件管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ currentPluginTypeLabel }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="container">
      <div v-if="routeSearchKeyword" class="search-tip">
        <span>当前显示的是全局搜索结果：{{ routeSearchKeyword }}</span>
        <el-button type="text" @click="clearGlobalSearchFilter">清除全局筛选</el-button>
      </div>

      <plugin-type-toolbar
        v-model="currentPluginType"
        @change="handleTypeChange"
        @create="handleCreate"
        @install="installDialogVisible = true" />

      <plugin-table
        :table-data="filteredTableData"
        :loading="loading"
        :current-plugin-type="currentPluginType"
        :active-plugin-key="activePluginKey"
        @set-active="handleSetActive"
        @edit="handleEdit"
        @toggle-status="handleStatus"
        @delete="handleDelete" />

      <mouse-effect-settings-panel v-if="currentPluginType === 'mouse_click_effect'" />

      <plugin-edit-dialog
        ref="editDialog"
        :visible.sync="editVisible"
        :dialog-title="dialogTitle"
        :form="form"
        :rules="rules"
        :is-edit="isEdit"
        :current-plugin-type="currentPluginType"
        :is-schema-driven-plugin-type="isSchemaDrivenPluginType"
        :waifu-config="waifuConfig"
        :theme-config="themeConfig"
        :payment-config="paymentConfig"
        :payment-config-schema="paymentConfigSchema"
        :ai-tool-form="aiToolForm"
        :ai-tool-editor-mode="aiToolEditorMode"
        :ai-tool-assistant-prompt="aiToolAssistantPrompt"
        :plugin-code-editor-meta="pluginCodeEditorMeta"
        :ai-prompt-dialog="aiPromptDialog"
        :ai-prompt-visible="aiPromptVisible"
        :preview-image="currentWaifuPreviewImage"
        @save="saveEdit"
        @apply-theme-preset="handleThemePreset"
        @update:ai-prompt-visible="aiPromptVisible = $event" />

      <plugin-install-dialog
        :visible.sync="installDialogVisible"
        @installed="refreshData" />
    </div>
  </div>
</template>

<script>
import { clearEditorThemeCache, initEditorTheme } from '@/utils/useEditorTheme';
import PluginTypeToolbar from '@/components/admin/pluginManager/PluginTypeToolbar.vue';
import PluginTable from '@/components/admin/pluginManager/PluginTable.vue';
import MouseEffectSettingsPanel from '@/components/admin/pluginManager/MouseEffectSettingsPanel.vue';
import PluginEditDialog from '@/components/admin/pluginManager/PluginEditDialog.vue';
import PluginInstallDialog from '@/components/admin/pluginManager/PluginInstallDialog.vue';
import {
  AI_TOOL_ASSISTANT_PROMPT,
  PLUGIN_TYPES,
  PLUGIN_TYPE_LABELS,
  getAiPromptDialog,
  getPluginCodeEditorMeta,
  getPreviewImage
} from '@/components/admin/pluginManager/pluginManagerTransforms';
import {
  applyThemePreset,
  createPluginEditState,
  initializeCreateState,
  initializeEditState,
  prepareFormForSave
} from '@/components/admin/pluginManager/usePluginEditState';
import {
  createPluginManagerListState,
  deletePlugin,
  loadPluginListData,
  setActivePlugin,
  togglePluginStatus
} from '@/components/admin/pluginManager/usePluginManagerList';

export default {
  name: 'pluginManager',
  components: {
    PluginTypeToolbar,
    PluginTable,
    MouseEffectSettingsPanel,
    PluginEditDialog,
    PluginInstallDialog
  },
  watch: {
    '$route.query.type': {
      handler(val) {
        const t = String(val || '');
        if (PLUGIN_TYPES.includes(t) && t !== this.currentPluginType) {
          this.currentPluginType = t;
          this.form.pluginType = t;
          this.refreshData();
        }
      }
    }
  },
  data() {
    return {
      ...createPluginManagerListState(),
      ...createPluginEditState(),
      installDialogVisible: false
    };
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑插件' : '新增插件';
    },
    currentPluginTypeLabel() {
      return PLUGIN_TYPE_LABELS[this.currentPluginType] || '插件管理';
    },
    routeSearchKeyword() {
      return ((this.$route.query.search || '') + '').toLowerCase().replace(/\s+/g, '').trim();
    },
    filteredTableData() {
      if (!this.routeSearchKeyword) {
        return this.tableData;
      }

      return this.tableData.filter((item) => {
        return [item.pluginName, item.pluginKey, item.pluginDescription].some((value) => {
          return ((value || '') + '').toLowerCase().replace(/\s+/g, '').includes(this.routeSearchKeyword);
        });
      });
    },
    isSchemaDrivenPluginType() {
      return ['payment'].includes(this.currentPluginType);
    },
    aiToolAssistantPrompt() {
      return AI_TOOL_ASSISTANT_PROMPT;
    },
    aiPromptDialog() {
      return getAiPromptDialog(this.currentPluginType);
    },
    pluginCodeEditorMeta() {
      return getPluginCodeEditorMeta(this.currentPluginType);
    },
    currentWaifuPreviewImage() {
      return getPreviewImage(this.form);
    }
  },
  created() {
    const t = String(this.$route?.query?.type || '');
    if (PLUGIN_TYPES.includes(t)) {
      this.currentPluginType = t;
      this.form.pluginType = t;
    }
    this.refreshData();
  },
  methods: {
    refreshData() {
      loadPluginListData(this);
    },
    handleTypeChange() {
      if (this.$router && this.$route) {
        const nextQuery = { ...this.$route.query, type: this.currentPluginType };
        delete nextQuery.search;
        this.$router.replace({ name: 'pluginManager', query: nextQuery });
      }
      this.refreshData();
    },
    clearGlobalSearchFilter() {
      const nextQuery = { ...this.$route.query };
      delete nextQuery.search;
      this.$router.replace({ path: this.$route.path, query: nextQuery });
    },
    openEditDialog() {
      this.editVisible = true;
      this.$nextTick(() => {
        if (this.$refs.editDialog) {
          this.$refs.editDialog.clearValidate();
        }
      });
    },
    handleCreate() {
      initializeCreateState(this, this.currentPluginType);
      this.openEditDialog();
    },
    handleEdit(row) {
      initializeEditState(this, row, this.currentPluginType);
      this.openEditDialog();
    },
    handleSetActive(row) {
      setActivePlugin(this, row);
    },
    handleStatus(row, enabled) {
      togglePluginStatus(this, row, enabled);
    },
    handleDelete(row) {
      deletePlugin(this, row);
    },
    handleThemePreset(presetKey) {
      if (applyThemePreset(this, presetKey)) {
        this.$message.success(`已应用「${presetKey}」预设`);
      }
    },
    saveEdit() {
      try {
        prepareFormForSave(this);
      } catch (error) {
        this.$message.error(error.message || '配置格式有误');
        return;
      }

      const url = this.$constant.baseURL + (this.isEdit ? '/sysPlugin/updatePlugin' : '/sysPlugin/addPlugin');
      this.$http.post(url, this.form, true)
        .then(() => {
          this.$message.success(this.isEdit ? '修改成功' : '新增成功');
          this.editVisible = false;
          this.refreshData();
          if (this.currentPluginType === 'article_theme') {
            clearEditorThemeCache();
            initEditorTheme();
          }
        })
        .catch((error) => {
          this.$message.error(error.message);
        });
    }
  }
}
</script>

<style scoped>
.search-tip {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f4f8ff;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
