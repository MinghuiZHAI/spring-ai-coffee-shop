<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPoints, type PointRecord } from '@/api/user'

/**
 * 积分详情（/profile/points，M1-6 批次 4）：
 * 自 ProfilePage Tab 迁移而来，复用原 UI 与数据源（GET /api/user/points）。
 * 当前积分大字 + 流水列表 + 空态引导 + 底部版本提示。
 */
const router = useRouter()

const balance = ref<number | null>(null)
const records = ref<PointRecord[]>([])
const loading = ref(true)

const POINT_EMPTY_HINT = '下单即可获得积分，1 元 = 10 积分'
const POINT_USAGE_NOTE = '当前版本支持积分累积与查询，抵扣与兑换将在后续版本上线'

onMounted(async () => {
  try {
    const res = await getPoints()
    balance.value = res.balance
    records.value = res.records
  } catch {
    balance.value = null
  } finally {
    loading.value = false
  }
})

function formatDateTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function onGoMenu() {
  router.push('/menu')
}
</script>

<template>
  <div class="points">
    <header class="points__head">
      <RouterLink to="/profile" class="points__back" aria-label="返回个人中心">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回个人中心
      </RouterLink>
    </header>

    <section class="points__card">
      <span class="points__card-label">当前积分</span>
      <span class="points__card-value">{{ balance ?? '--' }}</span>
    </section>

    <section class="points__body">
      <template v-if="loading">
        <div v-for="n in 4" :key="n" class="skeleton skeleton--row"></div>
      </template>

      <template v-else>
        <div v-if="records.length === 0" class="points__empty">
          <p class="points__empty-hint">{{ POINT_EMPTY_HINT }}</p>
          <el-button class="el-button--cta" size="small" @click="onGoMenu">去菜单点单</el-button>
        </div>

        <ul v-else class="points__records">
          <li v-for="(record, index) in records" :key="index" class="points__record">
            <div class="points__record-info">
              <span class="points__record-type">获得</span>
              <span class="points__record-order">订单 #{{ record.relatedOrderId }}</span>
            </div>
            <span class="points__record-change">+{{ record.changeValue }}</span>
            <span class="points__record-time">{{ formatDateTime(record.createdAt) }}</span>
          </li>
        </ul>

        <p class="points__usage">{{ POINT_USAGE_NOTE }}</p>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.points {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: calc(var(--ac-tabbar-h) + var(--ac-space-6));
}

.points__head {
  margin-bottom: var(--ac-space-4);
}

.points__back {
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

/* 积分大字卡：深海蓝渐变，与会员卡同一视觉语言 */
.points__card {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-2);
  padding: var(--ac-space-6) var(--ac-space-5);
  margin-bottom: var(--ac-space-4);
  color: #fff;
  background: linear-gradient(140deg, #0c4a6e 0%, #0369a1 52%, #0e7ab8 100%);
  border-radius: var(--ac-radius-overlay);
  box-shadow: var(--ac-shadow-lg);
}

.points__card-label {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.72);
}

.points__card-value {
  font-family: var(--ac-font-display);
  font-size: 44px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.points__body {
  padding: var(--ac-space-2) var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.points__records {
  margin: 0;
  padding: 0;
  list-style: none;
}

.points__record {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-4) 0;

  & + & {
    border-top: 1px solid var(--ac-border);
  }
}

.points__record-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.points__record-type {
  font-size: 14px;
  font-weight: 500;
  color: var(--ac-primary-deep);
}

.points__record-order {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points__record-change {
  font-family: var(--ac-font-display);
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-success);
}

.points__record-time {
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points__usage {
  margin: var(--ac-space-4) 0 0;
  font-size: 11px;
  color: var(--ac-text-dim);
}

.points__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-8) 0;
}

.points__empty-hint {
  margin: 0;
  font-size: 14px;
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
  height: 52px;
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
