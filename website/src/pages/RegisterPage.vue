<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authContent } from '@/config/authContent'
import { isApiSuccess } from '@/api/client'
import { uploadFile } from '@/api/auth'
import { isLikelyImageFile } from '@/lib/resizeAvatar'
import { useAuthStore } from '@/stores/auth'

const content = authContent.register
const auth = useAuthStore()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const avatarPreviewUrl = ref<string | null>(null)
const avatarFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const passwordMatched = computed(() => form.password === form.confirmPassword)

function revokeAvatarPreview() {
  if (avatarPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
}

function clearAvatar() {
  revokeAvatarPreview()
  avatarPreviewUrl.value = null
  avatarFile.value = null
}

function openAvatarPicker() {
  fileInputRef.value?.click()
}

async function onAvatarSelected(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!isLikelyImageFile(file)) {
    errorMessage.value = '请选择 JPG、PNG、WebP 等常见图片（不支持 SVG）。'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '图片请小于 5MB。'
    return
  }

  try {
    revokeAvatarPreview()
    avatarPreviewUrl.value = URL.createObjectURL(file)
    avatarFile.value = file
    if (errorMessage.value) errorMessage.value = ''
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '头像处理失败，请换一张图片重试。'
    clearAvatar()
  }
}

watch(
  () => [form.username, form.password, form.confirmPassword, avatarFile.value],
  () => {
    if (errorMessage.value) errorMessage.value = ''
  }
)

onBeforeUnmount(() => {
  revokeAvatarPreview()
})

function validateRegisterForm() {
  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名。'
    return false
  }

  if (!form.password.trim()) {
    errorMessage.value = '请输入密码。'
    return false
  }

  if (!passwordMatched.value) {
    errorMessage.value = '两次输入密码不一致。'
    return false
  }

  errorMessage.value = ''
  return true
}

async function handleRegisterSubmit() {
  successMessage.value = ''

  if (!validateRegisterForm()) {
    return
  }

  loading.value = true
  try {
    let userImageUrl: string | undefined
    if (avatarFile.value) {
      const up = await uploadFile(avatarFile.value)
      if (!isApiSuccess(up.code) || !up.data) {
        errorMessage.value = up.msg || '头像上传失败'
        return
      }
      userImageUrl = up.data
    }

    const r = await auth.register(form.username.trim(), form.password, userImageUrl)
    if (!isApiSuccess(r.code)) {
      errorMessage.value = r.msg || '注册失败'
      return
    }
    successMessage.value = '注册成功，请前往登录'
    await router.push({ name: 'login' })
  } catch (e) {
    errorMessage.value = '注册失败，请稍后重试。'
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
        <h2>注册</h2>

        <form class="auth-form" @submit.prevent="handleRegisterSubmit">
          <div class="auth-avatar-block">
            <p class="auth-avatar-label">头像（可选）</p>
            <div class="auth-avatar-row">
              <button type="button" class="auth-avatar-preview" @click="openAvatarPicker">
                <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="" class="auth-avatar-img" />
                <span v-else class="auth-avatar-placeholder" aria-hidden="true">
                  <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.25">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c1.5-4 4.5-6 8-6s6.5 2 8 6" />
                  </svg>
                </span>
              </button>
              <div class="auth-avatar-actions">
                <button type="button" class="auth-avatar-btn" @click="openAvatarPicker">上传图片</button>
                <button v-if="avatarFile" type="button" class="auth-avatar-btn ghost" @click="clearAvatar">移除</button>
                <p class="auth-avatar-hint">支持 JPG / PNG / WebP，最大 5MB，将在注册前上传。</p>
              </div>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              class="auth-avatar-input"
              accept="image/jpeg,image/png,image/webp,image/gif"
              @change="onAvatarSelected"
            />
          </div>

          <label>
            用户名
            <input v-model="form.username" type="text" autocomplete="username" placeholder="请输入用户名" />
          </label>

          <label>
            密码
            <input v-model="form.password" type="password" autocomplete="new-password" placeholder="至少 8 位" />
          </label>

          <label>
            确认密码
            <input v-model="form.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码" />
          </label>

          <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="message success">{{ successMessage }}</p>

          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? '创建中...' : content.primaryActionText }}
          </button>
        </form>

        <RouterLink to="/login" class="switch-link">{{ content.secondaryActionText }}</RouterLink>
        <div class="auth-footer-links">
          <RouterLink to="/">返回首页</RouterLink>
        </div>
      </article>
    </section>
  </main>
</template>
