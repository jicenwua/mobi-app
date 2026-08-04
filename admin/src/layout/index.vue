<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" />

    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main">
        <SkyDecoration />
        <div class="app-main-inner">
          <router-view v-slot="{ Component, route }">
            <transition name="fade-transform">
              <div
                v-if="Component"
                :key="route.fullPath"
                class="app-main-page"
              >
                <keep-alive v-if="!route.meta?.noCache">
                  <component :is="Component" />
                </keep-alive>
                <component v-else :is="Component" />
              </div>
            </transition>
          </router-view>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import Sidebar from './components/Sidebar/index.vue'
import Navbar from './components/Navbar/index.vue'
import TagsView from './components/TagsView/index.vue'
import SkyDecoration from './components/SkyDecoration/index.vue'
import { useTicketStore } from '@/store/modules/ticket'
import { connectNotifySocket, disconnectNotifySocket, onNotifyMessage } from '@/utils/notifySocket'

const ticketStore = useTicketStore()
let offNotify = null

onMounted(() => {
  // 首次进入拉一次未读数；后续由 WebSocket unread_changed 推送触发刷新
  void ticketStore.refreshUnreadSummary()
  connectNotifySocket()
  offNotify = onNotifyMessage((payload) => {
    if (payload?.type === 'unread_changed') {
      void ticketStore.refreshUnreadSummary()
    }
  })
})

onUnmounted(() => {
  offNotify?.()
  disconnectNotifySocket()
})
</script>

<style scoped>
.app-wrapper {
  position: relative;
  height: 100vh;
  width: 100%;
  display: flex;
  overflow: hidden;
}

.sidebar-container {
  transition:
    width 0.28s,
    background-color 0.35s ease,
    border-color 0.35s ease,
    box-shadow 0.35s ease;
  width: 220px;
  height: 100%;
  position: fixed;
  font-size: 0px;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 1001;
  overflow: hidden;
  border-right: 1px solid var(--sidebar-border);
  box-shadow: var(--sidebar-shadow);
}

.main-container {
  min-height: 100%;
  transition:
    margin-left 0.28s,
    background-color 0.35s ease;
  margin-left: 220px;
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--content-bg);
  background-image: var(--content-bg-pattern);
  background-size: var(--content-bg-size);
}

.app-main {
  position: relative;
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: transparent;
  transition: background-color 0.35s ease;
}

.app-main-inner {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

.app-main-page {
  min-height: 100%;
}

:deep(.fade-transform-leave-active),
:deep(.fade-transform-enter-active) {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

:deep(.fade-transform-enter-from) {
  opacity: 0;
  transform: translateX(-16px);
}

:deep(.fade-transform-leave-to) {
  opacity: 0;
  transform: translateX(16px);
}

/* 响应式：平板 */
@media screen and (max-width: 1024px) {
  .sidebar-container {
    width: 180px;
  }

  .main-container {
    margin-left: 180px;
  }

  .app-main {
    padding: 15px;
  }
}

/* 响应式：手机 */
@media screen and (max-width: 768px) {
  .app-wrapper {
    flex-direction: column;
  }

  .sidebar-container {
    width: 100%;
    height: auto;
    position: relative;
    border-right: none;
    border-bottom: 1px solid var(--sidebar-border);
  }

  .main-container {
    margin-left: 0;
    flex: 1;
  }

  .app-main {
    padding: 10px;
  }
}

@media screen and (max-width: 480px) {
  .app-main {
    padding: 8px;
  }
}
</style>
