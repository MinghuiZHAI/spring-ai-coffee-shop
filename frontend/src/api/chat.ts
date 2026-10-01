import { request, getToken } from '@/api/client'
import { sendChatMessage } from '@/utils/sse'
import type { ChatStreamHandlers } from '@/utils/sse'

/**
 * 聊天域 API（批次 5，契约=总体设计 §5.2/§5.3 + ChatController）：
 * 统一响应体经 client.ts 解包；流式发送走 utils/sse.ts（fetch+ReadableStream，JWT 可携带）。
 */

/** 单条消息上限（后端 ChatDtos.SendMessageRequest @Size(max=500)） */
export const CHAT_CONTENT_MAX = 500

/** 会话视图（后端 ChatSessionService.SessionView）；status: AI_SERVING / HUMAN_SERVING（M3 预留）/ ENDED */
export interface ChatSessionView {
  id: number
  title: string
  status: string
  createdAt: string
  updatedAt: string
}

/** 历史消息中的工具调用摘要（ChatSessionService.ToolCallItem） */
export interface ChatToolCallItem {
  toolCallId: string
  tool: string
  success: boolean
  durationMs: number | null
}

/** 历史消息项（ChatSessionService.MessageView）；role 四值贯穿（决策 #18，MVP 出现 USER/AI/SYSTEM） */
export interface ChatMessageView {
  id: number
  sessionId: number
  role: 'USER' | 'AI' | 'AGENT' | 'SYSTEM'
  content: string
  toolCalls: ChatToolCallItem[] | null
  createdAt: string
}

/** 游标分页统一结构（总体设计 §5.1：list + nextCursor，null 表示到底） */
export interface CursorPage<T> {
  list: T[]
  nextCursor: number | null
}

export function createSession(): Promise<ChatSessionView> {
  return request('/api/user/chat/sessions', { method: 'POST' })
}

export function listSessions(cursor?: number, limit = 20): Promise<CursorPage<ChatSessionView>> {
  const q = new URLSearchParams({ limit: String(limit) })
  if (cursor !== undefined) q.set('cursor', String(cursor))
  return request(`/api/user/chat/sessions?${q.toString()}`)
}

export function closeSession(id: number): Promise<void> {
  return request(`/api/user/chat/sessions/${id}/close`, { method: 'POST' })
}

export function getMessages(
  sessionId: number,
  cursor?: number,
  limit = 20,
): Promise<CursorPage<ChatMessageView>> {
  const q = new URLSearchParams({ limit: String(limit) })
  if (cursor !== undefined) q.set('cursor', String(cursor))
  return request(`/api/user/chat/sessions/${sessionId}/messages?${q.toString()}`)
}

/** 流式发送：token 直读 localStorage（client.ts 口径，聊天模块不依赖 pinia user store） */
export function streamMessage(
  sessionId: number,
  content: string,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  return sendChatMessage(sessionId, content, getToken(), handlers, signal)
}

/** 工具名 → 文案（与 backend ai/tool 四工具类的 7 个 @Tool 方法名一一对应） */
const TOOL_LABELS: Record<string, string> = {
  queryMyOrders: '查询您的订单',
  queryEstimatedTime: '查询制作进度',
  queryRefundProgress: '查询退款进度',
  queryPoints: '查询积分',
  queryCoupons: '查询优惠券',
  recommendDrinks: '为你挑选饮品',
  queryStores: '查询门店信息',
}

export function toolLabel(tool: string): string {
  return TOOL_LABELS[tool] ?? tool
}
