<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CategoryGlyph from '@/components/CategoryGlyph.vue'
import { useCartStore } from '@/stores/cart'
import { listStores, type StoreBrief } from '@/api/store'
import { getMenu } from '@/api/menu'
import { createOrder } from '@/api/order'
import { visualOf, type CategoryVisual } from '@/data/menu'
import { parseSpecSnapshot } from '@/utils/specs'

/**
 * 购物车（批次 7 接真数据）：cartStore 驱动商品行（specSnapshot 解析 + 步进器 PUT、
 * 删除 DELETE）→ 猜你喜欢横滚（真菜单数据，排除已在购物车的商品）→ 底部固定结算悬浮栏
 * （门店下拉 GET /api/user/stores + 合计 totalAmount + 预估积分 ×10）
 * → POST /api/user/orders（批次 7 决策：不带券 userCouponId=null，选券列 M2）→ 跳订单详情。
 */
const cartStore = useCartStore()
const router = useRouter()

const lines = computed(() => cartStore.lines)
const totalAmount = computed(() => cartStore.totalAmount)
const totalQty = computed(() => cartStore.badgeCount)
const estPoints = computed(() => Math.floor(totalAmount.value) * 10)

const stores = ref<StoreBrief[]>([])
const storeId = ref<number | null>(null)
const submitting = ref(false)

/** 行/推荐卡的分类视觉：商品 id → 分类视觉映射来自真菜单（一次拉取复用） */
const menuProducts = ref<Array<{ id: number; name: string; basePrice: number; visual: CategoryVisual }>>([])

onMounted(async () => {
  await cartStore.fetch().catch(() => {})
  try {
    const [storesRes, menuRes] = await Promise.all([listStores(), getMenu()])
    stores.value = storesRes.stores
    storeId.value = storesRes.stores[0]?.id ?? null
    menuProducts.value = menuRes.categories.flatMap((c) =>
      c.products.map((p) => ({ id: p.id, name: p.name, basePrice: p.basePrice, visual: visualOf(c.id) })),
    )
  } catch {
    // 门店/菜单拉取失败不阻塞购物车本体（结算时再校验门店）
  }
})

const recommends = computed(() =>
  menuProducts.value.filter((p) => !lines.value.some((l) => l.productId === p.id)).slice(0, 6),
)

function visualOfProduct(productId: number): CategoryVisual {
  return menuProducts.value.find((p) => p.id === productId)?.visual ?? visualOf(0)
}

async function changeQty(line: (typeof lines.value)[number], delta: number) {
  const next = line.quantity + delta
  if (next < 1) return
  await cartStore.updateQty(line.id, next)
}

async function removeLine(line: (typeof lines.value)[number]) {
  await cartStore.remove(line.id)
}

async function onAddReco(productId: number, name: string) {
  await cartStore.add(productId, {}, 1)
  ElMessage.success(`已加入购物车：${name}`)
}

