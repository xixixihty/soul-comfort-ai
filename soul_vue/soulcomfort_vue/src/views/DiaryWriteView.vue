<template>
  <div class="diary-write-view">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <h2 class="page-title">{{ isEdit ? '编辑日记' : '写日记' }}</h2>
      </div>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <div class="diary-form">
      <el-input
        v-model="form.title"
        placeholder="给这篇日记起个标题吧..."
        size="large"
        class="title-input"
        maxlength="100"
        show-word-limit
      />

      <div class="mood-selector">
        <span class="mood-label">心情：</span>
        <el-radio-group v-model="form.mood" size="small">
          <el-radio-button
            v-for="m in presetMoods"
            :key="m.value"
            :value="m.value"
          >{{ m.label }}</el-radio-button>
        </el-radio-group>
        <span class="mood-divider">或</span>
        <el-input
          v-model="customMood"
          placeholder="自定义心情"
          size="small"
          class="custom-mood"
          maxlength="10"
          @input="onCustomMoodInput"
        />
      </div>

      <el-input
        v-model="form.content"
        type="textarea"
        placeholder="今天发生了什么？你的心情如何..."
        :rows="12"
        :autosize="{ minRows: 12, maxRows: 30 }"
        resize="none"
        class="content-input"
        maxlength="5000"
        show-word-limit
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchDiary, createDiary, updateDiary } from '../api/diary.js'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const customMood = ref('')

const isEdit = computed(() => !!route.params.id)

const form = ref({
  title: '',
  content: '',
  mood: ''
})

const presetMoods = [
  { value: 'sad', label: '😢 悲伤' },
  { value: 'anxious', label: '😰 焦虑' },
  { value: 'lost', label: '😶 迷茫' },
  { value: 'angry', label: '😠 生气' },
  { value: 'happy', label: '😊 开心' },
  { value: 'calm', label: '😌 平静' }
]

function onCustomMoodInput(val) {
  form.value.mood = val
}

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateDiary(route.params.id, form.value)
      ElMessage.success('日记更新成功')
    } else {
      await createDiary(form.value)
      ElMessage.success('日记保存成功')
    }
    router.push('/diary')
  } catch (e) {
    ElMessage.error('保存失败')
    console.error(e)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await fetchDiary(route.params.id)
      if (res.code === 0 && res.data) {
        form.value = {
          title: res.data.title || '',
          content: res.data.content || '',
          mood: res.data.mood || ''
        }
      }
    } catch (e) {
      ElMessage.error('加载日记失败')
      console.error(e)
      router.push('/diary')
    }
  }
})
</script>

<style scoped>
.diary-write-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-chat-area);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
  font-weight: 600;
}

.diary-form {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.title-input {
  margin-bottom: 16px;
}

.title-input :deep(.el-input__inner) {
  font-size: 20px;
  font-weight: 500;
  border: none;
  background: transparent;
  padding: 8px 0;
  color: var(--text-primary);
}

.mood-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.mood-label {
  font-size: 14px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.mood-divider {
  font-size: 13px;
  color: var(--text-secondary);
}

.custom-mood {
  width: 140px;
}

.content-input :deep(.el-textarea__inner) {
  border: none;
  background: transparent;
  font-size: 16px;
  line-height: 1.9;
  padding: 12px 0;
  color: var(--text-primary);
}

.content-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}
</style>