<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCoupons } from '@/api/coupon'
import { getWallet } from '@/api/wallet'

/**
 * 个人中心 v5（M1-6 批次 4 重构）：
 * 首页只保留四个区块，全部正常文档流（无 absolute 覆盖）：
 * ① 用户信息头（点击进 /profile/info）② 双数据卡（优惠券→/profile/coupons、
 * 钱包余额→/wallet/recharge，真接口）③ 会员卡（右上角积分详情→/profile/points）
 * ④ 四宫格。积分/优惠券详情迁至独立页 /profile/points、/profile/coupons。
 * 布局断点：移动端单列堆叠；≥768px 顶部区两栏（用户信息头在左、双数据卡在右）。
 */
const store = useUserStore()

const walletBalance = ref<number | null>(null)
const couponCount = ref<number | null>(null)

const wechatVisible = ref(false)

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
    <!-- 区块 ①+②：顶部区（Grid 断点：移动端单列堆叠 / ≥768px 用户信息头在左、双数据卡在右） -->
    <div class="profile__top">
      <!-- 区块 ① 用户信息头：整行可点进 /profile/info -->
      <RouterLink to="/profile/info" class="profile__header" aria-label="查看个人资料">
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

      <!-- 区块 ② 双数据卡：两列等宽，点击跳详情页 -->
      <div class="profile__stats">
        <RouterLink to="/profile/coupons" class="profile__stat" aria-label="未使用优惠券">
          <span class="profile__stat-icon profile__stat-icon--coupon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z" />
              <path d="M16 8H8" />
            </svg>
          </span>
          <span class="profile__stat-value">{{ couponCount ?? '--' }}</span>
          <span class="profile__stat-label">优惠券</span>
        </RouterLink>
        <RouterLink to="/wallet/recharge" class="profile__stat" aria-label="钱包余额与充值">
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
        </RouterLink>
      </div>
    </div>

    <!-- 区块 ③ 会员卡：右上角积分详情入口，其余不变 -->
    <section class="member-card">
      <div class="member-card__head">
        <span class="member-card__level">
          <span class="member-card__level-dot" aria-hidden="true"></span>
          {{ store.memberLevelLabel }}
        </span>
        <RouterLink to="/profile/points" class="member-card__detail">
          积分详情
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M9 18l6-6-6-6" />
          </svg>
        </RouterLink>
      </div>

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

    <!-- 区块 ④ 四宫格入口 -->
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

/* ===== 顶部区：Grid 断点重排（移动端单列 / ≥768px 用户信息头在左、双数据卡在右） ===== */
.profile__top {
  display: grid;
  grid-template-columns: 1fr; /* 移动端：用户信息头整行 → 双数据卡整行（内部两列等宽） */
  gap: var(--ac-space-3);
  align-items: center;
}

/* 区块 ① 用户信息头：内部 Flex 水平排列（头像/文字/徽标），整行可点 */
.profile__header {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-4) var(--ac-space-2);
  cursor: pointer;
}

.profile__identity {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  flex: 1;
  min-width: 0;
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

.profile__header:hover .profile__identity-chevron {
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

/* 区块 ② 双数据卡：两列等宽 Grid，可点击跳详情 */
.profile__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 移动端两列等宽（Flex: 1 1 0 等效） */
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
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-md);
  }
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

/* ===== 区块 ③ 会员卡 ===== */
.member-card {
  position: relative;
  overflow: hidden;
  padding: var(--ac-space-5);
  color: #fff;
  background: linear-gradient(140deg, #0c4a6e 0%, #0369a1 52%, #0e7ab8 100%);
  border-radius: var(--ac-radius-overlay);
  box-shadow: var(--ac-shadow-lg);
}

.member-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ac-space-2);
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

/* 积分详情入口：晴空蓝点缀 */
.member-card__detail {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 500;
  color: #7dd3fc;
  transition: opacity var(--ac-dur-fast) var(--ac-ease-enter);

  svg {
    width: 13px;
    height: 13px;
  }

  &:hover {
    opacity: 0.8;
  }
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

/* ===== 区块 ④ 四宫格 ===== */
.profile__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 移动端 2×2 换行 */
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

/* ===== PC（≥768px）：顶部两栏 + 四宫格一行 ===== */
@media (min-width: 768px) {
  .profile {
    max-width: 1080px;
    padding-bottom: var(--ac-space-8);
  }

  .profile__top {
    grid-template-columns: minmax(0, 1fr) auto; /* 用户信息头在左、双数据卡在右 */
    gap: var(--ac-space-6);
  }

  .profile__stats {
    min-width: 400px;
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
