<template>
  <el-aside :width="sidebarWidth" class="sidebar">
    <div class="logo">短链接系统</div>
    <el-menu
      :default-active="activeMenu"
      router
      :collapse="sidebarCollapsed"
    >
      <el-menu-item index="/dashboard">
        <el-icon><House /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>
      <el-menu-item index="/link">
        <el-icon><Link /></el-icon>
        <span>短链接管理</span>
      </el-menu-item>
      <el-menu-item index="/group">
        <el-icon><Folder /></el-icon>
        <span>分组管理</span>
      </el-menu-item>
      <el-menu-item index="/recycle">
        <el-icon><Delete /></el-icon>
        <span>回收站</span>
      </el-menu-item>
      <el-menu-item index="/stats">
        <el-icon><DataLine /></el-icon>
        <span>监控统计</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores'
import { House, Link, Folder, Delete, DataLine } from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)
const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const sidebarWidth = computed(() => appStore.sidebarWidth)
</script>

<style scoped>
.sidebar {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  background-color: #263445;
}

.sidebar :deep(.el-menu) {
  background-color: #304156;
  border-right: none;
}

.sidebar :deep(.el-menu-item) {
  color: #bfcbd9;
}

.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-menu-item.is-active) {
  background-color: #263445;
  color: #409eff;
}
</style>
