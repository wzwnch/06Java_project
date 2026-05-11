<template>
  <div class="dashboard-container">
    <h2>仪表盘</h2>

    <div class="stats-cards">
      <el-card class="stat-card" shadow="hover" v-loading="statsLoading">
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

      <el-card class="stat-card" shadow="hover" v-loading="statsLoading">
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

      <el-card class="stat-card" shadow="hover" v-loading="statsLoading">
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

      <el-card class="stat-card" shadow="hover" v-loading="statsLoading">
        <div class="stat-content">
          <div class="stat-icon total">
            <el-icon><Link /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">短链接总数</div>
            <div class="stat-value">{{ formatNumber(linkTotal) }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">访问趋势（近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" v-loading="chartLoading"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="recent-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">最近创建的短链接</span>
              <el-button type="primary" link @click="goToLinkPage">
                查看全部
              </el-button>
            </div>
          </template>
          <div class="recent-list" v-loading="recentLoading">
            <div v-if="recentLinks.length === 0" class="empty-tip">
              <el-empty description="暂无短链接" :image-size="80" />
            </div>
            <div v-else class="link-item" v-for="link in recentLinks" :key="link.shortCode">
              <div class="link-info">
                <div class="link-title">
                  <img v-if="link.faviconUrl" :src="link.faviconUrl" class="favicon" alt="" />
                  <el-icon v-else class="default-icon"><Link /></el-icon>
                  <span class="title-text" :title="link.title || link.originUrl">
                    {{ link.title || link.originUrl }}
                  </span>
                </div>
                <div class="link-url">{{ link.shortUrl }}</div>
              </div>
              <div class="link-time">{{ formatRelative(link.createTime) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { View, User, Location, Link } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import dayjs from 'dayjs'
import { listGroups } from '@/api/group'
import { pageLinks } from '@/api/link'
import { getGroupStats, getHistoryStats } from '@/api/stats'
import type { GroupInfo } from '@/types/group'
import type { LinkInfo } from '@/types/link'
import type { StatsResp, StatsHistory } from '@/types/stats'
import { formatNumber, formatRelative } from '@/utils/format'

const router = useRouter()

const statsLoading = ref(false)
const chartLoading = ref(false)
const recentLoading = ref(false)

const todayStats = reactive({
  pv: 0,
  uv: 0,
  uip: 0
})

const linkTotal = ref(0)
const recentLinks = ref<LinkInfo[]>([])

const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null

async function fetchDashboardData(): Promise<void> {
  statsLoading.value = true
  chartLoading.value = true
  recentLoading.value = true

  try {
    const [groups, linksResult] = await Promise.all([
      listGroups(),
      pageLinks({ current: 1, size: 10 })
    ])

    const groupList: GroupInfo[] = groups || []
    recentLinks.value = linksResult.records || []
    linkTotal.value = linksResult.total || 0

    await fetchGroupStats(groupList)

    await fetchTrendData(groupList)
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  } finally {
    statsLoading.value = false
    chartLoading.value = false
    recentLoading.value = false
  }
}

async function fetchGroupStats(groups: GroupInfo[]): Promise<void> {
  if (!groups || groups.length === 0) {
    return
  }

  try {
    const statsPromises = groups.map(group => getGroupStats(group.gid))
    const statsResults = await Promise.all(statsPromises)

    let totalPv = 0
    let totalUv = 0
    let totalUip = 0

    statsResults.forEach((stats: StatsResp) => {
      totalPv += stats.pv || 0
      totalUv += stats.uv || 0
      totalUip += stats.uip || 0
    })

    todayStats.pv = totalPv
    todayStats.uv = totalUv
    todayStats.uip = totalUip
  } catch (error) {
    console.error('获取分组统计失败:', error)
  }
}

async function fetchTrendData(groups: GroupInfo[]): Promise<void> {
  if (!groups || groups.length === 0) {
    await nextTick()
    initChart([])
    return
  }

  try {
    const endDate = dayjs().format('YYYY-MM-DD')
    const startDate = dayjs().subtract(6, 'day').format('YYYY-MM-DD')

    const linksResult = await pageLinks({ current: 1, size: 100 })
    const allLinks = linksResult.records || []

    if (allLinks.length === 0) {
      await nextTick()
      initChart([])
      return
    }

    const historyPromises = allLinks.map(link =>
      getHistoryStats(link.shortCode, startDate, endDate)
    )
    const historyResults = await Promise.all(historyPromises)

    const mergedData = mergeHistoryData(historyResults, startDate, endDate)

    await nextTick()
    initChart(mergedData)
  } catch (error) {
    console.error('获取趋势数据失败:', error)
    await nextTick()
    initChart([])
  }
}

function mergeHistoryData(
  historyResults: StatsHistory[][],
  startDate: string,
  endDate: string
): StatsHistory[] {
  const dateMap = new Map<string, { pv: number; uv: number; uip: number }>()

  let current = dayjs(startDate)
  const end = dayjs(endDate)

  while (current.isBefore(end) || current.isSame(end, 'day')) {
    const dateStr = current.format('YYYY-MM-DD')
    dateMap.set(dateStr, { pv: 0, uv: 0, uip: 0 })
    current = current.add(1, 'day')
  }

  historyResults.forEach(historyList => {
    if (!historyList) return
    historyList.forEach(item => {
      const dateStr = dayjs(item.date).format('YYYY-MM-DD')
      const existing = dateMap.get(dateStr)
      if (existing) {
        existing.pv += item.pv || 0
        existing.uv += item.uv || 0
        existing.uip += item.uip || 0
      }
    })
  })

  const result: StatsHistory[] = []
  current = dayjs(startDate)
  while (current.isBefore(end) || current.isSame(end, 'day')) {
    const dateStr = current.format('YYYY-MM-DD')
    const data = dateMap.get(dateStr) || { pv: 0, uv: 0, uip: 0 }
    result.push({
      date: dateStr,
      pv: data.pv,
      uv: data.uv,
      uip: data.uip
    })
    current = current.add(1, 'day')
  }

  return result
}

function initChart(data: StatsHistory[]): void {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

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

function handleResize(): void {
  trendChart?.resize()
}

function goToLinkPage(): void {
  router.push('/link')
}

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.dashboard-container h2 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #303133;
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
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.chart-container {
  height: 320px;
}

.recent-card {
  border-radius: 8px;
  height: 100%;
}

.recent-list {
  max-height: 320px;
  overflow-y: auto;
}

.empty-tip {
  padding: 40px 0;
}

.link-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.link-item:last-child {
  border-bottom: none;
}

.link-info {
  flex: 1;
  min-width: 0;
}

.link-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.favicon {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  flex-shrink: 0;
}

.default-icon {
  font-size: 16px;
  color: #909399;
  flex-shrink: 0;
}

.title-text {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-url {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-time {
  font-size: 12px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 12px;
}
</style>
