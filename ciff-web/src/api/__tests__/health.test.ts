import { describe, it, expect, vi } from 'vitest'
import { getHealth } from '../health'
import * as request from '@/utils/request'

vi.mock('@/utils/request')

describe('getHealth', () => {
  it('should call GET /v1/health', async () => {
    const mockGet = vi.spyOn(request, 'get').mockResolvedValue('ok')

    const result = await getHealth()

    expect(mockGet).toHaveBeenCalledWith('/v1/health')
    expect(result).toBe('ok')
  })
})
