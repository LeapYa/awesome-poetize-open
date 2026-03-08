<template>
  <div class="effect-settings">
    <el-divider content-position="left">特效设置</el-divider>
    <el-row :gutter="20">
      <el-col :span="12">
        <div id="field-plugin-low-perf" class="setting-item">
          <el-switch v-model="disableLowPerf" @change="saveSettings"></el-switch>
          <span class="setting-label">低性能设备自动关闭特效</span>
          <div class="setting-desc">
            低性能设备中禁用点击特效，提高页面流畅性，<a href="javascript:void(0)" @click="lowPerfConfigVisible = true" style="color: #409EFF">定义低性能设备</a>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div id="field-plugin-disable-admin" class="setting-item">
          <el-switch v-model="disableInAdmin" @change="saveSettings"></el-switch>
          <span class="setting-label">后台管理系统关闭特效</span>
          <div class="setting-desc">在后台管理系统中禁用点击特效，提高操作效率</div>
        </div>
      </el-col>
    </el-row>

    <el-dialog title="定义低性能设备" :visible.sync="lowPerfConfigVisible" width="500px" custom-class="centered-dialog" append-to-body>
      <div class="low-perf-config-container">
        <div class="device-status-card">
          <div class="status-header">当前设备状态</div>
          <el-row :gutter="10" class="status-grid">
            <el-col :span="8">
              <div class="status-item">
                <i class="el-icon-cpu"></i>
                <div class="status-val">{{ deviceCpuCores }} 核</div>
                <div class="status-label">CPU核心</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-item">
                <i class="el-icon-monitor"></i>
                <div class="status-val">
                  <span v-if="deviceMemory >= 8">≥</span>{{ deviceMemory }} GB
                </div>
                <div class="status-label">设备内存</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-item">
                <i class="el-icon-mobile"></i>
                <div class="status-val">{{ isMobileDevice ? '移动端' : '桌面端' }}</div>
                <div class="status-label">设备类型</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <el-divider content-position="left">判定标准</el-divider>
        <div class="config-section">
          <div class="config-row">
            <span class="label">CPU限制</span>
            <span class="desc">核心数 ≤</span>
            <el-input-number v-model="cpuCoreThreshold" :min="1" :max="32" size="small" @change="saveSettings"></el-input-number>
          </div>

          <div class="config-row">
            <span class="label">内存限制</span>
            <span class="desc">内存容量 ≤</span>
            <el-input-number v-model="memoryThreshold" :min="1" :max="32" size="small" @change="saveSettings"></el-input-number>
            <span class="unit">GB</span>
          </div>

          <div class="config-row checkbox-row">
            <el-checkbox v-model="disableMobile" @change="saveSettings">移动设备一律视为低性能</el-checkbox>
          </div>
        </div>

        <el-divider content-position="left">动态监测 (推荐)</el-divider>
        <div class="config-section">
          <div class="config-row checkbox-row">
            <el-checkbox v-model="enableFpsCheck" @change="saveSettings">开启FPS帧率监测</el-checkbox>
            <el-tooltip content="开启后，将在运行时实时监测页面流畅度。如果连续卡顿，自动关闭特效。" placement="top">
              <i class="el-icon-question" style="color: #909399; margin-left: 5px;"></i>
            </el-tooltip>
          </div>

          <div v-if="enableFpsCheck" class="config-row fps-row">
            <span class="desc">帧率低于</span>
            <el-slider v-model="fpsThreshold" :min="15" :max="60" :step="5" show-stops style="width: 180px; margin: 0 15px;" @change="saveSettings"></el-slider>
            <span class="unit">{{ fpsThreshold }} FPS</span>
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="lowPerfConfigVisible = false">完成配置</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createMouseEffectSettingsState, loadMouseEffectSettings, saveMouseEffectSettings } from '@/components/admin/pluginManager/useMouseEffectSettings';

export default {
  name: 'MouseEffectSettingsPanel',
  data() {
    return {
      lowPerfConfigVisible: false,
      ...createMouseEffectSettingsState()
    };
  },
  created() {
    loadMouseEffectSettings(this);
  },
  methods: {
    saveSettings() {
      saveMouseEffectSettings(this);
    }
  }
}
</script>

<style scoped>
.effect-settings {
  margin-top: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 6px;
}
.setting-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
}
.setting-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.setting-desc {
  width: 100%;
  font-size: 12px;
  color: #909399;
}
.low-perf-config-container {
  padding: 0 10px;
}
.device-status-card {
  background: #f4f6fc;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
}
.status-header {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  font-weight: 500;
}
.status-grid .status-item {
  background: white;
  border-radius: 6px;
  padding: 10px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.03);
}
.status-item i {
  font-size: 20px;
  color: #409EFF;
  margin-bottom: 5px;
  display: block;
}
.status-val {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 2px;
}
.status-label {
  font-size: 12px;
  color: #909399;
}
.config-section {
  padding: 0 10px;
}
.config-row {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
}
.config-row.checkbox-row {
  margin-top: 5px;
  margin-bottom: 10px;
}
.config-row .label {
  width: 80px;
  color: #606266;
}
.config-row .desc {
  color: #909399;
  margin-right: 10px;
  font-size: 13px;
}
.config-row .unit {
  margin-left: 10px;
  color: #606266;
}
.fps-row {
  padding-left: 24px;
  margin-top: 10px;
}
</style>
