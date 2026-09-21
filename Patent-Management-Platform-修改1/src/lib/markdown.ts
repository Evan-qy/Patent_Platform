import DOMPurify from 'dompurify'
import { Marked } from 'marked'

const marked = new Marked({
  breaks: true,
  gfm: true
})

marked.use({
  renderer: {
    link(token) {
      const href = typeof token.href === 'string' ? token.href : ''
      const title = token.title ? ` title="${token.title}"` : ''
      const text = token.text || href
      const safeHref = href || '#'
      return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer nofollow"${title}>${text}</a>`
    }
  }
})

export const renderMarkdown = (value?: string | null) => {
  const source = value || ''
  const html = marked.parse(source)
  return DOMPurify.sanitize(String(html))
}
