<template>
  <div class="checkin-history">
    <div class="page-header">
      <h2 class="page-title">签到记录</h2>
      <div class="view-tabs">
        <span
          class="tab-item"
          :class="{ 'tab-item--active': viewMode === 'list' }"
          @click="viewMode = 'list'"
        >列表视图</span>
        <span class="tab-divider">|</span>
        <span
          class="tab-item"
          :class="{ 'tab-item--active': viewMode === 'calendar' }"
          @click="viewMode = 'calendar'"
        >日历视图</span>
        <span class="tab-divider">|</span>
        <span
          class="tab-item"
          :class="{ 'tab-item--active': viewMode === 'tree' }"
          @click="switchToTree"
        >情绪年轮</span>
      </div>
    </div>

    <div v-if="viewMode === 'list'" class="list-view">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索备注关键词..."
          :prefix-icon="Search"
          clearable
          size="default"
          class="search-input"
          @keyup.enter="loadList"
          @clear="loadList"
        />
        <el-button type="primary" @click="loadList" :loading="listLoading">搜索</el-button>
      </div>

      <div class="checkin-list" v-loading="listLoading">
        <div v-if="records.length === 0 && !listLoading" class="empty-state">
          <el-icon :size="48" color="#e8a9c1"><Calendar /></el-icon>
          <p>还没有签到记录，去和甜弈聊聊天吧</p>
        </div>

        <div
          v-for="record in records"
          :key="record.id"
          class="checkin-card"
        >
          <div class="checkin-emotion">
            <span class="emotion-icon">{{ record.emotionLabel }}</span>
          </div>
          <div class="checkin-info">
            <span class="checkin-date">{{ record.date }}</span>
            <span v-if="record.note" class="checkin-note">{{ record.note }}</span>
            <span v-else class="checkin-note checkin-note--empty">无备注</span>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="total > listSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="listSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </div>

    <div v-else-if="viewMode === 'calendar'" class="calendar-view">
      <div class="calendar-header">
        <el-button :icon="ArrowLeft" circle text @click="prevMonth" />
        <span class="calendar-month">{{ calendarYear }}年{{ calendarMonth }}月</span>
        <el-button :icon="ArrowRight" circle text @click="nextMonth" />
        <el-button type="primary" size="small" @click="goToToday" class="today-btn">今天</el-button>
      </div>

      <div class="calendar-weekdays">
        <span v-for="day in weekDays" :key="day" class="weekday">{{ day }}</span>
      </div>

      <div class="calendar-grid" v-loading="calendarLoading">
        <div
          v-for="(cell, idx) in calendarCells"
          :key="idx"
          class="calendar-cell"
          :class="{
            'calendar-cell--empty': !cell.day,
            'calendar-cell--today': cell.isToday,
            'calendar-cell--checked': cell.checked
          }"
          @click="cell.checked && showDetail(cell)"
        >
          <span v-if="cell.day" class="cell-day">{{ cell.day }}</span>
          <span v-if="cell.checked" class="cell-emotion">
            {{ cell.emotionLabel || '' }}
          </span>
        </div>
      </div>
    </div>

    <div v-else class="tree-view" v-loading="treeLoading">
      <div class="tree-header">
        <el-button :icon="ArrowLeft" circle text @click="prevYear" />
        <span class="calendar-month">{{ treeYear }} 年</span>
        <el-button :icon="ArrowRight" circle text :disabled="treeYear >= new Date().getFullYear()" @click="nextYear" />
      </div>

      <div class="tree-legend">
        <span v-for="item in legendItems" :key="item.emotion" class="legend-item">
          <i class="legend-dot" :style="{ background: item.color }"></i>{{ item.word }}
        </span>
      </div>

      <div class="tree-grid">
        <div v-for="m in treeMonths" :key="m.month" class="tree-month-row">
          <span class="tree-month-label">{{ m.month }}月</span>
          <div class="tree-dots">
            <span
              v-for="d in m.days"
              :key="d.date"
              class="tree-dot"
              :style="{ background: d.color }"
              :title="d.checked ? `${d.date} ${d.word}` : d.date"
            ></span>
          </div>
        </div>
      </div>

      <div class="star-section">
        <h3 class="star-title">⭐ 解忧纸星</h3>
        <p class="star-desc">那些日子被你折成了星星收进瓶里，点开一颗，看看甜弈想对你说的话</p>
        <div v-if="starDays.length" class="star-grid">
          <button
            v-for="s in starDays"
            :key="s.date"
            class="star-item"
            :disabled="starOpening === s.date"
            @click="openStar(s)"
          >
            <span class="star-icon">✦</span>
            <span class="star-date">{{ s.date.slice(5) }}</span>
            <span class="star-word">{{ s.word }}</span>
          </button>
        </div>
        <p v-else-if="!treeLoading" class="star-empty">这一年没有需要解开的心事，真好 ✨</p>
      </div>

      <el-dialog v-model="starDialogVisible" title="解忧瓶打开了" width="380px" :close-on-click-modal="true">
        <div class="star-care-text">{{ starCareText }}</div>
        <template #footer>
          <el-button @click="starDialogVisible = false">先收好啦</el-button>
          <el-button type="primary" @click="goStarConv">去和甜弈聊聊</el-button>
        </template>
      </el-dialog>
    </div>

    <el-dialog v-model="detailVisible" title="签到详情" width="360px" :close-on-click-modal="true">
      <div v-if="detailRecord" class="detail-content">
        <div class="detail-emotion">{{ detailRecord.emotionLabel }}</div>
        <div class="detail-date">{{ detailRecord.date }}</div>
        <div v-if="detailRecord.note" class="detail-note">{{ detailRecord.note }}</div>
        <div v-else class="detail-note detail-note--empty">无备注</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Search, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchCheckinList, fetchCheckinCalendar } from '../api/checkin.js'
