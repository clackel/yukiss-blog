import { describe, expect, it } from 'vitest'
import { markdownExcerpt, renderMarkdown } from './markdown'

describe('Markdown rendering', () => {
  it('renders common Markdown while blocking raw HTML', () => {
    const html = renderMarkdown('# 标题\n\n<script>alert(1)</script>\n\n**正文**')

    expect(html).toContain('<h1>标题</h1>')
    expect(html).toContain('<strong>正文</strong>')
    expect(html).not.toContain('<script>')
  })

  it('renders external HTTP images and site uploads only', () => {
    const external = renderMarkdown('![远程图片](https://example.com/image.png)')
    const upload = renderMarkdown('![上传图片](/uploads/0b1c-image.png)')
    const otherRelative = renderMarkdown('![本地图片](/etc/image.png)')
    const traversal = renderMarkdown('![越界](/uploads/../secret.png)')

    expect(external).toContain('https://example.com/image.png')
    expect(external).toContain('loading="lazy"')
    expect(upload).toContain('src="http://localhost:4000/uploads/0b1c-image.png"')
    expect(otherRelative).not.toContain('<img')
    expect(traversal).not.toContain('<img')
  })

  it('creates a compact plain-text excerpt', () => {
    expect(markdownExcerpt('## 标题\n\n这是 **正文** 和 [链接](https://example.com)。', 12)).toBe(
      '标题 这是 正文 和 链…'
    )
  })
})
