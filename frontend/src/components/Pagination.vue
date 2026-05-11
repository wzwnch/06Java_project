<template>
  <div class="pagination-wrapper" v-if="total > 0">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      :layout="layout"
      :background="background"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  current: number
  size: number
  total: number
  pageSizes?: number[]
  layout?: string
  background?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pageSizes: () => [10, 20, 50, 100],
  layout: 'total, sizes, prev, pager, next, jumper',
  background: true
})

const emit = defineEmits<{
  (e: 'update:current', value: number): void
  (e: 'update:size', value: number): void
  (e: 'change', current: number, size: number): void
}>()

const currentPage = computed({
  get: () => props.current,
  set: (val: number) => emit('update:current', val)
})

const pageSize = computed({
  get: () => props.size,
  set: (val: number) => emit('update:size', val)
})

function handleSizeChange(val: number) {
  emit('change', currentPage.value, val)
}

function handleCurrentChange(val: number) {
  emit('change', val, pageSize.value)
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 10px 0;
}
</style>