import { fetchMoodCalendar, openCareOpener } from '../api/care.js'

const viewMode = ref('list')
const router = useRouter()

/* ===== 情绪年轮（12×31 心情点阵）+ 解忧纸星 ===== */
const EMOTION_COLORS = {
  happy: '#e8a9c1', grateful: '#dfb98e', energetic: '#94c9a5', calm: '#a9cbd9',
  tired: '#a99ac6', sad: '#7d8ea6', anxious: '#b58fb0', angry: '#cf7f8e'
}
const LEGEND = [
  ['happy', '开心'], ['grateful', '感恩'], ['energetic', '活力'], ['calm', '平静'],
  ['tired', '疲惫'], ['sad', '难过'], ['anxious', '焦虑'], ['angry', '生气']
]
/** 被折进"解忧瓶"的日子：低谷系心情 */
const LOW_EMOTIONS = ['sad', 'anxious', 'angry', 'tired']

const treeLoading = ref(false)
const treeYear = ref(new Date().getFullYear())
const moodPoints = ref([])
const starOpening = ref('')
const starDialogVisible = ref(false)
const starCareText = ref('')
const starConvId = ref('')

const legendItems = LEGEND.map(([emotion, word]) => ({ emotion, word, color: EMOTION_COLORS[emotion] }))

const moodByDate = computed(() => {
  const map = {}
  moodPoints.value.forEach(p => { map[p.date] = p })
  return map
})

