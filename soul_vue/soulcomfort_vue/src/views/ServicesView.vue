<template>
  <div class="page-wrap">
    <div class="panel">
      <p class="page-kicker">SERVICES</p>
      <h1 class="page-title">服务与功能</h1>
      <p class="page-lead">四件小事，一起把自己照顾好。</p>

      <div class="service-grid">
        <div v-for="s in services" :key="s.title" class="service-card">
          <span class="service-icon" aria-hidden="true">{{ s.icon }}</span>
          <h2 class="service-title">{{ s.title }}</h2>
          <p class="service-desc">{{ s.desc }}</p>
        </div>
      </div>

      <button class="cta-btn" @click="goChat">开启今天的守护</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const services = [
  {
    icon: '💬',
    title: 'AI 贴心倾听',
    desc: '与甜弈随时对话，深夜也有温柔回应的对象。'
  },
  {
    icon: '📔',
    title: '情绪日记',
    desc: '记下每天的起落，回看时看见自己的成长曲线。'
  },
  {
    icon: '🎵',
    title: '心情音乐',
    desc: '点一首应景的歌，让旋律替你安放下情绪。'
  },
  {
    icon: '🌱',
    title: '每日签到',
    desc: '一小步的仪式感，让照顾自己被写进日常。'
  }
]

function goChat() {
  router.push('/login')
}
</script>

<style scoped>
.page-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  position: relative;
  z-index: 1;
}

.panel {
  width: 720px;
  max-width: 100%;
  padding: 44px 48px;
  border-radius: 20px;
  background: rgba(255, 251, 244, 0.72);
  backdrop-filter: blur(18px) saturate(1.1);
  -webkit-backdrop-filter: blur(18px) saturate(1.1);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 16px 48px rgba(160, 110, 60, 0.16);
}

.page-kicker {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 4px;
  color: #c08457;
}

.page-title {
  margin: 0 0 14px;
  font-size: 30px;
  letter-spacing: 6px;
  color: #6b4a33;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.page-lead {
  margin: 0 0 26px;
  font-size: 15px;
  line-height: 1.9;
  color: #8a6a50;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 28px;
}

.service-card {
  padding: 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(212, 163, 115, 0.25);
  transition: all 0.3s;
}

.service-card:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 157, 92, 0.55);
  box-shadow: 0 8px 20px rgba(255, 157, 92, 0.18);
}

.service-icon {
  font-size: 26px;
  line-height: 1;
}

.service-title {
  margin: 10px 0 6px;
  font-size: 16px;
  letter-spacing: 1px;
  color: #7a4424;
}

.service-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #8a6a50;
}

.cta-btn {
  padding: 12px 28px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  background: rgba(255, 138, 61, 0.78);
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  color: #fff;
  font-size: 15px;
  letter-spacing: 2px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(255, 130, 40, 0.35);
  transition: all 0.3s;
}

.cta-btn:hover {
  background: rgba(255, 122, 41, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(255, 130, 40, 0.45);
}

html.dark .panel {
  background: rgba(42, 33, 27, 0.72);
  border-color: rgba(212, 163, 115, 0.2);
}

html.dark .page-title {
  color: #f0c89a;
}

html.dark .page-kicker {
  color: #e8a878;
}

html.dark .page-lead {
  color: #cdb79f;
}

html.dark .service-card {
  background: rgba(32, 25, 19, 0.55);
  border-color: rgba(212, 163, 115, 0.2);
}

html.dark .service-title {
  color: #e0b790;
}

html.dark .service-desc {
  color: #b39d87;
}
</style>