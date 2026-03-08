<template>
  <el-table :data="tableData" border class="table" header-cell-class-name="table-header" v-loading="loading">
    <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>

    <el-table-column v-if="currentPluginType === 'waifu_model'" label="预览" width="80" align="center">
      <template slot-scope="scope">
        <el-tooltip :content="getModelPath(scope.row)" placement="top">
          <el-avatar
            :size="50"
            :src="getPreviewImage(scope.row)"
            style="cursor: pointer; border: 2px solid #e0e0e0;">
            <i class="el-icon-s-custom"></i>
          </el-avatar>
        </el-tooltip>
      </template>
    </el-table-column>

    <el-table-column prop="pluginName" label="插件名称" min-width="120"></el-table-column>
    <el-table-column prop="pluginKey" label="标识符(Key)" width="150"></el-table-column>
    <el-table-column prop="pluginDescription" label="描述" min-width="200" show-overflow-tooltip></el-table-column>

    <el-table-column label="状态" width="120" align="center">
      <template slot-scope="scope">
        <el-tag effect="dark" type="success" v-if="isPluginInUse(scope.row)">
          使用中
        </el-tag>
        <el-tag :type="scope.row.enabled ? 'info' : 'danger'" v-else>
          {{ scope.row.enabled ? '已启用' : '已禁用' }}
        </el-tag>
      </template>
    </el-table-column>

    <el-table-column label="操作" width="280" align="center">
      <template slot-scope="scope">
        <el-button
          v-if="currentPluginType === 'waifu_model' && !isPluginInUse(scope.row)"
          type="text"
          icon="el-icon-star-on"
          class="primary-text"
          @click="$emit('set-active', scope.row)">使用此模型</el-button>

        <el-button
          v-if="currentPluginType === 'editor' && !isPluginInUse(scope.row) && scope.row.enabled"
          type="text"
          icon="el-icon-edit-outline"
          class="primary-text"
          @click="$emit('set-active', scope.row)">使用此编辑器</el-button>

        <el-button
          v-if="currentPluginType === 'article_theme' && !isPluginInUse(scope.row) && scope.row.enabled"
          type="text"
          icon="el-icon-magic-stick"
          class="primary-text"
          @click="$emit('set-active', scope.row)">使用此主题</el-button>

        <el-button
          v-if="currentPluginType === 'payment' && !isPluginInUse(scope.row) && scope.row.enabled"
          type="text"
          icon="el-icon-money"
          class="primary-text"
          @click="$emit('set-active', scope.row)">使用此付费插件</el-button>

        <el-button
          v-if="currentPluginType === 'particle_effect' && !isPluginInUse(scope.row) && scope.row.enabled"
          type="text"
          icon="el-icon-magic-stick"
          class="primary-text"
          @click="$emit('set-active', scope.row)">使用此特效</el-button>

        <el-button type="text" icon="el-icon-edit" @click="$emit('edit', scope.row)">编辑</el-button>
        <el-button
          v-if="scope.row.enabled"
          type="text"
          icon="el-icon-close"
          class="red-text"
          @click="$emit('toggle-status', scope.row, false)">禁用</el-button>
        <el-button
          v-else
          type="text"
          icon="el-icon-check"
          class="green-text"
          @click="$emit('toggle-status', scope.row, true)">启用</el-button>
        <el-button
          v-if="!scope.row.isSystem"
          type="text"
          icon="el-icon-delete"
          class="red-text"
          @click="$emit('delete', scope.row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
import { getModelPath, getPreviewImage } from '@/components/admin/pluginManager/pluginManagerTransforms';

export default {
  name: 'PluginTable',
  props: {
    tableData: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    currentPluginType: { type: String, required: true },
    activePluginKey: { type: String, default: '' }
  },
  methods: {
    getModelPath,
    getPreviewImage,
    isPluginInUse(row) {
      return !!row && !!row.enabled && this.activePluginKey === row.pluginKey;
    }
  }
}
</script>

<style scoped>
.primary-text {
  color: #409EFF !important;
}
.red-text {
  color: #ff0000;
}
.green-text {
  color: #67c23a;
}
</style>
