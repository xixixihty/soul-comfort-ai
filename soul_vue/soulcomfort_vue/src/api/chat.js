const API_BASE = '/api'

export async function* streamChat(convId, message, userId = 'default_user') {
  const url = `${API_BASE}/soulComfort/chat?convId=${encodeURIComponent(convId)}&userId=${encodeURIComponent(userId)}&message=${encodeURIComponent(message)}`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.startsWith('data:')) {
        const data = trimmed.substring(5).trim()
        if (data) {
          yield data
        }
      }
    }
  }
}