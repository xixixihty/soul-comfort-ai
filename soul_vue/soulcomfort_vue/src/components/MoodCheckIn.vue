<template>
  <div class="mood-checkin">
    <p class="mood-caption">今天的心情，也可以轻轻说出来</p>
    <div class="mood-list" role="group" aria-label="选择今日心情">
      <button
        v-for="m in moods"
        :key="m.label"
        type="button"
        class="mood-btn"
        :class="{ 'mood-btn--active': selected === m.label }"
        :aria-pressed="selected === m.label"
        @click="pick(m.label)"
      >
        <span class="mood-icon">{{ m.icon }}</span>
        <span class="mood-label">{{ m.label }}</span>
      </button>
    </div>
    <p class="mood-result" :class="{ 'is-empty': !selected }">
      {{ selected ? `已记下：${selected}` : '点击任意一枚心情' }}
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const moods = [
  { icon: '😄', label: '开心' },
  { icon: '🙂', label: '平静' },
  { icon: '😕', label: '低落' },
  { icon: '😢', label: '难过' },
  { icon: '😴', label: '疲惫' }
]

const STORAGE_KEY = 'sc-login-last-mood'
const selected = ref(localStorage.getItem(STORAGE_KEY) || '')

function pick(label) {
  selected.value = label
  localStorage.setItem(STORAGE_KEY, label)
}
</script>

<style scoped>
.mood-checkin {
  animation: moodIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.7s both;
}

@keyframes moodIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mood-caption {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #9a8771;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

.mood-list {
  display: flex;
  gap: 10px;
}

.mood-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: rgba(255, 251, 244, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  cursor: pointer;
  transition: all 0.25s;
}

.mood-icon {
  font-size: 20px;
  line-height: 1;
  filter: grayscale(0.25);
  transition: transform 0.25s, filter 0.25s;
}

.mood-label {
  font-size: 11px;
  color: #9a8771;
}

.mood-btn:hover {
  transform: translateY(-3px);
  background: rgba(255, 251, 244, 0.75);
}

.mood-btn:hover .mood-icon {
  filter: grayscale(0);
  transform: scale(1.15);
}

.mood-btn--active {
  border-color: rgba(255, 157, 92, 0.65);
  background: rgba(255, 224, 186, 0.55);
  box-shadow: 0 4px 14px rgba(255, 157, 92, 0.28);
}

.mood-btn--active .mood-icon {
  filter: grayscale(0);
  transform: scale(1.2);
}

.mood-btn--active .mood-label {
  color: #8a4b2e;
  font-weight: 600;
}

.mood-result {
  margin: 8px 0 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #8a4b2e;
  transition: color 0.3s;
}

.mood-result.is-empty {
  color: #b39d87;
}

html.dark .mood-caption,
html.dark .mood-result.is-empty {
  color: #84705f;
}

html.dark .mood-btn {
  background: rgba(42, 33, 27, 0.6);
}

html.dark .mood-label {
  color: #a08a6f;
}

html.dark .mood-btn--active .mood-label {
  color: #f0c89a;
}

html.dark .mood-result {
  color: #e0b790;
}
</style>