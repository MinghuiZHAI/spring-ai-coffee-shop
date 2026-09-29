<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusChip from '@/components/StatusChip.vue'
import {
  ORDERS,
  ORDER_STATUS_META,
  formatTime,
  type OrderStatus,
  type OrderView,
} from '@/data/orders'

/**
 * 订单列表（M1-6 步骤 3b，静态假数据）：
 * 状态筛选胶囊（全部 + 主流程 + 售后）+ 订单卡（状态 chip/商品摘要/实付/按状态操作）。
 * 接后端后由 GET /api/user/orders?status= 替换（游标分页）。
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

const activeFilter = ref<FilterKey>('ALL')

const AFTER_SALE_STATUSES: OrderStatus[] = ['REFUNDING', 'REFUNDED', 'CANCELLED']

const filteredOrders = computed(() => {
  if (activeFilter.value === 'ALL') {
    return ORDERS
  }
  if (activeFilter.value === 'AFTER_SALE') {
    return ORDERS.filter((order) => AFTER_SALE_STATUSES.includes(order.status))
  }
  return ORDERS.filter((order) => order.status === activeFilter.value)
})

/** 商品摘要：首商品 ×数量，多件追加"等 N 件" */
function itemSummary(order: OrderView): string {
  const first = order.items[0]
  if (!first) {
    return ''
  }
  const base = `${first.productName} ×${first.quantity}`
  const restQty = order.items.slice(1).reduce((sum, item) => sum + item.quantity, 0)
  return restQty > 0 ? `${base} 等 ${restQty + first.quantity} 件` : base
}

function onAction(order: OrderView, action: string) {
  ElMessage.info(`「${action}」在真实接口接入后可用（订单 ${order.orderNo}）`)
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
            <span class="order-card__summary">{{ itemSummary(order) }}</span>
            <span class="order-card__amount">
              <i>¥</i>{{ order.payAmount }}
            </span>
          </div>

          <div class="order-card__row">
            <StatusChip
              :label="ORDER_STATUS_META[order.status].label"
              :tone="ORDER_STATUS_META[order.status].tone"
            />
            <span class="order-card__store">{{ order.storeName.replace('Atlantic Coffee·', '') }}</span>
          </div>
        </RouterLink>

        <div v-if="order.status !== 'CANCELLED' && order.status !== 'REFUNDED'" class="order-card__actions">
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            size="small"
            @click="onAction(order, '取消订单')"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 'COMPLETED'"
            size="small"
            @click="onAction(order, '申请退款')"
          >
            申请退款
          </el-button>
          <el-button
            v-if="order.status === 'READY'"
            size="small"
            @click="onAction(order, '确认取餐')"
          >
            确认取餐
          </el-button>
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            class="el-button--cta"
            size="small"
            @click="onAction(order, '去支付')"
          >
            去支付
          </el-button>
        </div>
      </article>

      <p v-if="filteredOrders.length === 0" class="orders__empty">该状态下暂无订单</p>
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

@media (min-width: 768px) {
  .orders__filters {
    position: static;
    margin: 0 0 var(--ac-space-5);
    padding: 0;
    overflow: visible;
  }
}
</style>
