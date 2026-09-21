<template>
  <p class="quote-rotator" role="status" aria-live="polite">
    <span class="quote-bar" aria-hidden="true"></span>
    <transition name="quote-fade">
      <span :key="index" class="quote-text">{{ quotes[index] }}</span>
    </transition>
  </p>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const quotes = [
  '慢慢来，你不必立刻痊愈',
  '听见你了，辛苦了',
  '任何情绪都值得被温柔对待',
  '休息不是逃避，是蓄力',
  '今天的你，已经做得很好了',
  '把烦恼交给风，把夜晚留给自己',
  '你不必完美，也同样闪闪发光',
  '有一盏灯，一直为你亮着'
]

const index = ref(Math.floor(Math.random() * quotes.length))
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    // 步长 1..n-1 随机，避免连续重复
    index.value =
      (index.value + 1 + Math.floor(Math.random() * (quotes.length - 1))) % quotes.length
  }, 6500)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.quote-rotator {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin: 0;
  padding: 0 4px;
  animation: quoteIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.35s both;
}

@keyframes quoteIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.quote-bar {
  flex: none;
  width: 3px;
  height: 26px;
  margin-top: 6px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ffb877 0%, #e8a15a 100%);
  box-shadow: 0 0 10px rgba(255, 184, 119, 0.4);
}

.quote-text {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 16px;
  line-height: 1.8;
  letter-spacing: 1px;
  color: #6b4a33;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

.quote-fade-enter-active,
.quote-fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.quote-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.quote-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

html.dark .quote-text {
  color: #d9c3a9;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.45);
}
</style>