<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/store/modules/theme'

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.themeMode === 'dark')
</script>

<template>
  <div class="sky-decoration" aria-hidden="true">
    <!-- 日间：太阳 + 白云 -->
    <div class="sky-scene sky-scene--day" :class="{ 'is-visible': !isDark }">
      <div class="sun">
        <div class="sun-core"></div>
        <div class="sun-rays"></div>
      </div>
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
    </div>

    <!-- 夜间：月亮 + 黑云 + 星星 -->
    <div class="sky-scene sky-scene--night" :class="{ 'is-visible': isDark }">
      <div class="star" v-for="n in 18" :key="n" :class="`star-${n}`"></div>
      <div class="moon"></div>
      <div class="dark-cloud dark-cloud-1"></div>
      <div class="dark-cloud dark-cloud-2"></div>
      <div class="dark-cloud dark-cloud-3"></div>
    </div>
  </div>
</template>

<style scoped>
.sky-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.sky-scene {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.6s ease;
  pointer-events: none;
}

.sky-scene.is-visible {
  opacity: 1;
}

/* ---------- 太阳 ---------- */
.sun {
  position: absolute;
  top: 28px;
  right: 48px;
  width: 72px;
  height: 72px;
}

.sun-core {
  position: absolute;
  inset: 12px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #fff9c4 0%, #ffd54f 45%, #ffb300 100%);
  box-shadow:
    0 0 24px rgba(255, 193, 7, 0.55),
    0 0 48px rgba(255, 193, 7, 0.25);
  animation: sun-pulse 4s ease-in-out infinite;
}

.sun-rays {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: repeating-conic-gradient(
    from 0deg,
    rgba(255, 213, 79, 0.35) 0deg 8deg,
    transparent 8deg 18deg
  );
  animation: sun-spin 60s linear infinite;
}

@keyframes sun-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.04);
    opacity: 0.92;
  }
}

@keyframes sun-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ---------- 白云 ---------- */
.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 100px;
  box-shadow: 0 4px 16px rgba(148, 163, 184, 0.15);
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.cloud-1 {
  width: 100px;
  height: 32px;
  top: 18%;
  left: 8%;
  animation: cloud-drift 28s ease-in-out infinite;
}

.cloud-1::before {
  width: 44px;
  height: 44px;
  top: -22px;
  left: 14px;
}

.cloud-1::after {
  width: 52px;
  height: 52px;
  top: -28px;
  right: 12px;
}

.cloud-2 {
  width: 130px;
  height: 38px;
  top: 42%;
  right: 12%;
  animation: cloud-drift 36s ease-in-out infinite reverse;
}

.cloud-2::before {
  width: 56px;
  height: 56px;
  top: -30px;
  left: 20px;
}

.cloud-2::after {
  width: 48px;
  height: 48px;
  top: -24px;
  right: 24px;
}

.cloud-3 {
  width: 86px;
  height: 28px;
  bottom: 22%;
  left: 22%;
  animation: cloud-drift 32s ease-in-out infinite 2s;
}

.cloud-3::before {
  width: 38px;
  height: 38px;
  top: -20px;
  left: 10px;
}

.cloud-3::after {
  width: 42px;
  height: 42px;
  top: -22px;
  right: 8px;
}

@keyframes cloud-drift {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(18px);
  }
}

/* ---------- 月亮 ---------- */
.moon {
  position: absolute;
  top: 32px;
  right: 52px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #f5f5f5 0%, #e2e8f0 60%, #cbd5e1 100%);
  box-shadow:
    0 0 20px rgba(226, 232, 240, 0.35),
    inset -8px -4px 0 rgba(148, 163, 184, 0.15);
}

