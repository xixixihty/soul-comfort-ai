<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="logo-mark"
    aria-hidden="true"
  >
    <path
      d="M40 10 A 26 26 0 1 0 54 40 A 19 19 0 1 1 40 10 Z"
      fill="url(#logo-mark-gradient)"
    />
    <path
      d="M34 22 Q 35.6 28.4 42 30 Q 35.6 31.6 34 38 Q 32.4 31.6 26 30 Q 32.4 28.4 34 22 Z"
      fill="#fff6ec"
    />
    <defs>
      <linearGradient
        id="logo-mark-gradient"
        x1="14"
        y1="14"
        x2="52"
        y2="52"
        gradientUnits="userSpaceOnUse"
      >
        <stop stop-color="#f2b98c" />
        <stop offset="1" stop-color="#d4a373" />
      </linearGradient>
    </defs>
  </svg>
</template>

<script setup>
defineProps({
  size: { type: Number, default: 32 }
})
</script>

<style scoped>
.logo-mark {
  display: block;
  filter: drop-shadow(0 4px 10px rgba(212, 163, 115, 0.35));
}
</style>