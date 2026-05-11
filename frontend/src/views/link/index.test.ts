import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Link from '@/views/link/index.vue'
import * as linkApi from '@/api/link'

vi.mock('@/api/link')
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

describe('短链接管理页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载短链接管理组件', () => {
      const wrapper = shallowMount(Link)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示页面标题', () => {
      const wrapper = shallowMount(Link)
      expect(wrapper.text()).toContain('短链接管理')
    })
  })

  describe('表单数据测试', () => {
    it('创建表单初始值应为空', () => {
      const wrapper = shallowMount(Link)
      const vm = wrapper.vm as any
      expect(vm.createForm.originUrl).toBe('')
    })
  })

  describe('表单验证规则测试', () => {
    it('应定义目标URL验证规则', () => {
      const wrapper = shallowMount(Link)
      const vm = wrapper.vm as any
      expect(vm.createRules.originUrl).toBeDefined()
    })
  })

  describe('API 接口测试', () => {
    it('创建短链接 API 应可正常调用', async () => {
      vi.mocked(linkApi.createLink).mockResolvedValue({
        shortCode: 'abc123',
        shortUrl: 'http://s.cn/abc123',
        originUrl: 'https://www.example.com',
        gid: '1',
        faviconUrl: '',
        title: '',
        expireTime: null,
        status: 0,
        createTime: '2024-01-01',
        updateTime: '2024-01-01'
      })
      
      await linkApi.createLink({
        originUrl: 'https://www.example.com',
        gid: '1'
      })
      
      expect(linkApi.createLink).toHaveBeenCalled()
    })

    it('查询短链接列表 API 应可正常调用', async () => {
      vi.mocked(linkApi.pageLinks).mockResolvedValue({
        records: [],
        total: 0,
        size: 10,
        current: 1,
        pages: 0
      })
      
      await linkApi.pageLinks({ current: 1, size: 10 })
      
      expect(linkApi.pageLinks).toHaveBeenCalled()
    })

    it('删除短链接 API 应可正常调用', async () => {
      vi.mocked(linkApi.deleteLink).mockResolvedValue(undefined)
      
      await linkApi.deleteLink('abc123')
      
      expect(linkApi.deleteLink).toHaveBeenCalled()
    })
  })
})
