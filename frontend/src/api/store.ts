/**
 * 门店接口（批次 7，契约=后端 StoreDtos）：GET /api/user/stores。
 * 批次 7 用于下单结算的门店选择（MVP 仅门店自取，PRD 决策）。
 */
import { request } from './client'

export interface StoreBrief {
  id: number
  name: string
  address: string
  businessHours: string
  phone: string
}

export interface StoreListResponse {
  stores: StoreBrief[]
}

export function listStores(): Promise<StoreListResponse> {
  return request<StoreListResponse>('/api/user/stores')
}
