<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusChip from '@/components/StatusChip.vue'
import {
  MAIN_FLOW,
  ORDERS,
  ORDER_STATUS_META,
  REFUND_STATUS_LABEL,
  STATUS_HINT,
  formatTime,
  parseSpecs,
} from '@/data/orders'

/**
 * 订单详情（M1-6 步骤 3b，静态假数据）：
 * 状态头卡（主流程步进条/售后提示）→ 取餐码 → 门店信息 → 商品清单 →
 * 金额明细 → 退款单 → 按状态操作。接后端后由 GET /api/user/orders/{id} 替换。
 */
const route = useRoute()

const order = computed(() => ORDERS.find((o) => o.id === Number(route.params.id)))

/** 主流程步进条：当前步索引（售后/取消态不展示） */
const flowIndex = computed(() => {
  if (!order.value) {
    return -1
  }
  return MAIN_FLOW.indexOf(order.value.status)
})

const showFlow = computed(() => flowIndex.value >= 0)
const showPickupCode = computed(
  () => !!order.value?.pickupCode && ['PAID_TODO', 'MAKING', 'READY'].includes(order.value.status),
)

function onAction(action: string) {
  ElMessage.info(`「${action}」在真实接口接入后可用`)
}
</script>

<template>
  <div v-if="order" class="detail">
    <header class="detail__head">
      <RouterLink to="/orders" class="detail__back" aria-label="返回订单列表">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回订单列表
      </RouterLink>
    </header>

    <!-- 状态头卡：chip + 提示 + 主流程步进条 -->
    <section class="detail__status">
      <div class="detail__status-row">
        <StatusChip
          :label="ORDER_STATUS_META[order.status].label"
          :tone="ORDER_STATUS_META[order.status].tone"
        />
        <span class="detail__no">{{ order.orderNo }}</span>
      </div>
      <p class="detail__hint">{{ STATUS_HINT[order.status] }}</p>

      <!-- 主流程步进条（待支付→待制作→制作中→待取餐→已完成） -->
      <ol v-if="showFlow" class="detail__flow" aria-label="订单进度">
        <li
          v-for="(step, index) in MAIN_FLOW"
          :key="step"
          class="detail__flow-step"
          :class="{ 'is-done': index < flowIndex, 'is-current': index === flowIndex }"
        >
          <span class="detail__flow-dot"></span>
          <span class="detail__flow-label">{{ ORDER_STATUS_META[step].label }}</span>
        </li>
      </ol>
    </section>

    <!-- 取餐码（支付成功后生成，04 §2.2） -->
    <section v-if="showPickupCode" class="detail__pickup">
      <span class="detail__pickup-label">取餐码</span>
      <span class="detail__pickup-code">{{ order.pickupCode }}</span>
      <span class="detail__pickup-tip">向门店出示即可取餐</span>
    </section>

    <!-- 门店与取餐 -->
    <section class="detail__card">
      <h3 class="detail__section-title">门店信息</h3>
      <div class="detail__kv">
        <span class="detail__kv-key">门店</span>
        <span class="detail__kv-value">{{ order.storeName }}</span>
      </div>
      <div class="detail__kv">
        <span class="detail__kv-key">取餐方式</span>
        <span class="detail__kv-value">门店自取</span>
      </div>
      <div v-if="order.expectedFinishTime && ['PAID_TODO', 'MAKING'].includes(order.status)" class="detail__kv">
        <span class="detail__kv-key">预计完成</span>
        <span class="detail__kv-value">{{ formatTime(order.expectedFinishTime, true) }}</span>
      </div>
      <div v-if="order.status === 'PENDING_PAYMENT' && order.expireAt" class="detail__kv">
        <span class="detail__kv-key">支付截止</span>
        <span class="detail__kv-value">{{ formatTime(order.expireAt, true) }}</span>
      </div>
    </section>

    <!-- 商品清单 -->
    <section class="detail__card">
      <h3 class="detail__section-title">商品清单</h3>
      <div v-for="item in order.items" :key="item.productId" class="detail__item">
        <div class="detail__item-info">
          <span class="detail__item-name">{{ item.productName }}</span>
          <span
            v-for="spec in parseSpecs(item.specSnapshot)"
            :key="spec.group"
            class="detail__item-spec"
          >
            {{ spec.option }}
          </span>
        </div>
        <span class="detail__item-qty">×{{ item.quantity }}</span>
        <span class="detail__item-amount">¥{{ item.lineAmount }}</span>
      </div>
    </section>

    <!-- 金额明细 -->
    <section class="detail__card">
      <h3 class="detail__section-title">金额明细</h3>
      <div class="detail__kv">
        <span class="detail__kv-key">商品总额</span>
        <span class="detail__kv-value">¥{{ order.totalAmount }}</span>
      </div>
      <div v-if="order.discountAmount > 0" class="detail__kv">
        <span class="detail__kv-key">优惠券</span>
        <span class="detail__kv-value detail__kv-discount">-¥{{ order.discountAmount }}</span>
      </div>
      <div class="detail__kv detail__kv--pay">
        <span class="detail__kv-key">实付</span>
        <span class="detail__kv-value detail__kv-amount">¥{{ order.payAmount }}</span>
      </div>
      <div v-if="order.rewardPoints > 0" class="detail__kv">
        <span class="detail__kv-key">积分</span>
        <span class="detail__kv-value detail__kv-discount">+{{ order.rewardPoints }} 积分</span>
      </div>
    </section>

    <!-- 退款单（两跳节奏：申请中→已通过→已到账） -->
    <section v-if="order.refund" class="detail__card">
      <h3 class="detail__section-title">退款单</h3>
      <div class="detail__kv">
        <span class="detail__kv-key">退款单号</span>
        <span class="detail__kv-value">{{ order.refund.refundNo }}</span>
      </div>
      <div class="detail__kv">
        <span class="detail__kv-key">退款状态</span>
        <span class="detail__kv-value">{{ REFUND_STATUS_LABEL[order.refund.status] }}</span>
      </div>
      <div class="detail__kv">
        <span class="detail__kv-key">退款金额</span>
        <span class="detail__kv-value">¥{{ order.refund.amount }}</span>
      </div>
      <div class="detail__kv">
        <span class="detail__kv-key">申请时间</span>
        <span class="detail__kv-value">{{ formatTime(order.refund.applyAt, true) }}</span>
      </div>
      <div v-if="order.refund.finishedAt" class="detail__kv">
        <span class="detail__kv-key">到账时间</span>
        <span class="detail__kv-value">{{ formatTime(order.refund.finishedAt, true) }}</span>
      </div>
    </section>

    <!-- 按状态操作 -->
    <div class="detail__actions">
      <el-button
        v-if="order.status === 'PENDING_PAYMENT'"
        @click="onAction('取消订单')"
      >
        取消订单
      </el-button>
      <el-button
        v-if="order.status === 'READY'"
        class="el-button--cta"
        @click="onAction('确认取餐')"
      >
        确认取餐
      </el-button>
      <el-button
        v-if="order.status === 'COMPLETED'"
        @click="onAction('申请退款（完成后 24 小时内）')"
      >
        申请退款
      </el-button>
      <el-button
        v-if="order.status === 'PENDING_PAYMENT'"
        class="el-button--cta"
        @click="onAction('去支付')"
      >
        去支付
      </el-button>
    </div>
  </div>

  <div v-else class="detail detail--missing">
    <p class="detail__missing-text">订单不存在或已被删除</p>
    <RouterLink to="/orders" class="detail__back">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M15 18l-6-6 6-6" />
      </svg>
      返回订单列表
    </RouterLink>
  </div>
