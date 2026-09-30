/**
 * 认证接口（总体设计 §5.2 认证域契约 / 04 v1.6 UserInfo 字段口径）：
 * login/register/refresh 为匿名接口（auth:false）；logout 白名单校验失败也幂等成功。
 */
import { request } from './client'

export type UserRole = 'USER' | 'ADMIN' | 'AGENT'

/** 对齐后端 AuthDtos.UserInfo（01 v1.3 追加字段口径） */
export interface UserInfo {
  userId: number
  phone: string
  nickname: string
  role: UserRole
  memberLevel: number
  avatarUrl: string
  gender: string
  luckyDay: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  accessExpiresIn: number
  userInfo: UserInfo
}

export function login(phone: string, password: string): Promise<TokenResponse> {
  return request<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: { phone, password },
    auth: false,
  })
}

export function register(phone: string, password: string, nickname: string): Promise<TokenResponse> {
  return request<TokenResponse>('/api/auth/register', {
    method: 'POST',
    body: { phone, password, nickname },
    auth: false,
  })
}

export function refresh(refreshToken: string): Promise<TokenResponse> {
  return request<TokenResponse>('/api/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
    auth: false,
  })
}

/** 幂等登出：refreshToken 失效也返回成功 */
export function logout(refreshToken: string): Promise<void> {
  return request<void>('/api/auth/logout', {
    method: 'POST',
    body: { refreshToken },
    auth: false,
  })
}
