<script setup lang="ts">
import type { MenuCategory, MenuItem } from '@/data/menu'

/**
 * 商品卡（M1-6 步骤 2）：分类渐变图占位（真实商品图就位后替换）、名称、
 * 两行描述、口味标签 chip、价格（tabular 数字）、烘焙琥珀 CTA。
 * 入场 stagger：delay 由父级按网格序传入（40ms/卡）。
 */
const props = defineProps<{
  product: MenuItem
  category: MenuCategory
  index: number
}>()

defineEmits<{
  add: [product: MenuItem]
}>()

/** 分类线稿 glyph（Lucide 风格，与底栏图标同一视觉语言） */
const GLYPHS: Record<
  MenuCategory['glyph'],
  { paths: string[]; circles?: Array<[number, number, number]>; dots?: Array<[number, number]> }
> = {
  coffee: {
    paths: [
      'M17 8h1a4 4 0 1 1 0 8h-1',
      'M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z',
      'M6 2v2',
      'M10 2v2',
      'M14 2v2',
    ],
  },
  latte: {
    paths: ['M8 3h8l-1 15a3 3 0 0 1-6 0L8 3Z', 'M8.6 9h6.8', 'M9.2 13h5.6'],
  },
  citrus: {
    paths: ['M12 6.5v11', 'M6.5 12h11', 'M8.2 8.2l7.6 7.6', 'M15.8 8.2l-7.6 7.6'],
    circles: [[12, 12, 7.5]],
  },
  bubbles: {
    paths: ['M7 3h10l-1.2 16a2 2 0 0 1-2 1.8h-3.6a2 2 0 0 1-2-1.8L7 3Z', 'M14.5 3L17 1.2'],
    dots: [
      [10, 16.5],
      [13, 18],
    ],
  },
  bread: {
    paths: [
      'M5 11c0-2.8 3.1-5 7-5s7 2.2 7 5v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6Z',
      'M9 15l1.2-5',
      'M13 15l1.2-5',
    ],
  },
}

const glyph = GLYPHS[props.category.glyph]
</script>

<template>
  <article class="pcard" :style="{ animationDelay: `${props.index * 40}ms` }">
    <div
      class="pcard__media"
      :style="{ background: `linear-gradient(135deg, ${props.category.tint[0]}, ${props.category.tint[1]})` }"
      role="img"
      :aria-label="`${props.product.name} 商品图占位`"
    >
      <svg
        class="pcard__glyph"
        viewBox="0 0 24 24"
        fill="none"
        :stroke="props.category.glyphColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path v-for="(d, i) in glyph.paths" :key="i" :d="d" />
        <circle
          v-for="(c, i) in glyph.circles ?? []"
          :key="`c${i}`"
          :cx="c[0]"
          :cy="c[1]"
          :r="c[2]"
        />
        <circle
          v-for="(d, i) in glyph.dots ?? []"
          :key="`d${i}`"
          :cx="d[0]"
          :cy="d[1]"
          r="0.9"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    </div>

    <div class="pcard__body">
      <h3 class="pcard__name">{{ props.product.name }}</h3>
      <p class="pcard__desc">{{ props.product.description }}</p>

      <div class="pcard__tags">
        <span v-for="tag in props.product.tags.slice(0, 2)" :key="tag" class="pcard__tag">
          {{ tag }}
        </span>
      </div>

      <div class="pcard__foot">
        <span class="pcard__price">
          <span class="pcard__price-symbol">¥</span>
          <span class="pcard__price-value">{{ props.product.price }}</span>
        </span>

        <el-button class="el-button--cta" size="small" @click="$emit('add', props.product)">
          <svg
            class="pcard__cta-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <circle cx="8" cy="21" r="1" />
            <circle cx="19" cy="21" r="1" />
            <path
              d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"
            />
          </svg>
          加入购物车
        </el-button>
      </div>
    </div>
  </article>
</template>

<style scoped lang="scss">
.pcard {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
  animation: pcard-in var(--ac-dur-base) var(--ac-ease-enter) backwards;
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-md);
  }
}

@keyframes pcard-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

.pcard__media {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 4 / 3;
}

.pcard__glyph {
  width: 44px;
  height: 44px;
  opacity: 0.9;
}

.pcard__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--ac-space-2);
  padding: var(--ac-space-4);
}

.pcard__name {
  font-family: var(--ac-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.pcard__desc {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.55;
  color: var(--ac-text-dim);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.pcard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ac-space-1);
}

.pcard__tag {
  padding: 1px 8px;
  font-size: 11px;
  color: var(--ac-primary);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
}

.pcard__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: var(--ac-space-2);
}

/* 价格：Poppins + tabular 数字，价格是最突出的信息 */
.pcard__price {
  display: inline-flex;
  align-items: baseline;
  gap: 1px;
  font-family: var(--ac-font-display);
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);
}

.pcard__price-symbol {
  font-size: 12px;
  font-weight: 500;
  color: var(--ac-text-dim);
}

.pcard__price-value {
  font-size: 19px;
  font-weight: 600;
  line-height: 1;
}

.pcard__cta-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
}
</style>
