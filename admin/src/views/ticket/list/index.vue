<template>
  <div class="app-container">
    <div class="page-head">
      <span class="page-title">工单列表</span>
    </div>

    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="用户ID">
        <el-input v-model="filterForm.userId" placeholder="用户ID" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
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
      <el-table-column label="标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="96" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="ticketStatusTag(row.status)">{{ ticketStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理人ID" prop="assignedStaffId" width="110" align="center">
        <template #default="{ row }">{{ row.assignedStaffId || '—' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 2"
            link
            type="primary"
            :disabled="row.status !== 0 || claimLoading"
            :loading="claimLoading"
            @click="handleProcessLocked(row)"
            v-hasPermi="['system:ticket:claim']"
          >处理</el-button>
          <el-button
            link
            type="primary"
            @click="goInfo(row.ticketId)"
            v-hasPermi="['system:ticket:query']"
          >详情</el-button>
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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { useLockedFn } from '@/hooks/useLockedFn'
import { formatDateTime } from '@/utils/dateTime'
import { ticketStatusLabel, ticketStatusTag } from '@/constants/ticket'
import { claimTicket, listTicket } from '@/api/ticket'

const router = useRouter()
const filterDefaults = { userId: '', status: undefined }

const table = useTable(
  (params) =>
    listTicket({
      userId: params.userId ? Number(params.userId) : undefined,
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

function goInfo(ticketId) {
  router.push(`/ticket/info/${ticketId}`)
}

async function handleProcess(row) {
  if (row.status !== 0) return
  try {
    await claimTicket(row.ticketId)
    ElMessage.success('认领成功')
    router.push(`/ticket/handle/${row.ticketId}`)
  } catch (e) {
    ElMessage.error(e?.msg || e?.message || '认领失败')
  }
}

const { run: handleProcessLocked, loading: claimLoading } = useLockedFn(handleProcess)

onMounted(() => initAndLoad())
</script>

<style scoped>
.page-head {
  margin-bottom: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
