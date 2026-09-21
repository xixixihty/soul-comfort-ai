import net from 'net'

const client = net.connect(6379, '127.0.0.1')
function enc(s) { return Buffer.from(s, 'utf8') }
function cmdBuf(...parts) {
  return Buffer.concat([enc(`*${parts.length}\r\n`), ...parts.map(p => enc(`$${Buffer.byteLength(p)}\r\n${p}\r\n`))])
}
let buffer = Buffer.alloc(0)
const pending = []
function pump(v) {
  const cb = pending.shift()
  if (cb) cb(v)
}
client.on('data', buf => {
  buffer = Buffer.concat([buffer, buf])
  // 尝试逐条解析当前 buffer 中的回复（可能是多个）
  let done = false
  while (!done) {
    try {
      const p = new Parser(buffer)
      const v = p.read()
      buffer = buffer.subarray(p.pos)
      pump(v)
    } catch { done = true }
  }
})
class Parser {
  constructor(buf) { this.buf = buf; this.pos = 0 }
  read() {
    if (this.pos >= this.buf.length) throw new Error('needmore')
    if (this.buf[this.pos] === 0x2a) {
      const n = this.line()
      const arr = []
      for (let i = 0; i < n; i++) arr.push(this.read())
      return arr
    }
    const type = String.fromCharCode(this.buf[this.pos]); this.pos++
    const line = this.line()
    if (type === '+') return line
    if (type === ':') return Number(line)
    if (type === '-') return new Error(line)
    if (type === '$') {
      const len = Number(line)
      if (len === -1) return null
      if (this.pos + len > this.buf.length) throw new Error('needmore')
      const data = this.buf.subarray(this.pos, this.pos + len).toString('utf8')
      this.pos += len + 2
      return data
    }
    return line
  }
  line() {
    const end = this.buf.indexOf('\r\n', this.pos)
    if (end < 0) throw new Error('needmore')
    const s = this.buf.subarray(this.pos, end).toString('utf8')
    this.pos = end + 2
    return s
  }
}
function cmd(...parts) {
  return new Promise(res => {
    pending.push(v => res(v instanceof Error ? v : v))
    client.write(cmdBuf(...parts))
  })
}
client.on('connect', async () => {
  try {
    console.log('SELECT 0 ->', await cmd('SELECT', '0'))
    console.log('DBSIZE(0):', await cmd('DBSIZE'))
    const keys0 = await cmd('KEYS', 'sc:*')
    console.log('sc:* count:', keys0.length)
    console.log('sample:', JSON.stringify((keys0||[]).slice(0, 12)))
    console.log('---')
    console.log('SELECT 1 ->', await cmd('SELECT', '1'))
    console.log('DBSIZE(1):', await cmd('DBSIZE'))
    const keys1 = await cmd('KEYS', 'sc:*')
    console.log('sc:* count(db1):', keys1.length)
    console.log('sample:', JSON.stringify((keys1||[]).slice(0, 12)))
  } catch (e) {
    console.log('ERR', e.message)
  }
  client.destroy()
})