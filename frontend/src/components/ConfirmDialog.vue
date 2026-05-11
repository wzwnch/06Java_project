<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :show-close="showClose"
    @close="handleClose"
  >
    <div class="confirm-content">
      <el-icon v-if="showIcon" :class="['confirm-icon', type]">
        <Warning v-if="type === 'warning'" />
        <CircleCheck v-else-if="type === 'success'" />
        <CircleClose v-else-if="type === 'danger'" />
        <InfoFilled v-else />
      </el-icon>
      <span class="confirm-message">{{ message }}</span>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button v-if="showCancelButton" @click="handleCancel">
          {{ cancelText }}
        </el-button>
        <el-button :type="confirmButtonType" @click="handleConfirm">
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Warning, CircleCheck, CircleClose, InfoFilled } from '@element-plus/icons-vue'

type ConfirmType = 'warning' | 'success' | 'danger' | 'info'

interface Props {
  modelValue: boolean
  title?: string
  message?: string
  type?: ConfirmType
  width?: string | number
  confirmText?: string
  cancelText?: string
  showCancelButton?: boolean
  showIcon?: boolean
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
  showClose?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认操作',
  message: '确定要执行此操作吗？',
  type: 'warning',
  width: '400px',
  confirmText: '确定',
  cancelText: '取消',
  showCancelButton: true,
  showIcon: true,
  closeOnClickModal: false,
  closeOnPressEscape: true,
  showClose: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'close'): void
}>()

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const confirmButtonType = computed(() => {
  const typeMap: Record<ConfirmType, string> = {
    warning: 'warning',
    success: 'success',
    danger: 'danger',
    info: 'primary'
  }
  return typeMap[props.type]
})

function handleConfirm() {
  emit('confirm')
  visible.value = false
}

function handleCancel() {
  emit('cancel')
  visible.value = false
}

function handleClose() {
  emit('close')
}
</script>

<style scoped>
.confirm-content {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.confirm-icon {
  font-size: 24px;
  margin-right: 12px;
}

.confirm-icon.warning {
  color: #e6a23c;
}

.confirm-icon.success {
  color: #67c23a;
}

.confirm-icon.danger {
  color: #f56c6c;
}

.confirm-icon.info {
  color: #909399;
}

.confirm-message {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
