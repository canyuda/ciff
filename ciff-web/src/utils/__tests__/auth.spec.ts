import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  getToken,
  setToken,
  removeToken,
  isAuthenticated,
  getUser,
  setUser,
} from '../auth'

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  // --- getToken / setToken / removeToken ---

  it('should return null when token is not set', () => {
    expect(getToken()).toBeNull()
  })

  it('should set and get token', () => {
    setToken('test-jwt-token')
    expect(getToken()).toBe('test-jwt-token')
  })

  it('should remove token and user on removeToken', () => {
    setToken('test-jwt-token')
    setUser({ id: 1, username: 'admin', role: 'admin' })

    removeToken()

    expect(getToken()).toBeNull()
    expect(getUser()).toBeNull()
  })

  // --- isAuthenticated ---

  it('should return false when no token', () => {
    expect(isAuthenticated()).toBe(false)
  })

  it('should return true when token exists', () => {
    setToken('test-jwt-token')
    expect(isAuthenticated()).toBe(true)
  })

  it('should return false after token is removed', () => {
    setToken('test-jwt-token')
    removeToken()
    expect(isAuthenticated()).toBe(false)
  })

  // --- getUser / setUser ---

  it('should return null when user is not set', () => {
    expect(getUser()).toBeNull()
  })

  it('should set and get user', () => {
    const user = { id: 1, username: 'admin', role: 'admin' }
    setUser(user)

    const stored = getUser()
    expect(stored).toEqual(user)
  })

  it('should handle corrupted JSON in user storage', () => {
    localStorage.setItem('ciff_user', '{invalid-json')
    expect(getUser()).toBeNull()
  })

  it('should overwrite user on subsequent setUser calls', () => {
    setUser({ id: 1, username: 'admin', role: 'admin' })
    setUser({ id: 2, username: 'user', role: 'user' })

    const stored = getUser()
    expect(stored).toEqual({ id: 2, username: 'user', role: 'user' })
  })
})
