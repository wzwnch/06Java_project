import { get } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  StatsResp,
  StatsToday,
  StatsHistory,
  AccessLog,
  StatsLogPageQuery
} from '@/types/stats'

export function getLinkStats(shortCode: string): Promise<StatsResp> {
  return get<StatsResp>(`/stats/link/${shortCode}`)
}

export function getTodayStats(shortCode: string): Promise<StatsToday> {
  return get<StatsToday>('/stats/today', { shortCode })
}

export function getHistoryStats(
  shortCode: string,
  startDate: string,
  endDate: string
): Promise<StatsHistory[]> {
  return get<StatsHistory[]>('/stats/history', {
    shortCode,
    startDate,
    endDate
  })
}

export function getGroupStats(gid: string): Promise<StatsResp> {
  return get<StatsResp>(`/stats/group/${gid}`)
}

export function pageAccessLog(params: StatsLogPageQuery): Promise<PageResult<AccessLog>> {
  return get<PageResult<AccessLog>>('/stats/log/page', params)
}

export function getHighFreqIp(shortCode: string, limit: number = 10): Promise<string[]> {
  return get<string[]>('/stats/high-freq-ip', { shortCode, limit })
}
