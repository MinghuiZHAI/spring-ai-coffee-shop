<script setup lang="ts">
/**
 * 页面占位（M1-6 分步实施的过渡态）：波浪徽章 + 标题 + 一句说明。
 * 各页面完成开发后由真实内容整体替换。
 */
defineProps<{
  title: string
  hint: string
}>()
</script>

<template>
  <section class="placeholder">
    <span class="placeholder__badge" aria-hidden="true">
      <svg width="30" height="30" viewBox="0 0 32 32" fill="none">
        <path
          d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0"
          stroke="var(--ac-primary)"
          stroke-width="2.2"
          stroke-linecap="round"
        />
        <path
          d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0"
          stroke="var(--ac-sky)"
          stroke-width="2"
          stroke-linecap="round"
        />
      </svg>
    </span>
    <h2 class="placeholder__title">{{ title }}</h2>
    <p class="placeholder__hint">{{ hint }}</p>
  </section>
</template>

<style scoped lang="scss">
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ac-space-3);
  min-height: 60vh;
  text-align: center;
}

.placeholder__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: var(--ac-radius-pill);
  background: var(--ac-tide);
}

.placeholder__title {
  font-size: 20px;
  font-weight: 600;
}

.placeholder__hint {
  margin: 0;
  font-size: 14px;
  color: var(--ac-text-dim);
}
</style>
