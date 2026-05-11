import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Recycle from '@/views/recycle/index.vue'
import * as recycleApi from '@/api/recycle'

vi.mock('@/api/recycle')
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

describe('回收站页面测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('组件挂载测试', () => {
    it('应成功挂载回收站组件', () => {
      const wrapper = shallowMount(Recycle)
      expect(wrapper.exists()).toBe(true)
    })

    it('应显示页面标题', () => {
      const wrapper = shallowMount(Recycle)
      expect(wrapper.text()).toContain('回收站')
    })
  })

  describe('查询回收站接口测试', () => {
    it('正常查询应返回数据', async () => {
      vi.mocked(recycleApi.pageRecycle).mockResolvedValue({
        records: [],
        total: 0,
        size: 10,
        current: 1,
        pages: 0
      })
      
      const wrapper = shallowMount(Recycle)
      const vm = wrapper.vm as any
      
      await vm.fetchRecycleList()
      
      expect(recycleApi.pageRecycle).toHaveBeenCalled()
    })
  })
})
