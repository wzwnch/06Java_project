import { get, post, put } from '@/utils/request'
import type {
  LoginForm,
  RegisterForm,
  UserUpdateForm,
  LoginResponse,
  UserInfo,
  CheckUsernameResponse
} from '@/types/user'

export function register(data: RegisterForm): Promise<void> {
  return post<void>('/user/register', data)
}

export function login(data: LoginForm): Promise<LoginResponse> {
  return post<LoginResponse>('/user/login', data)
}

export function logout(): Promise<void> {
  return post<void>('/user/logout')
}

export function getUserInfo(): Promise<UserInfo> {
  return get<UserInfo>('/user/info')
}

export function updateUserInfo(data: UserUpdateForm): Promise<UserInfo> {
  return put<UserInfo>('/user/info', data)
}

export function checkUsername(username: string): Promise<CheckUsernameResponse> {
  return get<CheckUsernameResponse>('/user/check-username', { username })
}
