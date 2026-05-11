import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Stats from '@/views/stats/index.vue'

vi.mock('@/api/stats')
vi.mock('@/api/group')
vi.mock('@/api/link')
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
vi.mock('echarts', () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn()
  })),
  graphic: {
    LinearGradient: vi.fn()
  }
}))
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('监控统计页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载监控统计组件', () => {
      const wrapper = shallowMount(Stats)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示页面标题', () => {
      const wrapper = shallowMount(Stats)
      expect(wrapper.text()).toContain('监控统计')
    })
  })

  describe('统计数据测试', () => {
    it('今日统计数据初始值应为0', () => {
      const wrapper = shallowMount(Stats)
      const vm = wrapper.vm as any
      expect(vm.todayStats.pv).toBe(0)
      expect(vm.todayStats.uv).toBe(0)
      expect(vm.todayStats.uip).toBe(0)
    })

    it('累计统计数据初始值应为0', () => {
      const wrapper = shallowMount(Stats)
      const vm = wrapper.vm as any
      expect(vm.totalStats.pv).toBe(0)
      expect(vm.totalStats.uv).toBe(0)
      expect(vm.totalStats.uip).toBe(0)
    })
  })

  describe('查询分组列表接口测试', () => {
    it('正常查询应返回数据', async () => {
      const { listGroups } = await import('@/api/group')
      vi.mocked(listGroups).mockResolvedValue([])
      
      const wrapper = shallowMount(Stats)
      const vm = wrapper.vm as any
      
      await vm.fetchGroupList()
      
      expect(listGroups).toHaveBeenCalled()
    })
  })

  describe('查询短链接列表接口测试', () => {
    it('正常查询应返回数据', async () => {
      const { pageLinks } = await import('@/api/link')
      vi.mocked(pageLinks).mockResolvedValue({
        records: [],
        total: 0,
        size: 10,
        current: 1,
        pages: 0
      })
      
      const wrapper = shallowMount(Stats)
      const vm = wrapper.vm as any
      
      await vm.fetchLinkList()
      
      expect(pageLinks).toHaveBeenCalled()
    })
  })
})
