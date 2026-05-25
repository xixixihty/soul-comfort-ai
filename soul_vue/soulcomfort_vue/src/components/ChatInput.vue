<template>
  <div class="chat-input-area">
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
import { ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['send'])

const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-input-area {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0e6dc;
  background: #fff;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.message-input :deep(.el-textarea__inner) {
  border-radius: 24px;
  padding: 12px 20px;
  background: #faf7f2;
  border: 1px solid #f0e6dc;
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.3s;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: #d4a373;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(212, 163, 115, 0.12);
}

.send-button {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #fa9e6c, #d4a373);
  border: none;
}

.send-button:hover {
  background: linear-gradient(135deg, #f08a4a, #c98d5a);
}

.send-button:active {
  background: linear-gradient(135deg, #e07a3a, #b87d4a);
}

.input-hint {
  font-size: 11px;
  color: #bbb;
  margin-top: 8px;
  text-align: center;
}
</style>