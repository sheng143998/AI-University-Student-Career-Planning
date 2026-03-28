<template>
  <div class="max-w-4xl mx-auto py-10 px-4 sm:px-6 lg:px-8">
    <header class="mb-10">
      <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">个人中心</h1>
      <p class="text-on-surface-variant">管理您的个人资料、安全设置和偏好。</p>
    </header>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
      <!-- 侧边导航 -->
      <aside class="space-y-2">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          @click="activeTab = tab.id"
          :class="[
            'w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium',
            activeTab === tab.id 
              ? 'bg-primary text-on-primary shadow-md' 
              : 'hover:bg-surface-container-low text-on-surface-variant'
          ]"
        >
          <span class="material-symbols-outlined">{{ tab.icon }}</span>
          {{ tab.label }}
        </button>
      </aside>

      <!-- 主要内容区 -->
      <main class="md:col-span-2 space-y-6">
        <!-- 个人资料编辑 -->
        <section v-if="activeTab === 'profile'" class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant animate-fade-in">
          <h2 class="text-xl font-bold mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">person</span>
            个人资料
          </h2>
          
          <div class="space-y-6">
            <!-- 头像上传 -->
            <div class="flex flex-col items-center sm:flex-row sm:items-start gap-6 pb-6 border-b border-outline-variant">
              <div class="relative group cursor-pointer" @click="triggerFileUpload">
                <img 
                  :src="form.userImage || '/default-avatar.png'" 
                  class="w-24 h-24 rounded-full object-cover border-4 border-surface-container-high shadow-inner"
                  alt="Avatar"
                />
                <div class="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <span class="material-symbols-outlined text-white">upload</span>
                </div>
                <input 
                  type="file" 
                  ref="fileInput" 
                  class="hidden" 
                  accept="image/*" 
                  @change="handleAvatarUpload"
                />
              </div>
              <div class="flex-1 text-center sm:text-left">
                <h3 class="font-bold text-lg mb-1">修改头像</h3>
                <p class="text-sm text-on-surface-variant mb-3">支持 JPG, PNG 格式，大小不超过 2MB</p>
                <button 
                  @click="triggerFileUpload"
                  class="px-4 py-2 bg-secondary-container text-on-secondary-container rounded-lg text-sm font-medium hover:bg-secondary-container/80 transition-colors"
                  :disabled="uploading"
                >
                  {{ uploading ? '上传中...' : '选择图片' }}
                </button>
              </div>
            </div>

            <!-- 基本信息表单 -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface-variant">用户名（邮箱）</label>
                <input 
                  v-model="form.username" 
                  type="text" 
                  disabled 
                  class="w-full px-4 py-2 bg-surface-container-high rounded-lg text-on-surface-variant cursor-not-allowed border-none focus:ring-0"
                />
              </div>
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface">昵称</label>
                <input 
                  v-model="form.name" 
                  type="text" 
                  placeholder="输入您的昵称"
                  class="w-full px-4 py-2 bg-surface-container-low border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary transition-all"
                />
              </div>
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface">性别</label>
                <div class="flex gap-4">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" v-model="form.sex" :value="1" class="w-4 h-4 text-primary" />
                    <span>男</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" v-model="form.sex" :value="0" class="w-4 h-4 text-primary" />
                    <span>女</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="pt-4 flex justify-end">
              <button 
                @click="handleSaveProfile"
                class="px-6 py-2 bg-primary text-on-primary rounded-xl font-bold shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all disabled:opacity-50"
                :disabled="saving"
              >
                {{ saving ? '保存中...' : '保存更改' }}
              </button>
            </div>
          </div>
        </section>

        <!-- 安全设置 -->
        <section v-if="activeTab === 'security'" class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant animate-fade-in">
          <h2 class="text-xl font-bold mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">security</span>
            账户安全
          </h2>
          
          <div class="space-y-6">
            <div class="space-y-4">
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface">当前密码</label>
                <input 
                  v-model="passwordForm.oldPassword" 
                  type="password" 
                  class="w-full px-4 py-2 bg-surface-container-low border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                />
              </div>
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface">新密码</label>
                <input 
                  v-model="passwordForm.newPassword" 
                  type="password" 
                  class="w-full px-4 py-2 bg-surface-container-low border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                />
              </div>
              <div class="space-y-2">
                <label class="text-sm font-medium text-on-surface">确认新密码</label>
                <input 
                  v-model="passwordForm.confirmPassword" 
                  type="password" 
                  class="w-full px-4 py-2 bg-surface-container-low border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                />
              </div>
            </div>

            <div class="pt-4 flex justify-end">
              <button 
                @click="handleUpdatePassword"
                class="px-6 py-2 bg-primary text-on-primary rounded-xl font-bold shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all disabled:opacity-50"
                :disabled="saving"
              >
                修改密码
              </button>
            </div>
          </div>
        </section>

        <!-- 通用设置 -->
        <section v-if="activeTab === 'general'" class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant animate-fade-in">
          <h2 class="text-xl font-bold mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">settings</span>
            通用偏好
          </h2>
          <div class="space-y-6">
            <div v-for="(setting, i) in settings" :key="i" class="flex items-center justify-between p-3 hover:bg-surface-container-low rounded-xl transition-colors">
              <div class="flex items-center gap-4">
                <span class="material-symbols-outlined text-on-surface-variant">{{ setting.icon }}</span>
                <span class="font-medium">{{ setting.label }}</span>
              </div>
              <button 
                @click="setting.enabled = !setting.enabled"
                class="w-12 h-6 rounded-full relative transition-colors duration-200"
                :class="setting.enabled ? 'bg-primary' : 'bg-surface-container-highest'"
              >
                <div 
                  class="absolute top-1 w-4 h-4 bg-white rounded-full transition-transform duration-200 shadow-sm"
                  :class="setting.enabled ? 'translate-x-7' : 'translate-x-1'"
                ></div>
              </button>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { getUserInfo, updateUserInfo, uploadFile, type AuthUser } from '@/api/auth'
