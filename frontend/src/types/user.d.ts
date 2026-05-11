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

export interface UserUpdateForm {
  phone?: string
  mail?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

export interface CheckUsernameResponse {
  exists: boolean
}
