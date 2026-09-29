<script setup lang="ts">
/** 状态徽标（订单状态/退款状态共用）：label + 色调（warning/primary/success/muted） */
defineProps<{
  label: string
  tone: 'warning' | 'primary' | 'success' | 'muted'
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
}
</style>
