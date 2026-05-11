<template>
  <div class="link-container">
    <div class="header-actions">
      <h2>短链接管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增短链接
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="queryParams.gid"
        placeholder="选择分组"
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
      :data="linkList"
      v-loading="loading"
      border
      class="link-table"
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
            <el-button
              type="primary"
              link
              size="small"
              @click="copyLink(row.shortUrl)"
            >
              复制
            </el-button>
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
      <el-table-column prop="expireTime" label="有效期" width="160" align="center">
        <template #default="{ row }">
          <span v-if="row.expireTime">
            <el-tag v-if="isExpired(row.expireTime)" type="danger" size="small">已过期</el-tag>
            <span v-else>{{ formatDate(row.expireTime) }}</span>
          </span>
          <span v-else class="text-muted">永久有效</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="openDeleteConfirm(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && linkList.length === 0" description="暂无短链接数据" />

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新增短链接"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="目标URL" prop="originUrl">
          <el-input
            v-model="createForm.originUrl"
            placeholder="请输入目标URL，以http://或https://开头"
            maxlength="2048"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="所属分组" prop="gid">
          <el-select
            v-model="createForm.gid"
            placeholder="请选择分组"
            style="width: 100%"
          >
            <el-option
              v-for="group in groupList"
              :key="group.gid"
              :label="group.name"
              :value="group.gid"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期" prop="expireTime">
          <el-date-picker
            v-model="createForm.expireTime"
            type="datetime"
            placeholder="选择过期时间，不选则永久有效"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="自定义短码" prop="customCode">
          <el-input
            v-model="createForm.customCode"
            placeholder="不填则自动生成，4-16位字母或数字"
            maxlength="16"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      title="修改短链接"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
      >
        <el-form-item label="短链接码">
          <el-input :model-value="editForm.shortCode" disabled />
        </el-form-item>
        <el-form-item label="目标URL" prop="originUrl">
          <el-input
            v-model="editForm.originUrl"
            placeholder="请输入目标URL，以http://或https://开头"
            maxlength="2048"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="所属分组" prop="gid">
          <el-select
            v-model="editForm.gid"
            placeholder="请选择分组"
            style="width: 100%"
          >
            <el-option
              v-for="group in groupList"
              :key="group.gid"
              :label="group.name"
              :value="group.gid"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期" prop="expireTime">
          <el-date-picker
            v-model="editForm.expireTime"
            type="datetime"
            placeholder="选择过期时间，清空则永久有效"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Link } from '@element-plus/icons-vue'
import { pageLinks, createLink, updateLink, deleteLink } from '@/api/link'
import { listGroups } from '@/api/group'
import type { LinkInfo, LinkCreateForm, LinkUpdateForm, LinkPageQuery } from '@/types/link'
import type { GroupInfo } from '@/types/group'
import dayjs from 'dayjs'

const loading = ref(false)
const submitLoading = ref(false)
const linkList = ref<LinkInfo[]>([])
const groupList = ref<GroupInfo[]>([])
const total = ref(0)

const queryParams = reactive<LinkPageQuery>({
  current: 1,
  size: 10,
  gid: undefined
})

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const createForm = reactive<LinkCreateForm>({
  originUrl: '',
  gid: '',
  expireTime: undefined,
  customCode: undefined
})

const editForm = reactive<LinkUpdateForm>({
  shortCode: '',
  originUrl: '',
  gid: '',
  expireTime: undefined
})

const validateUrl = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入目标URL'))
  } else if (!/^https?:\/\/.+/i.test(value.trim())) {
    callback(new Error('URL格式不合法，必须以http://或https://开头'))
  } else if (value.length > 2048) {
    callback(new Error('URL长度不能超过2048个字符'))
  } else {
    callback()
  }
}

const validateCustomCode = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
  if (!value) {
    callback()
  } else if (!/^[a-zA-Z0-9]+$/.test(value)) {
    callback(new Error('自定义短链接码只能包含字母和数字'))
  } else if (value.length < 4 || value.length > 16) {
    callback(new Error('自定义短链接码长度需在4-16个字符之间'))
  } else {
    callback()
  }
}

const createRules: FormRules = {
  originUrl: [{ required: true, validator: validateUrl, trigger: 'blur' }],
  gid: [{ required: true, message: '请选择分组', trigger: 'change' }],
  customCode: [{ validator: validateCustomCode, trigger: 'blur' }]
}

