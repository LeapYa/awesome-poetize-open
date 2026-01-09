<template>
  <div>
    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        基础信息
      </el-tag>
      <el-form :model="webInfo" :rules="rules" ref="ruleForm" label-width="100px"
               class="demo-ruleForm">
        <el-form-item label="网站名称" prop="webName">
          <el-input v-model="webInfo.webName"></el-input>
        </el-form-item>

        <el-form-item label="网站标题" prop="webTitle">
          <el-input v-model="webInfo.webTitle"></el-input>
        </el-form-item>

        <el-form-item label="网站地址" prop="siteAddress">
          <div class="site-address-container">
            <el-input 
              v-model="webInfo.siteAddress" 
              placeholder="自动检测的网站地址"
              :readonly="!editingSiteAddress"
              class="site-address-input">
            </el-input>
            
            <!-- 简约地址操作按钮 -->
            <div class="simple-address-actions" v-if="!editingSiteAddress">
              <el-button 
                size="small" 
                @click="detectSiteAddress" 
                :loading="detectingAddress"
                class="simple-address-btn">
                {{ detectingAddress ? '检测中...' : '自动检测' }}
              </el-button>
              
              <el-button 
                size="small" 
                @click="startEditSiteAddress"
                class="simple-address-btn">
                手动编辑
              </el-button>
            </div>
            
            <!-- 编辑状态下的按钮 -->
            <div class="simple-address-actions" v-if="editingSiteAddress">
              <el-button 
                size="small" 
                type="primary" 
                @click="editingSiteAddress = false"
                class="simple-address-btn">
                确认
              </el-button>
              
              <el-button 
                size="small" 
                @click="cancelEditSiteAddress"
                class="simple-address-btn">
                取消
              </el-button>
            </div>
          </div>
          <span class="tip">
            网站的完整访问地址，用于生成站点地图、二维码和其他功能。
            <strong>推荐使用自动检测</strong>，系统会根据当前访问地址自动填写。
          </span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <div style="display: flex; align-items: center;">
            <el-switch @change="changeWebStatus" v-model="webInfo.status"></el-switch>
            <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.status ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.status ? '已开启' : '已关闭' }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="鼠标点击效果" prop="mouseClickEffect">
          <el-select v-model="webInfo.mouseClickEffect" @change="handleMouseClickEffectChange" placeholder="请选择点击效果" :loading="mouseClickEffectLoading">
            <el-option 
              v-for="effect in mouseClickEffectOptions" 
              :key="effect.pluginKey" 
              :label="effect.pluginName" 
              :value="effect.pluginKey">
              <span>{{ effect.pluginName }}</span>
              <span v-if="effect.pluginDescription" style="color: #909399; font-size: 12px; margin-left: 8px;">（{{ effect.pluginDescription }}）</span>
            </el-option>
          </el-select>
          <router-link to="/pluginManager" style="margin-left: 10px; font-size: 12px;">管理插件</router-link>
        </el-form-item>

        <el-form-item label="看板娘" prop="enableWaifu">
          <div style="display: flex; align-items: center;">
            <el-switch @change="handleWaifuChange" v-model="webInfo.enableWaifu"></el-switch>
            <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.enableWaifu ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.enableWaifu ? '已开启' : '已关闭' }}
            </span>
          </div>
        </el-form-item>

        <!-- 看板娘显示模式 - 仅在看板娘开启时显示 -->
        <el-form-item v-if="webInfo.enableWaifu" label="显示模式" prop="waifuDisplayMode">
          <el-radio-group v-model="webInfo.waifuDisplayMode" @change="handleWaifuDisplayModeChange">
            <el-radio label="live2d">
              <span>Live2D看板娘</span>
              <span style="color: #909399; font-size: 12px; margin-left: 8px;">（完整动画角色）</span>
            </el-radio>
            <el-radio label="button">
              <span>简洁按钮</span>
              <span style="color: #909399; font-size: 12px; margin-left: 8px;">（圆形AI聊天按钮）</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- AI聊天配置区域 - 仅在看板娘开启时显示 -->
        <div v-if="webInfo.enableWaifu" style="margin-left: 20px; padding-left: 20px; margin-top: 20px; margin-bottom: 20px;">
          <el-divider content-position="left">
            <span style="color: #409EFF; font-weight: 500;">
              看板娘AI聊天配置
            </span>
          </el-divider>
          
          <!-- PC端：折叠面板形式展示配置 -->
          <el-collapse v-model="activeAiConfigPanels" accordion style="margin: 0 50px;" class="ai-config-collapse" v-if="!isMobileView">
            <!-- AI模型配置面板 -->
            <el-collapse-item title="AI模型配置" name="model">
              <AiModelConfig v-model="aiConfigs.modelConfig" />
            </el-collapse-item>
            
            <!-- 聊天设置面板 -->
            <el-collapse-item title="聊天设置" name="chat">
              <AiChatSettings v-model="aiConfigs.chatConfig" />
            </el-collapse-item>
            
            <!-- 外观设置面板 -->
            <el-collapse-item title="外观设置" name="appearance">
              <AiAppearanceConfig v-model="aiConfigs.appearanceConfig" />
            </el-collapse-item>
            
            <!-- 高级设置面板 -->
            <el-collapse-item title="高级设置" name="advanced">
              <AiAdvancedConfig 
                v-model="aiConfigs.advancedConfig"
                @export-config="exportAiConfig"
                @import-config="importAiConfig" />
            </el-collapse-item>
          </el-collapse>

          <!-- 移动端：卡片按钮形式 -->
          <div v-else class="ai-config-mobile-cards">
            <div class="config-card" @click="openMobileConfigDialog('model')">
              <i class="el-icon-setting"></i>
              <span>AI模型配置</span>
              <i class="el-icon-arrow-right"></i>
            </div>
            <div class="config-card" @click="openMobileConfigDialog('chat')">
              <i class="el-icon-chat-dot-round"></i>
              <span>聊天设置</span>
              <i class="el-icon-arrow-right"></i>
            </div>
            <div class="config-card" @click="openMobileConfigDialog('appearance')">
              <i class="el-icon-picture-outline"></i>
              <span>外观设置</span>
              <i class="el-icon-arrow-right"></i>
            </div>
            <div class="config-card" @click="openMobileConfigDialog('advanced')">
              <i class="el-icon-s-tools"></i>
              <span>高级设置</span>
              <i class="el-icon-arrow-right"></i>
            </div>
          </div>

          <!-- AI配置保存按钮 -->
          <div style="text-align: center; margin-top: 20px;">
            <el-button type="primary" @click="saveAiConfigs" :loading="savingAiConfigs">
              保存AI聊天配置
            </el-button>
          </div>
        </div>

        <!-- 移动端配置对话框 -->
        <el-dialog 
          :title="mobileConfigDialogTitle" 
          :visible.sync="mobileConfigDialogVisible"
          :fullscreen="false"
          :close-on-click-modal="false"
          width="90%"
          custom-class="centered-dialog mobile-ai-config-dialog">
          <div class="mobile-config-content">
            <AiModelConfig v-if="currentMobileConfig === 'model'" v-model="aiConfigs.modelConfig" />
            <AiChatSettings v-if="currentMobileConfig === 'chat'" v-model="aiConfigs.chatConfig" />
            <AiAppearanceConfig v-if="currentMobileConfig === 'appearance'" v-model="aiConfigs.appearanceConfig" />
            <AiAdvancedConfig 
              v-if="currentMobileConfig === 'advanced'"
              v-model="aiConfigs.advancedConfig"
              @export-config="exportAiConfig"
              @import-config="importAiConfig" />
          </div>
          <div slot="footer" class="dialog-footer">
            <el-button @click="mobileConfigDialogVisible = false">关闭</el-button>
            <el-button type="primary" @click="saveMobileConfig">保存</el-button>
          </div>
        </el-dialog>

        <!-- 自动夜间开关 -->
        <el-form-item label="自动夜间" prop="enableAutoNight">
          <el-switch v-model="webInfo.enableAutoNight"></el-switch>
          <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.enableAutoNight ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.enableAutoNight ? '已开启' : '已关闭' }}
            </span>
        </el-form-item>

        <!-- 夜间开始时间 -->
        <el-form-item v-if="webInfo.enableAutoNight" label="夜间开始(小时)">
          <el-input-number v-model="webInfo.autoNightStart" :min="0" :max="23"></el-input-number>
        </el-form-item>

        <!-- 夜间结束时间 -->
        <el-form-item v-if="webInfo.enableAutoNight" label="夜间结束(小时)">
          <el-input-number v-model="webInfo.autoNightEnd" :min="0" :max="23"></el-input-number>
        </el-form-item>

        <!-- 灰色模式开关 -->
        <el-form-item label="灰色模式" prop="enableGrayMode">
          <el-switch v-model="webInfo.enableGrayMode"></el-switch>
          <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.enableGrayMode ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.enableGrayMode ? '已开启' : '已关闭' }}
            </span>
        </el-form-item>

        <!-- 动态标题开关 -->
        <el-form-item label="动态标题" prop="enableDynamicTitle">
          <el-switch v-model="webInfo.enableDynamicTitle"></el-switch>
          <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.enableDynamicTitle ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.enableDynamicTitle ? '已开启' : '已关闭' }}
            </span>
          <div style="margin-top: 8px; font-size: 12px; color: #909399; line-height: 1.5;">
            <template v-if="webInfo.enableDynamicTitle">
              <span style="color: #67c23a;">✨ 当前状态：</span>
              当您离开页面时，标题会温柔地挽留"<span style="color: #f56c6c;">w(ﾟДﾟ)w 不要走！再看看嘛！</span>"；
              当您返回时，会热情地欢迎"<span style="color: #409EFF;">♪(^∇^*)欢迎肥来！</span>"，
              2秒后自动恢复原标题～
            </template>
            <template v-else>
              <span style="color: #c0c4cc;">📄 当前状态：</span>
              页面标题始终保持不变
            </template>
          </div>
        </el-form-item>

        <!-- 首页横幅高度 -->
        <el-form-item label="首页横幅高度" prop="homePagePullUpHeight">
          <el-input-number v-model="webInfo.homePagePullUpHeight" :min="10" :max="100" style="width: 120px;"></el-input-number>
          <span style="margin-left: 8px; color: #909399;">vh</span>
        </el-form-item>

        <!-- 移动端侧边栏配置 -->
        <el-form-item label="移动端侧边栏">
          <el-button @click="mobileDrawerDialogVisible = true" type="primary" size="small">
            <i class="el-icon-setting"></i> 配置移动端侧边栏
          </el-button>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            自定义移动端侧边栏的背景图片、颜色、渐变等样式
          </div>
        </el-form-item>

        <el-form-item label="背景" prop="backgroundImage">
          <div style="display: flex">
            <el-input v-model="webInfo.backgroundImage"></el-input>
            <el-image lazy class="table-td-thumb"
                      style="margin-left: 10px"
                      :preview-src-list="[webInfo.backgroundImage]"
                      :src="webInfo.backgroundImage"
                      fit="cover"></el-image>
          </div>
          <uploadPicture :isAdmin="true" :prefix="'webBackgroundImage'" style="margin-top: 15px"
                         @addPicture="addBackgroundImage"
                         :maxSize="10"
                         :maxNumber="1"></uploadPicture>
        </el-form-item>

        <el-form-item label="头像" prop="avatar">
          <div style="display: flex">
            <el-input v-model="webInfo.avatar"></el-input>
            <el-image lazy class="table-td-thumb"
                      style="margin-left: 10px"
                      :preview-src-list="[webInfo.avatar]"
                      :src="webInfo.avatar"
                      fit="cover"></el-image>
          </div>
          <uploadPicture :isAdmin="true" :prefix="'webAvatar'" style="margin-top: 15px" @addPicture="addAvatar"
                         :maxSize="2"
                         :maxNumber="1"></uploadPicture>
        </el-form-item>
        
        <!-- 极简页脚开关 -->
        <el-form-item label="极简页脚" prop="minimalFooter">
          <div style="display: flex; align-items: center;">
            <el-switch v-model="webInfo.minimalFooter"></el-switch>
            <span :style="{
                marginLeft: '10px',
                fontSize: '12px',
                color: webInfo.minimalFooter ? '#67c23a' : '#f56c6c'
              }">
              {{ webInfo.minimalFooter ? '已开启' : '已关闭' }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="页脚文案" prop="footer">
          <el-input 
            v-model="webInfo.footer" 
            placeholder="页脚文案（极简页脚开启时不显示）"
            :disabled="webInfo.minimalFooter">
          </el-input>
        </el-form-item>

        <el-form-item label="页脚背景" prop="footerBackgroundImage">
          <div style="display: flex">
            <el-input v-model="webInfo.footerBackgroundImage" placeholder="页脚背景图片URL（可选）"></el-input>
            <el-image lazy class="table-td-thumb"
                      style="margin-left: 10px"
                      v-if="webInfo.footerBackgroundImage"
                      :preview-src-list="[webInfo.footerBackgroundImage]"
                      :src="webInfo.footerBackgroundImage"
                      fit="cover"></el-image>
          </div>
          <uploadPicture :isAdmin="true" :prefix="'footerBackground'" style="margin-top: 15px"
                         @addPicture="addFooterBackgroundImage"
                         :maxSize="10"
                         :maxNumber="1"></uploadPicture>
          
          <!-- 背景图片配置选项 -->
          <div v-if="webInfo.footerBackgroundImage" style="margin-top: 15px;">
            <el-divider content-position="left">背景图片设置</el-divider>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="背景大小" label-width="80px">
                  <el-select v-model="footerBgConfig.backgroundSize" placeholder="选择背景大小">
                    <el-option label="覆盖 (cover)" value="cover"></el-option>
                    <el-option label="包含 (contain)" value="contain"></el-option>
                    <el-option label="自动 (auto)" value="auto"></el-option>
                    <el-option label="拉伸 (100% 100%)" value="100% 100%"></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="背景位置" label-width="80px">
                  <el-select v-model="footerBgConfig.backgroundPosition" placeholder="选择背景位置">
                    <el-option label="居中" value="center center"></el-option>
                    <el-option label="顶部居中" value="center top"></el-option>
                    <el-option label="底部居中" value="center bottom"></el-option>
                    <el-option label="左上角" value="left top"></el-option>
                    <el-option label="右上角" value="right top"></el-option>
                    <el-option label="左下角" value="left bottom"></el-option>
                    <el-option label="右下角" value="right bottom"></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="重复方式" label-width="80px">
                  <el-select v-model="footerBgConfig.backgroundRepeat" placeholder="选择重复方式">
                    <el-option label="不重复" value="no-repeat"></el-option>
                    <el-option label="重复" value="repeat"></el-option>
                    <el-option label="水平重复" value="repeat-x"></el-option>
                    <el-option label="垂直重复" value="repeat-y"></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="透明度" label-width="80px">
                  <el-slider v-model="footerBgConfig.opacity"
                           :min="0"
                           :max="100"
                           :step="5"
                           :format-tooltip="val => val + '%'"
                           @input="handleOpacityChange">
                  </el-slider>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="文字阴影" label-width="80px">
                  <el-switch v-model="footerBgConfig.textShadow"></el-switch>
                  <span style="margin-left: 10px; color: #999; font-size: 12px;">增强文字可读性</span>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="遮罩颜色" label-width="80px">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <el-color-picker v-model="footerBgConfig.maskColor"
                                   :predefine="['#000000', '#1a1a1a', '#333333', '#444444', '#555555', '#666666', '#FFFFFF']"
                                   show-alpha
                                   color-format="rgba"
                                   @change="handleMaskColorChange">
                    </el-color-picker>
                    <span style="color: #999; font-size: 12px;">调整遮罩颜色和透明度</span>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="效果预览" label-width="80px">
                  <div style="width: 100px; height: 30px; border: 1px solid #ddd; border-radius: 4px; position: relative; overflow: hidden;">
                    <div v-if="webInfo.footerBackgroundImage"
                         :style="{
                           position: 'absolute',
                           top: 0,
                           left: 0,
                           right: 0,
                           bottom: 0,
                           backgroundImage: 'url(' + webInfo.footerBackgroundImage + ')',
                           backgroundSize: footerBgConfig.backgroundSize || 'cover',
                           backgroundPosition: footerBgConfig.backgroundPosition || 'center center',
                           backgroundRepeat: footerBgConfig.backgroundRepeat || 'no-repeat'
                         }"></div>
                    <div v-else style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #f0f0f0;"></div>
                    <div :style="{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      background: footerBgConfig.maskColor || 'rgba(0, 0, 0, 0.5)'
                    }"></div>
                    <span :style="{
                      position: 'relative',
                      zIndex: 10,
                      color: 'white',
                      fontSize: '11px',
                      display: 'block',
                      textAlign: 'center',
                      lineHeight: '30px',
                      textShadow: footerBgConfig.textShadow ? '0 2px 8px rgba(0, 0, 0, 0.8), 0 1px 3px rgba(0, 0, 0, 0.6)' : 'none'
                    }">样例文字</span>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="webInfo.email" placeholder="联系邮箱（用于隐私政策和侵权联系）"></el-input>
        </el-form-item>
      </el-form>
      <div class="myCenter" style="margin-bottom: 22px">
        <el-button type="primary" @click="submitForm('ruleForm')">保存基本信息</el-button>
      </div>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        公告
      </el-tag>
      <el-tag
        :key="i"
        v-for="(notice, i) in notices"
        closable
        :disable-transitions="false"
        @close="handleClose(notices, notice)">
        {{notice}}
      </el-tag>
      <el-input
        class="input-new-tag"
        v-if="inputNoticeVisible"
        v-model="inputNoticeValue"
        ref="saveNoticeInput"
        size="small"
        placeholder="请输入公告内容"
        @keyup.enter.native="handleInputNoticeConfirm"
        @blur="handleInputNoticeConfirm"
        @keydown.enter.native="handleInputNoticeConfirm">
      </el-input>
      <el-button v-else class="button-new-tag" size="small" @click="showNoticeInput()">+ 公告</el-button>
      <div class="myCenter" style="margin-bottom: 22px">
        <el-button type="primary" @click="saveNotice()">保存公告</el-button>
      </div>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        随机名称
      </el-tag>
      <el-tag
        :key="i"
        effect="dark"
        v-for="(name, i) in randomName"
        closable
        :disable-transitions="false"
        :type="types[Math.floor(Math.random() * 5)]"
        @close="handleClose(randomName, name)">
        {{name}}
      </el-tag>
      <el-input
        class="input-new-tag"
        v-if="inputRandomNameVisible"
        v-model="inputRandomNameValue"
        ref="saveRandomNameInput"
        size="small"
        placeholder="请输入随机名称"
        @keyup.enter.native="handleInputRandomNameConfirm"
        @blur="handleInputRandomNameConfirm"
        @keydown.enter.native="handleInputRandomNameConfirm">
      </el-input>
      <el-button v-else class="button-new-tag" size="small" @click="showRandomNameInput">+ 随机名称</el-button>
      <div class="myCenter" style="margin-bottom: 22px">
        <el-button type="primary" @click="saveRandomName()">保存随机名称</el-button>
      </div>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        随机头像
      </el-tag>
      <div :key="i"
           style="display: flex"
           v-for="(avatar, i) in randomAvatar">
        <el-tag
          style="white-space: normal;height: unset"
          closable
          :disable-transitions="false"
          @close="handleClose(randomAvatar, avatar)">
          {{avatar}}
        </el-tag>
        <div>
          <el-image lazy class="table-td-thumb"
                    style="margin: 10px"
                    :preview-src-list="[avatar]"
                    :src="avatar"
                    fit="cover"></el-image>
        </div>
      </div>

      <el-input
        class="input-new-tag"
        v-if="inputRandomAvatarVisible"
        v-model="inputRandomAvatarValue"
        ref="saveRandomAvatarInput"
        size="small"
        @keyup.enter.native="handleInputRandomAvatarConfirm"
        @blur="handleInputRandomAvatarConfirm">
      </el-input>
      <el-button v-else class="button-new-tag" size="small" @click="showRandomAvatarInput">+ 随机头像</el-button>
      <uploadPicture :isAdmin="true" :prefix="'randomAvatar'" style="margin: 10px" @addPicture="addRandomAvatar"
                     :maxSize="1"
                     :maxNumber="5"></uploadPicture>
      <div class="myCenter" style="margin-bottom: 22px">
        <el-button type="primary" @click="saveRandomAvatar()">保存随机头像</el-button>
      </div>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        随机封面
      </el-tag>
      <div :key="i"
           style="display: flex"
           v-for="(cover, i) in randomCover">
        <el-tag
          style="white-space: normal;height: unset"
          closable
          :disable-transitions="false"
          @close="handleClose(randomCover, cover)">
          {{cover}}
        </el-tag>
        <div>
          <el-image lazy class="table-td-thumb"
                    style="margin: 10px"
                    :preview-src-list="[cover]"
                    :src="cover"
                    fit="cover"></el-image>
        </div>
      </div>

      <el-input
        class="input-new-tag"
        v-if="inputRandomCoverVisible"
        v-model="inputRandomCoverValue"
        ref="saveRandomCoverInput"
        size="small"
        @keyup.enter.native="handleInputRandomCoverConfirm"
        @blur="handleInputRandomCoverConfirm">
      </el-input>
      <el-button v-else class="button-new-tag" size="small" @click="showRandomCoverInput">+ 随机封面</el-button>
      <uploadPicture :isAdmin="true" :prefix="'randomCover'" style="margin: 10px" @addPicture="addRandomCover"
                     :maxSize="2"
                     :maxNumber="5"></uploadPicture>
      <div class="myCenter" style="margin-bottom: 40px">
        <el-button type="primary" @click="saveRandomCover()">保存随机封面</el-button>
      </div>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        邮箱配置
      </el-tag>
      
      <!-- 邮箱配置部分 -->
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
            
            <el-table-column
              prop="smtpHost"
              label="邮箱服务器"
              min-width="150">
          <template slot-scope="scope">
                <el-input v-model="scope.row.host" placeholder="例如: smtp.163.com"></el-input>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="smtpUsername"
              label="邮箱地址"
              min-width="180"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <el-input v-model="scope.row.username" placeholder="例如: example@163.com"></el-input>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="smtpPassword"
              label="授权码"
              min-width="150"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <el-input v-model="scope.row.password" placeholder="授权码" show-password></el-input>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="smtpPort"
              label="端口"
              min-width="80"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <el-input-number v-model="scope.row.port" :min="1" :max="65535" :controls="false" style="width: 100%" @change="(value) => onPortChange(value, scope.row, scope.$index)"></el-input-number>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="senderName"
              label="发件人名称"
              min-width="120"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <el-input v-model="scope.row.senderName" placeholder="发件人显示名称"></el-input>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="ssl"
              label="SSL"
              min-width="80"
              align="center">
          <template slot-scope="scope">
                <div style="display: flex; align-items: center; justify-content: center;">
                  <el-tooltip 
                    :content="scope.row.port === 465 ? 
                      '✅ 端口465必须启用SSL加密' : 
                      'SSL是一种安全加密连接方式，适用于端口465'" 
                    placement="top">
                    <i class="el-icon-lock" 
                       :style="{
                         marginRight: '8px', 
                         color: scope.row.port === 465 ? '#F56C6C' : '#909399',
                         fontSize: '14px',
                         cursor: 'help'
                       }"></i>
                  </el-tooltip>
                  <el-switch v-model="scope.row.useSsl" @change="(value) => onSslChange(value, scope.row, scope.$index)"></el-switch>
                </div>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="starttls"
              label="STARTTLS"
              min-width="100"
              align="center"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <div style="display: flex; align-items: center; justify-content: center;">
                  <el-tooltip 
                    :content="scope.row.port === 587 ? 
                      '✅ 端口587推荐启用STARTTLS加密' : 
                      'STARTTLS是连接后升级加密的方式，适用于端口587'" 
                    placement="top">
                    <i class="el-icon-unlock" 
                       :style="{
                         marginRight: '8px', 
                         color: scope.row.port === 587 ? '#E6A23C' : '#909399',
                         fontSize: '14px',
                         cursor: 'help'
                       }"></i>
                  </el-tooltip>
                  <el-switch v-model="scope.row.useStarttls" @change="(value) => onStarttlsChange(value, scope.row, scope.$index)"></el-switch>
                </div>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="auth"
              label="认证"
              min-width="80"
              align="center"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
          <template slot-scope="scope">
                <el-switch v-model="scope.row.auth"></el-switch>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="enabled"
              label="启用"
              min-width="80"
              align="center">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.enabled"></el-switch>
          </template>
        </el-table-column>
            
            <el-table-column
              prop="operation"
              label="操作"
              min-width="180">
          <template slot-scope="scope">
                <div>
                  <el-button 
                    type="text" 
                    size="small" 
                    @click="testEmailConfig(scope.row, scope.$index)">
                    <i class="el-icon-check"></i> 测试
                  </el-button>
                  <el-button 
                    type="text" 
                    size="small"
                    @click="showAdvancedConfig(scope.$index)">
                    <i class="el-icon-setting"></i> 高级
                  </el-button>
                  <el-button 
                    type="text" 
                    size="small"
                    class="delete-btn"
                    @click="deleteEmailConfig(scope.$index)">
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
          <el-button type="success" size="small"  @click="addEmailConfig">添加邮箱</el-button>
        </div>
        <div style="margin-top: 10px; margin-bottom: 22px; text-align: center;">
          
          <el-button type="primary" @click="saveEmailConfigs">保存邮箱配置</el-button>
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
              <el-descriptions-item label="邮箱服务器">
                {{ currentEmailConfig.host || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱地址">
                {{ currentEmailConfig.username || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="授权码">
                <span style="font-family: monospace;">{{ currentEmailConfig.password ? '••••••••' : '未设置' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="端口">
                {{ currentEmailConfig.port || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="发件人名称">
                {{ currentEmailConfig.senderName || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="SSL加密">
                <el-tag :type="currentEmailConfig.useSsl ? 'success' : 'info'" size="small">
                  {{ currentEmailConfig.useSsl ? '已启用' : '未启用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="STARTTLS">
                <el-tag :type="currentEmailConfig.useStarttls ? 'success' : 'info'" size="small">
                  {{ currentEmailConfig.useStarttls ? '已启用' : '未启用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="认证">
                <el-tag :type="currentEmailConfig.auth ? 'success' : 'info'" size="small">
                  {{ currentEmailConfig.auth ? '已启用' : '未启用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentEmailConfig.enabled ? 'success' : 'danger'" size="small">
                  {{ currentEmailConfig.enabled ? '已启用' : '已禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            
            <!-- 操作按钮 -->
            <div style="margin-top: 20px; display: flex; gap: 10px; justify-content: center;">
              <el-button 
                type="primary" 
                size="small" 
                @click="testEmailFromDetail">
                <i class="el-icon-check"></i> 测试邮件
              </el-button>
              <el-button 
                size="small"
                @click="editEmailFromDetail">
                <i class="el-icon-edit"></i> 编辑配置
              </el-button>
              <el-button 
                size="small"
                @click="showAdvancedFromDetail">
                <i class="el-icon-setting"></i> 高级设置
              </el-button>
            </div>
          </div>
          <span slot="footer" class="dialog-footer">
            <el-button @click="emailDetailDialogVisible = false">关 闭</el-button>
          </span>
        </el-dialog>
      </el-card>
    </div>

    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        第三方登录配置
      </el-tag>
      
      <!-- 第三方登录配置 -->
      <el-card class="box-card third-login-config" shadow="never" style="margin-top: 20px; border: none;">
        
        <el-row style="margin-bottom: 20px;">
          <el-col :span="24">
            <el-form label-width="150px">
              <el-form-item label="启用第三方登录">
                <el-switch 
                  v-model="thirdLoginConfig.enable" 
                  @change="handleThirdLoginToggle"
                  active-color="#13ce66"
                  inactive-color="#ff4949">
                </el-switch>
                <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                  {{ thirdLoginConfig.enable ? '已启用' : '已禁用' }}
                </span>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>

        <div class="platform-cards">
          <!-- 使用v-for循环渲染平台卡片，提高代码扩展性 -->
          <el-card 
            v-for="platform in platformsConfig" 
            :key="platform.type" 
            shadow="never" 
            :class="['platform-card', `${platform.type}-card`]" 
            style="border: none;">
            <div class="platform-header">
              <div class="platform-logo">
                <img :src="getPlatformIcon(platform.type)" width="28" height="28" :alt="platform.name">
                <span class="platform-name">{{ platform.name }}</span>
              </div>
              <el-switch
                :value="thirdLoginConfig[platform.type] && thirdLoginConfig[platform.type].enabled"
                @change="handlePlatformToggle(platform.type, $event)"
                active-color="#13ce66"
                inactive-color="#ff4949"
                :disabled="!thirdLoginConfig.enable">
              </el-switch>
            </div>
            
            <div class="platform-form">
              <el-form label-position="top" :disabled="!thirdLoginConfig.enable || !(thirdLoginConfig[platform.type] && thirdLoginConfig[platform.type].enabled)">
                <!-- 根据平台类型显示不同输入字段 -->
                <template v-if="platform.type === 'twitter'">
                  <el-form-item label="Client Key">
                    <el-input 
                      v-model="thirdLoginConfig.twitter.client_key" 
                      placeholder="请输入Client Key">
                    </el-input>
                  </el-form-item>
                </template>
                <template v-else>
                  <el-form-item label="Client ID">
                    <el-input 
                      v-model="thirdLoginConfig[platform.type].client_id" 
                      placeholder="请输入Client ID">
                    </el-input>
                  </el-form-item>
                </template>
                
                <el-form-item label="Client Secret">
                  <el-input 
                    v-model="thirdLoginConfig[platform.type].client_secret" 
                    placeholder="请输入Client Secret"
                    show-password>
                  </el-input>
                </el-form-item>
                
                <el-form-item label="回调地址">
                  <el-input 
                    v-model="thirdLoginConfig[platform.type].redirect_uri" 
                    placeholder="请输入回调地址">
                  </el-input>
                </el-form-item>
              </el-form>
            </div>
            
            <div class="platform-actions">
              <el-button 
                type="text" 
                icon="el-icon-link"
                :disabled="!thirdLoginConfig.enable || !(thirdLoginConfig[platform.type] && thirdLoginConfig[platform.type].enabled)"
                @click="openDeveloperCenter(platform.developerUrl)">
                开发者中心
              </el-button>
              <el-button 
                type="text" 
                icon="el-icon-check"
                :disabled="!thirdLoginConfig.enable || !(thirdLoginConfig[platform.type] && thirdLoginConfig[platform.type].enabled)"
                @click="testLogin(platform.type)">
                测试
              </el-button>
            </div>
          </el-card>
        </div>

        <div class="form-tip" style="margin-top: 15px; font-size: 13px; color: #909399;">
          * 回调地址为 http://你的域名/callback/{平台标识}
        </div>

        <div style="margin-top: 20px;margin-bottom: 22px; text-align: center;">
          <el-button type="primary" @click="saveThirdLoginConfig" :loading="loading">保存第三方登录配置</el-button>
        </div>
      </el-card>
    </div>

    <!-- 智能验证码配置 -->
    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        智能验证码配置
      </el-tag>
      <el-card class="box-card captcha-config-card" shadow="never" style="margin-top: 5px; border: none;">
        <el-row style="margin-bottom: 20px;">
          <el-col :span="24">
            <el-form label-width="150px">
              <el-form-item label="启用智能验证码">
                <el-switch 
                  v-model="captchaConfig.enable" 
                  @change="handleCaptchaToggle">
                </el-switch>
                <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                  {{ captchaConfig.enable ? '已启用' : '已禁用' }}
                </span>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
        
        <el-divider content-position="center">应用场景</el-divider>
        
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="12">
            <el-card 
              shadow="hover" 
              :class="{ 'captcha-card-disabled': !captchaConfig.enable }"
              :body-style="{ padding: '15px' }">
              <div slot="header" class="clearfix">
                <span>登录验证</span>
                <el-switch 
                  v-model="captchaConfig.login" 
                  :disabled="!captchaConfig.enable"
                  style="float: right;">
                </el-switch>
              </div>
              <div>
                <p>在用户登录时启用验证码</p>
                <p style="color: #909399; font-size: 12px;">推荐启用，可以防止暴力破解攻击</p>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card 
              shadow="hover" 
              :class="{ 'captcha-card-disabled': !captchaConfig.enable }"
              :body-style="{ padding: '15px' }">
              <div slot="header" class="clearfix">
                <span>注册验证</span>
                <el-switch 
                  v-model="captchaConfig.register" 
                  :disabled="!captchaConfig.enable"
                  style="float: right;">
                </el-switch>
              </div>
              <div>
                <p>在用户注册时启用验证码</p>
                <p style="color: #909399; font-size: 12px;">推荐启用，可以防止批量注册机器人</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="12">
            <el-card 
              shadow="hover" 
              :class="{ 'captcha-card-disabled': !captchaConfig.enable }"
              :body-style="{ padding: '15px' }">
              <div slot="header" class="clearfix">
                <span>评论验证</span>
                <el-switch 
                  v-model="captchaConfig.comment" 
                  :disabled="!captchaConfig.enable"
                  style="float: right;">
                </el-switch>
              </div>
              <div>
                <p>在用户发表评论时启用验证码</p>
                <p style="color: #909399; font-size: 12px;">可以减少垃圾评论</p>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card 
              shadow="hover" 
              :class="{ 'captcha-card-disabled': !captchaConfig.enable }"
              :body-style="{ padding: '15px' }">
              <div slot="header" class="clearfix">
                <span>密码重置验证</span>
                <el-switch 
                  v-model="captchaConfig.reset_password" 
                  :disabled="!captchaConfig.enable"
                  style="float: right;">
                </el-switch>
              </div>
              <div>
                <p>在用户重置密码时启用验证码</p>
                <p style="color: #909399; font-size: 12px;">推荐启用，可以提高账号安全性</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-divider content-position="center">设备适配</el-divider>
        
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="12">
            <el-form label-width="200px">
              <el-form-item label="屏幕宽度阈值(px)">
                <el-tooltip content="小于此宽度使用滑动验证码，大于等于此宽度使用勾选验证码" placement="top">
                  <el-input-number 
                    v-model="captchaConfig.screenSizeThreshold"
                    :min="320"
                    :max="1200"
                    :step="1"
                    :disabled="!captchaConfig.enable">
                  </el-input-number>
                </el-tooltip>
              </el-form-item>
            </el-form>
          </el-col>
          <el-col :span="12">
            <el-form label-width="200px">
              <el-form-item label="移动设备强制使用滑动验证">
                <el-tooltip content="在触摸设备上强制使用滑动验证码，无论屏幕大小" placement="top">
                  <el-switch 
                    v-model="captchaConfig.forceSlideForMobile"
                    :disabled="!captchaConfig.enable">
                  </el-switch>
                </el-tooltip>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
        
        <el-divider content-position="center">验证码参数</el-divider>
        
        <el-tabs v-model="activeCaptchaTab" :disabled="!captchaConfig.enable">
          <el-tab-pane label="勾选验证参数" name="checkbox">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form label-width="200px">
                  <el-form-item label="轨迹敏感度阈值">
                    <el-tooltip content="直线率超过此值将被判定为机器人，值越小越严格" placement="top">
                      <el-slider 
                        v-model="captchaConfig.checkbox.trackSensitivity" 
                        :min="0.8" 
                        :max="0.99" 
                        :step="0.01"
                        :format-tooltip="value => value ? value.toFixed(2) : '0'"
                        :disabled="!captchaConfig.enable">
                      </el-slider>
                    </el-tooltip>
                  </el-form-item>
                </el-form>
              </el-col>
              <el-col :span="12">
                <el-form label-width="200px">
                  <el-form-item label="最少轨迹点数">
                    <el-tooltip content="鼠标轨迹至少需要记录的点数，越多越严格" placement="top">
                      <el-input-number 
                        v-model="captchaConfig.checkbox.minTrackPoints"
                        :min="1"
                        :max="10"
                        :step="1"
                        :disabled="!captchaConfig.enable">
                      </el-input-number>
                    </el-tooltip>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>
          </el-tab-pane>
          
          <el-tab-pane label="滑动验证参数" name="slide">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form label-width="200px">
                  <el-form-item label="精确度">
                    <el-tooltip content="滑动验证的精确度，值越小越精确" placement="top">
                      <el-input-number 
                        v-model="captchaConfig.slide.accuracy"
                        :min="1"
                        :max="10"
                        :step="1"
                        :disabled="!captchaConfig.enable">
                      </el-input-number>
                    </el-tooltip>
                  </el-form-item>
                </el-form>
              </el-col>
              <el-col :span="12">
                <el-form label-width="200px">
                  <el-form-item label="成功阈值">
                    <el-tooltip content="滑动到最大距离的比例视为成功，值越大越难" placement="top">
                      <el-slider 
                        v-model="captchaConfig.slide.successThreshold" 
                        :min="0.8" 
                        :max="0.99" 
                        :step="0.01"
                        :format-tooltip="value => value ? (value * 100).toFixed(0) + '%' : '0%'"
                        :disabled="!captchaConfig.enable">
                      </el-slider>
                    </el-tooltip>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
        
        <div style="margin-top: 20px;margin-bottom: 22px; text-align: center;">
          <el-button type="primary" @click="saveCaptchaConfig" :loading="captchaLoading">保存智能验证码配置</el-button>
        </div>
      </el-card>
    </div>

    <!-- 导航栏配置 -->
    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        导航栏配置
      </el-tag>

      <!-- 导航栏配置部分 -->
      <el-card class="box-card" shadow="never" style="margin-top: 5px; border: none;">
        <!-- 响应式表格 -->
        <div class="responsive-table-container">
          <el-table
            :data="navItems"
            border
            style="width: 100%"
            :class="{'mobile-table': isMobileDevice}"
            size="small"
            @row-click="handleNavRowClick"
            @touchstart.native="handleTouchStart"
            @touchend.native="handleTouchEnd">

            <el-table-column
              label="排序"
              width="60"
              align="center">
              <template slot-scope="scope">
                <span style="color: #909399;">{{ scope.$index + 1 }}</span>
              </template>
            </el-table-column>

            <el-table-column
              prop="name"
              label="名称"
              min-width="120">
              <template slot-scope="scope">
                <el-input v-model="scope.row.name" placeholder="导航项名称"></el-input>
              </template>
            </el-table-column>

            <el-table-column
              prop="icon"
              label="图标"
              min-width="100"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
              <template slot-scope="scope">
                <el-input v-model="scope.row.icon" placeholder="🏡">
                  <template slot="prepend">
                    <span style="font-size: 18px;">{{scope.row.icon}}</span>
                  </template>
                </el-input>
              </template>
            </el-table-column>

            <el-table-column
              prop="link"
              label="链接"
              min-width="120"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
              <template slot-scope="scope">
                <el-input v-model="scope.row.link" placeholder="/"></el-input>
              </template>
            </el-table-column>

            <el-table-column
              prop="type"
              label="类型"
              min-width="110"
              :class-name="isMobileDevice ? 'hidden-xs-only' : ''">
              <template slot-scope="scope">
                <el-select v-model="scope.row.type" placeholder="请选择" style="width: 100%">
                  <el-option label="普通链接" value="internal"></el-option>
                  <el-option label="下拉菜单" value="dropdown"></el-option>
                  <el-option label="特殊功能" value="special"></el-option>
                </el-select>
              </template>
            </el-table-column>

            <el-table-column
              prop="enabled"
              label="启用"
              width="80"
              align="center">
              <template slot-scope="scope">
                <el-switch v-model="scope.row.enabled"></el-switch>
              </template>
            </el-table-column>

            <el-table-column
              prop="operation"
              label="操作"
              min-width="180"
              fixed="right">
              <template slot-scope="scope">
                <div>
                  <el-button
                    type="text"
                    size="small"
                    :disabled="scope.$index === 0"
                    @click="moveNavItem(scope.$index, 'up')">
                    <i class="el-icon-top"></i> 上移
                  </el-button>
                  <el-button
                    type="text"
                    size="small"
                    :disabled="scope.$index === navItems.length - 1"
                    @click="moveNavItem(scope.$index, 'down')">
                    <i class="el-icon-bottom"></i> 下移
                  </el-button>
                  <el-button
                    type="text"
                    size="small"
                    class="delete-btn"
                    @click="deleteNavItem(scope.$index)">
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

        <!-- 导航栏预览 -->
        <div style="margin-top: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px;">
          <div style="margin-bottom: 10px; font-weight: bold; color: #606266;">
            <i class="el-icon-view"></i> 导航栏预览：
          </div>
          <div class="nav-preview-container">
            <div
              v-for="(item, index) in navItems.filter(i => i.enabled)"
              :key="index"
              class="nav-item-preview">
              <span class="nav-item-icon">{{item.icon}}</span>
              <span class="nav-item-name">{{item.name}}</span>
              <span v-if="item.type === 'dropdown'" class="nav-item-dropdown">▼</span>
            </div>
          </div>
        </div>

        <div style="margin-top: 37px; text-align: center;">
          <el-button type="success" size="small" @click="addNavItem">添加导航项</el-button>
        </div>
        <div style="margin-top: 10px; margin-bottom: 22px; text-align: center;">
          <el-button type="warning" @click="resetToDefaultNav">恢复默认</el-button>
          <el-button type="primary" @click="saveNavConfig" :loading="navLoading">保存导航栏配置</el-button>
        </div>

        <!-- 导航项配置详情对话框（移动端） -->
        <el-dialog
          title="导航项配置详情"
          :visible.sync="navDetailDialogVisible"
          width="90%"
          :close-on-click-modal="false">
          <el-form v-if="currentNavItem" :model="currentNavItem" label-width="80px">
            <el-form-item label="名称">
              <el-input v-model="currentNavItem.name" placeholder="导航项名称"></el-input>
            </el-form-item>
            <el-form-item label="图标">
              <el-input v-model="currentNavItem.icon" placeholder="例如: 🏡">
                <template slot="prepend">
                  <span style="font-size: 20px;">{{currentNavItem.icon}}</span>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="链接">
              <el-input v-model="currentNavItem.link" placeholder="例如: /"></el-input>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="currentNavItem.type" placeholder="请选择" style="width: 100%">
                <el-option label="普通链接" value="internal"></el-option>
                <el-option label="下拉菜单" value="dropdown"></el-option>
                <el-option label="特殊功能" value="special"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="currentNavItem.enabled"></el-switch>
            </el-form-item>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="navDetailDialogVisible = false">确 定</el-button>
          </span>
        </el-dialog>
      </el-card>
    </div>

    <!-- 添加API配置 -->
    <div>
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        API 配置
      </el-tag>
      
      <!-- API 配置部分 -->
      <el-card class="box-card" shadow="never" style="margin-top: 5px; border: none;">
        <!-- API开关 -->
        <div style="margin-bottom: 20px;">
          <el-form :model="apiConfig" label-width="120px">
            <el-form-item label="启用API">
              <el-switch 
                v-model="apiConfig.enabled" 
                @change="handleApiToggle"
                active-color="#13ce66"
                inactive-color="#ff4949">
              </el-switch>
            </el-form-item>
            
            <el-form-item v-if="apiConfig.enabled" label="API密钥">
              <div style="display: flex; align-items: center;">
                <el-input 
                  v-model="apiConfig.apiKey" 
                  placeholder="API密钥" 
                  :disabled="true"
                  style="width: 350px;">
                </el-input>
                <el-button 
                  type="primary" 
                  size="small" 
                  style="margin-left: 10px;"
                  @click="regenerateApiKey">
                  重新生成
                </el-button>
              </div>
            </el-form-item>
            
            <el-form-item v-if="apiConfig.enabled" label="API端点">
              <div>
                <p style="margin: 5px 0; color: #606266;">文章创建API:</p>
                <el-input
                  :value="$constant.baseURL + '/api/article/create'"
                  :disabled="true"
                  style="width: 450px; margin-bottom: 10px;">
                </el-input>
              </div>
            </el-form-item>
            
            <el-form-item v-if="apiConfig.enabled" label="API文档">
              <el-collapse>
                <el-collapse-item title="API调用概述" name="0">
                  <div style="padding: 10px;">
                    <p><strong>API认证:</strong></p>
                    <p>所有API请求都需要在请求头中添加<code>X-API-KEY</code>字段，值为API密钥。</p>
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,       // 0表示成功，非0表示错误
  "message": null, // 错误信息，成功时为null
  "data": { ... }  // 响应数据
}
                    </pre>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="创建文章 API" name="1">
                  <div style="padding: 10px;">
                    <p><strong>请求格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
POST {{$constant.baseURL}}/api/article/create
Content-Type: application/json
X-API-KEY: {{apiConfig.apiKey}}

{
  "title": "文章标题",
  "content": "文章内容，支持Markdown格式",
  "cover": "封面图片URL(可选)",
  "sortName": "分类名称(将自动创建不存在的分类)",
  "labelName": "标签名称(将自动创建不存在的标签)",
  "summary": "文章摘要(可选)",
  "password": "文章密码(可选)",
  "viewStatus": true,  // 是否可见
  "commentStatus": true,  // 是否允许评论
  "submitToSearchEngine": true  // 是否推送至搜索引擎
}
                    </pre>
                    
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,       // 0表示成功
  "message": null,
  "data": {
    "id": 123      // 创建的文章ID
  }
}
                    </pre>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="获取文章列表 API" name="2">
                  <div style="padding: 10px;">
                    <p><strong>请求格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/article/list?current=1&size=10&sortId=1&labelId=1&searchKey=关键词
X-API-KEY: {{apiConfig.apiKey}}
                    </pre>
                    <p><strong>参数说明:</strong></p>
                    <ul>
                      <li><code>current</code>: 当前页码，从1开始，默认为1</li>
                      <li><code>size</code>: 每页大小，默认为10</li>
                      <li><code>sortId</code>: 分类ID，可选</li>
                      <li><code>labelId</code>: 标签ID，可选</li>
                      <li><code>searchKey</code>: 搜索关键词，可选</li>
                    </ul>
                    
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": {
    "records": [
      {
        "id": 123,
        "articleTitle": "文章标题",
        "articleContent": "文章内容摘要...",
        "articleCover": "图片URL",
        "viewCount": 100,
        "commentCount": 5,
        "createTime": "2023-04-01 12:00:00",
        "sort": { "id": 1, "sortName": "分类名称" },
        "label": { "id": 1, "labelName": "标签名称" }
      }
      // ...更多文章记录
    ],
    "current": 1,
    "size": 10,
    "total": 42,
    "pages": 5
  }
}
                    </pre>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="获取文章详情 API" name="3">
                  <div style="padding: 10px;">
                    <p><strong>请求格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/article/123
X-API-KEY: {{apiConfig.apiKey}}
                    </pre>
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": {
    "id": 123,
    "articleTitle": "文章标题",
    "articleContent": "完整文章内容，包括Markdown格式",
    "articleCover": "图片URL",
    "viewCount": 100,
    "commentStatus": true,
    "recommendStatus": false,
    "viewStatus": true,
    "createTime": "2023-04-01 12:00:00",
    "updateTime": "2023-04-02 14:30:00",
    "sortId": 1,
    "labelId": 1,
    "sortName": "分类名称",
    "labelName": "标签名称",
    "sort": { "id": 1, "sortName": "分类名称" },
    "label": { "id": 1, "labelName": "标签名称" }
  }
}
                    </pre>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="获取分类列表 API" name="4">
                  <div style="padding: 10px;">
                    <p><strong>请求格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/categories
X-API-KEY: {{apiConfig.apiKey}}
                    </pre>
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": [
    {
      "id": 1,
      "sortName": "技术文章",
      "sortDescription": "技术类文章",
      "sortType": 0,
      "priority": 1
    },
    // ...更多分类
  ]
}
                    </pre>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="获取标签列表 API" name="5">
                  <div style="padding: 10px;">
                    <p><strong>请求格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/tags
X-API-KEY: {{apiConfig.apiKey}}
                    </pre>
                    <p><strong>响应格式:</strong></p>
                    <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": [
    {
      "id": 1,
      "sortId": 1,
      "labelName": "Java",
      "labelDescription": "Java编程语言"
    },
    // ...更多标签
  ]
}
                    </pre>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
            
            <el-form-item v-if="apiConfig.enabled">
              <div style="display: flex; justify-content: flex-end; margin-top: 20px;">
                
              </div>
            </el-form-item>
          </el-form>
        </div>
        
        <div style="margin-top: 20px; margin-bottom: 22px; text-align: center;">
          <el-button type="primary" @click="saveApiConfig" :loading="apiLoading">保存API配置</el-button>
          <el-button @click="resetApiConfig" style="margin-left: 10px;">重置修改</el-button>
        </div>
      </el-card>
    </div>

    <div>
      <el-button type="danger" @click="resetForm('ruleForm')">重置所有修改</el-button>
    </div>

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
            <!-- 高级用户警告面板 -->
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

    <!-- 移动端侧边栏配置对话框 -->
    <el-dialog
      title="移动端侧边栏配置"
      :visible.sync="mobileDrawerDialogVisible"
      width="900px"
      :close-on-click-modal="false"
      custom-class="centered-dialog mobile-drawer-config-dialog">
      
      <el-form label-width="100px" class="drawer-config-form">
        <!-- 标题类型 -->
        <el-form-item label="标题类型">
          <el-radio-group v-model="drawerConfig.titleType">
            <el-radio label="text">文字</el-radio>
            <el-radio label="avatar">头像</el-radio>
          </el-radio-group>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            选择显示文字标题或博客头像
          </div>
        </el-form-item>

        <!-- 标题文字 -->
        <el-form-item label="标题文字" v-if="drawerConfig.titleType === 'text'">
          <el-input v-model="drawerConfig.titleText" placeholder="欢迎光临"></el-input>
        </el-form-item>

        <!-- 头像大小 -->
        <el-form-item label="头像大小" v-if="drawerConfig.titleType === 'avatar'">
          <el-slider 
            v-model="drawerConfig.avatarSize" 
            :min="60" 
            :max="150" 
            :step="5"
            style="width: 300px;">
          </el-slider>
          <span style="margin-left: 10px;">{{ drawerConfig.avatarSize }}px</span>
        </el-form-item>

        <!-- 背景类型 -->
        <el-form-item label="背景类型">
          <el-radio-group v-model="drawerConfig.backgroundType">
            <el-radio label="image">背景图片</el-radio>
            <el-radio label="color">纯色</el-radio>
            <el-radio label="gradient">渐变色</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 背景图片 -->
        <el-form-item label="背景图片" v-if="drawerConfig.backgroundType === 'image'">
          <el-input v-model="drawerConfig.backgroundImage" placeholder="图片URL"></el-input>
          <uploadPicture 
            :isAdmin="true" 
            :prefix="'mobileDrawerBg'" 
            style="margin-top: 10px"
            @addPicture="addDrawerBackgroundImage"
            :maxSize="5"
            :maxNumber="1">
          </uploadPicture>
          <div v-if="drawerConfig.backgroundImage" style="margin-top: 10px;">
            <el-image 
              :src="drawerConfig.backgroundImage" 
              style="width: 200px; height: 150px;"
              fit="cover">
            </el-image>
          </div>
        </el-form-item>

        <!-- 纯色背景 -->
        <el-form-item label="背景颜色" v-if="drawerConfig.backgroundType === 'color'">
          <el-color-picker v-model="drawerConfig.backgroundColor"></el-color-picker>
          <span style="margin-left: 10px;">{{ drawerConfig.backgroundColor }}</span>
        </el-form-item>

        <!-- 渐变背景 -->
        <el-form-item label="渐变背景" v-if="drawerConfig.backgroundType === 'gradient'">
          <el-select v-model="drawerConfig.backgroundGradient" placeholder="选择渐变样式">
            <el-option 
              v-for="(gradient, index) in gradientPresets" 
              :key="index"
              :label="gradient.name" 
              :value="gradient.value">
              <div style="display: flex; align-items: center;">
                <div :style="{ 
                  width: '100px', 
                  height: '20px', 
                  background: gradient.value, 
                  marginRight: '10px',
                  borderRadius: '3px'
                }"></div>
                <span>{{ gradient.name }}</span>
              </div>
            </el-option>
          </el-select>
          <div style="margin-top: 10px;">
            <div :style="{ 
              width: '100%', 
              height: '80px', 
              background: drawerConfig.backgroundGradient,
              borderRadius: '8px'
            }"></div>
          </div>
        </el-form-item>

        <!-- 遮罩透明度 -->
        <el-form-item label="遮罩透明度">
          <el-slider 
            v-model="drawerConfig.maskOpacity" 
            :min="0" 
            :max="1" 
            :step="0.05"
            :format-tooltip="formatOpacity"
            style="width: 300px;">
          </el-slider>
          <span style="margin-left: 10px;">{{ (drawerConfig.maskOpacity * 100).toFixed(0) }}%</span>
        </el-form-item>

        <!-- 菜单字体颜色 -->
        <el-form-item label="字体颜色">
          <el-color-picker v-model="drawerConfig.menuFontColor"></el-color-picker>
          <span style="margin-left: 10px;">{{ drawerConfig.menuFontColor }}</span>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            设置标题和菜单项的字体颜色
          </div>
        </el-form-item>

        <!-- 显示边框 -->
        <el-form-item label="显示分隔线">
          <el-switch v-model="drawerConfig.showBorder"></el-switch>
        </el-form-item>

        <!-- 显示雪花装饰 -->
        <el-form-item label="雪花装饰" v-if="drawerConfig.titleType === 'avatar'">
          <el-switch v-model="drawerConfig.showSnowflake"></el-switch>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            在头像和菜单之间的分隔线上显示雪花装饰
          </div>
        </el-form-item>

        <!-- 边框颜色 -->
        <el-form-item label="分隔线颜色" v-if="drawerConfig.showBorder">
          <el-input v-model="drawerConfig.borderColor" placeholder="rgba(255, 255, 255, 0.15)">
            <template slot="prepend">
              <el-color-picker 
                v-model="borderColorPicker" 
                show-alpha
                @change="updateBorderColor">
              </el-color-picker>
            </template>
          </el-input>
        </el-form-item>

        <!-- 预览 -->
        <el-form-item label="效果预览">
          <div class="drawer-preview" :style="getDrawerPreviewStyle()">
            <div class="drawer-preview-header">
              <!-- 文字标题 -->
              <div v-if="drawerConfig.titleType === 'text'" class="preview-title" :style="{ color: drawerConfig.menuFontColor }">
                {{ drawerConfig.titleText || '欢迎光临' }}
              </div>
              <!-- 头像 -->
              <div v-else-if="drawerConfig.titleType === 'avatar'" class="preview-avatar">
                <el-image :src="webInfo.avatar || '/assets/avatar.jpg'" fit="cover">
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </el-image>
              </div>
            </div>
            <!-- 头像模式下的分隔线 -->
            <hr v-if="drawerConfig.titleType === 'avatar'" 
                :class="['preview-divider', { 'show-snowflake': drawerConfig.showSnowflake }]" />
            <div class="drawer-preview-menu">
              <div class="preview-menu-item" :style="getMenuItemStyle()">
                <span :style="{ color: drawerConfig.menuFontColor }">🏡 首页</span>
              </div>
              <div class="preview-menu-item" :style="getMenuItemStyle()">
                <span :style="{ color: drawerConfig.menuFontColor }">📑 分类</span>
              </div>
              <div class="preview-menu-item" :style="getMenuItemStyle()">
                <span :style="{ color: drawerConfig.menuFontColor }">❤️‍🔥 家</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer drawer-config-footer">
        <el-button @click="resetDrawerConfig" class="footer-btn">重置为默认</el-button>
        <el-button @click="mobileDrawerDialogVisible = false" class="footer-btn">取消</el-button>
        <el-button type="primary" @click="saveDrawerConfig" class="footer-btn">保存</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
    import { useMainStore } from '@/stores/main';

const uploadPicture = () => import( "../common/uploadPicture");
  const ApiTestTool = () => import( "./ApiTestTool");
  const AiModelConfig = () => import( "./aiChat/AiModelConfig");
  const AiChatSettings = () => import( "./aiChat/AiChatSettings");
  const AiAppearanceConfig = () => import( "./aiChat/AiAppearanceConfig");
  const AiAdvancedConfig = () => import( "./aiChat/AiAdvancedConfig");

  export default {
    components: {
      uploadPicture,
      ApiTestTool,
      AiModelConfig,
      AiChatSettings,
      AiAppearanceConfig,
      AiAdvancedConfig
    },
    data() {
      return {
        disabled: true,
        types: ['', 'success', 'info', 'danger', 'warning'],
        inputNoticeVisible: false,
        inputNoticeValue: "",
        inputRandomNameVisible: false,
        inputRandomNameValue: "",
        inputRandomAvatarVisible: false,
        inputRandomAvatarValue: "",
        inputRandomCoverVisible: false,
        inputRandomCoverValue: "",
        webInfo: {
          id: null,
          webName: "",
          webTitle: "",
          siteAddress: "",
          footer: "",
          backgroundImage: "/assets/backgroundPicture.jpg",
          avatar: "",
          waifuJson: "",
          enableWaifu: false,
          waifuDisplayMode: 'live2d', // 默认使用Live2D模式
          status: false,
          navConfig: "",
          footerBackgroundImage: "",
          footerBackgroundConfig: "",
          email: "",
          minimalFooter: false,
          // 自动夜间配置
          enableAutoNight: false,
          autoNightStart: 23,
          autoNightEnd: 7,
          enableGrayMode: false,
          enableDynamicTitle: true, // 动态标题开关，默认开启
          mouseClickEffect: 'none', // 鼠标点击效果：none/text/firework
          homePagePullUpHeight: 50,
        },
        // 鼠标点击效果插件列表
        mouseClickEffectOptions: [],
        mouseClickEffectLoading: false,
        // 网站地址编辑状态
        editingSiteAddress: false,
        detectingAddress: false,
        originalSiteAddress: "",
        notices: [],
        randomAvatar: [],
        randomName: [],
        randomCover: [],
        emailConfigs: [],
        emailTestDialogVisible: false,
        emailTestForm: {
          testEmail: '',
          currentConfig: null
        },
        emailDetailDialogVisible: false,
        currentEmailConfig: null,
        currentEmailConfigIndex: -1,
        defaultEmailIndex: -1,
        // 触摸滑动检测
        touchStartX: 0,
        touchStartY: 0,
        touchStartTime: 0,
        isSwipeGesture: false,
        advancedConfigVisible: false,
        activeConfigTab: 'basic',
        currentAdvancedConfig: {
          connectionTimeout: 25000,
          timeout: 25000,
          jndiName: '',
          trustAllCerts: false,
          protocol: 'smtp',
          authMechanism: 'default',
          debug: false,
          useProxy: false,
          proxyHost: '',
          proxyPort: 8080,
          proxyUser: '',
          proxyPassword: '',
          customProperties: {}
        },
        customPropertyKeys: [],
        currentConfigIndex: -1,
        thirdLoginConfig: {
          enable: true,
          github: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/github',
            enabled: true
          },
          google: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/google',
            enabled: true
          },
          twitter: {
            client_key: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/x',
            enabled: true
          },
          yandex: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/yandex',
            enabled: true
          },
          gitee: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/gitee',
            enabled: true
          },
          qq: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/qq',
            enabled: true
          },
          baidu: {
            client_id: '',
            client_secret: '',
            redirect_uri: this.$constant.pythonBaseURL + '/callback/baidu',
            enabled: true
          }
        },
        rules: {
          webName: [
            {required: true, message: '请输入网站名称', trigger: 'blur'},
            {min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'change'}
          ],
          webTitle: [
            {required: true, message: '请输入网站标题', trigger: 'blur'}
          ],
          siteAddress: [
            {required: true, message: '请输入网站地址或点击自动检测', trigger: 'blur'},
            {pattern: /^https?:\/\/.+/, message: '请输入完整的网站地址（http://或https://开头）', trigger: 'blur'}
          ],
          footer: [
            // 移除长度限制，页脚内容完全自由
          ],
          backgroundImage: [
            {required: true, message: '请输入背景', trigger: 'change'}
          ],
          email: [
            {required: true, message: '请输入联系邮箱', trigger: 'blur'}
          ],
          status: [
            {required: true, message: '请设置网站状态', trigger: 'change'}
          ],
          avatar: [
            {required: true, message: '请上传头像', trigger: 'change'}
          ]
        },
        testEmailLoading: false,
        isMobileDevice: false,
        loading: false,
        defaultMailIndex: -1,
        
        // 滑动验证码配置
        captchaConfig: {
          enable: false,
          login: true,
          register: true,
          comment: false,
          reset_password: true,
          screenSizeThreshold: 768,
          forceSlideForMobile: false,
          checkbox: {
            trackSensitivity: 0.9,
            minTrackPoints: 5
          },
          slide: {
            accuracy: 5,
            successThreshold: 0.9
          }
        },
        captchaLoading: false,
        activeCaptchaTab: 'checkbox',
        apiConfig: {
          enabled: false,
          apiKey: ''
        },
        apiLoading: false,
        apiTestTab: 'create',
        apiTestLoading: false,
        apiTestResult: null,
        apiTestForm: {
          title: '测试文章标题',
          content: '# 这是一个测试文章\n\n这是通过API测试工具创建的文章，支持**Markdown**格式。',
          sortName: '测试分类',
          labelName: '测试标签',
          cover: '',
          summary: '测试文章摘要'
        },
        apiQueryForm: {
          current: 1,
          size: 10,
          searchKey: ''
        },
        apiDetailForm: {
          id: null
        },
        navItems: [],
        navLoading: false,
        navDetailDialogVisible: false,
        currentNavItem: null,
        currentNavItemIndex: -1,
        // 移动端侧边栏配置
        mobileDrawerDialogVisible: false,
        drawerConfig: {
          titleType: 'text', // 'text' 或 'avatar'
          titleText: '欢迎光临',
          avatarSize: 100,
          backgroundType: 'image',
          backgroundImage: '/assets/toolbar.jpg',
          backgroundColor: '#000000',
          backgroundGradient: 'linear-gradient(60deg, #ffd7e4, #c8f1ff 95%)',
          maskOpacity: 0.7,
          menuFontColor: '#ffffff',
          showBorder: true,
          borderColor: 'rgba(255, 255, 255, 0.15)',
          showSnowflake: true
        },
        borderColorPicker: '#ffffff',
        gradientPresets: [
          { name: '粉蓝渐变（默认）', value: 'linear-gradient(60deg, #ffd7e4, #c8f1ff 95%)' },
          { name: '紫色梦幻', value: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
          { name: '海洋蓝', value: 'linear-gradient(135deg, #0093E9 0%, #80D0C7 100%)' },
          { name: '日落橙', value: 'linear-gradient(135deg, #FDBB2D 0%, #22C1C3 100%)' },
          { name: '粉色浪漫', value: 'linear-gradient(135deg, #F093FB 0%, #F5576C 100%)' },
          { name: '绿色清新', value: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
          { name: '深空紫', value: 'linear-gradient(135deg, #434343 0%, #000000 100%)' },
          { name: '炫彩渐变', value: 'linear-gradient(to right, #ee7752, #e73c7e, #23a6d5, #23d5ab)' },
          { name: '夜空蓝', value: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)' },
        ],
        defaultNavItems: [
          { name: "首页", icon: "🏡", link: "/", type: "internal" },
          { name: "分类", icon: "📑", link: "#", type: "dropdown" },
          { name: "家", icon: "❤️‍🔥", link: "/love", type: "internal" },
          { name: "友人帐", icon: "🤝", link: "/friends", type: "internal" },
          { name: "曲乐", icon: "🎵", link: "/music", type: "internal" },
          { name: "收藏夹", icon: "📁", link: "/favorites", type: "internal" },
          { name: "留言", icon: "📪", link: "/message", type: "internal" },
          { name: "联系我", icon: "💬", link: "#chat", type: "special" }
        ],
        footerBgConfig: {
          backgroundSize: 'cover',
          backgroundPosition: 'center center',
          backgroundRepeat: 'no-repeat',
          opacity: 100,
          textShadow: false,
          maskColor: 'rgba(0, 0, 0, 0.5)'
        },
        // AI聊天配置相关数据
        activeAiConfigPanels: [],
        savingAiConfigs: false,
        // 移动端配置对话框
        isMobileView: false,
        mobileConfigDialogVisible: false,
        currentMobileConfig: '',
        mobileConfigDialogTitle: '',
        aiConfigs: {
          modelConfig: {
            provider: 'openai',
            apiKey: '',
            model: 'gpt-3.5-turbo',
            baseUrl: '',
            temperature: 0.7,
            maxTokens: 1000,
            enabled: false,
            enableStreaming: false
          },
          chatConfig: {
            systemPrompt: "AI assistant. Respond in Chinese naturally.",
            welcomeMessage: "你好！有什么可以帮助你的吗？",
            historyCount: 10,
            rateLimit: 20,
            requireLogin: false,
            saveHistory: true,
            contentFilter: true,
            maxMessageLength: 500
          },
          appearanceConfig: {
            botAvatar: '',
            botName: 'AI助手',
            themeColor: '#409EFF',
            position: 'bottom-right',
            bubbleStyle: 'modern',
            typingAnimation: true,
            showTimestamp: true
          },
          advancedConfig: {
            proxy: '',
            timeout: 30,
            retryCount: 3,
            customHeaders: [],
            debugMode: false,
            enableThinking: false
          }
        }
      }
    },

    computed: {
      mainStore() {
        return useMainStore();
      },
      // 平台配置数据源
      platformsConfig() {
        return [
          {
            name: 'GitHub',
            type: 'github',
            developerUrl: 'https://github.com/settings/developers',
            useClientId: true
          },
          {
            name: 'Google',
            type: 'google',
            developerUrl: 'https://console.cloud.google.com/apis/credentials',
            useClientId: true
          },
          {
            name: 'Twitter',
            type: 'twitter',
            developerUrl: 'https://developer.twitter.com/en/portal/dashboard',
            useClientId: false
          },
          {
            name: 'Yandex',
            type: 'yandex',
            developerUrl: 'https://oauth.yandex.com/',
            useClientId: true
          },
          {
            name: 'Gitee',
            type: 'gitee',
            developerUrl: 'https://gitee.com/oauth/applications',
            useClientId: true
          },
          {
            name: 'QQ',
            type: 'qq',
            developerUrl: 'https://connect.qq.com/manage.html',
            useClientId: true
          },
          {
            name: 'Baidu',
            type: 'baidu',
            developerUrl: 'https://developer.baidu.com/console#app/project',
            useClientId: true
          }
          // 要添加新平台，只需在这里添加配置项即可
        ];
      },
      
      thirdLoginTableData() {
        return this.platformsConfig.map(platform => ({
          platform: platform.name,
          type: platform.type,
          config: this.thirdLoginConfig[platform.type] || { enabled: false },
          enabled: this.thirdLoginConfig[platform.type]?.enabled || false,
          developerUrl: platform.developerUrl
        }));
      }
    },

    created() {
      // 并行执行所有初始化请求
      this.initializeData();
    },

    mounted() {
      if (this.isMobile()) {
        this.isMobileDevice = true;
      }
      
      // 检测设备类型
      this.checkDeviceType();
      // 检测移动端视图
      this.checkMobileView();
      window.addEventListener('resize', this.checkDeviceType);
      window.addEventListener('resize', this.checkMobileView);
    },

    beforeDestroy() {
      // 移除监听器
      window.removeEventListener('resize', this.checkDeviceType);
      window.removeEventListener('resize', this.checkMobileView);
    },

    methods: {
      // 新增：并行初始化所有数据
      async initializeData() {
        const startTime = Date.now();
        
        try {
          // 并行执行所有请求
          const promises = [
            this.getWebInfo(),
            this.getThirdLoginConfig(),
            this.getCaptchaConfig(),
            this.getEmailConfigs(),
            this.getApiConfig(),
            this.loadAiConfigs(),
            this.loadMouseClickEffectPlugins()
          ];
          
          // 等待所有请求完成
          await Promise.allSettled(promises);
          
          const endTime = Date.now();
        } catch (error) {
          console.error("初始化数据时出错:", error);
        }
      },

      // 处理透明度变化
      handleOpacityChange(val) {
        // 获取当前遮罩颜色
        let maskColor = this.footerBgConfig.maskColor;
        if (!maskColor) {
          maskColor = 'rgba(0, 0, 0, 0.5)';
        }

        // 如果是 rgba 格式，直接替换 alpha 值
        const rgbaMatch = maskColor.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+),?\s*([\d.]+)?\)$/);
        if (rgbaMatch) {
          const r = parseInt(rgbaMatch[1]);
          const g = parseInt(rgbaMatch[2]);
          const b = parseInt(rgbaMatch[3]);
          const alpha = (val / 100).toFixed(2);
          this.footerBgConfig.maskColor = `rgba(${r}, ${g}, ${b}, ${alpha})`;
        } else {
          // 如果是其他格式（hex等），转为 rgba
          const hex = maskColor.replace('#', '');
          let r, g, b;
          if (hex.length === 3) {
            r = parseInt(hex[0] + hex[0], 16);
            g = parseInt(hex[1] + hex[1], 16);
            b = parseInt(hex[2] + hex[2], 16);
          } else {
            r = parseInt(hex.substring(0, 2), 16);
            g = parseInt(hex.substring(2, 4), 16);
            b = parseInt(hex.substring(4, 6), 16);
          }
          const alpha = (val / 100).toFixed(2);
          this.footerBgConfig.maskColor = `rgba(${r}, ${g}, ${b}, ${alpha})`;
        }
      },

      // 处理遮罩颜色变化
      handleMaskColorChange(val) {
        // 从新的 maskColor 中提取 alpha 值，更新透明度滑块
        if (val) {
          const rgbaMatch = val.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+),?\s*([\d.]+)?\)$/);
          if (rgbaMatch && rgbaMatch[4]) {
            const alpha = parseFloat(rgbaMatch[4]);
            this.footerBgConfig.opacity = Math.round(alpha * 100);
          }
        }
      },

      addBackgroundImage(res) {
        this.webInfo.backgroundImage = res;
      },
      addAvatar(res) {
        this.webInfo.avatar = res;
      },
      addRandomAvatar(res) {
        this.randomAvatar.push(res);
      },
      addRandomCover(res) {
        this.randomCover.push(res);
      },
      changeWebStatus() {
        this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", {
          id: this.webInfo.id,
          status: this.webInfo.status
        }, true)
          .then((res) => {
            this.getWebInfo();
            this.$message({
              message: "保存成功！",
              type: "success"
            });
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      
      // 自动检测网站地址
      async detectSiteAddress() {
        this.detectingAddress = true;
        
        try {
          const currentOrigin = window.location.origin;
          let finalUrl = currentOrigin;
          
          // 处理标准端口
          if (!currentOrigin.includes(':3000') && !currentOrigin.includes(':8080')) {
            if (currentOrigin.includes(':80') && currentOrigin.startsWith('http://')) {
              finalUrl = currentOrigin.replace(':80', '');
            } else if (currentOrigin.includes(':443') && currentOrigin.startsWith('https://')) {
              finalUrl = currentOrigin.replace(':443', '');
            }
          }
          
          this.webInfo.siteAddress = finalUrl;
          
        } catch (error) {
          console.error('自动检测网站地址失败:', error);
          this.$message({
            type: 'error',
            message: '自动检测失败，请手动输入网站地址'
          });
        } finally {
          this.detectingAddress = false;
        }
      },
      
      // 开始编辑网站地址
      startEditSiteAddress() {
        this.originalSiteAddress = this.webInfo.siteAddress;
        this.editingSiteAddress = true;
      },
      
      // 取消编辑网站地址
      cancelEditSiteAddress() {
        this.webInfo.siteAddress = this.originalSiteAddress;
        this.editingSiteAddress = false;
      },
      // 优化：将getWebInfo改为异步方法
      async getWebInfo() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/admin/webInfo/getAdminWebInfoDetails", {}, true);
            if (!this.$common.isEmpty(res.data)) {
              this.webInfo.id = res.data.id;
              this.webInfo.webName = res.data.webName;
              this.webInfo.webTitle = res.data.webTitle;
              this.webInfo.siteAddress = res.data.siteAddress || "";
              this.webInfo.footer = res.data.footer;
              this.webInfo.backgroundImage = res.data.backgroundImage;
              this.webInfo.avatar = res.data.avatar;
              this.webInfo.waifuJson = res.data.waifuJson;
              this.webInfo.enableWaifu = res.data.enableWaifu;
              this.webInfo.waifuDisplayMode = res.data.waifuDisplayMode || 'live2d';
              this.webInfo.status = res.data.status;
              this.webInfo.navConfig = res.data.navConfig || "[]";
              this.webInfo.footerBackgroundImage = res.data.footerBackgroundImage || "";
              this.webInfo.footerBackgroundConfig = res.data.footerBackgroundConfig || "";
              this.webInfo.email = res.data.email || "";
              this.notices = JSON.parse(res.data.notices);
              this.randomAvatar = JSON.parse(res.data.randomAvatar);
              this.randomName = JSON.parse(res.data.randomName);
              this.randomCover = JSON.parse(res.data.randomCover);
              this.webInfo.enableAutoNight = res.data.enableAutoNight ?? false;
              this.webInfo.autoNightStart = res.data.autoNightStart ?? 23;
              this.webInfo.autoNightEnd = res.data.autoNightEnd ?? 7;
              
              this.webInfo.homePagePullUpHeight = res.data.homePagePullUpHeight > 0 ? res.data.homePagePullUpHeight : 50;
              
              // 加载动态标题配置
              this.webInfo.enableDynamicTitle = res.data.enableDynamicTitle ?? true;
              
              // 加载鼠标点击效果配置
              this.webInfo.mouseClickEffect = res.data.mouseClickEffect || 'none';
              
              // 加载页脚背景配置
              if (this.webInfo.footerBackgroundConfig) {
                try {
                  this.footerBgConfig = JSON.parse(this.webInfo.footerBackgroundConfig);
                } catch (e) {
                  console.error("解析页脚背景配置失败:", e);
                  // 使用默认配置
                  this.footerBgConfig = {
                    backgroundSize: 'cover',
                    backgroundPosition: 'center center',
                    backgroundRepeat: 'no-repeat',
                    opacity: 100,
                    textShadow: false,
                    maskColor: 'rgba(0, 0, 0, 0.5)'
                  };
                }
              }
              
              // 解析移动端侧边栏配置
              if (res.data.mobileDrawerConfig) {
                try {
                  this.drawerConfig = JSON.parse(res.data.mobileDrawerConfig);
                  this.webInfo.mobileDrawerConfig = res.data.mobileDrawerConfig;
                } catch (e) {
                  console.error("解析移动端侧边栏配置失败:", e);
                  // 使用默认配置
                  this.drawerConfig = {
                    titleType: 'text',
                    titleText: '欢迎光临',
                    avatarSize: 100,
                    backgroundType: 'image',
                    backgroundImage: '/assets/toolbar.jpg',
                    backgroundColor: '#000000',
                    backgroundGradient: 'linear-gradient(60deg, #ffd7e4, #c8f1ff 95%)',
                    maskOpacity: 0.7,
                    menuFontColor: '#ffffff',
                    showBorder: true,
                    borderColor: 'rgba(255, 255, 255, 0.15)',
                    showSnowflake: true
                  };
                }
              }
              
              // 解析导航栏配置
              try {
                const parsedNavItems = JSON.parse(this.webInfo.navConfig || "[]");
                if (parsedNavItems.length > 0) {
                  // 直接加载到navItems数组
                  this.navItems = parsedNavItems.map((item, index) => ({
                    ...item,
                    enabled: item.enabled !== false // 确保 enabled 是布尔值
                  }));
                } else {
                  this.resetToDefaultNav();
                }
              } catch (e) {
                console.error("解析导航栏配置失败:", e);
                this.resetToDefaultNav();
              }
              
              // 更新mainStore中的webInfo，确保Live2D组件能立即响应变化
              this.mainStore.setWebInfo({...this.webInfo});
            }
        } catch (error) {
            this.$message({
              message: error.message,
              type: "error"
            });
          throw error; // 重新抛出错误，让Promise.allSettled能够捕获
        }
      },
      submitForm(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            // 只发送基本信息字段，不包括公告、随机名称等专门管理的字段
            const basicInfoToUpdate = {
              id: this.webInfo.id,
              webName: this.webInfo.webName,
              webTitle: this.webInfo.webTitle,
              siteAddress: this.webInfo.siteAddress,
              footer: this.webInfo.footer,
              backgroundImage: this.webInfo.backgroundImage,
              avatar: this.webInfo.avatar,
              waifuJson: this.webInfo.waifuJson,
              status: this.webInfo.status,
              enableWaifu: this.webInfo.enableWaifu,
              waifuDisplayMode: this.webInfo.waifuDisplayMode,
              homePagePullUpHeight: this.webInfo.homePagePullUpHeight,
              apiEnabled: this.webInfo.apiEnabled,
              apiKey: this.webInfo.apiKey,
              footerBackgroundImage: this.webInfo.footerBackgroundImage,
              footerBackgroundConfig: JSON.stringify(this.footerBgConfig),
              email: this.webInfo.email,
              minimalFooter: this.webInfo.minimalFooter,
              enableAutoNight: this.webInfo.enableAutoNight,
              autoNightStart: this.webInfo.autoNightStart,
              autoNightEnd: this.webInfo.autoNightEnd,
              enableGrayMode: this.webInfo.enableGrayMode,
              enableDynamicTitle: this.webInfo.enableDynamicTitle,
              mouseClickEffect: this.webInfo.mouseClickEffect
            };

            this.updateWebInfo(basicInfoToUpdate);
          } else {
            this.$message({
              message: "请完善必填项！",
              type: "error"
            });
          }
        });
      },
      resetForm(formName) {
        this.$refs[formName].resetFields();
        this.getWebInfo();
        this.resetToDefaultNav();
      },
      handleClose(array, item) {
        array.splice(array.indexOf(item), 1);
      },
      handleInputNoticeConfirm() {
        if (this.inputNoticeValue && this.inputNoticeValue.trim()) {
          this.notices.push(this.inputNoticeValue.trim());
        }
        this.inputNoticeVisible = false;
        this.inputNoticeValue = '';
      },
      showNoticeInput() {
        this.inputNoticeVisible = true;
        this.$nextTick(() => {
          this.$refs.saveNoticeInput.$refs.input.focus();
        });
      },
      saveNotice() {
        // 验证数据
        if (!this.webInfo.id) {
          this.$message.error('网站信息ID不存在，请刷新页面重试');
          return;
        }
        
        if (!Array.isArray(this.notices)) {
          this.notices = [];
        }
        
        // 过滤空值
        this.notices = this.notices.filter(notice => notice && notice.trim());
        
        // 使用专门的公告更新接口
        let param = {
          id: this.webInfo.id,
          notices: JSON.stringify(this.notices)
        }
        
        
        this.$http.post(this.$constant.baseURL + "/webInfo/updateNotices", param, true)
          .then((res) => {
            if (res.code === 200) {
              this.getWebInfo();
              this.$message({
                message: "公告保存成功！",
                type: "success"
              });
            } else {
              this.$message({
                message: "保存失败: " + res.message,
                type: "error"
              });
            }
          })
          .catch((error) => {
            console.error('保存公告失败:', error);
            this.$message({
              message: "保存失败: " + (error.response?.data?.message || error.message),
              type: "error"
            });
          });
      },
      handleInputRandomNameConfirm() {
        if (!Array.isArray(this.randomName)) {
          this.randomName = [];
        }
        if (this.inputRandomNameValue && this.inputRandomNameValue.trim()) {
          this.randomName.push(this.inputRandomNameValue.trim());
        }
        this.inputRandomNameVisible = false;
        this.inputRandomNameValue = '';
      },
      showRandomNameInput() {
        this.inputRandomNameVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomNameInput.$refs.input.focus();
        });
      },
      saveRandomName() {
        // 验证数据
        if (!this.webInfo.id) {
          this.$message.error('网站信息ID不存在，请刷新页面重试');
          return;
        }
        
        if (!Array.isArray(this.randomName)) {
          this.randomName = [];
        }
        
        // 过滤空值
        this.randomName = this.randomName.filter(name => name && name.trim());
        
        // 使用专门的随机名称更新接口
        let param = {
          id: this.webInfo.id,
          randomName: JSON.stringify(this.randomName)
        }
        
        
        this.$http.post(this.$constant.baseURL + "/webInfo/updateRandomName", param, true)
          .then((res) => {
            if (res.code === 200) {
              this.getWebInfo();
              this.$message({
                message: "随机名称保存成功！",
                type: "success"
              });
            } else {
              this.$message({
                message: "保存失败: " + res.message,
                type: "error"
              });
            }
          })
          .catch((error) => {
            console.error('保存随机名称失败:', error);
            this.$message({
              message: "保存失败: " + (error.response?.data?.message || error.message),
              type: "error"
            });
          });
      },
      handleInputRandomAvatarConfirm() {
        if (!Array.isArray(this.randomAvatar)) {
          this.randomAvatar = [];
        }
        if (this.inputRandomAvatarValue) {
          this.randomAvatar.push(this.inputRandomAvatarValue);
        }
        this.inputRandomAvatarVisible = false;
        this.inputRandomAvatarValue = '';
      },
      showRandomAvatarInput() {
        this.inputRandomAvatarVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomAvatarInput.$refs.input.focus();
        });
      },
      saveRandomAvatar() {
        let param = {
          id: this.webInfo.id,
          randomAvatar: JSON.stringify(this.randomAvatar)
        }
        
        this.$http.post(this.$constant.baseURL + "/webInfo/updateRandomAvatar", param, true)
          .then((res) => {
            if (res.code === 200) {
              this.getWebInfo();
              this.$message({
                message: "随机头像保存成功！",
                type: "success"
              });
            } else {
              this.$message({
                message: "保存失败: " + res.message,
                type: "error"
              });
            }
          })
          .catch((error) => {
            console.error('保存随机头像失败:', error);
            this.$message({
              message: "保存失败: " + (error.response?.data?.message || error.message),
              type: "error"
            });
          });
      },
      handleInputRandomCoverConfirm() {
        if (this.inputRandomCoverValue) {
          this.randomCover.push(this.inputRandomCoverValue);
        }
        this.inputRandomCoverVisible = false;
        this.inputRandomCoverValue = '';
      },
      showRandomCoverInput() {
        this.inputRandomCoverVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomCoverInput.$refs.input.focus();
        });
      },
      saveRandomCover() {
        let param = {
          id: this.webInfo.id,
          randomCover: JSON.stringify(this.randomCover)
        }
        
        this.$http.post(this.$constant.baseURL + "/webInfo/updateRandomCover", param, true)
          .then((res) => {
            if (res.code === 200) {
              this.getWebInfo();
              this.$message({
                message: "随机封面保存成功！",
                type: "success"
              });
            } else {
              this.$message({
                message: "保存失败: " + res.message,
                type: "error"
              });
            }
          })
          .catch((error) => {
            console.error('保存随机封面失败:', error);
            this.$message({
              message: "保存失败: " + (error.response?.data?.message || error.message),
              type: "error"
            });
          });
      },
      updateWebInfo(value) {
        this.$confirm('确认保存？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success',
          center: true
        }).then(() => {
          // 统一更新逻辑：将看板娘状态包含在主更新请求中
          // 这样可以避免并发更新导致的缓存竞态条件
          const updateData = { ...value };


          // 使用单一请求更新所有信息，避免并发问题
          const promises = [
            this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", updateData, true)
          ];

          // 处理所有请求完成
          Promise.all(promises)
            .then(() => {
              this.getWebInfo();
              // 更新mainStore中的webInfo，确保Live2D组件能立即响应变化
              this.mainStore.setWebInfo({...this.webInfo});
              this.$message({
                message: "保存成功！",
                type: "success"
              });

              // 如果请求中包含了enableWaifu字段，说明看板娘状态改变了，提示用户刷新
              if ('enableWaifu' in value) {
                this.$confirm(
                  value.enableWaifu
                    ? '看板娘已启用，需要刷新页面才能生效。现在刷新页面吗？'
                    : '看板娘已禁用，需要刷新页面才能完全生效。现在刷新页面吗？',
                  '刷新提示',
                  {
                    confirmButtonText: '立即刷新',
                    cancelButtonText: '稍后刷新',
                    type: 'info'
                  }
                )
                  .then(() => {
                    window.location.reload();
                  })
                  .catch(() => {
                    this.$notify({
                      title: '提示',
                      message: '请注意，看板娘变更需要刷新页面后才能完全生效。',
                      type: 'warning',
                      duration: 5000
                    });
                  });
              }
            })
            .catch((error) => {
              this.$message({
                message: error.message || '部分保存失败，请检查',
                type: 'error'
              });
            });
        }).catch(() => {
          this.$message({
            type: 'success',
            message: '已取消保存!'
          });
        });
      },
      // 优化：将getEmailConfigs改为异步方法
      async getEmailConfigs() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getEmailConfigs", {}, true);
            this.emailConfigs = res.data || [];
            // 修正属性名
            this.emailConfigs.forEach(config => {
              // 将ssl改为useSsl
              if (config.hasOwnProperty('ssl') && !config.hasOwnProperty('useSsl')) {
                config.useSsl = config.ssl;
                delete config.ssl;
              }
              // 将starttls改为useStarttls
              if (config.hasOwnProperty('starttls') && !config.hasOwnProperty('useStarttls')) {
                config.useStarttls = config.starttls;
                delete config.starttls;
              }
            });
            
            // 获取默认邮箱索引
          await this.getDefaultMailConfigIndex();
        } catch (error) {
            this.$message.error("获取邮箱配置失败: " + error.message);
          throw error;
        }
      },
      // 优化：将getDefaultMailConfigIndex改为异步方法
      async getDefaultMailConfigIndex() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getDefaultMailConfig", {}, true);
            this.defaultMailIndex = res.data || -1;
        } catch (error) {
            this.$message.error("获取默认邮箱索引失败: " + error.message);
          throw error;
        }
      },
      addEmailConfig() {
        // 默认配置
        const defaultConfig = {
          host: "",
          username: "",
          password: "",
          port: 465, // 默认SSL端口
          senderName: "邮件服务",
          useSsl: true,
          useStarttls: false,
          auth: true,
          enabled: true,
          // 高级配置默认值
          connectionTimeout: 25000,
          timeout: 25000,
          protocol: 'smtp',
          authMechanism: 'default',
          debug: false,
          useProxy: false
        };
        
        // 弹出对话框，让用户选择预设配置
        this.$confirm('是否使用邮箱服务预设？', '添加邮箱', {
          confirmButtonText: '使用预设',
          cancelButtonText: '使用默认配置',
          type: 'info',
          customClass: 'mobile-responsive-confirm',
          distinguishCancelAndClose: true  // 区分取消和关闭
        }).then(() => {
          // 用户选择使用预设
          this.showPresetDialog();
        }).catch((action) => {
          // 判断是点击取消按钮还是点击叉叉关闭
          if (action === 'cancel') {
            // 用户点击"使用默认配置"按钮
            this.emailConfigs.push(defaultConfig);
          }
          // 如果 action === 'close'，说明是点击叉叉或ESC，不做任何操作
        });
      },
      
      // 显示预设配置对话框
      showPresetDialog() {
        this.$alert(
          '<div style="text-align:center"><strong>请选择邮箱服务商</strong></div>' +
          '<div style="margin-top:15px; display:flex; flex-wrap:wrap; justify-content:space-around;">' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'163\')">网易163邮箱</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'qq\')">QQ邮箱</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'gmail\')">Gmail</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'outlook\')">Outlook</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'aliyun\')">阿里云邮箱</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'qqex\')">腾讯企业邮箱</button>' +
          '<button class="preset-btn" style="margin:5px; padding:10px; background:#fff; border:1px solid #dcdfe6; border-radius:4px; cursor:pointer;" @click="usePreset(\'163ex\')">网易企业邮箱</button>' +
          '</div>',
          '选择预设配置',
          {
            dangerouslyUseHTMLString: true,
            showConfirmButton: false,
            showCancelButton: true,
            cancelButtonText: '取消',
            closeOnClickModal: false,
            customClass: 'preset-dialog'
          }
        );
        
        // 将button的点击事件处理绑定到Vue实例上
        setTimeout(() => {
          const btns = document.querySelectorAll('.preset-dialog .preset-btn');
          btns.forEach(btn => {
            btn.addEventListener('click', () => {
              // 关闭对话框
              document.querySelector('.el-message-box__close').click();
              // 获取preset参数并调用usePreset方法
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
      
      // 使用预设配置
      usePreset(preset) {
        let config = {
          senderName: "邮件服务",
          username: "",
          password: "",
          enabled: true,
          auth: true,
          // 高级配置默认值
          connectionTimeout: 25000,
          timeout: 25000,
          protocol: 'smtp',
          authMechanism: 'default',
          debug: false,
          useProxy: false
        };
        
        switch(preset) {
          case '163':
            config = {
              ...config,
              host: "smtp.163.com",
              port: 465,
              useSsl: true,
              useStarttls: false
            };
            break;
          case 'qq':
            config = {
              ...config,
              host: "smtp.qq.com",
              port: 465,
              useSsl: true,
              useStarttls: false
            };
            break;
          case 'gmail':
            config = {
              ...config,
              host: "smtp.gmail.com",
              port: 587,
              useSsl: false,
              useStarttls: true
            };
            break;
          case 'outlook':
            config = {
              ...config,
              host: "smtp.office365.com",
              port: 587,
              useSsl: false,
              useStarttls: true
            };
            break;
          case 'aliyun':
            config = {
              ...config,
              host: "smtp.aliyun.com",
              port: 465,
              useSsl: true,
              useStarttls: false
            };
            break;
          case 'qqex':
            config = {
              ...config,
              host: "smtp.exmail.qq.com",
              port: 465,
              useSsl: true,
              useStarttls: false
            };
            break;
          case '163ex':
            config = {
              ...config,
              host: "smtphz.qiye.163.com",
              port: 465,
              useSsl: true,
              useStarttls: false
            };
            break;
        }
        
        this.emailConfigs.push(config);
        this.$message({
          message: `已添加${preset}邮箱预设配置，请填写您的邮箱地址和授权码`,
          type: "success"
        });
      },
      removeEmailConfig(index) {
        this.emailConfigs.splice(index, 1);
      },
      saveEmailConfigs() {
        // 验证配置
        const validConfigs = this.emailConfigs.filter(config => {
          return config.host && config.username && config.password;
        });
        
        if (validConfigs.length === 0) {
          this.$message.error("请至少配置一个完整的邮箱");
            return;
        }
        
        this.loading = true;
        
        // 保存配置
        this.$http.post(this.$constant.baseURL + "/webInfo/saveEmailConfigs?defaultIndex=" + this.defaultMailIndex, 
          validConfigs, true)
          .then((res) => {
            this.$message.success("邮箱配置保存成功");
            this.loading = false;
          })
          .catch((error) => {
            this.$message.error("保存失败: " + error.message);
            this.loading = false;
          });
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
        
        // 构建测试数据
        const testConfig = this.emailConfigs[this.emailTestForm.configIndex];
        const testData = {
          testEmail: this.emailTestForm.testEmail,
          config: { // 将配置信息包装在config对象中
            host: testConfig.host,
            port: testConfig.port,
            username: testConfig.username,
            password: testConfig.password,
            useSsl: testConfig.useSsl,
            useStarttls: testConfig.useStarttls,
            auth: testConfig.auth,
            senderName: testConfig.senderName
          }
        };
        
        // 发送测试请求
        this.$http.post(this.$constant.baseURL + "/webInfo/testEmailConfig", testData, true)
          .then(res => {
            this.$message.success("测试邮件发送成功，请查收");
            this.emailTestDialogVisible = false;
            this.testEmailLoading = false;
          })
          .catch(error => {
            this.$message.error("测试邮件发送失败: " + error.message);
            this.testEmailLoading = false;
          });
      },
      showAdvancedConfig(config, index) {
        this.currentConfigIndex = index;
        // 初始化高级配置
        this.currentAdvancedConfig = {
          connectionTimeout: config.connectionTimeout || 25000,
          timeout: config.timeout || 25000,
          jndiName: config.jndiName || '',
          trustAllCerts: config.trustAllCerts || false
        };
        this.advancedConfigVisible = true;
      },
      saveAdvancedConfig() {
        // 确保索引有效
        if (this.currentConfigIndex >= 0 && this.currentConfigIndex < this.emailConfigs.length) {
          // 更新高级配置
          const config = this.emailConfigs[this.currentConfigIndex];
          
          // 基础配置
          config.connectionTimeout = this.currentAdvancedConfig.connectionTimeout;
          config.timeout = this.currentAdvancedConfig.timeout;
          config.jndiName = this.currentAdvancedConfig.jndiName;
          config.trustAllCerts = this.currentAdvancedConfig.trustAllCerts;
          
          // 协议配置
          config.protocol = this.currentAdvancedConfig.protocol;
          config.authMechanism = this.currentAdvancedConfig.authMechanism;
          config.debug = this.currentAdvancedConfig.debug;
          
          // 代理配置
          config.useProxy = this.currentAdvancedConfig.useProxy;
          if (config.useProxy) {
            config.proxyHost = this.currentAdvancedConfig.proxyHost;
            config.proxyPort = this.currentAdvancedConfig.proxyPort;
            config.proxyUser = this.currentAdvancedConfig.proxyUser;
            config.proxyPassword = this.currentAdvancedConfig.proxyPassword;
          }
          
          // 自定义属性
          config.customProperties = {...this.currentAdvancedConfig.customProperties};
          
          this.$message({
            message: "高级配置已保存",
            type: "success"
          });
        }
        this.advancedConfigVisible = false;
      },
      updateEnableWaifu() {
        // 获取实际状态（通过请求确认当前状态，统一使用Java API）
        this.$http.get(this.$constant.baseURL + "/webInfo/getWaifuStatus", true)
          .then((res) => {
            if (res.code === 200 && res.data) {
              // 使用后端返回的实际状态
              const actualStatus = res.data.enableWaifu;
              this.webInfo.enableWaifu = actualStatus; // 同步前端状态
              
              // 计算目标状态
              const newStatus = !actualStatus;
              const action = newStatus ? "启用" : "禁用";
              
              // 询问用户
              this.$confirm(`确认${action}看板娘功能吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info'
              }).then(() => {
                // 用户确认，更新状态
                this.webInfo.enableWaifu = newStatus;
                
                // 准备要更新的数据
                const param = {
                  id: this.webInfo.id,
                  enableWaifu: newStatus
                };
                
                // 调用Java后端API更新状态
                this.$http.post(this.$constant.baseURL + "/admin/webInfo/updateWaifuStatus", param, true)
                .then((res) => {
                  if (res.code === 200) {
                    // 更新本地存储 - 使用与前端一致的格式
                    const webInfoStr = localStorage.getItem('webInfo');
                    let webInfoData = {};
                    
                    try {
                      // 尝试解析现有数据
                      if (webInfoStr) {
                        webInfoData = JSON.parse(webInfoStr);
                      }
                      
                      // 更新数据
                      if (webInfoData.data) {
                        // 新格式
                        webInfoData.data.enableWaifu = newStatus;
                        webInfoData.timestamp = Date.now();
                      } else {
                        // 旧格式或新存储
                        webInfoData = {
                          timestamp: Date.now(),
                          data: {
                            ...this.webInfo,
                            enableWaifu: newStatus
                          }
                        };
                      }
                      
                      // 保存回localStorage
                      localStorage.setItem('webInfo', JSON.stringify(webInfoData));
                      
                    } catch (e) {
                      console.error('更新本地存储失败:', e);
                      // 使用简单格式保存
                      localStorage.setItem('webInfo', JSON.stringify({
                        timestamp: Date.now(),
                        data: {
                          id: this.webInfo.id,
                          enableWaifu: newStatus
                        }
                      }));
                    }
                    
                    this.$message({
                      message: newStatus ? 
                        "看板娘已启用，是否刷新页面立即生效？" : 
                        "看板娘已禁用，是否刷新页面立即生效？",
                      type: "success",
                      duration: 5000
                    });
                    
                    // 询问用户是否要刷新页面
                    this.$confirm('是否立即刷新页面使更改生效？', '提示', {
                      confirmButtonText: '立即刷新',
                      cancelButtonText: '稍后刷新',
                      type: 'success'
                    }).then(() => {
                      window.location.reload();
                    }).catch(() => {
                      this.$message({
                        type: 'info',
                        message: '您可以稍后手动刷新页面使更改生效'
                      });
                    });
                  } else {
                    // 恢复状态
                    this.webInfo.enableWaifu = actualStatus;
                    this.$message({
                      message: "更新看板娘状态失败: " + res.message,
                      type: "error",
                      duration: 3000
                    });
                  }
                })
                .catch((error) => {
                  // 恢复状态
                  this.webInfo.enableWaifu = actualStatus;
                  this.$message({
                    message: "更新看板娘状态失败: " + error.message,
                    type: "error",
                    duration: 3000
                  });
                });
              }).catch(() => {
                // 用户取消，恢复状态
                this.webInfo.enableWaifu = actualStatus;
                this.$message({
                  type: 'info',
                  message: '已取消操作'
                });
              });
            } else {
              this.$message({
                message: "获取看板娘状态失败",
                type: "error",
                duration: 3000
              });
            }
          })
          .catch((error) => {
            this.$message({
              message: "获取看板娘状态失败: " + error.message,
              type: "error",
              duration: 3000
            });
          });
      },
      // 优化：将getThirdLoginConfig改为异步方法
      async getThirdLoginConfig() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getThirdLoginConfig");
          
          // 确保返回完整的配置结构
          const defaultConfig = {
              enable: false,
            github: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/github',
              enabled: false
            },
            google: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/google',
              enabled: false
            },
            twitter: {
              client_key: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/x',
              enabled: false
            },
            yandex: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/yandex',
              enabled: false
            },
            gitee: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/gitee',
              enabled: false
            },
            qq: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/qq',
              enabled: false
            },
            baidu: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/baidu',
              enabled: false
            }
          };
          
          if (res.data) {
            // 合并返回的数据和默认配置，确保所有必需的属性都存在
            this.thirdLoginConfig = {
              enable: res.data.enable !== undefined ? res.data.enable : defaultConfig.enable,
              github: {
                ...defaultConfig.github,
                ...(res.data.github || {})
              },
              google: {
                ...defaultConfig.google,
                ...(res.data.google || {})
              },
              twitter: {
                ...defaultConfig.twitter,
                ...(res.data.twitter || {})
              },
              yandex: {
                ...defaultConfig.yandex,
                ...(res.data.yandex || {})
              },
              gitee: {
                ...defaultConfig.gitee,
                ...(res.data.gitee || {})
              },
              qq: {
                ...defaultConfig.qq,
                ...(res.data.qq || {})
              },
              baidu: {
                ...defaultConfig.baidu,
                ...(res.data.baidu || {})
              }
            };
          } else {
            this.thirdLoginConfig = defaultConfig;
          }
        } catch (error) {
          console.error("获取第三方登录配置失败:", error);
          // 设置完整的默认配置
          this.thirdLoginConfig = {
            enable: false,
            github: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/github',
              enabled: false
            },
            google: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/google',
              enabled: false
            },
            twitter: {
              client_key: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/x',
              enabled: false
            },
            yandex: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/yandex',
              enabled: false
            },
            gitee: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/gitee',
              enabled: false
            },
            qq: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/qq',
              enabled: false
            },
            baidu: {
              client_id: '',
              client_secret: '',
              redirect_uri: this.$constant.pythonBaseURL + '/callback/baidu',
              enabled: false
            }
          };
            this.$message.error("获取第三方登录配置失败: " + error.message);
          throw error;
        }
      },
      
      // 优化：将getApiConfig改为异步方法
      async getApiConfig() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getApiConfig", true);
            if (res.data) {
              this.apiConfig = res.data;
            } else {
              this.apiConfig = {
                enabled: false,
                apiKey: ''
              };
            }
        } catch (error) {
            this.$message.error("获取API配置失败: " + error.message);
          throw error;
        }
      },
      saveThirdLoginConfig() {
        // 检查配置是否有效
        let hasInvalidConfig = false;
        
        // 检查GitHub配置
        if (this.thirdLoginConfig.github.enabled) {
          if (!this.thirdLoginConfig.github.client_id) {
            this.$message.error("GitHub的Client ID不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.github.client_secret) {
            this.$message.error("GitHub的Client Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.github.redirect_uri) {
            this.$message.error("GitHub的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        // 检查Google配置
        if (this.thirdLoginConfig.google.enabled) {
          if (!this.thirdLoginConfig.google.client_id) {
            this.$message.error("Google的Client ID不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.google.client_secret) {
            this.$message.error("Google的Client Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.google.redirect_uri) {
            this.$message.error("Google的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        // 检查Twitter配置
        if (this.thirdLoginConfig.twitter.enabled) {
          if (!this.thirdLoginConfig.twitter.client_key) {
            this.$message.error("Twitter的API Key不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.twitter.client_secret) {
            this.$message.error("Twitter的API Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.twitter.redirect_uri) {
            this.$message.error("Twitter的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        // 检查Yandex配置
        if (this.thirdLoginConfig.yandex.enabled) {
          if (!this.thirdLoginConfig.yandex.client_id) {
            this.$message.error("Yandex的Client ID不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.yandex.client_secret) {
            this.$message.error("Yandex的Client Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.yandex.redirect_uri) {
            this.$message.error("Yandex的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        // 检查Gitee配置
        if (this.thirdLoginConfig.gitee.enabled) {
          if (!this.thirdLoginConfig.gitee.client_id) {
            this.$message.error("Gitee的Client ID不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.gitee.client_secret) {
            this.$message.error("Gitee的Client Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.gitee.redirect_uri) {
            this.$message.error("Gitee的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        // 检查Baidu配置
        if (this.thirdLoginConfig.baidu.enabled) {
          if (!this.thirdLoginConfig.baidu.client_id) {
            this.$message.error("Baidu的Client ID不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.baidu.client_secret) {
            this.$message.error("Baidu的Client Secret不能为空");
            hasInvalidConfig = true;
          }
          if (!this.thirdLoginConfig.baidu.redirect_uri) {
            this.$message.error("Baidu的回调地址不能为空");
            hasInvalidConfig = true;
          }
        }
        
        if (hasInvalidConfig) {
          return;
        }
        
        this.loading = true;
        this.$http.post(this.$constant.baseURL + "/webInfo/updateThirdLoginConfig", this.thirdLoginConfig)
          .then((res) => {
            this.$message({
              message: "第三方登录配置保存成功",
              type: "success"
            });
            this.loading = false;

            // 触发第三方登录配置变更事件
            this.$bus.$emit('thirdPartyLoginConfigChanged');
          })
          .catch((error) => {
            this.$message({
              message: error.message || "保存失败",
              type: "error"
            });
            this.loading = false;
          });
      },
      updateThirdLoginConfig() {
        this.loading = true;
        this.$http.post(this.$constant.baseURL + "/webInfo/updateThirdLoginConfig", this.thirdLoginConfig)
          .then((res) => {
            this.$message({
              message: "第三方登录功能" + (this.thirdLoginConfig.enable ? "已启用" : "已禁用"),
              type: "success"
            });
            this.loading = false;

            // 触发第三方登录配置变更事件
            this.$bus.$emit('thirdPartyLoginConfigChanged');
          })
          .catch((error) => {
            this.$message({
              message: error.message || "操作失败",
              type: "error"
            });
            // 出错时恢复原状态
            this.thirdLoginConfig.enable = !this.thirdLoginConfig.enable;
            this.loading = false;
          });
      },
      testThirdLogin(platform) {
        if (!platform.config.client_id && (platform.type !== 'twitter' || !platform.config.client_key)) {
          this.$message({
            message: "请先填写Client ID/API Key",
            type: "warning"
          });
          return;
        }

        if (!platform.config.client_secret) {
          this.$message({
            message: "请先填写Client Secret",
            type: "warning"
          });
          return;
        }

        this.$message({
          message: `正在测试${platform.platform}登录...`,
          type: "info"
        });

        // 打开登录窗口进行测试
        window.open(`/login/${platform.type}`, '_blank');
      },
      getPlatformIcon(type) {
        const iconMapping = {
          github: './static/svg/github.svg',
          google: './static/svg/google.svg',
          twitter: './static/svg/x.svg',
          yandex: './static/svg/yandex.svg',
          gitee: './static/svg/gitee.svg',
          qq: './static/svg/qq.svg',
          baidu: './static/svg/baidu.svg'
        };
        return iconMapping[type] || '';
      },
      // 测试第三方登录
      testLogin(type) {
        // 检查配置是否完整
        const platformConfig = this.thirdLoginConfig[type];
        if (!platformConfig) {
          this.$message.error("无法找到该平台配置");
          return;
        }
        
        // 检查是否启用
        if (!this.thirdLoginConfig.enable || !platformConfig.enabled) {
          this.$message.error("该平台登录功能未启用");
          return;
        }
        
        // 检查配置是否完整
        if (type === 'twitter' && (!platformConfig.client_key || !platformConfig.client_secret)) {
          this.$message.error("请先填写完整的API Key和Secret");
          return;
        } else if (type !== 'twitter' && (!platformConfig.client_id || !platformConfig.client_secret)) {
          this.$message.error("请先填写完整的Client ID和Secret");
          return;
        }
        
        // 打开登录测试窗口
        const loginUrl = `${this.$constant.pythonBaseURL}/login/${type === 'twitter' ? 'x' : type}`;
        window.open(loginUrl, '_blank', 'width=800,height=600');
      },
      // 检测设备类型
      checkDeviceType() {
        this.isMobileDevice = window.innerWidth <= 768 || this.isMobile();
      },
      // 检测是否为移动端视图（AI配置专用）
      checkMobileView() {
        this.isMobileView = window.innerWidth <= 768;
      },
      // 打开移动端配置对话框
      openMobileConfigDialog(type) {
        const titles = {
          model: 'AI模型配置',
          chat: '聊天设置',
          appearance: '外观设置',
          advanced: '高级设置'
        };
        this.currentMobileConfig = type;
        this.mobileConfigDialogTitle = titles[type];
        this.mobileConfigDialogVisible = true;
      },
      // 保存移动端配置
      saveMobileConfig() {
        this.mobileConfigDialogVisible = false;
        this.$message.success('配置已更新，点击"保存AI聊天配置"按钮完成保存');
      },
      // 处理默认邮箱变更
      handleDefaultChange(index) {
        this.defaultMailIndex = index;
      },
      // 删除邮箱配置
      deleteEmailConfig(index) {
        this.$confirm('确定要删除这个邮箱配置吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          customClass: 'mobile-responsive-confirm'
        }).then(() => {
          this.emailConfigs.splice(index, 1);
          if (this.defaultMailIndex === index) {
            this.defaultMailIndex = -1;
          } else if (this.defaultMailIndex > index) {
            this.defaultMailIndex--;
          }
          this.$message({
            type: 'success',
            message: '删除成功!'
          });
        }).catch(() => {});
      },
      addNewEmailConfig() {
        // 创建新的邮箱配置对象
        const newEmailConfig = {
          host: '',
          username: '',
          password: '',
          port: '465',
          ssl: true,
          starttls: false,
          auth: true,
          enabled: true,
          // 添加高级配置
          connectionTimeout: 25000,
          timeout: 25000,
          jndiName: '',
          trustAllCerts: false,
          protocol: 'smtp',
          authMechanism: 'default',
          debug: false,
          useProxy: false,
          proxyHost: '',
          proxyPort: 8080,
          proxyUser: '',
          proxyPassword: '',
          customProperties: {}
        };
        
        // 添加到配置列表
        this.emailConfigs.push(newEmailConfig);
      },
      
      // 显示高级配置对话框
      showAdvancedConfig(index) {
        this.currentConfigIndex = index;
        // 获取当前配置的高级设置
        const config = this.emailConfigs[index];
        this.currentAdvancedConfig = {
          connectionTimeout: config.connectionTimeout || 25000,
          timeout: config.timeout || 25000,
          jndiName: config.jndiName || '',
          trustAllCerts: config.trustAllCerts || false,
          protocol: config.protocol || 'smtp',
          authMechanism: config.authMechanism || 'default',
          debug: config.debug || false,
          useProxy: config.useProxy || false,
          proxyHost: config.proxyHost || '',
          proxyPort: config.proxyPort || 8080,
          proxyUser: config.proxyUser || '',
          proxyPassword: config.proxyPassword || '',
          customProperties: config.customProperties ? {...config.customProperties} : {}
        };
        
        // 设置自定义属性键名数组
        this.customPropertyKeys = Object.keys(this.currentAdvancedConfig.customProperties || {});
        
        this.activeConfigTab = 'basic';
        this.advancedConfigVisible = true;
      },
      
      // 添加自定义属性
      addCustomProperty() {
        const newKey = `mail.property.${this.customPropertyKeys.length + 1}`;
        this.$set(this.currentAdvancedConfig.customProperties, newKey, '');
        this.customPropertyKeys.push(newKey);
      },
      
      // 更新自定义属性的键
      updateCustomPropertyKey(index) {
        const oldKey = Object.keys(this.currentAdvancedConfig.customProperties)[index];
        const newKey = this.customPropertyKeys[index];
        
        if (oldKey !== newKey && newKey) {
          const value = this.currentAdvancedConfig.customProperties[oldKey];
          this.$delete(this.currentAdvancedConfig.customProperties, oldKey);
          this.$set(this.currentAdvancedConfig.customProperties, newKey, value);
        }
      },
      
      // 删除自定义属性
      removeCustomProperty(index) {
        const key = this.customPropertyKeys[index];
        this.$delete(this.currentAdvancedConfig.customProperties, key);
        this.customPropertyKeys.splice(index, 1);
      },
      
      // 保存高级配置
      saveAdvancedConfig() {
        // 确保索引有效
        if (this.currentConfigIndex >= 0 && this.currentConfigIndex < this.emailConfigs.length) {
          // 更新当前配置的高级设置
          const config = this.emailConfigs[this.currentConfigIndex];
          
          // 基础配置
          config.connectionTimeout = this.currentAdvancedConfig.connectionTimeout;
          config.timeout = this.currentAdvancedConfig.timeout;
          config.jndiName = this.currentAdvancedConfig.jndiName;
          config.trustAllCerts = this.currentAdvancedConfig.trustAllCerts;
          
          // 协议配置
          config.protocol = this.currentAdvancedConfig.protocol;
          config.authMechanism = this.currentAdvancedConfig.authMechanism;
          config.debug = this.currentAdvancedConfig.debug;
          
          // 代理配置
          config.useProxy = this.currentAdvancedConfig.useProxy;
          if (config.useProxy) {
            config.proxyHost = this.currentAdvancedConfig.proxyHost;
            config.proxyPort = this.currentAdvancedConfig.proxyPort;
            config.proxyUser = this.currentAdvancedConfig.proxyUser;
            config.proxyPassword = this.currentAdvancedConfig.proxyPassword;
          }
          
          // 自定义属性
          config.customProperties = {...this.currentAdvancedConfig.customProperties};
          
          this.$message({
            message: "高级配置已保存",
            type: "success"
          });
        }
        this.advancedConfigVisible = false;
      },
      // 处理端口变化，自动调整SSL/STARTTLS设置
      onPortChange(port, config, index) {
        if (port === 465) {
          // 端口465必须使用SSL
          if (!config.useSsl) {
            config.useSsl = true;
            this.$message({
              message: "端口465必须使用SSL加密，已自动启用SSL",
              type: "info"
            });
          }
          // 通常465端口不使用STARTTLS
          if (config.useStarttls) {
            config.useStarttls = false;
          }
        } else if (port === 587) {
          // 端口587通常使用STARTTLS
          if (!config.useStarttls) {
            config.useStarttls = true;
            this.$message({
              message: "端口587通常使用STARTTLS加密，已自动启用STARTTLS",
              type: "info"
            });
          }
          // 587端口通常不使用SSL
          if (config.useSsl) {
            config.useSsl = false;
          }
        } else if (port === 25) {
          // 端口25通常不加密，显示警告
          this.$message({
            message: "警告：端口25通常不加密，且可能被ISP阻止。建议使用465(SSL)或587(STARTTLS)端口",
            type: "warning"
          });
        }
      },
      // 处理SSL开关变化
      onSslChange(value, config, index) {
        if (!value && config.port === 465) {
          // 如果用户尝试在端口465上禁用SSL，自动重新启用并提示
          this.$nextTick(() => {
            config.useSsl = true;
            this.$message({
              message: "端口465必须使用SSL加密！",
              type: "warning"
            });
          });
        }
      },
      
      // 处理STARTTLS开关变化
      onStarttlsChange(value, config, index) {
        if (!value && config.port === 587) {
          // 如果用户尝试在端口587上禁用STARTTLS，给出警告提示
          this.$message({
            message: "警告：端口587通常需要启用STARTTLS，禁用可能导致邮件发送失败",
            type: "warning"
          });
        }
      },
      
      // 优化：将getCaptchaConfig改为异步方法
      async getCaptchaConfig() {
        try {
          const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getCaptchaConfig", {}, true);
            if (res.data) {
              this.captchaConfig = res.data;
            }
        } catch (error) {
            this.$message.error("获取智能验证码配置失败: " + error.message);
          throw error;
        }
      },
      
      // 更新智能验证码全局状态
      updateCaptchaStatus(value) {
        // 先设置本地状态，确保UI立即响应
        this.captchaConfig.enable = value;
        
        if (!value) {
          // 如果禁用了智能验证码，显示确认对话框
          this.$confirm('禁用智能验证码将降低网站安全性，可能导致机器人攻击，确定要禁用吗?', '安全提示', {
            confirmButtonText: '确定禁用',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            // 用户确认禁用，保持禁用状态并保存
            this.saveCaptchaConfig();
          }).catch(() => {
            // 用户取消，恢复开关状态为启用
            this.captchaConfig.enable = true;
          });
        } else {
          // 启用验证码，直接保存
          this.saveCaptchaConfig();
        }
      },
      
      // 保存智能验证码配置
      saveCaptchaConfig() {
        this.captchaLoading = true;
        this.$http.post(this.$constant.baseURL + "/webInfo/updateCaptchaConfig", this.captchaConfig, true)
          .then((res) => {
            this.$message.success("智能验证码配置保存成功");
            this.captchaLoading = false;
          })
          .catch((error) => {
            this.$message.error("保存智能验证码配置失败: " + error.message);
            this.captchaLoading = false;
          });
      },
      regenerateApiKey() {
        this.apiLoading = true;
        this.$http.post(this.$constant.baseURL + "/webInfo/regenerateApiKey", {}, true)
          .then((res) => {
            this.apiConfig.apiKey = res.data;
            this.$message({
              message: "API密钥已重新生成",
              type: "success"
            });
            this.apiLoading = false;
          })
          .catch((error) => {
            this.$message({
              message: "重新生成API密钥失败: " + error.message,
              type: "error"
            });
            this.apiLoading = false;
          });
      },
      saveApiConfig() {
        this.apiLoading = true;
        this.$http.post(this.$constant.baseURL + "/webInfo/saveApiConfig", this.apiConfig, true)
          .then((res) => {
            this.$message({
              message: "API配置保存成功",
              type: "success"
            });
            this.apiLoading = false;
          })
          .catch((error) => {
            this.$message({
              message: "保存API配置失败: " + error.message,
              type: "error"
            });
            this.apiLoading = false;
          });
      },
      isMobile() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
      },
      testCreateArticle() {
        if (!this.apiTestForm.title || !this.apiTestForm.content) {
          this.$message.warning('文章标题和内容不能为空');
          return;
        }
        
        this.apiTestLoading = true;
        
        const data = {
          title: this.apiTestForm.title,
          content: this.apiTestForm.content,
          sortName: this.apiTestForm.sortName,
          labelName: this.apiTestForm.labelName,
          cover: this.apiTestForm.cover || undefined,
          summary: this.apiTestForm.summary || undefined,
          viewStatus: true,
          commentStatus: true,
          submitToSearchEngine: false
        };
        
        this.$http.post(this.$constant.baseURL + "/api/article/create", data, {
          headers: {
            'X-API-KEY': this.apiConfig.apiKey
          }
        })
          .then(res => {
            this.apiTestResult = JSON.stringify(res, null, 2);
            this.apiTestLoading = false;
          })
          .catch(error => {
            this.apiTestResult = JSON.stringify(error.response ? error.response.data : error.message, null, 2);
            this.apiTestLoading = false;
          });
      },
      
      testQueryArticles() {
        this.apiTestLoading = true;
        
        const params = {
          current: this.apiQueryForm.current,
          size: this.apiQueryForm.size
        };
        
        if (this.apiQueryForm.searchKey) {
          params.searchKey = this.apiQueryForm.searchKey;
        }
        
        this.$http.get(this.$constant.baseURL + "/api/article/list", {
          params: params,
          headers: {
            'X-API-KEY': this.apiConfig.apiKey
          }
        })
          .then(res => {
            this.apiTestResult = JSON.stringify(res, null, 2);
            this.apiTestLoading = false;
          })
          .catch(error => {
            this.apiTestResult = JSON.stringify(error.response ? error.response.data : error.message, null, 2);
            this.apiTestLoading = false;
          });
      },
      
      testGetArticleDetail() {
        if (!this.apiDetailForm.id) {
          this.$message.warning('文章ID不能为空');
          return;
        }
        
        this.apiTestLoading = true;
        
        this.$http.get(this.$constant.baseURL + "/api/article/" + this.apiDetailForm.id, {
          headers: {
            'X-API-KEY': this.apiConfig.apiKey
          }
        })
          .then(res => {
            this.apiTestResult = JSON.stringify(res, null, 2);
            this.apiTestLoading = false;
          })
          .catch(error => {
            this.apiTestResult = JSON.stringify(error.response ? error.response.data : error.message, null, 2);
            this.apiTestLoading = false;
          });
      },
      
      testGetCategories() {
        this.apiTestLoading = true;
        
        this.$http.get(this.$constant.baseURL + "/api/categories", {
          headers: {
            'X-API-KEY': this.apiConfig.apiKey
          }
        })
          .then(res => {
            this.apiTestResult = JSON.stringify(res, null, 2);
            this.apiTestLoading = false;
          })
          .catch(error => {
            this.apiTestResult = JSON.stringify(error.response ? error.response.data : error.message, null, 2);
            this.apiTestLoading = false;
          });
      },
      
      testGetTags() {
        this.apiTestLoading = true;
        
        this.$http.get(this.$constant.baseURL + "/api/tags", {
          headers: {
            'X-API-KEY': this.apiConfig.apiKey
          }
        })
          .then(res => {
            this.apiTestResult = JSON.stringify(res, null, 2);
            this.apiTestLoading = false;
          })
          .catch(error => {
            this.apiTestResult = JSON.stringify(error.response ? error.response.data : error.message, null, 2);
            this.apiTestLoading = false;
          });
      },
      resetApiConfig() {
        this.getApiConfig();
      },
      resetToDefaultNav() {
        this.navItems = this.defaultNavItems.map((item, index) => ({
          ...item,
          order: index + 1,
          enabled: true
        }));
      },
      saveNavConfig() {
        this.navLoading = true;

        // 构建导航项数组，添加order字段
        const navItems = this.navItems.map((item, index) => ({
          ...item,
          order: index + 1
        }));

        // 更新WebInfo对象中的navConfig
        const param = {
          id: this.webInfo.id,
          navConfig: JSON.stringify(navItems)
        };

        this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", param, true)
          .then((res) => {
            this.$message({
              message: "导航栏配置保存成功",
              type: "success"
            });
            this.navLoading = false;
            // 更新本地webInfo和store中的配置
            this.webInfo.navConfig = JSON.stringify(navItems);
            this.mainStore.webInfo.navConfig = JSON.stringify(navItems);
            
            // 重新获取完整的webInfo以确保所有组件都能获取到最新配置
            this.getWebInfo().then(() => {
              // 确保store中的数据也是最新的
              this.mainStore.loadWebInfo(this.webInfo);
            });
          })
          .catch((error) => {
            this.$message({
              message: "保存导航栏配置失败: " + error.message,
              type: "error"
            });
            this.navLoading = false;
          });
      },
      // 添加导航项
      addNavItem() {
        this.navItems.push({
          name: "新导航",
          icon: "🔗",
          link: "/",
          type: "internal",
          enabled: true
        });
      },
      // 删除导航项
      deleteNavItem(index) {
        this.$confirm('确定要删除这个导航项吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.navItems.splice(index, 1);
          this.$message({
            message: '删除成功',
            type: 'success'
          });
        }).catch(() => {});
      },
      // 移动导航项
      moveNavItem(index, direction) {
        if (direction === 'up' && index > 0) {
          // 上移
          const temp = this.navItems[index];
          this.$set(this.navItems, index, this.navItems[index - 1]);
          this.$set(this.navItems, index - 1, temp);
        } else if (direction === 'down' && index < this.navItems.length - 1) {
          // 下移
          const temp = this.navItems[index];
          this.$set(this.navItems, index, this.navItems[index + 1]);
          this.$set(this.navItems, index + 1, temp);
        }
      },
      // 处理导航项行点击事件（移动端）
      handleNavRowClick(row, column, event) {
        // 如果是移动设备且不是操作列，则打开详情对话框
        if (this.isMobileDevice && column.property !== 'operation') {
          const index = this.navItems.indexOf(row);
          this.currentNavItem = row; // 直接引用，不拷贝
          this.currentNavItemIndex = index;
          this.navDetailDialogVisible = true;
        }
      },
      // 打开开发者中心页面
      openDeveloperCenter(url) {
        if (url) {
          window.open(url, '_blank');
        } else {
          this.$message.warning('开发者中心链接未配置');
        }
      },
      handleWaifuChange(value) {
        // 只更新本地状态，不立即更新mainStore，避免在保存前就显示效果
        this.webInfo.enableWaifu = value;
      },
      handleWaifuDisplayModeChange(value) {
        // 只更新本地状态，不立即更新mainStore，避免在保存前就显示效果
        this.webInfo.waifuDisplayMode = value;
      },
      handleMouseClickEffectChange(value) {
        // 只更新本地状态，不立即更新mainStore，避免在保存前就显示效果
        this.webInfo.mouseClickEffect = value;
        
        // 同步更新插件系统的激活状态
        this.$http.post(this.$constant.baseURL + "/sysPlugin/setActivePlugin", {
          pluginType: 'mouse_click_effect',
          pluginKey: value
        }).catch(error => {
          console.error('同步插件激活状态失败:', error);
        });
      },
      async loadMouseClickEffectPlugins() {
        this.mouseClickEffectLoading = true;
        try {
          // 1. 加载插件列表
          const res = await this.$http.get(this.$constant.baseURL + "/sysPlugin/getMouseClickEffects");
          if (res.code === 200 && res.data) {
            this.mouseClickEffectOptions = res.data.map(plugin => ({
              pluginKey: plugin.pluginKey,
              pluginName: plugin.pluginName,
              pluginDescription: plugin.pluginDescription
            }));
          }
          
          // 2. 获取当前激活的插件，并同步到下拉框
          const activeRes = await this.$http.get(this.$constant.baseURL + "/sysPlugin/getActiveMouseClickEffect");
          if (activeRes.code === 200 && activeRes.data && activeRes.data.pluginKey) {
            this.webInfo.mouseClickEffect = activeRes.data.pluginKey;
          }
        } catch (error) {
          console.error('加载鼠标点击效果插件失败:', error);
          // 加载失败时使用默认选项
          this.mouseClickEffectOptions = [
            { pluginKey: 'none', pluginName: '无效果', pluginDescription: '' },
            { pluginKey: 'love', pluginName: '爱心', pluginDescription: '点击显示爱心' },
            { pluginKey: 'star', pluginName: '星星', pluginDescription: '点击显示星星' },
            { pluginKey: 'text', pluginName: '文字', pluginDescription: '点击显示文字' }
          ];
        } finally {
          this.mouseClickEffectLoading = false;
        }
      },
      handleThirdLoginToggle(value) {
        this.thirdLoginConfig.enable = value;
      },
      handlePlatformToggle(platform, value) {
        if (this.thirdLoginConfig[platform]) {
          this.thirdLoginConfig[platform].enabled = value;
        }
      },
      handleCaptchaToggle(value) {
        // 先设置本地状态，确保UI立即响应
        this.captchaConfig.enable = value;
        
        if (!value) {
          // 如果禁用了智能验证码，显示确认对话框
          this.$confirm('禁用智能验证码将降低网站安全性，可能导致机器人攻击，确定要禁用吗?', '安全提示', {
            confirmButtonText: '确定禁用',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            // 用户确认禁用，保持禁用状态并保存
            this.saveCaptchaConfig();
          }).catch(() => {
            // 用户取消，恢复开关状态为启用
            this.captchaConfig.enable = true;
          });
        } else {
          // 启用验证码，直接保存
          this.saveCaptchaConfig();
        }
      },
      handleApiToggle(value) {
        this.apiConfig.enabled = value;
      },
      addFooterBackgroundImage(res) {
        this.webInfo.footerBackgroundImage = res;
      },

      // 移动端侧边栏配置相关方法
      addDrawerBackgroundImage(res) {
        this.drawerConfig.backgroundImage = res;
      },
      
      formatOpacity(val) {
        return `${(val * 100).toFixed(0)}%`;
      },
      
      updateBorderColor(color) {
        if (color) {
          this.drawerConfig.borderColor = color;
        }
      },
      
      getDrawerPreviewStyle() {
        let background = '';
        if (this.drawerConfig.backgroundType === 'image' && this.drawerConfig.backgroundImage) {
          background = `url(${this.drawerConfig.backgroundImage}) center center / cover no-repeat`;
        } else if (this.drawerConfig.backgroundType === 'color') {
          background = this.drawerConfig.backgroundColor;
        } else if (this.drawerConfig.backgroundType === 'gradient') {
          background = this.drawerConfig.backgroundGradient;
        }
        
        return {
          background: background,
          position: 'relative',
          '--drawer-mask-opacity': this.drawerConfig.maskOpacity
        };
      },
      
      getMenuItemStyle() {
        return {
          borderBottom: this.drawerConfig.showBorder ? `1px solid ${this.drawerConfig.borderColor}` : 'none'
        };
      },
      
      resetDrawerConfig() {
        this.drawerConfig = {
          titleType: 'text',
          titleText: '欢迎光临',
          avatarSize: 100,
          backgroundType: 'image',
          backgroundImage: '/assets/toolbar.jpg',
          backgroundColor: '#000000',
          backgroundGradient: 'linear-gradient(60deg, #ffd7e4, #c8f1ff 95%)',
          maskOpacity: 0.7,
          menuFontColor: '#ffffff',
          showBorder: true,
          borderColor: 'rgba(255, 255, 255, 0.15)',
          showSnowflake: true
        };
        this.$message.success('已重置为默认配置');
      },
      
      saveDrawerConfig() {
        // 将配置转换为JSON字符串
        const configJson = JSON.stringify(this.drawerConfig);
        
        // 更新webInfo
        this.webInfo.mobileDrawerConfig = configJson;
        
        // 保存到后端
        this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", {
          id: this.webInfo.id,
          mobileDrawerConfig: configJson
        }, true)
          .then(() => {
            this.$message.success('移动端侧边栏配置保存成功！');
            this.mobileDrawerDialogVisible = false;
            this.getWebInfo();
            // 同步更新Vuex store，让其他组件能立即获取最新配置
            this.mainStore.getWebsitConfig();
          })
          .catch((error) => {
            this.$message.error('保存失败: ' + (error.response?.data?.message || error.message));
          });
      },

      // AI聊天配置相关方法
      async loadAiConfigs() {
        try {
          const response = await this.$http.get(this.$constant.baseURL + "/webInfo/ai/config/chat/get", {}, true);
          if (response.code === 200 && response.data) {
            const config = response.data;
            
            // 映射AI模型配置（Java驼峰格式）
            this.aiConfigs.modelConfig = {
              provider: config.provider || 'openai',
              apiKey: config.apiKey || '',
              model: config.model || 'gpt-3.5-turbo',
              baseUrl: config.apiBase || '',
              temperature: config.temperature || 0.7,
              maxTokens: config.maxTokens || 1000,
              topP: config.topP || 1.0,
              frequencyPenalty: config.frequencyPenalty || 0,
              presencePenalty: config.presencePenalty || 0,
              enabled: config.enabled || false,
              enableStreaming: config.enableStreaming || false
            };
            
            // 映射聊天配置（Java驼峰格式）
            this.aiConfigs.chatConfig = {
              systemPrompt: config.customInstructions || "AI assistant. Respond in Chinese naturally.",
              welcomeMessage: config.welcomeMessage || "你好！有什么可以帮助你的吗？",
              historyCount: config.maxConversationLength || 10,
              rateLimit: config.rateLimit || 20,
              requireLogin: config.requireLogin || false,
              saveHistory: config.enableChatHistory !== false,
              contentFilter: config.enableContentFilter !== false,
              maxMessageLength: config.maxMessageLength || 500
            };
            
            // 映射外观配置（Java驼峰格式）
            this.aiConfigs.appearanceConfig = {
              botAvatar: config.chatAvatar || '',
              botName: config.chatName || 'AI助手',
              themeColor: config.themeColor || '#409EFF',
              position: 'bottom-right',
              bubbleStyle: 'modern',
              typingAnimation: config.enableTypingIndicator !== false,
              showTimestamp: true
            };
            
            // 映射高级配置（Java驼峰格式）
            this.aiConfigs.advancedConfig = {
              proxy: '',
              timeout: 30,
              retryCount: 3,
              customHeaders: [],
              debugMode: false,
              enableThinking: config.enableThinking || false
            };
          }
        } catch (error) {
          console.error('加载AI配置失败:', error);
          // 不显示错误消息，允许使用默认配置
        }
      },
      
      async saveAiConfigs() {
        this.savingAiConfigs = true;
        try {
          // 构建保存请求数据（Java驼峰格式）
          const saveData = {
            configType: 'ai_chat',
            configName: 'default',
            provider: this.aiConfigs.modelConfig.provider,
            apiBase: this.aiConfigs.modelConfig.baseUrl,
            model: this.aiConfigs.modelConfig.model,
            temperature: this.aiConfigs.modelConfig.temperature,
            maxTokens: this.aiConfigs.modelConfig.maxTokens,
            topP: this.aiConfigs.modelConfig.topP || 1.0,
            frequencyPenalty: this.aiConfigs.modelConfig.frequencyPenalty || 0,
            presencePenalty: this.aiConfigs.modelConfig.presencePenalty || 0,
            enabled: this.aiConfigs.modelConfig.enabled,
            enableStreaming: this.aiConfigs.modelConfig.enableStreaming,
            // 聊天配置
            customInstructions: this.aiConfigs.chatConfig.systemPrompt,
            welcomeMessage: this.aiConfigs.chatConfig.welcomeMessage,
            maxConversationLength: this.aiConfigs.chatConfig.historyCount,
            rateLimit: this.aiConfigs.chatConfig.rateLimit,
            requireLogin: this.aiConfigs.chatConfig.requireLogin,
            enableChatHistory: this.aiConfigs.chatConfig.saveHistory,
            enableContentFilter: this.aiConfigs.chatConfig.contentFilter,
            maxMessageLength: this.aiConfigs.chatConfig.maxMessageLength,
            // 外观配置
            chatAvatar: this.aiConfigs.appearanceConfig.botAvatar,
            chatName: this.aiConfigs.appearanceConfig.botName,
            themeColor: this.aiConfigs.appearanceConfig.themeColor,
            enableTypingIndicator: this.aiConfigs.appearanceConfig.typingAnimation,
            // 高级配置
            enableThinking: this.aiConfigs.advancedConfig.enableThinking
          };

          // 只有当API密钥不是隐藏格式时才发送
          if (this.aiConfigs.modelConfig.apiKey && !this.aiConfigs.modelConfig.apiKey.includes('*')) {
            saveData.apiKey = this.aiConfigs.modelConfig.apiKey;
          }

          const response = await this.$http.post(this.$constant.baseURL + '/webInfo/ai/config/chat/save', saveData, true);
          
          if (response.code === 200) {
            this.$message.success('AI聊天配置保存成功');
            // 重新加载配置，获取最新的隐藏密钥格式
            await this.loadAiConfigs();
          } else {
            this.$message.error(response.message || 'AI聊天配置保存失败');
          }
        } catch (error) {
          console.error('保存AI配置失败:', error);
          this.$message.error('保存失败，请检查网络连接');
        } finally {
          this.savingAiConfigs = false;
        }
      },
      
      exportAiConfig() {
        const config = {
          model: this.aiConfigs.modelConfig,
          chat: this.aiConfigs.chatConfig,
          appearance: this.aiConfigs.appearanceConfig,
          advanced: this.aiConfigs.advancedConfig
        };
        
        const blob = new Blob([JSON.stringify(config, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'ai-chat-config.json';
        link.click();
        URL.revokeObjectURL(url);
      },
      
      importAiConfig(config) {
        try {
          if (config.model) Object.assign(this.aiConfigs.modelConfig, config.model);
          if (config.chat) Object.assign(this.aiConfigs.chatConfig, config.chat);
          if (config.appearance) Object.assign(this.aiConfigs.appearanceConfig, config.appearance);
          if (config.advanced) Object.assign(this.aiConfigs.advancedConfig, config.advanced);
          this.$message.success('配置导入成功');
        } catch (error) {
          this.$message.error('配置导入失败：' + error.message);
        }
      },

      // 触摸开始事件
      handleTouchStart(event) {
        if (!this.isMobileDevice) {
          return;
        }
        
        const touch = event.touches[0];
        this.touchStartX = touch.clientX;
        this.touchStartY = touch.clientY;
        this.touchStartTime = Date.now();
      },

      // 触摸结束事件
      handleTouchEnd(event) {
        if (!this.isMobileDevice) {
          return;
        }
        
        const touch = event.changedTouches[0];
        const touchEndX = touch.clientX;
        const touchEndY = touch.clientY;
        const touchEndTime = Date.now();
        
        // 计算移动距离
        const deltaX = Math.abs(touchEndX - this.touchStartX);
        const deltaY = Math.abs(touchEndY - this.touchStartY);
        const deltaTime = touchEndTime - this.touchStartTime;
        
        // 设置阈值：如果移动距离超过10px，或者时间超过300ms，认为是滑动而不是点击
        const SWIPE_THRESHOLD = 10;
        const TIME_THRESHOLD = 300;
        
        // 如果是滑动操作，标记一下（用于row-click事件判断）
        if (deltaX > SWIPE_THRESHOLD || deltaY > SWIPE_THRESHOLD || deltaTime > TIME_THRESHOLD) {
          this.isSwipeGesture = true;
          // 100ms后重置标记
          setTimeout(() => {
            this.isSwipeGesture = false;
          }, 100);
        } else {
          this.isSwipeGesture = false;
        }
      },

      // 处理邮箱配置表格行点击事件（移动端）
      handleEmailRowClick(row, column, event) {
        // 只在移动设备上响应点击
        if (!this.isMobileDevice) {
          return;
        }
        
        // 如果是滑动手势，不触发点击
        if (this.isSwipeGesture) {
          return;
        }
        
        // 如果点击的是操作列中的按钮，不触发行点击
        if (event.target.tagName === 'BUTTON' || 
            event.target.closest('.el-button') || 
            event.target.closest('.el-switch') ||
            event.target.closest('.el-input') ||
            event.target.closest('.el-input-number')) {
          return;
        }
        
        // 保存当前邮箱配置和索引
        this.currentEmailConfig = { ...row };
        this.currentEmailConfigIndex = this.emailConfigs.indexOf(row);
        
        // 显示详情对话框
        this.emailDetailDialogVisible = true;
      },

      // 从详情页测试邮件
      testEmailFromDetail() {
        this.emailDetailDialogVisible = false;
        this.$nextTick(() => {
          this.testEmailConfig(this.currentEmailConfig, this.currentEmailConfigIndex);
        });
      },

      // 从详情页编辑配置（关闭详情对话框，让用户直接在表格编辑）
      editEmailFromDetail() {
        this.emailDetailDialogVisible = false;
        this.$message.info('请在表格中直接编辑配置');
        
        // 如果可能，滚动到对应的表格行
        this.$nextTick(() => {
          const tableElement = document.querySelector('.responsive-table-container');
          if (tableElement) {
            tableElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }
        });
      },

      // 从详情页显示高级设置
      showAdvancedFromDetail() {
        this.emailDetailDialogVisible = false;
        this.$nextTick(() => {
          this.showAdvancedConfig(this.currentEmailConfigIndex);
        });
      }
    }
  }
</script>

<style scoped>

  .my-tag {
    margin-bottom: 20px !important;
    width: 100%;
    text-align: left;
    background: var(--lightYellow);
    border: none;
    height: 40px;
    line-height: 40px;
    font-size: 16px;
    color: var(--black);
  }
  
  /* 网站地址容器样式 */
  .site-address-container {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .site-address-input {
    flex: 1;
  }
  
  .simple-address-actions {
    display: flex;
    gap: 8px;
  }
  
  .simple-address-btn {
    min-width: 90px;
  }
  
  .tip {
    display: block;
    margin-top: 8px;
    font-size: 12px;
    color: #999;
    line-height: 1.5;
  }
  
  .tip strong {
    color: #0071e3;
    font-weight: 600;
  }

  /* 网站地址移动端适配 */
  @media screen and (max-width: 768px) {
    .site-address-container {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    
    .site-address-input {
      width: 100%;
    }
    
    .simple-address-actions {
      width: 100%;
      justify-content: stretch;
    }
    
    .simple-address-btn {
      flex: 1;
      min-width: unset;
    }
  }

  @media screen and (max-width: 480px) {
    .site-address-container {
      gap: 8px;
    }
    
    .simple-address-actions {
      gap: 6px;
    }
    
    .simple-address-btn {
      font-size: 13px;
      padding: 8px 12px;
    }
  }

  /* ===========================================
     表单移动端样式 - PC端和移动端响应式
     =========================================== */
  
  /* PC端样式 - 768px以上 */
  @media screen and (min-width: 769px) {
    ::v-deep .el-form-item__label {
      float: left !important;
    }
  }

  /* 移动端样式 - 768px及以下 */
  @media screen and (max-width: 768px) {
    /* 表单标签 - 垂直布局 */
    ::v-deep .el-form-item__label {
      float: none !important;
      width: 100% !important;
      text-align: left !important;
      margin-bottom: 8px !important;
      font-weight: 500 !important;
      font-size: 14px !important;
      padding-bottom: 0 !important;
      line-height: 1.5 !important;
    }

    ::v-deep .el-form-item__content {
      margin-left: 0 !important;
      width: 100% !important;
    }

    ::v-deep .el-form-item {
      margin-bottom: 20px !important;
      padding: 0 10px !important;
    }

    /* 输入框移动端优化 */
    ::v-deep .el-input__inner {
      font-size: 16px !important;
      height: 44px !important;
      border-radius: 8px !important;
    }

    ::v-deep .el-textarea__inner {
      font-size: 16px !important;
      border-radius: 8px !important;
    }

    /* 选择器移动端优化 */
    ::v-deep .el-select {
      width: 100% !important;
    }

    ::v-deep .el-select .el-input__inner {
      height: 44px !important;
      line-height: 44px !important;
    }

    /* 按钮移动端优化 */
    ::v-deep .el-button {
      min-height: 40px !important;
      border-radius: 8px !important;
    }
  }

  /* 极小屏幕优化 - 480px及以下 */
  @media screen and (max-width: 480px) {
    ::v-deep .el-form-item__label {
      font-size: 13px !important;
    }

    ::v-deep .el-input__inner,
    ::v-deep .el-select .el-input__inner {
      height: 40px !important;
      line-height: 40px !important;
      font-size: 15px !important;
    }

    ::v-deep .el-button {
      min-height: 38px !important;
      font-size: 14px !important;
    }
  }

  .el-tag {
    margin: 10px;
  }

  .button-new-tag {
    margin: 10px;
    height: 32px;
    line-height: 32px;
    padding-top: 0;
    padding-bottom: 0;
  }

  .input-new-tag {
    width: 200px;
    margin: 10px;
  }

  .my-icon {
    cursor: pointer;
    margin-left: 10px;
    font-size: 18px;
    font-weight: bold;
    color: var(--blue);
  }

  .table-td-thumb {
    border-radius: 2px;
    width: 40px;
    height: 40px;
  }

  .template-hint {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
  }

  .third-login-config .platform-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .platform-card {
    border-radius: 8px;
    /* 性能优化: 只监听背景色和边框变化 */
    transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.3s ease;
    transform: translateZ(0);
  }

  .platform-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }

  /* 移动端第三方登录配置适配 */
  @media screen and (max-width: 768px) {
    .third-login-config .platform-cards {
      grid-template-columns: 1fr;
      gap: 15px;
    }
  }

  /* 超小屏幕适配 - 侧边栏未折叠 */
  @media screen and (max-width: 500px) {
    .third-login-config .platform-cards {
      grid-template-columns: 1fr;
      gap: 10px;
      padding: 0;
    }

    .platform-card {
      margin: 0;
      border-radius: 4px;
    }

    .platform-card:hover {
      transform: none;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    .platform-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
    }

    .platform-logo {
      width: 100%;
    }

    .platform-actions {
      flex-direction: column;
      gap: 8px;
    }

    .platform-actions .el-button {
      width: 100%;
      margin: 0 !important;
    }
  }

  /* 超小屏幕适配 - 侧边栏折叠 */
  @media screen and (max-width: 425px) {
    .third-login-config {
      padding: 0;
    }

    .third-login-config .box-card {
      margin: 0;
      border-radius: 0;
    }

    .third-login-config .platform-cards {
      grid-template-columns: 1fr;
      gap: 8px;
      padding: 0;
    }

    .platform-form .el-form-item {
      margin-bottom: 12px;
    }

    .platform-form .el-form-item__label {
      font-size: 13px;
    }

    .platform-form .el-input {
      font-size: 14px;
    }
  }

  .platform-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    padding-bottom: 15px;
    border-bottom: 1px solid #f0f0f0;
  }

  .platform-logo {
    display: flex;
    align-items: center;
  }

  .platform-name {
    font-size: 18px;
    font-weight: 500;
    margin-left: 10px;
  }

  .platform-form {
    margin-bottom: 15px;
  }

  .platform-actions {
    display: flex;
    justify-content: flex-end;
    padding-top: 10px;
    border-top: 1px dashed #f0f0f0;
  }

  /* 平台特定样式 */
  .github-card .platform-header {
    color: #333;
  }

  .google-card .platform-header {
    color: #4285F4;
  }

  .twitter-card .platform-header {
    color: #1DA1F2;
  }

  .yandex-card .platform-header {
    color: #FF0000;
  }

  /* 响应式表格样式 */
  .responsive-table-container {
    width: 100%;
    overflow-x: auto;
  }

  /* 移动设备样式 */
  @media screen and (max-width: 768px) {
    .el-table__header-wrapper {
      font-size: 13px;
    }

    .el-table--small td {
      padding: 8px 0;
    }

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

    .table-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-start;
    }

    .table-actions .el-button {
      margin: 2px 5px;
      padding: 5px;
    }

    .delete-btn {
      color: #f56c6c;
    }
  }
  
  /* 移动端提示面板优化 */
  .mobile-view-notice {
    animation: slideInDown 0.3s ease-out;
  }
  
  @keyframes slideInDown {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .captcha-card-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* 智能验证码配置移动端适配 */
  @media screen and (max-width: 768px) {
    /* 应用场景卡片适配 */
    .captcha-config-card .el-row .el-col-12 {
      width: 100% !important;
      margin-bottom: 15px;
    }
    
    /* 表单标签宽度适配 */
    .captcha-config-card .el-form-item__label {
      width: auto !important;
      text-align: left !important;
      padding-right: 10px !important;
      font-size: 14px !important;
    }
    
    /* 验证码参数表单适配 */
    .captcha-config-card .el-tabs .el-form {
      margin: 0 !important;
    }
    
    .captcha-config-card .el-tabs .el-form-item__label {
      font-size: 13px !important;
      line-height: 1.3 !important;
      white-space: normal !important;
      word-break: break-all !important;
    }
    
    .captcha-config-card .el-slider {
      width: 100% !important;
    }
    
    /* 应用场景卡片文字适配 */
    .captcha-config-card .el-card__header {
      font-size: 15px !important;
    }
    
    .captcha-config-card .el-card__body p {
      font-size: 14px !important;
      margin: 8px 0 !important;
    }
  }

  @media screen and (max-width: 480px) {
    /* 卡片内边距优化 */
    .captcha-config-card .el-card__body {
      padding: 10px !important;
    }
    
    /* 卡片头部适配 */
    .captcha-config-card .el-card__header {
      padding: 10px !important;
      font-size: 14px !important;
    }
    
    /* 表单项间距优化 */
    .captcha-config-card .el-form-item {
      margin-bottom: 15px !important;
    }
    
    .captcha-config-card .el-form-item__label {
      font-size: 12px !important;
      padding-right: 8px !important;
      line-height: 1.2 !important;
    }
    
    /* 分隔线文字适配 */
    .captcha-config-card .el-divider__text {
      font-size: 13px !important;
      padding: 0 10px !important;
    }
    
    /* Tabs适配 */
    .captcha-config-card .el-tabs__item {
      font-size: 13px !important;
      padding: 0 10px !important;
    }
    
    /* 输入框和数字输入框适配 */
    .captcha-config-card .el-input-number {
      width: 100% !important;
    }
    
    .captcha-config-card .el-input-number__decrease,
    .captcha-config-card .el-input-number__increase {
      width: 28px !important;
    }
    
    /* Tooltip内容适配 */
    .captcha-config-card .el-tooltip {
      width: 100% !important;
    }
    
    /* 应用场景卡片内容适配 */
    .captcha-config-card .el-card__body p {
      font-size: 13px !important;
      margin: 6px 0 !important;
    }
    
    .captcha-config-card .el-card__body p:last-child {
      font-size: 11px !important;
    }
  }

  .tag-icon {
    margin-top: 20px;
  }

  .icon {
    width: 16px;
    height: 16px;
  }

  /* 导航栏预览样式 */
  .nav-preview-section {
    margin-top: 15px;
    border-top: 1px solid #EBEEF5;
    padding-top: 15px;
  }

  .nav-preview-title {
    font-weight: bold;
    margin-bottom: 10px;
  }

  .nav-preview-container {
    background-color: rgba(0,0,0,0.7);
    padding: 8px 15px;
    border-radius: 4px;
    display: flex;
    flex-wrap: wrap;
    min-height: 40px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    margin-bottom: 5px;
    align-items: center;
  }

  .nav-item-preview {
    margin-right: 15px;
    color: white;
    padding: 5px 0;
    display: flex;
    align-items: center;
    cursor: pointer;
    position: relative;
    /* 性能优化: 只监听背景色和边框 */
    transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.3s ease;
    transform: translateZ(0);
  }

  .nav-item-preview:hover {
    color: var(--themeBackground, #409EFF);
    transform: translateY(-2px);
  }

  .nav-item-preview:hover::after {
    content: "";
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 100%;
    height: 2px;
    background-color: var(--themeBackground, #409EFF);
    animation: navItemHover 0.3s ease-in-out;
  }

  @keyframes navItemHover {
    from {
      width: 0;
    }
    to {
      width: 100%;
    }
  }

  .nav-item-icon {
    margin-right: 5px;
    font-size: 16px;
  }

  .nav-item-name {
    font-size: 14px;
  }

  .nav-item-dropdown {
    font-size: 12px;
    margin-left: 3px;
  }

  /* 移动端侧边栏预览样式 */
  .drawer-preview {
    width: 100%;
    min-height: 300px;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .drawer-preview {
    --drawer-mask-opacity: 0.7;
  }

  .drawer-preview::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, var(--drawer-mask-opacity));
    z-index: 1;
  }

  .drawer-preview-header {
    position: relative;
    z-index: 2;
    padding: 20px;
    text-align: center;
  }

  .preview-title {
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 2px;
  }

  .preview-avatar {
    width: 70px;
    height: 70px;
    border-radius: 50%;
    overflow: hidden;
    margin: auto;
  }

  .preview-avatar .el-image {
    width: 100%;
    height: 100%;
  }

  /* 预览区域的分隔线 */
  .preview-divider {
    position: relative;
    margin: 30px auto 20px;
    border: 0;
    border-top: 1px dashed var(--lightGreen);
    overflow: visible;
    z-index: 2;
  }

  .preview-divider::before {
    position: absolute;
    top: 50%;
    left: 5%;
    transform: translateY(-50%);
    color: var(--lightGreen);
    content: "";
    font-size: 28px;
    line-height: 1;
  }

  .preview-divider.show-snowflake::before {
    content: "❄";
  }

  .drawer-preview-menu {
    position: relative;
    z-index: 2;
    padding: 10px 0;
  }

  .preview-menu-item {
    padding: 15px 20px;
    color: white;
    font-size: 16px;
    cursor: pointer;
    transition: background-color 0.3s;
  }

  .preview-menu-item:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }

  /* 配置对话框表单样式 */
  .drawer-config-form .el-form-item__label {
    text-align: left !important;
    padding-right: 8px;
  }

  /* 配置对话框底部按钮样式 */
  .drawer-config-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }

  .drawer-config-footer .footer-btn {
    min-width: 100px;
  }

  /* 移动端侧边栏配置对话框适配 */
  @media screen and (max-width: 768px) {
    .mobile-drawer-config-dialog .el-dialog__title {
      font-size: 16px !important;
    }

    .mobile-drawer-config-dialog .el-form {
      margin: 0 !important;
    }

    .mobile-drawer-config-dialog .el-form-item {
      margin-bottom: 15px !important;
    }

    .mobile-drawer-config-dialog .el-form-item__label {
      width: 55px !important;
      font-size: 12px !important;
      padding-right: 4px !important;
      line-height: 1.2 !important;
      white-space: normal !important;
      word-break: break-all !important;
    }

    .mobile-drawer-config-dialog .el-form-item__content {
      margin-left: 55px !important;
    }

    .mobile-drawer-config-dialog .el-radio-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .mobile-drawer-config-dialog .el-radio {
      margin-right: 0 !important;
    }

    .mobile-drawer-config-dialog .el-input {
      font-size: 14px !important;
    }

    .mobile-drawer-config-dialog .el-slider {
      width: 100% !important;
    }

    .mobile-drawer-config-dialog .el-select {
      width: 100% !important;
    }

    .mobile-drawer-config-dialog .el-select-dropdown__item {
      padding: 8px 12px !important;
    }

    .mobile-drawer-config-dialog .el-color-picker {
      width: auto !important;
    }

    /* 预览区域适配 */
    .mobile-drawer-config-dialog .drawer-preview {
      min-height: 250px !important;
      border-radius: 8px !important;
    }

    .mobile-drawer-config-dialog .drawer-preview-header {
      padding: 15px !important;
    }

    .mobile-drawer-config-dialog .preview-title {
      font-size: 18px !important;
    }

    .mobile-drawer-config-dialog .preview-menu-item {
      padding: 12px 15px !important;
      font-size: 14px !important;
    }

    /* 图片上传组件适配 */
    .mobile-drawer-config-dialog .el-image {
      width: 150px !important;
      height: 100px !important;
    }

    /* 底部按钮适配 */
    .mobile-drawer-config-dialog .el-dialog__footer {
      padding: 12px 20px !important;
    }

    .mobile-drawer-config-dialog .drawer-config-footer {
      flex-direction: column;
      gap: 10px;
      width: 100%;
    }

    .mobile-drawer-config-dialog .drawer-config-footer .footer-btn {
      width: 100% !important;
      margin: 0 !important;
      height: 40px !important;
      font-size: 15px !important;
      min-width: unset !important;
    }
  }

  /* 超小屏幕适配 */
  @media screen and (max-width: 480px) {
    .mobile-drawer-config-dialog .el-form-item__label {
      width: 50px !important;
      font-size: 11px !important;
      padding-right: 3px !important;
    }

    .mobile-drawer-config-dialog .el-form-item__content {
      margin-left: 50px !important;
    }

    .mobile-drawer-config-dialog .drawer-preview {
      min-height: 200px !important;
    }

    .mobile-drawer-config-dialog .preview-title {
      font-size: 16px !important;
    }

    .mobile-drawer-config-dialog .el-image {
      width: 120px !important;
      height: 80px !important;
    }
  }

  /* 邮箱配置详情对话框样式 */
  .email-detail-dialog {
    max-width: 600px;
  }

  .email-detail-content {
    padding: 0;
  }

  .email-detail-content .el-descriptions {
    margin-bottom: 0;
  }

  .email-detail-content .el-descriptions-item__label {
    width: 35%;
    font-weight: 500;
    background-color: #fafafa;
  }

  .email-detail-content .el-descriptions-item__content {
    word-break: break-all;
  }

  /* 移动端邮箱详情对话框适配 */
  @media screen and (max-width: 768px) {
    .email-detail-content .el-descriptions-item__label {
      width: 40%;
      font-size: 13px;
      padding: 8px 10px !important;
    }

    .email-detail-content .el-descriptions-item__content {
      font-size: 13px;
      padding: 8px 10px !important;
    }

    .email-detail-content .el-button {
      font-size: 13px;
      padding: 8px 15px;
    }
  }

  @media screen and (max-width: 480px) {
    .email-detail-content .el-descriptions-item__label {
      width: 35%;
      font-size: 12px;
      padding: 6px 8px !important;
    }

    .email-detail-content .el-descriptions-item__content {
      font-size: 12px;
      padding: 6px 8px !important;
    }

    .email-detail-content > div:last-child {
      flex-direction: column;
      width: 100%;
    }

    .email-detail-content .el-button {
      width: 100%;
      margin: 0 0 8px 0 !important;
      font-size: 14px;
    }
  }

  /* 移动端AI配置卡片 */
  .ai-config-mobile-cards {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 0 10px;
  }

  .ai-config-mobile-cards .config-card {
    display: flex;
    align-items: center;
    padding: 16px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .ai-config-mobile-cards .config-card:active {
    background: #f5f7fa;
    transform: scale(0.98);
  }

  .ai-config-mobile-cards .config-card > i:first-child {
    font-size: 24px;
    color: #409EFF;
    margin-right: 12px;
  }

  .ai-config-mobile-cards .config-card > span {
    flex: 1;
    font-size: 15px;
    font-weight: 500;
    color: #303133;
  }

  .ai-config-mobile-cards .config-card > i:last-child {
    font-size: 16px;
    color: #c0c4cc;
  }

  /* ========== 暗色模式适配 - AI配置卡片 ========== */
  
  .dark-mode .ai-config-mobile-cards .config-card {
    background: #2c2c2c !important;
    border-color: #404040 !important;
  }
  
  .dark-mode .ai-config-mobile-cards .config-card:active {
    background: #333333 !important;
  }
  
  .dark-mode .ai-config-mobile-cards .config-card > span {
    color: #e0e0e0 !important;
  }
  
  .dark-mode .ai-config-mobile-cards .config-card > i:last-child {
    color: #707070 !important;
  }

  /* captcha-config-card 暗色模式 */
  .dark-mode .captcha-config-card {
    background-color: #2c2c2c !important;
    border-color: #404040 !important;
  }
  
  .dark-mode .captcha-config-card .el-card__header {
    background-color: #2c2c2c !important;
    border-bottom-color: #404040 !important;
    color: #e0e0e0 !important;
  }
  
  .dark-mode .captcha-config-card .el-card__body {
    background-color: #2c2c2c !important;
    color: #b0b0b0 !important;
  }

  /* 看板娘AI聊天配置移动端适配 */
  @media screen and (max-width: 768px) {
    /* AI聊天配置外层容器 */
    .el-form-item + div {
      margin-left: 0 !important;
      padding-left: 10px !important;
      padding-right: 10px !important;
    }
  }

  @media screen and (max-width: 480px) {
    .ai-config-mobile-cards {
      gap: 10px;
      padding: 0 5px;
    }

    .ai-config-mobile-cards .config-card {
      padding: 14px;
    }

    .ai-config-mobile-cards .config-card > span {
      font-size: 14px;
    }
  }


</style>

<style>
/* 移动端AI配置对话框 */
@media screen and (max-width: 768px) {
  /* 蓝色Header样式 */
  .mobile-ai-config-dialog .el-dialog__header {
    /* background: #409EFF; */
    padding: 16px 20px;
  }

  .mobile-ai-config-dialog .el-dialog__title {
    /* color: #fff; */
    font-size: 18px;
    font-weight: 600;
  }

  .mobile-ai-config-dialog .el-dialog__headerbtn .el-dialog__close {
    /* color: #fff; */
    font-size: 22px;
  }

  .mobile-ai-config-dialog .el-dialog__footer {
    padding: 0 !important;
  }

  .mobile-ai-config-dialog .dialog-footer {
    display: flex;
    gap: 10px;
    padding: 15px;
    border-top: 1px solid #e4e7ed;
    background: #fff;
  }
  
  /* 暗色模式 - 移动端AI配置对话框 footer */
  .dark-mode .mobile-ai-config-dialog .dialog-footer {
    background: #2c2c2c !important;
    border-top-color: #404040 !important;
  }

  .mobile-ai-config-dialog .dialog-footer .el-button {
    flex: 1;
    padding: 12px;
    font-size: 15px;
  }
}

@media screen and (max-width: 480px) {
  .mobile-ai-config-dialog .dialog-footer {
    padding: 12px 10px;
  }

  .mobile-ai-config-dialog .dialog-footer .el-button {
    font-size: 14px;
    padding: 10px;
  }
}

/* el-message-box 移动端适配 - 非scoped样式，作用于全局动态元素 */
@media screen and (max-width: 768px) {
  /* 通用confirm对话框适配 */
  .mobile-responsive-confirm {
    width: 90% !important;
    max-width: 400px !important;
  }

  .mobile-responsive-confirm .el-message-box__header {
    padding: 15px !important;
  }

  .mobile-responsive-confirm .el-message-box__title {
    font-size: 16px !important;
  }

  .mobile-responsive-confirm .el-message-box__content {
    padding: 15px !important;
    font-size: 14px !important;
  }

  .mobile-responsive-confirm .el-message-box__btns {
    padding: 10px 15px 15px !important;
  }

  .mobile-responsive-confirm .el-message-box__btns button {
    padding: 10px 20px !important;
    font-size: 14px !important;
  }

  /* 预设对话框适配 */
  .preset-dialog {
    width: 90% !important;
    max-width: 500px !important;
  }

  .preset-dialog .el-message-box__header {
    padding: 15px !important;
  }

  .preset-dialog .el-message-box__title {
    font-size: 16px !important;
  }

  .preset-dialog .el-message-box__content {
    padding: 15px !important;
  }

  .preset-dialog .preset-btn {
    width: calc(50% - 10px) !important;
    margin: 5px !important;
    padding: 12px 8px !important;
    font-size: 13px !important;
  }
}

@media screen and (max-width: 480px) {
  .mobile-responsive-confirm {
    width: 95% !important;
  }

  .mobile-responsive-confirm .el-message-box__header {
    padding: 12px !important;
  }

  .mobile-responsive-confirm .el-message-box__title {
    font-size: 15px !important;
  }

  .mobile-responsive-confirm .el-message-box__content {
    padding: 12px !important;
    font-size: 13px !important;
  }

  .mobile-responsive-confirm .el-message-box__btns {
    padding: 8px 12px 12px !important;
    display: flex;
    flex-direction: column-reverse;
    gap: 8px;
  }

  .mobile-responsive-confirm .el-message-box__btns button {
    width: 100% !important;
    margin: 0 !important;
    padding: 12px 20px !important;
    font-size: 15px !important;
  }

  /* 预设对话框超小屏幕适配 */
  .preset-dialog {
    width: 95% !important;
  }

  .preset-dialog .el-message-box__header {
    padding: 12px !important;
  }

  .preset-dialog .el-message-box__title {
    font-size: 15px !important;
  }

  .preset-dialog .el-message-box__content {
    padding: 12px !important;
    max-height: 60vh;
    overflow-y: auto;
  }

  .preset-dialog .preset-btn {
    width: 100% !important;
    margin: 5px 0 !important;
    padding: 14px 10px !important;
    font-size: 14px !important;
  }
}

</style>

<!-- 全局样式：修复 el-collapse 内 el-select 下拉框被裁剪的问题 -->
<style>
/* 确保折叠面板内容区域不裁剪下拉框 */
.el-collapse-item__wrap {
  overflow: visible !important;
}

.el-collapse-item__content {
  overflow: visible !important;
}

/* 确保下拉框有足够的 z-index */
.el-select-dropdown {
  z-index: 3000 !important;
}

/* 确保 popper 容器不被隐藏 */
.el-popper {
  z-index: 3000 !important;
}
</style>
