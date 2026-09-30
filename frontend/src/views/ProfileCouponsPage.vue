<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCoupons, type CouponStatus, type CouponView } from '@/api/coupon'

/**
 * 优惠券（/profile/coupons，M1-6 批次 4）：
 * 自 ProfilePage Tab 迁移而来，复用原卡片样式与数据源
 * （GET /api/user/coupons?status=，切换状态时按服务端过滤重新拉取）。
 * 未使用券带琥珀边框与"去使用"；已使用/已过期灰度可见。
 */
const FILTERS = [
  { key: 'UNUSED', label: '未使用' },
  { key: 'USED', label: '已使用' },
  { key: 'EXPIRED', label: '已过期' },
] as const

const activeFilter = ref<CouponStatus>('UNUSED')
const coupons = ref<CouponView[]>([])
const loading = ref(true)

const COUPON_STATUS_LABEL = { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }

async function loadCoupons(status: CouponStatus) {
  loading.value = true
  try {
    coupons.value = await getCoupons(status)
  } catch {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => loadCoupons(activeFilter.value))

function switchFilter(status: CouponStatus) {
  activeFilter.value = status
  loadCoupons(status)
}

function thresholdText(threshold: number | null): string {
  return threshold && threshold > 0 ? `满${threshold}可用` : '无门槛'
}

function onUseCoupon() {
  ElMessage.info('下单时自动抵扣，去菜单挑选饮品')
}
</script>

<template>
  <div class="coupons">
    <header class="coupons__head">
      <RouterLink to="/profile" class="coupons__back" aria-label="返回个人中心">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回个人中心
      </RouterLink>
    </header>

    <!-- 子 Tab：未使用 / 已使用 / 已过期 -->
    <div class="coupons__filters" role="tablist" aria-label="优惠券状态">
      <button
        v-for="filter in FILTERS"
        :key="filter.key"
        type="button"
        class="coupons__filter"
        :class="{ 'is-active': activeFilter === filter.key }"
        @click="switchFilter(filter.key)"
      >
        {{ filter.label }}
      </button>
    </div>

    <!-- 券列表 -->
    <section class="coupons__body">
      <template v-if="loading">
        <div v-for="n in 3" :key="n" class="skeleton skeleton--row"></div>
      </template>

      <template v-else>
        <div class="coupons__list">
          <article
            v-for="coupon in coupons"
            :key="coupon.id"
            class="coupon"
            :class="{ 'is-dim': coupon.status !== 'UNUSED' }"
          >
            <div class="coupon__face">
              <span class="coupon__value"><i>¥</i>{{ coupon.discountAmount ?? '--' }}</span>
              <span class="coupon__threshold">{{ thresholdText(coupon.thresholdAmount) }}</span>
            </div>
            <div class="coupon__info">
              <span class="coupon__name">{{ coupon.name }}</span>
              <span class="coupon__expire">有效期至 {{ coupon.expireAt?.slice(0, 10) }}</span>
            </div>
            <el-button
              v-if="coupon.status === 'UNUSED'"
              class="el-button--cta coupon__use"
              size="small"
              @click="onUseCoupon"
            >
              去使用
            </el-button>
            <span v-else class="coupon__status">{{ COUPON_STATUS_LABEL[coupon.status] }}</span>
          </article>

          <p v-if="coupons.length === 0" class="coupons__empty">
            {{ COUPON_STATUS_LABEL[activeFilter] }}的券暂无记录
          </p>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.coupons {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: calc(var(--ac-tabbar-h) + var(--ac-space-6));
}

.coupons__head {
  margin-bottom: var(--ac-space-4);
}

.coupons__back {
  display: inline-flex;
  align-items: center;
  gap: var(--ac-space-1);
  font-size: 14px;
  color: var(--ac-text-dim);
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover {
    color: var(--ac-primary);
  }
}

/* 子 Tab：未使用 / 已使用 / 已过期 */
.coupons__filters {
  display: flex;
  gap: var(--ac-space-2);
  margin-bottom: var(--ac-space-4);
}

.coupons__filter {
  padding: 8px var(--ac-space-5);
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  cursor: pointer;
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);

  &.is-active {
    color: var(--ac-primary-deep);
    font-weight: 600;
    background: var(--ac-tide);
    border-color: transparent;
  }
}

.coupons__body {
  min-height: 240px;
}

.coupons__list {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-3);
}

/* 券卡：可用券琥珀边框 + 琥珀面额块；历史灰度可见 */
.coupon {
  display: flex;
  align-items: center;
  gap: var(--ac-space-4);
  padding: var(--ac-space-3) var(--ac-space-4);
  background: var(--ac-card);
  border: 1.5px solid var(--ac-cta);
  border-radius: var(--ac-radius-card);

  &.is-dim {
    opacity: 0.65;
    border-color: var(--ac-border);
  }
}

.coupon__face {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  padding: var(--ac-space-2) 0;
  background: var(--ac-cta);
  border-radius: 12px;
}

.coupon.is-dim .coupon__face {
  background: var(--ac-border);
}

.coupon__value {
  font-family: var(--ac-font-display);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  color: #fff;

  i {
    margin-right: 1px;
    font-size: 13px;
    font-style: normal;
    font-weight: 500;
  }
}

.coupon__threshold {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.85);
}

.coupon.is-dim .coupon__threshold {
  color: var(--ac-text-dim);
}

.coupon__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.coupon__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.coupon__expire {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.coupon__use {
  flex: none;
}

.coupon__status {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.coupons__empty {
  margin: var(--ac-space-6) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

/* 骨架屏 */
.skeleton {
  background: linear-gradient(90deg, #eef4f8 25%, #f7fafc 37%, #eef4f8 63%);
  background-size: 400% 100%;
  border-radius: var(--ac-radius-btn);
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.skeleton--row {
  height: 64px;
  margin: var(--ac-space-3) 0;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: 0 0;
  }
}
</style>
