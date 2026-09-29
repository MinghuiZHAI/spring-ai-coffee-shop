<script setup lang="ts">
/** 状态徽标（订单状态/退款状态共用）：label + 色调；lavender 淡薰衣草紫用于区分"待取餐" */
defineProps<{
  label: string
  tone: 'warning' | 'primary' | 'success' | 'muted' | 'lavender'
}>()
</script>

<template>
  <span class="status-chip" :class="`status-chip--${tone}`">{{ label }}</span>
</template>

<style scoped lang="scss">
.status-chip {
  display: inline-flex;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--ac-radius-pill);

  &--primary {
    color: var(--ac-primary);
    background: var(--ac-tide);
  }

  &--warning {
    color: var(--ac-cta);
    background: #fdf0dc;
  }

  &--success {
    color: var(--ac-success);
    background: #e7f6ef;
  }

  &--muted {
    color: var(--ac-text-dim);
    background: var(--ac-bg);
    border: 1px solid var(--ac-border);
  }

  /* 淡薰衣草紫：仅"待取餐"，与"待制作/制作中"的浅潮蓝区分（用户指定） */
  &--lavender {
    color: #6d28d9;
    background: #ede9fe;
  }
}
</style>
