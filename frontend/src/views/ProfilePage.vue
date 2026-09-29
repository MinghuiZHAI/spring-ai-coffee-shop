<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusChip from '@/components/StatusChip.vue'
import {
  CLAIMABLE_TEMPLATES,
  COUPON_STATUS_LABEL,
  MEMBER_LEVEL_LABEL,
  MEMBER_LEVEL_RULES,
  MY_COUPONS,
  NEXT_LEVEL,
  POINT_BALANCE,
  POINT_EMPTY_HINT,
  POINT_RECORDS,
  POINT_USAGE_NOTE,
  PROFILE_USER,
  type CouponStatus,
} from '@/data/profile'
import { ORDERS, formatTime } from '@/data/orders'

/**
 * 个人中心 v2（M1-6 步骤 3c 重构）：
 * 顶部固定区（头像/昵称/等级徽标）→ 会员卡（深海蓝渐变 + 弧线母题 + 当前积分 + 升级进度条）→
 * 胶囊 Tab（我的积分 / 我的优惠券，客服记录隐藏）→ Tab 内容（各 Tab 独立状态，首次加载骨架屏）。
 * PC ≥900px 左右两栏（左会员卡粘性 / 右内容），移动端单列。
 * 接后端后由 /api/user/points、/api/user/coupons、/api/user/coupons/claim 替换；
 * 累计实付暂无后端接口，升级进度条用静态假数据（totalPaid=128 / L2 阈值 300）。
 */
const memberLabel = MEMBER_LEVEL_LABEL[PROFILE_USER.memberLevel]

/** 升级进度（静态假数据）：累计实付 / 下一级阈值 */
const progressPercent = computed(() =>
  Math.min(100, Math.round((PROFILE_USER.totalPaid / NEXT_LEVEL.threshold) * 100)),
)
const remainToNext = computed(() => Math.max(0, NEXT_LEVEL.threshold - PROFILE_USER.totalPaid))

/** 流水行的关联订单号 */
function orderNoOf(relatedOrderId: number): string {
  return ORDERS.find((order) => order.id === relatedOrderId)?.orderNo ?? `#${relatedOrderId}`
}

/* ===== 胶囊 Tab（各 Tab 独立状态：v-show 保持挂载与滚动位置；首次加载骨架屏） ===== */
type ProfileTab = 'points' | 'coupons'

const TABS: Array<{ key: ProfileTab; label: string }> = [
  { key: 'points', label: '我的积分' },
  { key: 'coupons', label: '我的优惠券' },
]

const activeTab = ref<ProfileTab>('points')
const tabLoading = reactive({ points: true, coupons: true })

function settleTab(tab: ProfileTab) {
  setTimeout(() => {
    tabLoading[tab] = false
  }, 500)
}

function switchTab(tab: ProfileTab) {
  activeTab.value = tab
  if (tabLoading[tab]) {
    settleTab(tab)
  }
}

onMounted(() => settleTab('points'))

/* ===== 积分 Tab ===== */
const records = ref([...POINT_RECORDS])

/* ===== 优惠券 Tab（二级状态切换；状态独立于 Tab 切换保留） ===== */
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

/* ===== 交互（静态阶段统一 toast 反馈） ===== */
function onUseCoupon() {
  ElMessage.info('下单时自动抵扣，去菜单挑选饮品')
}

function onClaim(id: number) {
  ElMessage.success(`领取成功：${CLAIMABLE_TEMPLATES.find((t) => t.id === id)?.name ?? ''}`)
}

function onGoMenu() {
  ElMessage.info('即将跳转菜单页')
}

function onEntry(label: string) {
  ElMessage.info(`「${label}」在后续步骤接入`)
}
</script>

