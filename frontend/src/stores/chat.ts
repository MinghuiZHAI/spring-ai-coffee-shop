import { defineStore } from 'pinia'
import type { ChatMessageView, ChatSessionView } from '@/api/chat'
import {
  CHAT_CONTENT_MAX,
  closeSession,
  createSession,
  getMessages,
  listSessions,
  streamMessage,
} from '@/api/chat'
import { ApiError } from '@/api/client'
import type { ChatStreamHandlers } from '@/utils/sse'

/**
 * 聊天 store（批次 5）：widget 开合 + 会话/消息/流式状态。
 * 开合入 store 供多入口唤起（PC 悬浮球；移动端面板由 /chat 路由挂载，见 ChatWidget）。
 * 发送时序：乐观插入用户气泡 + AI 占位（streaming）→ delta 拼接 → done 回填 messageId；
 * 流式期间 streaming=true 禁发送（后端 42901 单用户并发 1 路）；tool_start/tool_end
 * 按 toolCallId 配对驱动工具状态条；错误入 error（retryable 决定重试按钮）。
 */

/** 渲染消息 = 历史消息 + 本地运行时状态（streaming 标记流式中的 AI 气泡；本地乐观消息用负数 id） */
export interface ChatDisplayMessage extends ChatMessageView {
  streaming?: boolean
}

/** 错误气泡（不落库仅本地渲染）；content 为触发失败的用户输入，重试原样重发 */
export interface ChatErrorState {
  message: string
  retryable: boolean
  content: string
}

