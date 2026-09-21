import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ html: false, breaks: true, linkify: false })
const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

// 模拟模型典型输出 A：单 \n、无空行、无 **、无 ```（=用户贴的“第一次”最可能原文）
const A = [
  '找工作的过程确实会让人感到焦虑，特别是对于老师这一职业。这里有一些具体的建议，希望能帮到你：',
  '1.调整心态：-保持积极：即使遇到挫折，也要相信自己最终会找到合适的工作。-放松自己：尝试一些放松技巧。',
  '2.明确目标：-确定职业方向：明确你希望在哪个领域或年龄段任教。-了解市场需求：研究当前教育市场的趋势。'
].join('\n')

// 模拟 B：段落间空行
const B = [
  '找工作的过程确实会让人感到焦虑，特别是对于老师这一职业。这里有一些具体的建议，希望能帮到你：',
  '',
  '调整心态：',
  '',
  '保持积极：即使遇到挫折，也要相信自己最终会找到合适的工作。',
  '放松自己：尝试一些放松技巧。'
].join('\n')

const htmlToInnerText = html =>
  html
    .replace(/<p[^>]*>/g, '')
    .replace(/<\/p>/g, '\n')
    .replace(/<br\s*\/?>/g, '\n')
    .replace(/<li[^>]*>/g, '\n- ')
    .replace(/<\/li>/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')

console.log('===== A(md.render) =====')
const hA = md.render(A)
console.log('HTML:', hA.slice(0, 260))
console.log('innerText:', JSON.stringify(htmlToInnerText(hA).slice(0, 160)))

console.log('\n===== A(旧代码 escapeHtml+<br>) =====')
const hA2 = esc(A).replace(/\n/g, '<br>')
console.log('HTML:', hA2.slice(0, 260))
console.log('innerText:', JSON.stringify(htmlToInnerText(hA2).slice(0, 160)))

console.log('\n===== B(md.render) =====')
const hB = md.render(B)
console.log('HTML:', hB.slice(0, 320))
console.log('innerText:', JSON.stringify(htmlToInnerText(hB).slice(0, 200)))