<template>
  <div class="profile">
    <!-- 顶部固定区：头像 + 昵称 + 等级徽标 -->
    <header class="profile__header">
      <span class="profile__avatar" aria-hidden="true">
        <svg width="22" height="22" viewBox="0 0 32 32" fill="none">
          <path d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0" stroke="#fff" stroke-width="2.4" stroke-linecap="round" />
          <path d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0" stroke="#7fd1f5" stroke-width="2" stroke-linecap="round" />
        </svg>
      </span>
      <div class="profile__header-info">
        <span class="profile__nickname">{{ PROFILE_USER.nickname }}</span>
        <span class="profile__phone">{{ PROFILE_USER.phoneMasked }}</span>
      </div>
      <span class="profile__level-chip">
        <span class="profile__level-dot" aria-hidden="true"></span>
        {{ memberLabel }}
      </span>
    </header>

    <div class="profile__layout">
      <!-- 左栏：会员卡（深海蓝渐变 + 弧线母题 + 当前积分 + 升级进度） -->
      <aside class="profile__side">
        <section class="member-card">
          <span class="member-card__level">
            <span class="member-card__level-dot" aria-hidden="true"></span>
            {{ memberLabel }}
          </span>

          <div class="member-card__points">
            <span class="member-card__points-label">当前积分</span>
            <span class="member-card__points-value">{{ POINT_BALANCE }}</span>
          </div>

          <div class="member-card__progress">
            <div class="member-card__progress-head">
              <span>升级进度</span>
              <span class="member-card__progress-num">
                ¥{{ PROFILE_USER.totalPaid }} / ¥{{ NEXT_LEVEL.threshold }}
              </span>
            </div>
            <div
              class="member-card__progress-track"
              role="progressbar"
              :aria-valuenow="progressPercent"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-label="`升级到 ${NEXT_LEVEL.name} 的进度`"
            >
              <span
                class="member-card__progress-fill"
                :style="{ width: `${progressPercent}%` }"
              ></span>
            </div>
            <p class="member-card__progress-caption">
              再消费 ¥{{ remainToNext }} 升级 {{ NEXT_LEVEL.name }} · {{ MEMBER_LEVEL_RULES }}
            </p>
          </div>

          <!-- 弧线母题：收于卡底，呼应品牌徽标 -->
          <svg class="member-card__waves" viewBox="0 0 320 64" fill="none" preserveAspectRatio="none" aria-hidden="true">
            <path d="M-10 42c30-14 60-14 90 0s60 14 90 0 60-14 90 0 60 14 70 8" stroke="rgba(255,255,255,0.22)" stroke-width="2.4" stroke-linecap="round" />
            <path d="M-10 54c30-14 60-14 90 0s60 14 90 0 60-14 90 0 60 14 70 8" stroke="rgba(125,211,252,0.4)" stroke-width="2" stroke-linecap="round" />
          </svg>
        </section>
      </aside>

      <!-- 右栏：胶囊 Tab + 内容（各 Tab 独立状态，v-show 保挂载） -->
      <div class="profile__main">
        <div class="profile__tabs" role="tablist" aria-label="个人中心内容">
          <button
            v-for="tab in TABS"
            :key="tab.key"
            type="button"
            class="profile__tab"
            :class="{ 'is-active': activeTab === tab.key }"
            role="tab"
            :aria-selected="activeTab === tab.key"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 我的积分 -->
        <section v-show="activeTab === 'points'" class="profile__panel" aria-label="我的积分">
          <template v-if="tabLoading.points">
            <div class="skeleton skeleton--block"></div>
            <div v-for="n in 3" :key="n" class="skeleton skeleton--row"></div>
          </template>

          <template v-else>
            <div class="points-balance">
              <span class="points-balance__label">当前余额</span>
              <span class="points-balance__value">{{ POINT_BALANCE }}</span>
              <span class="points-balance__unit">分</span>
            </div>

            <!-- 空态：新用户无流水（假数据有记录时不显示） -->
            <div v-if="records.length === 0" class="points-empty">
              <p class="points-empty__hint">{{ POINT_EMPTY_HINT }}</p>
              <el-button class="el-button--cta" size="small" @click="onGoMenu">去菜单点单</el-button>
            </div>

            <ul v-else class="points-records">
              <li v-for="record in records" :key="record.id" class="points-record">
                <div class="points-record__info">
                  <span class="points-record__type">获得</span>
                  <span class="points-record__order">订单 {{ orderNoOf(record.relatedOrderId) }}</span>
                </div>
                <span class="points-record__change">+{{ record.changeValue }}</span>
                <span class="points-record__time">{{ formatTime(record.createdAt) }}</span>
              </li>
            </ul>

            <p class="points-usage">{{ POINT_USAGE_NOTE }}</p>
          </template>
        </section>

        <!-- 我的优惠券 -->
        <section v-show="activeTab === 'coupons'" class="profile__panel" aria-label="我的优惠券">
          <template v-if="tabLoading.coupons">
            <div v-for="n in 3" :key="n" class="skeleton skeleton--row"></div>
          </template>

          <template v-else>
            <!-- 二级状态切换 -->
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

            <div class="coupon-list">
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
                <div class="coupon__info">
                  <span class="coupon__name">{{ coupon.name }}</span>
                  <span class="coupon__expire">有效期至 {{ coupon.expireAt }}</span>
                  <span v-if="coupon.usedOrderNo" class="coupon__expire">
                    订单 {{ coupon.usedOrderNo }} · {{ COUPON_STATUS_LABEL[coupon.status] }}
                  </span>
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

              <p v-if="filteredCoupons.length === 0" class="coupon-list__empty">
                {{ COUPON_STATUS_LABEL[activeCouponFilter] }}的券暂无记录
              </p>
            </div>

            <!-- 领券中心 -->
            <h3 class="profile__sub-title">领券中心</h3>
            <div class="claim-rail">
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
          </template>
        </section>
      </div>
    </div>

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
  max-width: 1080px;
  margin: 0 auto;
  /* 移动端为底部 Tab 预留高度 */
  padding-bottom: calc(var(--ac-tabbar-h) + var(--ac-space-6));
}

/* ===== 顶部固定区 ===== */
.profile__header {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
}

.profile__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 44px;
  height: 44px;
  background: var(--ac-primary);
  border-radius: var(--ac-radius-pill);
}

.profile__header-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.profile__nickname {
  font-size: 18px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.profile__phone {
  font-size: 12px;
  color: var(--ac-text-dim);
}

/* 等级徽标：晴空蓝点缀 */
.profile__level-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #0284c7;
  background: var(--ac-tide);
  border: 1px solid rgba(56, 189, 248, 0.4);
  border-radius: var(--ac-radius-pill);
}

