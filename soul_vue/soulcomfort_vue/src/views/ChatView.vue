<template>
  <div class="chat-view">
    <aside class="conv-panel" :class="{ 'conv-panel--collapsed': panelCollapsed }">
      <div class="panel-header">
        <span class="panel-title">对话列表</span>
        <el-button
          :icon="Fold"
          circle
          text
          size="small"
          @click="panelCollapsed = true"
          title="收起面板"
        />
        <el-button
          :icon="Plus"
          size="small"
          type="primary"
          class="new-btn"
          @click="createNewConversation"
        >新建</el-button>
      </div>

      <div class="panel-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索对话..."
          :prefix-icon="Search"
          size="small"
          clearable
        />
      </div>

      <div class="conv-items" v-loading="convLoading">
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conv-item"
          :class="{ 'conv-item--active': currentConvId === conv.id }"
          @click="switchConversation(conv.id)"
        >
          <div class="conv-item-main">
            <span v-if="conv.editing" class="conv-item-editing" @click.stop>
              <el-input
                v-model="conv.editTitle"
                size="small"
                @keyup.enter="saveRename(conv)"
                @blur="saveRename(conv)"
              />
            </span>
            <template v-else>
              <span class="conv-item-title">{{ conv.title }}</span>
              <span class="conv-item-date">{{ formatConvDate(conv.updatedAt) }}</span>
            </template>
          </div>
          <div class="conv-item-actions" @click.stop v-show="!conv.editing">
            <el-button
              :icon="Edit"
              circle
              text
              size="small"
              @click="startRename(conv)"
              title="重命名"
            />
            <el-button
              :icon="Delete"
              circle
              text
              size="small"
              @click="handleDeleteConv(conv)"
              title="删除"
            />
          </div>
        </div>

        <div v-if="filteredConversations.length === 0 && !convLoading" class="panel-empty">
          <span v-if="searchKeyword">无匹配的对话</span>
          <span v-else>暂无对话，点击上方新建</span>
        </div>
      </div>

      <div class="panel-pagination" v-if="totalConvs > convPageSize">
        <el-pagination
          v-model:current-page="convPage"
          :page-size="convPageSize"
          :total="totalConvs"
          layout="prev, pager, next"
          small
          @current-change="loadConversations"
        />
      </div>
    </aside>

    <section class="chat-area">
      <header class="chat-header">
        <el-button
          v-if="panelCollapsed"
          :icon="Expand"
          circle
          text
          size="small"
          class="expand-btn"
          @click="panelCollapsed = false"
          title="展开对话列表"
        />
        <!-- 收起对话列表且未选中对话时，隐藏"选择一个对话开始聊天"占位（列表已隐藏，提示无意义） -->
        <span
          v-if="currentConvTitle || !panelCollapsed"
          class="current-title"
        >{{ currentConvTitle || '选择一个对话开始聊天' }}</span>
        <el-button
          v-if="currentConvId"
          :icon="Delete"
          circle
          text
          @click="deleteCurrentConversation"
          title="删除当前对话"
        />
      </header>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0 && !isStreaming" class="welcome-area">
          <!-- 未打卡提醒：欢迎页在场时内嵌在小太阳上方（内容流形态，零遮挡），
               其余场景由 CheckinDialog 的顶部悬浮胶囊兜底 -->
          <CheckinPill mode="inline" />
          <div class="welcome-icon">
            <el-icon :size="64" color="#4f8faa"><Sunny /></el-icon>
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
          :user-name="authStore.nickname"
          :user-avatar="authStore.avatarUrl"
          :is-streaming="index === messages.length - 1 && msg.role === 'assistant' && isStreaming"
          :kind="msg.kind || ''"
          :index="index"
          @quote="handleQuote"
        />
      </div>

      <ChatInput
        :disabled="isStreaming"
        :quote-msg="quoteMsg"
        @send="sendMessage"
        @cancel-quote="quoteMsg = null"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Delete, Edit, Search, Sunny, Fold, Expand } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'
import CheckinPill from '../components/CheckinPill.vue'
import { streamChat } from '../api/chat.js'
import { applyBgByPanel, applyBgNeutral } from '../composables/useBgLayer.js'
import { useCheckinReminder } from '../composables/useCheckinReminder.js'
import { useCareReminder } from '../composables/useCareReminder.js'
import {
  fetchConversationList,
  fetchConversation,
  createConversation,
  renameConversation,
  deleteConversation
} from '../api/conversation.js'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const messages = ref([])
const isStreaming = ref(false)
const messagesContainer = ref(null)
const currentConvId = ref('')
const conversations = ref([])
const searchKeyword = ref('')
const convLoading = ref(false)
const convPage = ref(1)
const convPageSize = 20
const totalConvs = ref(0)
const panelCollapsed = ref(false)
const quoteMsg = ref(null)

/* 打卡提醒胶囊接管标志：欢迎页（空会话）可见时内嵌胶囊上场，顶部悬浮胶囊让位，避免双份同屏 */
const { inlineActive } = useCheckinReminder()
const welcomeVisible = computed(() => messages.value.length === 0 && !isStreaming.value)
watch(welcomeVisible, v => { inlineActive.value = v }, { immediate: true })

