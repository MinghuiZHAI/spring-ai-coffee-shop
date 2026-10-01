<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatMessage from '@/components/ChatMessage.vue'
import { useChatStore } from '@/stores/chat'
import type { ChatSessionView } from '@/api/chat'
import { CHAT_CONTENT_MAX, toolLabel } from '@/api/chat'

/**
 * AI 客服唯一组件（批次 5，修正 2 形态收敛）：
 * - PC（≥768px）：悬浮球 + 右侧玻璃抽屉，开合走 chatStore.open；
 * - 移动端（≤767px）：不挂悬浮球（修正 1），面板由 /chat 路由挂载为全屏，关闭即回 /menu。
 * 两形态共用同一段模板与样式，仅 CSS 断点切换。
 * 消息渲染走 ChatMessage（role 四分支 + markdown）；流式增量/工具状态/错误重试由 chat store 驱动。
 */
const chatStore = useChatStore()
const route = useRoute()
const router = useRouter()

const panelVisible = computed(() => chatStore.open || route.name === 'chat')

/** 输入草稿：Enter 发送 / Shift+Enter 换行；maxlength 与后端 @Size(max=500) 对齐 */
const draft = ref('')
const bodyEl = ref<HTMLElement | null>(null)

const canSend = computed(
  () =>
    draft.value.trim().length > 0 &&
    draft.value.length <= CHAT_CONTENT_MAX &&
    !chatStore.streaming &&
    !chatStore.isEnded,
)

/** 历史会话面板开合；每次打开刷新列表（列表按 updated_at 倒序，当前会话在顶部） */
const historyOpen = ref(false)

function toggleHistory() {
  historyOpen.value = !historyOpen.value
  if (historyOpen.value) {
    void chatStore.loadSessions(true)
  }
}

/** 时间展示：MM-DD HH:mm（后端 LocalDateTime 无时区，直接切片） */
function formatTime(v: string | undefined): string {
  return (v ?? '').slice(5, 16).replace('T', ' ')
}

/** 列表标题兜底：title 由首条用户消息生成（≤20 字），尚未发言的新会话为空 */
function sessionTitle(s: ChatSessionView): string {
  const t = (s.title ?? '').trim()
  return t === '' ? '新会话' : t
}

async function onSwitchSession(s: ChatSessionView) {
  historyOpen.value = false
  await chatStore.switchSession(s.id)
}

function onNewSession() {
  historyOpen.value = false
  chatStore.newSession()
}

async function onEndSession(s: ChatSessionView) {
  try {
    await chatStore.endSession(s.id)
  } catch {
    // close 幂等；网络失败不阻塞 UI，后续操作会重新同步
  }
}

/** 加载更早消息：拼接后保持视口锚点（内容向上长，不跳底） */
async function onLoadMoreHistory() {
  const el = bodyEl.value
  const before = el?.scrollHeight ?? 0
  await chatStore.loadMoreHistory()
  await nextTick()
  if (el) el.scrollTop += el.scrollHeight - before
}

function onSend() {
  if (!canSend.value) return
  const content = draft.value
  draft.value = ''
  void chatStore.send(content)
}

/** 中文输入法组词态的回车是选词，不发送（e.isComposing） */
function onEnter(e: KeyboardEvent) {
  if (e.isComposing) return
  e.preventDefault()
  onSend()
}

/** 关闭：PC 收起抽屉；移动端全屏挂在 /chat 路由上，需同步回退 */
function close() {
  chatStore.closeWidget()
  if (route.name === 'chat') {
    router.replace('/menu')
  }
}

/** 面板打开/新消息/流式增量/工具状态/错误 → 吸底滚动 */
watch(
  () => [
    panelVisible.value,
    chatStore.messages.length,
    chatStore.messages[chatStore.messages.length - 1]?.content.length ?? 0,
    Object.keys(chatStore.activeTools).length,
    chatStore.error,
  ],
  ([visible]) => {
    if (!visible) return
    void nextTick(() => {
      bodyEl.value?.scrollTo({ top: bodyEl.value.scrollHeight })
    })
  },
)

/** 打开面板：恢复当前会话历史（刷新后 activeSession 为空，则等首条消息自建会话） */
watch(panelVisible, (visible) => {
  if (!visible) {
    historyOpen.value = false
    return
  }
  if (chatStore.activeSession !== null && chatStore.messages.length === 0) {
    const sid = chatStore.activeSession.id
    void chatStore.loadHistory(sid).then(() => {
      void nextTick(() => {
        bodyEl.value?.scrollTo({ top: bodyEl.value.scrollHeight })
      })
    })
  }
})
</script>

