import { describe, expect, it } from 'vitest'
import { markdownExcerpt, renderMarkdown } from './markdown'

describe('Markdown rendering', () => {
  it('renders common Markdown while blocking raw HTML', () => {
    const html = renderMarkdown('# 标题\n\n<script>alert(1)</script>\n\n**正文**')

    expect(html).toContain('<h1>标题</h1>')
    expect(html).toContain('<strong>正文</strong>')
    expect(html).not.toContain('<script>')
  })

  it('only renders external HTTP images', () => {
    const external = renderMarkdown('![远程图片](https://example.com/image.png)')
    const relative = renderMarkdown('![本地图片](/uploads/image.png)')

    expect(external).toContain('https://example.com/image.png')
    expect(external).toContain('loading="lazy"')
    expect(relative).not.toContain('<img')
  })

  it('creates a compact plain-text excerpt', () => {
    expect(markdownExcerpt('## 标题\n\n这是 **正文** 和 [链接](https://example.com)。', 12))
      .toBe('标题 这是 正文 和 链…')
  })
})
