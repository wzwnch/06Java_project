import type { PageQuery } from './common'

export interface LinkInfo {
  shortCode: string
  shortUrl: string
  gid: string
  originUrl: string
  faviconUrl: string | null
  title: string | null
  expireTime: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface LinkCreateForm {
  originUrl: string
  gid: string
  expireTime?: string
  customCode?: string
}

export interface LinkUpdateForm {
  shortCode: string
  originUrl?: string
  gid?: string
  expireTime?: string | null
}

export interface LinkPageQuery extends PageQuery {
  gid?: string
}

export const LinkStatusEnum = {
  NORMAL: 0,
  RECYCLED: 1
} as const

export type LinkStatusType = typeof LinkStatusEnum[keyof typeof LinkStatusEnum]
