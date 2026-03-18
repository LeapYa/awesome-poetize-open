<template>
  <div>
    <!-- 登陆和注册 -->
    <div
      v-if="$common.isEmpty(currentUser)"
      class="myCenter in-up-container my-animation-hideToShow"
    >
      <!-- 背景图片 -->
      <el-image
        class="my-el-image"
        style="position: absolute"
        v-once
        lazy
        :src="
          mainStore.webInfo.randomCover &&
          mainStore.webInfo.randomCover.length > 0
            ? mainStore.webInfo.randomCover[
                Math.floor(Math.random() * mainStore.webInfo.randomCover.length)
              ]
            : '/assets/backgroundPicture.jpg'
        "
        fit="cover"
      >
        <template v-slot:error>
          <div class="image-slot"></div>
        </template>
      </el-image>
      <div class="in-up" id="loginAndRegist">
        <div class="form-container sign-up-container">
          <div class="myCenter">
            <h1>注册</h1>
            <input
              v-model="username"
              type="text"
              maxlength="30"
              placeholder="用户名"
            />
            <div class="password-field">
              <input
                v-model="password"
                :type="showRegisterPassword ? 'text' : 'password'"
                maxlength="30"
                placeholder="密码"
              />
              <span
                v-show="password"
                role="button"
                tabindex="0"
                class="password-toggle"
                :aria-label="showRegisterPassword ? '隐藏密码' : '显示密码'"
                :title="showRegisterPassword ? '隐藏密码' : '显示密码'"
                @click="showRegisterPassword = !showRegisterPassword"
                @keydown.enter.prevent="showRegisterPassword = !showRegisterPassword"
                @keydown.space.prevent="showRegisterPassword = !showRegisterPassword"
              >
                <svg
                  v-if="showRegisterPassword"
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  aria-hidden="true"
                >
                  <path
                    d="M3 3L21 21M10.58 10.58A2 2 0 0 0 13.41 13.41M9.88 5.09A9.77 9.77 0 0 1 12 4.8c5 0 9.27 3.11 11 7.2a11.83 11.83 0 0 1-4.05 5.19M6.61 6.61A11.8 11.8 0 0 0 1 12c1.73 4.09 6 7.2 11 7.2a9.6 9.6 0 0 0 4.24-.93M14.12 14.12A3 3 0 0 1 9.88 9.88"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                  />
                </svg>
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  aria-hidden="true"
                >
                  <path
                    d="M1 12C2.73 7.91 7 4.8 12 4.8S21.27 7.91 23 12C21.27 16.09 17 19.2 12 19.2S2.73 16.09 1 12Z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                  />
                  <circle
                    cx="12"
                    cy="12"
                    r="3"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                </svg>
              </span>
            </div>
            <input v-model="email" type="email" placeholder="邮箱" />
            <input
              v-model="code"
              type="text"
              placeholder="验证码"
              disabled
              @keyup.enter="showRegistVerify()"
            />
            <a style="margin: 0" href="#" @click="changeDialog('邮箱验证码')"
              >获取验证码</a
            >
            <el-button
              type="primary"
              round
              class="auth-button"
              @click="showRegistVerify()"
              >注册</el-button
            >
          </div>
        </div>
        <div class="form-container sign-in-container">
          <form
            ref="loginForm"
            class="myCenter login-credential-form"
            autocomplete="on"
            @submit.prevent="showLoginVerify"
          >
            <h1>登录</h1>
            <input
              ref="accountInput"
              v-model="account"
              type="text"
              name="username"
              placeholder="用户名/邮箱/手机号"
            />
            <div class="password-field">
              <input
                ref="passwordInput"
                v-model="password"
                :type="showLoginPassword ? 'text' : 'password'"
                name="password"
                placeholder="密码"
              />
              <span
                v-show="password"
                role="button"
                tabindex="0"
                class="password-toggle"
                :aria-label="showLoginPassword ? '隐藏密码' : '显示密码'"
                :title="showLoginPassword ? '隐藏密码' : '显示密码'"
                @click="showLoginPassword = !showLoginPassword"
                @keydown.enter.prevent="showLoginPassword = !showLoginPassword"
                @keydown.space.prevent="showLoginPassword = !showLoginPassword"
              >
                <svg
                  v-if="showLoginPassword"
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  aria-hidden="true"
                >
                  <path
                    d="M3 3L21 21M10.58 10.58A2 2 0 0 0 13.41 13.41M9.88 5.09A9.77 9.77 0 0 1 12 4.8c5 0 9.27 3.11 11 7.2a11.83 11.83 0 0 1-4.05 5.19M6.61 6.61A11.8 11.8 0 0 0 1 12c1.73 4.09 6 7.2 11 7.2a9.6 9.6 0 0 0 4.24-.93M14.12 14.12A3 3 0 0 1 9.88 9.88"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                  />
                </svg>
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  aria-hidden="true"
                >
                  <path
                    d="M1 12C2.73 7.91 7 4.8 12 4.8S21.27 7.91 23 12C21.27 16.09 17 19.2 12 19.2S2.73 16.09 1 12Z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                  />
                  <circle
                    cx="12"
                    cy="12"
                    r="3"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                </svg>
              </span>
            </div>
            <a href="#" @click="changeDialog('找回密码')">忘记密码？</a>
            <el-button
              type="primary"
              round
              class="auth-button"
              native-type="submit"
              >登 录</el-button
            >

            <!-- 第三方登录区域 - 根据配置动态显示 -->
            <div
              v-if="
                thirdPartyLoginConfig.enable &&
                enabledThirdPartyProviders.length > 0
              "
            >
              <p
                style="
                  text-align: center;
                  margin-top: 20px;
                  margin-bottom: 10px;
                  font-size: 14px;
                  color: var(--articleGreyFontColor);
                "
              >
                第三方账号登录
              </p>

              <div
                class="third-party-login-container"
                style="
                  padding: 0;
                  position: relative;
                  height: 50px;
                  width: 100%;
                  text-align: center;
                  overflow: visible;
                "
              >
                <a
                  v-for="provider in enabledThirdPartyProviders"
                  :key="provider.key"
                  href="javascript:void(0)"
                  @click="showThirdPartyLoginVerify(provider.key)"
                  :title="provider.title"
                  class="third-party-login-btn"
                  style="
                    display: inline-block;
                    width: 40px;
                    height: 40px;
                    margin: 0 10px;
                    border-radius: 50%;
                    vertical-align: middle;
                    position: relative;
                    transition: transform 0.3s ease, opacity 0.3s ease;
                    transform: translateZ(0);
                  "
                >
                  <img
                    :src="provider.icon"
                    :alt="provider.name"
                    height="25"
                    style="
                      position: absolute;
                      top: 50%;
                      left: 50%;
                      transform: translate(-50%, -50%);
                    "
                  />
                </a>
              </div>
            </div>
          </form>
        </div>
        <div class="overlay-container">
          <div class="overlay">
            <div class="overlay-panel myCenter overlay-left">
              <h1>已有帐号？</h1>
              <p>请登录🚀</p>
              <button class="ghost" @click="signIn()">登录</button>
            </div>
            <div class="overlay-panel myCenter overlay-right">
              <h1>没有帐号？</h1>
              <p>立即注册吧😃</p>
              <button class="ghost" @click="signUp()">注册</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户信息 -->
    <div v-else class="user-container myCenter my-animation-hideToShow">
      <!-- 背景图片 -->
      <el-image
        class="my-el-image"
        style="position: absolute"
        v-once
        lazy
        :src="
          mainStore.webInfo.randomCover &&
          mainStore.webInfo.randomCover.length > 0
            ? mainStore.webInfo.randomCover[
                Math.floor(Math.random() * mainStore.webInfo.randomCover.length)
              ]
            : '/assets/backgroundPicture.jpg'
        "
        fit="cover"
      >
        <template v-slot:error>
          <div class="image-slot"></div>
        </template>
      </el-image>
      <div class="shadow-box-mini user-info" style="display: flex">
        <div class="user-left">
          <div>
            <el-avatar
              class="user-avatar"
              @click="changeDialog('修改头像')"
              :size="60"
              :src="$common.getAvatarUrl(currentUser.avatar)"
            >
              <img :src="$getDefaultAvatar()" />
            </el-avatar>
          </div>
          <div class="myCenter" style="margin-top: 12px">
            <div class="user-title">
              <div>用户名：</div>
              <div>手机号：</div>
              <div>邮箱：</div>
              <div v-if="!isThirdPartyUser">密码：</div>
              <div>性别：</div>
              <div>简介：</div>
            </div>
            <div class="user-content">
              <div>
                <el-input
                  maxlength="30"
                  v-model="currentUser.username"
                ></el-input>
              </div>
              <div>
                <div v-if="!$common.isEmpty(currentUser.phoneNumber)">
                  {{ currentUser.phoneNumber }}
                  <span class="changeInfo" @click="changeDialog('修改手机号')"
                    >修改（功能未接入）</span
                  >
                </div>
                <div v-else>
                  <span class="changeInfo" @click="changeDialog('绑定手机号')"
                    >绑定手机号（功能未接入）</span
                  >
                </div>
              </div>
              <div>
                <div v-if="!$common.isEmpty(currentUser.email)">
                  {{ currentUser.email }}
                  <span class="changeInfo" @click="changeDialog('修改邮箱')"
                    >修改</span
                  >
                </div>
                <div v-else>
                  <span class="changeInfo" @click="changeDialog('绑定邮箱')"
                    >绑定邮箱</span
                  >
                </div>
              </div>
              <div v-if="!isThirdPartyUser">
                <div>
                  <span>******</span>
                  <span class="changeInfo" @click="changeDialog('修改密码')" style="margin-left: 10px;"
                    >修改</span
                  >
                </div>
              </div>
              <div>
                <el-radio-group v-model="currentUser.gender">
                  <el-radio :label="0" style="margin-right: 10px"
                    >薛定谔的猫</el-radio
                  >
                  <el-radio :label="1" style="margin-right: 10px">男</el-radio>
                  <el-radio :label="2">女</el-radio>
                </el-radio-group>
              </div>
              <div>
                <el-input
                  v-model="currentUser.introduction"
                  maxlength="60"
                  type="textarea"
                  show-word-limit
                ></el-input>
              </div>
            </div>
          </div>
          <div style="margin-top: 20px">
            <proButton
              :info="'提交'"
              @click="submitUserInfo()"
              :before="'var(--gradualRed)'"
              :after="'var(--gradualRed)'"
            >
            </proButton>
          </div>
        </div>
        <div class="user-right"></div>
      </div>
    </div>

    <el-dialog
      :title="dialogTitle"
      v-model="showDialog"
      width="30%"
      :before-close="clearDialog"
      :append-to-body="true"
      class="centered-dialog"
      :close-on-click-modal="false"
      center
    >
      <div class="myCenter" style="flex-direction: column">
        <div>
          <div
            v-if="dialogTitle === '修改手机号' || dialogTitle === '绑定手机号'"
          >
            <div style="margin-bottom: 5px">手机号：</div>
            <el-input v-model="phoneNumber" placeholder="请输入手机号"></el-input>
            <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
            <el-input v-model="code" placeholder="请输入验证码"></el-input>
            <!-- 只有普通注册用户才需要输入密码，第三方登录用户没有密码 -->
            <div v-if="!isThirdPartyUser">
              <div style="margin-top: 10px; margin-bottom: 5px">密码：</div>
              <el-input
                type="password"
                v-model="password"
                show-password
                placeholder="请输入当前密码"
              ></el-input>
            </div>
          </div>
          <div
            v-else-if="dialogTitle === '修改邮箱' || dialogTitle === '绑定邮箱'"
          >
            <div style="margin-bottom: 5px">邮箱：</div>
            <el-input v-model="email" placeholder="请输入邮箱"></el-input>
            <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
            <el-input v-model="code" placeholder="请输入验证码"></el-input>
            <!-- 只有普通注册用户才需要输入密码，第三方登录用户没有密码 -->
            <div v-if="!isThirdPartyUser">
              <div style="margin-top: 10px; margin-bottom: 5px">密码：</div>
              <el-input
                type="password"
                v-model="password"
                show-password
                placeholder="请输入当前密码"
              ></el-input>
            </div>
          </div>
          <div v-else-if="dialogTitle === '修改密码'">
            <div style="margin-bottom: 5px">旧密码：</div>
            <el-input
              type="password"
              v-model="oldPassword"
              show-password
              placeholder="请输入旧密码"
            ></el-input>
            <div style="margin-top: 10px; margin-bottom: 5px">新密码：</div>
            <el-input
              type="password"
              v-model="newPassword"
              show-password
              placeholder="请输入新密码"
            ></el-input>
            <div style="margin-top: 10px; margin-bottom: 5px">确认新密码：</div>
            <el-input
              type="password"
              v-model="confirmPassword"
              show-password
              placeholder="请在此输入新密码"
            ></el-input>
            <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
            <el-input v-model="code" placeholder="请输入验证码"></el-input>
          </div>
          <div v-else-if="dialogTitle === '修改头像'">
            <uploadPicture
              :prefix="'userAvatar'"
              @addPicture="addPicture"
              :maxSize="1"
              :maxNumber="1"
            ></uploadPicture>
          </div>
          <div v-else-if="dialogTitle === '找回密码'">
            <div class="myCenter" style="margin-bottom: 12px">
              <el-radio-group v-model="passwordFlag">
                <el-radio :label="1" style="margin-right: 10px"
                  >手机号</el-radio
                >
                <el-radio :label="2">邮箱</el-radio>
              </el-radio-group>
            </div>
            <div v-if="passwordFlag === 1">
              <div style="margin-bottom: 5px">用户名：</div>
              <el-input v-model="username" placeholder="请输入绑定的用户名"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">手机号：</div>
              <el-input v-model="phoneNumber" placeholder="请输入绑定的手机号"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
              <el-input v-model="code" placeholder="请输入验证码"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">新密码：</div>
              <el-input maxlength="30" type="password" show-password v-model="password" placeholder="请输入新密码"></el-input>
            </div>
            <div v-else-if="passwordFlag === 2">
              <div style="margin-bottom: 5px">用户名：</div>
              <el-input v-model="username" placeholder="请输入绑定的用户名"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">邮箱：</div>
              <el-input v-model="email" placeholder="请输入绑定的邮箱"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
              <el-input v-model="code" placeholder="请输入验证码"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">新密码：</div>
              <el-input maxlength="30" type="password" show-password v-model="password" placeholder="请输入新密码"></el-input>
            </div>
          </div>
          <div v-else-if="dialogTitle === '邮箱验证码'">
            <div>
              <div style="margin-bottom: 5px">邮箱：</div>
              <el-input v-model="email" placeholder="请输入邮箱"></el-input>
              <div style="margin-top: 10px; margin-bottom: 5px">验证码：</div>
              <el-input v-model="code" placeholder="请输入验证码"></el-input>
            </div>
          </div>
        </div>
        <div
          style="display: flex; margin-top: 30px"
          v-show="dialogTitle !== '修改头像'"
        >
          <proButton
            :info="codeString"
            v-show="
              dialogTitle === '修改手机号' ||
              dialogTitle === '绑定手机号' ||
              dialogTitle === '修改邮箱' ||
              dialogTitle === '绑定邮箱' ||
              dialogTitle === '修改密码' ||
              dialogTitle === '找回密码' ||
              dialogTitle === '邮箱验证码'
            "
            @click="getCode()"
            :before="'var(--gradualRed)'"
            :after="'var(--gradualRed)'"
            style="margin-right: 20px"
          >
          </proButton>
          <proButton
            :info="'提交'"
            @click="submitDialog()"
            :before="'var(--gradualRed)'"
            :after="'var(--gradualRed)'"
          >
          </proButton>
        </div>
      </div>
    </el-dialog>

    <!-- 添加滑动验证组件 -->
    <component
      :is="captchaWrapperComponent"
      v-if="showCaptchaWrapper && captchaWrapperComponent"
      :visible="showCaptchaWrapper"
      :action="captchaAction"
      :force-slide="false"
      @success="onVerifySuccess"
      @fail="closeVerify"
      @refresh="$emit('refresh')"
      @close="closeVerify"
    ></component>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { $on, $off, $once, $emit } from '../utils/gogocodeTransfer'