.moon::after {
  content: '';
  position: absolute;
  top: -6px;
  right: -10px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #0d1117;
  box-shadow: inset 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.moon::after {
  background: var(--content-bg, #0d1117);
}

/* ---------- 星星 ---------- */
.star {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 4px rgba(255, 255, 255, 0.8);
  animation: star-twinkle 3s ease-in-out infinite;
}

.star-1 { top: 12%; left: 15%; animation-delay: 0s; }
.star-2 { top: 8%; left: 35%; width: 2px; height: 2px; animation-delay: 0.5s; }
.star-3 { top: 18%; left: 55%; animation-delay: 1s; }
.star-4 { top: 10%; left: 72%; width: 2px; height: 2px; animation-delay: 1.5s; }
.star-5 { top: 25%; left: 8%; animation-delay: 0.3s; }
.star-6 { top: 30%; left: 28%; width: 2px; height: 2px; animation-delay: 2s; }
.star-7 { top: 22%; left: 82%; animation-delay: 0.8s; }
.star-8 { top: 38%; left: 45%; width: 2px; height: 2px; animation-delay: 1.2s; }
.star-9 { top: 45%; left: 18%; animation-delay: 2.5s; }
.star-10 { top: 50%; left: 65%; width: 2px; height: 2px; animation-delay: 0.2s; }
.star-11 { top: 55%; left: 88%; animation-delay: 1.8s; }
.star-12 { top: 62%; left: 32%; width: 2px; height: 2px; animation-delay: 0.6s; }
.star-13 { top: 68%; left: 58%; animation-delay: 2.2s; }
.star-14 { top: 72%; left: 12%; width: 2px; height: 2px; animation-delay: 1.4s; }
.star-15 { top: 78%; left: 75%; animation-delay: 0.9s; }
.star-16 { top: 85%; left: 42%; width: 2px; height: 2px; animation-delay: 2.8s; }
.star-17 { top: 15%; left: 92%; animation-delay: 1.1s; }
.star-18 { top: 35%; left: 62%; width: 4px; height: 4px; animation-delay: 0.4s; box-shadow: 0 0 6px rgba(255, 255, 255, 0.9); }

@keyframes star-twinkle {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.3);
  }
}

/* ---------- 黑云 ---------- */
.dark-cloud {
  position: absolute;
  background: rgba(30, 41, 59, 0.75);
  border-radius: 100px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.35);
}

.dark-cloud::before,
.dark-cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.dark-cloud-1 {
  width: 110px;
  height: 34px;
  top: 16%;
  left: 6%;
  animation: cloud-drift 30s ease-in-out infinite;
}

.dark-cloud-1::before {
  width: 48px;
  height: 48px;
  top: -24px;
  left: 16px;
}

.dark-cloud-1::after {
  width: 54px;
  height: 54px;
  top: -28px;
  right: 10px;
}

.dark-cloud-2 {
  width: 140px;
  height: 40px;
  top: 40%;
  right: 8%;
  background: rgba(15, 23, 42, 0.82);
  animation: cloud-drift 38s ease-in-out infinite reverse;
}

.dark-cloud-2::before {
  width: 58px;
  height: 58px;
  top: -32px;
  left: 22px;
}

.dark-cloud-2::after {
  width: 50px;
  height: 50px;
  top: -26px;
  right: 20px;
}

.dark-cloud-3 {
  width: 92px;
  height: 30px;
  bottom: 20%;
  left: 18%;
  animation: cloud-drift 34s ease-in-out infinite 1.5s;
}

.dark-cloud-3::before {
  width: 40px;
  height: 40px;
  top: -20px;
  left: 12px;
}

.dark-cloud-3::after {
  width: 44px;
  height: 44px;
  top: -22px;
  right: 10px;
}

@media screen and (max-width: 768px) {
  .sun {
    top: 16px;
    right: 16px;
    width: 52px;
    height: 52px;
  }

  .moon {
    top: 16px;
    right: 20px;
    width: 44px;
    height: 44px;
  }

  .moon::after {
    width: 40px;
    height: 40px;
  }

  .cloud-1,
  .dark-cloud-1 {
    transform: scale(0.75);
  }

  .cloud-2,
  .dark-cloud-2 {
    transform: scale(0.8);
  }
}
</style>
