<template>
  <div class="feature-pills" aria-label="甜弈核心能力">
    <span
      v-for="f in features"
      :key="f.label"
      class="pill"
      :style="{ '--pill-color': f.color }"
    >
      <span class="pill-dot"></span>{{ f.label }}
    </span>
  </div>
</template>

<script setup>
const features = [
  { label: 'AI 倾听', color: '#ff9d5c' },
  { label: '情绪日记', color: '#e8a15a' },
  { label: '心情音乐', color: '#ffc48a' }
]
</script>

<style scoped>
.feature-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  animation: pillsIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.5s both;
}

@keyframes pillsIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(255, 251, 244, 0.55);
  border: 1px solid rgba(212, 163, 115, 0.35);
  color: #7c5c42;
  font-size: 13px;
  letter-spacing: 1px;
  --el-bg-color: transparent;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(180, 120, 80, 0.1);
  transition: all 0.3s;
}

.pill .pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--pill-color);
}

.pill:hover {
  background: rgba(255, 138, 61, 0.78);
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 130, 40, 0.3);
}

.pill:hover .pill-dot {
  background: #fff;
}

html.dark .pill {
  background: rgba(42, 33, 27, 0.6);
  border-color: rgba(212, 163, 115, 0.28);
  color: #d9c3a9;
}

html.dark .pill:hover {
  color: #fff;
}
</style>