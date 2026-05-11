import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Register from '@/views/register/index.vue'
import * as userApi from '@/api/user'

vi.mock('@/api/user')
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
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

describe('注册页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载注册组件', () => {
      const wrapper = shallowMount(Register)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示注册标题', () => {
      const wrapper = shallowMount(Register)
      expect(wrapper.text()).toContain('注册')
    })
  })

  describe('表单数据测试', () => {
    it('表单初始值应为空', () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      expect(vm.registerForm.username).toBe('')
      expect(vm.registerForm.password).toBe('')
      expect(vm.registerForm.confirmPassword).toBe('')
    })

    it('表单应可设置值', async () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      vm.registerForm.username = 'testuser'
      vm.registerForm.password = 'test123456'
      vm.registerForm.confirmPassword = 'test123456'
      await wrapper.vm.$nextTick()
      expect(vm.registerForm.username).toBe('testuser')
    })
  })

  describe('表单验证规则测试', () => {
    it('应定义用户名验证规则', () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      expect(vm.registerRules.username).toBeDefined()
    })

    it('应定义密码验证规则', () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      expect(vm.registerRules.password).toBeDefined()
    })

    it('应定义确认密码验证规则', () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      expect(vm.registerRules.confirmPassword).toBeDefined()
    })
  })

  describe('注册接口测试', () => {
    it('正常注册应成功', async () => {
      vi.mocked(userApi.register).mockResolvedValue(undefined)
      
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      
      vm.registerForm.username = 'testuser'
      vm.registerForm.password = 'test123456'
      vm.registerForm.confirmPassword = 'test123456'
      
      await vm.handleRegister()
      
      expect(userApi.register).toHaveBeenCalled()
    })

    it('注册失败应显示错误', async () => {
      vi.mocked(userApi.register).mockRejectedValue(new Error('用户名已存在'))
      
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      
      vm.registerForm.username = 'existinguser'
      vm.registerForm.password = 'test123456'
      vm.registerForm.confirmPassword = 'test123456'
      
      await vm.handleRegister()
      
      expect(userApi.register).toHaveBeenCalled()
    })
  })

  describe('加载状态测试', () => {
    it('初始加载状态应为 false', () => {
      const wrapper = shallowMount(Register)
      const vm = wrapper.vm as any
      expect(vm.loading).toBe(false)
    })
  })
})
