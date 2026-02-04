import Vue from 'vue'
import VueRouter from 'vue-router'
import { useMainStore } from '../stores/main'
import constant from '../utils/constant'
import common from '../utils/common'
import { handleTokenExpire, isLoggedIn, getValidToken } from '../utils/tokenExpireHandler'

const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err);
}

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/welcome',
    meta: { requiresAuth: true },
    component: () => import('../components/admin/admin'),
    children: [{
      path: 'welcome',
      name: 'welcome',
      component: () => import('../components/admin/welcome')
    }, {
      path: 'main',
      name: 'main',
      component: () => import('../components/admin/main')
    }, {
      path: 'webEdit',
      name: 'webEdit',
      component: () => import('../components/admin/webEdit')
    }, {
      path: 'userList',
      name: 'userList',
      component: () => import('../components/admin/userList')
    }, {
      path: 'postList',
      name: 'postList',
      component: () => import('../components/admin/postList')
    }, {
      path: 'postEdit',
      name: 'postEdit',
      component: () => import('../components/admin/postEdit')
    }, {
      path: 'sortList',
      name: 'sortList',
      component: () => import('../components/admin/sortList')
    }, {
      path: 'configList',
      name: 'configList',
      component: () => import('../components/admin/configList')
    }, {
      path: 'commentList',
      name: 'commentList',
      component: () => import('../components/admin/commentList')
    }, {
      path: 'treeHoleList',
      name: 'treeHoleList',
      component: () => import('../components/admin/treeHoleList')
    }, {
      path: 'resourceList',
      name: 'resourceList',
      component: () => import('../components/admin/resourceList')
    }, {
      path: 'loveList',
      name: 'loveList',
      component: () => import('../components/admin/loveList')
    }, {
      path: 'resourcePathList',
      name: 'resourcePathList',
      component: () => import('../components/admin/resourcePathList')
    }, {
      path: 'visitStats',
      name: 'visitStats',
      component: () => import('../components/admin/visitStats')
    }, {
      path: 'seoConfig',
      name: 'seoConfig',
      component: () => import('../components/admin/seoConfig')
    }, {
      path: 'aiChatConfig',
      name: 'aiChatConfig',
      component: () => import('../components/admin/aiChatConfig')
    }, {
      path: 'pluginManager',
      name: 'pluginManager',
      component: () => import('../components/admin/pluginManager')
    }, {
      path: 'translationModel',
      name: 'translationModel',
      component: () => import('../components/admin/translationModelManage.vue'),
      meta: {
        requireAuth: true,
        isAdmin: true
      }
    }]
  },
  // 仅在开发环境开启 /user 路由（生产环境请访问主站）
  ...(import.meta.env.DEV ? [{
    path: '/user',
    name: 'user',
    component: () => import('../components/user')
  }] : []),
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('../components/Forbidden')
  },
  {
    path: '/404',
    name: 'notFound',
    component: () => import('../components/NotFound')
  },
  {
    path: '*',
    name: 'catchAll',
    component: () => import('../components/NotFound')
  }
]

const router = new VueRouter({
  mode: "history",
  base: '/admin/',
  routes: routes,
  scrollBehavior(to, from, savedPosition) {
    return { x: 0, y: 0 }
  }
})

router.beforeEach(async (to, from, next) => {
  // 检查是否需要重定向到403页面
  if (to.query.redirect === '403') {
    next('/403');
    return;
  }

  // Token过期检查
  const publicPaths = ['/user', '/403', '/404'];
  const isPublicPath = publicPaths.includes(to.path);

  if (!isPublicPath) {
    // 检查管理员token
    const adminToken = getValidToken(true);
    const isAdminLoggedIn = isLoggedIn(true);

    if (!adminToken || !isAdminLoggedIn) {
      handleTokenExpire(true, to.fullPath, { showMessage: false });
      return;
    }
  }

  next();
})

export default router
