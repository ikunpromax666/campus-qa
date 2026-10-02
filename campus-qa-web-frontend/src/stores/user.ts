import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfoVO } from '../types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  // 刷新后从 localStorage 恢复（登录/个人中心时会重新拉 /user/info 覆盖）
  const savedUser = localStorage.getItem('user')
  const user = ref<UserInfoVO | null>(savedUser ? JSON.parse(savedUser) : null)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUser = (newUser: UserInfoVO) => {
    user.value = newUser
    // 同步持久化：路由守卫（/admin 校验 role）和刷新后导航栏恢复都依赖它
    localStorage.setItem('user', JSON.stringify(newUser))
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    setToken,
    setUser,
    logout
  }
})
