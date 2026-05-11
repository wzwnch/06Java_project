import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  LinkInfo,
  LinkCreateForm,
  LinkUpdateForm,
  LinkPageQuery
} from '@/types/link'

export function createLink(data: LinkCreateForm): Promise<LinkInfo> {
  return post<LinkInfo>('/link', data)
}

export function pageLinks(params: LinkPageQuery): Promise<PageResult<LinkInfo>> {
  return get<PageResult<LinkInfo>>('/link/page', params)
}

export function updateLink(data: LinkUpdateForm): Promise<void> {
  return put<void>('/link', data)
}

export function deleteLink(shortCode: string): Promise<void> {
  return del<void>(`/link/${shortCode}`)
}
