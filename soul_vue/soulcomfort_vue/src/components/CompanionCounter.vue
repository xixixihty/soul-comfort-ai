<template>
  <div class="companion-counter">
    <p class="cc-title">每一夜，甜弈都在这里</p>
    <p class="cc-sub">守护每一份倾诉 · 让心事被温柔接住</p>
  </div>
</template>

<script setup>
</script>

<style scoped>
.companion-counter {
  padding: 4px 2px;
  animation: ccIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.8s both;
}

@keyframes ccIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cc-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #7a4424;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.75);
}

.cc-sub {
  margin: 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #9a8771;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

html.dark .cc-title {
  color: #f0c89a;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.45);
}

html.dark .cc-sub {
  color: #84705f;
}
</style>