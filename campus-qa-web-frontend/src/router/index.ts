import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/question/:id',
      name: 'QuestionDetail',
      component: () => import('../views/QuestionDetail.vue')
    },
    {
      path: '/publish',
      name: 'QuestionPublish',
      component: () => import('../views/QuestionPublish.vue')
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Profile.vue')
    },
    {
      path: '/my',
      name: 'My',
      component: () => import('../views/My.vue')
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../views/Admin.vue'),
      // 前端守卫只是第一道门（体验层），真正的防线是后端 /admin/** 类级 @RequireAdmin
      meta: { requireAdmin: true }
    }
  ]
})

// 路由守卫：未登录访问受保护页面时跳登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  // 公开页面：登录、注册（其余都需要登录）
  const publicPages = ['Login', 'Register']
  if (!publicPages.includes(to.name as string) && !token) {
    return { name: 'Login' }
  }
  // 已登录再去登录/注册页，直接回首页
  if (publicPages.includes(to.name as string) && token) {
    return { name: 'Home' }
  }
  // 管理端页面：非管理员（role !== 1）打回首页
  if (to.meta.requireAdmin) {
    const raw = localStorage.getItem('user')
    const role = raw ? (JSON.parse(raw)?.role as number | undefined) : undefined
    if (role !== 1) {
      return { name: 'Home' }
    }
  }
})

export default router
