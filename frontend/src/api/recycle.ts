import { get, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LinkInfo } from '@/types/link'

export interface RecyclePageQuery {
  gid?: string
  current: number
  size: number
}

export function pageRecycle(params: RecyclePageQuery): Promise<PageResult<LinkInfo>> {
  return get<PageResult<LinkInfo>>('/recycle/page', params)
}

export function recoverLink(shortCode: string): Promise<void> {
  return put<void>('/recycle/recover', { shortCode })
}

export function removeLink(shortCode: string): Promise<void> {
  return del<void>(`/recycle/${shortCode}`)
}
