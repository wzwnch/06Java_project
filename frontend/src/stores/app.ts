import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { GroupInfo } from '@/types/common'
import { get } from '@/utils/request'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)
  const groups = ref<GroupInfo[]>([])
  const currentGroup = ref<string | null>(null)

  const sidebarWidth = computed(() => sidebarCollapsed.value ? '64px' : '200px')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setLoading(value: boolean) {
    loading.value = value
  }

  async function fetchGroups() {
    try {
      const data = await get<GroupInfo[]>('/group/list')
      groups.value = data || []
      if (data && data.length > 0 && !currentGroup.value) {
        currentGroup.value = data[0].gid
      }
      return data
    } catch (error) {
      groups.value = []
      return []
    }
  }

  function setCurrentGroup(gid: string | null) {
    currentGroup.value = gid
  }

  function addGroup(group: GroupInfo) {
    groups.value.push(group)
  }

  function updateGroup(gid: string, group: Partial<GroupInfo>) {
    const index = groups.value.findIndex(g => g.gid === gid)
    if (index !== -1) {
      groups.value[index] = { ...groups.value[index], ...group }
    }
  }

  function removeGroup(gid: string) {
    const index = groups.value.findIndex(g => g.gid === gid)
    if (index !== -1) {
      groups.value.splice(index, 1)
      if (currentGroup.value === gid) {
        currentGroup.value = groups.value.length > 0 ? groups.value[0].gid : null
      }
    }
  }

  function clearGroups() {
    groups.value = []
    currentGroup.value = null
  }

  return {
    sidebarCollapsed,
    loading,
    groups,
    currentGroup,
    sidebarWidth,
    toggleSidebar,
    setLoading,
    fetchGroups,
    setCurrentGroup,
    addGroup,
    updateGroup,
    removeGroup,
    clearGroups
  }
})
