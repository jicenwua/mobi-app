<template>
  <div class="app-container">
    <div class="page-head">
      <span class="page-title">我的工单</span>
    </div>

    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="处理中" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column label="工单ID" prop="ticketId" width="96" align="center" />
      <el-table-column label="用户ID" prop="userId" width="96" align="center" />
      <el-table-column label="昵称" prop="nickname" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.nickname || '—' }}</template>
      </el-table-column>
      <el-table-column label="标题" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="title-cell">
            <span v-if="row.unreadCount > 0" class="unread-dot" />
            <span>{{ row.title }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="未读" width="72" align="center">
        <template #default="{ row }">
          <el-badge v-if="row.unreadCount > 0" :value="row.unreadCount" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="warning">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="goHandle(row.ticketId)"
            v-hasPermi="['system:ticket:query']"
          >处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup>
import { onActivated, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { formatDateTime } from '@/utils/dateTime'
import { listMyTicket } from '@/api/ticket'
import { useTicketStore } from '@/store/modules/ticket'

const router = useRouter()
const ticketStore = useTicketStore()
const filterDefaults = { status: undefined }

const table = useTable(
  (params) =>
    listMyTicket({
      status: params.status,
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }),
  {
    defaultParams: { ...filterDefaults },
    refreshOnActivated: true
  }
)

const { loading, tableData, total, queryParams, getList } = table
const { filterForm, onSearch, onReset, initAndLoad } = useFilterTable(table, filterDefaults)

function statusLabel(s) {
  if (s === 2) return '已完成'
  return '处理中'
}

function goHandle(ticketId) {
  router.push(`/ticket/handle/${ticketId}`)
}

function refreshTicketSummary() {
  void ticketStore.refreshUnreadSummary()
}

onMounted(() => {
  initAndLoad()
  refreshTicketSummary()
})

onActivated(() => {
  refreshTicketSummary()
})
</script>

<style scoped>
.page-head {
  margin-bottom: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.title-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}
</style>
