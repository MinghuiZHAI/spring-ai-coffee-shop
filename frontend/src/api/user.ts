/**
 * 用户域接口（03 v1.4 追加契约）：GET/PUT /api/user/profile。
 * 本批仅 profile 两接口；points/coupons/wallet 的 api 封装随后续批次补齐。
 */
import { request } from './client'
import type { UserInfo } from './auth'

export function getProfile(): Promise<UserInfo> {
  return request<UserInfo>('/api/user/profile')
}

export interface UpdateProfilePayload {
  nickname?: string
  avatarUrl?: string
  /** UNKNOWN/MALE/FEMALE，后端 @Pattern 校验 */
  gender?: string
  luckyDay?: string
}

export function updateProfile(payload: UpdateProfilePayload): Promise<UserInfo> {
  return request<UserInfo>('/api/user/profile', { method: 'PUT', body: payload })
}

/** 积分（对齐后端 MemberDtos.PointItem：时间/变动值/类型/关联订单） */
export interface PointRecord {
  createdAt: string
  changeValue: number
  type: string
  relatedOrderId: number
}

export interface PointsView {
  balance: number
  records: PointRecord[]
}

export function getPoints(): Promise<PointsView> {
  return request<PointsView>('/api/user/points')
}
