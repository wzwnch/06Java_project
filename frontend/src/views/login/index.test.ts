import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Login from '@/views/login/index.vue'
import * as userApi from '@/api/user'

vi.mock('@/api/user')
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    currentRoute: { value: { query: {} } }
  }),
  useRoute: () => ({
    query: {}
  }),
  createRouter: vi.fn(() => ({
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
    beforeEach: vi.fn(),
    currentRoute: { value: { query: {} } }
  })),
  createWebHistory: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('登录页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载登录组件', () => {
      const wrapper = shallowMount(Login)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示登录标题', () => {
      const wrapper = shallowMount(Login)
      expect(wrapper.text()).toContain('登录')
    })
  })

  describe('表单数据测试', () => {
    it('表单初始值应为空', () => {
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      expect(vm.loginForm.username).toBe('')
      expect(vm.loginForm.password).toBe('')
    })

    it('表单应可设置值', async () => {
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      vm.loginForm.username = 'testuser'
      vm.loginForm.password = 'test123456'
      await wrapper.vm.$nextTick()
      expect(vm.loginForm.username).toBe('testuser')
    })
  })

  describe('表单验证规则测试', () => {
    it('应定义用户名验证规则', () => {
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      expect(vm.loginRules.username).toBeDefined()
    })

    it('应定义密码验证规则', () => {
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      expect(vm.loginRules.password).toBeDefined()
    })
  })

  describe('登录接口测试', () => {
    it('正常登录应成功', async () => {
      vi.mocked(userApi.login).mockResolvedValue({
        token: 'test-token',
        userInfo: {
          id: 1,
          username: 'testuser',
          phone: null,
          mail: null,
          realPhone: null,
          realMail: null,
          createTime: '2024-01-01',
          updateTime: '2024-01-01'
        }
      })
      
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      
      vm.loginForm.username = 'testuser'
      vm.loginForm.password = 'test123456'
      
      await vm.handleLogin()
      
      expect(userApi.login).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'test123456'
      })
    })

    it('登录失败应显示错误', async () => {
      vi.mocked(userApi.login).mockRejectedValue(new Error('用户名或密码错误'))
      
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      
      vm.loginForm.username = 'wronguser'
      vm.loginForm.password = 'wrongpassword'
      
      await vm.handleLogin()
      
      expect(userApi.login).toHaveBeenCalled()
    })
  })

  describe('加载状态测试', () => {
    it('初始加载状态应为 false', () => {
      const wrapper = shallowMount(Login)
      const vm = wrapper.vm as any
      expect(vm.loading).toBe(false)
    })
  })
})
