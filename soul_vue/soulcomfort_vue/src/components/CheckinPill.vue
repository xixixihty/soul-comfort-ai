<template>
  <transition name="pill-fade">
    <div v-if="shown" class="checkin-pill" :class="mode === 'inline' ? 'checkin-pill--inline' : 'checkin-pill--fixed'" @click="openFromPill">
      <span class="pill-text">🌙 今日心情还没记录，点这里说给甜弈听</span>
      <span class="pill-close" title="先不用提醒" @click.stop="closePill">×</span>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { useCheckinReminder } from '../composables/useCheckinReminder.js'
import { useCareReminder } from '../composables/useCareReminder.js'

/**
 * 未打卡提醒胶囊的两种形态（互斥，由 inlineActive 决定谁上场）：
 * - inline：聊天页空会话时内嵌在欢迎块里（小太阳图标上方），随内容流排布，零遮挡；
 * - fixed ：其余场景（会话已有消息/其他页面）顶部悬浮兜底，居中基准取主内容区（侧栏 200px 起算）。
 * 与回访关怀胶囊互斥：关怀优先，carePillVisible 时两种形态都让位。
 */
const props = defineProps({
  mode: { type: String, default: 'fixed' }
})

const { reminderVisible, dialogVisible, inlineActive, openFromPill, closePill } = useCheckinReminder()
const { carePillVisible } = useCareReminder()

const shown = computed(() =>
  reminderVisible.value && !dialogVisible.value && !carePillVisible.value &&
  (props.mode === 'inline' ? inlineActive.value : !inlineActive.value)
)
</script>

<style scoped>
.checkin-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: var(--accent-gradient);
  box-shadow: 0 8px 24px rgba(165, 105, 140, 0.35);
  cursor: pointer;
  user-select: none;
  transition: transform 0.2s;
}

.checkin-pill:hover {
  transform: translateY(-2px);
}

/* 内嵌形态：欢迎内容流第一段，小太阳图标上方 */
.checkin-pill--inline {
  margin: 0 auto 20px;
  max-width: 100%;
}

/* 悬浮形态：主内容区（排除 200px 侧栏）顶部居中 */
.checkin-pill--fixed {
  position: fixed;
  top: 76px;
  left: 200px;
  right: 0;
  margin: 0 auto;
  width: fit-content;
  max-width: 60vw;
  z-index: 2000;
}

.pill-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pill-close {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  font-size: 14px;
  transition: background 0.2s;
}

.pill-close:hover {
  background: rgba(255, 255, 255, 0.4);
}

.pill-fade-enter-active,
.pill-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.pill-fade-enter-from,
.pill-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