/* 主动关怀：把当前会话登记给关怀胶囊（点开时"甜弈先开口"优先落进正在看的会话） */
const { activeConvId, pendingCareMessage } = useCareReminder()
watch(currentConvId, v => { activeConvId.value = v }, { immediate: true })
onUnmounted(() => { activeConvId.value = '' })

/* opener 落点就是当前会话时路由不会变化，本地直接补一条 care 消息；
   落在其他会话时由导航 + 历史加载呈现，这里消费掉即可 */
watch(pendingCareMessage, data => {
  if (!data) return
  pendingCareMessage.value = null
  if (data.convId !== currentConvId.value) return
  messages.value.push({
    role: data.message.role === 'USER' ? 'user' : 'assistant',
    content: data.message.content,
    kind: data.message.kind || 'care'
  })
  scrollToBottom()
})

/* 背景联动（算法见 composables/useBgLayer.js）：展开面板→背景收拢进聊天区；折叠→放大填满主内容区。
   离开聊天页时置为中性布局（填满主内容区），避免把聊天收拢态残留到其他模块 */
function onWindowResize() {
  applyBgByPanel(panelCollapsed.value)
}

watch(panelCollapsed, applyBgByPanel)
onMounted(() => {
  window.addEventListener('resize', onWindowResize)
  // 首屏即应用当前面板态，避免初始闪动
  applyBgByPanel(panelCollapsed.value)
})
onUnmounted(() => {
  window.removeEventListener('resize', onWindowResize)
  applyBgNeutral()
  // 离开聊天页：交还提醒胶囊展示权，日记/记录等页面由顶部悬浮兜底形态接手
  inlineActive.value = false
})

const quickPrompts = [
  '我今天心情不太好，能陪我说说话吗？',
  '最近压力很大，总是焦虑怎么办？',
  '感觉好孤单，没有人理解我...',
  '工作中犯错了，好难过'
]

const filteredConversations = computed(() => {
  if (!searchKeyword.value.trim()) {
    return conversations.value
  }
  const kw = searchKeyword.value.trim().toLowerCase()
  return conversations.value.filter(c => c.title.toLowerCase().includes(kw))
})

const currentConvTitle = computed(() => {
  const conv = conversations.value.find(c => c.id === currentConvId.value)
  return conv ? conv.title : ''
})

function formatConvDate(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await fetchConversationList(convPage.value, convPageSize)
    if (res.code === 0 && res.data) {
      conversations.value = (res.data.records || []).map(c => ({
        ...c,
        editing: false,
        editTitle: c.title
      }))
      totalConvs.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载对话列表失败:', e)
  } finally {
    convLoading.value = false
  }
}

async function createNewConversation() {
  try {
    const res = await createConversation('新对话')
    if (res.code === 0 && res.data) {
      const newConv = { ...res.data, editing: false, editTitle: res.data.title }
      conversations.value.unshift(newConv)
      totalConvs.value++
      currentConvId.value = newConv.id
      messages.value = []
      searchKeyword.value = ''
      scrollToBottom()
    }
  } catch (e) {
    ElMessage.error('创建对话失败')
    console.error(e)
  }
}

async function switchConversation(convId) {
  if (!convId || convId === currentConvId.value) return

  messages.value = []
  const prevConvId = currentConvId.value
  currentConvId.value = convId

  try {
    const res = await fetchConversation(convId)
    if (res.code === 0 && res.data) {
      messages.value = (res.data.messages || []).map(m => ({
        role: m.role === 'USER' ? 'user' : 'assistant',
        content: m.content,
        kind: m.kind || ''
      }))
      scrollToBottom()
    }
  } catch (e) {
    currentConvId.value = prevConvId
    ElMessage.error('加载对话失败')
    console.error(e)
  }
}

function startRename(conv) {
  conversations.value.forEach(c => { c.editing = false })
  conv.editing = true
  conv.editTitle = conv.title
}

async function saveRename(conv) {
  conv.editing = false
  const newTitle = conv.editTitle?.trim()
  if (!newTitle || newTitle === conv.title) return

  try {
    const res = await renameConversation(conv.id, newTitle)
    if (res.code === 0) {
      conv.title = newTitle
    }
  } catch (e) {
    ElMessage.error('重命名失败')
    conv.editTitle = conv.title
    console.error(e)
  }
}