const editRules: FormRules = {
  originUrl: [{ validator: validateUrl, trigger: 'blur' }],
  gid: [{ required: true, message: '请选择分组', trigger: 'change' }]
}

function formatDate(date: string | null): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

function truncateUrl(url: string): string {
  if (!url) return '-'
  return url.length > 40 ? url.substring(0, 40) + '...' : url
}

function isExpired(expireTime: string): boolean {
  return dayjs(expireTime).isBefore(dayjs())
}

function disabledDate(time: Date): boolean {
  return time.getTime() < Date.now() - 86400000
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

async function fetchLinkList(): Promise<void> {
  loading.value = true
  try {
    const params: LinkPageQuery = {
      current: queryParams.current,
      size: queryParams.size
    }
    if (queryParams.gid) {
      params.gid = queryParams.gid
    }
    const data = await pageLinks(params)
    linkList.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取短链接列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuery(): void {
  queryParams.current = 1
  fetchLinkList()
}

function openCreateDialog(): void {
  createForm.originUrl = ''
  createForm.gid = groupList.value.length > 0 ? groupList.value[0].gid : ''
  createForm.expireTime = undefined
  createForm.customCode = undefined
  createDialogVisible.value = true
  setTimeout(() => {
    createFormRef.value?.clearValidate()
  }, 0)
}

async function handleCreate(): Promise<void> {
  if (!createFormRef.value) return
  
  try {
    await createFormRef.value.validate()
    submitLoading.value = true
    
    const data: LinkCreateForm = {
      originUrl: createForm.originUrl.trim(),
      gid: createForm.gid
    }
    if (createForm.expireTime) {
      data.expireTime = createForm.expireTime
    }
    if (createForm.customCode && createForm.customCode.trim()) {
      data.customCode = createForm.customCode.trim()
    }
    
    const result = await createLink(data)
    ElMessage.success('新增短链接成功')
    createDialogVisible.value = false
    
    copyLink(result.shortUrl)
    
    await fetchLinkList()
  } catch (error) {
    console.error('新增短链接失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function openEditDialog(row: LinkInfo): void {
  editForm.shortCode = row.shortCode
  editForm.originUrl = row.originUrl
  editForm.gid = row.gid
  editForm.expireTime = row.expireTime || undefined
  editDialogVisible.value = true
  setTimeout(() => {
    editFormRef.value?.clearValidate()
  }, 0)
}

async function handleEdit(): Promise<void> {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    submitLoading.value = true
    
    const data: LinkUpdateForm = {
      shortCode: editForm.shortCode,
      gid: editForm.gid
    }
    if (editForm.originUrl && editForm.originUrl.trim()) {
      data.originUrl = editForm.originUrl.trim()
    }
    if (editForm.expireTime) {
      data.expireTime = editForm.expireTime
    } else {
      data.expireTime = null
    }
    
    await updateLink(data)
    ElMessage.success('修改短链接成功')
    editDialogVisible.value = false
    await fetchLinkList()
  } catch (error) {
    console.error('修改短链接失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function openDeleteConfirm(row: LinkInfo): void {
  ElMessageBox.confirm(
    `确定要删除短链接「${row.shortUrl}」吗？删除后将移入回收站。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await deleteLink(row.shortCode)
        ElMessage.success('删除短链接成功')
        await fetchLinkList()
      } catch (error) {
        console.error('删除短链接失败:', error)
      }
    })
    .catch(() => {})
}

function copyLink(url: string): void {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(url)
      .then(() => {
        ElMessage.success('短链接已复制到剪贴板')
      })
      .catch(() => {
        fallbackCopy(url)
      })
  } else {
    fallbackCopy(url)
  }
}

function fallbackCopy(url: string): void {
  const textarea = document.createElement('textarea')
  textarea.value = url
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  try {
    const successful = document.execCommand('copy')
    if (successful) {
      ElMessage.success('短链接已复制到剪贴板')
    } else {
      ElMessage.error('复制失败，请手动复制')
    }
  } catch {
    ElMessage.error('复制失败，请手动复制')
  } finally {
    document.body.removeChild(textarea)
  }
}

onMounted(() => {
  fetchGroupList()
  fetchLinkList()
})
</script>

<style scoped>
.link-container {
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

.link-table {
  width: 100%;
}

.link-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  color: #409eff;
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

.text-muted {
  color: #909399;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
