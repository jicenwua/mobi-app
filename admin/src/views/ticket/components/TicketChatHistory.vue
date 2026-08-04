<template>
  <div
    ref="scrollRef"
    class="ticket-chat-history"
    :style="height ? { height, maxHeight: height } : undefined"
    @scroll="onScroll"
  >
    <el-empty v-if="!messages.length" description="暂无聊天记录" :image-size="72" />
    <div
      v-for="msg in messages"
      :key="msg.messageId"
      class="message-row"
      :class="isSelfMessage(msg) ? 'message-row--self' : 'message-row--other'"
    >
      <div v-if="!isSelfMessage(msg)" class="message-side">
        <span class="message-name">{{ msg.senderLabel }}</span>
        <el-avatar :size="36" :src="resolveAvatar(msg)" class="message-avatar" />
      </div>
      <div class="message-main">
        <div class="message-bubble">
          <div class="message-body">{{ msg.content }}</div>
        </div>
        <div class="message-time">{{ formatDateTime(msg.createTime) }}</div>
      </div>
      <div v-if="isSelfMessage(msg)" class="message-side">
        <span class="message-name">{{ msg.senderLabel }}</span>
        <el-avatar :size="36" :src="resolveAvatar(msg)" class="message-avatar" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { formatDateTime } from '@/utils/dateTime'
import { resolveTicketAvatar } from '@/constants/ticket'
import { useChatAutoScroll } from '@/hooks/useChatAutoScroll'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  /** 固定高度，如工单处理页 '480px' */
  height: {
    type: String,
    default: ''
  }
})

const { scrollRef, onScroll, scrollToBottom } = useChatAutoScroll()

onMounted(() => {
  void scrollToBottom(true)
})

watch(
  () => props.messages.length,
  () => {
    void scrollToBottom(true)
  }
)

function isSelfMessage(msg) {
  return msg.senderType === 2
}

function resolveAvatar(msg) {
  return resolveTicketAvatar(msg.senderAvatar, msg.senderType === 2)
}

defineExpose({ scrollToBottom })
</script>

<style scoped>
.ticket-chat-history {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 320px;
  max-height: 560px;
  overflow-y: auto;
  padding: 12px 8px;
  box-sizing: border-box;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 78%;
}

.message-row--other {
  align-self: flex-start;
}

.message-row--self {
  align-self: flex-end;
}

.message-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  width: 52px;
}

.message-name {
  font-size: 11px;
  color: #909399;
  text-align: center;
  line-height: 1.3;
  max-width: 52px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-avatar {
  flex-shrink: 0;
}

.message-main {
  min-width: 0;
  max-width: calc(100% - 62px);
}

.message-row--self .message-main {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  min-height: 40px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f4f4f5;
  box-sizing: border-box;
}

.message-row--self .message-bubble {
  background: #ecf5ff;
}

.message-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
  color: #303133;
}

.message-time {
  margin-top: 4px;
  font-size: 11px;
  color: #c0c4cc;
  text-align: right;
  line-height: 1.4;
}
</style>
