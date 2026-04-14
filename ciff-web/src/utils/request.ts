import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
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
    const msg = error.response?.data?.message || error.message || 'Network error'
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