<template>
  <!-- 悬浮球：仅 PC 显示（≤767px CSS 隐藏；面板打开时同步隐藏） -->
  <button
    v-if="!panelVisible"
    class="chat-fab"
    aria-label="联系 AI 客服"
    @click="chatStore.openWidget()"
  >
    <svg class="chat-fab__mark" viewBox="0 0 32 32" aria-hidden="true">
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
  </button>

  <!-- 面板：移动端全屏 / PC 抽屉，同一段模板（Teleport 隔离 AppLayout 层叠上下文） -->
  <Teleport to="body">
    <Transition name="chat-pop">
      <section v-if="panelVisible" class="chat-panel" role="dialog" aria-label="AI 客服对话">
        <header class="chat-panel__header">
          <svg class="chat-panel__mark" viewBox="0 0 32 32" aria-hidden="true">
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
          <div class="chat-panel__title">
            <strong>AI 客服</strong>
            <span class="chat-panel__status"><i class="chat-panel__dot" aria-hidden="true"></i>在线</span>
          </div>
          <button class="chat-panel__icon" type="button" aria-label="新建会话" @click="onNewSession">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5 8.38 8.38 0 0 1-3.4-.7L3 21l1.7-5.1A8.5 8.5 0 1 1 21 11.5Z" />
              <path d="M12 8.5v6" />
              <path d="M9 11.5h6" />
            </svg>
          </button>
          <button
            class="chat-panel__icon"
            type="button"
            :aria-label="historyOpen ? '收起历史会话' : '历史会话'"
            :class="{ 'is-active': historyOpen }"
            @click="toggleHistory"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" />
            </svg>
          </button>
          <button class="chat-panel__close" aria-label="关闭客服窗口" @click="close">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>

          <!-- 历史会话面板：列表倒序（当前会话在顶部），可切换/新建/结束 -->
          <div v-if="historyOpen" class="chat-history">
            <button class="chat-history__new" type="button" @click="onNewSession">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
              新建会话
            </button>
            <div class="chat-history__list">
              <div
                v-for="s in chatStore.sessions"
                :key="s.id"
                class="chat-history__item"
                :class="{ 'is-active': s.id === chatStore.activeSession?.id }"
                role="button"
                tabindex="0"
                @click="onSwitchSession(s)"
                @keydown.enter.prevent="onSwitchSession(s)"
              >
                <div class="chat-history__meta">
                  <span class="chat-history__title">{{ sessionTitle(s) }}</span>
                  <span class="chat-history__time">{{ formatTime(s.updatedAt) }}</span>
                </div>
                <div class="chat-history__side">
                  <span class="chat-history__status" :class="{ 'is-ended': s.status === 'ENDED' }">
                    {{ s.status === 'ENDED' ? '已结束' : '进行中' }}
                  </span>
                  <button
                    v-if="s.status !== 'ENDED'"
                    class="chat-history__end"
                    type="button"
                    @click.stop="onEndSession(s)"
                  >
                    结束
                  </button>
                </div>
              </div>
              <p v-if="chatStore.sessions.length === 0 && !chatStore.sessionsLoading" class="chat-history__empty">
                暂无历史会话
              </p>
              <button
                v-if="chatStore.hasMoreSessions"
                class="chat-history__more"
                type="button"
                :disabled="chatStore.sessionsLoading"
                @click="chatStore.loadSessions(false)"
              >
                {{ chatStore.sessionsLoading ? '加载中…' : '加载更多会话' }}
              </button>
            </div>
          </div>
        </header>

        <div ref="bodyEl" class="chat-panel__body">
          <!-- 历史消息向上翻页（游标到底后隐藏） -->
          <button
            v-if="chatStore.hasMoreHistory && chatStore.messages.length > 0"
            class="chat-older"
            type="button"
            :disabled="chatStore.streaming"
            @click="onLoadMoreHistory"
          >
            查看更早消息
          </button>

          <!-- 欢迎语：纯前端本地消息，不落库 -->
          <div v-if="chatStore.messages.length === 0 && chatStore.error === null" class="chat-welcome">
            您好，我是大西洋咖啡 AI 客服。订单进度、优惠券、积分、门店信息和新品推荐都可以问我。
          </div>

          <ChatMessage v-for="m in chatStore.messages" :key="m.id" :msg="m" />

          <!-- 工具状态条：tool_start 出现、tool_end 消失（按 toolCallId 配对） -->
          <div v-for="(tool, id) in chatStore.activeTools" :key="id" class="msg-row">
            <span class="tool-status">
              <i class="tool-status__spinner" aria-hidden="true"></i>正在{{ toolLabel(tool) }}…
            </span>
          </div>

          <!-- 错误气泡：SSE 建连前业务错误与流内 error 统一入口，retryable 才提供重试 -->
          <div v-if="chatStore.error" class="msg-row">
            <div class="chat-error" role="alert">
              <p class="chat-error__text">{{ chatStore.error.message }}</p>
              <button
                v-if="chatStore.error.retryable"
                class="chat-error__retry"
                type="button"
                @click="chatStore.retry()"
              >
                重试
              </button>
            </div>
          </div>
        </div>

        <!-- 会话已结束：禁输入，引导新建 -->
        <div v-if="chatStore.isEnded" class="chat-ended">
          <span class="chat-ended__text">会话已结束</span>
          <button class="chat-ended__new" type="button" @click="chatStore.newSession()">新建会话</button>
        </div>

        <footer class="chat-panel__footer">
          <div class="chat-input-wrap">
            <textarea
              v-model="draft"
              class="chat-input"
              rows="1"
              maxlength="500"
              :disabled="chatStore.isEnded"
              :placeholder="chatStore.isEnded ? '会话已结束，请新建会话' : '输入您的问题，Enter 发送'"
              @keydown.enter="onEnter"
            ></textarea>
            <span
              v-if="draft.length > 0"
              class="chat-counter"
              :class="{ 'is-limit': draft.length >= CHAT_CONTENT_MAX }"
            >{{ draft.length }}/500</span>
          </div>
          <button
            v-if="!chatStore.streaming"
            class="chat-send"
            type="button"
            :disabled="!canSend"
            aria-label="发送"
            @click="onSend"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m22 2-7 20-4-9-9-4Z" />
              <path d="M22 2 11 13" />
            </svg>
          </button>
          <button
            v-else
            class="chat-send chat-send--stop"
            type="button"
            aria-label="停止生成"
            @click="chatStore.stop()"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <rect x="7" y="7" width="10" height="10" rx="2" fill="currentColor" />
            </svg>
          </button>
        </footer>
      </section>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
