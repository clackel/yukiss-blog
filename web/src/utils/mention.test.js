import { describe, expect, it } from 'vitest'
import { mentionCandidates, splitMentions } from './mention'

describe('Mention helpers', () => {
  const alice = { id: 5, username: 'alice', nickname: '爱丽丝' }

  it('turns confirmed mentions into link segments and keeps the rest as text', () => {
    expect(splitMentions('你好 @alice，再见 @nobody', [alice])).toEqual([
      { type: 'text', text: '你好 ' },
      { type: 'mention', text: '@alice', user: alice },
      { type: 'text', text: '，再见 @nobody' },
    ])
  })

  it('does not treat email addresses as mentions', () => {
    expect(splitMentions('mail@alice.com', [alice])).toEqual([{ type: 'text', text: 'mail@alice.com' }])
  })

  it('returns plain text when nobody was mentioned', () => {
    expect(splitMentions('普通评论', [])).toEqual([{ type: 'text', text: '普通评论' }])
    expect(splitMentions('', [])).toEqual([])
  })

  it('builds unique autocomplete candidates without the current user', () => {
    expect(
      mentionCandidates(
        [
          alice,
          { username: 'me', nickname: '我' },
          alice,
          { username: null, nickname: '已注销用户' },
          { username: 'bob' },
        ],
        'me'
      )
    ).toEqual([
      { value: 'alice', label: '爱丽丝' },
      { value: 'bob', label: 'bob' },
    ])
  })
})
