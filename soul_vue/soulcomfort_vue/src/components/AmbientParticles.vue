<template>
  <div class="ambient-particles" aria-hidden="true">
    <span
      v-for="(p, i) in particles"
      :key="i"
      class="particle"
      :style="p.style"
    ></span>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const PALETTE = [
  'rgba(255, 217, 168, 0.9)',
  'rgba(255, 233, 207, 0.9)',
  'rgba(255, 196, 138, 0.8)'
]

const particles = ref([])

onMounted(() => {
  particles.value = Array.from({ length: 8 }, (_, i) => {
    const size = 10 + Math.random() * 18
    return {
      style: {
        left: `${2 + Math.random() * 56}%`,
        top: `${4 + Math.random() * 84}%`,
        width: `${size}px`,
        height: `${size}px`,
        background: `radial-gradient(circle, ${PALETTE[i % PALETTE.length]} 0%, transparent 70%)`,
        animationDuration: `${5 + Math.random() * 4}s`,
        animationDelay: `${Math.random() * 4}s`,
        opacity: `${0.3 + Math.random() * 0.25}`
      }
    }
  })
})
</script>

<style scoped>
.ambient-particles {
  position: absolute;
  inset: 0 40% 0 0;
  overflow: hidden;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  will-change: transform, opacity;
  animation-name: drift;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

@keyframes drift {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.45;
  }
  50% {
    transform: translate(10px, -14px) scale(1.15);
    opacity: 0.7;
  }
}

@media (prefers-reduced-motion: reduce) {
  .particle {
    animation: none;
  }
}
</style>