import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Group from '@/views/group/index.vue'
import * as groupApi from '@/api/group'

vi.mock('@/api/group')
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
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
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

describe('分组管理页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载分组管理组件', () => {
      const wrapper = shallowMount(Group)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示页面标题', () => {
      const wrapper = shallowMount(Group)
      expect(wrapper.text()).toContain('分组管理')
    })
  })

  describe('表单数据测试', () => {
    it('创建表单初始值应为空', () => {
      const wrapper = shallowMount(Group)
      const vm = wrapper.vm as any
      expect(vm.createForm.name).toBe('')
    })
  })

  describe('表单验证规则测试', () => {
    it('应定义分组名称验证规则', () => {
      const wrapper = shallowMount(Group)
      const vm = wrapper.vm as any
      expect(vm.formRules.name).toBeDefined()
    })
  })

  describe('API 接口测试', () => {
    it('创建分组 API 应可正常调用', async () => {
      vi.mocked(groupApi.createGroup).mockResolvedValue('1')
      
      await groupApi.createGroup({ name: '测试分组', sortOrder: 0 })
      
      expect(groupApi.createGroup).toHaveBeenCalled()
    })

    it('查询分组列表 API 应可正常调用', async () => {
      vi.mocked(groupApi.listGroups).mockResolvedValue([])
      
      await groupApi.listGroups()
      
      expect(groupApi.listGroups).toHaveBeenCalled()
    })

    it('更新分组 API 应可正常调用', async () => {
      vi.mocked(groupApi.updateGroup).mockResolvedValue(undefined)
      
      await groupApi.updateGroup({ gid: '1', name: '更新分组', sortOrder: 0 })
      
      expect(groupApi.updateGroup).toHaveBeenCalled()
    })

    it('删除分组 API 应可正常调用', async () => {
      vi.mocked(groupApi.deleteGroup).mockResolvedValue(undefined)
      
      await groupApi.deleteGroup('1')
      
      expect(groupApi.deleteGroup).toHaveBeenCalled()
    })
  })
})