import { useMainStore } from '@/stores/main'
import { encrypt } from '@/utils/crypto-utils'

import { checkCaptchaWithCache } from '@/utils/captchaUtil'
import { handleLoginRedirect } from '../utils/tokenExpireHandler'

export default {
  components: {
    proButton: defineAsyncComponent(() => import('./common/proButton')),
    uploadPicture: defineAsyncComponent(() => import('./common/uploadPicture')),
  },
  data() {
    return {
      currentUser: {},
      username: '',
      account: '',
      password: '',
      showRegisterPassword: false,
      showLoginPassword: false,
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      phoneNumber: '',
      email: '',
      avatar: '',
      showDialog: false,
      code: '',
      dialogTitle: '',
      codeString: '验证码',
      passwordFlag: null,
      intervalCode: null,
      showCaptchaWrapper: false,
      verifyAction: null,
      captchaAction: 'login',
      verifyParams: null,
      captchaWrapperComponent: null,
      captchaWrapperLoadingPromise: null,
      hasShownExpiredMessage: false,
      thirdPartyLoginConfig: {
        enable: false,
      },
      enabledThirdPartyProviders: [],
    }
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
    // 判断当前用户是否为第三方登录用户
    isThirdPartyUser() {
      return this.currentUser && this.currentUser.platformType
    },
  },
  created() {
    // 初始化当前用户
    this.currentUser = this.mainStore.currentUser
    
    // 如果本地有用户信息，则主动校验一下 Token 是否已过期
    if (!this.$common.isEmpty(this.currentUser)) {
      this.checkUserToken()
    }

    // 动态设置页面SEO信息
    this.updatePageSEO()
    this.showExpiredSessionNotice()
  },
  watch: {
    showCaptchaWrapper(newVal) {
      if (newVal) {
        this.ensureCaptchaWrapperLoaded()
      }
    },
    '$route.query.expired': function () {
      this.showExpiredSessionNotice()
    },
  },
  mounted() {
    // 获取第三方登录配置
    this.loadThirdPartyLoginConfig()

    // 监听第三方登录配置变更事件
    $on(
      this.$bus,
      'thirdPartyLoginConfigChanged',
      this.handleThirdPartyConfigChange
    )

    // 监听登录状态变化，动态更新SEO
    this.$watch('mainStore.currentUser', () => {
      this.updatePageSEO()
    })
  },
  beforeUnmount() {
    // 移除事件监听
    $off(
      this.$bus,
      'thirdPartyLoginConfigChanged',
      this.handleThirdPartyConfigChange
    )
  },
  methods: {
    ensureCaptchaWrapperLoaded() {
      if (this.captchaWrapperComponent) {
        return Promise.resolve(this.captchaWrapperComponent)
      }

      if (!this.captchaWrapperLoadingPromise) {
        this.captchaWrapperLoadingPromise = import('./common/CaptchaWrapper.vue')
          .then((module) => {
            this.captchaWrapperComponent = module.default || module
            return this.captchaWrapperComponent
          })
          .finally(() => {
            this.captchaWrapperLoadingPromise = null
          })
      }

      return this.captchaWrapperLoadingPromise
    },
    // 根据登录状态动态更新页面SEO信息
    updatePageSEO() {
      // 优先使用webTitle，fallback到webName，最后使用默认值
      const webTitle =
        this.mainStore.webInfo?.webTitle ||
        this.mainStore.webInfo?.webName ||
        'POETIZE'
      const isLoggedIn = !this.$common.isEmpty(this.mainStore.currentUser)

      let title, description, keywords

      if (isLoggedIn) {
        // 已登录：个人中心
        const userName = this.mainStore.currentUser?.username || '用户'
        title = `个人中心 - ${webTitle}`
        description = `${userName}的个人中心，管理个人资料和账户设置`
        keywords = `个人中心,用户资料,账户设置,${webTitle}`
      } else {
        // 未登录：登录页面
        title = `登录 - ${webTitle}`
        description = `登录${webTitle}，开始您的精彩之旅`
        keywords = `登录,注册,用户登录,${webTitle}`
      }

      // 更新页面title
      document.title = title
      window.OriginTitile = title

      // 更新meta标签
      this.updateMetaTags({
        title,
        description,
        keywords,
        'og:title': title,
        'og:description': description,
        'og:type': 'website',
      })
    },

    // 更新meta标签的通用方法
    updateMetaTags(metaData) {
      // 移除旧的动态meta标签
      document
        .querySelectorAll('meta[data-dynamic-seo="true"]')
        .forEach((el) => el.remove())

      // 添加新的meta标签
      Object.entries(metaData).forEach(([key, value]) => {
        if (!value || key === 'title') return // title已经设置过

        const meta = document.createElement('meta')
        const isProperty = key.startsWith('og:') || key.startsWith('twitter:')

        if (isProperty) {
          meta.setAttribute('property', key)
        } else {
          meta.setAttribute('name', key)
        }

        meta.setAttribute('content', value)
        meta.setAttribute('data-dynamic-seo', 'true')

        if (document.head) {
          document.head.appendChild(meta)
        }
      })
    },
    showExpiredSessionNotice() {
      if (
        this.$route.query.expired !== 'true' ||
        this.hasShownExpiredMessage
      ) {
        return
      }

      this.hasShownExpiredMessage = true
      this.$nextTick(() => {
        this.$message.warning(
          '你之前的登录状态已经失效，可能是 Token 已过期，请重新登录。'
        )
      })
    },
    // 主动校验前台 Token 是否过期
    checkUserToken() {
      // 通过发出一个需要验证的请求来判断前台Token是否已被移出或过期（401/300）
      this.$http.get(this.$constant.baseURL + '/user/current', {}, false).catch(() => {
        // 请求失败(例如 401)，统一拦截器会自动处理清理本地数据并退出
      });
    },
    addPicture(res) {
      this.avatar = res
      this.submitDialog()
    },
    signUp() {
      document
        .querySelector('#loginAndRegist')
        .classList.add('right-panel-active')
    },
    signIn() {
      document
        .querySelector('#loginAndRegist')
        .classList.remove('right-panel-active')
    },
    showLoginVerify() {
      if (
        this.$common.isEmpty(this.account) ||
        this.$common.isEmpty(this.password)
      ) {
        this.$message({
          message: '请输入账号或密码！',
          type: 'error',
        })
        return
      }

      // 检查是否需要验证码
      checkCaptchaWithCache('login')
        .then((required) => {
          if (required) {
            this.verifyAction = 'login'
            this.captchaAction = 'login'
            this.showCaptchaWrapper = true
          } else {
            // 不需要验证码，直接登录
            this.login()
          }
        })
        .catch((err) => {
          console.error('验证码检查出错:', err)
          // 出错时默认不使用验证码
          this.login()
        })
    },

    showRegistVerify() {
      if (
        this.$common.isEmpty(this.username) ||
        this.$common.isEmpty(this.password)
      ) {
        this.$message({
          message: '请输入用户名或密码！',
          type: 'error',
        })
        return
      }

      if (this.$common.isEmpty(this.email)) {
        this.$message({
          message: '请输入邮箱！',
          type: 'error',
        })
        return false
      }

      if (this.$common.isEmpty(this.code)) {
        this.$message({
          message: '请输入验证码！',
          type: 'error',
        })
        return
      }

      if (
        this.username.indexOf(' ') !== -1 ||
        this.password.indexOf(' ') !== -1
      ) {
        this.$message({
          message: '用户名或密码不能包含空格！',
          type: 'error',
        })
        return
      }

      // 检查是否需要验证码
      checkCaptchaWithCache('register').then((required) => {
        if (required) {
          this.verifyAction = 'regist'
          this.captchaAction = 'register'
          this.showCaptchaWrapper = true
        } else {
          // 不需要验证码，直接注册
          this.regist()
        }
      })
    },

    showThirdPartyLoginVerify(provider) {
      // 检查是否需要验证码
      checkCaptchaWithCache('login').then((required) => {
        if (required) {
          this.verifyAction = 'thirdPartyLogin'
          this.captchaAction = 'login'
          this.verifyParams = provider
          this.showCaptchaWrapper = true
        } else {
          // 不需要验证码，直接执行第三方登录
          this.thirdPartyLogin(provider)
        }
      })
    },

    onVerifySuccess(token) {
      this.showCaptchaWrapper = false

      // 根据当前操作类型继续相应流程
      if (this.verifyAction === 'login') {
        this.login(token)
      } else if (this.verifyAction === 'regist') {
        this.regist(token)
      } else if (this.verifyAction === 'thirdPartyLogin') {
        this.thirdPartyLogin(this.verifyParams, token)
      } else if (this.verifyAction === 'sendVerificationCode') {
        this.sendVerificationCode({ ...this.verifyParams, verificationToken: token })
      }
    },

    closeVerify() {
      this.showCaptchaWrapper = false

      if (this.verifyAction === 'sendVerificationCode') {
        // 重新打开之前的对话框
        this.dialogTitle = this.verifyParams.dialogTitle
        this.$nextTick(() => {
          this.showDialog = true
        })
      }

      // 重置验证相关状态
      this.verifyAction = null
      this.captchaAction = 'login'
      this.verifyParams = null
    },
    /**
     * 登录
     * 注意：虽然前端将同一个token同时存储为userToken和adminToken
     * 但实际的权限控制是在后端严格执行的，不会导致权限绕过问题：
     * 1. 后端通过token前缀和HMAC签名验证token类型
     * 2. 验证用户在数据库中的userType字段
     * 3. 使用@LoginCheck注解进行权限级别验证
     * 4. 即使前端错误设置了adminToken，后端也会拒绝非管理员访问管理员接口
     */
    async login(verificationToken = '') {
      if (
        this.$common.isEmpty(this.account) ||
        this.$common.isEmpty(this.password)
      ) {
        this.$message({
          message: '请输入账号或密码！',
          type: 'error',
        })
        return
      }

      try {
        let user = {
          account: this.account.trim(),
          password: await encrypt(this.password.trim()),
          isAdmin: false, // 普通用户登录，设置为false
        }

        // 添加验证令牌
        if (verificationToken) {
          user.verificationToken = verificationToken
        }

        // 对整个请求体进行加密
        let encryptedUser = await encrypt(JSON.stringify(user))

        this.$http
          .post(
            this.$constant.baseURL + '/user/login',
            { data: encryptedUser },
            true,
            true
          )
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              // Token由后端通过HttpOnly Cookie下发，前端不再存储
              this.mainStore.loadCurrentUser(res.data)
              this.mainStore.loadCurrentAdmin(res.data)

              // 显示登录成功消息
              if (this.$route.query.expired === 'true') {
                this.$message.success('重新登录成功')
              } else {
                this.$message.success('登录成功')
              }

              // 如果来自 /verify 路径，需要根据用户类型进行不同跳转

              if (this.$route.query.fromVerify === 'true') {
                // 检查是否是管理员（userType为0或1）
                if (res.data.userType === 0 || res.data.userType === 1) {
                  // 管理员用户，跳转到 /welcome（忽略 redirect 参数）
                  this.$router.replace('/welcome')
                } else {
                  // 普通用户，跳转到首页
                  this.$router.replace('/')
                }
              } else {
                // 正常情况下的重定向处理
                // 如果有redirect参数且不是/user或/verify，则跳转到该地址
                const redirect = this.$route.query.redirect
                if (
                  redirect &&
                  redirect !== '/user' &&
                  redirect !== '/verify'
                ) {
                  // 如果是跳转到后台管理系统（/admin 开头），需要跨应用跳转
                  if (redirect.startsWith('/admin')) {
                    window.location.href = redirect
                  } else {
                    this.$router.replace(redirect)
                  }
                } else {
                  this.$router.replace('/')
                }
              }
            }
          })
          .catch((error) => {
            if (error && (error.code === 460 || error.code === 461)) {
              this.verifyAction = 'login'
              this.captchaAction = 'login'
              this.showCaptchaWrapper = true
              return
            }
            this.$message({
              message: error.message,
              type: 'error',
            })
          })
      } catch (error) {
        this.$message({
          message: '加密失败: ' + error.message,
          type: 'error',
        })
      }
    },
    async regist(verificationToken) {
      if (
        this.$common.isEmpty(this.username) ||
        this.$common.isEmpty(this.password)
      ) {
        this.$message({
          message: '请输入用户名或密码！',
          type: 'error',
        })
        return
      }

      if (
        this.dialogTitle === '邮箱验证码' &&
        this.$common.isEmpty(this.email)
      ) {
        this.$message({
          message: '请输入邮箱！',
          type: 'error',
        })
        return false
      }

      if (this.$common.isEmpty(this.code)) {
        this.$message({
          message: '请输入验证码！',
          type: 'error',
        })
        return
      }

      if (
        this.username.indexOf(' ') !== -1 ||
        this.password.indexOf(' ') !== -1
      ) {
        this.$message({
          message: '用户名或密码不能包含空格！',
          type: 'error',
        })
        return
      }

      try {
        let user = {
          username: this.username.trim(),
          code: this.code.trim(),
          password: await encrypt(this.password.trim()),
        }

        if (this.dialogTitle === '邮箱验证码') {
          user.email = this.email
        }

        // 添加验证令牌
        if (verificationToken) {
          user.verificationToken = verificationToken
        }

        this.$http
          .post(this.$constant.baseURL + '/user/regist', user)
          .then(async (res) => {
            if (!this.$common.isEmpty(res.data)) {
              // Token由后端通过HttpOnly Cookie下发
              this.mainStore.loadCurrentUser(res.data)
              this.username = ''
              this.password = ''
              this.email = ''
              this.code = ''

              // 检查是否有重定向URL
              const redirect = this.$route.query.redirect
              const hasComment = this.$route.query.hasComment
              const hasReplyAction = this.$route.query.hasReplyAction

              if (redirect) {
                // 保留hasComment和hasReplyAction参数以触发评论/回复状态恢复
                const query = {}
                if (hasComment === 'true') query.hasComment = 'true'
                if (hasReplyAction === 'true') query.hasReplyAction = 'true'
                
                // 如果是跳转到后台管理系统（/admin 开头），需要跨应用跳转
                if (redirect.startsWith('/admin')) {
                  const queryString = Object.keys(query).length > 0 
                    ? '?' + new URLSearchParams(query).toString() 
                    : ''
                  window.location.href = redirect + queryString
                } else {
                  this.$router.push({ path: redirect, query: query })
                }
              } else {
                // 如果没有重定向，则跳转到IM聊天室（IM现在是主站的一部分）
                this.$router.push({ path: '/im' })
              }
            }
          })
          .catch((error) => {
            if (error && (error.code === 460 || error.code === 461)) {
              this.verifyAction = 'regist'
              this.captchaAction = 'register'
              this.showCaptchaWrapper = true
              return
            }
            this.$message({
              message: error.message,
              type: 'error',
            })
          })
      } catch (error) {
        this.$message({
          message: '加密失败: ' + error.message,
          type: 'error',
        })
      }
    },
    submitUserInfo() {
      if (!this.checkParameters()) {
        return
      }

      let user = {
        username: this.currentUser.username,
        gender: this.currentUser.gender,
      }

      if (!this.$common.isEmpty(this.currentUser.introduction)) {
        user.introduction = this.currentUser.introduction.trim()
      }

      this.$confirm('确认保存？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
        center: true,
      })
        .then(() => {
          this.$http
            .post(this.$constant.baseURL + '/user/updateUserInfo', user)
            .then((res) => {
              if (!this.$common.isEmpty(res.data)) {
                this.mainStore.loadCurrentUser(res.data)
                this.currentUser = this.mainStore.currentUser
                this.$message({
                  message: '修改成功！',
                  type: 'success',
                })
              }
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: 'error',
              })
            })
        })
        .catch(() => {
          this.$message({
            type: 'success',
            message: '已取消保存!',
          })
        })
    },
    checkParams(params) {
      if (
        this.dialogTitle === '修改手机号' ||
        this.dialogTitle === '绑定手机号' ||
        (this.dialogTitle === '找回密码' && this.passwordFlag === 1)
      ) {
        params.flag = 1
        if (this.dialogTitle === '找回密码') {
          if (this.$common.isEmpty(this.username)) {
            this.$message.error('请输入用户名！')
            return false
          }
          params.username = this.username.trim()
        }
        if (this.$common.isEmpty(this.phoneNumber)) {
          this.$message({
            message: '请输入手机号！',
            type: 'error',
          })
          return false
        }
        if (!/^1[345789]\d{9}$/.test(this.phoneNumber)) {
          this.$message({
            message: '手机号格式有误！',
            type: 'error',
          })
          return false
        }
        params.place = this.phoneNumber
        return true
      } else if (
        this.dialogTitle === '修改邮箱' ||
        this.dialogTitle === '绑定邮箱' ||
        this.dialogTitle === '邮箱验证码' ||
        (this.dialogTitle === '找回密码' && this.passwordFlag === 2)
      ) {
        params.flag = 2
        if (this.dialogTitle === '找回密码') {
          if (this.$common.isEmpty(this.username)) {
            this.$message.error('请输入用户名！')
            return false
          }
          params.username = this.username.trim()
        }
        if (this.$common.isEmpty(this.email)) {
          this.$message({
            message: '请输入邮箱！',
            type: 'error',
          })
          return false
        }
        if (!/^\w+@[a-zA-Z0-9]{2,10}(?:\.[a-z]{2,4}){1,3}$/.test(this.email)) {
          this.$message({
            message: '邮箱格式有误！',
            type: 'error',
          })
          return false
        }
        params.place = this.email
        return true
      } else if (this.dialogTitle === '修改密码') {
        params.flag = 2
        params.place = this.currentUser.email
        return true
      }
      return false
    },
    checkParameters() {
      if (this.$common.isEmpty(this.currentUser.username)) {
        this.$message({
          message: '请输入用户名！',
          type: 'error',
        })
        return false
      }

      if (this.currentUser.username.indexOf(' ') !== -1) {
        this.$message({
          message: '用户名不能包含空格！',
          type: 'error',
        })
        return false
      }
      return true
    },
    changeDialog(value) {
      if (value === '邮箱验证码') {
        if (this.$common.isEmpty(this.email)) {
          this.$message({
            message: '请输入邮箱！',
            type: 'error',
          })
          return false
        }
        if (!/^\w+@[a-zA-Z0-9]{2,10}(?:\.[a-z]{2,4}){1,3}$/.test(this.email)) {
          this.$message({
            message: '邮箱格式有误！',
            type: 'error',
          })
          return false
        }
      } else if (value === '修改密码') {
        if (this.$common.isEmpty(this.currentUser.email)) {
          this.$message.error('安全起见，请先绑定邮箱后再修改密码！')
          return false
        }
      }

      this.dialogTitle = value
      this.showDialog = true
    },
    submitDialog() {
      if (this.dialogTitle === '修改头像') {
        if (this.$common.isEmpty(this.avatar)) {
          this.$message({
            message: '请上传头像！',
            type: 'error',
          })
        } else {
          let user = {
            avatar: this.avatar.trim(),
          }

          this.$http
            .post(this.$constant.baseURL + '/user/updateUserInfo', user)
            .then((res) => {
              if (!this.$common.isEmpty(res.data)) {
                this.mainStore.loadCurrentUser(res.data)
                this.currentUser = this.mainStore.currentUser
                this.clearDialog()
                this.$message({
                  message: '修改成功！',
                  type: 'success',
                })
              }
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: 'error',
              })
            })
        }
      } else if (
        this.dialogTitle === '修改手机号' ||
        this.dialogTitle === '绑定手机号' ||
        this.dialogTitle === '修改邮箱' ||
        this.dialogTitle === '绑定邮箱'
      ) {
        this.updateSecretInfo()
      } else if (this.dialogTitle === '修改密码') {
        this.updatePassword()
      } else if (this.dialogTitle === '找回密码') {
        if (this.passwordFlag !== 1 && this.passwordFlag !== 2) {
          this.$message({
            message: '请选择找回方式！',
            type: 'error',
          })
        } else {
          this.updateSecretInfo()
        }
      } else if (this.dialogTitle === '邮箱验证码') {
        this.showDialog = false
      }
    },
    async updateSecretInfo() {
      if (this.$common.isEmpty(this.code)) {
        this.$message({
          message: '请输入验证码！',
          type: 'error',
        })
        return
      }
      // 只有普通注册用户才需要验证密码，第三方登录用户没有密码
      if (!this.isThirdPartyUser && this.$common.isEmpty(this.password)) {
        this.$message({
          message: '请输入密码！',
          type: 'error',
        })
        return
      }

      try {
        let params = {
          code: this.code.trim(),
          // 第三方用户没有密码，传空字符串
          password: this.isThirdPartyUser
            ? ''
            : await encrypt(this.password.trim()),
        }
        
        if (this.dialogTitle === '找回密码') {
          if (this.$common.isEmpty(this.username)) {
            this.$message.error('请输入用户名！')
            return
          }
          params.username = this.username.trim()
        }

        if (!this.checkParams(params)) {
          return
        }

        if (this.dialogTitle === '找回密码') {
          this.$http
            .post(
              this.$constant.baseURL + '/user/updateForForgetPassword',
              params,
              false,
              false
            )
            .then((res) => {
              this.clearDialog()
              this.$message({
                message: '修改成功，请重新登陆！',
                type: 'success',
              })
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: 'error',
              })
            })
        } else {
          this.$http
            .post(
              this.$constant.baseURL + '/user/updateSecretInfo',
              params,
              false,
              false
            )
            .then((res) => {
              if (!this.$common.isEmpty(res.data)) {
                this.mainStore.loadCurrentUser(res.data)
                this.currentUser = this.mainStore.currentUser
                this.clearDialog()
                this.$message({
                  message: '修改成功！',
                  type: 'success',
                })
              }
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: 'error',
              })
            })
        }
      } catch (error) {
        this.$message({
          message: '加密失败: ' + error.message,
          type: 'error',
        })
      }
    },
    async updatePassword() {
      if (this.$common.isEmpty(this.oldPassword)) {
        this.$message.error('请输入旧密码！')
        return
      }
      if (this.$common.isEmpty(this.newPassword)) {
        this.$message.error('请输入新密码！')
        return
      }
      if (this.newPassword !== this.confirmPassword) {
        this.$message.error('两次输入的新密码不一致！')
        return
      }
      if (this.$common.isEmpty(this.code)) {
        this.$message.error('请输入邮箱验证码！')
        return
      }

      try {
        let params = {
          place: await encrypt(this.oldPassword.trim()),
          password: await encrypt(this.newPassword.trim()),
          code: this.code.trim(),
          flag: 3
        }

        this.$http
          .post(
            this.$constant.baseURL + '/user/updateSecretInfo',
            params,
            false,
            false
          )
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.clearDialog()
              this.$message.success('密码修改成功，请使用新密码重新登录！')

              // 清除所有登录状态（token由后端通过cookie管理）
              this.mainStore.loadCurrentUser({})
              this.mainStore.loadCurrentAdmin({})
              
              // 强制刷新页面触发现有登出和重载判断，或者跳转回特定页面
              window.location.reload()
            }
          })
          .catch((error) => {
            this.$message.error(error.message)
          })
      } catch (error) {
        this.$message.error('加密失败: ' + error.message)
      }
    },
    getCode() {
      if (this.codeString === '验证码') {
        // 获取验证码前先进行参数检查
        let params = {}
        if (!this.checkParams(params)) {
          return
        }

        // 确定操作类型
        let action = 'reset_password'
        if (
          this.dialogTitle !== '找回密码' &&
          this.dialogTitle !== '邮箱验证码'
        ) {
          action = 'register' // 或其他适当的操作类型
        }

        // 检查是否需要验证码
        checkCaptchaWithCache(action).then((required) => {
          if (required) {
            // 保存当前对话框状态
            const currentDialogTitle = this.dialogTitle

            // 先关闭对话框，避免遮挡验证组件
            this.showDialog = false

            // 设置验证操作为发送验证码，同时保存当前对话框信息
            this.verifyAction = 'sendVerificationCode'
            this.captchaAction = action
            this.verifyParams = {
              ...params,
              dialogTitle: currentDialogTitle,
            }

            // 显示滑块验证
            this.$nextTick(() => {
              this.showCaptchaWrapper = true
            })
          } else {
            // 不需要验证码，直接发送验证码
            this.sendVerificationCode({
              ...params,
              dialogTitle: this.dialogTitle,
            })
          }
        })
      } else {
        this.$message({
          message: '请稍后再试！',
          type: 'warning',
        })
      }
    },
    /**
     * 发送验证码
     */
    sendVerificationCode(params) {
      // 提取出保存的对话框标题
      const savedDialogTitle = params.dialogTitle

      // 从params中移除我们添加的dialogTitle属性，避免发送到后端API
      delete params.dialogTitle

      // 如果有验证令牌，添加到参数中
      if (params.verificationToken) {
      }

      let url
      if (
        savedDialogTitle === '找回密码' ||
        savedDialogTitle === '邮箱验证码'
      ) {
        url = '/user/getCodeForForgetPassword'
      } else if (savedDialogTitle === '修改密码') {
        url = '/user/getCode' // 和后台复用一致机制的发送
      } else {
        url = '/user/getCodeForBind'
      }

      this.$http
        .get(this.$constant.baseURL + url, params)
        .then((res) => {
          this.$message({
            message: '验证码已发送，请注意查收！',
            type: 'success',
          })

          // 重新打开之前的对话框
          this.dialogTitle = savedDialogTitle
          this.$nextTick(() => {
            this.showDialog = true
          })
        })
        .catch((error) => {
          console.error('验证码发送失败:', error)
          this.$message({
            message: error.message,
            type: 'error',
          })

          // 发生错误也重新打开对话框
          this.dialogTitle = savedDialogTitle
          this.$nextTick(() => {
            this.showDialog = true
          })
        })

      // 开始倒计时
      this.codeString = '30'
      this.intervalCode = setInterval(() => {
        if (this.codeString === '0') {
          clearInterval(this.intervalCode)
          this.codeString = '验证码'
        } else {
          this.codeString = parseInt(this.codeString) - 1 + ''
        }
      }, 1000)
    },
    clearDialog() {
      this.password = ''
      this.showRegisterPassword = false
      this.showLoginPassword = false
      this.oldPassword = ''
      this.newPassword = ''
      this.confirmPassword = ''
      this.phoneNumber = ''
      this.email = ''
      this.avatar = ''
      this.showDialog = false
      this.code = ''
      this.dialogTitle = ''
      this.passwordFlag = null
    },
    thirdPartyLogin(provider, verificationToken) {
      if (!provider) return

      // 提取真正的重定向目标地址
      // 如果当前在登录页且有 redirect 参数，使用该参数作为目标地址
      // 否则使用当前路径（非登录页面的情况）
      const urlParams = new URLSearchParams(window.location.search)
      const redirectParam = urlParams.get('redirect')

      let targetRedirect
      if (window.location.pathname === '/user' && redirectParam) {
        // 在登录页面，使用 redirect 参数指定的目标地址
        targetRedirect = redirectParam
      } else if (window.location.pathname === '/user') {
        // 在登录页面但没有 redirect 参数，默认重定向到首页
        targetRedirect = '/'
      } else {
        // 不在登录页面，登录后返回当前页面
        targetRedirect = window.location.pathname + window.location.search
      }

      sessionStorage.setItem('oauthRedirectPath', targetRedirect)

      const params = {
        provider: provider,
      }

      // 添加验证令牌
      if (verificationToken) {
        params.verificationToken = verificationToken
      }

      // 构建请求URL - 使用Java后端OAuth端点（通过Nginx代理，使用相对路径）
      const loginUrl = `${this.$constant.baseURL}/oauth/login/${provider}?redirect=${encodeURIComponent(targetRedirect)}`

      // 记录当前登录方式
      localStorage.setItem('thirdPartyLoginProvider', provider)

      // 使用window.open打开第三方登录授权页面
      window.open(loginUrl, '_self')
    },

    // 处理第三方登录配置变更事件
    handleThirdPartyConfigChange() {
      this.loadThirdPartyLoginConfig()
    },

    // 加载第三方登录配置
    loadThirdPartyLoginConfig() {
      this.getThirdPartyLoginConfig().then((config) => {
        this.thirdPartyLoginConfig = config

        // 提取启用的第三方登录提供商列表
        this.enabledThirdPartyProviders = []
        if (config.enable) {
          // 定义支持的第三方登录平台及其显示信息
          const supportedProviders = [
            {
              key: 'github',
              name: 'GitHub',
              icon: '/static/svg/github.svg',
              title: 'GitHub登录',
            },
            {
              key: 'google',
              name: 'Google',
              icon: '/static/svg/google.svg',
              title: 'Google登录',
            },
            {
              key: 'x',
              name: 'Twitter',
              icon: '/static/svg/x.svg',
              title: 'Twitter登录',
              configKey: 'twitter',
            },
            {
              key: 'yandex',
              name: 'Yandex',
              icon: '/static/svg/yandex.svg',
              title: 'Yandex登录',
            },
            {
              key: 'gitee',
              name: 'Gitee',
              icon: '/static/svg/gitee.svg',
              title: 'Gitee登录',
            },
            {
              key: 'qq',
              name: 'QQ',
              icon: '/static/svg/qq.svg',
              title: 'QQ登录',
            },
            {
              key: 'baidu',
              name: 'Baidu',
              icon: '/static/svg/baidu.svg',
              title: 'Baidu登录',
            },
            {
              key: 'afdian',
              name: '爱发电',
              icon: '/static/svg/afdian.svg',
              title: '爱发电登录',
            },
          ]

          // 检查每个平台是否启用
          supportedProviders.forEach((provider) => {
            const configKey = provider.configKey || provider.key
            if (config[configKey] && config[configKey].enabled === true) {
              this.enabledThirdPartyProviders.push(provider)
            }
          })
        }
      })
    },

    // 获取第三方登录配置
    getThirdPartyLoginConfig() {
      return new Promise((resolve, reject) => {
        this.$http
          .get(this.$constant.baseURL + '/webInfo/getThirdLoginStatus')
          .then((res) => {
            if (res.code === 200 && res.data) {
              resolve(res.data)
            } else {
              resolve({ enable: false })
            }
          })
          .catch((error) => {
            console.error('获取第三方登录配置失败:', error)
            resolve({ enable: false })
          })
      })
    },

    testShowCaptcha() {
      this.showCaptchaWrapper = true
    },
  },
  emits: ['refresh'],
}
</script>

