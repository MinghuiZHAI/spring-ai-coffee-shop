<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusChip from '@/components/StatusChip.vue'
import {
  CLAIMABLE_TEMPLATES,
  COUPON_STATUS_LABEL,
  MEMBER_LEVEL_LABEL,
  MEMBER_LEVEL_RULES,
  MY_COUPONS,
  POINT_BALANCE,
  POINT_RECORDS,
  POINT_USAGE_NOTE,
  PROFILE_USER,
  type CouponStatus,
} from '@/data/profile'
import { ORDERS, formatTime } from '@/data/orders'

/**
 * 个人中心（M1-6 步骤 3c，静态假数据）：
 * 用户卡（等级徽章 + 升级规则）→ 积分区（余额 + EARN 流水）→
 * 优惠券（状态筛选 + 券卡四要素 + 领券中心横滚）→ 功能列表。
 * 接后端后由 /api/user/points、/api/user/coupons、/api/user/coupons/claim 替换。
 */
const memberLabel = MEMBER_LEVEL_LABEL[PROFILE_USER.memberLevel]

/** 流水行的关联订单号：本地数据用 orders.ts 的 orderNo 展示 */
function orderNoOf(relatedOrderId: number): string {
  return ORDERS.find((order) => order.id === relatedOrderId)?.orderNo ?? `#${relatedOrderId}`
}

/* —— 优惠券状态筛选 —— */
const COUPON_FILTERS: Array<{ key: CouponStatus; label: string }> = [
  { key: 'UNUSED', label: '未使用' },
  { key: 'USED', label: '已使用' },
  { key: 'EXPIRED', label: '已过期' },
]

const activeCouponFilter = ref<CouponStatus>('UNUSED')

const filteredCoupons = computed(() => MY_COUPONS.filter((c) => c.status === activeCouponFilter.value))

function thresholdText(thresholdAmount: number): string {
  return thresholdAmount > 0 ? `满${thresholdAmount}可用` : '无门槛'
}

/* —— 交互（静态阶段统一 toast 反馈） —— */
function onUseCoupon() {
  ElMessage.info('下单时自动抵扣，去菜单挑选饮品')
}

function onClaim(id: number) {
  ElMessage.success(`领取成功：${CLAIMABLE_TEMPLATES.find((t) => t.id === id)?.name ?? ''}`)
}

function onEntry(label: string) {
  ElMessage.info(`「${label}」在后续步骤接入`)
}
</script>

