<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { authContent } from "@/config/authContent";
import { registerRequest } from "@/services/authService";
import StarfieldBackground from "@/components/StarfieldBackground.vue";

const content = authContent.register;

const form = reactive({
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
});

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const passwordMatched = computed(() => form.password === form.confirmPassword);

function validateRegisterForm() {
  if (!form.username.trim()) {
    errorMessage.value = "请输入用户名。";
    return false;
  }

  if (!form.email.trim()) {
    errorMessage.value = "请输入邮箱。";
    return false;
  }

  if (!form.password.trim()) {
    errorMessage.value = "请输入密码。";
    return false;
  }

  if (!passwordMatched.value) {
    errorMessage.value = "两次输入密码不一致。";
    return false;
  }

  errorMessage.value = "";
  return true;
}

async function handleRegisterSubmit() {
  successMessage.value = "";

  if (!validateRegisterForm()) {
    return;
  }

  loading.value = true;
  try {
    const result = await registerRequest({
      username: form.username,
      email: form.email,
      password: form.password,
      confirmPassword: form.confirmPassword,
    });

    if (!result.ok) {
      errorMessage.value = result.message;
      return;
    }

    successMessage.value = result.message;

    // TODO: 注册成功后可以执行以下逻辑
    // 1) 自动登录并跳转主页
    // 2) 跳转邮箱验证页
    // 3) 进入新手引导页
  } catch (error) {
    errorMessage.value = "注册失败，请稍后重试。";
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
        <h2>注册</h2>

        <form @submit.prevent="handleRegisterSubmit" class="auth-form">
          <label>
            用户名
            <input v-model="form.username" type="text" placeholder="你的昵称" />
          </label>

          <label>
            邮箱
            <input v-model="form.email" type="email" placeholder="name@example.com" />
          </label>

          <label>
            密码
            <input v-model="form.password" type="password" placeholder="至少 8 位" />
          </label>

          <label>
            确认密码
            <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
          </label>

          <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="message success">{{ successMessage }}</p>

          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? "创建中..." : content.primaryActionText }}
          </button>
        </form>

        <RouterLink to="/login" class="switch-link">{{ content.secondaryActionText }}</RouterLink>
      </article>
    </section>
  </main>
</template>
