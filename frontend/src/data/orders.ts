/**
 * 订单静态假数据（M1-6 步骤 3b）：状态机 8 值、字段与退款单结构对齐
 * 04-详细设计 §2.1/§2.2 与 03-总体设计 §5.4；门店名镜像 V2 种子。
 * specSnapshot 为 JSON 对象字符串（与后端 CartService.toJson 一致），展示时解析。
 * 接后端后由 GET /api/user/orders、GET /api/user/orders/{id} 替换。
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

export interface OrderItemView {
  productId: number
  productName: string
  /** JSON 对象字符串：{"温度":"冰","糖度":"半糖"}（烘焙小食为空对象） */
  specSnapshot: string
  unitPrice: number
  quantity: number
  lineAmount: number
}

export interface RefundView {
  refundNo: string
  amount: number
  status: RefundStatus
  applyAt: string
  finishedAt: string | null
}

export interface OrderView {
  id: number
  orderNo: string
  status: OrderStatus
  storeName: string
  /** STORE_PICKUP 门店自取（MVP 仅此一种，PRD 决策） */
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
  /** 待支付订单的支付截止时间，超时自动取消（30 分钟） */
  expireAt: string | null
}

/** 状态元数据：中文标签 + 色调（chip 分类） */
export const ORDER_STATUS_META: Record<
  OrderStatus,
  { label: string; tone: 'warning' | 'primary' | 'success' | 'muted' }
