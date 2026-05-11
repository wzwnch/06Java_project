import { get, post, put, del } from '@/utils/request'
import type { GroupInfo, GroupCreateForm, GroupUpdateForm, GroupSortForm } from '@/types/group'

export function createGroup(data: GroupCreateForm): Promise<string> {
  return post<string>('/group', data)
}

export function listGroups(): Promise<GroupInfo[]> {
  return get<GroupInfo[]>('/group/list')
}

export function updateGroup(data: GroupUpdateForm): Promise<void> {
  return put<void>('/group', data)
}

export function deleteGroup(gid: string): Promise<void> {
  return del<void>(`/group/${gid}`)
}

export function sortGroups(data: GroupSortForm): Promise<void> {
  return put<void>('/group/sort', data)
}
