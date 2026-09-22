<template>
  <div class="message-bubble" :class="[role, { care: kind === 'care' }]">
    <div class="avatar-area">
      <!-- 甜弈头像：从背景图裁剪人脸（人物位于画面 50% 48%），点击放大预览 -->
      <div v-if="role === 'assistant'" class="tianyi-avatar" title="点击查看大头像" @click="showPreview = true"></div>
      <!-- 用户头像：上传过则显示 OSS 图片，否则显示默认图标；点击放大预览 -->
      <el-avatar
        v-else
        :size="40"
        :src="userAvatar || undefined"
        :style="{ backgroundColor: avatarColor, cursor: 'pointer' }"
        @click="showPreview = true"
      >
        <el-icon :size="22">
          <UserFilled />
        </el-icon>
      </el-avatar>
    </div>
    <div class="content-area">
      <div class="sender-name">
        {{ role === 'user' ? userName : '甜弈' }}
        <span v-if="kind === 'care'" class="care-badge"> 甜♥弈 </span>
      </div>
      <div class="bubble-row">
        <div class="bubble-text" :class="{ 'bubble-text--streaming': role === 'assistant' && isStreaming }">
          <span v-html="renderedContent"></span>
          <div v-if="role === 'assistant' && isStreaming && !content" class="thinking-placeholder">
            <span class="thinking-text">努力思考中</span>
            <span class="thinking-dots"><i></i><i></i><i></i></span>
          </div>
          <div v-if="role === 'assistant' && isStreaming && content" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
        <el-button
          v-if="!isStreaming && kind !== 'care'"
          :icon="ChatLineSquare"
          circle
          text
          size="small"
          class="quote-btn"
          @click="$emit('quote', { role, content, index })"
          title="引用回复"
        />
      </div>
    </div>

    <!-- 头像放大预览浮层 -->
    <Teleport to="body">
      <div v-if="showPreview" class="avatar-preview-mask" @click.self="showPreview = false">
        <div class="avatar-preview-card">
          <div v-if="role === 'assistant'" class="tianyi-avatar--lg"></div>
          <div v-else class="avatar-preview-box">
            <img v-if="userAvatar" :src="userAvatar" alt="用户头像" />
            <el-icon v-else :size="64"><UserFilled /></el-icon>
          </div>
          <span class="avatar-preview-name">{{ role === 'assistant' ? '甜弈' : userName }}</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ChatLineSquare, UserFilled } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'

defineEmits(['quote'])

const showPreview = ref(false)

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: (v) => ['user', 'assistant'].includes(v)
  },
  content: {
    type: String,
    default: ''
  },
  isStreaming: {
    type: Boolean,
    default: false
  },
  userName: {
    type: String,
    default: '我'
  },
  userAvatar: {
    type: String,
    default: ''
  },
  index: {
    type: Number,
    default: -1
  },
  kind: {
    type: String,
    default: ''
  }
})

const avatarColor = computed(() => {
  return '#e8a9c1'
})

/* Markdown 渲染：html:false 天然转义原始 HTML（防 XSS），breaks 保留单换行行为 */
const md = new MarkdownIt({ html: false, breaks: true, linkify: false })

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const renderedContent = computed(() => {
  const raw = props.content || ''
  // 流式中代码围栏可能未闭合：只需把未闭合的尾段（最后一个 ``` 之后）按纯文本转义，
  // 前半段照常走 markdown——保证流式渲染与历史加载完全一致，
  // 之前"奇数个```时整条退化为纯文本"会让 **、列表等 markdown 源码在流式中露出。
  const ticks = (raw.match(/```/g) || []).length
  if (props.isStreaming && ticks % 2 === 1) {
    const lastIdx = raw.lastIndexOf('```')
    const head = raw.slice(0, lastIdx)
    const tail = raw.slice(lastIdx)
    return md.render(head) + escapeHtml(tail).replace(/\n/g, '<br>')
  }
  return md.render(raw)
})
</script>

<style scoped>
.message-bubble {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  animation: fadeInUp 0.3s ease;
}

.message-bubble.user {
  flex-direction: row-reverse;
}

.avatar-area {
  flex-shrink: 0;
}

/* 甜弈头像：从背景图裁剪人物（头部+肩部，人物位于图中 50% 48%），放大显示、圆形裁切 + 浅蓝柔光圈 */
.tianyi-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-image: url('/image/bankground.png');
  background-size: 265% auto;
  background-position: 50% 48%;
  background-repeat: no-repeat;
  border: 2px solid #d3e6ee;
  box-shadow: 0 0 0 3px rgba(211,230,238, 0.35), var(--shadow);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.tianyi-avatar:hover {
  transform: scale(1.06);
}

/* ===== 头像放大预览浮层 ===== */
.avatar-preview-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

.avatar-preview-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 32px 20px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  animation: previewPop 0.25s cubic-bezier(0.34, 1.45, 0.5, 1);
}

