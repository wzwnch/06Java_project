export interface GroupInfo {
  gid: string
  name: string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface GroupCreateForm {
  name: string
  sortOrder?: number
}

export interface GroupUpdateForm {
  gid: string
  name: string
  sortOrder?: number
}

export interface GroupSortForm {
  gidList: string[]
}
