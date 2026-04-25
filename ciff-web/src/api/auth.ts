import { post, get } from '@/utils/request'
import type { LoginUser } from '@/utils/auth'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: LoginUser
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/login', data)
}

export function register(data: { username: string; password: string; role?: string }): Promise<LoginUser> {
  return post<LoginUser>('/auth/register', data)
}

export function getMe(): Promise<LoginUser> {
  return get<LoginUser>('/auth/me')
}

export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}
