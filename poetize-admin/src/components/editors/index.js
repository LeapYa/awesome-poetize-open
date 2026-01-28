/**
 * @file 自实现编辑器模块索引
 * @description 导出所有编辑器相关的模块
 */

// 核心模块
export { DocumentModel } from './core/DocumentModel';
export { CursorManager, Position, Selection } from './core/CursorManager';
export { HistoryManager, OperationType } from './core/HistoryManager';

// 输入处理器
export { KeyboardHandler } from './input/KeyboardHandler';
export { IMEHandler } from './input/IMEHandler';
export { MouseHandler } from './input/MouseHandler';
export { ClipboardHandler } from './input/ClipboardHandler';

// 渲染器
export { IRRenderer, BlockType } from './render/IRRenderer';

// Vue 组件（懒加载）
export const IRMarkdownEditor = () => import('./IRMarkdownEditor.vue');
export const SplitPreviewMarkdownEditor = () => import('./SplitPreviewMarkdownEditor.vue');

/**
 * 编辑器类型常量（plugin_key）
 */
export const EditorTypes = {
  VDITOR: 'vditor',
  SPLIT_PREVIEW: 'split_preview',   // 分屏预览编辑器 SplitPreviewMarkdownEditor
  IR: 'ir',
  WYSIWYG: 'wysiwyg',
};
