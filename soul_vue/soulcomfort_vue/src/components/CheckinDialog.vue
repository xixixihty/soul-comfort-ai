<template>
  <el-dialog
    v-model="visible"
    title="今日心情打卡"
    width="420px"
    :close-on-click-modal="false"
    align-center
    class="checkin-dialog"
    @closed="markDismissed"
  >
    <div class="checkin-body">
      <p class="checkin-subtitle">今天你的心情怎么样？</p>
      <div class="emotion-options">
        <div
          v-for="opt in emotionOptions"
          :key="opt.value"
          class="emotion-option"
          :class="{ selected: selected === opt.value }"
          @click="selected = opt.value"
        >
          <span class="emotion-emoji">{{ opt.emoji }}</span>
          <span class="emotion-label">{{ opt.label }}</span>
        </div>
      </div>
      <el-input
        v-model="note"
        type="textarea"
        :rows="2"
        placeholder="说点什么吧...（可选）"
        maxlength="100"
        show-word-limit
        class="checkin-note"
      />
    </div>
    <template #footer>
      <el-button @click="handleSkip">今天不想打卡</el-button>
      <el-button type="primary" :disabled="!selected" @click="handleSubmit" :loading="submitting">
        打卡
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { doCheckin, fetchTodayCheckin } from '../api/checkin'

const visible = ref(false)
const selected = ref('')
const note = ref('')
const submitting = ref(false)

/** 当天日期（本地时间）作为"已跳过打卡"标记的键，次日自然失效 */
const DISMISS_KEY = 'checkin_dismiss_date'

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 今天是否已被用户关闭过（X / 跳过 / ESC 任一方式），关闭后当天不再打扰 */
function dismissedToday() {
  try {
    return localStorage.getItem(DISMISS_KEY) === todayStr()
  } catch {
    return false
  }
}

function markDismissed() {
  try {
    localStorage.setItem(DISMISS_KEY, todayStr())
  } catch {}
}

const emotionOptions = [
  { value: 'happy', emoji: '😊', label: '开心' },
  { value: 'calm', emoji: '😌', label: '平静' },
  { value: 'sad', emoji: '😢', label: '难过' },
  { value: 'anxious', emoji: '😰', label: '焦虑' },
  { value: 'angry', emoji: '😡', label: '生气' },
  { value: 'energetic', emoji: '💪', label: '充满活力' },
  { value: 'tired', emoji: '😴', label: '疲惫' },
  { value: 'grateful', emoji: '🙏', label: '感恩' }
]

async function tryShow() {
  // 用户当天已关闭过（X/跳过/ESC）→ 切页回来也不再弹出，避免反复打扰
  if (dismissedToday()) return
  try {
    const res = await fetchTodayCheckin()
    if (res.code === 0 && res.data && !res.data.checked) {
      selected.value = ''
      note.value = ''
      visible.value = true
    }
  } catch {}
}

async function handleSubmit() {
  if (!selected.value) return
  submitting.value = true
  try {
    await doCheckin(selected.value, note.value.trim())
    ElMessage.success('打卡成功！今天也要开心哦 🌿')
    visible.value = false
  } catch {
    ElMessage.error('打卡失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleSkip() {
  visible.value = false
}

defineExpose({ tryShow })
</script>

<style scoped>
.checkin-subtitle {
  text-align: center;
  color: var(--text-secondary);
  margin: 0 0 16px;
  font-size: 14px;
}

.emotion-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.emotion-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  background: var(--bg-card);
}

.emotion-option:hover {
  background: var(--bg-conv-item-hover);
}

.emotion-option.selected {
  border-color: var(--accent-color);
  background: var(--bg-quote);
}

.emotion-emoji {
  font-size: 28px;
  margin-bottom: 4px;
}

.emotion-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.checkin-note {
  margin-top: 4px;
}
</style>