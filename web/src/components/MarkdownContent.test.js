import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MarkdownContent from './MarkdownContent.vue'

describe('MarkdownContent', () => {
  it('renders a sanitized article body', () => {
    const wrapper = mount(MarkdownContent, {
      props: {
        source: '## 小标题\n\n正文<script>alert(1)</script>',
      },
    })

    expect(wrapper.find('h2').text()).toBe('小标题')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.classes()).toContain('markdown-body')
  })
})