<template>
  <div class="profile">
    <!-- 用户信息卡 -->
    <section class="profile__user">
      <span class="profile__avatar" aria-hidden="true">
        <svg width="30" height="30" viewBox="0 0 32 32" fill="none">
          <path d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0" stroke="#fff" stroke-width="2.2" stroke-linecap="round" />
          <path d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0" stroke="#7fd1f5" stroke-width="2" stroke-linecap="round" />
        </svg>
      </span>
      <div class="profile__user-info">
        <div class="profile__user-row">
          <span class="profile__nickname">{{ PROFILE_USER.nickname }}</span>
          <span class="profile__level">{{ memberLabel }}</span>
        </div>
        <span class="profile__phone">{{ PROFILE_USER.phoneMasked }}</span>
        <p class="profile__rules">{{ MEMBER_LEVEL_RULES }}</p>
      </div>
    </section>

    <!-- 积分区 -->
    <section class="profile__card">
      <h3 class="profile__section-title">我的积分</h3>
      <div class="profile__points">
        <div class="profile__points-balance">
          <span class="profile__points-label">积分余额</span>
          <span class="profile__points-value">{{ POINT_BALANCE }}</span>
        </div>
        <p class="profile__points-note">{{ POINT_USAGE_NOTE }}</p>
      </div>
      <div class="profile__records">
        <div v-for="record in POINT_RECORDS" :key="record.id" class="profile__record">
          <div class="profile__record-info">
            <span class="profile__record-type">下单获得</span>
            <span class="profile__record-order">订单 {{ orderNoOf(record.relatedOrderId) }}</span>
          </div>
          <div class="profile__record-side">
            <span class="profile__record-change">+{{ record.changeValue }}</span>
            <span class="profile__record-time">{{ formatTime(record.createdAt) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 优惠券 -->
    <section class="profile__card">
      <h3 class="profile__section-title">我的优惠券</h3>

      <div class="profile__coupon-filters" role="tablist" aria-label="优惠券状态">
        <button
          v-for="filter in COUPON_FILTERS"
          :key="filter.key"
          type="button"
          class="profile__coupon-filter"
          :class="{ 'is-active': activeCouponFilter === filter.key }"
          @click="activeCouponFilter = filter.key"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="profile__coupon-list">
        <article
          v-for="coupon in filteredCoupons"
          :key="coupon.id"
          class="coupon"
          :class="{ 'is-dim': coupon.status !== 'UNUSED' }"
        >
          <div class="coupon__face">
            <span class="coupon__value"><i>¥</i>{{ coupon.discountAmount }}</span>
            <span class="coupon__threshold">{{ thresholdText(coupon.thresholdAmount) }}</span>
          </div>
          <div class="coupon__divider" aria-hidden="true"></div>
          <div class="coupon__info">
            <span class="coupon__name">{{ coupon.name }}</span>
            <span class="coupon__expire">有效期至 {{ coupon.expireAt }}</span>
            <span v-if="coupon.usedOrderNo" class="coupon__used-order">订单 {{ coupon.usedOrderNo }}</span>
          </div>
          <el-button
            v-if="coupon.status === 'UNUSED'"
            class="el-button--cta coupon__use"
            size="small"
            @click="onUseCoupon"
          >
            去使用
          </el-button>
          <StatusChip
            v-else
            class="coupon__status"
            :label="COUPON_STATUS_LABEL[coupon.status]"
            tone="muted"
          />
        </article>

        <p v-if="filteredCoupons.length === 0" class="profile__coupon-empty">
          {{ COUPON_STATUS_LABEL[activeCouponFilter] }}的券暂无记录
        </p>
      </div>
    </section>

    <!-- 领券中心 -->
    <section class="profile__card">
      <h3 class="profile__section-title">领券中心</h3>
      <div class="profile__claim-rail">
        <article v-for="template in CLAIMABLE_TEMPLATES" :key="template.id" class="claim-card">
          <span class="claim-card__value"><i>¥</i>{{ template.discountAmount }}</span>
          <span class="claim-card__threshold">{{ thresholdText(template.thresholdAmount) }}</span>
          <span class="claim-card__name">{{ template.name }}</span>
          <span class="claim-card__valid">领取后 {{ template.validDays }} 天内有效</span>
          <el-button class="el-button--cta claim-card__btn" size="small" @click="onClaim(template.id)">
            领取
          </el-button>
        </article>
      </div>
    </section>

    <!-- 功能列表 -->
    <section class="profile__card profile__entries">
      <button type="button" class="profile__entry" @click="onEntry('知识库管理后台')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
        </svg>
        <span class="profile__entry-label">知识库管理后台</span>
        <span class="profile__entry-badge">ADMIN</span>
        <svg class="profile__entry-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="profile__entry" @click="onEntry('联系客服')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M3 12a9 9 0 0 1 18 0" />
          <path d="M21 12v4a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2h3" />
          <path d="M3 12v4a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-2a2 2 0 0 0-2-2H3" />
        </svg>
        <span class="profile__entry-label">联系客服</span>
        <svg class="profile__entry-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="profile__entry" @click="onEntry('帮助中心')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" />
          <path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3" />
          <path d="M12 17h.01" />
        </svg>
        <span class="profile__entry-label">帮助中心</span>
        <svg class="profile__entry-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="profile__entry profile__entry--danger" @click="onEntry('退出登录')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <path d="M16 17l5-5-5-5" />
          <path d="M21 12H9" />
        </svg>
        <span class="profile__entry-label">退出登录</span>
      </button>
    </section>
  </div>
</template>

<style scoped lang="scss">
.profile {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-4);
  max-width: 720px;
  margin: 0 auto;
}

/* ===== 用户信息卡 ===== */
.profile__user {
  display: flex;
  align-items: center;
  gap: var(--ac-space-4);
  padding: var(--ac-space-5);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.profile__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 56px;
  height: 56px;
  background: var(--ac-primary);
  border-radius: var(--ac-radius-pill);
}

.profile__user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.profile__user-row {
  display: flex;
  align-items: center;
  gap: var(--ac-space-2);
}

.profile__nickname {
  font-size: 18px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.profile__level {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--ac-primary);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
}

.profile__phone {
  font-size: 13px;
  color: var(--ac-text-dim);
}

.profile__rules {
  margin: var(--ac-space-1) 0 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--ac-text-dim);
}

