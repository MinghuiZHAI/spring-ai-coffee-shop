<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import StatusChip from '@/components/StatusChip.vue'
import { listOrders, payOrder, pickupOrder, cancelOrder, applyRefund, type OrderBrief } from '@/api/order'
import { formatTime, isStatus, statusMeta, type OrderStatus } from '@/utils/orderDisplay'

/**
 * 订单列表（批次 7 接真数据）：GET /api/user/orders（游标分页，status 服务端筛选）。
 * 状态筛选胶囊：全部/单状态走服务端参数；「售后」为三状态聚合（退款中/已退款/已取消），
 * 服务端无聚合参数——拉全量后在客户端过滤。订单卡操作（去支付/取消/取餐/退款）
 * 调真实接口，成功后重拉当前筛选视图。
 */
type FilterKey = 'ALL' | OrderStatus | 'AFTER_SALE'

const FILTERS: Array<{ key: FilterKey; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'PAID_TODO', label: '待制作' },
  { key: 'MAKING', label: '制作中' },
  { key: 'READY', label: '待取餐' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'AFTER_SALE', label: '售后' },
]

const AFTER_SALE_STATUSES: OrderStatus[] = ['REFUNDING', 'REFUNDED', 'CANCELLED']

const activeFilter = ref<FilterKey>('ALL')
const orders = ref<OrderBrief[]>([])
const cursor = ref<number | null>(null)
const exhausted = ref(false)
const loading = ref(false)

const isAfterSaleView = computed(() => activeFilter.value === 'AFTER_SALE')

const filteredOrders = computed(() =>
  isAfterSaleView.value
    ? orders.value.filter((o) => AFTER_SALE_STATUSES.some((s) => isStatus(o.status, s)))
    : orders.value,
)

async function fetchOrders(reset = true) {
  if (loading.value) return
  loading.value = true
  try {
    const status = activeFilter.value === 'ALL' || isAfterSaleView.value ? undefined : activeFilter.value
    const page = await listOrders(reset ? undefined : cursor.value ?? undefined, 10, status)
    orders.value = reset ? page.list : [...orders.value, ...page.list]
    cursor.value = page.nextCursor
    exhausted.value = page.nextCursor === null
  } finally {
    loading.value = false
  }
}

watch(activeFilter, () => fetchOrders(true))
fetchOrders(true)

/** 订单卡快捷操作：真实接口 + 成功后重拉当前视图（状态机冲突等错误由 ApiError 带出） */
async function onPay(order: OrderBrief) {
  await payOrder(order.id)
  ElMessageBox.alert(`订单 ${order.orderNo} 支付成功`, '支付完成', { confirmButtonText: '知道了' }).catch(() => {})
  await fetchOrders(true)
}

async function onPickup(order: OrderBrief) {
  await pickupOrder(order.id)
  await fetchOrders(true)
}

async function onCancel(order: OrderBrief) {
  const ok = await ElMessageBox.confirm(`取消订单 ${order.orderNo}？待制作订单取消后自动全额退款`, '取消订单', {
    type: 'warning',
    confirmButtonText: '取消订单',
    cancelButtonText: '再想想',
  }).then(() => true)
    .catch(() => false)
  if (!ok) return
  await cancelOrder(order.id)
  await fetchOrders(true)
}

async function onRefund(order: OrderBrief) {
  const ok = await ElMessageBox.confirm(`对订单 ${order.orderNo} 发起退款申请？`, '申请退款', {
    type: 'warning',
    confirmButtonText: '申请退款',
    cancelButtonText: '再想想',
  }).then(() => true)
    .catch(() => false)
  if (!ok) return
  await applyRefund(order.id)
  await fetchOrders(true)
}
</script>

