import { useAuthStore } from '@/stores/auth'

export async function* streamChat(convId, message, quoteMsg) {
  const authStore = useAuthStore()
  let url = `/api/soulComfort/chat?convId=${convId}&message=${encodeURIComponent(message)}`
  if (quoteMsg) {
    url += `&quoteMessage=${encodeURIComponent(quoteMsg.message)}&quoteRole=${encodeURIComponent(quoteMsg.role)}`
  }

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 120_000)

  try {
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      },
      signal: controller.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const contentType = response.headers.get('content-type') || ''
    if (!contentType.includes('text/event-stream')) {
      const text = await response.text()
      throw new Error(`非SSE响应: ${text.substring(0, 200)}`)
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
        if (trimmed === '') {
          currentEvent = ''
          continue
        }
        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim()
          continue
        }
        if (trimmed.startsWith('data:')) {
          const data = trimmed.substring(5).trim()
          if (data && (!currentEvent || currentEvent === 'message')) {
            // 后端将载荷包装为 {"content":"..."}（换行被转义为字面 \n，保护 SSE 协议）。
            // 此处解析还原；解析失败（历史/旧格式）时按原样透传。
            try {
              const parsed = JSON.parse(data)
              if (parsed && typeof parsed.content === 'string') {
                yield parsed.content
              } else {
                yield data
              }
            } catch {
              yield data
            }
          }
          currentEvent = ''
        }
      }
    }
  } finally {
    clearTimeout(timeout)
  }
}