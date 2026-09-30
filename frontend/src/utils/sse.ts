/**
 * SSE 消费工具（04-详细设计 v1.2 §5.3，技术栈 #28）：
 * fetch + ReadableStream 消费流式接口——POST 可用、JWT 头可携带（EventSource 的两个硬伤）。
 * 事件协议：写法 A（事件类型放 JSON 的 type 字段），一次 JSON.parse 拿全部信息；
 * tool_start/tool_end 携带 toolCallId，模型并行工具调用时按其配对，避免状态错乱；
 * error.retryable 决定前端是否展示"重试"按钮。
 *
 * 建连前错误边界（docs/05 附录 A5）：本项目业务错误统一 HTTP 200 + {code,message} 信封
 * （GlobalExceptionHandler），SSE 建立前的 40902/42901/50002 亦如此——仅靠 !resp.ok 会把
 * 这类错误当流静默吞掉（无 data: 行，静默结束）。故读流前先检查 Content-Type：
 * application/json 则按业务错误抛 onError（42901/50002 → retryable=true）；
 * HTTP 401 / 业务码 40101 清 token 回登录页（与 api/client.ts 口径一致）。
 */
import { clearTokens } from '@/api/client'

export interface ChatStreamHandlers {
  onIntent?: (e: { intent: string; confidence: number }) => void
  onToolStart?: (e: { toolCallId: string; tool: string }) => void
  onToolEnd?: (e: { toolCallId: string; tool: string; success: boolean; durationMs: number }) => void
  onDelta?: (text: string) => void
  onDone?: (e: { messageId: number }) => void
  onError?: (e: { message: string; retryable: boolean }) => void
}

/** 业务码 → 是否可重试：42901 并发门 / 50002 上游模型异常可重试；40902 等状态类不可 */
function isRetryable(code: number): boolean {
  return code === 42901 || code === 50002
}

function redirectToLogin() {
  clearTokens()
  if (location.pathname !== '/login') {
    location.href = '/login'
  }
}

export async function sendChatMessage(
  sessionId: number,
  content: string,
  token: string,
  h: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const resp = await fetch(`/api/user/chat/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ content }),
    signal,
  })

  // SSE 建立前的错误：业务错误为 HTTP 200 + 信封，鉴权失败为 HTTP 401（总体设计 §5.5 边界）
  const contentType = resp.headers.get('Content-Type') ?? ''
  if (resp.ok && contentType.includes('application/json')) {
    let code = -1
    let message = ''
    try {
      const env = (await resp.json()) as { code?: number; message?: string }
      code = env.code ?? -1
      message = env.message ?? ''
    } catch {
      // 信封解析失败：保留默认文案
    }
    if (code === 40101) {
      redirectToLogin()
    }
    h.onError?.({ message: message || `请求失败(${code})`, retryable: isRetryable(code) })
    return
  }
  if (!resp.ok) {
    let message = ''
    try {
      const body = (await resp.json()) as { message?: string }
      message = body.message ?? ''
    } catch {
      // 非 JSON 体（网关错误页等）：退回状态码文案
    }
    if (resp.status === 401) {
      redirectToLogin()
    }
    h.onError?.({ message: message || `请求失败(${resp.status})`, retryable: resp.status >= 500 })
    return
  }

  const reader = resp.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  const openTools = new Map<string, string>() // toolCallId → tool 名：start/end 配对

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop()! // 末帧可能不完整，留缓冲
    for (const frame of frames) {
      const dataLine = frame.split('\n').find((l) => l.startsWith('data:'))
      if (!dataLine) continue
      let evt: { type?: string }
      try {
        evt = JSON.parse(dataLine.slice(5).trim())
      } catch {
        continue // 残帧（代理截断等）跳过，等待后续帧
      }
      switch (evt.type) {
        case 'intent':
          h.onIntent?.(evt as { intent: string; confidence: number })
          break
        case 'tool_start':
          openTools.set((evt as { toolCallId: string }).toolCallId, (evt as { tool: string }).tool)
          h.onToolStart?.(evt as { toolCallId: string; tool: string })
          break
        case 'tool_end':
          openTools.delete((evt as { toolCallId: string }).toolCallId)
          h.onToolEnd?.(evt as { toolCallId: string; tool: string; success: boolean; durationMs: number })
          break
        case 'delta':
          h.onDelta?.((evt as { content: string }).content)
          break
        case 'done':
          h.onDone?.(evt as { messageId: number })
          return
        case 'error':
          h.onError?.(evt as { message: string; retryable: boolean })
          return
      }
    }
  }
}
