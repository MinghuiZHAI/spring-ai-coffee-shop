<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getProduct, type ProductCardDto, type SpecGroupDto } from '@/api/menu'
import { useCartStore } from '@/stores/cart'
import { formatSpecDelta } from '@/utils/specs'

/**
 * 规格选择弹窗（M2 批次 1，04 v1.7.1）：GET /products/{id} specGroups → 三组单选
 * （默认每组第一项：冰/标准糖/正常冰），实时差价试算；确认调 cartStore.add 带 specs——
 * 差价由后端按 spec_option.price_delta 计入 unitPrice，前端只提交规格选择值
 * （key 为规格组枚举名，与后端 CartService 查字典口径一致）。
 */
const props = defineProps<{ product: ProductCardDto | null; visible: boolean }>()

const emit = defineEmits<{ close: []; added: [name: string, specText: string] }>()

const cartStore = useCartStore()

const loading = ref(false)
const specGroups = ref<SpecGroupDto[]>([])
/** 每组选中的 optionName（key = 规格组枚举名） */
const selection = reactive<Record<string, string>>({})

watch(
  () => props.visible,
  async (open) => {
    if (!open || !props.product) return
    loading.value = true
    specGroups.value = []
    try {
      const detail = await getProduct(props.product.id)
      specGroups.value = detail.specs
      for (const g of detail.specs) {
        selection[g.group] = g.options[0]?.optionName ?? ''
      }
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '规格加载失败，请稍后重试')
      emit('close')
    } finally {
      loading.value = false
    }
  },
)

/** 差价合计 = Σ 选中选项的 priceDelta（实时试算） */
const deltaTotal = computed(() =>
  specGroups.value.reduce((sum, g) => {
    const opt = g.options.find((o) => o.optionName === selection[g.group])
    return sum + (opt?.priceDelta ?? 0)
  }, 0),
)

const trialPrice = computed(() => (props.product?.basePrice ?? 0) + deltaTotal.value)

const specText = computed(() =>
  specGroups.value.map((g) => `${g.label} ${selection[g.group]}`).join(' / '),
)

const canConfirm = computed(() => !loading.value && specGroups.value.length > 0)

async function confirm() {
  if (!props.product || !canConfirm.value) return
  await cartStore.add(props.product.id, { ...selection }, 1)
  emit('added', props.product.name, specText.value)
  emit('close')
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`选择规格 · ${product?.name ?? ''}`"
    width="440px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => !v && emit('close')"
  >
    <div v-if="loading" class="spec-picker__loading">规格加载中…</div>
    <div v-else class="spec-picker">
      <div v-for="g in specGroups" :key="g.group" class="spec-group" role="radiogroup" :aria-label="g.label">
        <p class="spec-group__label">{{ g.label }}</p>
        <div class="spec-group__options">
          <button
            v-for="opt in g.options"
            :key="opt.optionName"
            type="button"
            class="spec-opt"
            role="radio"
            :aria-checked="selection[g.group] === opt.optionName"
            :class="{ 'is-selected': selection[g.group] === opt.optionName }"
            @click="selection[g.group] = opt.optionName"
          >
            {{ opt.optionName }}
            <em v-if="opt.priceDelta" class="spec-opt__delta">+{{ opt.priceDelta }} 元</em>
          </button>
        </div>
      </div>

      <!-- 差价试算 -->
      <div class="spec-trial">
        <span class="spec-trial__label">单价试算</span>
        <span class="spec-trial__price">
          <i>¥</i>{{ trialPrice }}
        </span>
        <span class="spec-trial__delta" :class="{ 'is-zero': deltaTotal === 0 }">
          {{ deltaTotal === 0 ? '无规格加价' : `含规格加价 ${formatSpecDelta(deltaTotal)}` }}
        </span>
      </div>
    </div>

    <template #footer>
      <div class="spec-picker__footer">
        <el-button @click="emit('close')">取消</el-button>
        <el-button type="primary" :disabled="!canConfirm" @click="confirm">加入购物车</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.spec-picker {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-4);
}

.spec-picker__loading {
  padding: var(--ac-space-6) 0;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.spec-group__label {
  margin: 0 0 var(--ac-space-2);
  font-size: 13px;
  font-weight: 600;
  color: var(--ac-text);
}

.spec-group__options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ac-space-2);
}

.spec-opt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-family: var(--ac-font-body);
  font-size: 13px;
  color: var(--ac-text);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-pill);
  cursor: pointer;
  transition:
    background-color var(--ac-dur-fast) var(--ac-ease-enter),
    color var(--ac-dur-fast) var(--ac-ease-enter),
    border-color var(--ac-dur-fast) var(--ac-ease-enter);

  em {
    font-style: normal;
    font-size: 11px;
    color: var(--ac-cta);
  }

  &.is-selected {
    color: var(--ac-primary-deep);
    font-weight: 600;
    background: var(--ac-tide);
    border-color: var(--ac-primary);
  }
}

.spec-trial {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-2);
  padding: var(--ac-space-3) var(--ac-space-4);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-btn);
}

.spec-trial__label {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.spec-trial__price {
  font-family: var(--ac-font-display);
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ac-primary-deep);

  i {
    margin-right: 2px;
    font-size: 13px;
    font-style: normal;
    color: var(--ac-text-dim);
  }
}

.spec-trial__delta {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--ac-cta);

  &.is-zero {
    color: var(--ac-text-dim);
    font-weight: 400;
  }
}

.spec-picker__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--ac-space-2);
}
</style>
