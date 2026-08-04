<template>
  <div class="app-container ticket-handle">
    <el-page-header @back="goBack" content="工单处理" class="mb16" />

    <el-card v-loading="loading" shadow="never">
      <template v-if="detail">
        <div class="handle-head">
          <div class="handle-head-main">
            <span class="handle-title">{{ detail.title }}</span>
            <el-tag size="small" :type="ticketStatusTag(detail.status)">{{ detail.statusLabel }}</el-tag>
          </div>
          <el-button link type="primary" @click="goInfo">查看工单信息</el-button>
        </div>

        <el-divider content-position="left">对话记录</el-divider>

        <TicketChatHistory
          ref="chatRef"
          :messages="detail.messages || []"
          height="480px"
        />

        <div v-if="canReply" class="reply-box">
          <el-alert
            v-if="detail.status === 0 && !detail.assignedStaffId"
            type="info"
            :closable="false"
            show-icon
            title="首次回复将自动认领该工单"
            class="reply-hint"
          />
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="输入回复内容"
          />
          <div class="reply-actions">
            <el-button
              type="primary"
              :loading="replySubmitting"
              @click="submitReplyLocked"
              v-hasPermi="['system:ticket:reply']"
            >发送回复</el-button>
            <el-button
              type="success"
              :loading="completeSubmitting"
              @click="submitCompleteLocked"
              v-hasPermi="['system:ticket:complete']"
            >标记完成</el-button>
          </div>
        </div>
        <el-alert v-else type="info" :closable="false" title="工单已完成，无法继续回复" />
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLockedFn } from '@/hooks/useLockedFn'
import { ticketStatusTag } from '@/constants/ticket'
import { getTicketDetail, replyTicket, completeTicket } from '@/api/ticket'
import { useTicketStore } from '@/store/modules/ticket'
import { onNotifyMessage } from '@/utils/notifySocket'
import TicketChatHistory from '../components/TicketChatHistory.vue'

const route = useRoute()
const router = useRouter()
const ticketStore = useTicketStore()
const ticketId = Number(route.params.id)

const loading = ref(false)
const detail = ref(null)
const replyContent = ref('')
const chatRef = ref(null)

const canReply = computed(() => detail.value && detail.value.status !== 2)

let offNotify = null

onMounted(() => {
  void loadDetail()
  offNotify = onNotifyMessage((payload) => {
    if (payload?.type !== 'ticket_message' || payload.ticketId !== ticketId) {
      return
    }
    if (!detail.value) {
      return
    }
    const exists = detail.value.messages?.some((item) => item.messageId === payload.messageId)
    if (exists) {
      return
    }
    detail.value.messages = [
      ...(detail.value.messages || []),
      {
        messageId: payload.messageId,
        senderType: payload.senderType,
        senderLabel: payload.senderLabel,
        senderId: payload.senderId,
        senderAvatar: payload.senderAvatar,
        content: payload.content,
        createTime: payload.createTime
      }
    ]
    void chatRef.value?.scrollToBottom(true)
  })
})

onUnmounted(() => {
  offNotify?.()
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getTicketDetail(ticketId)
    detail.value = res.data
    await ticketStore.refreshUnreadSummary()
    await chatRef.value?.scrollToBottom(true)
  } catch (e) {
    ElMessage.error(e?.msg || e?.message || '加载工单失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function goInfo() {
  router.push(`/ticket/info/${ticketId}`)
}

async function submitReply() {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await replyTicket(ticketId, { content })
  replyContent.value = ''
  ElMessage.success('回复成功')
  await loadDetail()
}

async function submitComplete() {
  await completeTicket(ticketId)
  ElMessage.success('工单已完成')
  await loadDetail()
}

const { run: submitReplyLocked, loading: replySubmitting } = useLockedFn(submitReply)
const { run: submitCompleteLocked, loading: completeSubmitting } = useLockedFn(submitComplete)
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.handle-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.handle-head-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.handle-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.reply-hint {
  margin-bottom: 0;
}

.reply-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}

.reply-actions {
  display: flex;
  gap: 12px;
}
</style>
