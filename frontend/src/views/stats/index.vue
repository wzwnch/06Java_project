<template>
  <div class="stats-container">
    <h2>监控统计</h2>

    <div class="filter-bar">
      <el-select
        v-model="selectedGid"
        placeholder="选择分组"
        clearable
        style="width: 180px"
        @change="handleGroupChange"
      >
        <el-option
          v-for="group in groupList"
          :key="group.gid"
          :label="group.name"
          :value="group.gid"
        />
      </el-select>

      <el-select
        v-model="selectedShortCode"
        placeholder="选择短链接"
        clearable
        filterable
        style="width: 280px"
        @change="handleShortCodeChange"
      >
        <el-option
          v-for="link in linkList"
          :key="link.shortCode"
          :label="link.shortUrl || link.shortCode"
          :value="link.shortCode"
        />
      </el-select>

      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        :disabled-date="disabledDate"
        style="width: 260px"
        @change="handleDateChange"
      />
    </div>

    <div v-if="!selectedShortCode" class="empty-tip">
      <el-empty description="请先选择一个短链接查看统计数据" />
    </div>

    <template v-else>
      <div class="stats-cards">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon pv">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日 PV</div>
              <div class="stat-value">{{ formatNumber(todayStats.pv) }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon uv">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日 UV</div>
              <div class="stat-value">{{ formatNumber(todayStats.uv) }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon uip">
              <el-icon><Location /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日 UIP</div>
              <div class="stat-value">{{ formatNumber(todayStats.uip) }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon total">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">累计 PV</div>
              <div class="stat-value">{{ formatNumber(totalStats.pv) }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <el-card class="chart-card" shadow="hover">
        <template #header>
          <span class="chart-title">历史趋势</span>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>

      <div class="pie-charts">
        <el-card class="pie-card" shadow="hover">
          <template #header>
            <span class="chart-title">地区分布</span>
          </template>
          <div ref="regionChartRef" class="pie-chart-container"></div>
        </el-card>

        <el-card class="pie-card" shadow="hover">
          <template #header>
            <span class="chart-title">设备分布</span>
          </template>
          <div ref="deviceChartRef" class="pie-chart-container"></div>
        </el-card>

        <el-card class="pie-card" shadow="hover">
          <template #header>
            <span class="chart-title">浏览器分布</span>
          </template>
          <div ref="browserChartRef" class="pie-chart-container"></div>
        </el-card>

        <el-card class="pie-card" shadow="hover">
          <template #header>
            <span class="chart-title">操作系统分布</span>
          </template>
          <div ref="osChartRef" class="pie-chart-container"></div>
        </el-card>
      </div>

      <el-card class="log-card" shadow="hover">
        <template #header>
          <span class="chart-title">访问日志</span>
        </template>
        <el-table
          :data="logList"
          v-loading="logLoading"
          border
          class="log-table"
        >
          <el-table-column prop="ip" label="IP 地址" width="140" align="center" />
          <el-table-column prop="region" label="地区" width="120" align="center">
            <template #default="{ row }">
              {{ row.region || '未知' }}
            </template>
          </el-table-column>
          <el-table-column prop="device" label="设备" width="100" align="center">
            <template #default="{ row }">
              {{ row.device || '未知' }}
            </template>
          </el-table-column>
          <el-table-column prop="browser" label="浏览器" width="120" align="center">
            <template #default="{ row }">
              {{ row.browser || '未知' }}
            </template>
          </el-table-column>
          <el-table-column prop="os" label="操作系统" width="120" align="center">
            <template #default="{ row }">
              {{ row.os || '未知' }}
            </template>
          </el-table-column>
          <el-table-column prop="network" label="网络" width="100" align="center">
            <template #default="{ row }">
              {{ row.network || '未知' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="访问时间" width="180" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper" v-if="logTotal > 0">
          <el-pagination
            v-model:current-page="logQuery.current"
            v-model:page-size="logQuery.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="logTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchLogList"
            @current-change="fetchLogList"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { View, User, Location, DataLine } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import dayjs from 'dayjs'
import { listGroups } from '@/api/group'
import { pageLinks } from '@/api/link'
import { getLinkStats, getTodayStats, getHistoryStats, pageAccessLog } from '@/api/stats'
import type { GroupInfo } from '@/types/group'
import type { LinkInfo } from '@/types/link'
import type { StatsResp, StatsToday, StatsHistory, AccessLog, StatsLogPageQuery } from '@/types/stats'

const groupList = ref<GroupInfo[]>([])
const linkList = ref<LinkInfo[]>([])
const selectedGid = ref<string>('')
const selectedShortCode = ref<string>('')
const dateRange = ref<[string, string] | null>(null)

const todayStats = reactive<StatsToday>({
  date: '',
  pv: 0,
  uv: 0,
  uip: 0
})

const totalStats = reactive<StatsResp>({
  shortCode: '',
  gid: '',
  pv: 0,
  uv: 0,
  uip: 0
})

const logLoading = ref(false)
const logList = ref<AccessLog[]>([])
const logTotal = ref(0)
const logQuery = reactive<StatsLogPageQuery>({
  current: 1,
  size: 10,
  shortCode: '',
  startTime: undefined,
  endTime: undefined
})

const trendChartRef = ref<HTMLElement>()
const regionChartRef = ref<HTMLElement>()
const deviceChartRef = ref<HTMLElement>()
const browserChartRef = ref<HTMLElement>()
const osChartRef = ref<HTMLElement>()

let trendChart: ECharts | null = null
let regionChart: ECharts | null = null
let deviceChart: ECharts | null = null
let browserChart: ECharts | null = null
let osChart: ECharts | null = null

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

function formatDateTime(date: string | null): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

function disabledDate(time: Date): boolean {
  return time.getTime() > Date.now()
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
  try {
    const params: { current: number; size: number; gid?: string } = {
      current: 1,
      size: 100
    }
    if (selectedGid.value) {
      params.gid = selectedGid.value
    }
    const data = await pageLinks(params)
    linkList.value = data.records || []
  } catch (error) {
    console.error('获取短链接列表失败:', error)
  }
}

function handleGroupChange(): void {
  selectedShortCode.value = ''
  fetchLinkList()
}

async function handleShortCodeChange(): Promise<void> {
  if (!selectedShortCode.value) return
  
  logQuery.shortCode = selectedShortCode.value
  logQuery.current = 1
  
  await Promise.all([
    fetchTodayStats(),
    fetchTotalStats(),
    fetchHistoryStats(),
    fetchLogList()
  ])
  
  await nextTick()
  initCharts()
  updatePieCharts(logList.value)
}

function handleDateChange(): void {
  if (dateRange.value && dateRange.value.length === 2) {
    logQuery.startTime = dateRange.value[0] + ' 00:00:00'
    logQuery.endTime = dateRange.value[1] + ' 23:59:59'
  } else {
    logQuery.startTime = undefined
    logQuery.endTime = undefined
  }
  logQuery.current = 1
  fetchLogList().then(() => {
    updatePieCharts(logList.value)
  })
  fetchHistoryStats()
}

async function fetchTodayStats(): Promise<void> {
  if (!selectedShortCode.value) return
  try {
    const data = await getTodayStats(selectedShortCode.value)
    todayStats.date = data.date
    todayStats.pv = data.pv || 0
    todayStats.uv = data.uv || 0
    todayStats.uip = data.uip || 0
  } catch (error) {
    console.error('获取今日统计失败:', error)
  }
}

async function fetchTotalStats(): Promise<void> {
  if (!selectedShortCode.value) return
  try {
    const data = await getLinkStats(selectedShortCode.value)
    totalStats.shortCode = data.shortCode
    totalStats.gid = data.gid
    totalStats.pv = data.pv || 0
    totalStats.uv = data.uv || 0
    totalStats.uip = data.uip || 0
  } catch (error) {
    console.error('获取总统计失败:', error)
  }
}

async function fetchHistoryStats(): Promise<void> {
  if (!selectedShortCode.value) return
  
  let startDate: string
  let endDate: string
  
  if (dateRange.value && dateRange.value.length === 2) {
    startDate = dateRange.value[0]
    endDate = dateRange.value[1]
  } else {
    endDate = dayjs().format('YYYY-MM-DD')
    startDate = dayjs().subtract(7, 'day').format('YYYY-MM-DD')
  }
  
  try {
    const data = await getHistoryStats(selectedShortCode.value, startDate, endDate)
    updateTrendChart(data || [])
  } catch (error) {
    console.error('获取历史统计失败:', error)
  }
}

async function fetchLogList(): Promise<void> {
  if (!selectedShortCode.value) return
  
  logLoading.value = true
  try {
    const params: StatsLogPageQuery = {
      current: logQuery.current,
      size: logQuery.size,
      shortCode: selectedShortCode.value
    }
    if (logQuery.startTime) {
      params.startTime = logQuery.startTime
    }
    if (logQuery.endTime) {
      params.endTime = logQuery.endTime
    }
    
    const data = await pageAccessLog(params)
    logList.value = data.records || []
    logTotal.value = data.total || 0
  } catch (error) {
    console.error('获取访问日志失败:', error)
  } finally {
    logLoading.value = false
  }
}

function initCharts(): void {
  if (trendChartRef.value) {
    if (trendChart) {
      trendChart.dispose()
    }
    trendChart = echarts.init(trendChartRef.value)
  }
  
  if (regionChartRef.value) {
    if (regionChart) {
      regionChart.dispose()
    }
    regionChart = echarts.init(regionChartRef.value)
  }
  
  if (deviceChartRef.value) {
    if (deviceChart) {
      deviceChart.dispose()
    }
    deviceChart = echarts.init(deviceChartRef.value)
  }
  
  if (browserChartRef.value) {
    if (browserChart) {
      browserChart.dispose()
    }
    browserChart = echarts.init(browserChartRef.value)
  }
  
  if (osChartRef.value) {
    if (osChart) {
      osChart.dispose()
    }
    osChart = echarts.init(osChartRef.value)
  }
}

function updateTrendChart(data: StatsHistory[]): void {
  if (!trendChart) return
  
  const dates = data.map(item => item.date)
  const pvList = data.map(item => item.pv || 0)
  const uvList = data.map(item => item.uv || 0)
  const uipList = data.map(item => item.uip || 0)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['PV', 'UV', 'UIP'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '40px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: 'PV',
        type: 'line',
        smooth: true,
        data: pvList,
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        data: uvList,
        itemStyle: { color: '#67c23a' }
      },
      {
        name: 'UIP',
        type: 'line',
        smooth: true,
        data: uipList,
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }
  
  trendChart.setOption(option)
}

function updatePieCharts(logs: AccessLog[]): void {
  const regionMap = new Map<string, number>()
  const deviceMap = new Map<string, number>()
  const browserMap = new Map<string, number>()
  const osMap = new Map<string, number>()
  
  logs.forEach(log => {
    const region = log.region || '未知'
    const device = log.device || '未知'
    const browser = log.browser || '未知'
    const os = log.os || '未知'
    
    regionMap.set(region, (regionMap.get(region) || 0) + 1)
    deviceMap.set(device, (deviceMap.get(device) || 0) + 1)
    browserMap.set(browser, (browserMap.get(browser) || 0) + 1)
    osMap.set(os, (osMap.get(os) || 0) + 1)
  })
  
  updatePieChart(regionChart, regionMap, '地区')
  updatePieChart(deviceChart, deviceMap, '设备')
  updatePieChart(browserChart, browserMap, '浏览器')
  updatePieChart(osChart, osMap, '操作系统')
}

function updatePieChart(chart: ECharts | null, dataMap: Map<string, number>, name: string): void {
  if (!chart) return
  
  const data = Array.from(dataMap.entries())
    .map(([key, value]) => ({ name: key, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name,
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data
      }
    ]
  }
  
  chart.setOption(option)
}

function handleResize(): void {
  trendChart?.resize()
  regionChart?.resize()
  deviceChart?.resize()
  browserChart?.resize()
  osChart?.resize()
}

onMounted(() => {
  fetchGroupList()
  fetchLinkList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  regionChart?.dispose()
  deviceChart?.dispose()
  browserChart?.dispose()
  osChart?.dispose()
})

watch(selectedShortCode, (newVal) => {
  if (newVal) {
    logQuery.shortCode = newVal
  }
})
</script>

<style scoped>
.stats-container {
  padding: 20px;
}

.stats-container h2 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.empty-tip {
  padding: 60px 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.pv {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.stat-icon.uv {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.stat-icon.uip {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.stat-icon.total {
  background: linear-gradient(135deg, #909399, #b1b3b8);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.chart-container {
  height: 320px;
}

.pie-charts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1400px) {
  .pie-charts {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .pie-charts {
    grid-template-columns: 1fr;
  }
}

.pie-card {
  border-radius: 8px;
}

.pie-chart-container {
  height: 260px;
}

.log-card {
  border-radius: 8px;
}

.log-table {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
