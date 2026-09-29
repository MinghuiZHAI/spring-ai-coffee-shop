<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CategoryGlyph from '@/components/CategoryGlyph.vue'
import { CART_LINES, RECOMMEND_IDS, type CartLine } from '@/data/cart'
import { categoryOf, MENU_ITEMS } from '@/data/menu'

/**
 * 购物车（M1-6 步骤 3a，静态假数据）：
 * 商品行（规格快照 chip + 数量步进器）→ 猜你喜欢横滚 → 底部固定结算悬浮栏
 * （合计 + 预估积分 + 去结算）。预估积分 = 合计金额 × 10（M1 规则：1 元 = 10 分）。
 */
const lines = ref<CartLine[]>(CART_LINES.map((line) => ({ ...line, specs: [...line.specs] })))

const total = computed(() =>
  lines.value.reduce((sum, line) => sum + line.product.price * line.quantity, 0),
)

const totalQty = computed(() => lines.value.reduce((sum, line) => sum + line.quantity, 0))

/** 预估积分（实付 1 元 = 10 分，对齐后端 PointService 规则） */
const estPoints = computed(() => Math.floor(total.value) * 10)

function changeQty(line: CartLine, delta: number) {
  line.quantity = Math.max(1, line.quantity + delta)
}

/** 猜你喜欢：静态阶段与菜单共用假数据 */
const recommends = RECOMMEND_IDS.map((id) => MENU_ITEMS.find((product) => product.id === id)!).map(
  (product) => ({ product, category: categoryOf(product.categoryId) }),
)

function onAddReco(product: { name: string }) {
  ElMessage.success(`已加入购物车：${product.name}`)
}

function onCheckout() {
  ElMessage.info('去结算将在「订单」步骤接入')
}
</script>

<template>
  <div class="cart">
    <header class="cart__head">
      <h2 class="cart__title">购物车</h2>
      <p class="cart__meta">{{ totalQty }} 件商品</p>
    </header>

    <!-- 商品行：规格快照 + 步进器 -->
    <section class="cart__list">
      <article v-for="line in lines" :key="line.productId" class="cart-line">
        <div
          class="cart-line__media"
          :style="{
            background: `linear-gradient(135deg, ${categoryOf(line.product.categoryId).tint[0]}, ${categoryOf(line.product.categoryId).tint[1]})`,
          }"
          role="img"
          :aria-label="`${line.product.name} 商品图占位`"
        >
          <CategoryGlyph :category="categoryOf(line.product.categoryId)" :size="30" />
        </div>

        <div class="cart-line__info">
          <h3 class="cart-line__name">{{ line.product.name }}</h3>
          <div v-if="line.specs.length" class="cart-line__specs">
            <span v-for="spec in line.specs" :key="spec.group" class="cart-line__spec">
              <em>{{ spec.group }}</em>{{ spec.option }}
            </span>
          </div>
          <span class="cart-line__unit">¥{{ line.product.price }}</span>
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
          <span class="cart-line__amount">¥{{ line.product.price * line.quantity }}</span>
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
        <article v-for="reco in recommends" :key="reco.product.id" class="reco-card">
          <div
            class="reco-card__media"
            :style="{ background: `linear-gradient(135deg, ${reco.category.tint[0]}, ${reco.category.tint[1]})` }"
            role="img"
            :aria-label="`${reco.product.name} 商品图占位`"
          >
            <CategoryGlyph :category="reco.category" :size="34" />
          </div>
          <h4 class="reco-card__name">{{ reco.product.name }}</h4>
          <div class="reco-card__foot">
            <span class="reco-card__price">¥{{ reco.product.price }}</span>
            <button
              type="button"
              class="reco-card__add"
              :aria-label="`加入购物车 ${reco.product.name}`"
              @click="onAddReco(reco.product)"
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

    <!-- 底部固定结算悬浮栏 -->
    <footer class="settle-bar">
      <div class="settle-bar__info">
        <span class="settle-bar__total">
          <i>¥</i>{{ total }}
        </span>
        <span class="settle-bar__points">预计获得 {{ estPoints }} 积分</span>
      </div>
      <el-button class="el-button--cta settle-bar__btn" @click="onCheckout">
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
}
</style>
