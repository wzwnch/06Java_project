import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginForm, RegisterForm, UserUpdateForm, LoginResponse } from '@/types/user'
import { getToken, setToken, clearToken, setUserInfo, getUserInfo } from '@/utils/auth'
import { login as loginApi, register as registerApi, logout as logoutApi, getUserInfo as getUserInfoApi, updateUserInfo as updateUserInfoApi, checkUsername as checkUsernameApi } from '@/api/user'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(getUserInfo<UserInfo>())
  const isLoggedIn = computed(() => !!token.value)

  async function login(form: LoginForm): Promise<boolean> {
    try {
      const data: LoginResponse = await loginApi(form)
      token.value = data.token
      userInfo.value = data.userInfo
      setToken(data.token)
      setUserInfo(data.userInfo)
      return true
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }

  async function register(form: RegisterForm): Promise<boolean> {
    try {
      await registerApi(form)
      return true
    } catch (error) {
      console.error('注册失败:', error)
      return false
    }
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // ignore
    } finally {
      token.value = null
      userInfo.value = null
      clearToken()
      router.push('/login')
    }
  }

  async function fetchUserInfo(): Promise<UserInfo | null> {
    try {
      const data = await getUserInfoApi()
      userInfo.value = data
      setUserInfo(data)
      return data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  async function updateUserInfo(form: UserUpdateForm): Promise<boolean> {
    try {
      const data = await updateUserInfoApi(form)
      userInfo.value = data
      setUserInfo(data)
      return true
    } catch (error) {
      console.error('更新用户信息失败:', error)
      return false
    }
  }

  async function checkUsername(username: string): Promise<boolean> {
    try {
      const data = await checkUsernameApi(username)
      return data.exists
    } catch (error) {
      console.error('检查用户名失败:', error)
      return false
    }
  }

  function setTokenValue(newToken: string): void {
    token.value = newToken
    setToken(newToken)
  }

  function setUserInfoValue(info: UserInfo): void {
    userInfo.value = info
    setUserInfo(info)
  }

  function clearUserData(): void {
    token.value = null
    userInfo.value = null
    clearToken()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    logout,
    fetchUserInfo,
    updateUserInfo,
    checkUsername,
    setTokenValue,
    setUserInfoValue,
    clearUserData
  }
})
