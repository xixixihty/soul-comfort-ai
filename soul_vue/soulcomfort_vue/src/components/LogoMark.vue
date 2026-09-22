<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="logo-mark"
    :class="moodClass"
    aria-hidden="true"
  >
    <path
      d="M40 10 A 26 26 0 1 0 54 40 A 19 19 0 1 1 40 10 Z"
      fill="url(#logo-mark-gradient)"
    />
    <path
      d="M34 22 Q 35.6 28.4 42 30 Q 35.6 31.6 34 38 Q 32.4 31.6 26 30 Q 32.4 28.4 34 22 Z"
      fill="#fbf3f7"
    />
    <defs>
      <linearGradient
        id="logo-mark-gradient"
        x1="14"
        y1="14"
        x2="52"
        y2="52"
        gradientUnits="userSpaceOnUse"
      >
        <stop stop-color="#f0bcd2" />
        <stop offset="1" stop-color="#e8a9c1" />
      </linearGradient>
    </defs>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: Number, default: 32 },
  /** 晴雨云头像：情绪趋势（CareTrend 名）驱动月牙的阴晴，NONE/RECOVERY 保持本彩 */
  mood: { type: String, default: 'NONE' }
})

const moodClass = computed(() => {
  if (props.mood === 'TREND_DOWN') return 'logo-mark--cloudy'
  if (props.mood === 'PERSISTENT_LOW') return 'logo-mark--rainy'
  return ''
})
</script>

<style scoped>
.logo-mark {
  display: block;
  filter: drop-shadow(0 4px 10px rgba(232,169,193, 0.35));
  transition: filter 0.4s ease, opacity 0.4s ease;
}

/* 心情走低：月牙褪成多云灰 */
.logo-mark--cloudy {
  filter: drop-shadow(0 4px 10px rgba(130, 140, 155, 0.35)) grayscale(0.35) brightness(0.95);
}

/* 持续低压：更深的雨云灰并轻微收暗 */
.logo-mark--rainy {
  filter: drop-shadow(0 4px 10px rgba(110, 120, 140, 0.4)) grayscale(0.6) brightness(0.85);
  opacity: 0.85;
}
</style>