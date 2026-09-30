/**
 * API 客户端（M1-6 批次 3）：
 * 统一走 /api 前缀（开发期 Vite proxy 同域转发，生产 Nginx 同源）；
 * 自动附 Authorization: Bearer {token}；统一解析 { code, message, data }，
 * code !== 0 抛 ApiError；401（HTTP 401 或业务码 40101）清 token 并跳 /login。
 * 本文件不依赖 pinia（避免 store ↔ client 循环引用），token 直读 localStorage。
 */

const TOKEN_KEY = 'ac_token'
const REFRESH_TOKEN_KEY = 'ac_refresh_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

export function setTokens(token: string, refreshToken: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/** 业务错误：code 为后端语义码（40001/40101/40902/50002...），message 为可展示文案 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /** 默认 true：附 Authorization 头（login/register/refresh 等匿名接口传 false） */
  auth?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (options.auth !== false) {
    const token = getToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  let res: Response
  try {
    res = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(-1, '网络异常，请稍后重试')
  }

  let envelope: ApiEnvelope<T>
  try {
    envelope = (await res.json()) as ApiEnvelope<T>
  } catch {
    throw new ApiError(-1, `服务异常（HTTP ${res.status}）`)
  }

  // 401：清登录态回登录页（HTTP 401 与业务码 40101 等价处理）
  if (res.status === 401 || envelope.code === 40101) {
    clearTokens()
    if (location.pathname !== '/login') {
      location.href = '/login'
    }
    throw new ApiError(40101, '登录已失效，请重新登录')
  }

  if (envelope.code !== 0) {
    throw new ApiError(envelope.code, envelope.message)
  }
  return envelope.data
}
