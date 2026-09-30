<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCoupons, type CouponView } from '@/api/coupon'
import { getWallet } from '@/api/wallet'
import { getPoints, type PointRecord } from '@/api/user'

/**
 * 个人中心 v4（M1-6 批次 4）：
 * 顶部真数据（点击进 /profile/info）→ 数据按钮（优惠券数量/钱包余额，真接口）→
 * 会员卡 → 四宫格入口 → 胶囊 Tab（我的积分/我的优惠券：真接口、骨架屏、独立状态，
 * 客服记录隐藏）。PC ≥900px 左会员卡粘性 + 右 Tab 内容，移动端单列。
 * 优惠券/余额/积分在挂载时拉取，未返回前显示 -- 或骨架屏。
 */
const store = useUserStore()
const router = useRouter()

const walletBalance = ref<number | null>(null)
const couponCount = ref<number | null>(null)

const wechatVisible = ref(false)

/* ===== 内容 Tab（积分/优惠券）：v-show 保持各自状态，首次加载骨架屏 ===== */
type ProfileTab = 'points' | 'coupons'
const TABS = [
  { key: 'points', label: '我的积分' },
  { key: 'coupons', label: '我的优惠券' },
] as const
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

/* 积分（真接口 GET /api/user/points） */
const pointBalance = ref<number | null>(null)
const pointRecords = ref<PointRecord[]>([])

/* 优惠券（真接口一次拉全量，Tab 内按状态过滤；未使用带琥珀边框，历史灰度可见） */
const coupons = ref<CouponView[]>([])
const COUPON_FILTERS = [
  { key: 'UNUSED', label: '未使用' },
  { key: 'USED', label: '已使用' },
  { key: 'EXPIRED', label: '已过期' },
] as const
const activeCouponFilter = ref<'UNUSED' | 'USED' | 'EXPIRED'>('UNUSED')
const filteredCoupons = computed(() =>
  coupons.value.filter((coupon) => coupon.status === activeCouponFilter.value),
)

const COUPON_STATUS_LABEL = { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }
const POINT_EMPTY_HINT = '下单即可获得积分，1 元 = 10 积分'
const POINT_USAGE_NOTE = '当前版本支持积分累积与查询，抵扣与兑换将在后续版本上线'

