<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 我是商家/招商加盟页（/merchant，M1-6 批次 4，静态内容）：
 * 普通用户显示招商加盟与商家合作说明；ADMIN 额外显示知识库管理后台入口。
 */
const store = useUserStore()
const isAdmin = computed(() => store.userInfo?.role === 'ADMIN')

const FRANCHISE_POINTS = [
  { title: '统一品牌视觉', detail: '门店设计、物料与设备由总部统一输出，开业周期约 45 天' },
  { title: '原料供应链直供', detail: '咖啡豆与鲜奶由总部仓每周冷链配送，品控标准与直营店一致' },
  { title: '数字化运营系统', detail: '点单小程序、门店作业与会员体系全套接入，总部提供培训' },
]

function onCooperation() {
  ElMessage.info('商家合作洽谈通道在后续版本接入，可先拨打客服热线')
}
</script>

<template>
  <div class="merchant">
    <header class="merchant__head">
      <h2 class="merchant__title">我是商家</h2>
      <p class="merchant__meta">招商加盟 · 商家合作</p>
    </header>

    <!-- ADMIN 专属：知识库管理后台入口 -->
    <RouterLink
      v-if="isAdmin"
      to="/admin/kb"
      class="merchant__kb"
      aria-label="进入知识库管理后台"
    >
      <span class="merchant__kb-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
        </svg>
      </span>
      <span class="merchant__kb-info">
        <span class="merchant__kb-name">知识库管理后台</span>
        <span class="merchant__kb-desc">管理客服知识库文档与向量索引</span>
      </span>
      <span class="merchant__kb-badge">ADMIN</span>
      <svg class="merchant__kb-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M9 18l6-6-6-6" />
      </svg>
    </RouterLink>

    <section class="merchant__card">
      <h3 class="merchant__section-title">招商加盟</h3>
      <p class="merchant__intro">
        Atlantic Coffee 正在寻找同样热爱海洋与咖啡的伙伴。我们提供从选址评估到开业运营的全流程支持，
        加盟门店与直营门店共享同一套会员与点单体系。
      </p>
      <div v-for="point in FRANCHISE_POINTS" :key="point.title" class="merchant__point">
        <span class="merchant__point-title">{{ point.title }}</span>
        <span class="merchant__point-detail">{{ point.detail }}</span>
      </div>
    </section>

    <section class="merchant__card">
      <h3 class="merchant__section-title">商家合作说明</h3>
      <p class="merchant__intro">
        门店供货、企业团购、联名活动等合作意向，请通过客服热线或「联系客服」提交需求，
        合作团队将在 3 个工作日内与您联系。合作条款以正式签署的协议为准。
      </p>
      <el-button class="merchant__contact" @click="onCooperation">提交合作意向</el-button>
    </section>
  </div>
</template>

<style scoped lang="scss">
.merchant {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: var(--ac-space-8);
}

.merchant__head {
  display: flex;
  align-items: baseline;
  gap: var(--ac-space-3);
  margin-bottom: var(--ac-space-5);
}

.merchant__title {
  font-size: 22px;
}

.merchant__meta {
  margin: 0;
  font-size: 13px;
  color: var(--ac-text-dim);
}

.merchant__card {
  padding: var(--ac-space-5);
  margin-bottom: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
}

.merchant__section-title {
  margin-bottom: var(--ac-space-3);
  font-size: 15px;
  font-weight: 600;
}

.merchant__intro {
  margin: 0 0 var(--ac-space-3);
  font-size: 13px;
  line-height: 1.7;
  color: var(--ac-text);
}

.merchant__point {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--ac-space-2) 0;

  & + & {
    border-top: 1px dashed var(--ac-border);
  }
}

.merchant__point-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.merchant__point-detail {
  font-size: 12px;
  line-height: 1.6;
  color: var(--ac-text-dim);
}

.merchant__contact {
  width: 100%;
}

/* ADMIN 专属入口 */
.merchant__kb {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  padding: var(--ac-space-4);
  margin-bottom: var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
  transition:
    transform var(--ac-dur-fast) var(--ac-ease-enter),
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ac-shadow-md);
  }
}

.merchant__kb-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 40px;
  height: 40px;
  color: var(--ac-primary);
  background: var(--ac-tide);
  border-radius: 12px;

  svg {
    width: 20px;
    height: 20px;
  }
}

.merchant__kb-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.merchant__kb-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-primary-deep);
}

.merchant__kb-desc {
  font-size: 12px;
  color: var(--ac-text-dim);
}

.merchant__kb-badge {
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--ac-primary);
  background: var(--ac-tide);
  border-radius: var(--ac-radius-pill);
}

.merchant__kb-chevron {
  width: 14px;
  height: 14px;
  color: var(--ac-text-dim);
}
</style>
