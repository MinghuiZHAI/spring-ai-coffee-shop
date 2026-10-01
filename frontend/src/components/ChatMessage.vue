<script setup lang="ts">
import { computed } from 'vue'
import type { ChatDisplayMessage } from '@/stores/chat'
import { toolLabel } from '@/api/chat'
import { renderMarkdown } from '@/utils/markdown'

/**
 * 聊天气泡（04-详细设计 v1.6 §5.4）：role 四分支渲染——SYSTEM 居中提示条、
 * USER 右侧、AI/AGENT 左侧（AGENT 独立 class，MVP 样式与 AI 相同，M3 只改样式不改结构）。
 * AI 内容走站内统一 Markdown 管线（utils/markdown：html:false + DOMPurify，防提示注入）；
 * 流式中的气泡带光标；历史/本次会话的工具调用摘要以 chip 展示。
 */
const props = defineProps<{ msg: ChatDisplayMessage }>()

const html = computed(() => renderMarkdown(props.msg.content))
</script>

<template>
  <!-- SYSTEM：居中系统提示条 -->
  <div v-if="msg.role === 'SYSTEM'" class="msg-system">{{ msg.content }}</div>

  <!-- USER：右侧气泡（纯文本插值，不渲染 markdown） -->
  <div v-else-if="msg.role === 'USER'" class="msg-row msg-row--user">
    <div class="bubble bubble--user">{{ msg.content }}</div>
  </div>

  <!-- AI / AGENT：左侧气泡 -->
  <div v-else class="msg-row msg-row--bot">
    <div class="bubble-col">
      <div v-if="msg.toolCalls?.length" class="tool-summary">
        <span v-for="t in msg.toolCalls" :key="t.toolCallId ?? t.tool" class="tool-summary__chip">
          {{ toolLabel(t.tool) }}{{ t.success === false ? ' · 未完成' : '' }}
        </span>
      </div>
      <div class="bubble bubble--ai" :class="{ 'bubble--agent': msg.role === 'AGENT' }">
        <div class="bubble__md" v-html="html"></div>
        <span v-if="msg.streaming" class="cursor" aria-hidden="true">▍</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.msg-system {
  align-self: center;
  max-width: 90%;
  font-size: 12px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border-radius: var(--ac-radius-pill);
  padding: 4px 12px;
  text-align: center;
  word-break: break-word;
}

.msg-row {
  display: flex;
}

.msg-row--user {
  justify-content: flex-end;
}

.msg-row--bot {
  justify-content: flex-start;
}

.bubble-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 84%;
  min-width: 0;
}

.bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.bubble--user {
  background: var(--ac-primary);
  color: #fff;
  border-radius: 14px 14px 4px 14px;
  white-space: pre-wrap;
}

.bubble--ai {
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  color: var(--ac-text);
  border-radius: 14px 14px 14px 4px;
}

/* AGENT 人工客服：结构保留，M3 仅调整此样式（决策 #18 预留） */
.bubble--agent {
  border-color: var(--ac-sky);
}

.bubble__md {
  :deep(p) {
    margin: 0 0 6px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(ul),
  :deep(ol) {
    margin: 4px 0;
    padding-left: 18px;
  }

  :deep(code) {
    font-size: 13px;
    background: var(--ac-tide);
    border-radius: 4px;
    padding: 1px 5px;
  }

  :deep(a) {
    color: var(--ac-primary);
  }

  :deep(table) {
    border-collapse: collapse;

    th,
    td {
      border: 1px solid var(--ac-border);
      padding: 4px 8px;
      font-size: 13px;
    }
  }
}

.cursor {
  display: inline-block;
  margin-left: 1px;
  color: var(--ac-primary);
  animation: msg-blink 0.9s step-end infinite;
}

@keyframes msg-blink {
  50% {
    opacity: 0;
  }
}

/* 工具调用摘要 chip（历史消息与流结束后展示） */
.tool-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tool-summary__chip {
  font-size: 12px;
  color: var(--ac-text-dim);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
  padding: 3px 10px;
}
</style>
