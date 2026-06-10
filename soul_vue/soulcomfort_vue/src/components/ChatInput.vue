<template>
  <div class="chat-input-area">
    <div v-if="quoteMsg" class="quote-banner">
      <div class="quote-banner-content">
        <span class="quote-banner-label">回复</span>
        <span class="quote-banner-text">{{ truncateText(quoteMsg.content) }}</span>
      </div>
      <el-button :icon="Close" circle text size="small" @click="$emit('cancelQuote')" />
    </div>
    <div class="input-wrapper">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="和甜弈说说心里话吧..."
        @keydown.enter.exact.prevent="handleSend"
        :disabled="disabled"
        resize="none"
        class="message-input"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        circle
        :disabled="!inputText.trim() || disabled"
        @click="handleSend"
        class="send-button"
      />
    </div>
    <div class="input-hint">按 Enter 发送，Shift + Enter 换行</div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Promotion, Close } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  quoteMsg: { type: Object, default: null }
})

const emit = defineEmits(['send', 'cancelQuote'])

const inputText = ref('')

function truncateText(text) {
  if (!text) return ''
  return text.length > 50 ? text.substring(0, 50) + '...' : text
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}

watch(() => props.quoteMsg, (v) => {
  if (v) {
    nextTick(() => {
      const ta = document.querySelector('.message-input textarea')
      if (ta) ta.focus()
    })
  }
})
</script>

<style scoped>
.chat-input-area {
  padding: 0 24px 24px;
  background: transparent;
}

.quote-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  margin-bottom: 8px;
  background: var(--bg-quote);
  border-left: 3px solid var(--accent-color);
  border-radius: 8px;
}

.quote-banner-content {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.quote-banner-label {
  font-size: 12px;
  color: var(--accent-color);
  font-weight: 500;
  flex-shrink: 0;
}

.quote-banner-text {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.message-input :deep(.el-textarea__inner) {
  border-radius: 24px;
  padding: 12px 20px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.3s;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: var(--accent-color);
  background: var(--bg-card);
  box-shadow: 0 0 0 3px rgba(212, 163, 115, 0.12);
}

.send-button {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  background: var(--accent-gradient);
  border: none;
}

.send-button:hover {
  background: var(--accent-gradient-hover);
}

.send-button:active {
  background: var(--accent-gradient-disabled);
}

.input-hint {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 8px;
  text-align: center;
}
</style>