<template>
  <div>
    <!-- 悬浮兜底胶囊（欢迎页时让位给 ChatView 里的内嵌形态） -->
    <CheckinPill mode="fixed" />
    <el-dialog
      v-model="dialogVisible"
      title="今日心情打卡"
      width="420px"
      :close-on-click-modal="false"
      align-center
      class="checkin-dialog"
      @closed="onDialogClosed"
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
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { doCheckin, fetchTodayCheckin } from '../api/checkin'
import CheckinPill from './CheckinPill.vue'
import { useCheckinReminder } from '../composables/useCheckinReminder.js'

const {
  dialogVisible,
  dismissedRecently,
  markDismissed,
  clearDismiss,
  showPill,
  hidePill
} = useCheckinReminder()

const selected = ref('')
const note = ref('')
const submitting = ref(false)
let justChecked = false

/* 每次打开弹窗（自动弹/胶囊点开的入口都汇聚到这里）重置选择，保证是全新一次打卡 */
watch(dialogVisible, (v) => {
  if (v) {
    selected.value = ''
    note.value = ''
  }
})

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
  try {
    const res = await fetchTodayCheckin()
    const checked = res.code === 0 && res.data && res.data.checked
    if (checked) {
      // 已打卡：清除一切提醒状态
      hidePill()
      clearDismiss()
      return
    }
    // 未打卡：冷却期内不再自动弹窗打扰，只挂提醒胶囊（8 秒后自动淡出，
    // 淡出不记冷却，下次切页仍会再提醒）；冷却结束后进聊天页重新弹出打卡窗
    if (dismissedRecently()) {
      showPill()
      return
    }
    dialogVisible.value = true
  } catch {}
}

function onDialogClosed() {
  if (justChecked) {
    justChecked = false
    hidePill()
    clearDismiss()
    return
  }
  markDismissed()
  showPill()
}

async function handleSubmit() {
  if (!selected.value) return
  submitting.value = true
  try {
    await doCheckin(selected.value, note.value.trim())
    ElMessage.success('打卡成功！今天也要开心哦 🌿')
    justChecked = true
    dialogVisible.value = false
  } catch {
    ElMessage.error('打卡失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleSkip() {
  dialogVisible.value = false
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
