<script setup lang="ts">
/**
 * /chat 路由宿主（批次 5 修正 2）：移动端全屏聊天由 AppLayout 内的
 * ChatWidget 以 route.name === 'chat' 判定挂载为全屏形态；本页不承载 UI。
 */
</script>

<template>
  <div class="chat-page" aria-hidden="true"></div>
</template>

<style scoped>
.chat-page {
  min-height: 40vh;
}
</style>
