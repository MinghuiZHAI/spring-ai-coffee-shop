<script setup lang="ts">
import { computed, ref } from 'vue'
import type { KbDoc, KbStatus, KbType } from '@/data/kbDocs'
import { KB_STATUS_LABELS, KB_TYPE_LABELS } from '@/data/kbDocs'

/**
 * 知识库文档表格（04 §5.2 AdminKbPage ── KbDocTable）：类型/状态筛选 + 标题搜索，
 * 行操作（编辑/删除）向页面层抛出——数据变更由页面层的 mock 状态承接（批次 6 静态版）。
 */
const props = defineProps<{ docs: KbDoc[] }>()

const emit = defineEmits<{ edit: [doc: KbDoc]; remove: [doc: KbDoc] }>()

const typeFilter = ref<KbType | ''>('')
const statusFilter = ref<KbStatus | ''>('')
const keyword = ref('')

const filtered = computed(() =>
  props.docs.filter(
    (d) =>
      (typeFilter.value === '' || d.kbType === typeFilter.value) &&
      (statusFilter.value === '' || d.status === statusFilter.value) &&
      (keyword.value.trim() === '' || d.title.includes(keyword.value.trim())),
  ),
)

const typeOptions = Object.entries(KB_TYPE_LABELS).map(([value, label]) => ({ value, label }))
const statusOptions = Object.entries(KB_STATUS_LABELS).map(([value, label]) => ({ value, label }))
</script>

<template>
  <div class="kb-table">
    <div class="kb-table__filters">
      <el-select v-model="typeFilter" placeholder="全部类型" clearable class="kb-table__select">
        <el-option
          v-for="opt in typeOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable class="kb-table__select">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-input
        v-model="keyword"
        class="kb-table__search"
        placeholder="搜索文档标题"
        clearable
      />
      <span class="kb-table__count">{{ filtered.length }} 篇</span>
    </div>

    <el-table :data="filtered" class="kb-table__grid" empty-text="没有符合条件的知识库文档">
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <span class="kb-tag kb-tag--type">{{ KB_TYPE_LABELS[row.kbType as KbType] }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span
            class="kb-tag"
            :class="row.status === 'PUBLISHED' ? 'kb-tag--published' : 'kb-tag--draft'"
          >
            {{ KB_STATUS_LABELS[row.status as KbStatus] }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="80" align="center">
        <template #default="{ row }">v{{ row.version }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="150" />
      <el-table-column label="操作" width="140" align="right">
        <template #default="{ row }">
          <button class="kb-row-btn" type="button" @click="emit('edit', row as KbDoc)">编辑</button>
          <button
            class="kb-row-btn kb-row-btn--danger"
            type="button"
            @click="emit('remove', row as KbDoc)"
          >
            删除
          </button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.kb-table__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.kb-table__select {
  width: 130px;
}

.kb-table__search {
  width: 220px;
}

.kb-table__count {
  margin-left: auto;
  font-size: 13px;
  color: var(--ac-text-dim);
  font-variant-numeric: tabular-nums;
}

.kb-table__grid {
  width: 100%;
}

.kb-tag {
  display: inline-block;
  font-size: 12px;
  border-radius: var(--ac-radius-pill);
  padding: 2px 10px;
  background: var(--ac-tide);
  color: var(--ac-primary-deep);
}

.kb-tag--published {
  background: var(--ac-tide);
  color: var(--ac-success);
}

.kb-tag--draft {
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  color: var(--ac-text-dim);
}

.kb-row-btn {
  border: none;
  background: transparent;
  color: var(--ac-primary);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;

  &:hover {
    text-decoration: underline;
  }

  &--danger {
    color: var(--ac-danger);

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
