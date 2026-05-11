import type { PageQuery } from './common'

export interface StatsResp {
  shortCode: string
  gid: string
  pv: number
  uv: number
  uip: number
}

export interface StatsToday {
  date: string
  pv: number
  uv: number
  uip: number
}

export interface StatsHistory {
  date: string
  pv: number
  uv: number
  uip: number
}

export interface AccessLog {
  id: number
  shortCode: string
  gid: string
  pv: number
  uv: string | null
  uip: string | null
  ip: string | null
  region: string | null
  os: string | null
  browser: string | null
  device: string | null
  network: string | null
  createTime: string
}

export interface StatsLogPageQuery extends PageQuery {
  shortCode?: string
  gid?: string
  startTime?: string
  endTime?: string
}

export interface RegionStats {
  region: string
  count: number
}

export interface DeviceStats {
  device: string
  count: number
}

export interface BrowserStats {
  browser: string
  count: number
}

export interface OsStats {
  os: string
  count: number
}
