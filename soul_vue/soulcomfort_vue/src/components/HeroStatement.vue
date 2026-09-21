<template>
  <section class="hero-statement">
    <h2 class="hero-title">在每一个难眠的夜</h2>
    <p class="hero-sub">甜弈都安静地陪着你</p>
  </section>
</template>

<script setup>
</script>

<style scoped>
.hero-statement {
  position: relative;
  text-align: left;
  animation: heroIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.15s both;
}

@keyframes heroIn {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-title {
  margin: 0 0 12px;
  font-size: 34px;
  line-height: 1.35;
  font-weight: 800;
  letter-spacing: 3px;
  color: #7a4424;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  background: linear-gradient(120deg, #7a4424 0%, #c97f4a 60%, #d4a373 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(255, 224, 186, 0.6);
}

.hero-sub {
  margin: 0;
  font-size: 15px;
  letter-spacing: 4px;
  color: #7c5c42;
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.75);
}

html.dark .hero-title {
  background: linear-gradient(120deg, #f2cd9f 0%, #e3a878 60%, #d4a373 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(255, 210, 160, 0.3);
}

html.dark .hero-sub {
  color: #d9c3a9;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.45);
}
</style>