async function onCheckout() {
  if (submitting.value || lines.value.length === 0) return
  if (storeId.value === null) {
    ElMessage.warning('请选择自取门店')
    return
  }
  submitting.value = true
  try {
    const created = await createOrder({
      storeId: storeId.value,
      pickupMethod: 'STORE_PICKUP',
      cartItemIds: lines.value.map((l) => l.id),
      userCouponId: null,
    })
    await cartStore.fetch().catch(() => {})
    ElMessage.success(`下单成功：${created.orderNo}，请尽快支付`)
    router.push(`/orders/${created.orderId}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cart">
    <header class="cart__head">
      <h2 class="cart__title">购物车</h2>
      <p class="cart__meta">{{ totalQty }} 件商品</p>
    </header>

    <!-- 空态：真购物车可为空 -->
    <section v-if="!cartStore.loading && lines.length === 0" class="cart__empty">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M17 8h1a4 4 0 1 1 0 8h-1" />
        <path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z" />
        <path d="M6 2v2" />
        <path d="M10 2v2" />
        <path d="M14 2v2" />
      </svg>
      <p class="cart__empty-title">购物车还是空的</p>
      <p class="cart__empty-hint">去菜单挑一杯，开启今天的航行</p>
      <RouterLink to="/menu" class="cart__empty-cta">去点单</RouterLink>
    </section>

    <!-- 商品行：规格快照 + 步进器 -->
    <section v-else class="cart__list">
      <article v-for="line in lines" :key="line.id" class="cart-line">
        <div
          class="cart-line__media"
          :style="{
            background: `linear-gradient(135deg, ${visualOfProduct(line.productId).tint[0]}, ${visualOfProduct(line.productId).tint[1]})`,
          }"
          role="img"
          :aria-label="`${line.productName} 商品图占位`"
        >
          <CategoryGlyph :category="visualOfProduct(line.productId)" :size="30" />
        </div>

        <div class="cart-line__info">
          <h3 class="cart-line__name">{{ line.productName }}</h3>
          <div v-if="parseSpecSnapshot(line.specSnapshot).length" class="cart-line__specs">
            <span
              v-for="spec in parseSpecSnapshot(line.specSnapshot)"
              :key="spec.group"
              class="cart-line__spec"
            >
              <em>{{ spec.group }}</em>{{ spec.option }}
            </span>
          </div>
          <span class="cart-line__unit">¥{{ line.unitPrice }}</span>
        </div>

        <div class="cart-line__side">
          <div class="stepper" role="group" aria-label="调整数量">
            <button
              type="button"
              class="stepper__btn"
              :disabled="line.quantity <= 1"
              aria-label="减少数量"
              @click="changeQty(line, -1)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M5 12h14" />
              </svg>
            </button>
            <span class="stepper__num">{{ line.quantity }}</span>
            <button
              type="button"
              class="stepper__btn"
              aria-label="增加数量"
              @click="changeQty(line, 1)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
            </button>
          </div>
          <div class="cart-line__side-bottom">
            <span class="cart-line__amount">¥{{ line.unitPrice * line.quantity }}</span>
            <button type="button" class="cart-line__remove" @click="removeLine(line)">删除</button>
          </div>
        </div>
      </article>
    </section>

    <!-- 猜你喜欢横滚 -->
    <section class="cart__reco">
      <header class="cart__reco-head">
        <h3>猜你喜欢</h3>
        <p>根据你的口味推荐</p>
      </header>
      <div class="cart__reco-rail">
        <article v-for="reco in recommends" :key="reco.id" class="reco-card">
          <div
            class="reco-card__media"
            :style="{ background: `linear-gradient(135deg, ${reco.visual.tint[0]}, ${reco.visual.tint[1]})` }"
            role="img"
            :aria-label="`${reco.name} 商品图占位`"
          >
            <CategoryGlyph :category="reco.visual" :size="34" />
          </div>
          <h4 class="reco-card__name">{{ reco.name }}</h4>
          <div class="reco-card__foot">
            <span class="reco-card__price">¥{{ reco.basePrice }}</span>
            <button
              type="button"
              class="reco-card__add"
              :aria-label="`加入购物车 ${reco.name}`"
              @click="onAddReco(reco.id, reco.name)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true">
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
            </button>
          </div>
        </article>
      </div>
    </section>

    <!-- 底部固定结算悬浮栏：门店选择 + 合计 + 下单 -->
    <footer v-if="lines.length > 0" class="settle-bar">
      <div class="settle-bar__info">
        <div class="settle-bar__store">
          <span class="settle-bar__store-label">自取门店</span>
          <el-select v-model="storeId" class="settle-bar__store-select" size="small" placeholder="选择门店">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </div>
        <div class="settle-bar__summary">
          <span class="settle-bar__total">
            <i>¥</i>{{ totalAmount }}
          </span>
          <span class="settle-bar__points">预计获得 {{ estPoints }} 积分</span>
        </div>
      </div>
      <el-button
        class="el-button--cta settle-bar__btn"
        :loading="submitting"
        @click="onCheckout"
      >
        去结算（{{ totalQty }}）
      </el-button>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.cart__head {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-3);
  margin-bottom: var(--ac-space-5);
}

.cart__title {
  font-size: 22px;
}

.cart__meta {
  margin: 0;
  font-size: 13px;
  color: var(--ac-text-dim);
}

/* ===== 空态 ===== */
.cart__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-2);
  padding: var(--ac-space-8) 0;

  svg {
    width: 44px;
    height: 44px;
    color: var(--ac-text-dim);
    opacity: 0.5;
  }
}

.cart__empty-title {
  margin: var(--ac-space-2) 0 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ac-text);
}

.cart__empty-hint {
  margin: 0;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.cart__empty-cta {
  margin-top: var(--ac-space-3);
  padding: 8px 28px;
  font-size: 14px;
  color: #fff;
  text-decoration: none;
  background: var(--ac-cta);
  border-radius: var(--ac-radius-pill);
  transition: background-color var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    background: var(--ac-cta-hover);
  }
}

/* ===== 商品行 ===== */
.cart__list {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-3);
}

.cart-line {
  display: flex;
  align-items: center;
  gap: var(--ac-space-4);
  padding: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.cart-line__media {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 12px;
}

.cart-line__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--ac-space-1);
  min-width: 0;
}

.cart-line__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.cart-line__specs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ac-space-1);
}

.cart-line__spec {
  padding: 1px 8px;
  font-size: 11px;
  color: var(--ac-text);
  background: var(--ac-bg);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);

  /* 组名弱化为前缀标签 */
  em {
    font-style: normal;
    margin-right: 3px;
    color: var(--ac-text-dim);
  }
}

.cart-line__unit {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.cart-line__side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--ac-space-2);
}

.cart-line__side-bottom {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
}

.cart-line__remove {
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--ac-text-dim);
  cursor: pointer;
  padding: 0;

  &:hover {
    color: var(--ac-danger);
  }
}

/* ===== 数量步进器 ===== */
.stepper {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
}

.stepper__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  transition:
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter),
    transform var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  svg {
    width: 14px;
    height: 14px;
  }

  &:hover:not(:disabled) {
    color: var(--ac-primary);
    border-color: var(--ac-primary);
  }

  &:active:not(:disabled) {
    transform: scale(0.92);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.stepper__num {
  min-width: 20px;
  text-align: center;
  font-family: var(--ac-font-display);
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

.cart-line__amount {
  font-family: var(--ac-font-display);
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

/* ===== 猜你喜欢横滚 ===== */
.cart__reco {
  margin-top: var(--ac-space-8);
}

.cart__reco-head {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-2);
  margin-bottom: var(--ac-space-3);

  h3 {
    font-size: 16px;
    font-weight: 600;
  }

  p {
    margin: 0;
    font-size: 12px;
    color: var(--ac-text-dim);
  }
}

.cart__reco-rail {
  display: flex;
  gap: var(--ac-space-3);
  margin: calc(-1 * var(--ac-space-4)) calc(-1 * var(--ac-space-4)) 0;
  padding: 0 var(--ac-space-4) var(--ac-space-2);
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.reco-card {
  flex: none;
  width: 132px;
  padding: var(--ac-space-2);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
}

.reco-card__media {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 76px;
  border-radius: 12px;
}

.reco-card__name {
  margin: var(--ac-space-2) 0 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--ac-primary-deep);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reco-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--ac-space-1);
}

.reco-card__price {
  font-family: var(--ac-font-display);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

.reco-card__add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #fff;
  background: var(--ac-cta);
  border: none;
  border-radius: var(--ac-radius-pill);
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    transform var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  svg {
    width: 12px;
    height: 12px;
  }

  &:hover {
    background: var(--ac-cta-hover);
  }

  &:active {
    transform: scale(0.92);
  }
}

/* ===== 底部固定结算悬浮栏 ===== */
.settle-bar {
  position: fixed;
  inset: auto 12px calc(var(--ac-tabbar-h) + env(safe-area-inset-bottom, 0px) + 12px) 12px;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ac-space-4);
  padding: var(--ac-space-3) var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-lg);
}

.settle-bar__info {
  display: flex;
  align-items: center;
  gap: var(--ac-space-4);
  min-width: 0;
}

.settle-bar__store {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.settle-bar__store-label {
  flex: none;
  font-size: 11px;
  color: var(--ac-text-dim);
}

.settle-bar__store-select {
  width: 128px;
}

.settle-bar__summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.settle-bar__total {
  font-family: var(--ac-font-display);
  font-size: 24px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--ac-primary-deep);

  i {
    margin-right: 2px;
    font-size: 14px;
    font-style: normal;
    font-weight: 500;
    color: var(--ac-text-dim);
  }
}

.settle-bar__points {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.settle-bar__btn {
  flex: none;
}

/* ===== PC（≥768px）：悬浮栏居中悬浮于内容区下方 ===== */
@media (min-width: 768px) {
  .settle-bar {
    inset: auto auto 24px 50%;
    width: calc(var(--ac-content-max) - 2 * var(--ac-space-6));
    max-width: calc(100vw - 48px);
    transform: translateX(-50%);
    padding: var(--ac-space-4) var(--ac-space-6);
  }

  /* 内容为悬浮栏预留空间 */
  .cart {
    padding-bottom: 160px;
  }
}

@media (max-width: 767.98px) {
  .cart {
    padding-bottom: calc(var(--ac-tabbar-h) + 140px);
  }

  /* 移动端悬浮栏收纳：门店选择下移一行，避免挤压合计 */
  .settle-bar {
    flex-direction: column;
    align-items: stretch;
    gap: var(--ac-space-2);
  }

  .settle-bar__info {
    flex-direction: column;
    align-items: stretch;
    gap: var(--ac-space-2);
  }

  .settle-bar__store {
    justify-content: space-between;
  }

  .settle-bar__store-select {
    flex: 1;
  }

  .settle-bar__summary {
    flex-direction: row;
    align-items: baseline;
    justify-content: space-between;
  }
}
</style>
