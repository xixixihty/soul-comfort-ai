<template>
  <div class="chat-view">
    <header class="chat-header">
      <div class="header-left">
        <div class="conv-selector">
          <el-select
            v-model="currentConvId"
            placeholder="选择对话"
            class="conv-select"
            @change="switchConversation"
            popper-class="conv-popper"
          >
            <el-option
              v-for="conv in conversations"
              :key="conv.id"
              :label="conv.title"
              :value="conv.id"
            />
          </el-select>

          <el-button
            :icon="Plus"
            circle
            size="small"
            class="new-conv-btn"
            @click="createNewConversation"
            title="新建对话"
          />
        </div>
      </div>

      <el-button
        :icon="Delete"
        circle
        text
        @click="deleteCurrentConversation"
        :disabled="!currentConvId"
        title="删除当前对话"
      />
    </header>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0 && !isStreaming" class="welcome-area">
        <div class="welcome-icon">
          <el-icon :size="64" color="#d4a373"><Sunny /></el-icon>
        </div>
        <h2 class="welcome-title">Hi，我是甜弈 🌿</h2>
        <p class="welcome-desc">
          无论你此刻是什么心情，我都在这儿安静地陪着你<br />
          难过、焦虑、烦躁、疲惫... 都可以和我说说
        </p>
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
    </div>

    <ChatInput :disabled="isStreaming || !currentConvId" @send="sendMessage" />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'
import { streamChat } from '../api/chat.js'
import { fetchConversationList, fetchConversation, createConversation, deleteConversation } from '../api/conversation.js'

const messages = ref([])
const isStreaming = ref(false)
const messagesContainer = ref(null)
const currentConvId = ref('')
const conversations = ref([])

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

async function loadConversations() {
  try {
    const res = await fetchConversationList(1, 50)
    if (res.code === 0 && res.data) {
      conversations.value = res.data.records || []
    }
  } catch (e) {
    console.error('加载对话列表失败:', e)
  }
}

async function createNewConversation() {
  try {
    const res = await createConversation('新对话')
    if (res.code === 0 && res.data) {
      conversations.value.unshift(res.data)
      currentConvId.value = res.data.id
      messages.value = []
      scrollToBottom()
    }
  } catch (e) {
    ElMessage.error('创建对话失败')
    console.error(e)
  }
}

async function switchConversation(convId) {
  if (!convId) return
  try {
    const res = await fetchConversation(convId)
    if (res.code === 0 && res.data) {
      const conv = res.data
      messages.value = (conv.messages || []).map(m => ({
        role: m.role === 'USER' ? 'user' : 'assistant',
        content: m.content
      }))
      scrollToBottom()
    }
  } catch (e) {
    ElMessage.error('加载对话失败')
    console.error(e)
  }
}

async function deleteCurrentConversation() {
  if (!currentConvId.value) return
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？删除后不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteConversation(currentConvId.value)
    conversations.value = conversations.value.filter(c => c.id !== currentConvId.value)
    currentConvId.value = ''
    messages.value = []
    ElMessage.success('对话已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function sendMessage(text) {
  if (isStreaming.value || !currentConvId.value) return

  if (!currentConvId.value) {
    await createNewConversation()
    if (!currentConvId.value) return
  }

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
    for await (const chunk of streamChat(currentConvId.value, text)) {
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

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length > 0) {
    currentConvId.value = conversations.value[0].id
    await switchConversation(currentConvId.value)
  }
})
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
  padding: 12px 24px;
  background: linear-gradient(180deg, #fff8f0, #fffdf9);
  border-bottom: 1px solid #f5ede0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.conv-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-select {
  width: 220px;
}

.new-conv-btn {
  background: linear-gradient(135deg, #fa9e6c, #d4a373);
  border: none;
  color: #fff;
}

.new-conv-btn:hover {
  background: linear-gradient(135deg, #f08a4a, #c98d5a);
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
  color: #b8956a;
  line-height: 1.8;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.prompt-tag {
  padding: 8px 18px;
  border-radius: 20px;
  background: #faf0e2;
  color: #8b6b5a;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-tag:hover {
  background: #f0dcc5;
  color: #5c3d2e;
}
</style>