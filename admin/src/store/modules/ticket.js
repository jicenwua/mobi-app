import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTicketUnreadSummary } from '@/api/ticket'

export const useTicketStore = defineStore('ticket', () => {
  const mineUnread = ref(0)

  async function refreshUnreadSummary() {
    try {
      const res = await getTicketUnreadSummary()
      mineUnread.value = res.data?.mineUnread || 0
    } catch {
      mineUnread.value = 0
    }
  }

  return {
    mineUnread,
    refreshUnreadSummary
  }
})