async function handleDeleteConv(conv) {
  try {
    await ElMessageBox.confirm(
      `确定删除对话「${conv.title}」吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteConversation(conv.id)
    conversations.value = conversations.value.filter(c => c.id !== conv.id)
    totalConvs.value--
    if (conv.id === currentConvId.value) {
      currentConvId.value = ''
      messages.value = []
    }
    ElMessage.success('对话已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(e)
    }
  }
}

async function deleteCurrentConversation() {
  if (!currentConvId.value) return
  const conv = conversations.value.find(c => c.id === currentConvId.value)
  await handleDeleteConv(conv || { id: currentConvId.value, title: '当前对话' })
}

function handleQuote({ role, content, index }) {
  quoteMsg.value = { role, content, index }
}

/**
 * 流结束后同步 AI 自动生成的会话标题到侧栏。
 * 后端在 SSE 关闭后还要调一次模型生成标题（约 1~3 秒），所以每 2 秒拉一次、最多重试 4 次；
 * 用户手动改过名（标题已非"新对话"）时直接覆盖为服务端最新值。
 */
async function syncConvTitle(convId, attempts = 4) {
  await new Promise(resolve => setTimeout(resolve, 2000))
  try {
    const res = await fetchConversation(convId)
    if (res.code === 0 && res.data) {
      const conv = conversations.value.find(c => c.id === convId)
      if (!conv) return
      if (res.data.title && res.data.title !== conv.title) {
        conv.title = res.data.title
        conv.editTitle = res.data.title
      } else if (conv.title === '新对话' && attempts > 1) {
        syncConvTitle(convId, attempts - 1)
      }
    }
  } catch (e) {
    console.error('同步会话标题失败:', e)
  }
}

async function sendMessage(text) {
  if (isStreaming.value) return

  if (!currentConvId.value) {
    try {
      const res = await createConversation('新对话')
      if (res.code === 0 && res.data) {
        const newConv = { ...res.data, editing: false, editTitle: res.data.title }
        conversations.value.unshift(newConv)
        totalConvs.value++
        currentConvId.value = newConv.id
        messages.value = []
      } else {
        ElMessage.error('创建对话失败')
        return
      }
    } catch (e) {
      ElMessage.error('创建对话失败')
      console.error(e)
      return
    }
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
  // 必须取数组里的响应式代理来改写：直接改上面 push 前的普通对象会绕过
  // Vue 的 set 拦截，流式期间不触发重渲染，导致回答整段一次性出现
  const liveMsg = messages.value[messages.value.length - 1]

  isStreaming.value = true

  const q = quoteMsg.value
  quoteMsg.value = null

  const convIdForTitle = currentConvId.value

  try {
    for await (const chunk of streamChat(currentConvId.value, text, q)) {
      if (chunk && chunk.reset) {
        // 后端风格哨兵判定回答滑向"助手腔"，已中断并重生成：清掉屏幕上的半截清单
        liveMsg.content = ''
        continue
      }
      liveMsg.content += chunk
      scrollToBottom()
    }
  } catch (e) {
    liveMsg.content = '抱歉，连接出了点问题，请稍后再试... 🍃'
    console.error('SSE 流读取失败:', e)
  } finally {
    isStreaming.value = false
    scrollToBottom()
    syncConvTitle(convIdForTitle)
  }
}

onMounted(async () => {
  await authStore.fetchUserInfo()
  await loadConversations()

  const targetConvId = route.query.convId
  if (targetConvId && conversations.value.some(c => c.id === targetConvId)) {
    await switchConversation(targetConvId)
  }
})

watch(() => route.query.convId, (newConvId) => {
  if (newConvId && newConvId !== currentConvId.value) {
    if (conversations.value.some(c => c.id === newConvId)) {
      switchConversation(newConvId)
    } else {
      loadConversations().then(() => {
        if (conversations.value.some(c => c.id === newConvId)) {
          switchConversation(newConvId)
        }
      })
    }
  }
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: 100vh;
  background: var(--bg-chat-area);
  box-shadow: var(--shadow);
}

.conv-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  background: var(--bg-chat-header);
  transition: width 0.3s ease, border-color 0.3s ease;
  overflow: hidden;
}

.conv-panel--collapsed {
  width: 0;
  border-right-color: transparent;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.new-btn {
  background: var(--accent-gradient);
  border: none;
}

.new-btn:hover {
  background: var(--accent-gradient-hover);
}

.panel-search {
  padding: 0 12px 10px;
}

.conv-items {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.panel-empty {
  display: flex;
  justify-content: center;
  padding: 40px 16px;
  font-size: 13px;
  color: var(--accent-color);
}

.conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px;
  margin-bottom: 2px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.conv-item:hover {
  background: var(--bg-conv-item-hover);
}

.conv-item--active {
  background: var(--bg-active);
}

.conv-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conv-item-editing {
  padding: 2px 0;
}

.conv-item-title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-item-date {
  font-size: 11px;
  color: var(--text-secondary);
}

.conv-item-actions {
  display: none;
  flex-shrink: 0;
  margin-left: 4px;
}

.conv-item:hover .conv-item-actions {
  display: flex;
}

.panel-pagination {
  display: flex;
  justify-content: center;
  padding: 10px 0 14px;
  border-top: 1px solid var(--border-color);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: var(--gradient-chat);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.expand-btn {
  margin-right: 8px;
  flex-shrink: 0;
}

.current-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  color: var(--text-primary);
  font-weight: 600;
}

.welcome-desc {
  margin: 0 0 32px;
  font-size: 15px;
  color: var(--text-secondary);
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
  background: var(--bg-hover);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-tag:hover {
  background: var(--bg-active);
  color: var(--text-primary);
}
</style>