/**
 * 规格快照解析（批次 7 自 data/orders.ts 提取共用）：
 * specSnapshot 为 JSON 对象字符串（如 {"温度":"冰","糖度":"半糖"}，烘焙小食为 "{}"），
 * 解析为组名/选项对供购物车行与订单详情渲染；空对象/解析失败返回空数组。
 */
export interface SpecItem {
  group: string
  option: string
}

export function parseSpecSnapshot(specSnapshot: string): SpecItem[] {
  if (!specSnapshot || specSnapshot === '{}') return []
  try {
    const parsed = JSON.parse(specSnapshot) as Record<string, string>
    return Object.entries(parsed).map(([group, option]) => ({ group, option }))
  } catch {
    return []
  }
}
