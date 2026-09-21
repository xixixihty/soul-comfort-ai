<template>
  <div class="page-wrap">
    <div class="panel">
      <p class="page-kicker">ABOUT SOULCOMFORT</p>
      <h1 class="page-title">关于甜弈</h1>
      <p class="page-lead">
        甜弈是一位安静、温柔、随时在线的 AI 心灵陪伴者。它不评判、不催促，只想认真听你说完。
      </p>

      <div class="story">
        <h2 class="story-title">我们为什么做甜弈</h2>
        <p>
          每个人都会有一段夜里无人说话的时光。我们把对话模型、情绪记录与音乐疗愈结合在一起，
          希望当你感到低落、深夜睡不着、或只是想说说话时，能有一个不打扰、不评判的地方，
          把心事轻轻放下。
        </p>
      </div>

      <div class="story">
        <h2 class="story-title">甜弈能做什么</h2>
        <ul class="value-list">
          <li>AI 贴心倾听：自然对话，随时回应你的情绪</li>
          <li>情绪日记：记录每天的心情，回看自己的变化</li>
          <li>心情音乐：点一首歌，让旋律替你说出感受</li>
          <li>每日签到：用细微的仪式感，照顾自己的状态</li>
        </ul>
      </div>

      <div class="story">
        <h2 class="story-title">我们的承诺</h2>
        <p>
          我们认真对待每一份倾诉，也重视你的隐私——你的内容只服务于你，甜弈会一直安静地陪在你的身边。
        </p>
      </div>

      <button class="cta-btn" @click="goChat">开始和甜弈聊聊</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

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
  width: 640px;
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

.story {
  margin-bottom: 20px;
}

.story-title {
  margin: 0 0 8px;
  font-size: 16px;
  letter-spacing: 2px;
  color: #7a4424;
}

.story p {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: #8a6a50;
}

.value-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  line-height: 2;
  color: #8a6a50;
}

.cta-btn {
  margin-top: 10px;
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

html.dark .page-lead,
html.dark .story p,
html.dark .value-list {
  color: #cdb79f;
}

html.dark .story-title {
  color: #e0b790;
}
</style>