import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由骨架（M1-6 步骤 1）：AppLayout 为全局壳，三个一级页面均为占位，
 * 逐步填充（菜单 → 购物车/订单 → 个人中心 → 知识库后台 → ChatWidget）。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
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
      ],
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

export default router