/* ===== 卡片通用 ===== */
.profile__card {
  padding: var(--ac-space-5);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.profile__section-title {
  margin-bottom: var(--ac-space-3);
  font-size: 15px;
  font-weight: 600;
}

/* ===== 积分 ===== */
.profile__points {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ac-space-4);
  padding-bottom: var(--ac-space-3);
  border-bottom: 1px dashed var(--ac-border);
}

.profile__points-balance {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-2);
}

.profile__points-label {
  font-size: 13px;
  color: var(--ac-text-dim);
}

.profile__points-value {
  font-family: var(--ac-font-display);
  font-size: 32px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--ac-primary-deep);
}

.profile__points-note {
  margin: 0;
  max-width: 260px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--ac-text-dim);
  text-align: right;
}

.profile__records {
  margin-top: var(--ac-space-3);
}

.profile__record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ac-space-3);

  & + & {
    margin-top: var(--ac-space-3);
  }
}

.profile__record-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.profile__record-type {
  font-size: 14px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

.profile__record-order {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.profile__record-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}

.profile__record-change {
  font-family: var(--ac-font-display);
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-success);
}

.profile__record-time {
  font-size: 11px;
  color: var(--ac-text-dim);
}

/* ===== 优惠券 ===== */
.profile__coupon-filters {
  display: flex;
  gap: var(--ac-space-2);
  margin-bottom: var(--ac-space-3);
}

.profile__coupon-filter {
  padding: 6px var(--ac-space-4);
  font-family: var(--ac-font-body);
  font-size: 13px;
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

.profile__coupon-list {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-3);
}

/* 券卡：左面额 + 虚线分割 + 信息/操作 */
.coupon {
  display: flex;
  align-items: center;
  gap: var(--ac-space-4);
  padding: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);

  &.is-dim {
    opacity: 0.62;
  }
}

.coupon__face {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  width: 76px;
}

.coupon__value {
  font-family: var(--ac-font-display);
  font-size: 26px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  color: var(--ac-cta);

  i {
    margin-right: 1px;
    font-size: 14px;
    font-style: normal;
    font-weight: 500;
  }
}

.coupon__threshold {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.coupon__divider {
  align-self: stretch;
  width: 0;
  border-left: 1px dashed var(--ac-border);
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

.coupon__used-order {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.coupon__use {
  flex: none;
}

.coupon__status {
  flex: none;
}

.profile__coupon-empty {
  margin: var(--ac-space-4) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

/* ===== 领券中心横滚 ===== */
.profile__claim-rail {
  display: flex;
  gap: var(--ac-space-3);
  margin: calc(-1 * var(--ac-space-5)) calc(-1 * var(--ac-space-5)) 0;
  padding: 0 var(--ac-space-5) var(--ac-space-2);
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.claim-card {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  width: 150px;
  padding: var(--ac-space-3);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-card);
}

.claim-card__value {
  font-family: var(--ac-font-display);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  color: var(--ac-primary-deep);

  i {
    margin-right: 1px;
    font-size: 13px;
    font-style: normal;
    font-weight: 500;
  }
}

.claim-card__threshold {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.claim-card__name {
  margin-top: var(--ac-space-1);
  font-size: 12px;
  font-weight: 500;
  color: var(--ac-primary-deep);
  text-align: center;
}

.claim-card__valid {
  font-size: 10px;
  color: var(--ac-text-dim);
}

.claim-card__btn {
  margin-top: var(--ac-space-2);
}

/* ===== 功能列表 ===== */
.profile__entries {
  padding: var(--ac-space-2) var(--ac-space-4);
}

.profile__entry {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  width: 100%;
  padding: var(--ac-space-4) 0;
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text);
  background: none;
  border: none;
  border-bottom: 1px solid var(--ac-border);
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    color: var(--ac-primary);
  }

  svg {
    width: 18px;
    height: 18px;
    flex: none;
  }
}

.profile__entry-label {
  flex: 1;
  text-align: left;
}

.profile__entry-badge {
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--ac-primary);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
}

.profile__entry-chevron {
  color: var(--ac-text-dim);
}

.profile__entry--danger {
  color: var(--ac-danger);

  &:hover {
    color: var(--ac-danger);
    opacity: 0.8;
  }
}
</style>