/* ===== 悬浮球（仅 PC：≤767px 隐藏，修正 1） ===== */
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 100;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  box-shadow: var(--ac-shadow-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-lg), 0 0 0 4px var(--ac-ring);
  }

  &__mark {
    width: 32px;
    height: 32px;
  }
}

/* 修正 1：移动端不挂悬浮球，入口为底部 AI Tab（/chat 全屏） */
@media (max-width: 767px) {
  .chat-fab {
    display: none;
  }
}

/* ===== 面板：移动端全屏（默认） / PC 抽屉（≥768px） ===== */
.chat-panel {
  position: fixed;
  z-index: 200;
  inset: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--ac-bg);

  @media (min-width: 768px) {
    inset: auto 24px 24px auto;
    width: 400px;
    height: min(640px, calc(100vh - 48px));
    border-radius: var(--ac-radius-overlay);
    background: var(--ac-glass-bg);
    backdrop-filter: var(--ac-glass-blur);
    -webkit-backdrop-filter: var(--ac-glass-blur);
    border: 1px solid var(--ac-glass-border);
    box-shadow: var(--ac-shadow-lg);
  }
}

/* ===== 头部 ===== */
.chat-panel__header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  padding-top: calc(12px + env(safe-area-inset-top));
  border-bottom: 1px solid var(--ac-border);
}

.chat-panel__mark {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

.chat-panel__title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;

  strong {
    font-family: var(--ac-font-display);
    font-size: 15px;
    color: var(--ac-text);
  }
}

.chat-panel__status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--ac-text-dim);
}

.chat-panel__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ac-success);
}

.chat-panel__close {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--ac-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background var(--ac-dur-fast) var(--ac-ease-enter);

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    background: var(--ac-card);
    color: var(--ac-text);
  }
}

/* 头部图标按钮：新建会话 / 历史会话 */
.chat-panel__icon {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--ac-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    background var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter);

  svg {
    width: 19px;
    height: 19px;
  }

  &:hover,
  &.is-active {
    background: var(--ac-tide);
    color: var(--ac-primary);
  }
}

/* ===== 历史会话面板（头部下方下拉） ===== */
.chat-history {
  position: absolute;
  top: calc(100% + 1px);
  left: 0;
  right: 0;
  max-height: min(380px, 62%);
  overflow-y: auto;
  background: var(--ac-bg);
  border-bottom: 1px solid var(--ac-border);
  box-shadow: var(--ac-shadow-md);
  z-index: 5;
}

.chat-history__new {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 1px solid var(--ac-border);
  background: transparent;
  color: var(--ac-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  svg {
    width: 15px;
    height: 15px;
  }

  &:hover {
    background: var(--ac-tide);
  }
}

.chat-history__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--ac-border);

  &:hover {
    background: var(--ac-card);
  }

  &.is-active {
    background: var(--ac-tide);
    box-shadow: inset 3px 0 0 var(--ac-primary);
  }
}