<style scoped>
.in-up-container {
  height: 100vh;
  position: relative;
}
.in-up {
  opacity: 0.9;
  border-radius: 10px;
  box-shadow: 0 15px 30px var(--miniMask), 0 10px 10px var(--miniMask);
  position: relative;
  overflow: hidden;
  width: 750px;
  max-width: 100%;
  min-height: 450px;
  margin: 10px;
}
.in-up p {
  font-size: 14px;
  letter-spacing: 1px;
  margin: 20px 0 30px 0;
  color: var(--articleGreyFontColor);
}
.in-up a {
  color: var(--fontColor);
  font-size: 14px;
  text-decoration: none;
  margin: 15px 0;
}
.form-container {
  position: absolute;
  height: 100%;
  transition: transform 0.5s ease-in-out, left 0.5s ease-in-out;
  will-change: transform, left;
  transform: translateZ(0);
}
.sign-in-container {
  left: 0;
  width: 50%;
  z-index: 2;
  visibility: visible;
  transition: all 0.5s ease-in-out;
}
.sign-up-container {
  left: 0;
  width: 50%;
  opacity: 0;
  z-index: 1;
  visibility: hidden;
  transition: all 0.5s ease-in-out;
}
.form-container > div,
.form-container > form {
  background: var(--background);
  flex-direction: column;
  padding: 10px 20px;
  height: 100%;
  color: var(--fontColor);
}
.login-credential-form {
  width: 100%;
}
.form-container input {
  background: var(--inputBackground);
  border-radius: 3px;
  border: none;
  padding: 12px 15px;
  margin: 10px 0;
  width: 90%;
  height: 40px;
  outline: none;
  color: var(--fontColor);
  line-height: 1.5;
  box-sizing: border-box;
}
.password-field {
  position: relative;
  width: 90%;
  height: 40px;
  margin: 10px 0;
}
.password-field input {
  width: 100%;
  height: 100%;
  margin: 0;
  padding-right: 42px;
  box-sizing: border-box;
}
.password-toggle {
  position: absolute;
  top: 50%;
  right: 12px;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--articleGreyFontColor);
  cursor: pointer;
  z-index: 2;
  line-height: 1;
  user-select: none;
}
.password-toggle:hover {
  color: var(--fontColor);
  transform: translateY(-50%);
}
.password-toggle:focus {
  outline: none;
  color: var(--fontColor);
}
.form-container input::placeholder {
  color: var(--articleGreyFontColor);
}
.in-up button {
  border-radius: 2rem;
  border: none;
  background: var(--gradualRed);
  color: var(--white);
  font-size: 16px;
  font-weight: bold;
  padding: 12px 45px;
  letter-spacing: 2px;
  cursor: pointer;
  box-shadow: 3px 3px 6px var(--miniMask), -1px -1px 4px var(--miniWhiteMask);
  transition: box-shadow 0.3s ease, transform 0.3s ease;
  transform: translateZ(0);
}
.in-up button:hover {
  background: var(--gradualRed);
  box-shadow: 4px 4px 8px var(--mask), -2px -2px 6px var(--miniWhiteMask);
  transform: translateY(-3px);
}
@keyframes glow {
  0% {
    box-shadow: 0 0 5px rgba(255, 99, 71, 0.6);
  }
  50% {
    box-shadow: 0 0 20px rgba(255, 99, 71, 0.8);
  }
  100% {
    box-shadow: 0 0 5px rgba(255, 99, 71, 0.6);
  }
}
.in-up button:active {
  transform: translateY(1px);
  box-shadow: 2px 2px 4px var(--mask);
}
.in-up button.ghost {
  background: linear-gradient(145deg, var(--miniWhiteMask), var(--transparent));
  border: 1px solid var(--miniWhiteMask);
  box-shadow: 3px 3px 6px var(--mask), -1px -1px 4px var(--miniWhiteMask);
}
.in-up button.ghost:hover {
  background: linear-gradient(145deg, var(--whiteMask), var(--miniWhiteMask));
  box-shadow: 4px 4px 8px var(--translucent), -2px -2px 6px var(--miniWhiteMask);
  transform: translateY(-3px);
}
.in-up button.ghost:active {
  transform: translateY(1px);
  box-shadow: 2px 2px 4px var(--mask);
}
.sign-up-container button {
  margin-top: 20px;
}

