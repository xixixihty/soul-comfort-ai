<template>
  <div class="conversation-view">
    <div class="page-header">
      <h2 class="page-title">对话记录</h2>
      <el-button type="primary" :icon="Plus" @click="handleCreate" class="create-btn">
        新建对话
      </el-button>
    </div>

    <div class="tag-filter" v-if="userTags.length > 0 || activeTag">
      <div class="filter-label">标签筛选：</div>
      <div class="tag-list">
        <span
          class="filter-tag"
          :class="{ active: !activeTag }"
          @click="filterByTag('')"
        >全部</span>
        <span
          v-for="t in userTags"
          :key="t"
          class="filter-tag"
          :class="{ active: activeTag === t }"
          @click="filterByTag(t)"
        >{{ t }}</span>
      </div>
    </div>

    <div class="conv-list" v-loading="loading">
      <div v-if="conversations.length === 0 && !loading" class="empty-state">
        <el-icon :size="48" color="#d4a373"><ChatDotRound /></el-icon>
        <p v-if="activeTag">该标签下还没有对话</p>
        <p v-else>还没有对话记录，去和甜弈聊聊吧</p>
        <el-button type="primary" @click="$router.push('/')">开始聊天</el-button>
      </div>

      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conv-card"
        @click="viewConversation(conv.id)"
      >
        <div class="conv-info">
          <div class="conv-title-row">
            <span v-if="!conv.editing" class="conv-title">{{ conv.title }}</span>
            <el-input
              v-else
              v-model="conv.editTitle"
              size="small"
              class="edit-input"
              @keyup.enter="saveRename(conv)"
              @blur="saveRename(conv)"
              ref="editInputRef"
            />
            <TagSelector
              :modelValue="conv.tag"
              @update:modelValue="(val) => handleTagChange(conv, val)"
              @click.stop
            />
          </div>
          <span class="conv-date">{{ formatDate(conv.updatedAt) }}</span>
        </div>
        <div class="conv-actions" @click.stop>
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
            @click="handleDelete(conv)"
            title="删除"
          />
        </div>
      </div>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadConversations"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TagSelector from '../components/TagSelector.vue'
import {
  fetchConversationList,
  fetchConversationListByTag,
  createConversation,
  renameConversation,
  updateConversationTag,
  getUserTags,
  deleteConversation
} from '../api/conversation.js'

const router = useRouter()
const loading = ref(false)
const conversations = ref([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const activeTag = ref('')
const userTags = ref([])

function formatDate(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadConversations() {
  loading.value = true
  try {
    const res = activeTag.value
      ? await fetchConversationListByTag(activeTag.value, currentPage.value, pageSize)
      : await fetchConversationList(currentPage.value, pageSize)
    if (res.code === 0 && res.data) {
      conversations.value = (res.data.records || []).map(c => ({
        ...c,
        editing: false,
        editTitle: c.title
      }))
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载对话列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const res = await getUserTags()
    if (res.code === 0 && res.data) {
      userTags.value = res.data || []
    }
  } catch (e) {
    console.error('加载标签列表失败', e)
  }
}

function filterByTag(tag) {
  activeTag.value = tag
  currentPage.value = 1
  loadConversations()
}

async function handleTagChange(conv, tag) {
  try {
    const res = await updateConversationTag(conv.id, tag)
    if (res.code === 0) {
      conv.tag = tag
      ElMessage.success('标签已更新')
      loadTags()
      if (activeTag.value && activeTag.value !== tag) {
        loadConversations()
      }
    }
  } catch (e) {
    ElMessage.error('更新标签失败')
    console.error(e)
  }
}

async function handleCreate() {
  try {
    const res = await createConversation('新对话')
    if (res.code === 0 && res.data) {
      ElMessage.success('对话创建成功')
      router.push({ path: '/', query: { convId: res.data.id } })
    }
  } catch (e) {
    ElMessage.error('创建对话失败')
    console.error(e)
  }
}

function startRename(conv) {
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
      ElMessage.success('重命名成功')
    }
  } catch (e) {
    ElMessage.error('重命名失败')
    conv.editTitle = conv.title
    console.error(e)
  }
}

async function handleDelete(conv) {
  try {
    await ElMessageBox.confirm(
      `确定删除对话「${conv.title}」吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteConversation(conv.id)
    conversations.value = conversations.value.filter(c => c.id !== conv.id)
    total.value--
    ElMessage.success('对话已删除')
    if (conv.tag) {
      loadTags()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function viewConversation(convId) {
  router.push({ path: '/', query: { convId } })
}

onMounted(() => {
  loadConversations()
  loadTags()
})
</script>

<style scoped>
.conversation-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-chat-area);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 600;
}

.create-btn {
  background: var(--accent-gradient);
  border: none;
}

.create-btn:hover {
  background: var(--accent-gradient-hover);
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  color: var(--text-secondary);
  gap: 16px;
}

.empty-state p {
  font-size: 15px;
  margin: 0;
}

.conv-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 8px;
  background: var(--bg-card);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.conv-card:hover {
  background: var(--bg-conv-item-hover);
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-title-row {
  display: flex;
  align-items: center;
}

.conv-title {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.edit-input {
  max-width: 300px;
}

.conv-date {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
}

.conv-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 20px;
}

.tag-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.filter-label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-tag {
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: var(--bg-note);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  background: var(--bg-note-hover);
  color: var(--text-primary);
}

.filter-tag.active {
  background: var(--accent-color);
  color: #fff;
}
</style>