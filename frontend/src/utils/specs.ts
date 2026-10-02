/**
 * 规格快照解析与展示助手（批次 7 提取，M2 批次 1 扩展）：
 * specSnapshot 为 JSON 对象字符串（key 为规格组枚举名 TEMPERATURE/SWEETNESS/ICE，
 * 如 {"TEMPERATURE":"冰","SWEETNESS":"无糖"}，烘焙小食为 "{}"），
 * 解析为组名/中文 label/选项对供购物车行与订单详情渲染；空对象/解析失败返回空数组。
 */

/** 规格组枚举名 → 中文展示名（与后端 ProductQueryService.GROUP_LABELS 同口径） */
export const SPEC_GROUP_LABELS: Record<string, string> = {
  TEMPERATURE: '温度',
  SWEETNESS: '糖度',
  ICE: '冰量',
}

export interface SpecItem {
  group: string
  /** 中文展示名（快照 key 为枚举名，展示前经 SPEC_GROUP_LABELS 映射） */
  label: string
  option: string
}

export function parseSpecSnapshot(specSnapshot: string): SpecItem[] {
  if (!specSnapshot || specSnapshot === '{}') return []
  try {
    const parsed = JSON.parse(specSnapshot) as Record<string, string>
    return Object.entries(parsed).map(([group, option]) => ({
      group,
      label: SPEC_GROUP_LABELS[group] ?? group,
      option,
    }))
  } catch {
    return []
  }
}

/** 差价展示：0 → 空串；正数 → "+3 元"；负数 → "−2 元"（防御性，当前无负差价场景） */
export function formatSpecDelta(delta: number): string {
  if (!delta) return ''
  return delta > 0 ? `+${delta} 元` : `−${Math.abs(delta)} 元`
}
