import { MENU_ITEMS, type MenuItem } from './menu'

/**
 * 购物车静态假数据（M1-6 步骤 3a）：规格快照对齐 V2 种子的全局规格组
 * （温度/糖度/冰量，烘焙小食无规格）；接后端后由购物车接口替换。
 */

export interface SpecSnapshotItem {
  /** 规格组名：温度/糖度/冰量 */
  group: string
  /** 选项值：冰/热、标准糖/半糖/少糖/无糖、正常冰/少冰/去冰 */
  option: string
}

export interface CartLine {
  productId: number
  /** 静态阶段冗余商品引用（接后端后由接口聚合） */
  product: MenuItem
  specs: SpecSnapshotItem[]
  quantity: number
}

const productOf = (id: number): MenuItem => MENU_ITEMS.find((product) => product.id === id)!

export const CART_LINES: CartLine[] = [
  {
    productId: 5,
    product: productOf(5),
    specs: [
      { group: '温度', option: '冰' },
      { group: '糖度', option: '半糖' },
      { group: '冰量', option: '少冰' },
    ],
    quantity: 1,
  },
  {
    productId: 11,
    product: productOf(11),
    specs: [
      { group: '温度', option: '冰' },
      { group: '糖度', option: '标准糖' },
      { group: '冰量', option: '正常冰' },
    ],
    quantity: 1,
  },
  {
    productId: 21,
    product: productOf(21),
    specs: [],
    quantity: 2,
  },
]

/** 猜你喜欢：不在购物车中的商品 id（静态阶段直接取菜单数据） */
export const RECOMMEND_IDS = [4, 13, 18, 23]
