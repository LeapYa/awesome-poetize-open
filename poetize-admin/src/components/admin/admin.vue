<template>
  <div :class="{ 'admin-dark-mode': isAdminDark }">
    <myHeader :isAdminDark="isAdminDark" @toggle-theme="toggleAdminTheme"></myHeader>
    <sidebar :isAdminDark="isAdminDark"></sidebar>
    <div class="content-box">
      <div class="content">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script>
    import { useMainStore } from '@/stores/main';

import myHeader from "./common/myHeader.vue";
  import sidebar from "./common/sidebar.vue";

  export default {
    components: {
      myHeader,
      sidebar
    },

    data() {
      return {
        isAdminDark: false
      }
    },

    computed: {
      mainStore() {
        return useMainStore();
      },},

    watch: {
      '$route'(to, from) {
      }
    },

    created() {
      // 延迟获取系统配置，确保mainStore已经初始化
      this.$nextTick(() => {
        let sysConfig = this.mainStore.sysConfig;
        if (!this.$common.isEmpty(sysConfig) && !this.$common.isEmpty(sysConfig['webStaticResourcePrefix'])) {
          let root = document.querySelector(":root");
          let webStaticResourcePrefix = sysConfig['webStaticResourcePrefix'];
          root.style.setProperty("--backgroundPicture", "url(" + webStaticResourcePrefix + "assets/backgroundPicture.jpg)");
        }
        this.getWebsitConfig();
      });
    },

    mounted() {
      
      // 初始化后台暗色模式
      this.initAdminTheme();
      
      // 监听系统暗色模式变化
      this.setupAdminThemeListener();
    },

    methods: {
      // 初始化后台主题
      initAdminTheme() {
        try {
          
          // 使用与前台共享的theme键
          const theme = localStorage.getItem('theme');
          
          if (theme === 'dark') {
            this.isAdminDark = true;
          } else if (theme === 'light') {
            this.isAdminDark = false;
          } else {
            // 用户未设置，检查系统偏好
            const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
            this.isAdminDark = prefersDark;
          }
          
          // 统一应用主题：在 body 上添加/移除 dark-mode 类（与前台保持一致）
          this.applyThemeToBody();
          
          
          // 触发全局事件，通知所有组件当前主题
          this.$nextTick(() => {
            this.$root.$emit('theme-changed', this.isAdminDark);
          });
        } catch (error) {
          console.error('初始化后台主题失败:', error);
        }
      },
      
      // 监听系统暗色模式变化（与前台共享）
      setupAdminThemeListener() {
        if (!window.matchMedia) return;
        
        const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');
        
        const handleThemeChange = (e) => {
          // 使用与前台共享的theme键
          const theme = localStorage.getItem('theme');
          
          // 只有在用户未手动设置时才自动切换
          if (!theme) {
            this.isAdminDark = e.matches;
            // 统一应用主题到 body
            this.applyThemeToBody();
            // 触发全局事件
            this.$root.$emit('theme-changed', this.isAdminDark);
          }
        };
        
        if (darkModeQuery.addEventListener) {
          darkModeQuery.addEventListener('change', handleThemeChange);
        } else if (darkModeQuery.addListener) {
          darkModeQuery.addListener(handleThemeChange);
        }
      },
      
      // 切换后台主题（与前台共享）
      toggleAdminTheme() {
        this.isAdminDark = !this.isAdminDark;
        
        // 保存到与前台共享的theme键
        localStorage.setItem('theme', this.isAdminDark ? 'dark' : 'light');
        
        // 统一应用主题到 body（与前台保持一致）
        this.applyThemeToBody();
        
        
        // 触发全局事件，通知所有组件主题已切换
        this.$root.$emit('theme-changed', this.isAdminDark);
      },
      
      // 应用主题到 body（统一前台和后台的实现）
      applyThemeToBody() {
        if (this.isAdminDark) {
          // 暗色模式：添加 dark-mode 类到 body 和 html（与前台一致）
          document.body.classList.add('dark-mode');
          document.documentElement.classList.add('dark-mode');
        } else {
          // 亮色模式：移除 dark-mode 类（与前台一致）
          document.body.classList.remove('dark-mode');
          document.documentElement.classList.remove('dark-mode');
        }
      },
      
      getWebsitConfig() {
        // 获取网站配置信息
        this.getWebInfo();
        this.getSysConfig();
      },
      
      getWebInfo() {
        this.$http.get(this.$constant.baseURL + "/webInfo/getWebInfo")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.mainStore.loadWebInfo( res.data);
            }
          })
          .catch((error) => {
            console.error("获取网站信息失败:", error);
          });
      },
      
      getSysConfig() {
        this.$http.get(this.$constant.baseURL + "/sysConfig/listSysConfig")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.mainStore.loadSysConfig( res.data);
            }
          })
          .catch((error) => {
            console.error("获取系统配置失败:", error);
          });
      },
      loadFont() {
        
      }
    }
  }
</script>

<style scoped>

  .content-box {
    position: absolute;
    left: 130px;
    right: 0;
    top: 70px;
    bottom: 0;
    transition: left .3s ease-in-out;
  }

  .content {
    width: auto;
    height: 100%;
    padding: 30px;
    overflow-y: scroll;
    background-color: #f5f7fa; /* 亮色模式默认背景色 */
    transition: background-color 0.3s ease;
  }
  
  /* ========== 后台深色模式样式 ========== */
  .admin-dark-mode .content {
    background-color: #1e1e1e;
  }

</style>