<template>
  <div class="orders">
    <header class="orders__head">
      <h2 class="orders__title">订单</h2>
      <p class="orders__meta">{{ filteredOrders.length }} 单</p>
    </header>

    <!-- 状态筛选：全部固定最左，其余状态横向滚动 -->
    <nav class="orders__filters" aria-label="订单状态筛选">
      <button
        type="button"
        class="orders__filter"
        :class="{ 'is-active': activeFilter === 'ALL' }"
        @click="activeFilter = 'ALL'"
      >
        全部
      </button>
      <div class="orders__filters-scroll">
        <button
          v-for="filter in FILTERS.slice(1)"
          :key="filter.key"
          type="button"
          class="orders__filter"
          :class="{ 'is-active': activeFilter === filter.key }"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }}
        </button>
      </div>
    </nav>

    <!-- 订单卡列表 -->
    <div class="orders__list">
      <article
        v-for="(order, index) in filteredOrders"
        :key="order.id"
        class="order-card"
        :style="{ animationDelay: `${index * 40}ms` }"
      >
        <RouterLink
          class="order-card__main"
          :to="`/orders/${order.id}`"
          :aria-label="`查看订单 ${order.orderNo} 详情`"
        >
          <div class="order-card__row">
            <span class="order-card__no">{{ order.orderNo }}</span>
            <span class="order-card__time">{{ formatTime(order.createdAt) }}</span>
          </div>

          <div class="order-card__row order-card__row--body">
            <span class="order-card__summary">{{ order.itemSummary }}</span>
            <span class="order-card__amount">
              <i>¥</i>{{ order.payAmount }}
            </span>
          </div>

          <div class="order-card__row">
            <StatusChip
              :label="statusMeta(order.status).label"
              :tone="statusMeta(order.status).tone"
            />
            <span class="order-card__store">{{ order.storeName.replace('Atlantic Coffee·', '') }}</span>
          </div>
        </RouterLink>

        <div v-if="order.status !== 'CANCELLED' && order.status !== 'REFUNDED'" class="order-card__actions">
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            size="small"
            @click="onCancel(order)"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 'COMPLETED'"
            size="small"
            @click="onRefund(order)"
          >
            申请退款
          </el-button>
          <el-button
            v-if="order.status === 'READY'"
            size="small"
            @click="onPickup(order)"
          >
            确认取餐
          </el-button>
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            class="el-button--cta"
            size="small"
            @click="onPay(order)"
          >
            去支付
          </el-button>
        </div>
      </article>

      <p v-if="filteredOrders.length === 0 && !loading" class="orders__empty">该状态下暂无订单</p>
      <el-button
        v-if="!exhausted && filteredOrders.length > 0"
        class="orders__more"
        :loading="loading"
        @click="fetchOrders(false)"
      >
        加载更多
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.orders__head {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-3);
  margin-bottom: var(--ac-space-5);
}

.orders__title {
  font-size: 22px;
}

.orders__meta {
  margin: 0;
  font-size: 13px;
  color: var(--ac-text-dim);
}

/* 状态筛选：全部固定最左；其余在独立滚动区横滚（圆角矩形外观） */
.orders__filters {
  display: flex;
  align-items: center;
  gap: var(--ac-space-2);
  position: sticky;
  top: var(--ac-nav-h-mobile);
  z-index: 10;
  margin: calc(-1 * var(--ac-space-4)) calc(-1 * var(--ac-space-4)) var(--ac-space-5);
  padding: var(--ac-space-3) var(--ac-space-4);
  background: var(--ac-bg);
}

.orders__filters-scroll {
  display: flex;
  flex: 1;
  gap: var(--ac-space-2);
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.orders__filter {
  flex: none;
  padding: 8px var(--ac-space-4);
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-btn);
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

.orders__list {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-3);
}

.order-card {
  padding: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
  animation: order-card-in var(--ac-dur-base) var(--ac-ease-enter) backwards;
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-md);
  }
}

@keyframes order-card-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

.order-card__main {
  display: block;
}

.order-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ac-space-3);

  & + & {
    margin-top: var(--ac-space-2);
  }

  &--body {
    align-items: flex-end;
  }
}

.order-card__no {
  font-family: var(--ac-font-display);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
  color: var(--ac-text-dim);
}

.order-card__time {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.order-card__summary {
  font-size: 15px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

.order-card__amount {
  font-family: var(--ac-font-display);
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);

  i {
    margin-right: 1px;
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
    color: var(--ac-text-dim);
  }
}

.order-card__store {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.order-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--ac-space-2);
  margin-top: var(--ac-space-3);
  padding-top: var(--ac-space-3);
  border-top: 1px dashed var(--ac-border);
}

.orders__empty {
  margin: var(--ac-space-10) 0;
  text-align: center;
  font-size: 14px;
  color: var(--ac-text-dim);
}

.orders__more {
  display: block;
  margin: var(--ac-space-4) auto 0;
}

@media (min-width: 768px) {
  .orders__filters {
    position: static;
    margin: 0 0 var(--ac-space-5);
    padding: 0;
    overflow: visible;
  }
}
</style>
