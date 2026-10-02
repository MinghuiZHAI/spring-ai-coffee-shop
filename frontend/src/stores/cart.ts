import { defineStore } from 'pinia'
import type { CartItemView, CartView } from '@/api/cart'
import { addCartItem, deleteCartItem, getCart, updateCartItem } from '@/api/cart'

/**
 * 购物车 store（批次 7）：菜单加购 / 购物车页 / AppLayout 角标的唯一数据源。
 * 后端所有变更操作的响应都是最新 CartView，动作后整包替换；badgeCount 供导航角标。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    lines: [] as CartItemView[],
    totalAmount: 0,
    loading: false,
  }),

  getters: {
    badgeCount: (state) => state.lines.reduce((sum, line) => sum + line.quantity, 0),
  },

  actions: {
    apply(view: CartView) {
      this.lines = view.items
      this.totalAmount = view.totalAmount
    },

    async fetch() {
      this.loading = true
      try {
        this.apply(await getCart())
      } finally {
        this.loading = false
      }
    },

    /** 加购（批次 7 决策：specs 直传空对象，后端已验证合法；完整规格选择器列 M2） */
    async add(productId: number, specs: Record<string, string>, quantity = 1) {
      this.apply(await addCartItem({ productId, specs, quantity }))
    },

    async updateQty(id: number, quantity: number) {
      this.apply(await updateCartItem(id, quantity))
    },

    async remove(id: number) {
      this.apply(await deleteCartItem(id))
    },
  },
})
