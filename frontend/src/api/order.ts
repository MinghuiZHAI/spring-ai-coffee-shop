/**
 * 订单接口（批次 7，契约=后端 OrderDtos）：下单 / 游标列表 / 详情 / 四个状态动作。
 * 列表是否支持 status 服务端筛选在步骤 3 实施时核实（不支持则前端过滤）。
 */
import { request } from './client'
import type { CursorPage } from './chat'

export interface CreateOrderPayload {
  storeId: number
  /** MVP 仅门店自取（PRD 决策） */
  pickupMethod: string
  cartItemIds: number[]
  /** 批次 7 决策：下单不带券（结算选券列 M2） */
  userCouponId: null
}

export interface OrderCreatedResponse {
  orderId: number
  orderNo: string
  status: string
  totalAmount: number
  discountAmount: number
  payAmount: number
  expireAt: string
}

export interface OrderBrief {
  id: number
  orderNo: string
  storeName: string
  status: string
  payAmount: number
  itemSummary: string
  createdAt: string
}

export interface OrderItemView {
  productId: number
  productName: string
  specSnapshot: string
  unitPrice: number
  quantity: number
  lineAmount: number
}

export interface RefundView {
  refundNo: string
  amount: number
  status: string
  applyAt: string
  finishedAt: string | null
}

export interface OrderDetail {
  id: number
  orderNo: string
  storeName: string
  status: string
  cancelReason: string | null
  pickupMethod: string
  pickupCode: string | null
  expectedFinishTime: string | null
  totalAmount: number
  discountAmount: number
  payAmount: number
  rewardPoints: number
  items: OrderItemView[]
  refund: RefundView | null
  createdAt: string
}

export interface PayResponse {
  orderId: number
  orderNo: string
  status: string
  pickupCode: string
}

export function createOrder(payload: CreateOrderPayload): Promise<OrderCreatedResponse> {
  return request<OrderCreatedResponse>('/api/user/orders', { method: 'POST', body: payload })
}

export function listOrders(cursor?: number, limit = 10, status?: string): Promise<CursorPage<OrderBrief>> {
  const q = new URLSearchParams({ limit: String(limit) })
  if (cursor !== undefined) q.set('cursor', String(cursor))
  if (status) q.set('status', status)
  return request<CursorPage<OrderBrief>>(`/api/user/orders?${q.toString()}`)
}

export function getOrder(id: number): Promise<OrderDetail> {
  return request<OrderDetail>(`/api/user/orders/${id}`)
}

export function payOrder(id: number): Promise<PayResponse> {
  return request<PayResponse>(`/api/user/orders/${id}/pay`, { method: 'POST' })
}

export function pickupOrder(id: number): Promise<PayResponse> {
  return request<PayResponse>(`/api/user/orders/${id}/pickup`, { method: 'POST' })
}

export function cancelOrder(id: number, reason?: string): Promise<void> {
  return request<void>(`/api/user/orders/${id}/cancel`, {
    method: 'POST',
    body: reason ? { reason } : undefined,
  })
}

export function applyRefund(id: number, reason?: string): Promise<RefundView> {
  return request<RefundView>(`/api/user/orders/${id}/refunds`, {
    method: 'POST',
    body: reason ? { reason } : undefined,
  })
}