import { isApiSuccess } from '@/api/client'

const activeTab = ref('profile')
const uploading = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const tabs = [
  { id: 'profile', label: '个人资料', icon: 'person' },
  { id: 'security', label: '账户安全', icon: 'security' },
  { id: 'general', label: '通用偏好', icon: 'settings' }
]

const form = reactive({
  username: '',
  name: '',
  userImage: '',
  sex: 1
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const settings = ref([
  { label: '邮件通知', icon: 'mail', enabled: true },
  { label: '深色模式', icon: 'dark_mode', enabled: false },
  { label: '公开个人档案', icon: 'visibility', enabled: true }
])

// 获取用户信息
async function fetchUserInfo() {
  const res = await getUserInfo()
  if (isApiSuccess(res.code) && res.data) {
    Object.assign(form, {
      username: res.data.username,
      name: res.data.name,
      userImage: res.data.userImage,
      sex: res.data.sex ?? 1
    })
  }
}

// 触发文件选择
function triggerFileUpload() {
  fileInput.value?.click()
}

// 处理头像上传
async function handleAvatarUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 简单校验
  if (file.size > 2 * 1024 * 1024) {
    alert('图片大小不能超过 2MB')
    return
  }

  uploading.value = true
  try {
    const res = await uploadFile(file)
    if (isApiSuccess(res.code) && res.data) {
      form.userImage = res.data
      alert('头像上传成功')
    } else {
      alert(res.msg || '上传失败')
    }
  } catch (error) {
    alert('上传过程中出现错误')
  } finally {
    uploading.value = false
  }
}

// 保存个人资料
async function handleSaveProfile() {
  saving.value = true
  try {
    const res = await updateUserInfo({
      name: form.name,
      userImage: form.userImage,
      sex: form.sex
    })
    if (isApiSuccess(res.code)) {
      alert('保存成功')
    } else {
      alert(res.msg || '保存失败')
    }
  } catch (error) {
    alert('请求失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
async function handleUpdatePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    alert('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }

  saving.value = true
  try {
    const res = await updateUserInfo({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (isApiSuccess(res.code)) {
      alert('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      alert(res.msg || '密码修改失败')
    }
  } catch (error) {
    alert('请求失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
