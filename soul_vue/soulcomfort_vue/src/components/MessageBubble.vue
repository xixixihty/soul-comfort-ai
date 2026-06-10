<template>
  <div class="message-bubble" :class="[role]">
    <div class="avatar-area">
      <el-avatar :size="40" :style="{ backgroundColor: avatarColor }">
        <el-icon :size="22">
          <UserFilled v-if="role === 'user'" />
          <StarFilled v-else />
        </el-icon>
      </el-avatar>
    </div>
    <div class="content-area">
      <div class="sender-name">{{ role === 'user' ? userName : '甜弈' }}</div>
      <div class="bubble-row">
        <div class="bubble-text" :class="{ 'bubble-text--streaming': role === 'assistant' && isStreaming }">
          <span v-html="renderedContent"></span>
          <div v-if="role === 'assistant' && isStreaming" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
        <el-button
          v-if="!isStreaming"
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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ChatLineSquare, UserFilled, StarFilled } from '@element-plus/icons-vue'

defineEmits(['quote'])

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
  index: {
    type: Number,
    default: -1
  }
})

const avatarColor = computed(() => {
  return props.role === 'user' ? '#a0c4ff' : '#ffc8a2'
})

const renderedContent = computed(() => {
  return props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
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
  white-space: pre-wrap;
  position: relative;
  min-height: 20px;
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