</template>

<style scoped lang="scss">
.detail {
  max-width: 720px;
  margin: 0 auto;
}

.detail--missing {
  padding: var(--ac-space-10) 0;
  text-align: center;
}

.detail__missing-text {
  margin: 0 0 var(--ac-space-4);
  color: var(--ac-text-dim);
}

.detail__head {
  margin-bottom: var(--ac-space-4);
}

.detail__back {
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

/* 卡片通用 */
.detail__status,
.detail__pickup,
.detail__card {
  padding: var(--ac-space-4) var(--ac-space-5);
  margin-bottom: var(--ac-space-3);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.detail__status-row {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
}

.detail__no {
  font-family: var(--ac-font-display);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
  color: var(--ac-text-dim);
}

.detail__hint {
  margin: var(--ac-space-2) 0 0;
  font-size: 13px;
  color: var(--ac-text);
}

/* 主流程步进条 */
.detail__flow {
  display: flex;
  margin: var(--ac-space-5) 0 0;
  padding: 0;
  list-style: none;
}

.detail__flow-step {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-1);

  /* 连接线：越过圆点 */
  &:not(:first-child)::before {
    content: '';
    position: absolute;
    top: 6px;
    right: 50%;
    width: 100%;
    height: 2px;
    background: var(--ac-border);
  }

  &.is-done:not(:first-child)::before,
  &.is-current:not(:first-child)::before {
    background: var(--ac-primary);
  }
}

.detail__flow-dot {
  z-index: 1;
  width: 12px;
  height: 12px;
  background: var(--ac-card);
  border: 2px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
}

.detail__flow-step.is-done .detail__flow-dot {
  background: var(--ac-primary);
  border-color: var(--ac-primary);
}

.detail__flow-step.is-current .detail__flow-dot {
  box-shadow: 0 0 0 3px var(--ac-tide);
  border-color: var(--ac-primary);
  background: var(--ac-primary);
}

.detail__flow-label {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.detail__flow-step.is-current .detail__flow-label {
  font-weight: 600;
  color: var(--ac-primary);
}

.detail__flow-step.is-done .detail__flow-label {
  color: var(--ac-text);
}

/* 取餐码 */
.detail__pickup {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-1);
  border-style: dashed;
}

.detail__pickup-label {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.detail__pickup-code {
  font-family: var(--ac-font-display);
  font-size: 40px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 8px;
  line-height: 1.1;
  color: var(--ac-primary);
}

.detail__pickup-tip {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.detail__section-title {
  margin-bottom: var(--ac-space-3);
  font-size: 15px;
  font-weight: 600;
}

/* 键值对 */
.detail__kv {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ac-space-4);

  & + & {
    margin-top: var(--ac-space-2);
  }

  &--pay {
    margin-top: var(--ac-space-3) !important;
    padding-top: var(--ac-space-3);
    border-top: 1px dashed var(--ac-border);
  }
}

.detail__kv-key {
  font-size: 13px;
  color: var(--ac-text-dim);
}

.detail__kv-value {
  font-size: 13px;
  color: var(--ac-text);
  text-align: right;
}

.detail__kv-discount {
  color: var(--ac-success);
}

.detail__kv-amount {
  font-family: var(--ac-font-display);
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

/* 商品清单行 */
.detail__item {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-3);

  & + & {
    margin-top: var(--ac-space-2);
  }
}

.detail__item-info {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ac-space-2);
  min-width: 0;
}

.detail__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

.detail__item-spec {
  padding: 1px 8px;
  font-size: 11px;
  color: var(--ac-text-dim);
  background: var(--ac-bg);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
}

.detail__item-qty {
  font-size: 13px;
  color: var(--ac-text-dim);
}

.detail__item-amount {
  font-family: var(--ac-font-display);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

.detail__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--ac-space-2);
  margin-top: var(--ac-space-4);
}
</style>
