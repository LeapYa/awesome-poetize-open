<!-- eslint-disable vue/no-mutating-props -->
<template>
  <div>
    <el-divider content-position="left">标题装饰设置</el-divider>
    <div class="theme-config-section">
      <div class="theme-heading-row" v-for="(level, index) in levels" :key="level">
        <span class="theme-level-label">H{{ index + 1 }}</span>
        <el-input
          v-model="themeConfig.headings[level].emoji"
          placeholder="emoji/符号"
          size="small"
          style="width: 100px; margin-right: 10px;">
        </el-input>
        <el-color-picker
          v-model="themeConfig.headings[level].color"
          size="small"
          show-alpha
          style="margin-right: 10px;">
        </el-color-picker>
        <el-tooltip content="emoji类建议32~40px，符号类建议16~25px" placement="top" :open-delay="300">
          <el-input
            v-model="themeConfig.headings[level].paddingLeft"
            placeholder="左边距"
            size="small"
            style="width: 100px; margin-right: 10px;">
          </el-input>
        </el-tooltip>
        <el-switch
          v-model="themeConfig.headings[level].show"
          active-text="显示"
          inactive-text="隐藏"
          style="margin-right: 10px;">
        </el-switch>
      </div>
    </div>

    <el-divider content-position="left">目录装饰</el-divider>
    <div class="theme-config-section">
      <div class="theme-heading-row">
        <span class="theme-level-label">TOC</span>
        <el-input
          v-model="themeConfig.toc.emoji"
          placeholder="emoji/符号"
          size="small"
          style="width: 100px; margin-right: 10px;">
        </el-input>
        <el-switch
          v-model="themeConfig.toc.show"
          active-text="显示"
          inactive-text="隐藏">
        </el-switch>
      </div>
    </div>

    <el-divider content-position="left">快速预设</el-divider>
    <div class="theme-preset-buttons">
      <el-button size="small" @click="$emit('apply-preset', 'default')">📑 默认</el-button>
      <el-button size="small" @click="$emit('apply-preset', 'minimal')">📖 简约</el-button>
      <el-button size="small" @click="$emit('apply-preset', 'plain')">📄 无装饰</el-button>
      <el-button size="small" @click="$emit('apply-preset', 'garden')">🌿 花园</el-button>
      <el-button size="small" @click="$emit('apply-preset', 'academic')">§ 学术</el-button>
      <el-button size="small" @click="$emit('apply-preset', 'space')">🌟 星空</el-button>
    </div>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
export default {
  name: 'ArticleThemeEditor',
  props: {
    themeConfig: { type: Object, required: true }
  },
  data() {
    return {
      levels: ['h1', 'h2', 'h3', 'h4', 'h5']
    };
  }
}
</script>

<style scoped>
.theme-config-section {
  padding: 0 10px;
}
.theme-heading-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}
.theme-level-label {
  display: inline-block;
  width: 40px;
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}
.theme-preset-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 10px;
}
</style>