/** 本地时间拼 ISO（后端 LocalDateTime 无时区；toISOString 是 UTC，直接用会偏 8 小时） */
function nowLocalIso(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    open: false,
    /** 当前会话；null 时发送前自动 create（刷新即失效，历史回放属批次 5 步骤 3） */
    activeSession: null as ChatSessionView | null,
    /** 渲染消息（正序） */
    messages: [] as ChatDisplayMessage[],
    /** 历史向上翻页游标（messages() 按 id 倒序返回，前端反转正序；null=到底） */
    historyCursor: null as number | null,
    /** 历史会话列表（列表页按 updated_at 倒序游标分页） */
    sessions: [] as ChatSessionView[],
    sessionsCursor: null as number | null,
    sessionsLoading: false,
    streaming: false,
    /** 进行中的工具调用：toolCallId → 工具名 */
    activeTools: {} as Record<string, string>,
    abortController: null as AbortController | null,
    error: null as ChatErrorState | null,
  }),

  getters: {
    hasMoreHistory: (state) => state.historyCursor !== null,
    hasMoreSessions: (state) => state.sessionsCursor !== null,
    /** 当前会话是否已结束（禁输入 + 提示新建） */
    isEnded: (state) => state.activeSession?.status === 'ENDED',
  },

  actions: {
    openWidget() {
      this.open = true
    },
    closeWidget() {
      this.open = false
    },

    /** 拉取历史消息：倒序页反转正序整段替换（进会话/切换会话用） */
    async loadHistory(sessionId: number) {
      const page = await getMessages(sessionId)
      this.messages = [...page.list].reverse().map((m) => ({ ...m }))
      this.historyCursor = page.nextCursor
    },

    /** 加载更早消息：向上拼接（列表顶部"加载更多"） */
    async loadMoreHistory() {
      if (this.activeSession === null || this.historyCursor === null || this.streaming) return
      const page = await getMessages(this.activeSession.id, this.historyCursor)
      this.messages = [...[...page.list].reverse().map((m) => ({ ...m })), ...this.messages]
      this.historyCursor = page.nextCursor
    },

    /** 历史会话列表（reset=true 拉首页，否则翻下一页） */
    async loadSessions(reset = true) {
      if (this.sessionsLoading) return
      this.sessionsLoading = true
      try {
        const page = await listSessions(reset ? undefined : this.sessionsCursor ?? undefined)
        this.sessions = reset ? page.list : [...this.sessions, ...page.list]
        this.sessionsCursor = page.nextCursor
      } finally {
        this.sessionsLoading = false
      }
    },

    /** 切换会话：停掉进行中的流，清当前渲染态后拉取目标会话历史 */
    async switchSession(id: number) {
      if (this.activeSession?.id === id) return
      const target = this.sessions.find((s) => s.id === id)
      if (target === undefined) return
      if (this.streaming) this.stop()
      this.activeSession = target
      this.error = null
      this.activeTools = {}
      this.messages = []
      this.historyCursor = null
      await this.loadHistory(id)
    },

    /** 新建会话：清空渲染态（会话延迟到首条消息发送时创建，避免空会话） */
    newSession() {
      if (this.streaming) this.stop()
      this.activeSession = null
      this.messages = []
      this.historyCursor = null
      this.error = null
      this.activeTools = {}
    },

    /** 结束会话（幂等）：本地同步状态，若为当前会话则触发输入禁用 */
    async endSession(id: number) {
      if (this.streaming && this.activeSession?.id === id) this.stop()
      await closeSession(id)
      this.sessions = this.sessions.map((s) => (s.id === id ? { ...s, status: 'ENDED' } : s))
      if (this.activeSession?.id === id) {
        this.activeSession = { ...this.activeSession, status: 'ENDED' }
      }
    },

    /** 发送：无会话先建（失败入 error，重试走完整重发）；乐观插入用户气泡 + AI 占位 */
    async send(rawContent: string) {
      const content = rawContent.trim()
      if (!content || content.length > CHAT_CONTENT_MAX || this.streaming) return
      this.error = null
      if (this.activeSession === null) {
        try {
          this.activeSession = await createSession()
        } catch (e) {
          this.error = {
            message: e instanceof ApiError ? e.message : '会话创建失败，请稍后重试',
            retryable: true,
            content,
          }
          return
        }
      }
      this.messages.push({
        id: -Date.now(),
        sessionId: this.activeSession.id,
        role: 'USER',
        content,
        toolCalls: null,
        createdAt: nowLocalIso(),
      })
      this.messages.push({
        id: -Date.now() - 1,
        sessionId: this.activeSession.id,
        role: 'AI',
        content: '',
        toolCalls: null,
        createdAt: nowLocalIso(),
        streaming: true,
      })
      const placeholder = this.messages[this.messages.length - 1] as ChatDisplayMessage
      await this.stream(content, placeholder)
    },

    /** 重试：原输入重发。会话未建（建会话失败）走完整重发；已建则不重复插入用户气泡
     * （已知妥协：后端无 requestId 幂等，docs/07 §6.3） */
    async retry() {
      if (this.error === null || this.streaming) return
      const content = this.error.content
      this.error = null
      if (this.activeSession === null) {
        await this.send(content)
        return
      }
      this.messages.push({
        id: -Date.now() - 1,
        sessionId: this.activeSession.id,
        role: 'AI',
        content: '',
        toolCalls: null,
        createdAt: nowLocalIso(),
        streaming: true,
      })
      const placeholder = this.messages[this.messages.length - 1] as ChatDisplayMessage
      await this.stream(content, placeholder)
    },

    /** 停止生成：abort 后 sse 读取以 AbortError 结束，部分内容保留 */
    stop() {
      this.abortController?.abort()
    },

    /** 六事件驱动：intent 不展示（规格定为埋点）；tool 按 toolCallId 配对进状态条与气泡摘要 */
    async stream(content: string, placeholder: ChatDisplayMessage) {
      const sessionId = this.activeSession!.id
      this.streaming = true
      this.activeTools = {}
      const controller = new AbortController()
      this.abortController = controller

      const handlers: ChatStreamHandlers = {
        onIntent: () => {},
        onToolStart: (e) => {
          this.activeTools = { ...this.activeTools, [e.toolCallId]: e.tool }
        },
        onToolEnd: (e) => {
          const next = { ...this.activeTools }
          delete next[e.toolCallId]
          this.activeTools = next
          placeholder.toolCalls = [
            ...(placeholder.toolCalls ?? []),
            { toolCallId: e.toolCallId, tool: e.tool, success: e.success, durationMs: e.durationMs },
          ]
        },
        onDelta: (text) => {
          placeholder.content += text
        },
        onDone: (e) => {
          placeholder.streaming = false
          placeholder.id = e.messageId
        },
        onError: (e) => {
          placeholder.streaming = false
          this.error = { message: e.message, retryable: e.retryable, content }
        },
      }

      try {
        await streamMessage(sessionId, content, handlers, controller.signal)
      } catch (e) {
        if ((e as Error)?.name !== 'AbortError') {
          this.error = { message: '网络异常，请稍后重试', retryable: true, content }
        }
      } finally {
        placeholder.streaming = false
        this.streaming = false
        if (this.abortController === controller) {
          this.abortController = null
        }
        // 无产出且失败的占位气泡移除，让位错误气泡
        if (!placeholder.content && this.error !== null) {
          this.messages = this.messages.filter((m) => m !== placeholder)
        }
      }
    },
  },
})
