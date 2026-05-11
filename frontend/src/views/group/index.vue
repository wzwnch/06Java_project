<template>
  <div class="group-container">
    <div class="header-actions">
      <h2>分组管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增分组
      </el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="tip-alert"
    >
      <template #title>
        提示：通过"上移""下移"按钮调整分组排序顺序
      </template>
    </el-alert>

    <el-table
      :data="groupList"
      v-loading="loading"
      row-key="gid"
      border
      class="group-table"
    >
      <el-table-column prop="name" label="分组名称" min-width="200" />
      <el-table-column prop="sortOrder" label="排序值" width="100" align="center" />
      <el-table-column prop="createTime" label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatDate(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="排序" width="120" align="center">
        <template #default="{ $index }">
          <el-button
            type="primary"
            link
            :disabled="$index === 0"
            @click="handleMoveUp($index)"
          >
            上移
          </el-button>
          <el-button
            type="primary"
            link
            :disabled="$index === groupList.length - 1"
            @click="handleMoveDown($index)"
          >
            下移
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="openDeleteConfirm(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && groupList.length === 0" description="暂无分组数据" />

    <el-dialog
      v-model="createDialogVisible"
      title="新增分组"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="分组名称" prop="name">
          <el-input
            v-model="createForm.name"
            placeholder="请输入分组名称"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number
            v-model="createForm.sortOrder"
            :min="0"
            :max="9999"
            placeholder="越小越靠前"
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
      title="修改分组"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="分组名称" prop="name">
          <el-input
            v-model="editForm.name"
            placeholder="请输入分组名称"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number
            v-model="editForm.sortOrder"
            :min="0"
            :max="9999"
            placeholder="越小越靠前"
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
import { Plus } from '@element-plus/icons-vue'
import { listGroups, createGroup, updateGroup, deleteGroup, sortGroups } from '@/api/group'
import type { GroupInfo, GroupCreateForm, GroupUpdateForm } from '@/types/group'
import dayjs from 'dayjs'

const loading = ref(false)
const submitLoading = ref(false)
const groupList = ref<GroupInfo[]>([])

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const createForm = reactive<GroupCreateForm>({
  name: '',
  sortOrder: 0
})

const editForm = reactive<GroupUpdateForm>({
  gid: '',
  name: '',
  sortOrder: 0
})

const validateName = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入分组名称'))
  } else if (value.length > 64) {
    callback(new Error('分组名称不能超过64个字符'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  name: [{ required: true, validator: validateName, trigger: 'blur' }]
}

function formatDate(date: string): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

async function fetchGroupList() {
  loading.value = true
  try {
    const data = await listGroups()
    groupList.value = data || []
  } catch (error) {
    console.error('获取分组列表失败:', error)
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  createForm.name = ''
  createForm.sortOrder = 0
  createDialogVisible.value = true
  setTimeout(() => {
    createFormRef.value?.clearValidate()
  }, 0)
}

async function handleCreate() {
  if (!createFormRef.value) return
  
  try {
    await createFormRef.value.validate()
    submitLoading.value = true
    await createGroup({
      name: createForm.name.trim(),
      sortOrder: createForm.sortOrder || 0
    })
    ElMessage.success('新增分组成功')
    createDialogVisible.value = false
    await fetchGroupList()
  } catch (error) {
    console.error('新增分组失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function openEditDialog(row: GroupInfo) {
  editForm.gid = row.gid
  editForm.name = row.name
  editForm.sortOrder = row.sortOrder
  editDialogVisible.value = true
  setTimeout(() => {
    editFormRef.value?.clearValidate()
  }, 0)
}

async function handleEdit() {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    submitLoading.value = true
    await updateGroup({
      gid: editForm.gid,
      name: editForm.name.trim(),
      sortOrder: editForm.sortOrder || 0
    })
    ElMessage.success('修改分组成功')
    editDialogVisible.value = false
    await fetchGroupList()
  } catch (error) {
    console.error('修改分组失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function openDeleteConfirm(row: GroupInfo) {
  ElMessageBox.confirm(
    `确定要删除分组「${row.name}」吗？如果分组下有短链接，将无法删除。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await deleteGroup(row.gid)
        ElMessage.success('删除分组成功')
        await fetchGroupList()
      } catch (error) {
        console.error('删除分组失败:', error)
      }
    })
    .catch(() => {})
}

async function handleMoveUp(index: number) {
  if (index <= 0) return
  
  const newList = [...groupList.value]
  const temp = newList[index]
  newList[index] = newList[index - 1]
  newList[index - 1] = temp
  
  await saveSort(newList)
}

async function handleMoveDown(index: number) {
  if (index >= groupList.value.length - 1) return
  
  const newList = [...groupList.value]
  const temp = newList[index]
  newList[index] = newList[index + 1]
  newList[index + 1] = temp
  
  await saveSort(newList)
}

async function saveSort(newList: GroupInfo[]) {
  const gidList = newList.map(item => item.gid)
  
  try {
    await sortGroups({ gidList })
    ElMessage.success('排序保存成功')
    groupList.value = newList
  } catch (error) {
    console.error('排序保存失败:', error)
    await fetchGroupList()
  }
}

onMounted(() => {
  fetchGroupList()
})
</script>

<style scoped>
.group-container {
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

.tip-alert {
  margin-bottom: 16px;
}

.group-table {
  width: 100%;
}
</style>
