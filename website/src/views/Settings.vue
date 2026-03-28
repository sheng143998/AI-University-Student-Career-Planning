<template>
  <div class="space-y-10">
    <header>
      <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">设置与帮助</h1>
      <p class="text-on-surface-variant">管理您的账户偏好，获取所需支持。</p>
    </header>

    <div class="max-w-2xl space-y-8">
      <section class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
        <h2 class="text-xl font-bold mb-6">通用设置</h2>
        <div class="space-y-6">
          <div v-for="(setting, i) in settings" :key="i" class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <span class="material-symbols-outlined text-on-surface-variant">{{ setting.icon }}</span>
              <span class="font-medium">{{ setting.label }}</span>
            </div>
            <button class="w-12 h-6 bg-surface-container-highest rounded-full relative">
              <div class="absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition-all" :class="{'translate-x-6 bg-primary': setting.enabled}"></div>
            </button>
          </div>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
        <h2 class="text-xl font-bold mb-6">帮助中心</h2>
        <div class="space-y-4">
          <a v-for="(item, i) in helpItems" :key="i" href="#" class="flex items-center justify-between p-4 hover:bg-surface-container-low rounded-lg transition-colors">
            <span class="font-medium">{{ item }}</span>
            <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
          </a>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
const settings = [
  { label: '邮件通知', icon: 'mail', enabled: true },
  { label: '深色模式', icon: 'dark_mode', enabled: false },
  { label: '公开个人档案', icon: 'visibility', enabled: true }
]

const helpItems = [
  '常见问题解答',
  '联系技术支持',
  '隐私政策',
  '服务条款'
]
</script>
