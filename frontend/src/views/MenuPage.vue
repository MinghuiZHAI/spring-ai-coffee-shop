<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ProductCard from '@/components/ProductCard.vue'
import SpecPicker from '@/components/SpecPicker.vue'
import { visualOf } from '@/data/menu'
import { getMenu, type MenuCategoryNode, type ProductCardDto } from '@/api/menu'

/**
 * 菜单页（批次 7 接真数据 + M2 批次 1 规格选择器）：GET /api/user/menu 驱动分类与商品卡；
 * 分类胶囊（移动端顶部横滚 / PC 左侧粘性栏）+ 商品卡网格（移动 2 列 / PC 3 列），
 * 切换分类重放 40ms stagger 入场。分类视觉（tint/glyph）按分类 id 走 data/menu 映射。
 * 加入购物车 → SpecPicker 规格弹窗（默认预填 + 差价试算）→ cartStore.add 带 specs。
 */

const categories = ref<MenuCategoryNode[]>([])
const activeId = ref<number | null>(null)
const loading = ref(true)

const activeCategory = computed(() => categories.value.find((c) => c.id === activeId.value) ?? null)

onMounted(async () => {
  try {
    categories.value = (await getMenu()).categories
    activeId.value = categories.value[0]?.id ?? null
  } finally {
    loading.value = false
  }
})

const filteredItems = computed(() => activeCategory.value?.products ?? [])

function countOf(categoryId: number): number {
  return categories.value.find((c) => c.id === categoryId)?.products.length ?? 0
}

/** 加购 → 弹规格选择器（M2 批次 1） */
const pickerProduct = ref<ProductCardDto | null>(null)
const pickerVisible = ref(false)

function onAdd(product: ProductCardDto) {
  pickerProduct.value = product
  pickerVisible.value = true
}

function onAdded(name: string, specText: string) {
  ElMessage.success(`已加入购物车：${name}（${specText}）`)
}
</script>

<template>
  <div class="menu">
    <header class="menu__head">
      <h2 class="menu__title">菜单</h2>
      <p class="menu__meta">
        <template v-if="!loading">{{ categories.reduce((sum, c) => sum + c.products.length, 0) }} 款 · 每日现制</template>
        <template v-else>加载中…</template>
      </p>
    </header>

    <div class="menu__layout">
      <!-- 分类：移动端顶部横滚胶囊 / PC 左侧粘性栏 -->
      <nav class="menu__cats" aria-label="商品分类">
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="menu__cat"
          :class="{ 'is-active': category.id === activeId }"
          @click="activeId = category.id"
        >
          <span class="menu__cat-name">{{ category.name }}</span>
          <span class="menu__cat-count">{{ countOf(category.id) }}</span>
        </button>
      </nav>

      <!-- :key 驱动重挂载：切换分类时重放 40ms stagger 入场 -->
      <div :key="activeId ?? 'loading'" class="menu__grid">
        <template v-if="!loading">
          <ProductCard
            v-for="(item, index) in filteredItems"
            :key="item.id"
            :product="item"
            :category="visualOf(activeId ?? 0)"
            :index="index"
            @add="onAdd"
          />
        </template>
        <p v-if="!loading && filteredItems.length === 0" class="menu__empty">该分类暂无在售商品</p>
      </div>
    </div>

    <SpecPicker
      :product="pickerProduct"
      :visible="pickerVisible"
      @close="pickerVisible = false"
      @added="onAdded"
    />
  </div>
</template>

<style scoped lang="scss">
.menu__head {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-3);
  margin-bottom: var(--ac-space-5);
}

.menu__title {
  font-size: 22px;
}

.menu__meta {
  margin: 0;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.menu__empty {
  margin: 0;
  grid-column: 1 / -1;
  padding: var(--ac-space-6) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

/* 分类：移动端顶部横滚胶囊（粘性，贴在导航下方） */
.menu__cats {
  display: flex;
  gap: var(--ac-space-2);
  position: sticky;
  top: var(--ac-nav-h-mobile);
  z-index: 10;
  margin: calc(-1 * var(--ac-space-4)) calc(-1 * var(--ac-space-4)) var(--ac-space-5);
  padding: var(--ac-space-3) var(--ac-space-4);
  overflow-x: auto;
  background: var(--ac-bg);
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.menu__cat {
  display: inline-flex;
  align-items: center;
  gap: var(--ac-space-2);
  flex: none;
  padding: 8px var(--ac-space-4);
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text-dim);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  cursor: pointer;
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);

  &.is-active {
    color: var(--ac-primary-deep);
    font-weight: 600;
    background: var(--ac-tide);
    border-color: transparent;
  }
}

.menu__cat-count {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  opacity: 0.75;
}

/* 商品网格：移动 2 列 */
.menu__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ac-space-4);
}

/* PC（≥768px）：左侧粘性分类栏 + 3 列网格，激活项带洋流弧条 */
@media (min-width: 768px) {
  .menu__layout {
    display: grid;
    grid-template-columns: 188px 1fr;
    gap: var(--ac-space-6);
    align-items: start;
  }

  .menu__cats {
    flex-direction: column;
    position: sticky;
    top: calc(var(--ac-nav-h) + var(--ac-space-6));
    margin: 0;
    padding: 0;
    overflow: visible;
  }

  .menu__cat {
    position: relative;
    width: 100%;
    justify-content: space-between;
    padding: var(--ac-space-3) var(--ac-space-4);
    border-radius: var(--ac-radius-btn);
    text-align: left;

    &.is-active::before {
      content: '';
      position: absolute;
      left: 0;
      top: 20%;
      bottom: 20%;
      width: 3px;
      border-radius: var(--ac-radius-pill);
      background: var(--ac-current-gradient);
    }
  }

  .menu__grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
