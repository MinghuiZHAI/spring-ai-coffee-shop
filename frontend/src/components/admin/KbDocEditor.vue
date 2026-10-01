<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { KbDoc, KbType } from '@/data/kbDocs'
import { KB_TYPE_LABELS } from '@/data/kbDocs'
import { renderMarkdown } from '@/utils/markdown'

/**
 * 知识库文档编辑抽屉（04 §5.2 AdminKbPage ── KbDocEditor）：
 * 编辑/新建双模式（新建由页面预填类型骨架），左侧 Markdown 源文、右侧实时预览
 * （站内统一渲染管线 utils/markdown，与 AI 客服气泡同口径）。
 * 工作副本由本组件持有（props.doc 视为只读入参），保存时整体上抛，版本/状态由页面层维护。
 */
const props = defineProps<{
  open: boolean
  mode: 'edit' | 'create'
  doc: KbDoc
}>()

const emit = defineEmits<{
  close: []
  save: [draft: KbDoc, status: KbDoc['status']]
}>()

const form = reactive({ title: '', kbType: 'FAQ' as KbType, content: '' })

watch(
  () => props.open,
  (open) => {
    if (!open) return
    form.title = props.doc.title
    form.kbType = props.doc.kbType
    form.content = props.doc.content
  },
)

const previewHtml = computed(() => renderMarkdown(form.content))

const canSave = computed(() => form.title.trim() !== '' && form.content.trim() !== '')

function close() {
  emit('close')
}

function save(status: KbDoc['status']) {
  if (!canSave.value) return
  emit('save', { ...props.doc, title: form.title.trim(), kbType: form.kbType, content: form.content }, status)
}
</script>

<template>
  <el-drawer
    :model-value="open"
    :title="mode === 'create' ? '新建文档' : '编辑文档'"
    size="720px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => !v && close()"
  >
    <div class="kb-editor">
      <div class="kb-editor__meta">
        <el-input v-model="form.title" placeholder="文档标题（必填）" maxlength="100" />
        <el-select v-model="form.kbType" class="kb-editor__type">
          <el-option
            v-for="(label, value) in KB_TYPE_LABELS"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <span v-if="mode === 'edit'" class="kb-editor__version">v{{ props.doc.version }}</span>
      </div>

      <div class="kb-editor__split">
        <div class="kb-editor__pane">
          <p class="kb-editor__pane-label">Markdown 源文</p>
          <textarea
            v-model="form.content"
            class="kb-editor__textarea"
            placeholder="输入 Markdown 内容（必填）"
          ></textarea>
        </div>
        <div class="kb-editor__pane">
          <p class="kb-editor__pane-label">实时预览</p>
          <div class="kb-editor__preview" v-html="previewHtml"></div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="kb-editor__footer">
        <span class="kb-editor__hint" :class="{ 'is-disabled': !canSave }">
          {{ canSave ? '状态门：发布后向量索引按 PUBLISHED 口径重建' : '标题与内容必填' }}
        </span>
        <div class="kb-editor__actions">
          <el-button @click="close">取消</el-button>
          <el-button :disabled="!canSave" @click="save('DRAFT')">保存草稿</el-button>
          <el-button type="primary" :disabled="!canSave" @click="save('PUBLISHED')">发布</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.kb-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.kb-editor__meta {
  display: flex;
  align-items: center;
  gap: 10px;

  .el-input {
    flex: 1;
  }
}

.kb-editor__type {
  width: 130px;
}

.kb-editor__version {
  font-size: 12px;
  color: var(--ac-text-dim);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.kb-editor__split {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.kb-editor__pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  overflow: hidden;
  background: var(--ac-card);
}

.kb-editor__pane-label {
  margin: 0;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--ac-text-dim);
  border-bottom: 1px solid var(--ac-border);
  background: var(--ac-bg);
}

.kb-editor__textarea {
  flex: 1;
  min-height: 0;
  border: none;
  resize: none;
  padding: 12px;
  font-family: var(--ac-font-body);
  font-size: 13px;
  line-height: 1.7;
  color: var(--ac-text);
  background: transparent;
  outline: none;
}

.kb-editor__preview {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ac-text);

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 10px 0 6px;
    color: var(--ac-primary-deep);
  }

  :deep(h1) {
    font-size: 16px;
  }

  :deep(h2) {
    font-size: 15px;
  }

  :deep(h3) {
    font-size: 14px;
  }

  :deep(ul),
  :deep(ol) {
    margin: 4px 0;
    padding-left: 18px;
  }

  :deep(p) {
    margin: 6px 0;
  }

  :deep(code) {
    font-size: 12px;
    background: var(--ac-tide);
    border-radius: 4px;
    padding: 1px 5px;
  }
}

.kb-editor__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.kb-editor__hint {
  font-size: 12px;
  color: var(--ac-text-dim);

  &.is-disabled {
    color: var(--ac-warning);
  }
}

.kb-editor__actions {
  display: flex;
  gap: 8px;
}

/* 移动端：抽屉全宽、分栏改上下（后台工具桌面优先，保证不崩即可） */
@media (max-width: 767px) {
  :deep(.el-drawer) {
    width: 100% !important;
  }

  .kb-editor__split {
    grid-template-columns: 1fr;
  }
}
</style>
