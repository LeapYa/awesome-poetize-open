<template>
  <div class="myCenter verify-container">
    <div class="verify-content">
      <form ref="loginForm" class="login-form" autocomplete="on" @submit.prevent="login">
        <div>
          <el-avatar :size="50" :src="$common.getAvatarUrl(mainStore.webInfo.avatar)">
            <img :src="$getDefaultAvatar()" />
          </el-avatar>
        </div>
        <div>
          <el-input
            ref="accountInput"
            v-model="account"
            name="username"
            autocomplete="username">
            <template slot="prepend">账号</template>
          </el-input>
        </div>
        <div>
          <el-input
            ref="passwordInput"
            v-model="password"
            type="password"
            name="password"
            autocomplete="current-password">
            <template slot="prepend">密码</template>
          </el-input>
        </div>
        <div>
          <proButton :info="'提交'"
                     @click.native="submitLoginForm"
                     :before="$constant.before_color_2"
                     :after="$constant.after_color_2">
          </proButton>
          <button type="submit" class="browser-login-submit" aria-hidden="true" tabindex="-1"></button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
    import { useMainStore } from '@/stores/main';

const proButton = () => import( "../common/proButton");

  import { handleLoginRedirect } from '../../utils/tokenExpireHandler';

  export default {
    components: {
      proButton
    },
    data() {
      return {
        redirect: this.$route.query.redirect || '/welcome',
        account: "",
        password: ""
      }
    },
    computed: {
      mainStore() {
        return useMainStore();
      },},
    created() {

    },
    mounted() {
      this.$nextTick(() => {
        this.syncCredentialInputAttrs();
      });
    },
    updated() {
      this.syncCredentialInputAttrs();
    },
    methods: {
      syncCredentialInputAttrs() {
        const accountInput = this.$refs.accountInput?.$el?.querySelector('input');
        const passwordInput = this.$refs.passwordInput?.$el?.querySelector('input');

        if (accountInput) {
          accountInput.setAttribute('name', 'username');
          accountInput.setAttribute('autocomplete', 'username');
        }

        if (passwordInput) {
          passwordInput.setAttribute('name', 'password');
          passwordInput.setAttribute('autocomplete', 'current-password');
        }
      },
      submitLoginForm() {
        const form = this.$refs.loginForm;
        if (form && typeof form.requestSubmit === "function") {
          form.requestSubmit();
          return;
        }

        this.login();
      },
      async login() {
        if (this.$common.isEmpty(this.account) || this.$common.isEmpty(this.password)) {
          this.$message({
            message: "请输入账号或密码！",
            type: "error"
          });
          return;
        }

        try {
          let user = {
            account: this.account.trim(),
            password: await this.$common.encrypt(this.password.trim()),
            isAdmin: true
          };

          // 对整个请求体进行加密
          let encryptedUser = await this.$common.encrypt(JSON.stringify(user));

          this.$http.post(this.$constant.baseURL + "/user/login", {data: encryptedUser}, true, true)
            .then((res) => {
              if (!this.$common.isEmpty(res.data)) {
                // 清除旧的缓存数据
                localStorage.removeItem("currentAdmin");
                localStorage.removeItem("currentUser");

                // Token由后端通过HttpOnly Cookie下发，前端不再存储

                // 更新Store状态
                this.mainStore.loadCurrentUser( res.data);
                this.mainStore.loadCurrentAdmin( res.data);

                // 显示登录成功消息
                if (this.$route.query.expired === 'true') {
                  this.$message.success('重新登录成功');
                } else {
                  this.$message.success('登录成功');
                }

                // 使用统一的重定向处理逻辑
                handleLoginRedirect(this.$route, this.$router, {
                  defaultPath: '/welcome'
                });
              }
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: "error"
              });
            });
        } catch (error) {
          this.$message({
            message: error.message || "登录失败",
            type: "error"
          });
        }
      }
    }
  }
</script>

<style scoped>

  .verify-container {
    height: 100vh;
    background: var(--backgroundPicture) center center / cover repeat;
  }

  .verify-content {
    background: var(--maxWhiteMask);
    padding: 30px 40px 5px;
    position: relative;
  }

  .login-form > div:first-child {
    position: absolute;
    left: 50%;
    transform: translate(-50%);
    top: -25px;
  }

  .login-form > div:not(:first-child) {
    margin: 25px 0;
  }

  .login-form > div:last-child > div {
    margin: 0 auto;
  }

  .login-form {
    margin: 0;
  }

  .browser-login-submit {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

</style>
