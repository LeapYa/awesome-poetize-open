import {
  buildAiToolManifest,
  createAiToolConfigState,
  createDefaultAiToolEditorMode,
  createDefaultAiToolForm,
  createDefaultForm,
  createDefaultThemeConfig,
  createDefaultWaifuConfig,
  createEditorRules,
  createSchemaDrivenConfigState,
  getThemePresetConfig,
  normalizeJsonText,
  parseThemeConfig,
  parseWaifuConfig,
  serializeThemeConfig,
  serializeWaifuConfig
} from './pluginManagerTransforms';

export function createPluginEditState() {
  return {
    editVisible: false,
    isEdit: false,
    aiPromptVisible: false,
    form: createDefaultForm(),
    waifuConfig: createDefaultWaifuConfig(),
    themeConfig: createDefaultThemeConfig(),
    paymentConfig: {},
    paymentConfigSchema: {},
    aiToolEditorMode: createDefaultAiToolEditorMode(),
    aiToolForm: createDefaultAiToolForm(),
    rules: createEditorRules()
  };
}

export function resetSchemaDrivenConfigState(vm) {
  vm.paymentConfigSchema = {};
  vm.paymentConfig = {};
}

export function initializeCreateState(vm, pluginType) {
  vm.isEdit = false;
  vm.form = createDefaultForm(pluginType);
  vm.waifuConfig = createDefaultWaifuConfig();
  vm.themeConfig = createDefaultThemeConfig();
  vm.aiToolEditorMode = createDefaultAiToolEditorMode();
  vm.aiToolForm = createDefaultAiToolForm();
  resetSchemaDrivenConfigState(vm);
}

export function initializeEditState(vm, row, pluginType) {
  vm.isEdit = true;
  vm.form = JSON.parse(JSON.stringify(row));
  if (!vm.form.pluginConfig) {
    vm.form.pluginConfig = '{}';
  }

  vm.waifuConfig = pluginType === 'waifu_model'
    ? parseWaifuConfig(vm.form.pluginConfig)
    : createDefaultWaifuConfig();

  vm.themeConfig = pluginType === 'article_theme'
    ? parseThemeConfig(vm.form.pluginConfig)
    : createDefaultThemeConfig();

  if (pluginType === 'ai_tool') {
    const aiToolState = createAiToolConfigState(row, vm.form.pluginConfig);
    vm.aiToolEditorMode = aiToolState.aiToolEditorMode;
    vm.aiToolForm = aiToolState.aiToolForm;
  } else {
    vm.aiToolEditorMode = createDefaultAiToolEditorMode();
    vm.aiToolForm = createDefaultAiToolForm();
  }

  if (pluginType === 'payment') {
    const schemaState = createSchemaDrivenConfigState(row, vm.form.pluginConfig);
    vm.paymentConfigSchema = schemaState.paymentConfigSchema;
    vm.paymentConfig = schemaState.paymentConfig;
  } else {
    resetSchemaDrivenConfigState(vm);
  }
}

export function applyThemePreset(vm, presetKey) {
  const preset = getThemePresetConfig(presetKey);
  if (!preset) return false;
  vm.themeConfig = preset;
  return true;
}

export function prepareFormForSave(vm) {
  if (vm.currentPluginType === 'waifu_model') {
    vm.form.pluginConfig = serializeWaifuConfig(vm.waifuConfig);
  }

  if (vm.currentPluginType === 'article_theme') {
    vm.form.pluginConfig = serializeThemeConfig(vm.themeConfig);
  }

  if (vm.currentPluginType === 'ai_tool') {
    vm.form.manifest = JSON.stringify(buildAiToolManifest({
      form: vm.form,
      aiToolForm: vm.aiToolForm,
      aiToolEditorMode: vm.aiToolEditorMode
    }));
    vm.form.pluginConfig = normalizeJsonText(vm.aiToolForm.pluginConfigText, '工具配置');
  }

  if (vm.isSchemaDrivenPluginType && Object.keys(vm.paymentConfigSchema).length > 0) {
    vm.form.pluginConfig = JSON.stringify(vm.paymentConfig);
  }
}