> = {
  PENDING_PAYMENT: { label: '待支付', tone: 'warning' },
  PAID_TODO: { label: '待制作', tone: 'primary' },
  MAKING: { label: '制作中', tone: 'primary' },
  READY: { label: '待取餐', tone: 'primary' },
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

/** 主流程五步（售后/取消态不展示步进条） */
export const MAIN_FLOW: OrderStatus[] = [
  'PENDING_PAYMENT',
  'PAID_TODO',
  'MAKING',
  'READY',
  'COMPLETED',
]

/** 解析 specSnapshot JSON 字符串为规格组数组（与购物车 chip 展示口径一致） */
export function parseSpecs(specSnapshot: string): Array<{ group: string; option: string }> {
  try {
    const obj = JSON.parse(specSnapshot) as Record<string, unknown>
    return Object.entries(obj).map(([group, option]) => ({ group, option: String(option) }))
  } catch {
    return []
  }
}

/** 时间格式化：列表用短格式，详情用完整格式 */
export function formatTime(iso: string, full = false): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  const md = `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return full ? `${d.getFullYear()}-${md} ${hm}` : `${md} ${hm}`
}

const SPECS = (specs: Record<string, string>) => JSON.stringify(specs)

const STORE_BUND = 'Atlantic Coffee·外滩航海旗舰店'
const STORE_LUJIAZUI = 'Atlantic Coffee·陆家嘴灯塔店'
const STORE_XUHUI = 'Atlantic Coffee·徐汇滨江船坞店'

export const ORDERS: OrderView[] = [
  {
    id: 108,
    orderNo: 'AC20260928000008',
    status: 'PENDING_PAYMENT',
    storeName: STORE_BUND,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: null,
    expectedFinishTime: null,
    totalAmount: 22,
    discountAmount: 0,
    payAmount: 22,
    rewardPoints: 220,
    items: [
      {
        productId: 7,
        productName: '焦糖风暴玛奇朵',
        specSnapshot: SPECS({ 温度: '热', 糖度: '标准糖' }),
        unitPrice: 22,
        quantity: 1,
        lineAmount: 22,
      },
    ],
    refund: null,
    createdAt: '2026-09-28T17:05:00',
    expireAt: '2026-09-28T17:35:00',
  },
  {
    id: 101,
    orderNo: 'AC20260928000003',
    status: 'PAID_TODO',
    storeName: STORE_BUND,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '8842',
    expectedFinishTime: '2026-09-28T16:20:00',
    totalAmount: 34,
    discountAmount: 0,
    payAmount: 34,
    rewardPoints: 340,
    items: [
      {
        productId: 5,
        productName: '湾流拿铁',
        specSnapshot: SPECS({ 温度: '热', 糖度: '标准糖', 冰量: '去冰' }),
        unitPrice: 18,
        quantity: 1,
        lineAmount: 18,
      },
      {
        productId: 24,
        productName: '芝士贝果',
        specSnapshot: SPECS({}),
        unitPrice: 16,
        quantity: 1,
        lineAmount: 16,
      },
    ],
    refund: null,
    createdAt: '2026-09-28T15:58:00',
    expireAt: null,
  },
  {
    id: 102,
    orderNo: 'AC20260928000004',
    status: 'MAKING',
    storeName: STORE_LUJIAZUI,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '5517',
    expectedFinishTime: '2026-09-28T16:12:00',
    totalAmount: 24,
    discountAmount: 0,
    payAmount: 24,
    rewardPoints: 240,
    items: [
      {
        productId: 1,
        productName: '深海美式',
        specSnapshot: SPECS({ 温度: '冰', 糖度: '无糖', 冰量: '正常冰' }),
        unitPrice: 12,
        quantity: 2,
        lineAmount: 24,
      },
    ],
    refund: null,
    createdAt: '2026-09-28T15:50:00',
    expireAt: null,
  },
  {
    id: 103,
    orderNo: 'AC20260928000005',
    status: 'READY',
    storeName: STORE_XUHUI,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '7309',
    expectedFinishTime: '2026-09-28T15:30:00',
    totalAmount: 27,
    discountAmount: 0,
    payAmount: 27,
    rewardPoints: 270,
    items: [
      {
        productId: 2,
        productName: '冷萃远航',
        specSnapshot: SPECS({ 温度: '冰', 糖度: '少糖', 冰量: '正常冰' }),
        unitPrice: 15,
        quantity: 1,
        lineAmount: 15,
      },
      {
        productId: 21,
        productName: '黄油可颂',
        specSnapshot: SPECS({}),
        unitPrice: 12,
        quantity: 1,
        lineAmount: 12,
      },
    ],
    refund: null,
    createdAt: '2026-09-28T15:12:00',
    expireAt: null,
  },
  {
    id: 104,
    orderNo: 'AC20260928000006',
    status: 'COMPLETED',
    storeName: STORE_LUJIAZUI,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '6128',
    expectedFinishTime: null,
    totalAmount: 44,
    discountAmount: 8,
    payAmount: 36,
    rewardPoints: 360,
    items: [
      {
        productId: 9,
        productName: '摩卡航海家',
        specSnapshot: SPECS({ 温度: '热', 糖度: '标准糖' }),
        unitPrice: 22,
        quantity: 1,
        lineAmount: 22,
      },
      {
        productId: 23,
        productName: '提拉米苏',
        specSnapshot: SPECS({}),
        unitPrice: 22,
        quantity: 1,
        lineAmount: 22,
      },
    ],
    refund: null,
    createdAt: '2026-09-28T13:40:00',
    expireAt: null,
  },
  {
    id: 105,
    orderNo: 'AC20260928000007',
    status: 'REFUNDING',
    storeName: STORE_XUHUI,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '9051',
    expectedFinishTime: null,
    totalAmount: 22,
    discountAmount: 0,
    payAmount: 22,
    rewardPoints: 0,
    items: [
      {
        productId: 23,
        productName: '提拉米苏',
        specSnapshot: SPECS({}),
        unitPrice: 22,
        quantity: 1,
        lineAmount: 22,
      },
    ],
    refund: {
      refundNo: 'RF20260928000012',
      amount: 22,
      status: 'APPLYING',
      applyAt: '2026-09-28T16:40:00',
      finishedAt: null,
    },
    createdAt: '2026-09-28T15:02:00',
    expireAt: null,
  },
  {
    id: 106,
    orderNo: 'AC20260928000001',
    status: 'REFUNDED',
    storeName: STORE_BUND,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: '3346',
    expectedFinishTime: null,
    totalAmount: 16,
    discountAmount: 0,
    payAmount: 16,
    rewardPoints: 0,
    items: [
      {
        productId: 8,
        productName: '馥芮白·深海版',
        specSnapshot: SPECS({ 温度: '冰', 糖度: '标准糖', 冰量: '少冰' }),
        unitPrice: 16,
        quantity: 1,
        lineAmount: 16,
      },
    ],
    refund: {
      refundNo: 'RF20260928000001',
      amount: 16,
      status: 'SUCCESS',
      applyAt: '2026-09-28T13:20:00',
      finishedAt: '2026-09-28T13:21:00',
    },
    createdAt: '2026-09-28T13:03:00',
    expireAt: null,
  },
  {
    id: 107,
    orderNo: 'AC20260928000002',
    status: 'CANCELLED',
    storeName: STORE_LUJIAZUI,
    pickupMethod: 'STORE_PICKUP',
    pickupCode: null,
    expectedFinishTime: null,
    totalAmount: 12,
    discountAmount: 3,
    payAmount: 9,
    rewardPoints: 0,
    items: [
      {
        productId: 1,
        productName: '深海美式',
        specSnapshot: SPECS({ 温度: '冰', 糖度: '无糖', 冰量: '正常冰' }),
        unitPrice: 12,
        quantity: 1,
        lineAmount: 12,
      },
    ],
    refund: {
      refundNo: 'RF20260928000002',
      amount: 9,
      status: 'SUCCESS',
      applyAt: '2026-09-28T13:19:00',
      finishedAt: '2026-09-28T13:20:00',
    },
    createdAt: '2026-09-28T13:02:00',
    expireAt: null,
  },
]
