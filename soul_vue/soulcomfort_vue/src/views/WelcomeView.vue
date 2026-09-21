<template>
  <div class="welcome-page">
    <ThemeToggle />
    <AmbientParticles />
    <div class="welcome-inner">
      <div class="welcome-left">
        <div class="welcome-left-top">
          <HeroStatement />
          <QuoteRotator />
        </div>

        <div class="welcome-left-mid">
          <FeaturePills />
          <BreathGuide />
          <MoodCheckIn />
        </div>

        <div class="welcome-left-bot">
          <CompanionCounter />
          <GuestNav />
        </div>
      </div>

      <aside class="welcome-card">
        <div class="welcome-card-brand">
          <LogoMark :size="52" />
          <h2 class="welcome-card-title">遇见甜弈之前</h2>
        </div>
        <p class="welcome-card-text">
          先轻轻吸一口气，把那些暂时说不出口的话，
          放心地交给我吧。
        </p>
        <button type="button" class="welcome-cta" @click="goLogin">
          <span>进入甜弈</span>
          <span class="welcome-cta-arrow" aria-hidden="true">→</span>
        </button>
        <p class="welcome-card-foot">无需注册，也可以先四处看看</p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import LogoMark from '../components/LogoMark.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import HeroStatement from '../components/HeroStatement.vue'
import QuoteRotator from '../components/QuoteRotator.vue'
import AmbientParticles from '../components/AmbientParticles.vue'
import FeaturePills from '../components/FeaturePills.vue'
import BreathGuide from '../components/BreathGuide.vue'
import MoodCheckIn from '../components/MoodCheckIn.vue'
import CompanionCounter from '../components/CompanionCounter.vue'
import GuestNav from '../components/GuestNav.vue'

const router = useRouter()

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.welcome-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 56px;
  overflow: hidden;
  z-index: 1;
}

.welcome-page :deep(.ambient-particles) {
  inset: 0;
  opacity: 0.9;
}

.welcome-inner {
  position: relative;
  width: 100%;
  max-width: 1080px;
  display: flex;
  align-items: stretch;
  gap: 56px;
  animation: welcomeIn 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes welcomeIn {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-left {
  flex: 1 1 auto;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
  gap: 34px;
}

.welcome-left-top,
.welcome-left-mid,
.welcome-left-bot {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 18px;
}

/* 右侧引导卡：给页面一个温柔收口 */
.welcome-card {
  flex: none;
  width: 340px;
  align-self: center;
  padding: 34px 30px 26px;
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.5);
  border: 1px solid rgba(212, 163, 115, 0.32);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  box-shadow: 0 12px 40px rgba(180, 120, 80, 0.14);
}

.welcome-card-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.welcome-card-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #8a4b2e;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.welcome-card-text {
  margin: 0 0 24px;
  font-size: 14px;
  line-height: 1.9;
  letter-spacing: 1px;
  color: #7c5c42;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  text-align: center;
}

.welcome-cta {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 0;
  border: none;
  border-radius: 14px;
  background: rgba(255, 138, 61, 0.6);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #fff;
  font-size: 16px;
  letter-spacing: 3px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  cursor: pointer;
  box-shadow:
    0 4px 14px rgba(255, 130, 40, 0.35),
    inset 0 1px 2px rgba(255, 255, 255, 0.35);
  transition: all 0.3s;
}

.welcome-cta:hover {
  background: rgba(255, 122, 41, 0.75);
  border-color: rgba(255, 255, 255, 0.65);
  transform: translateY(-2px);
  box-shadow:
    0 6px 18px rgba(255, 130, 40, 0.45),
    inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.welcome-cta-arrow {
  font-size: 15px;
  transition: transform 0.25s;
}

.welcome-cta:hover .welcome-cta-arrow {
  transform: translateX(3px);
}

.welcome-card-foot {
  margin: 16px 0 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #9a8771;
  text-align: center;
}

html.dark .welcome-card {
  background: rgba(42, 33, 27, 0.6);
  border-color: rgba(212, 163, 115, 0.28);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
}

html.dark .welcome-card-title {
  color: #f0c89a;
}

html.dark .welcome-card-text {
  color: #d9c3a9;
}

html.dark .welcome-card-foot {
  color: #84705f;
}

@media (max-width: 960px) {
  .welcome-page {
    padding: 32px 28px;
  }

  .welcome-inner {
    flex-direction: column;
    gap: 32px;
  }

  .welcome-left {
    max-width: 100%;
  }

  .welcome-card {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .welcome-page {
    padding: 24px 18px;
    overflow-y: auto;
  }

  .welcome-page :deep(.ambient-particles) {
    display: none;
  }

  .welcome-left {
    gap: 26px;
  }

  .welcome-card {
    padding: 26px 20px 22px;
  }
}
</style>