function thresholdText(threshold: number | null): string {
  return threshold && threshold > 0 ? `满${threshold}可用` : '无门槛'
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function onGoMenu() {
  router.push('/menu')
}

function onUseCoupon() {
  ElMessage.info('下单时自动抵扣，去菜单挑选饮品')
}

onMounted(async () => {
  try {
    walletBalance.value = (await getWallet()).balance
  } catch {
    walletBalance.value = null
  }
  try {
    couponCount.value = (await getCoupons('UNUSED')).length
  } catch {
    couponCount.value = null
  }
  try {
    const points = await getPoints()
    pointBalance.value = points.balance
    pointRecords.value = points.records
  } catch {
    pointBalance.value = null
  }
  try {
    coupons.value = await getCoupons()
  } catch {
    coupons.value = []
  }
  settleTab('points')
  settleTab('coupons')
})

/** 头像：avatarUrl 非空用图片，否则默认波浪徽章 */
const avatarUrl = computed(() => store.userInfo?.avatarUrl ?? '')

/* 演示二维码：确定性伪 QR 图案（21×21 模块，三个定位角），真实二维码接入时替换 */
const QR_SIZE = 21
const qrCells = computed(() => {
  const cells: Array<[number, number]> = []
  const inFinder = (x: number, y: number) =>
    (x < 7 && y < 7) || (x > QR_SIZE - 8 && y < 7) || (x < 7 && y > QR_SIZE - 8)
  for (let y = 0; y < QR_SIZE; y++) {
    for (let x = 0; x < QR_SIZE; x++) {
      if (inFinder(x, y)) {
        const lx = Math.min(x, QR_SIZE - 1 - x)
        const ly = Math.min(y, QR_SIZE - 1 - y)
        const edge = lx === 0 || ly === 0 || lx === 6 || ly === 6
        const core = lx >= 2 && lx <= 4 && ly >= 2 && ly <= 4
        if (edge || core) {
          cells.push([x, y])
        }
      } else if ((x * 7 + y * 13 + ((x * y) % 5)) % 3 < 1) {
        cells.push([x, y])
      }
    }
  }
  return cells
})

const GRID_TILES = [
  { key: 'franchise', label: '招商加盟', to: '/merchant', icon: 'briefcase' },
  { key: 'wechat', label: '关注微信', to: '', icon: 'wechat' },
  { key: 'recharge', label: '余额充值', to: '/wallet/recharge', icon: 'wallet' },
  { key: 'merchant', label: '我是商家', to: '/merchant', icon: 'store' },
] as const

function onGridTile(tile: (typeof GRID_TILES)[number]) {
  if (tile.key === 'wechat') {
    wechatVisible.value = true
    return
  }
  if (tile.to) {
    ElMessage.info(`即将前往「${tile.label}」`)
    // 统一跳转由模板内 RouterLink 完成，此处仅微信弹窗分支
  }
}
</script>

<template>
  <div class="profile">
    <!-- 顶部：真数据（点击进 /profile/info），上下留白加大 -->
    <header class="profile__header">
      <RouterLink to="/profile/info" class="profile__identity" aria-label="查看个人资料">
        <span class="profile__avatar" aria-hidden="true">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <svg v-else width="22" height="22" viewBox="0 0 32 32" fill="none">
            <path d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0" stroke="#fff" stroke-width="2.4" stroke-linecap="round" />
            <path d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0" stroke="#7fd1f5" stroke-width="2" stroke-linecap="round" />
          </svg>
        </span>
        <span class="profile__identity-info">
          <span class="profile__identity-name">
            {{ store.userInfo?.nickname ?? '未登录' }}
            <svg class="profile__identity-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </span>
          <span class="profile__identity-phone">{{ store.userInfo?.phone }}</span>
        </span>
        <span class="profile__level-chip">
          <span class="profile__level-dot" aria-hidden="true"></span>
          {{ store.memberLevelLabel }}
        </span>
      </RouterLink>

      <!-- 数据按钮：优惠券数量 / 钱包余额（真接口） -->
      <div class="profile__stats">
        <div class="profile__stat" role="status" aria-label="未使用优惠券数量">
          <span class="profile__stat-icon profile__stat-icon--coupon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z" />
              <path d="M16 8H8" />
            </svg>
          </span>
          <span class="profile__stat-value">{{ couponCount ?? '--' }}</span>
          <span class="profile__stat-label">优惠券</span>
        </div>
        <div class="profile__stat" role="status" aria-label="钱包余额">
          <span class="profile__stat-icon profile__stat-icon--wallet" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
              <path d="M21 12h-5a2 2 0 0 0 0 4h5V12Z" />
            </svg>
          </span>
          <span class="profile__stat-value">
            <i>¥</i>{{ walletBalance ?? '--' }}
          </span>
          <span class="profile__stat-label">钱包余额</span>
        </div>
      </div>
    </header>

    <!-- 双栏：左会员卡（粘性）+ 右 Tab 内容；移动端单列 -->
    <div class="profile__columns">
      <aside class="profile__side">
          <!-- 会员卡（视觉重心，结构不变） -->
          <section class="member-card">
            <span class="member-card__level">
              <span class="member-card__level-dot" aria-hidden="true"></span>
              {{ store.memberLevelLabel }}
            </span>

            <div class="member-card__points">
              <span class="member-card__points-label">当前积分</span>
              <span class="member-card__points-value">850</span>
            </div>

            <div class="member-card__progress">
              <div class="member-card__progress-head">
                <span>升级进度</span>
                <span class="member-card__progress-num">¥128 / ¥300</span>
              </div>
              <div class="member-card__progress-track" role="progressbar" aria-valuenow="43" aria-valuemin="0" aria-valuemax="100" aria-label="升级到 L2 领航员的进度">
                <span class="member-card__progress-fill" style="width: 43%"></span>
              </div>
              <p class="member-card__progress-caption">
                再消费 ¥172 升级 L2 领航员 · L2 积分 1.2 倍，L3 满 1000 元 1.5 倍
              </p>
            </div>

            <svg class="member-card__waves" viewBox="0 0 320 64" fill="none" preserveAspectRatio="none" aria-hidden="true">
              <path d="M-10 42c30-14 60-14 90 0s60 14 90 0 60-14 90 0 60 14 70 8" stroke="rgba(255,255,255,0.22)" stroke-width="2.4" stroke-linecap="round" />
              <path d="M-10 54c30-14 60-14 90 0s60 14 90 0 60-14 90 0 60 14 70 8" stroke="rgba(125,211,252,0.4)" stroke-width="2" stroke-linecap="round" />
            </svg>
          </section>
        </aside>

        <div class="profile__main">
          <!-- 胶囊 Tab：我的积分 / 我的优惠券（客服记录隐藏） -->
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
                <span class="points-balance__value">{{ pointBalance ?? '--' }}</span>
                <span class="points-balance__unit">分</span>
              </div>

              <div v-if="pointRecords.length === 0" class="points-empty">
                <p class="points-empty__hint">{{ POINT_EMPTY_HINT }}</p>
                <el-button class="el-button--cta" size="small" @click="onGoMenu">去菜单点单</el-button>
              </div>

              <ul v-else class="points-records">
                <li v-for="(record, index) in pointRecords" :key="index" class="points-record">
                  <div class="points-record__info">
                    <span class="points-record__type">获得</span>
                    <span class="points-record__order">订单 #{{ record.relatedOrderId }}</span>
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

                <p v-if="filteredCoupons.length === 0" class="coupon-list__empty">
                  {{ COUPON_STATUS_LABEL[activeCouponFilter] }}的券暂无记录
                </p>
              </div>
            </template>
          </section>
        </div>
      </div>

      <!-- 四宫格入口 -->
      <nav class="profile__grid" aria-label="快捷入口">
        <RouterLink
          v-for="tile in GRID_TILES"
          :key="tile.key"
          class="profile__tile"
          :to="tile.to || { path: '/' }"
          @click="onGridTile(tile)"
        >
          <span class="profile__tile-icon" aria-hidden="true">
            <svg v-if="tile.icon === 'briefcase'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="7" width="20" height="14" rx="2" />
              <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2" />
            </svg>
            <svg v-else-if="tile.icon === 'wechat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-8.5 8.5 8.5 8.5 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 8.5-8.5c4.7 0 8.5 3.8 8.5 8.5Z" />
              <path d="M9 11h.01" />
              <path d="M12.5 11h.01" />
              <path d="M16 11h.01" />
            </svg>
            <svg v-else-if="tile.icon === 'wallet'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
              <path d="M21 12h-5a2 2 0 0 0 0 4h5V12Z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l1-5h16l1 5" />
              <path d="M4 9v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V9" />
              <path d="M9 21v-6h6v6" />
            </svg>
          </span>
          <span class="profile__tile-label">{{ tile.label }}</span>
        </RouterLink>
      </nav>

      <!-- 关注微信弹窗（演示二维码） -->
      <el-dialog v-model="wechatVisible" title="关注微信公众号" width="320" align-center>
        <div class="profile__qr">
          <svg class="profile__qr-svg" viewBox="0 0 168 168" aria-label="微信公众号二维码（演示占位）">
            <rect width="168" height="168" fill="#fff" />
            <rect
              v-for="(cell, index) in qrCells"
              :key="index"
              :x="cell[0] * 8"
              :y="cell[1] * 8"
              width="8"
              height="8"
              fill="#0c4a6e"
            />
          </svg>
          <p class="profile__qr-tip">微信扫一扫，关注 Atlantic Coffee（演示二维码）</p>
        </div>
      </el-dialog>
    </div>
</template>

<style scoped lang="scss">
.profile {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-4);
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: calc(var(--ac-tabbar-h) + var(--ac-space-6));
}

