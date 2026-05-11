<template>
  <el-header class="header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="toggleSidebar">
        <Expand v-if="sidebarCollapsed" />
        <Fold v-else />
      </el-icon>
    </div>
    <div class="header-right">
      <el-dropdown>
        <span class="user-info">
          {{ userStore.userInfo?.username || '用户' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore, useAppStore } from '@/stores'
import { Expand, Fold, ArrowDown } from '@element-plus/icons-vue'

const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

function toggleSidebar() {
  appStore.toggleSidebar()
}

function handleLogout() {
  userStore.logout()
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