.profile__level-dot {
  width: 6px;
  height: 6px;
  background: var(--ac-sky);
  border-radius: var(--ac-radius-pill);
}

/* ===== 双栏布局 ===== */
.profile__layout {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-4);
}

.profile__side {
  min-width: 0;
}

.profile__main {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-4);
  min-width: 0;
}

/* ===== 会员卡（深海蓝渐变 + 弧线母题，全页视觉重心） ===== */
.member-card {
  position: relative;
  overflow: hidden;
  padding: var(--ac-space-5);
  color: #fff;
  background: linear-gradient(140deg, #0c4a6e 0%, #0369a1 52%, #0e7ab8 100%);
  border-radius: var(--ac-radius-overlay);
  box-shadow: var(--ac-shadow-lg);
}

.member-card__level {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: #7dd3fc;
  background: rgba(56, 189, 248, 0.16);
  border: 1px solid rgba(125, 211, 252, 0.45);
  border-radius: var(--ac-radius-pill);
}

.member-card__level-dot {
  width: 6px;
  height: 6px;
  background: var(--ac-sky);
  border-radius: var(--ac-radius-pill);
}

.member-card__points {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-2);
  margin: var(--ac-space-6) 0 var(--ac-space-5);
}

.member-card__points-label {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.72);
}

.member-card__points-value {
  font-family: var(--ac-font-display);
  font-size: 44px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.member-card__progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--ac-space-2);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
}

.member-card__progress-num {
  font-family: var(--ac-font-display);
  font-variant-numeric: tabular-nums;
}

.member-card__progress-track {
  height: 6px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.22);
  border-radius: var(--ac-radius-pill);
}

.member-card__progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--ac-sky), #7dd3fc);
  border-radius: var(--ac-radius-pill);
  transition: width var(--ac-dur-slow) var(--ac-ease-enter);
}

.member-card__progress-caption {
  margin: var(--ac-space-2) 0 0;
  font-size: 11px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.62);
}

.member-card__waves {
  position: absolute;
  inset: auto 0 -2px 0;
  width: 100%;
  height: 64px;
  pointer-events: none;
}

/* ===== 胶囊 Tab（一级：深海蓝实底激活） ===== */
.profile__tabs {
  display: flex;
  gap: var(--ac-space-2);
}

.profile__tab {
  padding: 9px var(--ac-space-5);
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  &.is-active {
    color: #fff;
    background: var(--ac-primary);
    border-color: var(--ac-primary);
  }
}

/* ===== Tab 内容面板 ===== */
.profile__panel {
  min-height: 320px;
}

/* 余额：第一层重点信息 */
.points-balance {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-2);
  margin-bottom: var(--ac-space-5);
}

.points-balance__label {
  font-size: 13px;
  color: var(--ac-text-dim);
}

.points-balance__value {
  font-family: var(--ac-font-display);
  font-size: 36px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--ac-primary-deep);
}

.points-balance__unit {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.points-records {
  margin: 0;
  padding: 0;
  list-style: none;
}

.points-record {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-3) 0;

  & + & {
    border-top: 1px solid var(--ac-border);
  }
}

.points-record__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.points-record__type {
  font-size: 14px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

.points-record__order {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points-record__change {
  font-family: var(--ac-font-display);
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-success);
}

.points-record__time {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points-usage {
  margin: var(--ac-space-4) 0 0;
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-8) 0;
}

.points-empty__hint {
  margin: 0;
  font-size: 14px;
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

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-3);
}

/* 可用券：琥珀边框；不可用：灰边框 + 半透明（历史可见） */
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
  flex: none;
}

.coupon-list__empty {
  margin: var(--ac-space-4) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.profile__sub-title {
  margin: var(--ac-space-6) 0 var(--ac-space-3);
  font-size: 15px;
  font-weight: 600;
}

/* 领券中心横滚 */
.claim-rail {
  display: flex;
  gap: var(--ac-space-3);
  overflow-x: auto;
  padding-bottom: var(--ac-space-2);
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
.profile__card {
  padding: var(--ac-space-2) var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
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

/* ===== 骨架屏 ===== */
.skeleton {
  background: linear-gradient(90deg, #eef4f8 25%, #f7fafc 37%, #eef4f8 63%);
  background-size: 400% 100%;
  border-radius: var(--ac-radius-btn);
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.skeleton--block {
  height: 72px;
  margin-bottom: var(--ac-space-4);
}

.skeleton--row {
  height: 52px;
  margin-bottom: var(--ac-space-3);
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: 0 0;
  }
}

/* ===== PC（≥900px）：左会员卡粘性 + 右内容 ===== */
@media (min-width: 900px) {
  .profile {
    padding-bottom: var(--ac-space-8);
  }

  .profile__layout {
    display: grid;
    grid-template-columns: 340px 1fr;
    gap: var(--ac-space-6);
    align-items: start;
  }

  .profile__side {
    position: sticky;
    top: calc(var(--ac-nav-h) + var(--ac-space-6));
  }
}
</style>
