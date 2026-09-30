<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ApiError } from '@/api/client'

/**
 * 登录页（M1-6 批次 3）：手机号 + 密码登录，成功后按 role 跳转
 * （USER/AGENT → / 菜单；ADMIN → /admin/kb 知识库后台）。
 */
const router = useRouter()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 位之间', trigger: 'blur' },
  ],
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const res = await store.login(form.phone, form.password)
    ElMessage.success(`欢迎回来，${res.userInfo.nickname}`)
    router.push(res.userInfo.role === 'ADMIN' ? '/admin/kb' : '/')
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="login__card">
      <div class="login__brand">
        <svg width="44" height="44" viewBox="0 0 32 32" aria-hidden="true">
          <circle cx="16" cy="16" r="16" fill="var(--ac-primary)" />
          <path d="M7 13.5c2.6-2.4 5-2.4 7.5 0s4.9 2.4 7.5 0" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" opacity=".95" />
          <path d="M8.5 19c2.2-2 4.2-2 6.5 0s4.3 2 6.5 0" fill="none" stroke="#7fd1f5" stroke-width="2" stroke-linecap="round" opacity=".9" />
        </svg>
        <h1 class="login__title">Atlantic Coffee</h1>
        <p class="login__sub">大西洋咖啡 · 海洋与探索的一杯</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button
          class="login__submit"
          type="primary"
          size="large"
          :loading="loading"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <p class="login__demo">演示账号：13800000001 · 密码 123456（管理员 13800000000）</p>
    </section>
  </div>
</template>

<style scoped lang="scss">
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--ac-space-4);
  background: var(--ac-bg);
}

.login__card {
  width: 100%;
  max-width: 380px;
  padding: var(--ac-space-8) var(--ac-space-6);
  background: var(--ac-card);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-overlay);
  box-shadow: var(--ac-shadow-md);
}

.login__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ac-space-1);
  margin-bottom: var(--ac-space-6);
}

.login__title {
  margin-top: var(--ac-space-2);
  font-size: 20px;
}

.login__sub {
  margin: 0;
  font-size: 12px;
  color: var(--ac-text-dim);
}

.login__submit {
  width: 100%;
  margin-top: var(--ac-space-2);
}

.login__demo {
  margin: var(--ac-space-4) 0 0;
  font-size: 11px;
  color: var(--ac-text-dim);
  text-align: center;
}
</style>
