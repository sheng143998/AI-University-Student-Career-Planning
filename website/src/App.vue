<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

async function onLogout() {
  await auth.logout()
}
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <Transition v-if="route.meta.layout === 'auth'" name="warp-route" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
    <div v-else class="min-h-screen bg-surface">
      <!-- TopNavBar -->
      <nav class="fixed top-0 w-full z-50 flex justify-between items-center px-8 h-16 bg-slate-50 dark:bg-slate-950 border-b border-surface-container-highest">
        <div class="text-xl font-extrabold text-blue-700 dark:text-blue-400 tracking-tighter headline-font">职引AI</div>
        <div class="hidden md:flex items-center space-x-8">
          <router-link to="/" class="text-slate-500 dark:text-slate-400 hover:text-blue-600 font-bold tracking-tight" active-class="text-blue-700 dark:text-blue-400 border-b-2 border-blue-700 dark:border-blue-400 pb-1">首页</router-link>
          <router-link to="/market" class="text-slate-500 dark:text-slate-400 hover:text-blue-600 font-bold tracking-tight" active-class="text-blue-700 dark:text-blue-400 border-b-2 border-blue-700 dark:border-blue-400 pb-1">职场市场</router-link>
          <router-link to="/resume" class="text-slate-500 dark:text-slate-400 hover:text-blue-600 font-bold tracking-tight" active-class="text-blue-700 dark:text-blue-400 border-b-2 border-blue-700 dark:border-blue-400 pb-1">简历分析</router-link>
          <router-link to="/reports" class="text-slate-500 dark:text-slate-400 hover:text-blue-600 font-bold tracking-tight" active-class="text-blue-700 dark:text-blue-400 border-b-2 border-blue-700 dark:border-blue-400 pb-1">职业报告</router-link>
        </div>
        <div class="flex items-center space-x-3">
          <button type="button" class="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
            <span class="material-symbols-outlined text-slate-600 dark:text-slate-400">notifications</span>
          </button>
          <template v-if="auth.isLoggedIn">
            <span class="hidden sm:inline text-xs font-bold text-slate-600 dark:text-slate-300 max-w-[120px] truncate">{{ auth.user?.name || auth.user?.id }}</span>
            <button
              type="button"
              class="text-xs font-bold text-slate-500 dark:text-slate-400 hover:text-blue-600 px-2 py-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
              @click="onLogout"
            >
              退出
            </button>
          </template>
          <router-link
            v-else
            to="/login"
            class="text-sm font-bold text-blue-600 dark:text-blue-400 hover:underline px-2"
          >
            登录
          </router-link>
        </div>
      </nav>

      <!-- SideNavBar -->
      <aside class="fixed left-0 top-16 h-[calc(100vh-64px)] w-64 p-4 flex flex-col bg-slate-50 dark:bg-slate-950 hidden md:flex border-r border-surface-container-highest">
        <div class="mb-8 px-2">
          <div class="flex items-center space-x-3 mb-4">
            <div class="w-10 h-10 rounded-full bg-primary-fixed overflow-hidden">
              <img alt="Student Profile Picture" class="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBsHdE7z4BMZxenSjHKWkRZNsh5ZPUS6BfCOdRfO0KL5SQZXKb0uJsqU7ojlh2jg671gvbtcbDc7T6zdk12p0nznfT5FTDPF52p7ELaa4kK10n6heErugUci18j1c1bGU-_givSLvosn19I9TRUfWq8r3nYGlemi4-2SOnVNzyxcaXdWoHHKU2dNCo7pY68X0w-pn1WJRmrEVaswKG1eQt4IPR04d0kY0zwBSmycD6ZWg-ub2ElJ3HGD9iYB7xBpZXMlCKr5C-RUtg"/>
            </div>
            <div>
              <h3 class="text-sm font-bold text-blue-700 dark:text-blue-400">职业仪表盘</h3>
              <p class="text-[10px] text-on-surface-variant uppercase tracking-wider">AI 引导路径</p>
            </div>
          </div>
        </div>
        <nav class="flex-1 space-y-1">
          <router-link to="/" class="flex items-center space-x-3 px-3 py-2.5 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-sm font-medium transition-transform duration-200 hover:translate-x-1" active-class="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
            <span class="material-symbols-outlined">person_outline</span>
            <span>档案概览</span>
          </router-link>
          <router-link to="/chat" class="flex items-center space-x-3 px-3 py-2.5 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-sm font-medium transition-transform duration-200 hover:translate-x-1" active-class="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
            <span class="material-symbols-outlined">psychology</span>
            <span>AI 分析</span>
          </router-link>
          <router-link to="/goals" class="flex items-center space-x-3 px-3 py-2.5 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-sm font-medium transition-transform duration-200 hover:translate-x-1" active-class="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
            <span class="material-symbols-outlined">flag</span>
            <span>我的目标</span>
          </router-link>
          <router-link to="/roadmap" class="flex items-center space-x-3 px-3 py-2.5 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-sm font-medium transition-transform duration-200 hover:translate-x-1" active-class="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
            <span class="material-symbols-outlined">map</span>
            <span>职业地图</span>
          </router-link>
        </nav>
        <div class="mt-auto space-y-4">
          <router-link to="/chat" class="block w-full text-center py-3 px-4 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl font-bold text-sm shadow-lg hover:opacity-90 transition-all">
            与 AI 导师对话
          </router-link>
          <div class="pt-4 border-t border-surface-container-highest space-y-1">
            <router-link to="/settings" class="flex items-center space-x-3 px-3 py-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-xs font-medium" active-class="bg-slate-100 dark:bg-slate-800">
              <span class="material-symbols-outlined text-lg">settings</span>
              <span>设置</span>
            </router-link>
            <router-link to="/settings" class="flex items-center space-x-3 px-3 py-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-xs font-medium">
              <span class="material-symbols-outlined text-lg">help_outline</span>
              <span>帮助</span>
            </router-link>
          </div>
        </div>
      </aside>

      <main
        :class="[
          'md:ml-64 pt-24',
          route.name === 'chat' ? 'pb-8 px-6 max-w-none overflow-x-hidden' : 'pb-24 px-8 max-w-7xl mx-auto'
        ]"
      >
        <component :is="Component" :key="route.fullPath" />
      </main>

      <!-- BottomNavBar (Mobile Only) -->
      <nav class="fixed bottom-0 w-full z-50 flex justify-around items-center p-4 bg-white/70 dark:bg-slate-900/70 backdrop-blur-xl md:hidden rounded-t-3xl shadow-[0_-10px_40px_rgba(0,0,0,0.04)]">
        <router-link to="/" class="flex flex-col items-center justify-center p-2 px-6 rounded-2xl transition-all" active-class="bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-100">
          <span class="material-symbols-outlined">smart_toy</span>
          <span class="text-[10px] uppercase tracking-widest mt-1">AI 指引</span>
        </router-link>
        <router-link to="/market" class="flex flex-col items-center justify-center text-slate-400 dark:text-slate-500 p-2 hover:text-blue-500 transition-all" active-class="text-blue-800 dark:text-blue-100">
          <span class="material-symbols-outlined">work</span>
          <span class="text-[10px] uppercase tracking-widest mt-1">市场</span>
        </router-link>
        <router-link to="/reports" class="flex flex-col items-center justify-center text-slate-400 dark:text-slate-500 p-2 hover:text-blue-500 transition-all" active-class="text-blue-800 dark:text-blue-100">
          <span class="material-symbols-outlined">assessment</span>
          <span class="text-[10px] uppercase tracking-widest mt-1">报告</span>
        </router-link>
        <router-link to="/resume" class="flex flex-col items-center justify-center text-slate-400 dark:text-slate-500 p-2 hover:text-blue-500 transition-all" active-class="text-blue-800 dark:text-blue-100">
          <span class="material-symbols-outlined">account_box</span>
          <span class="text-[10px] uppercase tracking-widest mt-1">档案</span>
        </router-link>
      </nav>

      <!-- FAB -->
      <button class="fixed bottom-24 right-8 w-14 h-14 bg-primary text-white rounded-full flex items-center justify-center shadow-xl hover:scale-110 active:scale-90 transition-all md:bottom-8 z-40">
        <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">add</span>
      </button>
    </div>
  </RouterView>
</template>

<style>
.warp-route-enter-active,
.warp-route-leave-active {
  transition:
    opacity 520ms ease,
    transform 640ms cubic-bezier(0.2, 0.8, 0.2, 1),
    filter 520ms ease;
}

.warp-route-enter-from {
  opacity: 0;
  transform: scale(1.06) translate3d(0, 10px, 0);
  filter: blur(8px) saturate(1.25);
}

.warp-route-enter-to {
  opacity: 1;
  transform: scale(1) translate3d(0, 0, 0);
  filter: blur(0) saturate(1);
}

.warp-route-leave-from {
  opacity: 1;
  transform: scale(1) translate3d(0, 0, 0);
  filter: blur(0) saturate(1);
}

.warp-route-leave-to {
  opacity: 0;
  transform: scale(0.965) translate3d(0, -8px, 0);
  filter: blur(6px) saturate(1.15);
}

@media (prefers-reduced-motion: reduce) {
  .warp-route-enter-active,
  .warp-route-leave-active {
    transition: none;
  }
}
</style>
