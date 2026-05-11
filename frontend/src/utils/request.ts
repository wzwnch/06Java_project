import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearToken } from '@/utils/auth'
import router from '@/router'

const BASE_URL = '/api'
const TIMEOUT = 10000

const instance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code === 0 || data.code === 200) {
      return data.data
    }
    if (data.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearToken()
      router.push('/login')
      return Promise.reject(new Error(data.msg || '未授权'))
    }
    ElMessage.error(data.msg || '请求失败')
    return Promise.reject(new Error(data.msg || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          clearToken()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export interface R<T> {
  code: number
  msg: string
  data: T
}

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return instance.get(url, { params, ...config })
}

export function post<T>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return instance.post(url, data, config)
}

export function put<T>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return instance.put(url, data, config)
}

export function del<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return instance.delete(url, { params, ...config })
}

export default instance
