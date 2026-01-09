<template>
  <div class="sidebar" :class="{ 'sidebar-dark': isAdminDark }">
    <div @click="collapse()" class="collapse-btn" :class="{ 'collapse-dark': isAdminDark }">
      <i class="el-icon-menu" style="margin: 14px;font-size: 17px"></i>
      <div style="font-size: 15px;margin-top: 13px">折叠</div>
    </div>
    <el-menu class="sidebar-el-menu"
             :background-color="isAdminDark ? '#2d2d2d' : '#ebf1f6'"
             :text-color="isAdminDark ? '#b0b0b0' : '#606266'"
             active-text-color="#20a0ff"
             unique-opened
             :default-active="$router.currentRoute.path"
             router>
      <template v-for="item in items">
        <template v-if="hasPermission(item)">
          <template v-if="item.subs">
            <el-submenu :index="item.index" :key="item.index">
              <template slot="title">
                <i :class="item.icon"></i>
                <span>{{ item.title }}</span>
              </template>
              <template v-for="subItem in item.subs">
                <el-submenu v-if="subItem.subs" :index="subItem.index" :key="subItem.index">
                  <template slot="title">
                    {{ subItem.title }}
                  </template>
                  <el-menu-item v-for="threeItem in subItem.subs" :key="threeItem.index" :index="threeItem.index">
                    {{ threeItem.title }}
                  </el-menu-item>
                </el-submenu>
                <el-menu-item v-else :index="subItem.index" :key="'item-'+subItem.index">
                  {{ subItem.title }}
                </el-menu-item>
              </template>
            </el-submenu>
          </template>
          <template v-else>
            <el-menu-item :index="item.index" :key="item.index" @click="item.title === 'SEO优化' ? goToSeoConfig() : null">
              <i :class="item.icon"></i>
              {{ item.title }}
            </el-menu-item>
          </template>
        </template>
      </template>
    </el-menu>
  </div>
</template>

<script>
    import { useMainStore } from '@/stores/main';

export default {
    props: {
      isAdminDark: {
        type: Boolean,
        default: false
      }
    },
    
    data() {
      return {
        isCollapse: true,
        items: [{
          icon: "el-icon-s-home",
          index: "/main",
          title: "系统首页",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-s-tools",
          index: "/webEdit",
          title: "网站设置",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-user-solid",
          index: "/userList",
          title: "用户管理",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-postcard",
          index: "/postList",
          title: "文章管理",
          requiredUserType: 1  // 管理员及以上可访问
        }, {
          icon: "el-icon-s-unfold",
          index: "/translationModel",
          title: "文章AI助手",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-notebook-2",
          index: "/sortList",
          title: "分类管理",
          requiredUserType: 1  // 管理员及以上可访问
        }, {
          icon: "el-icon-notebook-1",
          index: "/configList",
          title: "配置管理",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-s-operation",
          index: "/pluginManager",
          title: "插件管理",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-edit-outline",
          index: "/commentList",
          title: "评论管理",
          requiredUserType: 1  // 管理员及以上可访问
        }, {
          icon: "el-icon-s-comment",
          index: "/treeHoleList",
          title: "留言管理",
          requiredUserType: 1  // 管理员及以上可访问
        }, {
          icon: "el-icon-paperclip",
          index: "/resourceList",
          title: "资源管理",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-bank-card",
          index: "/resourcePathList",
          title: "资源聚合",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-sugar",
          index: "/loveList",
          title: "表白墙",
          requiredUserType: 0  // 仅站长可访问
        }, {
          icon: "el-icon-search",
          index: "/seoConfig",
          title: "SEO优化",
          requiredUserType: 0,  // 仅站长可访问
          click: function() {
            this.goToSeoConfig();
          }
        }]
      }
    },

    computed: {
      mainStore() {
        return useMainStore();
      },
      // 响应式获取当前管理员信息
      currentAdmin() {
        return this.mainStore.currentAdmin;
      },

      // 响应式获取isBoss状态
      isBoss() {
        return this.currentAdmin.isBoss;
      },

      // 响应式获取用户类型
      userType() {
        return this.currentAdmin.userType;
      }
    },

    watch: {
      // 监听管理员信息变化，确保权限实时更新
      currentAdmin: {
        handler(newAdmin) {
        },
        deep: true
      }
    },

    created() {

    },

    mounted() {

    },

    methods: {
      // 权限判断方法
      hasPermission(item) {
        // 如果没有设置权限要求，默认允许访问
        if (item.requiredUserType === undefined) {
          return true;
        }

        // 基于userType的权限验证
        // userType越小权限越高：0(站长) > 1(管理员) > 2(普通用户)
        const hasUserTypePermission = this.userType <= item.requiredUserType;


        return hasUserTypePermission;
      },

      collapse() {
        const sidebarElements = document.querySelectorAll('.sidebar');
        const contentBoxElements = document.querySelectorAll('.content-box');
        
        if (this.isCollapse) {
          sidebarElements.forEach(element => {
            element.style.width = '45px';
          });
          contentBoxElements.forEach(element => {
            element.style.left = '45px';
          });
        } else {
          sidebarElements.forEach(element => {
            element.style.width = '130px';
          });
          contentBoxElements.forEach(element => {
            element.style.left = '130px';
          });
        }
        this.isCollapse = !this.isCollapse;
      },
      
      goToSeoConfig() {
        this.$router.push('/seoConfig').catch(err => {
          this.$router.push({path: '/admin/seoConfig'}).catch(e => {
            console.error('嵌套路由也失败:', e);
          });
        });
      }
    }
  }
</script>

<style scoped>

  .sidebar {
    display: block;
    position: absolute;
    left: 0;
    top: 70px;
    bottom: 0;
    overflow-y: scroll;
    width: 130px;
    user-select: none;
  }

  .sidebar::-webkit-scrollbar {
    width: 0;
  }

  .sidebar > ul {
    height: 100%;
  }

  .sidebar-el-menu .el-menu-item {
    padding: 0 10px !important;
  }
  
  /* 折叠按钮样式 */
  .collapse-btn {
    color: rgb(96, 98, 102);
    cursor: pointer;
    background-color: #ebf1f6;
    display: flex;
    transition: all 0.3s ease;
  }
  
  /* ========== 深色模式下的sidebar样式 ========== */
  .sidebar-dark {
    background-color: #2d2d2d;
  }
  
  .collapse-dark {
    background-color: #2d2d2d !important;
    color: #b0b0b0 !important;
  }
</style>
