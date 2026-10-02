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

export interface SpecOptionDto {
  optionName: string
  priceDelta: number
}

/** group 为枚举名（TEMPERATURE/SWEETNESS/ICE），label 为中文展示名 */
export interface SpecGroupDto {
  group: string
  label: string
  options: SpecOptionDto[]
}

export interface ProductDetail {
  id: number
  categoryId: number
  categoryName: string
  name: string
  description: string
  basePrice: number
  tags: string[]
  specs: SpecGroupDto[]
}

export function getProduct(id: number): Promise<ProductDetail> {
  return request<ProductDetail>(`/api/user/products/${id}`)
}
