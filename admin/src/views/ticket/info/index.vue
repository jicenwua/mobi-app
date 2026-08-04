<template>
  <div class="app-container ticket-info">
    <el-page-header @back="goBack" content="工单详情" class="mb16" />

    <el-card v-loading="loading" shadow="never">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="工单ID">{{ detail.ticketId }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="问题描述">
            <span class="description-text">{{ detail.description || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发送用户">
            {{ detail.nickname || '—' }}（ID: {{ detail.userId }}）
          </el-descriptions-item>
          <el-descriptions-item label="发起时间">
            {{ formatDateTime(detail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="ticketStatusTag(detail.status)">{{ detail.statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理人ID">
            {{ detail.assignedStaffId || '未分配' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.updateTime" label="最近更新">
            {{ formatDateTime(detail.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === 0" class="info-actions">
          <el-button
            type="primary"
            @click="goHandle"
            v-hasPermi="['system:ticket:reply']"
          >前往处理</el-button>
        </div>
        <el-alert
          v-else-if="detail.status === 1"
          type="info"
          :closable="false"
          show-icon
          title="该工单正在处理中，请前往「我的工单」继续跟进"
        />

        <template v-if="detail.status === 2">
          <el-divider content-position="left">客服聊天记录</el-divider>
          <TicketChatHistory :messages="detail.messages || []" />
        </template>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '@/utils/dateTime'
import { ticketStatusTag } from '@/constants/ticket'
import { getTicketDetail } from '@/api/ticket'
import TicketChatHistory from '../components/TicketChatHistory.vue'

const route = useRoute()
const router = useRouter()
const ticketId = Number(route.params.id)

const loading = ref(false)
const detail = ref(null)

onMounted(() => {
  void loadDetail()
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getTicketDetail(ticketId)
    detail.value = res.data
  } catch (e) {
    ElMessage.error(e?.msg || e?.message || '加载工单详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function goHandle() {
  router.push(`/ticket/handle/${ticketId}`)
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.description-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.info-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-alert) {
  margin-top: 20px;
}

:deep(.el-divider) {
  margin: 24px 0 16px;
}
</style>
