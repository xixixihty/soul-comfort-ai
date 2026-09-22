import { ref } from 'vue'

/**
 * 打卡提醒的共享状态（模块级单例）：
 * CheckinDialog（弹窗本体）、CheckinPill（提醒胶囊）、ChatView（内嵌形态的挂载判断）共用一份。
 * <p>
 * 冷却语义：关闭打卡弹窗（跳过/X/ESC）或点胶囊上的 × → 写入时间戳，30 分钟内不再自动弹窗，
 * 只挂提醒胶囊；胶囊 8 秒自动淡出【不】记冷却——切页回来还会再提醒，"点到为止"。
 */

/** 上次关闭提醒的时间戳键 */
const DISMISS_KEY = 'checkin_dismiss_at'
const REMIND_COOLDOWN_MS = 30 * 60 * 1000
/** 胶囊出现后自动淡出的时长 */
const PILL_AUTO_HIDE_MS = 8000

const reminderVisible = ref(false)
const dialogVisible = ref(false)
/** 聊天页空会话（欢迎页可见）时置 true：内嵌胶囊接管，顶部悬浮胶囊让位，避免双份同屏 */
const inlineActive = ref(false)

let pillTimer = null

function dismissedRecently() {
  try {
    const t = Number(localStorage.getItem(DISMISS_KEY) || 0)
    return t > 0 && Date.now() - t < REMIND_COOLDOWN_MS
  } catch {
    return false
  }
}

function markDismissed() {
  try {
    localStorage.setItem(DISMISS_KEY, String(Date.now()))
  } catch {}
}

function clearDismiss() {
  try {
    localStorage.removeItem(DISMISS_KEY)
  } catch {}
}

function showPill() {
  clearTimeout(pillTimer)
  reminderVisible.value = true
  pillTimer = setTimeout(() => {
    reminderVisible.value = false
  }, PILL_AUTO_HIDE_MS)
}

function hidePill() {
  clearTimeout(pillTimer)
  reminderVisible.value = false
}

function openFromPill() {
  clearTimeout(pillTimer)
  dialogVisible.value = true
}

/** 手动点 ×：写入 30 分钟冷却并收起胶囊（与"自动淡出不记冷却"区分开） */
function closePill() {
  markDismissed()
  hidePill()
}

export function useCheckinReminder() {
  return {
    reminderVisible,
    dialogVisible,
    inlineActive,
    dismissedRecently,
    markDismissed,
    clearDismiss,
    showPill,
    hidePill,
    openFromPill,
    closePill
  }
}