/* 登录/注册按钮样式 */
.auth-button {
  border-radius: 10px !important;
  width: 90% !important;
  height: 40px !important;
  background: var(--gradualRed) !important;
  border: none !important;
  box-shadow: 3px 3px 6px var(--miniMask), -1px -1px 4px var(--miniWhiteMask) !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease !important;
  padding: 12px 30px !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  transform: translateZ(0);
  line-height: 1.5 !important;
}

.auth-button:hover {
  background: var(--gradualRed) !important;
  box-shadow: 4px 4px 8px var(--mask), -2px -2px 6px var(--miniWhiteMask) !important;
  transform: translateY(-3px) !important;
}

.auth-button:active {
  transform: translateY(1px) !important;
  box-shadow: 2px 2px 4px var(--mask) !important;
}

.overlay-container {
  position: absolute;
  left: 50%;
  width: 50%;
  height: 100%;
  overflow: hidden;
  transition: transform 0.5s ease-in-out, left 0.5s ease-in-out;
  will-change: transform, left;
}
.overlay {
  background: var(--gradualRed);
  color: var(--white);
  position: relative;
  left: -100%;
  height: 100%;
  width: 200%;
}
.overlay-panel {
  position: absolute;
  top: 0;
  flex-direction: column;
  height: 100%;
  width: 50%;
  transition: transform 0.5s ease-in-out, left 0.5s ease-in-out;
  will-change: transform, left;
}
.overlay-right {
  right: 0;
  transform: translateY(0);
  background: var(--gradualRed);
  box-shadow: -4px 0 15px rgba(0, 0, 0, 0.1);
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}
.overlay-left {
  transform: translateY(-20%);
}
.in-up.right-panel-active .sign-in-container {
  transform: translateY(100%);
  opacity: 0;
  visibility: hidden;
  z-index: 1;
}
.in-up.right-panel-active .overlay-container {
  transform: translateX(-100%);
}
.in-up.right-panel-active .sign-up-container {
  transform: translateX(100%);
  opacity: 1;
  z-index: 5;
  visibility: visible;
}
.in-up.right-panel-active .overlay {
  transform: translateX(50%);
}
.in-up.right-panel-active .overlay-left {
  transform: translateY(0);
}
.in-up.right-panel-active .overlay-right {
  transform: translateY(20%);
}
.user-container {
  width: 100vw;
  height: 100vh;
  position: relative;
}
.user-info {
  width: 80%;
  z-index: 10;
  margin-top: 70px;
  height: calc(100vh - 90px);
  margin-bottom: 20px;
  border-radius: 10px;
  overflow: hidden;
}
.user-left {
  width: 50%;
  background: var(--maxMaxWhiteMask);
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
  padding: 20px;
}
.user-right {
  width: 50%;
  background: var(--maxWhiteMask);
  padding: 20px;
}
.user-title {
  text-align: right;
  user-select: none;
}
.user-content {
  text-align: left;
}
.user-title div {
  min-height: 55px;
  line-height: 55px;
  text-align: center;
}
.user-content > div {
  min-height: 55px;
  display: flex;
  align-items: center;
}
.user-content :deep(.el-input__wrapper),
.user-content :deep(.el-textarea__inner){
  border: none;
  background: var(--whiteMask) !important;
  color: var(--fontColor);
  box-shadow: none !important;
}

