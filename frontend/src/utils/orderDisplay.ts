/**
 * 订单展示常量（批次 7 自 data/orders.ts 收编）：8 值状态机的徽标/提示/主流程步进、
 * 退款状态文案与时间格式化——数据源已切真接口（api/order.ts），此处为纯展示层口径。
 */

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID_TODO'
  | 'MAKING'
  | 'READY'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED'

export type RefundStatus = 'APPLYING' | 'APPROVED' | 'SUCCESS' | 'REJECTED'

export const ORDER_STATUS_META: Record<
  OrderStatus,
  { label: string; tone: 'warning' | 'primary' | 'success' | 'muted' | 'lavender' }
> = {
  PENDING_PAYMENT: { label: '待支付', tone: 'warning' },
  PAID_TODO: { label: '待制作', tone: 'primary' },
  MAKING: { label: '制作中', tone: 'primary' },
  READY: { label: '待取餐', tone: 'lavender' },
  COMPLETED: { label: '已完成', tone: 'success' },
  CANCELLED: { label: '已取消', tone: 'muted' },
  REFUNDING: { label: '退款中', tone: 'warning' },
  REFUNDED: { label: '已退款', tone: 'success' },
}

export const REFUND_STATUS_LABEL: Record<RefundStatus, string> = {
  APPLYING: '申请中',
  APPROVED: '已通过',
  SUCCESS: '已到账',
  REJECTED: '已拒绝',
}

/** 详情页状态提示文案（对齐模拟推进器节奏：支付 30s → 制作中，制作 3min → 待取餐） */
export const STATUS_HINT: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '请在支付截止时间前完成支付，超时订单将自动取消',
  PAID_TODO: '门店已接单，即将开始制作',
  MAKING: '制作中，约 3 分钟出餐',
  READY: '已出餐，凭取餐码到门店取餐',
  COMPLETED: '订单已完成，感谢惠顾',
  CANCELLED: '订单已取消',
  REFUNDING: '退款处理中，申请后约 30 秒显示已通过，约 1 分钟内到账',
  REFUNDED: '退款已到账，请留意原路退回',
}

/** 主流程步进条：待支付→待制作→制作中→待取餐→已完成（售后/取消态不展示） */
export const MAIN_FLOW: OrderStatus[] = [
  'PENDING_PAYMENT',
  'PAID_TODO',
  'MAKING',
  'READY',
  'COMPLETED',
]

/**
 * 状态口径规范化（批次 7 实测修正）：接口 status 为**中文 label**（03 §5.4 契约示例
 * "status": "待支付"，OrderStatus.label()），展示层统一先规范化为枚举名再查表——
 * 枚举名/中文双口径兼容，未知值返回 null（宁缺勿串）。
 */
export function normalizeStatus(status: string): OrderStatus | null {
  if (status in ORDER_STATUS_META) return status as OrderStatus
  const byLabel = (Object.keys(ORDER_STATUS_META) as OrderStatus[]).find(
    (key) => ORDER_STATUS_META[key].label === status,
  )
  return byLabel ?? null
}

/** 状态相等判断（双口径） */
export function isStatus(status: string, expect: OrderStatus): boolean {
  return normalizeStatus(status) === expect
}

/** 接口 status 为 string（8 值枚举全量预留），未知值回退中性展示 */
export function statusMeta(status: string): { label: string; tone: 'warning' | 'primary' | 'success' | 'muted' | 'lavender' } {
  const key = normalizeStatus(status)
  return key ? ORDER_STATUS_META[key] : { label: status, tone: 'muted' }
}

export function statusHint(status: string): string {
  const key = normalizeStatus(status)
  return key ? STATUS_HINT[key] : ''
}

export function refundStatusLabel(status: string): string {
  return REFUND_STATUS_LABEL[status as RefundStatus] ?? status
}

export function formatTime(iso: string, full = false): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  const md = `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return full ? `${d.getFullYear()}-${md} ${hm}` : `${md} ${hm}`
}
