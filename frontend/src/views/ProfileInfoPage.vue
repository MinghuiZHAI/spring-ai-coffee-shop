<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api/user'

/**
 * 个人资料页（/profile/info，M1-6 批次 4）：
 * 头像/用户名/关联手机只读；性别、幸运日可编辑（PUT /api/user/profile）；
 * 收货地址/账号管理/免密支付/通用设置/协议与说明为静态入口（后续接入）；
 * 底部退出登录：清 token 跳 /login。
 */
const router = useRouter()
const store = useUserStore()

const GENDER_LABEL: Record<string, string> = { MALE: '男', FEMALE: '女', UNKNOWN: '保密' }
const genderLabel = computed(() => GENDER_LABEL[store.userInfo?.gender ?? 'UNKNOWN'] ?? '保密')

const genderDialogVisible = ref(false)
const genderDraft = ref<'UNKNOWN' | 'MALE' | 'FEMALE'>('UNKNOWN')

const luckyDayDialogVisible = ref(false)
const luckyDayDraft = ref('')

function onEditGender() {
  genderDraft.value = (store.userInfo?.gender ?? 'UNKNOWN') as 'UNKNOWN' | 'MALE' | 'FEMALE'
  genderDialogVisible.value = true
}

async function onGenderSave() {
  try {
    await updateProfile({ gender: genderDraft.value })
    await store.fetchProfile()
    genderDialogVisible.value = false
    ElMessage.success('性别已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

function onEditLuckyDay() {
  luckyDayDraft.value = store.userInfo?.luckyDay ?? ''
  luckyDayDialogVisible.value = true
}

async function onLuckyDaySave() {
  if (luckyDayDraft.value.length > 20) {
    ElMessage.error('幸运日最长 20 字')
    return
  }
  try {
    await updateProfile({ luckyDay: luckyDayDraft.value })
    await store.fetchProfile()
    luckyDayDialogVisible.value = false
    ElMessage.success('幸运日已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

function onStaticEntry(label: string) {
  ElMessage.info(`「${label}」在后续版本接入`)
}

async function onLogout() {
  await store.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="info">
    <header class="info__head">
      <RouterLink to="/profile" class="info__back" aria-label="返回个人中心">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回个人中心
      </RouterLink>
    </header>

    <section class="info__card">
      <button type="button" class="info__row" @click="onStaticEntry('头像上传')">
        <span class="info__row-label">头像</span>
        <span class="info__row-avatar" aria-hidden="true">
          <img v-if="store.userInfo?.avatarUrl" :src="store.userInfo.avatarUrl" alt="头像" />
          <svg v-else width="18" height="18" viewBox="0 0 32 32" fill="none">
            <path d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0" stroke="#fff" stroke-width="2.4" stroke-linecap="round" />
            <path d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0" stroke="#7fd1f5" stroke-width="2" stroke-linecap="round" />
          </svg>
        </span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>

      <div class="info__row">
        <span class="info__row-label">用户名</span>
        <span class="info__row-value">{{ store.userInfo?.nickname ?? '--' }}</span>
      </div>

      <button type="button" class="info__row" @click="onEditGender">
        <span class="info__row-label">性别</span>
        <span class="info__row-value">{{ genderLabel }}</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>

      <button type="button" class="info__row" @click="onEditLuckyDay">
        <span class="info__row-label">我的幸运日</span>
        <span class="info__row-value">{{ store.userInfo?.luckyDay || '未设置' }}</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>

      <div class="info__row">
        <span class="info__row-label">关联手机</span>
        <span class="info__row-value">{{ store.userInfo?.phone ?? '--' }}</span>
      </div>

      <button type="button" class="info__row" @click="onStaticEntry('收货地址')">
        <span class="info__row-label">收货地址</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="info__row" @click="onStaticEntry('账号管理')">
        <span class="info__row-label">账号管理</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="info__row" @click="onStaticEntry('免密支付')">
        <span class="info__row-label">免密支付</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="info__row" @click="onStaticEntry('通用设置')">
        <span class="info__row-label">通用设置</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
      <button type="button" class="info__row" @click="onStaticEntry('协议与说明')">
        <span class="info__row-label">协议与说明</span>
        <svg class="info__row-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </section>

    <button type="button" class="info__logout" @click="onLogout">退出登录</button>

    <!-- 性别编辑 -->
    <el-dialog v-model="genderDialogVisible" title="修改性别" width="320" align-center>
      <el-radio-group v-model="genderDraft" class="info__gender-group">
        <el-radio value="MALE">男</el-radio>
        <el-radio value="FEMALE">女</el-radio>
        <el-radio value="UNKNOWN">保密</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="genderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onGenderSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 幸运日编辑 -->
    <el-dialog v-model="luckyDayDialogVisible" title="修改我的幸运日" width="360" align-center>
      <el-input
        v-model="luckyDayDraft"
        placeholder="如：每月 8 日 / 周五"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="luckyDayDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onLuckyDaySave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.info {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: var(--ac-space-8);
}

.info__head {
  margin-bottom: var(--ac-space-4);
}

.info__back {
  display: inline-flex;
  align-items: center;
  gap: var(--ac-space-1);
  font-size: 14px;
  color: var(--ac-text-dim);
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);
  cursor: pointer;

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover {
    color: var(--ac-primary);
  }
}

.info__card {
  padding: 0 var(--ac-space-4);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  box-shadow: var(--ac-shadow-sm);
  overflow: hidden;
}

.info__row {
  display: flex;
  align-items: center;
  gap: var(--ac-space-3);
  width: 100%;
  padding: var(--ac-space-4) 0;
  font-family: var(--ac-font-body);
  font-size: 14px;
  color: var(--ac-text);
  background: none;
  border: none;
  border-bottom: 1px solid var(--ac-border);
  cursor: pointer;
  transition: color var(--ac-dur-fast) var(--ac-ease-enter);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    color: var(--ac-primary);
  }
}

div.info__row {
  cursor: default;
}

.info__row-label {
  flex: 1;
  text-align: left;
}

.info__row-value {
  color: var(--ac-text);
}

.info__row-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  overflow: hidden;
  background: var(--ac-primary);
  border-radius: var(--ac-radius-pill);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.info__row-chevron {
  width: 14px;
  height: 14px;
  color: var(--ac-text-dim);
}

.info__logout {
  display: block;
  width: 100%;
  margin-top: var(--ac-space-6);
  padding: var(--ac-space-4) 0;
  font-family: var(--ac-font-body);
  font-size: 15px;
  font-weight: 600;
  color: var(--ac-danger);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-card);
  cursor: pointer;
  transition:
    box-shadow var(--ac-dur-fast) var(--ac-ease-enter),
    transform var(--ac-dur-fast) var(--ac-ease-enter);

  &:hover {
    box-shadow: var(--ac-shadow-md);
  }

  &:active {
    transform: scale(0.99);
  }
}

.info__gender-group {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-2);
}
</style>
