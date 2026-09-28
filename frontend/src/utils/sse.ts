/**
 * SSE 消费工具（04-详细设计 v1.2 §5.3，技术栈 #28）：
 * fetch + ReadableStream 消费流式接口——POST 可用、JWT 头可携带（EventSource 的两个硬伤）。
 * 事件协议：写法 A（事件类型放 JSON 的 type 字段），一次 JSON.parse 拿全部信息；
 * tool_start/tool_end 携带 toolCallId，模型并行工具调用时按其配对，避免状态错乱；
 * error.retryable 决定前端是否展示"重试"按钮；SSE 建立前的错误走 HTTP 状态码（总体设计 §5.5 边界）。
 */
export interface ChatStreamHandlers {
  onIntent?: (e: { intent: string; confidence: number }) => void
  onToolStart?: (e: { toolCallId: string; tool: string }) => void
  onToolEnd?: (e: { toolCallId: string; tool: string; success: boolean; durationMs: number }) => void
  onDelta?: (text: string) => void
  onDone?: (e: { messageId: number }) => void
  onError?: (e: { message: string; retryable: boolean }) => void
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
  if (!resp.ok) {
    // SSE 建立前的鉴权/参数错误走标准 HTTP 状态码（总体设计 §5.5）
    const message = await resp.text()
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
      const evt = JSON.parse(dataLine.slice(5).trim())
      switch (evt.type) {
        case 'intent':
          h.onIntent?.(evt)
          break
        case 'tool_start':
          openTools.set(evt.toolCallId, evt.tool)
          h.onToolStart?.(evt)
          break
        case 'tool_end':
          openTools.delete(evt.toolCallId)
          h.onToolEnd?.(evt)
          break
        case 'delta':
          h.onDelta?.(evt.content)
          break
        case 'done':
          h.onDone?.(evt)
          return
        case 'error':
          h.onError?.(evt)
          return
      }
    }
  }
}
