<template>
  <div id="field-email-settings">
    <SectionTag>邮箱配置</SectionTag>

    <el-card class="box-card" shadow="never" style="margin-top: 5px; border: none;">
      <!-- 响应式表格 -->
      <div class="responsive-table-container">
        <el-table
          :data="emailConfigs"
          border
          style="width: 100%"
          :class="{'mobile-table': isMobileDevice}"
          size="small"
          @row-click="handleEmailRowClick"
          @touchstart.native="handleTouchStart"
          @touchend.native="handleTouchEnd">

          <el-table-column prop="smtpHost" label="邮箱服务器" min-width="150">
            <template slot-scope="scope">
              <el-input v-model="scope.row.host" placeholder="例如: smtp.163.com"></el-input>
            </template>
          </el-table-column>

          <el-table-column prop="smtpUsername" label="邮箱地址" min-width="180" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <el-input v-model="scope.row.username" placeholder="例如: example@163.com"></el-input>
            </template>
          </el-table-column>

          <el-table-column prop="smtpPassword" label="授权码" min-width="150" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <el-input v-model="scope.row.password" placeholder="授权码" show-password></el-input>
            </template>
          </el-table-column>

          <el-table-column prop="smtpPort" label="端口" min-width="80" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.port" :min="1" :max="65535" :controls="false" style="width: 100%" @change="(value) => onPortChange(value, scope.row, scope.$index)"></el-input-number>
            </template>
          </el-table-column>

          <el-table-column prop="senderName" label="发件人名称" min-width="120" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <el-input v-model="scope.row.senderName" placeholder="发件人显示名称"></el-input>
            </template>
          </el-table-column>

          <el-table-column prop="ssl" label="SSL" min-width="80" align="center">
            <template slot-scope="scope">
              <div style="display: flex; align-items: center; justify-content: center;">
                <el-tooltip
                  :content="scope.row.port === 465 ? '✅ 端口465必须启用SSL加密' : 'SSL是一种安全加密连接方式，适用于端口465'"
                  placement="top">
                  <i class="el-icon-lock"
                     :style="{ marginRight: '8px', color: scope.row.port === 465 ? '#F56C6C' : '#909399', fontSize: '14px', cursor: 'help' }"></i>
                </el-tooltip>
                <el-switch v-model="scope.row.useSsl" @change="(value) => onSslChange(value, scope.row, scope.$index)"></el-switch>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="starttls" label="STARTTLS" min-width="100" align="center" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <div style="display: flex; align-items: center; justify-content: center;">
                <el-tooltip
                  :content="scope.row.port === 587 ? '✅ 端口587推荐启用STARTTLS加密' : 'STARTTLS是连接后升级加密的方式，适用于端口587'"
                  placement="top">
                  <i class="el-icon-unlock"
                     :style="{ marginRight: '8px', color: scope.row.port === 587 ? '#E6A23C' : '#909399', fontSize: '14px', cursor: 'help' }"></i>
                </el-tooltip>
                <el-switch v-model="scope.row.useStarttls" @change="(value) => onStarttlsChange(value, scope.row, scope.$index)"></el-switch>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="auth" label="认证" min-width="80" align="center" :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
            <template slot-scope="scope">
              <el-switch v-model="scope.row.auth"></el-switch>
            </template>
          </el-table-column>

          <el-table-column prop="enabled" label="启用" min-width="80" align="center">
            <template slot-scope="scope">
              <el-switch v-model="scope.row.enabled"></el-switch>
            </template>
          </el-table-column>

          <el-table-column prop="operation" label="操作" min-width="180">
            <template slot-scope="scope">
              <div>
                <el-button type="text" size="small" @click="testEmailConfig(scope.row, scope.$index)">
                  <i class="el-icon-check"></i> 测试
                </el-button>
                <el-button type="text" size="small" @click="showAdvancedConfig(scope.$index)">
                  <i class="el-icon-setting"></i> 高级
                </el-button>
                <el-button type="text" size="small" class="delete-btn" @click="deleteEmailConfig(scope.$index)">
                  <i class="el-icon-delete"></i> 删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动设备提示面板 -->
      <div v-if="isMobileDevice" class="mobile-view-notice">
        <div style="margin: 10px 0; padding: 8px 12px; background: #f0f9ff; border-radius: 3px; font-size: 13px;">
          <i class="el-icon-mobile" style="color: #409EFF; margin-right: 6px;"></i>
          <span style="color: #606266;">在移动设备上点击表格行可查看完整信息</span>
        </div>
      </div>

      <div style="margin-top: 37px; text-align: center;">
        <el-button type="success" size="small" @click="addEmailConfig">添加邮箱</el-button>
      </div>
      <div style="margin-top: 10px; margin-bottom: 22px; text-align: center;">
        <el-button type="primary" @click="saveEmailConfigs" :loading="loading">保存邮箱配置</el-button>
      </div>

      <!-- 邮件测试对话框 -->
      <el-dialog title="测试邮件发送" :visible.sync="emailTestDialogVisible" width="500px" custom-class="centered-dialog">
        <el-form :model="emailTestForm" label-width="100px">
          <el-form-item label="测试邮箱">
            <el-input v-model="emailTestForm.testEmail" placeholder="请输入接收测试邮件的邮箱地址"></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="emailTestDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitTestEmail" :loading="testEmailLoading">发送测试邮件</el-button>
        </span>
      </el-dialog>

      <!-- 邮箱配置详情对话框（移动端） -->
      <el-dialog
        title="邮箱配置详情"
        :visible.sync="emailDetailDialogVisible"
        width="90%"
        :close-on-click-modal="true"
        custom-class="centered-dialog email-detail-dialog">
        <div v-if="currentEmailConfig" class="email-detail-content">
          <el-descriptions :column="1" border size="medium">
            <el-descriptions-item label="邮箱服务器">{{ currentEmailConfig.host || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱地址">{{ currentEmailConfig.username || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="授权码">
              <span style="font-family: monospace;">{{ currentEmailConfig.password ? '••••••••' : '未设置' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="端口">{{ currentEmailConfig.port || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="发件人名称">{{ currentEmailConfig.senderName || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="SSL加密">
              <el-tag :type="currentEmailConfig.useSsl ? 'success' : 'info'" size="small">{{ currentEmailConfig.useSsl ? '已启用' : '未启用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="STARTTLS">
              <el-tag :type="currentEmailConfig.useStarttls ? 'success' : 'info'" size="small">{{ currentEmailConfig.useStarttls ? '已启用' : '未启用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="认证">
              <el-tag :type="currentEmailConfig.auth ? 'success' : 'info'" size="small">{{ currentEmailConfig.auth ? '已启用' : '未启用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="currentEmailConfig.enabled ? 'success' : 'danger'" size="small">{{ currentEmailConfig.enabled ? '已启用' : '已禁用' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 20px; display: flex; gap: 10px; justify-content: center;">
            <el-button type="primary" size="small" @click="testEmailFromDetail"><i class="el-icon-check"></i> 测试邮件</el-button>
            <el-button size="small" @click="editEmailFromDetail"><i class="el-icon-edit"></i> 编辑配置</el-button>
            <el-button size="small" @click="showAdvancedFromDetail"><i class="el-icon-setting"></i> 高级设置</el-button>
          </div>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="emailDetailDialogVisible = false">关 闭</el-button>
        </span>
      </el-dialog>
    </el-card>

    <!-- 高级配置对话框 -->
    <el-dialog title="邮箱高级配置" :visible.sync="advancedConfigVisible" width="600px" custom-class="centered-dialog">
      <el-form :model="currentAdvancedConfig" label-width="160px">
        <el-tabs v-model="activeConfigTab">
          <el-tab-pane label="基础设置" name="basic">
            <el-form-item label="连接超时(ms)">
              <el-input-number v-model="currentAdvancedConfig.connectionTimeout" :min="1000" :max="120000" :step="1000" :controls="false"></el-input-number>
            </el-form-item>
            <el-form-item label="超时时间(ms)">
              <el-input-number v-model="currentAdvancedConfig.timeout" :min="1000" :max="120000" :step="1000" :controls="false"></el-input-number>
            </el-form-item>
            <el-form-item label="JNDI名称">
              <el-input v-model="currentAdvancedConfig.jndiName" placeholder="可选"></el-input>
            </el-form-item>
            <el-form-item label="信任所有证书">
              <el-switch v-model="currentAdvancedConfig.trustAllCerts"></el-switch>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="协议设置" name="protocol">
            <el-form-item label="协议类型">
              <el-select v-model="currentAdvancedConfig.protocol" placeholder="请选择">
                <el-option label="SMTP" value="smtp"></el-option>
                <el-option label="SMTPS" value="smtps"></el-option>
                <el-option label="IMAP" value="imap"></el-option>
                <el-option label="IMAPS" value="imaps"></el-option>
                <el-option label="POP3" value="pop3"></el-option>
                <el-option label="POP3S" value="pop3s"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="认证机制">
              <el-select v-model="currentAdvancedConfig.authMechanism" placeholder="请选择">
                <el-option label="默认" value="default"></el-option>
                <el-option label="LOGIN" value="login"></el-option>
                <el-option label="PLAIN" value="plain"></el-option>
                <el-option label="CRAM-MD5" value="cram-md5"></el-option>
                <el-option label="DIGEST-MD5" value="digest-md5"></el-option>
                <el-option label="XOAUTH2" value="xoauth2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="调试模式">
              <el-switch v-model="currentAdvancedConfig.debug"></el-switch>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="代理设置" name="proxy">
            <el-form-item label="使用代理">
              <el-switch v-model="currentAdvancedConfig.useProxy"></el-switch>
            </el-form-item>
            <el-form-item label="代理主机" v-if="currentAdvancedConfig.useProxy">
              <el-input v-model="currentAdvancedConfig.proxyHost" placeholder="代理服务器地址"></el-input>
            </el-form-item>
            <el-form-item label="代理端口" v-if="currentAdvancedConfig.useProxy">
              <el-input-number v-model="currentAdvancedConfig.proxyPort" :min="1" :max="65535" :controls="false"></el-input-number>
            </el-form-item>
            <el-form-item label="代理用户名" v-if="currentAdvancedConfig.useProxy">
              <el-input v-model="currentAdvancedConfig.proxyUser" placeholder="可选"></el-input>
            </el-form-item>
            <el-form-item label="代理密码" v-if="currentAdvancedConfig.useProxy">
              <el-input v-model="currentAdvancedConfig.proxyPassword" type="password" placeholder="可选"></el-input>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="自定义属性" name="custom">
            <div style="margin-bottom: 15px; padding: 10px; background: #fef7e6; border: 1px solid #fde2a7; border-radius: 4px;">
              <div style="display: flex; align-items: center;">
                <i class="el-icon-warning-outline" style="color: #E6A23C; margin-right: 8px; font-size: 16px;"></i>
                <div>
                  <div style="color: #E6A23C; font-weight: 500; font-size: 14px;">高级用户选项</div>
                  <div style="color: #999; font-size: 12px; margin-top: 2px;">这些属性直接传递给JavaMail，请确保您了解其含义</div>
                </div>
              </div>
            </div>
            <div v-for="(value, key, index) in currentAdvancedConfig.customProperties" :key="index" style="display: flex; margin-bottom: 10px;">
              <el-input v-model="customPropertyKeys[index]" placeholder="属性名" style="width: 40%; margin-right: 10px;" @change="updateCustomPropertyKey(index)"></el-input>
              <el-input v-model="currentAdvancedConfig.customProperties[customPropertyKeys[index]]" placeholder="属性值" style="width: 50%"></el-input>
              <el-button type="danger" icon="el-icon-delete" circle style="margin-left: 10px;" @click="removeCustomProperty(index)"></el-button>
            </div>
            <el-button type="primary" icon="el-icon-plus" size="small" @click="addCustomProperty">添加自定义属性</el-button>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="advancedConfigVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveAdvancedConfig">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import SectionTag from './SectionTag.vue';

export default {
  name: 'EmailSettings',
  components: { SectionTag },
  data() {
    return {
      emailConfigs: [],
      emailTestDialogVisible: false,
      emailTestForm: { testEmail: '', currentConfig: null, configIndex: -1 },
      emailDetailDialogVisible: false,
      currentEmailConfig: null,
      currentEmailConfigIndex: -1,
      defaultMailIndex: -1,
      touchStartX: 0,
      touchStartY: 0,
      touchStartTime: 0,
      isSwipeGesture: false,
      advancedConfigVisible: false,
      activeConfigTab: 'basic',
      currentAdvancedConfig: {
        connectionTimeout: 25000, timeout: 25000, jndiName: '', trustAllCerts: false,
        protocol: 'smtp', authMechanism: 'default', debug: false,
        useProxy: false, proxyHost: '', proxyPort: 8080, proxyUser: '', proxyPassword: '',
        customProperties: {}
      },
      customPropertyKeys: [],
      currentConfigIndex: -1,
      testEmailLoading: false,
      isMobileDevice: false,
      loading: false
    }
  },
  created() {
    this.getEmailConfigs();
  },
  mounted() {
    this.checkDeviceType();
    window.addEventListener('resize', this.checkDeviceType);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.checkDeviceType);
  },
  methods: {
    isMobile() {
      return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    },
    checkDeviceType() {
      this.isMobileDevice = window.innerWidth <= 768 || this.isMobile();
    },
    async getEmailConfigs() {
      try {
        const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getEmailConfigs", {}, true);
        this.emailConfigs = res.data || [];
        this.emailConfigs.forEach(config => {
          if (config.hasOwnProperty('ssl') && !config.hasOwnProperty('useSsl')) {
            config.useSsl = config.ssl;
            delete config.ssl;
          }
          if (config.hasOwnProperty('starttls') && !config.hasOwnProperty('useStarttls')) {
            config.useStarttls = config.starttls;
            delete config.starttls;
          }
        });
        await this.getDefaultMailConfigIndex();
      } catch (error) {
        this.$message.error("获取邮箱配置失败: " + error.message);
      }
    },
    async getDefaultMailConfigIndex() {
      try {
        const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getDefaultMailConfig", {}, true);
        this.defaultMailIndex = res.data || -1;
      } catch (error) {
        this.$message.error("获取默认邮箱索引失败: " + error.message);
      }
    },
    addEmailConfig() {
      const defaultConfig = {
        host: "", username: "", password: "", port: 465, senderName: "邮件服务",
        useSsl: true, useStarttls: false, auth: true, enabled: true,
        connectionTimeout: 25000, timeout: 25000, protocol: 'smtp',
        authMechanism: 'default', debug: false, useProxy: false
      };
      this.$confirm('是否使用邮箱服务预设？', '添加邮箱', {
        confirmButtonText: '使用预设', cancelButtonText: '使用默认配置',
        type: 'info', customClass: 'mobile-responsive-confirm', distinguishCancelAndClose: true
      }).then(() => {
        this.showPresetDialog();
      }).catch((action) => {
        if (action === 'cancel') {
          this.emailConfigs.push(defaultConfig);
        }
      });
    },
    showPresetDialog() {
      this.$alert(
        '<div style="text-align:center"><strong>请选择邮箱服务商</strong></div>' +
        '<div style="margin-top:15px; display:flex; flex-wrap:wrap; justify-content:space-around;">' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">网易163邮箱</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">QQ邮箱</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">Gmail</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">Outlook</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">阿里云邮箱</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">腾讯企业邮箱</button>' +
        '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;">网易企业邮箱</button>' +
        '</div>',
        '选择预设配置',
        { dangerouslyUseHTMLString: true, showConfirmButton: false, showCancelButton: true, cancelButtonText: '取消', closeOnClickModal: false, customClass: 'preset-dialog' }
      );
      setTimeout(() => {
        const btns = document.querySelectorAll('.preset-dialog .preset-btn');
        btns.forEach(btn => {
          btn.addEventListener('click', () => {
            document.querySelector('.el-message-box__close').click();
            const preset = btn.innerText.includes('网易企业邮箱') ? '163ex' :
                           btn.innerText.includes('网易163邮箱') ? '163' :
                           btn.innerText.includes('腾讯企业邮箱') ? 'qqex' :
                           btn.innerText.includes('QQ邮箱') ? 'qq' :
                           btn.innerText.includes('Gmail') ? 'gmail' :
                           btn.innerText.includes('Outlook') ? 'outlook' : 'aliyun';
            this.usePreset(preset);
          });
        });
      }, 100);
    },
    usePreset(preset) {
      let config = {
        senderName: "邮件服务", username: "", password: "", enabled: true, auth: true,
        connectionTimeout: 25000, timeout: 25000, protocol: 'smtp',
        authMechanism: 'default', debug: false, useProxy: false
      };
      const presets = {
        '163':   { host: "smtp.163.com",          port: 465, useSsl: true,  useStarttls: false },
        'qq':    { host: "smtp.qq.com",            port: 465, useSsl: true,  useStarttls: false },
        'gmail': { host: "smtp.gmail.com",         port: 587, useSsl: false, useStarttls: true },
        'outlook': { host: "smtp.office365.com",   port: 587, useSsl: false, useStarttls: true },
        'aliyun': { host: "smtp.aliyun.com",       port: 465, useSsl: true,  useStarttls: false },
        'qqex':  { host: "smtp.exmail.qq.com",     port: 465, useSsl: true,  useStarttls: false },
        '163ex': { host: "smtphz.qiye.163.com",    port: 465, useSsl: true,  useStarttls: false }
      };
      config = { ...config, ...(presets[preset] || {}) };
      this.emailConfigs.push(config);
      this.$message({ message: `已添加${preset}邮箱预设配置，请填写您的邮箱地址和授权码`, type: "success" });
    },
    saveEmailConfigs() {
      const validConfigs = this.emailConfigs.filter(c => c.host && c.username && c.password);
      if (validConfigs.length === 0) {
        this.$message.error("请至少配置一个完整的邮箱");
        return;
      }
      this.loading = true;
      this.$http.post(this.$constant.baseURL + "/webInfo/saveEmailConfigs?defaultIndex=" + this.defaultMailIndex, validConfigs, true)
        .then(() => { this.$message.success("邮箱配置保存成功"); })
        .catch((error) => { this.$message.error("保存失败: " + error.message); })
        .finally(() => { this.loading = false; });
    },
    deleteEmailConfig(index) {
      this.$confirm('确定要删除这个邮箱配置吗?', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning', customClass: 'mobile-responsive-confirm'
      }).then(() => {
        this.emailConfigs.splice(index, 1);
        if (this.defaultMailIndex === index) { this.defaultMailIndex = -1; }
        else if (this.defaultMailIndex > index) { this.defaultMailIndex--; }
        this.$message({ type: 'success', message: '删除成功!' });
      }).catch(() => {});
    },
    testEmailConfig(config, index) {
      this.emailTestForm.testEmail = "";
      this.emailTestForm.configIndex = index;
      this.emailTestDialogVisible = true;
    },
    submitTestEmail() {
      if (!this.emailTestForm.testEmail) {
        this.$message.error("请输入测试邮箱地址");
        return;
      }
      this.testEmailLoading = true;
      const testConfig = this.emailConfigs[this.emailTestForm.configIndex];
      const testData = {
        testEmail: this.emailTestForm.testEmail,
        config: {
          host: testConfig.host, port: testConfig.port, username: testConfig.username,
          password: testConfig.password, useSsl: testConfig.useSsl, useStarttls: testConfig.useStarttls,
          auth: testConfig.auth, senderName: testConfig.senderName
        }
      };
      this.$http.post(this.$constant.baseURL + "/webInfo/testEmailConfig", testData, true)
        .then(() => {
          this.$message.success("测试邮件发送成功，请查收");
          this.emailTestDialogVisible = false;
        })
        .catch(error => { this.$message.error("测试邮件发送失败: " + error.message); })
        .finally(() => { this.testEmailLoading = false; });
    },
    showAdvancedConfig(index) {
      this.currentConfigIndex = index;
      const config = this.emailConfigs[index];
      this.currentAdvancedConfig = {
        connectionTimeout: config.connectionTimeout || 25000, timeout: config.timeout || 25000,
        jndiName: config.jndiName || '', trustAllCerts: config.trustAllCerts || false,
        protocol: config.protocol || 'smtp', authMechanism: config.authMechanism || 'default',
        debug: config.debug || false, useProxy: config.useProxy || false,
        proxyHost: config.proxyHost || '', proxyPort: config.proxyPort || 8080,
        proxyUser: config.proxyUser || '', proxyPassword: config.proxyPassword || '',
        customProperties: config.customProperties ? {...config.customProperties} : {}
      };
      this.customPropertyKeys = Object.keys(this.currentAdvancedConfig.customProperties || {});
      this.activeConfigTab = 'basic';
      this.advancedConfigVisible = true;
    },
    saveAdvancedConfig() {
      if (this.currentConfigIndex >= 0 && this.currentConfigIndex < this.emailConfigs.length) {
        const config = this.emailConfigs[this.currentConfigIndex];
        config.connectionTimeout = this.currentAdvancedConfig.connectionTimeout;
        config.timeout = this.currentAdvancedConfig.timeout;
        config.jndiName = this.currentAdvancedConfig.jndiName;
        config.trustAllCerts = this.currentAdvancedConfig.trustAllCerts;
        config.protocol = this.currentAdvancedConfig.protocol;
        config.authMechanism = this.currentAdvancedConfig.authMechanism;
        config.debug = this.currentAdvancedConfig.debug;
        config.useProxy = this.currentAdvancedConfig.useProxy;
        if (config.useProxy) {
          config.proxyHost = this.currentAdvancedConfig.proxyHost;
          config.proxyPort = this.currentAdvancedConfig.proxyPort;
          config.proxyUser = this.currentAdvancedConfig.proxyUser;
          config.proxyPassword = this.currentAdvancedConfig.proxyPassword;
        }
        config.customProperties = {...this.currentAdvancedConfig.customProperties};
        this.$message({ message: "高级配置已保存", type: "success" });
      }
      this.advancedConfigVisible = false;
    },
    addCustomProperty() {
      const newKey = `mail.property.${this.customPropertyKeys.length + 1}`;
      this.$set(this.currentAdvancedConfig.customProperties, newKey, '');
      this.customPropertyKeys.push(newKey);
    },
    updateCustomPropertyKey(index) {
      const oldKey = Object.keys(this.currentAdvancedConfig.customProperties)[index];
      const newKey = this.customPropertyKeys[index];
      if (oldKey !== newKey && newKey) {
        const value = this.currentAdvancedConfig.customProperties[oldKey];
        this.$delete(this.currentAdvancedConfig.customProperties, oldKey);
        this.$set(this.currentAdvancedConfig.customProperties, newKey, value);
      }
    },
    removeCustomProperty(index) {
      const key = this.customPropertyKeys[index];
      this.$delete(this.currentAdvancedConfig.customProperties, key);
      this.customPropertyKeys.splice(index, 1);
    },
    onPortChange(port, config) {
      if (port === 465) {
        if (!config.useSsl) { config.useSsl = true; this.$message({ message: "端口465必须使用SSL加密，已自动启用SSL", type: "info" }); }
        if (config.useStarttls) { config.useStarttls = false; }
      } else if (port === 587) {
        if (!config.useStarttls) { config.useStarttls = true; this.$message({ message: "端口587通常使用STARTTLS加密，已自动启用STARTTLS", type: "info" }); }
        if (config.useSsl) { config.useSsl = false; }
      } else if (port === 25) {
        this.$message({ message: "警告：端口25通常不加密，且可能被ISP阻止。建议使用465(SSL)或587(STARTTLS)端口", type: "warning" });
      }
    },
    onSslChange(value, config) {
      if (!value && config.port === 465) {
        this.$nextTick(() => { config.useSsl = true; this.$message({ message: "端口465必须使用SSL加密！", type: "warning" }); });
      }
    },
    onStarttlsChange(value, config) {
      if (!value && config.port === 587) {
        this.$message({ message: "警告：端口587通常需要启用STARTTLS，禁用可能导致邮件发送失败", type: "warning" });
      }
    },
    // 触摸事件（移动端）
    handleTouchStart(event) {
      if (!this.isMobileDevice) return;
      const touch = event.touches[0];
      this.touchStartX = touch.clientX;
      this.touchStartY = touch.clientY;
      this.touchStartTime = Date.now();
    },
    handleTouchEnd(event) {
      if (!this.isMobileDevice) return;
      const touch = event.changedTouches[0];
      const deltaX = Math.abs(touch.clientX - this.touchStartX);
      const deltaY = Math.abs(touch.clientY - this.touchStartY);
      const deltaTime = Date.now() - this.touchStartTime;
      if (deltaX > 10 || deltaY > 10 || deltaTime > 300) {
        this.isSwipeGesture = true;
        setTimeout(() => { this.isSwipeGesture = false; }, 100);
      } else {
        this.isSwipeGesture = false;
      }
    },
    handleEmailRowClick(row, column, event) {
      if (!this.isMobileDevice || this.isSwipeGesture) return;
      if (event.target.tagName === 'BUTTON' || event.target.closest('.el-button') ||
          event.target.closest('.el-switch') || event.target.closest('.el-input') ||
          event.target.closest('.el-input-number')) return;
      this.currentEmailConfig = { ...row };
      this.currentEmailConfigIndex = this.emailConfigs.indexOf(row);
      this.emailDetailDialogVisible = true;
    },
    testEmailFromDetail() {
      this.emailDetailDialogVisible = false;
      this.$nextTick(() => { this.testEmailConfig(this.currentEmailConfig, this.currentEmailConfigIndex); });
    },
    editEmailFromDetail() {
      this.emailDetailDialogVisible = false;
      this.$message.info('请在表格中直接编辑配置');
      this.$nextTick(() => {
        const el = this.$el.querySelector('.responsive-table-container');
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      });
    },
    showAdvancedFromDetail() {
      this.emailDetailDialogVisible = false;
      this.$nextTick(() => { this.showAdvancedConfig(this.currentEmailConfigIndex); });
    }
  }
}
</script>

<style scoped>
.responsive-table-container {
  width: 100%;
  overflow-x: auto;
}
.mobile-view-notice {
  animation: slideInDown 0.3s ease-out;
}
@keyframes slideInDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
.delete-btn {
  color: #f56c6c;
}
@media screen and (max-width: 768px) {
  .mobile-table .el-table__row {
    cursor: pointer;
    transition: background-color 0.2s ease;
  }
  .mobile-table .el-table__row:active {
    background-color: #f5f7fa !important;
  }
  .mobile-table .el-table__row:hover {
    background-color: #f0f9ff !important;
  }
}
.email-detail-dialog { max-width: 600px; }
.email-detail-content { padding: 0; }
.email-detail-content .el-descriptions { margin-bottom: 0; }
.email-detail-content .el-descriptions-item__label { width: 35%; font-weight: 500; background-color: #fafafa; }
.email-detail-content .el-descriptions-item__content { word-break: break-all; }
@media screen and (max-width: 768px) {
  .email-detail-content .el-descriptions-item__label { width: 40%; font-size: 13px; padding: 8px 10px !important; }
  .email-detail-content .el-descriptions-item__content { font-size: 13px; padding: 8px 10px !important; }
}
</style>
