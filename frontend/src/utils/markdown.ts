import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/**
 * 站内唯一 Markdown 渲染管线（批次 6 自 ChatMessage 提取共用）：
 * markdown-it(html:false) + DOMPurify 消毒，防提示注入——AI 客服气泡与知识库预览共用同一管线，
 * 渲染口径变化只改这一处。
 */
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

export function renderMarkdown(source: string): string {
  return DOMPurify.sanitize(md.render(source))
}
