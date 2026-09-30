/**
 * 优惠券接口（M1-4 已上线）：GET /api/user/coupons?status=UNUSED|USED|EXPIRED。
 */
import { request } from './client'

export type CouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

/** 对齐后端 MemberDtos.CouponView（券四要素 + 状态） */
export interface CouponView {
  id: number
  name: string
  thresholdAmount: number | null
  discountAmount: number | null
  expireAt: string
  status: CouponStatus
}

export function getCoupons(status?: CouponStatus): Promise<CouponView[]> {
  const query = status ? `?status=${status}` : ''
  return request<CouponView[]>(`/api/user/coupons${query}`)
}
