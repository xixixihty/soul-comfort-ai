<template>
  <div class="diary-view">
    <div class="page-header">
      <h2 class="page-title">心情日记</h2>
      <el-button type="primary" :icon="Edit" @click="$router.push('/diary/write')" class="write-btn">
        写日记
      </el-button>
    </div>

    <div class="diary-list" v-loading="loading">
      <div v-if="diaries.length === 0 && !loading" class="empty-state">
        <el-icon :size="48" color="#e8a9c1"><Notebook /></el-icon>
        <p>还没有写过日记，记录下此刻的心情吧</p>
        <el-button type="primary" @click="$router.push('/diary/write')">写日记</el-button>
      </div>

      <div
        v-for="diary in diaries"
        :key="diary.id"
        class="diary-card"
        @click="viewDiary(diary.id)"
      >
        <div class="diary-top">
          <h3 class="diary-title">{{ diary.title || '无标题' }}</h3>
          <el-tag v-if="diary.mood" size="small" :type="moodTagType(diary.mood)" effect="light">
            {{ diary.mood }}
          </el-tag>
        </div>
        <p class="diary-preview">{{ truncate(diary.content, 120) }}</p>
        <div class="diary-footer">
          <span class="diary-date">{{ formatDate(diary.createdAt) }}</span>
          <div class="diary-actions" @click.stop>
            <el-button
              :icon="Edit"
              circle
              text
              size="small"
              @click="$router.push(`/diary/${diary.id}/edit`)"
              title="编辑"
            />
            <el-button
              :icon="Delete"
              circle
              text
              size="small"
              @click="handleDelete(diary)"
              title="删除"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadDiaries"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchDiaryList, deleteDiary } from '../api/diary.js'

const router = useRouter()
const loading = ref(false)
const diaries = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

function formatDate(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function truncate(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

function moodTagType(mood) {
  const map = {
    'sad': 'info',
    'anxious': 'warning',
    'lost': '',
    'angry': 'danger',
    'happy': 'success',
    'calm': ''
  }
  return map[mood] || ''
}

async function loadDiaries() {
  loading.value = true
  try {
    const res = await fetchDiaryList(currentPage.value, pageSize)
    if (res.code === 0 && res.data) {
      diaries.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载日记列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

function viewDiary(id) {
  router.push(`/diary/${id}/edit`)
}

async function handleDelete(diary) {
  try {
    await ElMessageBox.confirm(
      `确定删除日记「${diary.title || '无标题'}」吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteDiary(diary.id)
    diaries.value = diaries.value.filter(d => d.id !== diary.id)
    ElMessage.success('日记已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadDiaries)
</script>

<style scoped>
.diary-view {
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

.write-btn {
  background: var(--accent-gradient);
  border: none;
}

.write-btn:hover {
  background: var(--accent-gradient-hover);
}

.diary-list {
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

.diary-card {
  padding: 20px;
  margin-bottom: 12px;
  background: var(--bg-card);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.diary-card:hover {
  background: var(--bg-conv-item-hover);
}

.diary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.diary-title {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 600;
}

.diary-preview {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.diary-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.diary-date {
  font-size: 12px;
  color: var(--text-secondary);
}

.diary-actions {
  display: flex;
  gap: 4px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 20px;
  flex-shrink: 0;
}
</style>