.chat-history__meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.chat-history__title {
  font-size: 14px;
  color: var(--ac-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-history__time {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--ac-text-dim);
}

.chat-history__side {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.chat-history__status {
  font-size: 11px;
  color: var(--ac-success);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
  padding: 2px 8px;

  &.is-ended {
    color: var(--ac-text-dim);
    background: var(--ac-card);
    border: 1px solid var(--ac-border);
  }
}

.chat-history__end {
  border: none;
  background: transparent;
  color: var(--ac-text-dim);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;

  &:hover {
    color: var(--ac-danger);
  }
}

.chat-history__empty {
  margin: 0;
  padding: 20px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.chat-history__more {
  display: block;
  width: 100%;
  padding: 10px;
  border: none;
  background: transparent;
  color: var(--ac-primary);
  font-size: 13px;
  cursor: pointer;

  &:disabled {
    color: var(--ac-text-dim);
    cursor: not-allowed;
  }
}

/* ===== 消息区 ===== */
.chat-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;
}

/* 欢迎语（本地消息，不落库）：样式与 AI 气泡同款 */
.chat-welcome {
  align-self: flex-start;
  max-width: 84%;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
  color: var(--ac-text);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: 14px 14px 14px 4px;
}

/* 查看更早消息（消息区顶部） */
.chat-older {
  align-self: center;
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  background: var(--ac-card);
  color: var(--ac-text-dim);
  font-size: 12px;
  padding: 4px 14px;
  cursor: pointer;
  transition:
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover:not(:disabled) {
    color: var(--ac-primary);
    border-color: var(--ac-primary);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* 会话已结束横幅（输入区上方） */
.chat-ended {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 16px;
  background: var(--ac-card);
  border-top: 1px solid var(--ac-border);

  &__text {
    font-size: 12px;
    color: var(--ac-text-dim);
  }

  &__new {
    border: 1px solid var(--ac-primary);
    border-radius: var(--ac-radius-pill);
    background: transparent;
    color: var(--ac-primary);
    font-size: 12px;
    padding: 3px 14px;
    cursor: pointer;
    transition: background var(--ac-dur-fast) var(--ac-ease-enter);

    &:hover {
      background: var(--ac-primary);
      color: #fff;
    }
  }
}

/* 工具状态条：tool_start/tool_end 之间展示 */
.tool-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px dashed var(--ac-border);
  border-radius: var(--ac-radius-pill);
  padding: 5px 12px;
}

.tool-status__spinner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--ac-sky);
  border-top-color: transparent;
  animation: chat-spin 0.8s linear infinite;
}

/* 错误气泡（retryable 决定重试按钮） */
.chat-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  max-width: 84%;
  padding: 10px 14px;
  background: var(--ac-card);
  border: 1px solid var(--ac-danger);
  border-radius: 14px 14px 14px 4px;

  &__text {
    margin: 0;
    font-size: 14px;
    line-height: 1.6;
    color: var(--ac-danger);
  }

  &__retry {
    border: 1px solid var(--ac-danger);
    border-radius: var(--ac-radius-pill);
    background: transparent;
    color: var(--ac-danger);
    font-size: 12px;
    padding: 3px 14px;
    cursor: pointer;
    transition: background var(--ac-dur-fast) var(--ac-ease-enter);

    &:hover {
      background: var(--ac-danger);
      color: #fff;
    }
  }
}

/* ===== 输入区 ===== */
.chat-panel__footer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 14px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--ac-border);
}

.chat-input-wrap {
  flex: 1;
  position: relative;
  min-width: 0;
}

.chat-input {
  display: block;
  width: 100%;
  resize: none;
  min-height: 42px;
  max-height: 96px;
  padding: 10px 12px;
  padding-bottom: 12px;
  font-family: var(--ac-font-body);
  font-size: 14px;
  line-height: 1.5;
  color: var(--ac-text);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-input);
}

.chat-counter {
  position: absolute;
  right: 10px;
  bottom: 22px;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: var(--ac-text-dim);
  pointer-events: none;

  &.is-limit {
    color: var(--ac-danger);
  }
}

.chat-send {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  background: var(--ac-cta);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity var(--ac-dur-fast) var(--ac-ease-enter);

  svg {
    width: 18px;
    height: 18px;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.chat-send--stop {
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  color: var(--ac-text);
}

/* ===== 过渡 ===== */
.chat-pop-enter-active,
.chat-pop-leave-active {
  transition:
    opacity var(--ac-dur-base) var(--ac-ease-enter),
    transform var(--ac-dur-base) var(--ac-ease-enter);
}

.chat-pop-enter-from,
.chat-pop-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

@keyframes chat-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
