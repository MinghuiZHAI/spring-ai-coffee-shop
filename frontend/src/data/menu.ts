/**
 * 菜单分类视觉映射（批次 7 收敛）：接口数据来自 GET /api/user/menu（后端 ProductCard
 * 不含视觉字段），分类图占位的渐变/线稿按 V2 种子的分类 id 固定映射，纯视觉元数据。
 * 原 MenuCategory/MenuItem/MENU_ITEMS 静态假数据已随 CartPage 接真退役（data/cart.ts 同批删除）。
 */
export interface CategoryVisual {
  /** 图占位渐变（起/止色） */
  tint: [string, string]
  /** 图占位线条颜色 */
  glyphColor: string
  /** 图占位线稿类型 */
  glyph: 'coffee' | 'latte' | 'citrus' | 'bubbles' | 'bread'
}

export const CATEGORY_VISUALS: Record<number, CategoryVisual> = {
  1: { tint: ['#dcebf5', '#c3ddec'], glyphColor: '#0369a1', glyph: 'coffee' },
  2: { tint: ['#f4efe8', '#e7dccb'], glyphColor: '#8a6a4b', glyph: 'latte' },
  3: { tint: ['#fdeee3', '#fadcc2'], glyphColor: '#c2410c', glyph: 'citrus' },
  4: { tint: ['#e3f2fd', '#c8e4f9'], glyphColor: '#0284c7', glyph: 'bubbles' },
  5: { tint: ['#f5efe2', '#ebdfc8'], glyphColor: '#b45309', glyph: 'bread' },
}

/** 兜底：接口返回未知分类 id 时使用中性视觉 */
export const FALLBACK_VISUAL: CategoryVisual = {
  tint: ['#e3f2fd', '#c8e4f9'],
  glyphColor: '#0369a1',
  glyph: 'coffee',
}

export function visualOf(categoryId: number): CategoryVisual {
  return CATEGORY_VISUALS[categoryId] ?? FALLBACK_VISUAL
}