/* ===== 顶部：真数据 + 留白加大，整块可点进资料页 ===== */
.profile__header {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  margin: var(--ac-space-4) 0 var(--ac-space-2);
  padding: var(--ac-space-4) var(--ac-space-2);
}

.profile__identity {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.profile__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 52px;
  height: 52px;
  overflow: hidden;
  background: var(--ac-primary);
  border-radius: var(--ac-radius-pill);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.profile__identity-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.profile__identity-name {
  display: inline-flex;
  align-items: center;
  gap: var(--ac-space-1);
  font-size: 18px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.profile__identity-chevron {
  width: 16px;
  height: 16px;
  color: var(--ac-text-dim);
  transition: transform var(--ac-dur-fast) var(--ac-ease-enter);
}

.profile__identity:hover .profile__identity-chevron {
  transform: translateX(2px);
}

.profile__identity-phone {
  font-size: 12px;
  color: var(--ac-text-dim);
}

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

/* ===== 数据按钮：优惠券 / 钱包余额 ===== */
.profile__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ac-space-3);
}

.profile__stat {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.profile__stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 12px;

  svg {
    width: 20px;
    height: 20px;
  }

  &--coupon {
    color: var(--ac-cta);
    background: #fdf0dc;
  }

  &--wallet {
    color: var(--ac-primary);
    background: var(--ac-tide);
  }
}

