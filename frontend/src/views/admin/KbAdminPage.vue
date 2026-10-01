<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import KbDocTable from '@/components/admin/KbDocTable.vue'
import KbDocEditor from '@/components/admin/KbDocEditor.vue'
import ReindexButton from '@/components/admin/ReindexButton.vue'
import type { KbDoc, KbStatus } from '@/data/kbDocs'
import { KB_DOCS, draftSkeleton } from '@/data/kbDocs'

/**
 * 知识库管理后台（批次 6 静态版）：/admin/kb 为 AppLayout 之外的独立路由（ADMIN 专属，
 * 登录按 role 跳转/守卫校验），页内自带极简管理头——轻差异化：品牌色不变，仅以
 * 「内部管理」徽标与返回门店入口标识内部工具属性。
 * 数据为 V2 种子镜像 mock，工作副本由页面持有：编辑/新建/删除/发布均为本地变更；
 * 真实 CRUD 与向量重建属 M2（docs/06 §8 批次 6）。
 */
const docs = ref<KbDoc[]>([...KB_DOCS])

const publishedCount = computed(() => docs.value.filter((d) => d.status === 'PUBLISHED').length)
const draftCount = computed(() => docs.value.filter((d) => d.status === 'DRAFT').length)

const editorOpen = ref(false)
const editorMode = ref<'edit' | 'create'>('edit')
const editingDoc = ref<KbDoc>(KB_DOCS[0])

