import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    const msg = res.message || 'Request failed'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      removeToken()
      const currentPath = window.location.pathname
      if (currentPath !== '/login') {
        ElMessage.warning('Session expired, please sign in again')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    if (status === 403) {
      ElMessage.error('Permission denied')
      return Promise.reject(error)
    }
    if (status === 404) {
      ElMessage.error('Resource not found')
      return Promise.reject(error)
    }
    if (!error.response) {
      ElMessage.error('Network error, please check your connection')
      return Promise.reject(error)
    }
    const msg = error.response.data?.message || 'Server error, please try again'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
  return instance.get<T, T>(url, { params, ...config })
}

export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return instance.post<T, T>(url, data, config)
}

export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return instance.put<T, T>(url, data, config)
}

export function del<T = unknown>(url: string, config?: AxiosRequestConfig) {
  return instance.delete<T, T>(url, config)
}
