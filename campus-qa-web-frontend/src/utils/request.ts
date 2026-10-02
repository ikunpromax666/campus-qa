import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8848',
  timeout: 10000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data

    // 后端统一返回格式：{ code, message, data }
    if (code === 200) {
      return response.data
    }

    // 业务错误（400/401/403 等）
    ElMessage.error(message || '操作失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    // HTTP 错误（404/500 等）
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)

    // 401 未登录，跳登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }

    return Promise.reject(error)
  }
)

// 响应拦截器 return response.data 后，调用处拿到的就是 { code, message, data }（Result 结构）
// 用接口如实声明，避免 axios 默认 AxiosResponse 类型与运行时不一致
interface IRequest {
  get<T = any>(url: string, config?: any): Promise<T>
  post<T = any>(url: string, data?: any, config?: any): Promise<T>
  put<T = any>(url: string, data?: any, config?: any): Promise<T>
  delete<T = any>(url: string, config?: any): Promise<T>
}

export default request as IRequest
