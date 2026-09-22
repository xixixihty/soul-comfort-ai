<template>
  <transition name="care-pill-fade">
    <div
      v-if="carePillVisible && careCard"
      class="care-pill"
      @click="openCare"
    >
      <span class="care-pill-icon">🌙</span>
      <span class="care-pill-text">{{ careCard.text }}</span>
      <span class="care-pill-close" title="这次先不用" @click.stop="dismissCare">×</span>
    </div>
  </transition>
</template>

<script setup>
import { useCareReminder } from '../composables/useCareReminder.js'

/**
 * 回访关怀胶囊：顶部悬浮（与打卡胶囊同位置互斥，关怀优先，互斥逻辑在 CheckinPill），
 * 月光蓝渐变区别于打卡的粉金渐变；不自动淡出，点开进会话或 × 轻拒。
 */
const { carePillVisible, careCard, openCare, dismissCare } = useCareReminder()
</script>

<style scoped>
.care-pill {
  position: fixed;
  top: 76px;
  left: 200px;
  right: 0;
  margin: 0 auto;
  width: fit-content;
  max-width: 60vw;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: linear-gradient(135deg, #4f8faa, #6f7fae);
  box-shadow: 0 8px 24px rgba(79, 143, 170, 0.35);
  cursor: pointer;
  user-select: none;
  transition: transform 0.2s;
}

.care-pill:hover {
  transform: translateY(-2px);
}

.care-pill-icon {
  flex-shrink: 0;
}

.care-pill-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.care-pill-close {
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

.care-pill-close:hover {
  background: rgba(255, 255, 255, 0.4);
}

.care-pill-fade-enter-active,
.care-pill-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.care-pill-fade-enter-from,
.care-pill-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
