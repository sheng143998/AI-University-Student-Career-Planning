import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/CareerDashboard.vue'),
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('../views/MarketExploration.vue'),
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/Chat.vue'),
    },
    {
      path: '/roadmap',
      name: 'roadmap',
      component: () => import('../views/Roadmap.vue'),
    },
    {
      path: '/goals',
      name: 'goals',
      component: () => import('../views/Goals.vue'),
    },
    {
      path: '/login',
      name: 'login',
      meta: { layout: 'auth', public: true },
      component: () => import('../pages/LoginPage.vue'),
    },
    {
      path: '/register',
      name: 'register',
      meta: { layout: 'auth', public: true },
      component: () => import('../pages/RegisterPage.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/Settings.vue'),
    },
    {
      path: '/resume',
      name: 'resume',
      component: () => import('../views/ResumeAnalysis.vue'),
    },
    {
      path: '/reports',
      name: 'reports',
      component: () => import('../views/CareerReport.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath !== '/' ? to.fullPath : undefined },
    }
  }
  return true
})

export default router
