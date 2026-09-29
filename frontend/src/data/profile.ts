/**
 * 个人中心静态假数据（M1-6 步骤 3c）：字段口径对齐
 * GET /api/user/points（PointItem：时间/变动值/类型/关联订单）、
 * GET /api/user/coupons（CouponView：名称/门槛/面额/有效期/状态）与 V1 表结构；
 * 等级命名与升级规则出自 V2 种子《会员与积分规则》KB 文档。
 * 接后端后由对应接口替换。
 */

/** 静态用户（V2 种子 USER 账号 13800000001 张三） */
export const PROFILE_USER = {
  nickname: '张三',
  phoneMasked: '138****0001',
  role: 'USER',
  memberLevel: 1,
}

/** 等级名映射（DB 存 TINYINT 1/2/3，等级名为前端展示职责） */
export const MEMBER_LEVEL_LABEL: Record<number, string> = {
  1: 'L1 航海员',
  2: 'L2 领航员',
  3: 'L3 船长',
}

/** 升级规则文案（KB 文档原文口径） */
export const MEMBER_LEVEL_RULES =
  'L2 领航员：累计实付满 300 元自动升级，积分获取 1.2 倍；L3 船长：累计实付满 1000 元，积分获取 1.5 倍'

export const POINT_BALANCE = 850

/** 积分使用口径（KB 文档原文） */
export const POINT_USAGE_NOTE = '当前版本支持积分累积与查询，抵扣与兑换将在后续版本上线'

/** 后端唯一写入类型为 EARN（实付 1 元 = 10 分，备注"下单获得"） */
export interface PointRecordItem {
  id: number
  createdAt: string
  changeValue: number
  type: string
  relatedOrderId: number
}

export const POINT_RECORDS: PointRecordItem[] = [
  { id: 3, createdAt: '2026-09-28T15:50:00', changeValue: 240, type: 'EARN', relatedOrderId: 102 },
  { id: 2, createdAt: '2026-09-28T13:40:00', changeValue: 360, type: 'EARN', relatedOrderId: 104 },
  { id: 1, createdAt: '2026-09-28T15:12:00', changeValue: 270, type: 'EARN', relatedOrderId: 103 },
]

export type CouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

export interface MyCoupon {
  id: number
  name: string
  thresholdAmount: number
  discountAmount: number
  expireAt: string
  status: CouponStatus
  usedOrderNo?: string
}

export const MY_COUPONS: MyCoupon[] = [
  {
    id: 1,
    name: '新航程·满20减5券',
    thresholdAmount: 20,
    discountAmount: 5,
    expireAt: '2026-10-28',
    status: 'UNUSED',
  },
  {
    id: 2,
    name: '无门槛 3 元尝鲜券',
    thresholdAmount: 0,
    discountAmount: 3,
    expireAt: '2026-10-13',
    status: 'UNUSED',
  },
  {
    id: 3,
    name: '深海会员·满30减8券',
    thresholdAmount: 30,
    discountAmount: 8,
    expireAt: '2026-10-28',
    status: 'USED',
    usedOrderNo: 'AC20260928000006',
  },
  {
    id: 4,
    name: '新航程·满20减5券',
    thresholdAmount: 20,
    discountAmount: 5,
    expireAt: '2026-09-25',
    status: 'EXPIRED',
  },
]

export const COUPON_STATUS_LABEL: Record<CouponStatus, string> = {
  UNUSED: '未使用',
  USED: '已使用',
  EXPIRED: '已过期',
}

/** 领券中心：V2 coupon_template 全量 3 张（领取后 valid_days 天内有效） */
export interface ClaimableTemplate {
  id: number
  name: string
  thresholdAmount: number
  discountAmount: number
  validDays: number
}

export const CLAIMABLE_TEMPLATES: ClaimableTemplate[] = [
  { id: 11, name: '新航程·满20减5券', thresholdAmount: 20, discountAmount: 5, validDays: 30 },
  { id: 12, name: '深海会员·满30减8券', thresholdAmount: 30, discountAmount: 8, validDays: 30 },
  { id: 13, name: '无门槛 3 元尝鲜券', thresholdAmount: 0, discountAmount: 3, validDays: 15 },
]
