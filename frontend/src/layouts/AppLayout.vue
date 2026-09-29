<script setup lang="ts">
import { useRoute } from 'vue-router'
import { CART_LINES } from '@/data/cart'

/**
 * 全局壳（M1-6 步骤 1）：
 * PC（≥768px）固定玻璃导航 + 内容区居中；移动端精简顶栏 + 底部 Tab 栏。
 * 洋流母题：导航下缘渐变线 + 激活态短弧线 + 品牌波浪徽标，全站贯穿。
 */
const route = useRoute()

/** 主导航数据：PC 顶部链接与移动端底部 Tab 共用（≤5 项，导航层级原则） */
const NAV_ITEMS = [
  { name: 'menu', label: '菜单', to: '/menu' },
  { name: 'orders', label: '订单', to: '/orders' },
  { name: 'profile', label: '我的', to: '/profile' },
] as const

/** 购物车角标（静态假数据，接后端后由购物车 store 驱动） */
const cartCount = CART_LINES.reduce((sum, line) => sum + line.quantity, 0)
</script>

<template>
  <div class="app-shell">
    <!-- 顶部：PC 完整导航 / 移动端精简品牌条（同一元素，CSS 响应式） -->
    <header class="topnav">
      <div class="topnav__inner">
        <RouterLink to="/menu" class="brand" aria-label="Atlantic Coffee 首页">
          <svg class="brand__mark" width="28" height="28" viewBox="0 0 32 32" aria-hidden="true">
            <circle cx="16" cy="16" r="16" fill="var(--ac-primary)" />
            <path
              d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0"
              fill="none"
              stroke="#fff"
              stroke-width="2.2"
              stroke-linecap="round"
              opacity=".95"
            />
            <path
              d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0"
              fill="none"
              stroke="#7fd1f5"
              stroke-width="2"
              stroke-linecap="round"
              opacity=".9"
            />
          </svg>
          <span class="brand__name">Atlantic Coffee</span>
          <span class="brand__sub">大西洋咖啡</span>
        </RouterLink>

        <nav class="topnav__links" aria-label="主导航">
          <RouterLink
            v-for="item in NAV_ITEMS"
            :key="item.name"
            :to="item.to"
            class="topnav__link"
            :class="{ 'is-active': route.name === item.name }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <RouterLink to="/cart" class="topnav__cart" aria-label="购物车">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="8" cy="21" r="1" />
            <circle cx="19" cy="21" r="1" />
            <path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12" />
          </svg>
          <span v-if="cartCount > 0" class="topnav__cart-badge">{{ cartCount }}</span>
        </RouterLink>

        <RouterLink to="/profile" class="topnav__avatar" aria-label="个人中心">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
            <circle cx="12" cy="8" r="4.5" />
            <path d="M19.5 21a7.5 7.5 0 0 0-15 0" />
          </svg>
        </RouterLink>
      </div>
      <!-- 洋流弧线：导航下缘渐变线，两端隐入海雾 -->
      <div class="topnav__current" aria-hidden="true"></div>
    </header>

    <!-- 内容区：PC 居中 1200px，移动端为底部 Tab 预留高度 -->
    <main class="app-main">
      <RouterView />
    </main>

    <!-- 移动端底部 Tab（<768px 显示） -->
    <nav class="tabbar" aria-label="底部导航">
      <RouterLink
        v-for="item in NAV_ITEMS"
        :key="item.name"
        :to="item.to"
        class="tabbar__item"
        :class="{ 'is-active': route.name === item.name }"
      >
        <span class="tabbar__arc" aria-hidden="true"></span>

        <svg
          v-if="item.name === 'menu'"
          class="tabbar__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M17 8h1a4 4 0 1 1 0 8h-1" />
          <path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z" />
          <path d="M6 2v2" />
          <path d="M10 2v2" />
          <path d="M14 2v2" />
        </svg>

        <svg
          v-else-if="item.name === 'orders'"
          class="tabbar__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z" />
          <path d="M16 8H8" />
          <path d="M16 12H8" />
          <path d="M13 16H8" />
        </svg>

        <svg
          v-else
          class="tabbar__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="8" r="4.5" />
          <path d="M19.5 21a7.5 7.5 0 0 0-15 0" />
        </svg>

        <span class="tabbar__label">{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航（PC 玻璃 / 移动端精简条） ===== */
.topnav {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 100;
  background: var(--ac-glass-bg);
  backdrop-filter: var(--ac-glass-blur);
  -webkit-backdrop-filter: var(--ac-glass-blur);
}

.topnav__inner {
  display: flex;
  align-items: center;
  gap: var(--ac-space-8);
  height: var(--ac-nav-h-mobile);
  padding: 0 var(--ac-space-4);
}

/* 洋流弧线：下缘渐变线，两端隐入背景 */
.topnav__current {
  position: absolute;
  inset: auto 0 0 0;
  height: 2px;
  background: var(--ac-current-gradient);
  opacity: 0.9;
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--ac-space-2);
  margin-right: auto;
}

