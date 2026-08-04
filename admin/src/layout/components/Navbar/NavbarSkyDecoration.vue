<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/store/modules/theme'

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.themeMode === 'dark')
</script>

<template>
  <div class="navbar-sky" aria-hidden="true">
    <!-- 顶栏格子底纹（仅装饰层，不影响主内容区） -->
    <div class="navbar-sky-grid"></div>

    <!-- 日间：太阳 + 白云 -->
    <div class="sky-scene sky-scene--day" :class="{ 'is-visible': !isDark }">
      <div class="sun">
        <div class="sun-core"></div>
      </div>
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
    </div>

    <!-- 夜间：月亮 + 黑云 + 星星 -->
    <div class="sky-scene sky-scene--night" :class="{ 'is-visible': isDark }">
      <div class="star" v-for="n in 10" :key="n" :class="`star-${n}`"></div>
      <div class="moon"></div>
      <div class="dark-cloud dark-cloud-1"></div>
      <div class="dark-cloud dark-cloud-2"></div>
    </div>
  </div>
</template>

<style scoped>
.navbar-sky {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.navbar-sky-grid {
  position: absolute;
  inset: 0;
  background-color: var(--navbar-sky-bg, transparent);
  background-image: var(--navbar-sky-pattern, none);
  background-size: var(--navbar-sky-size, 24px 24px);
  opacity: 0.85;
}

.sky-scene {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.sky-scene.is-visible {
  opacity: 1;
}

/* ---------- 日间太阳 ---------- */
.sun {
  position: absolute;
  top: 6px;
  right: 168px;
  width: 36px;
  height: 36px;
}

.sun-core {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #fff9c4 0%, #ffd54f 50%, #ffb300 100%);
  box-shadow: 0 0 12px rgba(255, 193, 7, 0.45);
  animation: sun-pulse 4s ease-in-out infinite;
}

@keyframes sun-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}

/* ---------- 白云 ---------- */
.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 100px;
  box-shadow: 0 2px 8px rgba(148, 163, 184, 0.12);
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.cloud-1 {
  width: 72px;
  height: 22px;
  top: 8px;
  left: 28%;
  animation: cloud-drift 24s ease-in-out infinite;
}

.cloud-1::before {
  width: 28px;
  height: 28px;
  top: -14px;
  left: 10px;
}

.cloud-1::after {
  width: 32px;
  height: 32px;
  top: -16px;
  right: 8px;
}

.cloud-2 {
  width: 56px;
  height: 18px;
  top: 22px;
  right: 22%;
  animation: cloud-drift 30s ease-in-out infinite reverse;
}

.cloud-2::before {
  width: 24px;
  height: 24px;
  top: -12px;
  left: 8px;
}

.cloud-2::after {
  width: 22px;
  height: 22px;
  top: -11px;
  right: 6px;
}

@keyframes cloud-drift {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(10px);
  }
}

/* ---------- 月亮 ---------- */
.moon {
  position: absolute;
  top: 8px;
  right: 172px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #f8fafc 0%, #e2e8f0 100%);
  box-shadow: 0 0 10px rgba(226, 232, 240, 0.3);
}

.moon::after {
  content: '';
  position: absolute;
  top: -4px;
  right: -8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--navbar-sky-bg, #0d1117);
}

/* ---------- 星星 ---------- */
.star {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 3px rgba(255, 255, 255, 0.7);
  animation: star-twinkle 2.5s ease-in-out infinite;
}

.star-1 { top: 10px; left: 22%; animation-delay: 0s; }
.star-2 { top: 18px; left: 32%; animation-delay: 0.4s; }
.star-3 { top: 8px; left: 42%; animation-delay: 0.8s; }
.star-4 { top: 24px; left: 52%; animation-delay: 1.2s; }
.star-5 { top: 12px; left: 58%; animation-delay: 0.2s; }
.star-6 { top: 20px; right: 28%; animation-delay: 1.6s; }
.star-7 { top: 8px; right: 22%; animation-delay: 0.6s; }
.star-8 { top: 26px; right: 18%; width: 3px; height: 3px; animation-delay: 1s; }
.star-9 { top: 14px; right: 32%; animation-delay: 1.8s; }
.star-10 { top: 6px; right: 38%; animation-delay: 0.3s; }

@keyframes star-twinkle {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

/* ---------- 黑云 ---------- */
.dark-cloud {
  position: absolute;
  background: rgba(30, 41, 59, 0.8);
  border-radius: 100px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.25);
}

.dark-cloud::before,
.dark-cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.dark-cloud-1 {
  width: 68px;
  height: 20px;
  top: 10px;
  left: 26%;
  animation: cloud-drift 26s ease-in-out infinite;
}

.dark-cloud-1::before {
  width: 26px;
  height: 26px;
  top: -13px;
  left: 10px;
}

.dark-cloud-1::after {
  width: 30px;
  height: 30px;
  top: -15px;
  right: 6px;
}

.dark-cloud-2 {
  width: 52px;
  height: 16px;
  top: 24px;
  right: 24%;
  background: rgba(15, 23, 42, 0.85);
  animation: cloud-drift 32s ease-in-out infinite reverse;
}

.dark-cloud-2::before {
  width: 22px;
  height: 22px;
  top: -11px;
  left: 8px;
}

.dark-cloud-2::after {
  width: 20px;
  height: 20px;
  top: -10px;
  right: 6px;
}

@media screen and (max-width: 1024px) {
  .sun {
    right: 120px;
    width: 30px;
    height: 30px;
  }

  .moon {
    right: 124px;
    width: 26px;
    height: 26px;
  }

  .moon::after {
    width: 24px;
    height: 24px;
  }

  .cloud-1 {
    left: 34%;
    transform: scale(0.85);
  }
}

@media screen and (max-width: 768px) {
  .sun,
  .moon {
    display: none;
  }

  .cloud-1 {
    left: 38%;
    transform: scale(0.7);
  }

  .cloud-2,
  .dark-cloud-2 {
    display: none;
  }
}
</style>