body.dark-mode .user-content :deep(.el-input__wrapper),
body.dark-mode .user-content :deep(.el-textarea__inner){
  background: #2d2d2d !important;
}

.user-content :deep(.el-input__inner) {
  background: transparent !important;
  color: var(--fontColor);
  border: none;
}
.user-content :deep(.el-input__count){
  background: var(--transparent);
  user-select: none;
}
.changeInfo {
  color: var(--white);
  font-size: 0.75rem;
  cursor: pointer;
  background: var(--themeBackground);
  padding: 3px;
  border-radius: 0.2rem;
  user-select: none;
}
@media screen and (max-width: 920px) {
  .user-info {
    width: 90%;
  }
  .user-left {
    width: 100%;
  }
  .user-right {
    display: none;
  }
}
@media screen and (max-width: 480px) {
  .user-info {
    width: 95%;
    margin-top: 60px;
  }
  .user-left {
    padding: 15px;
  }
  .myCenter {
    flex-direction: column !important;
  }
  .user-title {
    display: none;
  }
  .user-content {
    width: 100%;
  }
  .user-content > div {
    margin-bottom: 15px;
    flex-direction: column;
    align-items: flex-start;
    height: auto;
    min-height: 40px;
  }
  .user-content > div:nth-child(1):before {
    content: '用户名：';
    font-size: 0.85rem;
    margin-bottom: 5px;
    color: var(--fontColor);
    font-weight: 500;
  }
  .user-content > div:nth-child(2):before {
    content: '手机号：';
    font-size: 0.85rem;
    margin-bottom: 5px;
    color: var(--fontColor);
    font-weight: 500;
  }
  .user-content > div:nth-child(3):before {
    content: '邮箱：';
    font-size: 0.85rem;
    margin-bottom: 5px;
    color: var(--fontColor);
    font-weight: 500;
  }
  .user-content > div:nth-child(4):before {
    content: '性别：';
    font-size: 0.85rem;
    margin-bottom: 5px;
    color: var(--fontColor);
    font-weight: 500;
  }
  .user-content > div:nth-child(5):before {
    content: '简介：';
    font-size: 0.85rem;
    margin-bottom: 5px;
    color: var(--fontColor);
    font-weight: 500;
  }
  .user-content :deep(.el-input__inner){
    font-size: 0.85rem;
    padding: 8px 10px;
  }
  .user-content :deep(.el-textarea__inner){
    font-size: 0.85rem;
    padding: 8px 10px;
  }
  .changeInfo {
    font-size: 0.7rem;
    padding: 2px 4px;
    white-space: nowrap;
    margin-left: 8px;
  }
  .user-content > div > div {
    word-break: break-all;
    overflow-wrap: break-word;
    line-height: 1.3;
    max-width: 100%;
  }
  .user-content :deep(.el-radio-group){
    flex-wrap: wrap;
  }
  .user-content :deep(.el-radio){
    margin-right: 8px;
    margin-bottom: 5px;
    font-size: 0.85rem;
  }
}
@media screen and (max-width: 360px) {
  .user-info {
    width: 98%;
    margin-top: 50px;
  }
  .user-left {
    padding: 10px;
  }
  .user-content > div:before {
    font-size: 0.8rem !important;
  }
  .user-content > div {
    margin-bottom: 12px;
    min-height: 40px;
  }
  .user-content :deep(.el-input__inner),
  .user-content :deep(.el-textarea__inner){
    font-size: 0.8rem;
    padding: 6px 8px;
  }
  .changeInfo {
    font-size: 0.65rem;
    padding: 1px 3px;
    margin-left: 6px;
  }
  .user-content :deep(.el-radio){
    font-size: 0.8rem;
    margin-right: 6px;
  }
  .user-avatar {
    width: 50px !important;
    height: 50px !important;
  }
}
@media screen and (max-width: 768px) {
  .el-dialog__body {
    padding: 15px 20px;
  }
}
.third-party-login {
  margin-top: 20px;
  width: 100%;
}
.divider {
  position: relative;
  margin: 15px 0;
  text-align: center;
}
.divider:before,
.divider:after {
  content: '';
  position: absolute;
  top: 50%;
  width: 35%;
  height: 1px;
  background-color: var(--borderColor);
}
.divider:before {
  left: 0;
}
.divider:after {
  right: 0;
}
.divider span {
  display: inline-block;
  padding: 0 10px;
  background-color: var(--background);
  color: var(--articleGreyFontColor);
  position: relative;
  z-index: 1;
  font-size: 12px;
}
.login-buttons {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 10px;
}
.login-button {
  margin: 5px;
  padding: 8px 12px;
  border: 1px solid var(--borderColor);
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: background-color 0.3s ease, box-shadow 0.3s ease,
    transform 0.3s ease;
  background-color: var(--background);
  box-shadow: 0 2px 5px var(--miniMask);
  transform: translateZ(0);
  font-size: 12px;
}
.login-button:hover {
  box-shadow: 0 4px 12px var(--borderHoverColor);
  transform: translateY(-2px);
}
.login-button i {
  margin-right: 6px;
  font-size: 16px;
}
.login-button span {
  font-size: 12px;
}
.login-circle-btn {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 45px;
  height: 40px;
  border-radius: 50%;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: box-shadow 0.3s ease, transform 0.3s ease;
  box-shadow: 0 2px 5px var(--mask);
  transform: translateZ(0);
}
.login-circle-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px var(--borderHoverColor);
}
div > a[href='javascript:void(0)'] {
  overflow: hidden;
}
div > a[href='javascript:void(0)']:hover {
  transform: scale(1.1);
  box-shadow: 0 0 10px var(--borderHoverColor);
}
div > a[href='javascript:void(0)']:hover::before {
  content: '';
  position: absolute;
  top: -10px;
  left: -10px;
  right: -10px;
  bottom: -10px;
  border-radius: 50%;
  animation: pulse 1s infinite;
  z-index: -1;
}
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 var(--borderHoverColor);
  }
  70% {
    box-shadow: 0 0 0 5px var(--transparent);
  }
  100% {
    box-shadow: 0 0 0 0 var(--transparent);
  }
}
.pro-btn {
  box-shadow: 3px 3px 6px var(--miniMask), -1px -1px 4px var(--miniWhiteMask) !important;
  border-radius: 2rem !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
}
.pro-btn:hover {
  box-shadow: 4px 4px 8px var(--mask), -2px -2px 6px var(--miniWhiteMask) !important;
  transform: translateY(-3px) !important;
}
.pro-btn:active {
  box-shadow: 2px 2px 4px var(--miniMask) !important;
  transform: translateY(1px) !important;
}
.form-container .submit {
  background: var(--gradualRed);
  border: none;
  border-radius: 4px;
  color: var(--white);
  width: 90%;
  padding: 15px 20px;
  margin: 15px 10px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease, color 0.3s ease;
}
.form-container .submit:hover {
  background: var(--gradualRed);
  border: none;
}
@media screen and (max-width: 768px) {
  .third-party-login-container {
    height: auto !important;
    min-height: 50px !important;
    padding: 10px 5px !important;
    display: flex !important;
    flex-wrap: wrap !important;
    justify-content: center !important;
    align-items: center !important;
    gap: 8px !important;
    flex-direction: row !important;
  }
  .third-party-login-btn {
    width: 35px !important;
    height: 35px !important;
    margin: 4px !important;
    flex-shrink: 0 !important;
  }
  .third-party-login-btn img {
    height: 20px !important;
  }
}
@media screen and (max-width: 480px) {
  .third-party-login-container {
    padding: 8px 2px !important;
    gap: 4px !important;
    max-width: 100% !important;
    overflow: hidden !important;
    flex-direction: row !important;
  }
  .third-party-login-btn {
    width: 30px !important;
    height: 30px !important;
    margin: 2px !important;
  }
  .third-party-login-btn img {
    height: 17px !important;
  }
}
@media screen and (max-width: 420px) {
  .third-party-login-container {
    padding: 6px 1px !important;
    gap: 3px !important;
    flex-direction: row !important;
  }
  .third-party-login-btn {
    width: 28px !important;
    height: 28px !important;
    margin: 1px !important;
  }
  .third-party-login-btn img {
    height: 16px !important;
  }
}
@media screen and (max-width: 360px) {
  .third-party-login-container {
    padding: 4px 1px !important;
    gap: 2px !important;
  }
  .third-party-login-btn {
    width: 26px !important;
    height: 26px !important;
    margin: 1px !important;
  }
  .third-party-login-btn img {
    height: 15px !important;
  }
}
@media screen and (max-width: 320px) {
  .third-party-login-container {
    padding: 3px 1px !important;
    gap: 1px !important;
    max-width: 100% !important;
  }
  .third-party-login-btn {
    width: 24px !important;
    height: 24px !important;
    margin: 1px !important;
  }
  .third-party-login-btn img {
    height: 13px !important;
  }
}
</style>

<style>
/* 个人中心页面头像旋转动画 */
.user-info .el-avatar.user-avatar {
  cursor: pointer;
  transition: transform 0.6s ease;
  will-change: transform;
  transform: translateZ(0);
}

.user-info .el-avatar.user-avatar:hover {
  transform: rotate(360deg);
}
</style>
