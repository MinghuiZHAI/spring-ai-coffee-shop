<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getWallet, recharge } from '@/api/wallet'

/**
 * 余额充值页（/wallet/recharge，M1-6 批次 4）：
 * 余额卡（深海蓝渐变）+ 金额预设/自定义输入 + 模拟充值（POST /api/user/wallet/recharge）。
 * MVP 范围（01 v1.3 追加）：单次 1~1000 元，不做真实支付/余额抵扣/提现。
 */
const balance = ref<number | null>(null)
const amount = ref<string>('')
const loading = ref(false)

const PRESETS = [50, 100, 200, 500] as const

const amountValue = computed(() => Number(amount.value))

async function refreshBalance() {
  try {
    balance.value = (await getWallet()).balance
  } catch {
    balance.value = null
  }
}

onMounted(refreshBalance)

function selectPreset(value: number) {
  amount.value = String(value)
}

async function onRecharge() {
  const value = amountValue.value
  if (!Number.isFinite(value) || value < 1 || value > 1000) {
    ElMessage.error('单次充值金额需在 1~1000 元之间')
    return
  }
  loading.value = true
  try {
    const res = await recharge(Number(value.toFixed(2)))
    balance.value = res.balance
    amount.value = ''
    ElMessage.success(`充值成功，当前余额 ¥${res.balance}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '充值失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="recharge">
    <header class="recharge__head">
      <RouterLink to="/profile" class="recharge__back" aria-label="返回个人中心">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回个人中心
      </RouterLink>
    </header>

    <!-- 余额卡：深海蓝渐变 + 弧线母题（与会员卡同一视觉语言） -->
    <section class="recharge__balance-card">
      <span class="recharge__balance-label">当前余额（元）</span>
      <span class="recharge__balance-value">{{ balance ?? '--' }}</span>
      <svg class="recharge__waves" viewBox="0 0 320 48" fill="none" preserveAspectRatio="none" aria-hidden="true">
        <path d="M-10 30c30-12 60-12 90 0s60 12 90 0 60-12 90 0 60 12 70 6" stroke="rgba(255,255,255,0.2)" stroke-width="2.2" stroke-linecap="round" />
        <path d="M-10 40c30-12 60-12 90 0s60 12 90 0 60-12 90 0 60 12 70 6" stroke="rgba(125,211,252,0.38)" stroke-width="2" stroke-linecap="round" />
      </svg>
    </section>

    <section class="recharge__form">
      <h3 class="recharge__title">充值金额</h3>
      <div class="recharge__presets" role="group" aria-label="充值金额预设">
        <button
          v-for="preset in PRESETS"
          :key="preset"
          type="button"
          class="recharge__preset"
          :class="{ 'is-active': amountValue === preset }"
          @click="selectPreset(preset)"
        >
          ¥{{ preset }}
        </button>
      </div>
      <el-input
        v-model="amount"
        class="recharge__input"
        placeholder="自定义金额（1~1000）"
        inputmode="decimal"
        maxlength="8"
        clearable
      />
      <p class="recharge__hint">模拟充值，单次 1~1000 元；不做真实支付，余额抵扣与提现将后续上线</p>
      <el-button
        class="el-button--cta recharge__submit"
        size="large"
        :loading="loading"
        @click="onRecharge"
      >
        立即充值
      </el-button>
    </section>
  </div>
</template>

<style scoped lang="scss">
.recharge {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: var(--ac-space-8);
}

.recharge__head {
  margin-bottom: var(--ac-space-4);
}

.recharge__back {
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

/* 余额卡：与会员卡同一渐变语言 */
.recharge__balance-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-2);
  padding: var(--ac-space-6) var(--ac-space-5);
  color: #fff;
  background: linear-gradient(140deg, #0c4a6e 0%, #0369a1 52%, #0e7ab8 100%);
  border-radius: var(--ac-radius-overlay);
  box-shadow: var(--ac-shadow-lg);
}

.recharge__balance-label {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.72);
}

.recharge__balance-value {
  font-family: var(--ac-font-display);
  font-size: 40px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.recharge__waves {
  position: absolute;
  inset: auto 0 -2px 0;
  width: 100%;
  height: 48px;
  pointer-events: none;
}

.recharge__form {
  margin-top: var(--ac-space-5);
}

.recharge__title {
  margin-bottom: var(--ac-space-3);
  font-size: 15px;
  font-weight: 600;
}

.recharge__presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--ac-space-3);
  margin-bottom: var(--ac-space-3);
}

.recharge__preset {
  padding: var(--ac-space-4) 0;
  font-family: var(--ac-font-display);
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
  background: var(--ac-card);
  border: 1.5px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  cursor: pointer;
  transition:
    border-color var(--ac-dur-fast) var(--ac-ease-enter),
    background-color var(--ac-dur-fast) var(--ac-ease-enter);

  &.is-active {
    color: var(--ac-primary-deep);
    background: var(--ac-tide);
    border-color: var(--ac-primary);
  }
}

.recharge__hint {
  margin: var(--ac-space-3) 0 var(--ac-space-5);
  font-size: 11px;
  line-height: 1.5;
  color: var(--ac-text-dim);
}

.recharge__submit {
  width: 100%;
}
</style>
