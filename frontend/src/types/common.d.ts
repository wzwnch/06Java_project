export interface R<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageQuery {
  current: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  phone: string | null
  mail: string | null
  realPhone: string | null
  realMail: string | null
  createTime: string
  updateTime: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  phone?: string
  mail?: string
}

export interface GroupInfo {
  id: number
  gid: string
  name: string
  username: string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface LinkInfo {
  id: number
  shortCode: string
  gid: string
  originUrl: string
  faviconUrl: string | null
  title: string | null
  expireTime: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface LinkStats {
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

export interface StatsToday {
  date: string
  pv: number
  uv: number
  uip: number
}

export interface StatsHistory {
  dateList: string[]
  pvList: number[]
  uvList: number[]
  uipList: number[]
}

export interface HighFreqIp {
  ip: string
  count: number
}

export type LinkStatus = 'normal' | 'recycled'
export const LinkStatusEnum = {
  NORMAL: 0,
  RECYCLED: 1
} as const

export type DelFlag = 'normal' | 'deleted'
export const DelFlagEnum = {
  NORMAL: 0,
  DELETED: 1
} as const
