<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authContent } from '@/config/authContent'
import { useAuthStore } from '@/stores/auth'
import { isApiSuccess } from '@/api/client'

const content = authContent.login
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  account: '',
  password: '',
})

const loading = ref(false)
const errorMessage = ref('')

function validateLoginForm() {
  if (!form.account.trim()) {
    errorMessage.value = '请输入账号（邮箱或用户名）。'
    return false
  }

  if (!form.password.trim()) {
    errorMessage.value = '请输入密码。'
    return false
  }

  errorMessage.value = ''
  return true
}

async function handleLoginSubmit() {
  if (!validateLoginForm()) {
    return
  }

  loading.value = true
  try {
    const r = await auth.login(form.account.trim(), form.password)
    if (!isApiSuccess(r.code)) {
      errorMessage.value = r.msg || '登录失败'
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect || '/')
  } catch (e) {
    errorMessage.value = '登录失败，请稍后重试。'
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-shell">
      <article class="hero-card">
        <p class="tagline">{{ content.heroTagline }}</p>
        <h1>{{ content.heroTitle }}</h1>
        <p class="subtitle">{{ content.heroSubtitle }}</p>

        <ul class="tips-list">
          <li v-for="tip in content.tips" :key="tip">{{ tip }}</li>
        </ul>
      </article>

      <article class="form-card">
        <h2>登录</h2>

        <form class="auth-form" @submit.prevent="handleLoginSubmit">
          <label>
            账号
            <input v-model="form.account" type="text" autocomplete="username" placeholder="邮箱或用户名" />
          </label>

          <label>
            密码
            <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
          </label>

          <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? '连接中...' : content.primaryActionText }}
          </button>
        </form>

        <RouterLink to="/register" class="switch-link">{{ content.secondaryActionText }}</RouterLink>
        <div class="auth-footer-links">
          <RouterLink to="/">返回首页</RouterLink>
        </div>
      </article>
    </section>
  </main>
</template>
