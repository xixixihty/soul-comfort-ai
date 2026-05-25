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
      <div class="sender-name">{{ role === 'user' ? '我' : '甜弈' }}</div>
      <div class="bubble-text" v-html="renderedContent"></div>
      <div v-if="role === 'assistant' && isStreaming" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

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
  color: #999;
  margin-bottom: 4px;
}

.bubble-text {
  padding: 12px 18px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-bubble.user .bubble-text {
  background: linear-gradient(135deg, #a0c4ff, #bdd4ff);
  color: #333;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant .bubble-text {
  background: linear-gradient(135deg, #fff8f0, #ffecd2);
  color: #4a3728;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(180, 120, 80, 0.08);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 12px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #d4a373;
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