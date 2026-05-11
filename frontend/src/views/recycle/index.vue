<template>
  <div class="recycle-container">
    <div class="header-actions">
      <h2>回收站</h2>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="queryParams.gid"
        placeholder="选择分组筛选"
        clearable
        style="width: 200px"
        @change="handleQuery"
      >
        <el-option
          v-for="group in groupList"
          :key="group.gid"
          :label="group.name"
          :value="group.gid"
        />
      </el-select>
    </div>

    <el-table
      :data="recycleList"
      v-loading="loading"
      border
      class="recycle-table"
    >
      <el-table-column label="短链接" min-width="200">
        <template #default="{ row }">
          <div class="link-cell">
            <div class="link-info">
              <img
                v-if="row.faviconUrl"
                :src="row.faviconUrl"
                class="favicon"
                @error="handleFaviconError"
              />
              <el-icon v-else class="favicon-placeholder"><Link /></el-icon>
              <span class="short-url">{{ row.shortUrl }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="150">
        <template #default="{ row }">
          <span>{{ row.title || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="originUrl" label="原始链接" min-width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.originUrl" placement="top" :show-after="500">
            <a
              :href="row.originUrl"
              target="_blank"
              class="origin-url"
            >
              {{ truncateUrl(row.originUrl) }}
            </a>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="gid" label="所属分组" width="120" align="center">
        <template #default="{ row }">
          <span>{{ getGroupName(row.gid) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="删除时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatDate(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="success" link @click="openRecoverConfirm(row)">恢复</el-button>
          <el-button type="danger" link @click="openRemoveConfirm(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && recycleList.length === 0" description="回收站为空" />

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { pageRecycle, recoverLink, removeLink } from '@/api/recycle'
import { listGroups } from '@/api/group'
import type { LinkInfo } from '@/types/link'
import type { GroupInfo } from '@/types/group'
import type { RecyclePageQuery } from '@/api/recycle'
import dayjs from 'dayjs'

const loading = ref(false)
const recycleList = ref<LinkInfo[]>([])
const groupList = ref<GroupInfo[]>([])
const total = ref(0)

const queryParams = reactive<RecyclePageQuery>({
  current: 1,
  size: 10,
  gid: undefined
})

function formatDate(date: string | null): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

function truncateUrl(url: string): string {
  if (!url) return '-'
  return url.length > 40 ? url.substring(0, 40) + '...' : url
}

function getGroupName(gid: string): string {
  const group = groupList.value.find(g => g.gid === gid)
  return group ? group.name : '-'
}

function handleFaviconError(e: Event): void {
  const target = e.target as HTMLImageElement
  target.style.display = 'none'
}

async function fetchGroupList(): Promise<void> {
  try {
    const data = await listGroups()
    groupList.value = data || []
  } catch (error) {
    console.error('获取分组列表失败:', error)
  }
}

async function fetchRecycleList(): Promise<void> {
  loading.value = true
  try {
    const params: RecyclePageQuery = {
      current: queryParams.current,
      size: queryParams.size
    }
    if (queryParams.gid) {
      params.gid = queryParams.gid
    }
    const data = await pageRecycle(params)
    recycleList.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取回收站列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuery(): void {
  queryParams.current = 1
  fetchRecycleList()
}

function handleSizeChange(): void {
  queryParams.current = 1
  fetchRecycleList()
}

function handleCurrentChange(): void {
  fetchRecycleList()
}

function openRecoverConfirm(row: LinkInfo): void {
  ElMessageBox.confirm(
    `确定要恢复短链接「${row.shortUrl}」吗？恢复后短链接将重新可用。`,
    '恢复确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  )
    .then(async () => {
      try {
        await recoverLink(row.shortCode)
        ElMessage.success('恢复短链接成功')
        await fetchRecycleList()
      } catch (error) {
        console.error('恢复短链接失败:', error)
      }
    })
    .catch(() => {})
}

function openRemoveConfirm(row: LinkInfo): void {
  ElMessageBox.confirm(
    `确定要彻底删除短链接「${row.shortUrl}」吗？此操作不可恢复，数据将被永久删除。`,
    '彻底删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(async () => {
      try {
        await removeLink(row.shortCode)
        ElMessage.success('彻底删除成功')
        await fetchRecycleList()
      } catch (error) {
        console.error('彻底删除失败:', error)
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchGroupList()
  fetchRecycleList()
})
</script>

<style scoped>
.recycle-container {
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.filter-bar {
  margin-bottom: 16px;
}

.recycle-table {
  width: 100%;
}

.link-cell {
  display: flex;
  align-items: center;
}

.link-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.favicon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.favicon-placeholder {
  width: 16px;
  height: 16px;
  color: #909399;
  flex-shrink: 0;
}

.short-url {
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.origin-url {
  color: #606266;
  text-decoration: none;
}

.origin-url:hover {
  color: #409eff;
  text-decoration: underline;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
