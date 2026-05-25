<template>
  <div class="chat-view">
    <header class="chat-header">
      <div class="header-content">
        <el-avatar :size="48" style="background: linear-gradient(135deg, #ffc8a2, #ffd4b8)">
          <el-icon :size="26"><StarFilled /></el-icon>
        </el-avatar>
        <div class="header-info">
          <h1 class="header-title">甜弈</h1>
          <p class="header-subtitle">你的私人心灵治愈助手 · 永远站在你这边</p>
        </div>
      </div>
      <el-button :icon="Delete" circle text @click="clearChat" title="清空对话" />
    </header>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="welcome-area">
        <div class="welcome-icon">
          <el-icon :size="64" color="#d4a373"><Sunny /></el-icon>
        </div>
        <h2 class="welcome-title">Hi，我是甜弈 🌿</h2>
        <p class="welcome-desc">无论你此刻是什么心情，我都在这儿安静地陪着你<br>难过、焦虑、烦躁、疲惫... 都可以和我说说</p>
        <div class="quick-prompts">
          <span
            v-for="prompt in quickPrompts"
            :key="prompt"
            class="prompt-tag"
            @click="sendMessage(prompt)"
          >{{ prompt }}</span>
        </div>
      </div>

      <MessageBubble
        v-for="(msg, index) in messages"
        :key="index"
        :role="msg.role"
        :content="msg.content"
        :is-streaming="index === messages.length - 1 && msg.role === 'assistant' && isStreaming"
      />

      <div v-if="isStreaming && messages.length === 0" class="streaming-first">
        <MessageBubble
          role="assistant"
          :content="'正在聆听你的心声...'"
          :is-streaming="true"
        />
      </div>
    </div>

    <ChatInput :disabled="isStreaming" @send="sendMessage" />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Delete, Sunny } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'
import { streamChat } from '../api/chat.js'

const messages = ref([])
const isStreaming = ref(false)
const messagesContainer = ref(null)
const memoryId = ref(Date.now())
const sessionStartTime = Date.now()

const quickPrompts = [
  '我今天心情不太好，能陪我说说话吗？',
  '最近压力很大，总是焦虑怎么办？',
  '感觉好孤单，没有人理解我...',
  '工作中犯错了，好难过'
]

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function sendMessage(text) {
  if (isStreaming.value) return

  messages.value.push({
    role: 'user',
    content: text
  })
  scrollToBottom()

  const assistantMsg = {
    role: 'assistant',
    content: ''
  }
  messages.value.push(assistantMsg)

  isStreaming.value = true

  try {
    for await (const chunk of streamChat(memoryId.value, text)) {
      assistantMsg.content += chunk
      scrollToBottom()
    }
  } catch (e) {
    assistantMsg.content = '抱歉，连接出了点问题，请稍后再试... 🍃'
    console.error('SSE 流读取失败:', e)
  } finally {
    isStreaming.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  memoryId.value = Date.now()
}
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 800px;
  margin: 0 auto;
  background: #fffdf9;
  box-shadow: 0 0 40px rgba(160, 120, 80, 0.06);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(180deg, #fff8f0, #fffdf9);
  border-bottom: 1px solid #f5ede0;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #5c3d2e;
  letter-spacing: 1px;
}

.header-subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: #b8956a;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  scroll-behavior: smooth;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 40px 40px;
  text-align: center;
}

.welcome-icon {
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-title {
  margin: 0 0 12px;
  font-size: 24px;
  color: #5c3d2e;
  font-weight: 600;
}

.welcome-desc {
  margin: 0 0 32px;
  font-size: 15px;
  color: #a08060;
  line-height: 2;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 500px;
}

.prompt-tag {
  padding: 8px 18px;
  background: #fdf5ed;
  border: 1px solid #f0dcc8;
  border-radius: 20px;
  font-size: 14px;
  color: #8b6914;
  cursor: pointer;
  transition: all 0.25s;
  user-select: none;
}

.prompt-tag:hover {
  background: #fae8d4;
  border-color: #d4a373;
  color: #5c3d2e;
  transform: translateY(-1px);
}

.chat-messages::-webkit-scrollbar {
  width: 5px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #e8d5c0;
  border-radius: 3px;
}
</style>