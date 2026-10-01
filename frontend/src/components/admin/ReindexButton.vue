<script setup lang="ts">
import { ref } from 'vue'

/**
 * 向量重建按钮（04 §5.2 AdminKbPage ── ReindexButton）：
 * 静态版模拟——点击后 2s 假重建，完成抛 reindexed 由页面层演示状态门
 * （草稿置 PUBLISHED）；真实重建接口属 M2（docs/06 §8 批次 6）。
 */
const emit = defineEmits<{ reindexed: [] }>()

const loading = ref(false)

function reindex() {
  if (loading.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    emit('reindexed')
  }, 2000)
}
</script>

<template>
  <el-button :loading="loading" @click="reindex">重建向量索引</el-button>
</template>
