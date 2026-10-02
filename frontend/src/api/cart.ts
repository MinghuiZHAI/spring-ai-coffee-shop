/**
 * 购物车接口（批次 7，契约=后端 CartDtos）：GET /api/user/cart 与三个行级操作。
 * 所有变更操作的响应都是最新 CartView（后端实现），store 直接整包替换。
 * specSnapshot 为 JSON 对象字符串（如 {"温度":"冰","糖度":"半糖"}，烘焙小食为 "{}"），
 * 展示时解析——复用 3b 的 parseSpecs。
 */
import { request } from './client'

export interface CartItemView {
  id: number
  productId: number
  productName: string
  specSnapshot: string
  unitPrice: number
  quantity: number
}

export interface CartView {
  items: CartItemView[]
  totalAmount: number
}

export interface AddCartPayload {
  productId: number
  /** 规格快照（批次 7 决策：直传空对象合法，curl 已验证；完整规格选择器列 M2） */
  specs: Record<string, string>
  quantity: number
}

export function getCart(): Promise<CartView> {
  return request<CartView>('/api/user/cart')
}

export function addCartItem(payload: AddCartPayload): Promise<CartView> {
  return request<CartView>('/api/user/cart/items', { method: 'POST', body: payload })
}

export function updateCartItem(id: number, quantity: number): Promise<CartView> {
  return request<CartView>(`/api/user/cart/items/${id}`, { method: 'PUT', body: { quantity } })
}

export function deleteCartItem(id: number): Promise<CartView> {
  return request<CartView>(`/api/user/cart/items/${id}`, { method: 'DELETE' })
}