/** 本地时间串（mock updatedAt 用；与后端 LocalDateTime 同为无时区口径） */
function nowLabel(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function onEdit(doc: KbDoc) {
  editingDoc.value = { ...doc }
  editorMode.value = 'edit'
  editorOpen.value = true
}

function onCreate() {
  editingDoc.value = {
    id: 0,
    title: '',
    kbType: 'MENU',
    content: draftSkeleton('MENU'),
    status: 'DRAFT',
    version: 1,
    updatedAt: nowLabel(),
  }
  editorMode.value = 'create'
  editorOpen.value = true
}

/** 保存（状态门口径：重建成功才置 PUBLISHED——静态版以「发布」动作演示；版本号 +1） */
function onSave(draft: KbDoc, status: KbStatus) {
  if (draft.id === 0) {
    const nextId = Math.max(0, ...docs.value.map((d) => d.id)) + 1
    docs.value = [{ ...draft, id: nextId, status, updatedAt: nowLabel() }, ...docs.value]
    ElMessage.success(`已创建「${draft.title}」并${status === 'PUBLISHED' ? '发布' : '存为草稿'}（静态版演示）`)
  } else {
    const target = docs.value.find((d) => d.id === draft.id)
    if (target === undefined) return
    const saved = { ...draft, status, version: target.version + 1, updatedAt: nowLabel() }
    docs.value = docs.value.map((d) => (d.id === draft.id ? saved : d))
    ElMessage.success(`已保存「${saved.title}」v${saved.version}（${status === 'PUBLISHED' ? '已发布' : '草稿'}）`)
  }
  editorOpen.value = false
}

async function onRemove(doc: KbDoc) {
  const ok = await ElMessageBox.confirm(`删除「${doc.title}」？该操作不可恢复（静态版演示）`, '删除文档', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => true)
    .catch(() => false)
  if (!ok) return
  docs.value = docs.value.filter((d) => d.id !== doc.id)
  ElMessage.success('已删除（静态版演示）')
}

/** 重建成功 → 状态门演示：草稿按"重建成功才置发布"口径置 PUBLISHED（静态版模拟） */
function onReindexed() {
  let flipped = 0
  docs.value = docs.value.map((d) => {
    if (d.status !== 'DRAFT') return d
    flipped++
    return { ...d, status: 'PUBLISHED' }
  })
  ElMessage.success(
    flipped > 0
      ? `向量索引已重建（静态版模拟，真实接口属 M2），${flipped} 篇草稿已按状态门置为发布`
      : '向量索引已重建（静态版模拟，真实接口属 M2）',
  )
}
</script>

<template>
  <div class="kb-admin">
    <!-- 极简管理头：轻差异化（品牌色不变 + 内部管理徽标） -->
    <header class="kb-admin__header">
      <div class="kb-admin__brand">
        <svg class="kb-admin__mark" viewBox="0 0 32 32" aria-hidden="true">
          <circle cx="16" cy="16" r="16" fill="var(--ac-primary)" />
          <path
            d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0"
            fill="none"
            stroke="#fff"
            stroke-width="2.2"
            stroke-linecap="round"
            opacity=".95"
          />
          <path
            d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0"
            fill="none"
            stroke="#7fd1f5"
            stroke-width="2"
            stroke-linecap="round"
            opacity=".9"
          />
        </svg>
        <span class="kb-admin__name">Atlantic Coffee</span>
        <span class="kb-admin__badge">内部管理 · 知识库</span>
      </div>
      <div class="kb-admin__actions">
        <ReindexButton @reindexed="onReindexed" />
        <RouterLink to="/menu" class="kb-admin__back">← 返回门店</RouterLink>
      </div>
    </header>

    <main class="kb-admin__main">
      <!-- 统计条 + 新建入口 -->
      <div class="kb-admin__stats">
        <div class="kb-stat">
          <span class="kb-stat__num">{{ docs.length }}</span>
          <span class="kb-stat__label">全部文档</span>
        </div>
        <div class="kb-stat">
          <span class="kb-stat__num kb-stat__num--published">{{ publishedCount }}</span>
          <span class="kb-stat__label">已发布</span>
        </div>
        <div class="kb-stat">
          <span class="kb-stat__num kb-stat__num--draft">{{ draftCount }}</span>
          <span class="kb-stat__label">草稿</span>
        </div>
        <el-button class="kb-admin__create" type="primary" @click="onCreate">新建文档</el-button>
      </div>

      <KbDocTable :docs="docs" @edit="onEdit" @remove="onRemove" />
    </main>

    <KbDocEditor
      :open="editorOpen"
      :mode="editorMode"
      :doc="editingDoc"
      @close="editorOpen = false"
      @save="onSave"
    />
  </div>
</template>

<style scoped lang="scss">
.kb-admin {
  min-height: 100vh;
  background: var(--ac-bg);
}

/* ===== 管理头 ===== */
.kb-admin__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 24px;
  background: var(--ac-card);
  border-bottom: 2px solid var(--ac-primary);
}

.kb-admin__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kb-admin__mark {
  width: 26px;
  height: 26px;
}

.kb-admin__name {
  font-family: var(--ac-font-display);
  font-weight: 600;
  font-size: 15px;
  color: var(--ac-primary-deep);
}

.kb-admin__badge {
  font-size: 12px;
  color: var(--ac-primary-deep);
  background: var(--ac-tide);
  border: 1px solid var(--ac-sky);
  border-radius: var(--ac-radius-pill);
  padding: 2px 12px;
}

.kb-admin__back {
  font-size: 13px;
  color: var(--ac-text-dim);
  text-decoration: none;

  &:hover {
    color: var(--ac-primary);
  }
}

.kb-admin__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ===== 主体 ===== */
.kb-admin__main {
  max-width: var(--ac-content-max);
  margin: 0 auto;
  padding: 20px 24px 40px;
}

.kb-admin__stats {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.kb-stat {
  display: flex;
  align-items: baseline;
  gap: 8px;
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  padding: 10px 18px;

  &__num {
    font-family: var(--ac-font-display);
    font-size: 22px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--ac-text);

    &--published {
      color: var(--ac-success);
    }

    &--draft {
      color: var(--ac-warning);
    }
  }

  &__label {
    font-size: 12px;
    color: var(--ac-text-dim);
  }
}

.kb-admin__create {
  margin-left: auto;
}
</style>