.profile__stat-value {
  font-family: var(--ac-font-display);
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  color: var(--ac-primary-deep);

  i {
    margin-right: 1px;
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
    color: var(--ac-text-dim);
  }
}

.profile__stat-label {
  font-size: 12px;
  color: var(--ac-text-dim);
}

/* ===== 会员卡 ===== */
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

/* ===== 四宫格 ===== */
.profile__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ac-space-3);
}

.profile__tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-2);
  padding: var(--ac-space-6) var(--ac-space-3);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-md);
  }
}

.profile__tile-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: var(--ac-radius-pill);
  background: var(--ac-tide);
  color: var(--ac-primary);

  svg {
    width: 22px;
    height: 22px;
  }
}

.profile__tile-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

/* ===== 双栏：左会员卡粘性 + 右 Tab 内容 ===== */
.profile__columns {
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
  cursor: pointer;
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);

  &.is-active {
    color: #fff;
    background: var(--ac-primary);
    border-color: var(--ac-primary);
  }
}

.profile__panel {
  min-height: 320px;
}

/* ===== 我的积分 ===== */
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

/* ===== 我的优惠券（可用券琥珀边框；历史灰度可见） ===== */
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

.coupon-list__empty {
  margin: var(--ac-space-4) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
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

/* ===== 微信弹窗 ===== */
.profile__qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-3);
}

.profile__qr-svg {
  width: 200px;
  height: 200px;
}

.profile__qr-tip {
  margin: 0;
  font-size: 12px;
  color: var(--ac-text-dim);
  text-align: center;
}

/* ===== PC（≥900px）：左会员卡粘性 + 右 Tab 内容，四宫格一行 ===== */
@media (min-width: 900px) {
  .profile {
    max-width: 1080px;
    padding-bottom: var(--ac-space-8);
  }

  .profile__columns {
    display: grid;
    grid-template-columns: 340px 1fr;
    gap: var(--ac-space-6);
    align-items: start;
  }

  .profile__side {
    position: sticky;
    top: calc(var(--ac-nav-h) + var(--ac-space-6));
  }

  .profile__grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .profile__qr-svg {
    width: 240px;
    height: 240px;
  }
}
</style>
