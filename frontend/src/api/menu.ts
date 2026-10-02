/**
 * 菜单接口（批次 7，契约=后端 ProductDtos）：GET /api/user/menu。
 * 分类视觉（tint/glyph）是纯前端元数据，见 data/menu.ts。
 */
import { request } from './client'

export interface ProductCardDto {
  id: number
  name: string
  description: string
  basePrice: number
  tags: string[]
}

export interface MenuCategoryNode {
  id: number
  name: string
  products: ProductCardDto[]
}

export interface MenuResponse {
  categories: MenuCategoryNode[]
}

export function getMenu(): Promise<MenuResponse> {
  return request<MenuResponse>('/api/user/menu')
}
