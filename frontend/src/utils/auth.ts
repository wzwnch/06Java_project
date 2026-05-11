const TOKEN_KEY = 'shortlink_token'
const USER_INFO_KEY = 'shortlink_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export function getUserInfo<T>(): T | null {
  const info = localStorage.getItem(USER_INFO_KEY)
  if (info) {
    try {
      return JSON.parse(info) as T
    } catch {
      return null
    }
  }
  return null
}

export function setUserInfo<T>(info: T): void {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function removeUserInfo(): void {
  localStorage.removeItem(USER_INFO_KEY)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}
