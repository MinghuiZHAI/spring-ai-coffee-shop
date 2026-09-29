<script setup lang="ts">
import { computed } from 'vue'
import type { MenuCategory } from '@/data/menu'

/**
 * 分类线稿 glyph（Lucide 风格，与底栏图标同一视觉语言）。
 * 商品卡 / 购物车行 / 推荐横滚共用，保证占位图形的视觉一致性。
 */
const props = defineProps<{
  category: MenuCategory
  size?: number
}>()

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

const glyph = computed(() => GLYPHS[props.category.glyph])
const sizePx = computed(() => `${props.size ?? 44}px`)
</script>

<template>
  <svg
    :width="sizePx"
    :height="sizePx"
    viewBox="0 0 24 24"
    fill="none"
    :stroke="props.category.glyphColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="(d, i) in glyph.paths" :key="`p${i}`" :d="d" />
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
</template>
