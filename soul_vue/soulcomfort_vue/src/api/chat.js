import { useAuthStore } from '../stores/auth'

const API_BASE = '/api'

export async function* streamChat(convId, message) {
  const authStore = useAuthStore()
  const url = `${API_BASE}/soulComfort/chat?convId=${encodeURIComponent(convId)}&message=${encodeURIComponent(message)}`

  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${authStore.token}`
    }
  })

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.startsWith('event:')) {
        currentEvent = trimmed.substring(6).trim()
      } else if (trimmed.startsWith('data:')) {
        const data = trimmed.substring(5).trim()
        if (data && (!currentEvent || currentEvent === 'message')) {
          yield data
        }
        currentEvent = ''
      }
    }
  }
}