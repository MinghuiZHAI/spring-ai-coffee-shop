import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由（M1-6 批次 3）：AppLayout 为用户端壳；/login 公开；/admin/kb 为 ADMIN 专属
 * （登录后按 role 跳转）。登录守卫：未登录跳 /login；已登录访问 /login 回首页；
 * 登录后首次导航拉取 profile 回填 userInfo（401 时 client 已清 token 跳登录）。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginPage.vue'), meta: { title: '登录' } },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/menu' },
        {
          path: 'menu',
          name: 'menu',
          component: () => import('@/views/MenuPage.vue'),
          meta: { title: '菜单' },
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('@/views/CartPage.vue'),
          meta: { title: '购物车' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrdersPage.vue'),
          meta: { title: '订单' },
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/OrderDetailPage.vue'),
          meta: { title: '订单详情' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfilePage.vue'),
          meta: { title: '我的' },
        },
        {
          path: 'profile/info',
          name: 'profile-info',
          component: () => import('@/views/ProfileInfoPage.vue'),
          meta: { title: '个人资料' },
        },
        {
          path: 'profile/points',
          name: 'profile-points',
          component: () => import('@/views/ProfilePointsPage.vue'),
          meta: { title: '积分详情' },
        },
        {
          path: 'profile/coupons',
          name: 'profile-coupons',
          component: () => import('@/views/ProfileCouponsPage.vue'),
          meta: { title: '优惠券' },
        },
        {
          path: 'profile/info',
          name: 'profile-info',
          component: () => import('@/views/ProfileInfoPage.vue'),
          meta: { title: '个人资料' },
        },
        {
          path: 'wallet/recharge',
          name: 'wallet-recharge',
          component: () => import('@/views/WalletRechargePage.vue'),
          meta: { title: '余额充值' },
        },
        {
          path: 'merchant',
          name: 'merchant',
          component: () => import('@/views/MerchantPage.vue'),
          meta: { title: '我是商家' },
        },
      ],
    },
    {
      path: '/admin/kb',
      name: 'admin-kb',
      component: () => import('@/views/admin/KbAdminPage.vue'),
      meta: { title: '知识库管理后台', roles: ['ADMIN'] },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · Atlantic Coffee` : 'Atlantic Coffee · 大西洋咖啡'
})

/** 登录守卫：公开页仅 /login；/admin/kb 额外要求 ADMIN 角色 */
router.beforeEach(async (to) => {
  const store = useUserStore()

  if (to.path === '/login') {
    return store.isLoggedIn ? { path: '/' } : true
  }
  if (!store.isLoggedIn) {
    return { path: '/login' }
  }
  if (!store.userInfo) {
    try {
      await store.fetchProfile()
    } catch {
      // 401 时 client 已清 token 并跳登录；此处兜底防死循环
      return { path: '/login' }
    }
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && !roles.includes(store.userInfo?.role ?? '')) {
    return { path: '/' }
  }
  return true
})

export default router
