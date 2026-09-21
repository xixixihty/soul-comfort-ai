<template>
  <div
    class="breath-guide"
    :class="{ 'breath-guide--paused': paused }"
    role="button"
    tabindex="0"
    aria-label="深呼吸引导，点击可暂停或继续"
    @click="toggle"
    @keydown.enter.prevent="toggle"
  >
    <div class="breath-ring">
      <span class="breath-core" :class="{ 'is-static': reduced }"></span>
    </div>
    <p class="breath-phase">{{ phaseText }}</p>
    <p class="breath-tip">{{ paused ? '点击继续' : '跟随圆环呼吸' }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const paused = ref(false)
const phaseText = ref(reduced ? '深呼吸 · 与呼吸同在' : '吸气 4 秒')

let cycleTimer = 0
let phaseTimers = []

function clearPhases() {
  clearTimeout(cycleTimer)
  phaseTimers.forEach((t) => clearTimeout(t))
  phaseTimers = []
}

function startCycle() {
  if (reduced) return
  phaseText.value = '吸气 4 秒'
  phaseTimers.push(setTimeout(() => (phaseText.value = '屏息 6 秒'), 4000))
  phaseTimers.push(setTimeout(() => (phaseText.value = '呼气 4 秒'), 10000))
  cycleTimer = setTimeout(startCycle, 14000)
}

function toggle() {
  if (reduced) return
  paused.value = !paused.value
  if (paused.value) {
    clearPhases()
  } else {
    startCycle()
  }
}

onMounted(() => {
  if (!reduced) startCycle()
})

onUnmounted(clearPhases)
</script>

<style scoped>
.breath-guide {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 4px;
  cursor: pointer;
  user-select: none;
  animation: breathIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.6s both;
}

@keyframes breathIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.breath-ring {
  position: relative;
  width: 66px;
  height: 66px;
  flex: none;
  border-radius: 50%;
  border: 1px solid rgba(212, 163, 115, 0.5);
  box-shadow: 0 0 18px rgba(255, 184, 119, 0.25);
}

.breath-core {
  position: absolute;
  inset: 12px;
  border-radius: 50%;
  background: radial-gradient(circle, #ffc48a 0%, #ff9d5c 70%);
  box-shadow: 0 0 14px rgba(255, 157, 92, 0.55);
  animation: breathe 14s ease-in-out infinite;
}

.breath-core.is-static {
  animation: none;
}

.breath-guide--paused .breath-core {
  animation-play-state: paused;
}

@keyframes breathe {
  0% {
    transform: scale(0.55);
  }
  28.5% {
    transform: scale(1);
  }
  71.5% {
    transform: scale(1);
  }
  100% {
    transform: scale(0.55);
  }
}

.breath-phase {
  margin: 0 0 4px;
  font-size: 15px;
  letter-spacing: 2px;
  color: #6b4a33;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

.breath-tip {
  margin: 0;
  font-size: 12px;
  color: #9a8771;
  letter-spacing: 1px;
}

html.dark .breath-phase {
  color: #d9c3a9;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.45);
}

html.dark .breath-tip {
  color: #84705f;
}

@media (prefers-reduced-motion: reduce) {
  .breath-core {
    animation: none;
  }
}
</style>