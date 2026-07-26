import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => {
  localStorage.clear()
  vi.resetModules()
})

describe('Shared user session', () => {
  it('clears reactive and stored state after an authentication-expired event', async () => {
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('user', JSON.stringify({ id: 7, username: 'friend' }))

    const { useUser } = await import('./useUser')
    const { token, userInfo } = useUser()

    expect(token.value).toBe('expired-token')
    expect(userInfo.value.username).toBe('friend')

    window.dispatchEvent(new CustomEvent('yukiss:auth-expired'))

    expect(token.value).toBe('')
    expect(userInfo.value).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
