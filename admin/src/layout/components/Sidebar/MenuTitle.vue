<template>
  <span class="menu-title-row">
    <span class="menu-title-text">{{ title }}</span>
    <span v-if="showMineUnreadBadge" class="menu-unread-badge">{{ badgeText }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { useTicketStore } from '@/store/modules/ticket'

const props = defineProps({
  path: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  }
})

const ticketStore = useTicketStore()

const showMineUnreadBadge = computed(() => {
  const normalized = props.path.replace(/\/+$/, '')
  return normalized.endsWith('/ticket/mine') && ticketStore.mineUnread > 0
})

const badgeText = computed(() => {
  const count = ticketStore.mineUnread
  return count > 99 ? '99+' : count
})
</script>

<style scoped>
.menu-title-row {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
  padding-right: 12px;
  box-sizing: border-box;
  line-height: 1;
}

.menu-title-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-unread-badge {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  margin-left: 8px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  box-sizing: border-box;
}
</style>
