<template>
  <div>
    <div class="crumbs">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item><i class="el-icon-menu"></i> 插件管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ currentPluginTypeLabel }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="container">
      <div class="handle-box">
        <el-select v-model="currentPluginType" placeholder="请选择插件类型" @change="handleTypeChange" class="handle-select mr10">
          <el-option label="鼠标点击效果" value="mouse_click_effect"></el-option>
          <el-option label="看板娘模型" value="waifu_model"></el-option>
          <el-option label="文章编辑器" value="editor"></el-option>
        </el-select>
        <el-button type="primary" icon="el-icon-plus" @click="handleCreate">新增插件</el-button>
      </div>

      <el-table :data="tableData" border class="table" header-cell-class-name="table-header" v-loading="loading">
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        
        <!-- 看板娘模型预览列 -->
        <el-table-column v-if="currentPluginType === 'waifu_model'" label="预览" width="80" align="center">
          <template slot-scope="scope">
            <el-tooltip :content="getModelPath(scope.row)" placement="top">
              <el-avatar 
                :size="50" 
                :src="getPreviewImage(scope.row.pluginKey, scope.row)"
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
            <el-tag effect="dark" type="success" v-if="activePluginKey === scope.row.pluginKey">
              使用中
            </el-tag>
            <el-tag :type="scope.row.enabled ? 'info' : 'danger'" v-else>
              {{ scope.row.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" align="center">
          <template slot-scope="scope">
            <!-- 看板娘模型专用：使用此模型按钮 -->
            <el-button 
              type="text" 
              icon="el-icon-star-on" 
              class="primary-text" 
              v-if="currentPluginType === 'waifu_model' && activePluginKey !== scope.row.pluginKey"
              @click="handleSetActive(scope.row)">使用此模型</el-button>
            
            <!-- 编辑器插件专用：使用此编辑器按钮 -->
            <el-button 
              type="text" 
              icon="el-icon-edit-outline" 
              class="primary-text" 
              v-if="currentPluginType === 'editor' && activePluginKey !== scope.row.pluginKey && scope.row.enabled"
              @click="handleSetActive(scope.row)">使用此编辑器</el-button>
            
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button 
              type="text" 
              icon="el-icon-close" 
              class="red-text" 
              v-if="scope.row.enabled"
              @click="handleStatus(scope.row, false)">禁用</el-button>
            <el-button 
              type="text" 
              icon="el-icon-check" 
              class="green-text" 
              v-else
              @click="handleStatus(scope.row, true)">启用</el-button>
              
            <el-button 
              type="text" 
              icon="el-icon-delete" 
              class="red-text" 
              v-if="!scope.row.isSystem"
              @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 特效设置区域 (仅鼠标点击效果插件显示) -->
      <div class="effect-settings" v-if="currentPluginType === 'mouse_click_effect'">
        <el-divider content-position="left">特效设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="setting-item">
              <el-switch v-model="disableLowPerf" @change="saveSettings"></el-switch>
              <span class="setting-label">低性能设备自动关闭特效</span>
              <div class="setting-desc">
                低性能设备中禁用点击特效，提高页面流畅性，<a href="javascript:void(0)" @click="lowPerfConfigVisible = true" style="color: #409EFF">定义低性能设备</a>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="setting-item">
              <el-switch v-model="disableInAdmin" @change="saveSettings"></el-switch>
              <span class="setting-label">后台管理系统关闭特效</span>
              <div class="setting-desc">在后台管理系统中禁用点击特效，提高操作效率</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 低性能设备定义配置对话框 -->
    <el-dialog title="定义低性能设备" :visible.sync="lowPerfConfigVisible" width="500px" custom-class="centered-dialog" append-to-body>
      <div class="low-perf-config-container">
        <!-- 设备状态仪表盘 -->
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
          
          <div class="config-row" v-if="enableFpsCheck" style="padding-left: 24px; margin-top: 10px;">
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


    <!-- 编辑/新增对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="editVisible" width="50%" :close-on-click-modal="false" custom-class="centered-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="插件类型">
           <el-input v-model="form.pluginType" disabled></el-input>
        </el-form-item>
        <el-form-item label="插件名称" prop="pluginName">
          <el-input v-model="form.pluginName"></el-input>
        </el-form-item>
        <el-form-item label="标识符(Key)" prop="pluginKey">
          <el-input v-model="form.pluginKey" :disabled="isEdit"></el-input>
          <div class="sub-title" style="font-size: 12px; color: #999; line-height: 20px;">唯一标识，不可重复，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="描述" prop="pluginDescription">
          <el-input type="textarea" v-model="form.pluginDescription"></el-input>
        </el-form-item>
        <el-form-item label="JSON配置" prop="pluginConfig" v-if="currentPluginType !== 'waifu_model'">
           <el-input type="textarea" :rows="4" v-model="form.pluginConfig" placeholder="请输入JSON格式配置"></el-input>
           <div class="sub-title" style="font-size: 12px; color: #999; line-height: 20px;">
             用于配置插件参数，必须是合法的JSON格式。例如：{"colors": ["#FF0000", "#00FF00"]}
           </div>
        </el-form-item>
        
        <!-- 看板娘模型专用配置 -->
        <template v-if="currentPluginType === 'waifu_model'">
          <el-divider content-position="left">模型配置</el-divider>
          <el-form-item label="模型路径">
            <el-input v-model="waifuConfig.modelPath" placeholder="例如: Potion-Maker/Pio"></el-input>
            <div class="sub-title" style="font-size: 12px; color: #999;">相对于 /static/live2d_api/model/ 的路径</div>
          </el-form-item>
          <el-form-item label="材质列表">
            <el-input type="textarea" :rows="3" v-model="waifuConfig.texturesStr" placeholder="每行一个材质路径，例如：&#10;Potion-Maker/Pio&#10;Potion-Maker/Pio-2"></el-input>
            <div class="sub-title" style="font-size: 12px; color: #999;">模型的不同材质/服装，每行一个</div>
          </el-form-item>
          <el-form-item label="缩放比例">
            <el-slider v-model="waifuConfig.scale" :min="0.5" :max="2" :step="0.1" show-input></el-slider>
          </el-form-item>
          <el-form-item label="预览图">
            <div class="thumbnail-upload-container">
              <el-avatar 
                v-if="waifuConfig.thumbnailUrl"
                :size="80" 
                :src="waifuConfig.thumbnailUrl"
                style="border: 2px solid #e0e0e0; margin-right: 15px;">
              </el-avatar>
              <el-avatar 
                v-else
                :size="80" 
                :src="getPreviewImage(form.pluginKey)"
                style="background: #f5f7fa; border: 1px solid #dcdfe6; color: #909399; margin-right: 15px;"
                icon="el-icon-s-custom">
              </el-avatar>
              <el-button type="primary" icon="el-icon-upload2" @click="uploadPictureDialog = true">选择/上传图片</el-button>
            </div>
            <div class="sub-title" style="font-size: 12px; color: #999; margin-top: 8px;">建议尺寸: 200x200px，支持 JPG/PNG 格式</div>
          </el-form-item>
          
          <el-dialog title="上传/选择图片" :visible.sync="uploadPictureDialog" width="45%" append-to-body>
            <upload-picture :maxNumber="1" :maxSize="2" :isAdmin="true" @addPicture="handleThumbnailSuccess"></upload-picture>
          </el-dialog>
          <el-divider content-position="left">对话配置</el-divider>
          <el-form-item label="问候语">
            <el-input type="textarea" :rows="2" v-model="waifuConfig.greetingStr" placeholder="每行一句，随机选择显示"></el-input>
          </el-form-item>
          <el-form-item label="闲置消息">
            <el-input type="textarea" :rows="2" v-model="waifuConfig.idleStr" placeholder="每行一句，随机选择显示"></el-input>
          </el-form-item>
        </template>
        <el-form-item label="JS代码" prop="pluginCode" v-if="currentPluginType !== 'waifu_model'">
           <el-input 
             type="textarea" 
             :rows="10" 
             v-model="form.pluginCode" 
             placeholder="自定义点击效果的JS代码。可用参数：x, y (点击坐标), config (JSON配置对象), anime (anime.js库)"
             style="font-family: 'Consolas', 'Monaco', monospace;"></el-input>
           <div class="sub-title" style="font-size: 12px; color: #999; line-height: 20px;">
              编写自定义点击效果。函数签名：<code>function(x, y, config, anime) { ... }</code><br/>
              <strong>anime</strong> 为 anime.js 动画库，可用于创建复杂动画效果。
              <a href="https://animejs.com/documentation/" target="_blank" style="color: #409EFF;">查看anime.js文档</a><br/>
              不会写代码？<a href="javascript:void(0)" @click="aiPromptVisible = true" style="color: #409EFF;">使用AI生成效果代码</a>
           </div>
        </el-form-item>
        <el-form-item label="是否启用">
           <el-switch v-model="form.enabled"></el-switch>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
           <el-input-number v-model="form.sortOrder" :min="0"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveEdit">确 定</el-button>
      </span>
    </el-dialog>

    <!-- AI提示词对话框 -->
    <el-dialog title="使用AI生成效果代码" :visible.sync="aiPromptVisible" width="600px" custom-class="centered-dialog">
      <p style="margin-bottom: 15px; color: #666;">复制以下提示词发送给AI（如DeepSeek、ChatGPT、Claude等），让AI帮你生成自定义点击效果代码：</p>
      <pre style="white-space: pre-wrap; background: #f5f5f5; padding: 15px; border-radius: 6px; font-size: 13px; line-height: 1.7; max-height: 350px; overflow-y: auto; border: 1px solid #e0e0e0;">请帮我编写一个网页鼠标点击效果的JavaScript代码。

函数签名：函数体直接执行，可用参数如下：
- x: 点击的页面X坐标（包含滚动偏移）
- y: 点击的页面Y坐标（包含滚动偏移）
- config: JSON配置对象，包含用户自定义的参数
- anime: anime.js动画库实例（可用于创建复杂动画效果）

要求：
1. 代码会被直接执行（无需定义函数名，直接写函数体）
2. 创建的DOM元素必须设置 pointer-events: none 防止干扰用户操作
3. 使用 z-index: 1000 或更高确保效果在最上层
4. 创建的元素必须在动画结束后自行移除，避免内存泄漏
5. 如果使用canvas，可以获取或创建id为"mousedown-effect"的canvas元素复用
6. 如果需要将页面坐标转为视口坐标（用于position:fixed元素），使用：
   const viewportX = x - window.scrollX;
   const viewportY = y - window.scrollY;

请生成一个【在此描述你想要的效果】的鼠标点击效果。</pre>
      <div style="margin-top: 15px;">
        <strong style="color: #409EFF;">效果描述示例</strong>
        <p style="margin: 8px 0; color: #909399; font-size: 12px;">可以替换上面模板最后一句的【在此描述你想要的效果】部分：</p>
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <thead>
            <tr style="background: #f5f7fa;">
              <th style="padding: 10px; border: 1px solid #ebeef5; text-align: left; width: 100px;">想要的效果</th>
              <th style="padding: 10px; border: 1px solid #ebeef5; text-align: left;">描述</th>
            </tr>
          </thead>
          <tbody>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">爱心飘落</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生一个红色爱心向上飘起并逐渐消失</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">水波纹</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生从中心向外扩散的圆形水波纹效果</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">星星闪烁</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生多个五角星向四周散开并闪烁</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">雪花飘落</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生几片雪花缓缓下落</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">彩色气泡</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生多个彩色气泡向上飘起</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">文字弹出</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时显示自定义文字向上弹出淡出</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">雷电效果</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生闪电样的光效扩散</td></tr>
            <tr><td style="padding: 8px 10px; border: 1px solid #ebeef5;">樱花飘落</td><td style="padding: 8px 10px; border: 1px solid #ebeef5;">点击时产生几片粉色樱花瓣随风飘落</td></tr>
          </tbody>
        </table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="aiPromptVisible = false">关 闭</el-button>
        <el-button type="primary" @click="copyAiPrompt">复制提示词</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import UploadPicture from "../common/uploadPicture";

export default {
  name: 'pluginManager',
  components: {
    UploadPicture
  },
  watch: {
    '$route.query.type': {
      handler(val) {
        const t = String(val || '');
        if (['mouse_click_effect', 'waifu_model', 'editor'].includes(t) && t !== this.currentPluginType) {
          this.currentPluginType = t;
          this.form.pluginType = t;
          this.getData();
        }
      }
    }
  },
  data() {
    return {
      loading: false,
      tableData: [],
      currentPluginType: 'mouse_click_effect',
      activePluginKey: '',
      editVisible: false,
      isEdit: false,
      aiPromptVisible: false,
      uploadPictureDialog: false, // 图片上传对话框
      lowPerfConfigVisible: false,
      webInfoId: null, // 网站信息ID，用于更新配置
      disableLowPerf: false,
      disableInAdmin: false,
      cpuCoreThreshold: 2,
      memoryThreshold: 4,
      enableFpsCheck: true,
      fpsThreshold: 30,
      disableMobile: true,
      deviceCpuCores: navigator.hardwareConcurrency || 4,
      deviceMemory: navigator.deviceMemory || '未知',
      isMobileDevice: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
      form: {
        id: null,
        pluginType: 'mouse_click_effect',
        pluginKey: '',
        pluginName: '',
        pluginDescription: '',
        pluginConfig: '{}',
        pluginCode: '',
        enabled: true,
        isSystem: false,
        sortOrder: 0
      },
      // 看板娘模型专用配置（可视化编辑）
      waifuConfig: {
        modelPath: '',
        texturesStr: '',
        scale: 1.0,
        greetingStr: '',
        idleStr: '',
        thumbnailUrl: ''
      },
      rules: {
        pluginName: [
          { required: true, message: '请输入插件名称', trigger: 'blur' }
        ],
        pluginKey: [
          { required: true, message: '请输入插件标识符', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
        ],
        pluginConfig: [
          { 
            validator: (rule, value, callback) => {
              if (!value) return callback();
              try {
                JSON.parse(value);
                callback();
              } catch (e) {
                callback(new Error('请输入合法的JSON格式'));
              }
            }, 
            trigger: 'blur' 
          }
        ]
      }
    };
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑插件' : '新增插件';
    },
    currentPluginTypeLabel() {
      const labels = {
        'mouse_click_effect': '鼠标效果管理',
        'waifu_model': '看板娘模型管理',
        'editor': '文章编辑器管理'
      };
      return labels[this.currentPluginType] || '插件管理';
    },
    uploadHeaders() {
      const token = localStorage.getItem("adminToken") || localStorage.getItem("userToken");
      return { Authorization: token };
    }
  },
  created() {
    const t = String(this.$route?.query?.type || '');
    if (['mouse_click_effect', 'waifu_model', 'editor'].includes(t)) {
      this.currentPluginType = t;
      this.form.pluginType = t;
    }
    this.getData();
    this.loadSettings();
  },
  methods: {
    loadSettings() {
      // 仅从后端加载配置
      this.$http.get(this.$constant.baseURL + "/webInfo/getWebInfo")
        .then(res => {
          if (res.code === 200 && res.data) {
            const webInfo = res.data;
            this.webInfoId = webInfo.id;
            
            // 如果后端有配置，使用后端配置
            if (webInfo.mouseClickEffectConfig) {
              try {
                const settings = JSON.parse(webInfo.mouseClickEffectConfig);
                this.disableLowPerf = settings.disableLowPerf ?? false;
                this.disableInAdmin = settings.disableInAdmin ?? false;
                this.cpuCoreThreshold = settings.cpuCoreThreshold ?? 2;
                this.memoryThreshold = settings.memoryThreshold ?? 4;
                this.disableMobile = settings.disableMobile ?? true;
                this.enableFpsCheck = settings.enableFpsCheck ?? true;
                this.fpsThreshold = settings.fpsThreshold ?? 30;
              } catch (e) {
                console.error("解析后端配置失败", e);
              }
            }
          }
        })
        .catch(err => {
          console.error("获取WebInfo失败", err);
          this.$message.error("获取配置失败，请检查网络");
        });
    },

    saveSettings() {
      const settings = {
        disableLowPerf: this.disableLowPerf,
        disableInAdmin: this.disableInAdmin,
        cpuCoreThreshold: this.cpuCoreThreshold,
        memoryThreshold: this.memoryThreshold,
        disableMobile: this.disableMobile,
        enableFpsCheck: this.enableFpsCheck,
        fpsThreshold: this.fpsThreshold
      };

      const doSave = () => {
        if (this.webInfoId) {
          this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", {
            id: this.webInfoId,
            mouseClickEffectConfig: JSON.stringify(settings)
          }).then(res => {
            if (res.code === 200) {
              this.$message.success('设置已同步至服务器');
            } else {
              this.$message.warning('保存到服务器失败: ' + res.msg);
            }
          }).catch(() => {
            this.$message.error('网络错误，保存失败');
          });
        } else {
          this.$message.warning('无法获取网站ID，无法保存配置');
        }
      };
      
      // 如果没有ID，尝试重新获取
      if (!this.webInfoId) {
        this.$http.get(this.$constant.baseURL + "/webInfo/getWebInfo").then(res => {
          if (res.code === 200 && res.data) {
            this.webInfoId = res.data.id;
            doSave();
          } else {
            this.$message.warning('无法获取网站ID，请刷新页面重试');
          }
        }).catch(() => {
          this.$message.error('网络错误，无法保存');
        });
      } else {
        doSave();
      }
    },

    handleTypeChange() {
      if (this.$router && this.$route) {
        const nextQuery = { ...this.$route.query, type: this.currentPluginType };
        this.$router.replace({ name: 'pluginManager', query: nextQuery });
      }
      this.getData();
    },

    getModelPath(row) {
      try {
        const config = JSON.parse(row.pluginConfig || '{}');
        return config.modelPath || '未配置';
      } catch (e) {
        return '配置解析失败';
      }
    },

    getPreviewImage(pluginKey, row) {
      // 优先从config中读取缩略图（无论是自定义还是内置的）
      if (row && row.pluginConfig) {
        try {
          const config = JSON.parse(row.pluginConfig);
          if (config.thumbnailUrl) {
            return config.thumbnailUrl;
          }
        } catch (e) {}
      }
      
      // 没有配置预览图时返回空，界面上会显示默认图标
      return '';
    },

    handleSetActive(row) {
      this.$http.post(this.$constant.baseURL + "/sysPlugin/setActivePlugin", {
        pluginType: this.currentPluginType,
        pluginKey: row.pluginKey
      }, true).then(res => {
        if (res.code === 200) {
          this.$message.success(`已切换为: ${row.pluginName}`);
          this.activePluginKey = row.pluginKey;
        } else {
          this.$message.error(res.message || '设置失败');
        }
      }).catch(() => {
        this.$message.error('网络错误');
      });
    },

    copyAiPrompt() {
      const prompt = `请帮我编写一个网页鼠标点击效果的JavaScript代码。

函数签名：函数体直接执行，可用参数如下：
- x: 点击的页面X坐标（包含滚动偏移）
- y: 点击的页面Y坐标（包含滚动偏移）
- config: JSON配置对象，包含用户自定义的参数
- anime: anime.js动画库实例（可用于创建复杂动画效果）

要求：
1. 代码会被直接执行（无需定义函数名，直接写函数体）
2. 创建的DOM元素必须设置 pointer-events: none 防止干扰用户操作
3. 使用 z-index: 1000 或更高确保效果在最上层
4. 创建的元素必须在动画结束后自行移除，避免内存泄漏
5. 如果使用canvas，可以获取或创建id为"mousedown-effect"的canvas元素复用
6. 如果需要将页面坐标转为视口坐标（用于position:fixed元素），使用：
   const viewportX = x - window.scrollX;
   const viewportY = y - window.scrollY;

请生成一个【在此描述你想要的效果】的鼠标点击效果。`;

      navigator.clipboard.writeText(prompt).then(() => {
        this.$message.success('提示词已复制到剪贴板');
      }).catch(() => {
        this.$message.error('复制失败，请手动选择复制');
      });
    },
    
    getData() {
      this.loading = true;
      // 1. 获取插件列表
      this.$http.get(this.$constant.baseURL + "/sysPlugin/listPlugins", { pluginType: this.currentPluginType }, true)
        .then((res) => {
          this.tableData = res.data;
          this.loading = false;
        })
        .catch((error) => {
          this.loading = false;
          this.$message.error(error.message);
        });
        
      // 2. 获取当前激活的插件
      this.$http.get(this.$constant.baseURL + "/sysPlugin/getActivePlugin", { pluginType: this.currentPluginType }, true)
        .then((res) => {
          if (res.data) {
            this.activePluginKey = res.data.pluginKey;
          } else {
            this.activePluginKey = 'none';
          }
        })
        .catch((error) => {
          console.error(error);
        });
    },

    handleCreate() {
      this.isEdit = false;
      this.form = {
        id: null,
        pluginType: this.currentPluginType,
        pluginKey: '',
        pluginName: '',
        pluginDescription: '',
        pluginConfig: '{}',
        pluginCode: '',
        enabled: true,
        isSystem: false,
        sortOrder: 0
      };
      
      // 重置看板娘配置
      if (this.currentPluginType === 'waifu_model') {
        this.waifuConfig = {
          modelPath: '',
          texturesStr: '',
          scale: 1.0,
          greetingStr: '',
          idleStr: '',
          thumbnailUrl: ''
        };
      }
      this.editVisible = true;
      if (this.$refs.form) {
        this.$refs.form.clearValidate();
      }
    },

    handleEdit(row) {
      this.isEdit = true;
      this.form = JSON.parse(JSON.stringify(row));
      // 如果没有配置，默认为空对象
      if (!this.form.pluginConfig) {
        this.form.pluginConfig = '{}';
      }
      
      // 如果是看板娘模型，解析JSON配置到可视化字段
      if (this.currentPluginType === 'waifu_model') {
        try {
          const config = JSON.parse(this.form.pluginConfig);
          this.waifuConfig = {
            modelPath: config.modelPath || '',
            texturesStr: (config.textures || []).join('\n'),
            scale: config.scale || 1.0,
            greetingStr: (config.messages?.greeting || []).join('\n'),
            idleStr: (config.messages?.idle || []).join('\n'),
            thumbnailUrl: config.thumbnailUrl || ''
          };
        } catch (e) {
          console.error('解析看板娘配置失败:', e);
          this.waifuConfig = { modelPath: '', texturesStr: '', scale: 1.0, greetingStr: '', idleStr: '', thumbnailUrl: '' };
        }
      }
      
      this.editVisible = true;
      if (this.$refs.form) {
        this.$refs.form.clearValidate();
      }
    },

    handleStatus(row, enabled) {
      this.$http.post(this.$constant.baseURL + "/sysPlugin/togglePluginStatus", {
        id: row.id,
        enabled: enabled
      }, true)
        .then(() => {
          this.$message.success(enabled ? '已启用' : '已禁用');
          row.enabled = enabled;
        })
        .catch((error) => {
          this.$message.error(error.message);
        });
    },

    handleDelete(row) {
      this.$confirm('确定要删除这个插件吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$http.post(this.$constant.baseURL + "/sysPlugin/deletePlugin", { id: row.id }, true)
          .then(() => {
            this.$message.success('删除成功');
            this.getData();
          })
          .catch((error) => {
            this.$message.error(error.message);
          });
      }).catch(() => {});
    },

    saveEdit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          // 如果是看板娘模型，将可视化配置序列化为JSON
          if (this.currentPluginType === 'waifu_model') {
            const textures = this.waifuConfig.texturesStr.split('\n').filter(t => t.trim());
            const greetings = this.waifuConfig.greetingStr.split('\n').filter(t => t.trim());
            const idles = this.waifuConfig.idleStr.split('\n').filter(t => t.trim());
            
            const config = {
              modelPath: this.waifuConfig.modelPath,
              textures: textures.length > 0 ? textures : [this.waifuConfig.modelPath],
              scale: this.waifuConfig.scale,
              thumbnailUrl: this.waifuConfig.thumbnailUrl || '',
              messages: {
                greeting: greetings.length > 0 ? greetings : ['你好呀~'],
                idle: idles.length > 0 ? idles : ['点点我嘛~']
              }
            };
            this.form.pluginConfig = JSON.stringify(config);
          }
          
          const url = this.$constant.baseURL + (this.isEdit ? "/sysPlugin/updatePlugin" : "/sysPlugin/addPlugin");
          
          this.$http.post(url, this.form, true)
            .then(() => {
              this.$message.success(this.isEdit ? '修改成功' : '新增成功');
              this.editVisible = false;
              this.getData();
            })
            .catch((error) => {
              this.$message.error(error.message);
            });
        }
      });
    },
    
    // 缩略图上传成功处理
    handleThumbnailSuccess(response) {
      if (response.code === 200) {
        this.waifuConfig.thumbnailUrl = response.data;
        this.$message.success('预览图上传成功');
      } else {
        this.$message.error(response.message || '上传失败');
      }
    },
    
    // 缩略图上传前校验
    beforeThumbnailUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isImage) {
        this.$message.error('只能上传图片文件！');
        return false;
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB！');
        return false;
      }
      return true;
    }
  }
};
</script>

<style scoped>
.handle-box {
  margin: 20px 0;
}
.handle-select {
  width: 150px;
}
.mr10 {
  margin-right: 10px;
}
.red-text {
  color: #ff0000;
}
.green-text {
  color: #67c23a;
}
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
/* 低性能配置弹窗样式 */
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

.primary-text {
  color: #409EFF !important;
}

.thumbnail-upload-container {
  display: flex;
  align-items: center;
}
</style>