.brand__mark {
  flex: none;
}

.brand__name {
  font-family: var(--ac-font-display);
  font-weight: 600;
  font-size: 16px;
  color: var(--ac-primary-deep);
  letter-spacing: 0.2px;
}

.brand__sub {
  font-size: 12px;
  color: var(--ac-text-dim);
  border-left: 1px solid var(--ac-border);
  padding-left: var(--ac-space-2);
}

.topnav__links {
  display: none;
  align-items: center;
  gap: var(--ac-space-6);
}

.topnav__link {
  position: relative;
  padding: var(--ac-space-2) var(--ac-space-1);
  font-size: 15px;
  font-weight: 500;
  color: var(--ac-text-dim);
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  /* 激活态：洋流短弧线（渐变小胶囊），悬停显现浅潮底 */
  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: -4px;
    width: 24px;
    height: 3px;
    border-radius: var(--ac-radius-pill);
    background: var(--ac-current-gradient);
    transform: translateX(-50%) scaleX(0);
    transition: transform var(--ac-dur-base) var(--ac-ease-enter);
  }

  &.is-active {
    color: var(--ac-primary-deep);

    &::after {
      transform: translateX(-50%) scaleX(1);
    }
  }

  &:hover:not(.is-active) {
    color: var(--ac-primary);
  }
}

.topnav__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--ac-radius-pill);
  color: var(--ac-text-dim);
  background: var(--ac-tide);
  transition:
    color var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  &:hover {
    color: var(--ac-primary);
    box-shadow: var(--ac-shadow-sm);
  }
}

.topnav__cart {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: var(--ac-space-2);
  color: var(--ac-text-dim);
  border-radius: var(--ac-radius-pill);
  transition:
    color var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  svg {
    width: 20px;
    height: 20px;
  }

  &:hover {
    color: var(--ac-primary);
    box-shadow: var(--ac-shadow-sm);
  }
}

.topnav__cart-badge {
  position: absolute;
  top: -3px;
  right: -5px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-family: var(--ac-font-display);
  font-size: 10px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 16px;
  color: #fff;
  text-align: center;
  background: var(--ac-cta);
  border-radius: var(--ac-radius-pill);
}

/* ===== 内容区 ===== */
.app-main {
  flex: 1;
  width: 100%;
  max-width: var(--ac-content-max);
  margin: 0 auto;
  padding: calc(var(--ac-nav-h-mobile) + var(--ac-space-4)) var(--ac-space-4)
    var(--ac-space-8);
}

/* ===== 移动端底部 Tab ===== */
.tabbar {
  position: fixed;
  inset: auto 0 0 0;
  z-index: 100;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  height: calc(var(--ac-tabbar-h) + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--ac-card);
  border-top: 1px solid var(--ac-border);
}

.tabbar__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--ac-text-dim);
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  /* 激活态：图标上方洋流短弧 */
  .tabbar__arc {
    position: absolute;
    top: 6px;
    width: 20px;
    height: 3px;
    border-radius: var(--ac-radius-pill);
    background: var(--ac-current-gradient);
    opacity: 0;
    transform: scaleX(0.4);
    transition:
      opacity var(--ac-dur-base) var(--ac-ease-enter),
      transform var(--ac-dur-base) var(--ac-ease-enter);
  }

  .tabbar__icon {
    width: 22px;
    height: 22px;
    margin-top: 6px;
  }

  .tabbar__label {
    font-size: 11px;
    line-height: 1;
  }

  &.is-active {
    color: var(--ac-primary);

    .tabbar__arc {
      opacity: 1;
      transform: scaleX(1);
    }
  }
}

/* ===== 响应式：≥768px 切换为 PC 形态 ===== */
@media (min-width: 768px) {
  .topnav__inner {
    height: var(--ac-nav-h);
    max-width: var(--ac-content-max);
    margin: 0 auto;
    padding: 0 var(--ac-space-6);
  }

  .brand__sub {
    display: inline;
  }

  .topnav__links {
    display: flex;
  }

  .topnav__avatar {
    margin-left: auto;
  }

  .brand {
    margin-right: 0;
  }

  .app-main {
    padding: calc(var(--ac-nav-h) + var(--ac-space-6)) var(--ac-space-6) var(--ac-space-10);
  }

  .tabbar {
    display: none;
  }
}
</style>