const treeMonths = computed(() => {
  const year = treeYear.value
  const pad = n => String(n).padStart(2, '0')
  const rows = []
  for (let m = 1; m <= 12; m++) {
    const daysInMonth = new Date(year, m, 0).getDate()
    const days = []
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${pad(m)}-${pad(d)}`
      const p = moodByDate.value[dateStr]
      days.push({
        date: dateStr,
        checked: !!p,
        word: p ? p.emotionWord : '',
        color: p ? (EMOTION_COLORS[p.emotion] || '#c9b8c4') : 'var(--bg-hover)'
      })
    }
    rows.push({ month: m, days })
  }
  return rows
})

const starDays = computed(() =>
  moodPoints.value
    .filter(p => LOW_EMOTIONS.includes(p.emotion))
    .sort((a, b) => a.date.localeCompare(b.date))
)

async function loadTree() {
  treeLoading.value = true
  try {
    const res = await fetchMoodCalendar(treeYear.value)
    if (res.code === 0) {
      moodPoints.value = res.data || []
    }
  } catch (e) {
    console.error('加载情绪年轮失败', e)
  } finally {
    treeLoading.value = false
  }
}

function prevYear() {
  treeYear.value--
  loadTree()
}

function nextYear() {
  treeYear.value++
  loadTree()
}

function switchToTree() {
  if (viewMode.value === 'tree') return
  viewMode.value = 'tree'
  loadTree()
}

async function openStar(star) {
  starOpening.value = star.date
  try {
    const res = await openCareOpener({ refDate: star.date })
    if (res.code === 0 && res.data) {
      starCareText.value = res.data.message.content
      starConvId.value = res.data.convId
      starDialogVisible.value = true
    }
  } catch (e) {
    console.error('打开解忧瓶失败', e)
  } finally {
    starOpening.value = ''
  }
}

function goStarConv() {
  starDialogVisible.value = false
  router.push({ path: '/', query: { convId: starConvId.value } })
}

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const listLoading = ref(false)
const records = ref([])
const currentPage = ref(1)
const listSize = 20
const total = ref(0)
const keyword = ref('')

const calendarLoading = ref(false)
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const checkedDays = ref([])

const detailVisible = ref(false)
const detailRecord = ref(null)

const today = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const calendarCells = computed(() => {
  const year = calendarYear.value
  const month = calendarMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()

  const cells = []

  for (let i = 0; i < startWeekday; i++) {
    cells.push({ day: null })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const checked = checkedDays.value.find(c => c.date === dateStr)
    cells.push({
      day: d,
      date: dateStr,
      isToday: dateStr === today.value,
      checked: !!checked,
      emotion: checked?.emotion || null,
      emotionLabel: checked?.emotionLabel || null,
      note: checked?.note || null
    })
  }

  return cells
})

async function loadList() {
  listLoading.value = true
  try {
    const res = await fetchCheckinList(currentPage.value, listSize, keyword.value || '')
    if (res.code === 0 && res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载签到记录失败')
    console.error(e)
  } finally {
    listLoading.value = false
  }
}

async function loadCalendar() {
  calendarLoading.value = true
  try {
    const res = await fetchCheckinCalendar(calendarYear.value, calendarMonth.value)
    if (res.code === 0 && res.data) {
      checkedDays.value = res.data.checkedDays || []
    }
  } catch (e) {
    ElMessage.error('加载日历数据失败')
    console.error(e)
  } finally {
    calendarLoading.value = false
  }
}

function prevMonth() {
  if (calendarMonth.value === 1) {
    calendarMonth.value = 12
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
  loadCalendar()
}

function nextMonth() {
  if (calendarMonth.value === 12) {
    calendarMonth.value = 1
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
  loadCalendar()
}

function goToToday() {
  calendarYear.value = new Date().getFullYear()
  calendarMonth.value = new Date().getMonth() + 1
  loadCalendar()
}

function showDetail(cell) {
  detailRecord.value = {
    date: cell.date,
    emotionLabel: cell.emotionLabel,
    note: cell.note
  }
  detailVisible.value = true
}

onMounted(() => {
  loadList()
  loadCalendar()
})
</script>

<style scoped>
.checkin-history {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 600;
}

.view-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tab-item {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
}

.tab-item--active {
  color: var(--accent-color);
  font-weight: 600;
}

.tab-item:hover {
  color: var(--accent-color);
}

.tab-divider {
  color: var(--border-color);
}

.list-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-bar {
  display: flex;
  gap: 10px;
  padding: 0 24px 16px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  max-width: 360px;
}

.checkin-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
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

.checkin-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 8px;
  background: var(--bg-card);
  border-radius: 12px;
  transition: all 0.2s;
}

.checkin-card:hover {
  background: var(--bg-conv-item-hover);
}

.checkin-emotion {
  flex-shrink: 0;
}

.emotion-icon {
  font-size: 16px;
}

.checkin-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.checkin-date {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.checkin-note {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkin-note--empty {
  color: var(--text-muted);
  font-style: italic;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 20px;
  flex-shrink: 0;
}

.calendar-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 24px;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 16px;
  flex-shrink: 0;
}

.calendar-month {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 120px;
  text-align: center;
}

.today-btn {
  margin-left: 8px;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  padding: 0 0 8px;
  flex-shrink: 0;
}

.weekday {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  padding: 8px 0;
}

.calendar-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: minmax(80px, 1fr);
  gap: 4px;
  align-content: start;
}

.calendar-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: default;
  transition: all 0.2s;
  position: relative;
}

.calendar-cell--empty {
  cursor: default;
}

.calendar-cell--today {
  border: 2px solid var(--accent-color);
}

.calendar-cell--checked {
  background: var(--bg-hover);
  cursor: pointer;
}

.calendar-cell--checked:hover {
  background: var(--bg-active);
}

.cell-day {
  font-size: 14px;
  font-weight: 500;
}

.cell-emotion {
  font-size: 14px;
  margin-top: 4px;
  color: var(--text-secondary);
  line-height: 1.3;
}

.detail-content {
  text-align: center;
  padding: 16px 0;
}

.detail-emotion {
  font-size: 24px;
  margin-bottom: 12px;
}

.detail-date {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.detail-note {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.8;
  padding: 12px 16px;
  background: var(--bg-note);
  border-radius: 8px;
}

.detail-note--empty {
  color: var(--text-muted);
  font-style: italic;
}

/* ===== 情绪年轮 ===== */
.tree-view {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: 0 24px 24px;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 14px;
  flex-shrink: 0;
}

.tree-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
  padding-bottom: 14px;
  flex-shrink: 0;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.tree-grid {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 14px 18px;
  background: var(--bg-card);
  border-radius: 14px;
  margin-bottom: 22px;
  overflow-x: auto;
}

.tree-month-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tree-month-label {
  width: 34px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
}

.tree-dots {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}

.tree-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: transform 0.15s;
}

.tree-dot:hover {
  transform: scale(1.35);
}

/* ===== 解忧纸星 ===== */
.star-section {
  border-top: 1px dashed var(--border-color);
  padding-top: 18px;
}

.star-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.star-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--text-secondary);
}

.star-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(118px, 1fr));
  gap: 10px;
}

.star-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.star-item:hover:not(:disabled) {
  border-color: var(--accent-blue, #4f8faa);
  background: var(--bg-hover);
  transform: translateY(-2px);
}

.star-item:disabled {
  opacity: 0.6;
  cursor: wait;
}

.star-icon {
  color: #c9a86b;
  font-size: 15px;
}

.star-date {
  font-variant-numeric: tabular-nums;
}

.star-word {
  color: var(--text-secondary);
  font-size: 12px;
}

.star-empty {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 8px 0 4px;
}

.star-care-text {
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eef5f9, #e3edf4);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.8;
}

html.dark .star-care-text {
  background: linear-gradient(135deg, #2a3140, #303c4e);
}
</style>