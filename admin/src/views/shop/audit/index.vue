<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺名称">
        <el-input v-model="filterForm.shopName" placeholder="模糊搜索" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="法人姓名">
        <el-input v-model="filterForm.legalPerson" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="店铺ID" prop="id" width="100" align="center" />
      <el-table-column label="店铺编码" prop="shopCode" width="150" show-overflow-tooltip />
      <el-table-column label="店铺名称" prop="shopName" min-width="140" show-overflow-tooltip />
      <el-table-column label="法人" prop="legalPerson" width="100" show-overflow-tooltip />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="auditTagType(row.auditStatus)">{{ auditLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goAudit(row)" v-hasPermi="['system:shop:audit']">审核</el-button>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { listShop } from '@/api/shop/shop'
import { formatDateTime } from '@/utils/dateTime'

const router = useRouter()
const DEFAULT_FILTERS = { shopName: '', legalPerson: '' }
const dateRange = ref([])

async function fetchApi(params) {
  // 待审列表：auditStatus 传非 1（0），后端查询待审(0)或驳回(2)
  return listShop({
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    shopName: params.shopName,
    legalPerson: params.legalPerson,
    auditStatus: 0,
    dateRange: dateRange.value
  })
}

const table = useTable(fetchApi, {
  defaultParams: { ...DEFAULT_FILTERS },
  refreshOnActivated: true
})

const { loading, tableData, total, queryParams, getList } = table
const { filterForm, onSearch: handleQuery, onReset: resetFilters, initAndLoad } = useFilterTable(
  table,
  DEFAULT_FILTERS
)

function resetQuery() {
  dateRange.value = []
  resetFilters()
}

function auditLabel(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return '—'
}

function auditTagType(s) {
  if (s === 0) return 'warning'
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'info'
}

function goAudit(row) {
  router.push(`/shop/audit/exec/${row.id}`)
}

onMounted(() => initAndLoad())
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
