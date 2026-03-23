<script setup lang="ts">
import { reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { authContent } from "@/config/authContent";
import { loginRequest } from "@/services/authService";
import StarfieldBackground from "@/components/StarfieldBackground.vue";

const content = authContent.login;

const form = reactive({
  account: "",
  password: "",
});

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

function validateLoginForm() {
  if (!form.account.trim()) {
    errorMessage.value = "请输入账号（邮箱或用户名）。";
    return false;
  }

  if (!form.password.trim()) {
    errorMessage.value = "请输入密码。";
    return false;
  }

  errorMessage.value = "";
  return true;
}

async function handleLoginSubmit() {
  successMessage.value = "";

  if (!validateLoginForm()) {
    return;
  }

  loading.value = true;
  try {
    const result = await loginRequest({
      account: form.account,
      password: form.password,
    });

    if (!result.ok) {
      errorMessage.value = result.message;
      return;
    }

    successMessage.value = result.message;

    // TODO: 登录成功后可以执行以下逻辑
    // 1) 持久化 token (localStorage/cookie/pinia)
    // 2) 拉取用户信息
    // 3) router.push('/dashboard')
  } catch (error) {
    errorMessage.value = "登录失败，请稍后重试。";
    console.error(error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <StarfieldBackground />

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

        <form @submit.prevent="handleLoginSubmit" class="auth-form">
          <label>
            账号
            <input v-model="form.account" type="text" placeholder="邮箱或用户名" />
          </label>

          <label>
            密码
            <input v-model="form.password" type="password" placeholder="请输入密码" />
          </label>

          <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="message success">{{ successMessage }}</p>

          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? "连接中..." : content.primaryActionText }}
          </button>
        </form>

        <RouterLink to="/register" class="switch-link">{{
          content.secondaryActionText
        }}</RouterLink>
      </article>
    </section>
  </main>
</template>