html.dark .avatar-preview-card {
  background: #2b2230;
}

/* 甜弈大头像：与消息缩略头像同比例放大裁切 */
.tianyi-avatar--lg {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background-image: url('/image/bankground.png');
  background-size: 265% auto;
  background-position: 50% 48%;
  background-repeat: no-repeat;
  border: 3px solid #d3e6ee;
  box-shadow: 0 0 0 4px rgba(211,230,238, 0.5);
}

.avatar-preview-box {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0e8ee;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b8a8b2;
  border: 3px solid var(--border-color);
}

.avatar-preview-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary, #333);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes previewPop {
  from { opacity: 0; transform: scale(0.85) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.content-area {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-bubble.user .content-area {
  align-items: flex-end;
}

.message-bubble.assistant .content-area {
  align-items: flex-start;
}

.sender-name {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.message-bubble.user .bubble-row {
  flex-direction: row-reverse;
}

.bubble-text {
  padding: 12px 18px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
  white-space: normal;
  position: relative;
  min-height: 20px;
}

/* ===== Markdown 内容排版（内容经 markdown-it 渲染，样式需 :deep 穿透） ===== */
.bubble-text :deep(p) {
  margin: 6px 0;
}
.bubble-text :deep(p:first-child) {
  margin-top: 0;
}
.bubble-text :deep(p:last-child) {
  margin-bottom: 0;
}
.bubble-text :deep(strong) {
  color: var(--accent-color);
  font-weight: 600;
}
.bubble-text :deep(em) {
  color: var(--accent-color);
}
.bubble-text :deep(ul),
.bubble-text :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;
}
.bubble-text :deep(li) {
  margin: 3px 0;
}
.bubble-text :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid var(--accent-color);
  color: var(--text-muted);
  background: var(--bg-quote);
  border-radius: 0 8px 8px 0;
}
.bubble-text :deep(h1),
.bubble-text :deep(h2),
.bubble-text :deep(h3),
.bubble-text :deep(h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 10px 0 4px;
  color: var(--text-primary);
}
.bubble-text :deep(code) {
  font-family: 'SF Mono', Consolas, 'Courier New', monospace;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 6px;
  border-radius: 5px;
}
.bubble-text :deep(pre) {
  background: rgba(0, 0, 0, 0.07);
  border-radius: 10px;
  padding: 10px 14px;
  overflow-x: auto;
  margin: 8px 0;
}
.bubble-text :deep(pre code) {
  background: none;
  padding: 0;
}
.bubble-text :deep(a) {
  color: var(--accent-color);
  text-decoration: underline;
  word-break: break-all;
}
.bubble-text :deep(hr) {
  border: none;
  border-top: 1px dashed rgba(0, 0, 0, 0.12);
  margin: 10px 0;
}

.bubble-text--streaming {
  padding-right: 50px;
}

.message-bubble.user .bubble-text {
  background: var(--bg-bubble-user);
  color: var(--text-primary);
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant .bubble-text {
  background: var(--bg-bubble-assistant);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow);
}

/* 回访关怀（甜弈先开口）：月光蓝左侧竖条 + 冷调气泡底，与普通回复区分 */
.message-bubble.care .bubble-text {
  padding-left: 24px;
  background: linear-gradient(135deg, #eef5f9, #e3edf4);
}

html.dark .message-bubble.care .bubble-text {
  background: linear-gradient(135deg, #2a3140, #303c4e);
}

.message-bubble.care .bubble-text::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #4f8faa, #6f7fae);
}

.care-badge {
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: linear-gradient(135deg, #4f8faa, #6f7fae);
}

.quote-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  color: var(--text-muted);
}

.message-bubble:hover .quote-btn {
  opacity: 1;
}

.quote-btn:hover {
  color: var(--accent-color);
  background: var(--bg-quote);
}

/* 等待首token/哨兵重答间隙：气泡内显示"努力思考中"+跳动三点，避免空荡气泡 */
.thinking-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.thinking-text {
  font-size: 13px;
  animation: thinkingBreath 2s ease-in-out infinite;
}

.thinking-dots {
  display: inline-flex;
  gap: 4px;
}

.thinking-dots i {
  width: 5px;
  height: 5px;
  background: var(--accent-color);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinkingBreath {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 0;
  position: absolute;
  right: 14px;
  bottom: 10px;
  align-items: center;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: var(--accent-color);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(1); }
  30% { opacity: 1; transform: scale(1.2); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>