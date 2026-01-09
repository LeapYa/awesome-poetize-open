import { createRouter, createWebHistory } from 'vue-router'
import { useMainStore } from '../stores/main'
import constant from '../utils/constant'
import common from '../utils/common'
import {
  handleTokenExpire,
  isLoggedIn,
  getValidToken,
} from '../utils/tokenExpireHandler'

const routes = [
  {
    path: '/',
    component: () => import('../components/home'),
    children: [
      {
        path: '/',
        name: 'index',
        component: () => import('../components/index'),
      },
      {
        path: '/sort',
        name: 'sort',
        component: () => import('../components/sort'),
      },
      {
        path: '/sort/:id',
        name: 'sort-category',
        component: () => import('../components/sort'),
      },
      {
        path: '/article/:lang/:id',
        name: 'article-translated',
        component: () => import('../components/article'),
      },
      {
        path: '/article/:id',
        name: 'article',
        component: () => import('../components/article'),
      },
      {
        path: '/weiYan',
        name: 'weiYan',
        component: () => import('../components/weiYan'),
      },
      {
        path: '/love',
        name: 'love',
        component: () => import('../components/love'),
      },
      {
        path: '/favorite',
        name: 'favorite',
        component: () => import('../components/favorite'),
      },
      {
        path: '/friends',
        name: 'friends',
        component: () => import('../components/FriendLinks'),
      },
      {
        path: '/music',
        name: 'music',
        component: () => import('../components/Music'),
      },
      {
        path: '/favorites',
        name: 'favorites',
        component: () => import('../components/Favorites'),
      },
      {
        path: '/travel',
        name: 'travel',
        component: () => import('../components/travel'),
      },
      {
        path: '/message',
        name: 'message',
        component: () => import('../components/message'),
      },
      {
        path: '/about',
        name: 'about',
        component: () => import('../components/about'),
      },
      {
        path: '/user',
        name: 'user',
        component: () => import('../components/user'),
      },
      {
        path: '/oauth-callback',
        name: 'oauth-callback',
        component: () => import('../components/oauth-callback'),
      },
      {
        path: '/letter',
        name: 'letter',
        component: () => import('../components/letter'),
      },
      {
        path: '/privacy',
        name: 'privacy',
        component: () => import('../views/Privacy'),
      },
    ],
  },
  {
    path: '/verify',
    redirect: (to) => {
      const redirect = to.query.redirect
      const query = { fromVerify: 'true' }
      if (redirect) {
        query.redirect = redirect
      }
      return {
        path: '/user',
        query: query,
      }
    },
  },
  {
    path: '/im',
    name: 'im',
    meta: { requireAuth: true },
    component: () => import('../components/im/index'),
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('../components/Forbidden'),
  },
  {
    path: '/404',
    name: 'notFound',
    component: () => import('../components/NotFound'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'catchAll',
    component: () => import('../components/NotFound'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: routes,
  scrollBehavior(to, from, savedPosition) {
    return { left: 0, top: 0 }
  },
})

router.beforeEach(async (to, from, next) => {
  if (to.query.redirect === '403') {
    next('/403')
    return
  }

  const publicPaths = [
    '/user',
    '/verify',
    '/403',
    '/404',
    '/',
    '/about',
    '/privacy',
  ]
  const isPublicPath =
    publicPaths.includes(to.path) ||
    to.path.startsWith('/article/') ||
    to.path.startsWith('/sort/')

  if (!isPublicPath) {
    const needsAdminAuth = to.matched.some((record) => record.meta.isAdmin)

    if (needsAdminAuth) {
      const adminToken = getValidToken(true)
      const isAdminLoggedIn = isLoggedIn(true)

      if (!adminToken || !isAdminLoggedIn) {
        handleTokenExpire(true, to.fullPath, { showMessage: false })
        return
      }
    } else {
      const needsAuth = to.matched.some((record) => record.meta.requireAuth)

      if (needsAuth) {
        const userToken = getValidToken(false)
        const isUserLoggedIn = isLoggedIn(false)

        if (!userToken || !isUserLoggedIn) {
          handleTokenExpire(false, to.fullPath, { showMessage: false })
          return
        }
      }
    }
  }

  if (to.query.userToken) {
    await handleOAuthToken(to, from, next)
    return
  }

  next()
})

async function handleOAuthToken(to, from, next) {
  const userToken = to.query.userToken
  const emailCollectionNeeded = to.query.emailCollectionNeeded === 'true'
  const baseURL = constant.baseURL

  try {
    const encryptedToken = await common.encrypt(userToken)

    const response = await fetch(baseURL + '/user/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: 'userToken=' + encryptedToken,
    })

    if (response.ok) {
      const result = await response.json()
      if (result && result.code === 200) {
        const needsEmailCollection =
          emailCollectionNeeded || result.message === 'EMAIL_COLLECTION_NEEDED'

        if (needsEmailCollection) {
          const provider =
            result.data.platformType || to.query.provider || 'unknown'
          const tempUserData = {
            ...result.data,
            needsEmailCollection: true,
            provider: provider,
          }

          localStorage.setItem('userToken', result.data.accessToken)
          localStorage.setItem('adminToken', result.data.accessToken)
          localStorage.setItem('tempUserData', JSON.stringify(tempUserData))

          const redirectPath =
            to.query.redirect ||
            from.query.redirect ||
            sessionStorage.getItem('oauthRedirectPath') ||
            '/'

          next({
            path: redirectPath,
            query: { showEmailCollection: 'true' },
            replace: true,
          })
          return
        }

        localStorage.removeItem('currentAdmin')
        localStorage.removeItem('currentUser')

        localStorage.setItem('userToken', result.data.accessToken)
        localStorage.setItem('adminToken', result.data.accessToken)
        const mainStore = useMainStore()
        mainStore.loadCurrentUser(result.data)
        mainStore.loadCurrentAdmin(result.data)

        const redirectPath =
          to.query.redirect ||
          from.query.redirect ||
          sessionStorage.getItem('oauthRedirectPath') ||
          '/'

        next({
          path: redirectPath,
          query: { ...to.query, token: undefined, state: undefined },
          replace: true,
        })
        return
      } else {
        console.error('OAuth token验证失败:', result)
        next({ path: to.path, query: {}, replace: true })
        return
      }
    } else {
      console.error('OAuth token验证HTTP错误:', response.status)
      next({ path: to.path, query: {}, replace: true })
      return
    }
  } catch (error) {
    console.error('OAuth token验证异常:', error)
    next({ path: to.path, query: {}, replace: true })
    return
  }
}

export default router
