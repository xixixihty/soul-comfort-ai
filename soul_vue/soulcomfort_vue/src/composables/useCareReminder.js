import { ref } from 'vue'
import { fetchCarePending, ackCare, openCareOpener } from '../api/care.js'
import router from '../router'

/**
 * 主动关怀（情绪灯塔）的共享状态（模块级单例，模式同 useCheckinReminder）：
 * CarePill（关怀胶囊）、App（趋势驱动晴雨云头像）、ChatView（"甜弈先开口"落点）、
 * ProfileDialog（开关）共用一份。
 * <p>
 * 交互约定：胶囊不自动淡出（关怀值得被看见），用户要么点开（ack=opened → 会话内落一条 care 消息），
 * 要么点 × 关闭（ack=dismissed → 后端连拒两次后自动降级为每周最多 1 次）。
 */

/** 待展示的关怀卡：{ careId, type, text, createdAt } 或 null */
const careCard = ref(null)
/** 情绪趋势：NONE | TREND_DOWN | PERSISTENT_LOW | RECOVERY（晴雨云头像用） */
const careTrend = ref('NONE')
/** 胶囊显隐（与 careCard 分开：关闭胶囊不必清掉卡，卡由后端 24h 缓存管理） */
const carePillVisible = ref(false)
/** ChatView 注册的当前会话 id：care 消息优先落进正在看的会话 */
const activeConvId = ref('')
/** opener 接口返回的 { convId, message }，ChatView 消费后置 null */
const pendingCareMessage = ref(null)

let refreshing = false

async function refreshCare() {
  if (refreshing) return
  refreshing = true
  try {
    const res = await fetchCarePending()
    if (res.code === 0 && res.data) {
      careTrend.value = res.data.trend || 'NONE'
      careCard.value = res.data.card || null
      carePillVisible.value = !!careCard.value
    }
  } catch (e) {
    console.warn('加载主动关怀失败:', e)
  } finally {
    refreshing = false
  }
}

function dismissCare() {
  carePillVisible.value = false
  if (careCard.value) {
    ackCare(careCard.value.careId, 'dismissed').catch(e => console.warn('ack dismissed 失败:', e))
  }
}

/** 点开关怀卡：ack → 调 opener 把"甜弈先开口"写进会话 → 导航到聊天页 */
async function openCare() {
  carePillVisible.value = false
  try {
    if (careCard.value) {
      await ackCare(careCard.value.careId, 'opened').catch(() => {})
    }
    const onChatPage = router.currentRoute.value.path === '/'
    const res = await openCareOpener({
      convId: onChatPage && activeConvId.value ? activeConvId.value : undefined
    })
    if (res.code === 0 && res.data) {
      careCard.value = null
      pendingCareMessage.value = res.data
      if (!onChatPage || router.currentRoute.value.query.convId !== res.data.convId) {
        router.push({ path: '/', query: { convId: res.data.convId } })
      }
    }
  } catch (e) {
    console.error('打开关怀会话失败:', e)
  }
}

export function useCareReminder() {
  return {
    careCard,
    careTrend,
    carePillVisible,
    activeConvId,
    pendingCareMessage,
    refreshCare,
    dismissCare,
    openCare
  }
}
