<template>
  <div class="app-container">
    <el-form :model="filterForm" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="日志类型" prop="logType">
        <el-select v-model="filterForm.logType" placeholder="请选择日志类型" clearable style="width: 150px">
          <el-option label="登录日志" value="login" />
          <el-option label="操作日志" value="oper" />
        </el-select>
      </el-form-item>
      <el-form-item label="系统模块" prop="title" v-if="filterForm.logType !== 'login'">
        <el-input
          v-model="filterForm.title"
          placeholder="请输入系统模块"
          clearable
          @keyup.enter="handleQuery"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="操作人员" prop="userName">
        <el-input
          v-model="filterForm.userName"
          placeholder="请输入操作人员"
          clearable
          @keyup.enter="handleQuery"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="登录地址" prop="ipaddr" v-if="filterForm.logType !== 'oper'">
        <el-input
          v-model="filterForm.ipaddr"
          placeholder="请输入登录地址"
          clearable
          @keyup.enter="handleQuery"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="类型" prop="businessType" v-if="filterForm.logType !== 'login'">
        <el-select v-model="filterForm.businessType" placeholder="请选择操作类型" clearable style="width: 150px">
          <el-option label="登录" :value="1" />
          <el-option label="更新" :value="2" />
          <el-option label="删除" :value="3" />
          <el-option label="添加" :value="4" />
          <el-option label="其他" :value="100" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="filterForm.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间" style="width: 308px">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleBatchDelete"
          v-hasPermi="['system:log:remove']"
        >批量删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Unlock"
          @click="handleUnlock"
          v-hasPermi="['system:logininfor:unlock']"
          v-if="queryParams.logType === 'login'"
        >解锁</el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="tableData"
      @selection-change="handleSelectionChange"
      border
    >
      <el-table-column type="selection" width="48" align="center" fixed="left" />
      <el-table-column label="日志编号" align="center" prop="logId" width="88" />
      <el-table-column label="日志类型" align="center" width="100">
        <template #default="scope">
          <el-tag :type="(scope.row.logTypeRaw === 1 || scope.row.logType === 'login') ? 'success' : 'primary'" size="small">
            {{ logTypeTableLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status == 0 ? 'success' : 'danger'" size="small">
            {{ scope.row.status == 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="88">
        <template #default="scope">
          <span>{{ scope.row.userId != null ? scope.row.userId : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户账号" align="center" prop="username" min-width="100" show-overflow-tooltip />
      <el-table-column label="系统模块" align="center" prop="module" min-width="110" show-overflow-tooltip />
      <el-table-column label="操作类型" align="center" prop="operation" width="100" show-overflow-tooltip />
      <el-table-column label="方法" align="center" prop="method" min-width="120" show-overflow-tooltip />
      <el-table-column label="请求方式" align="center" prop="requestMethod" width="90" show-overflow-tooltip />
      <el-table-column label="请求地址" align="center" prop="url" min-width="120" show-overflow-tooltip />
      <el-table-column label="请求参数" align="center" prop="params" min-width="120" show-overflow-tooltip />
      <el-table-column label="返回结果" align="center" prop="result" min-width="120" show-overflow-tooltip />
      <el-table-column label="错误信息" align="center" prop="errorMsg" min-width="120" show-overflow-tooltip />
      <el-table-column label="执行耗时" align="center" width="92">
        <template #default="scope">
          <span>{{ execTimeMs(scope.row) }}ms</span>
        </template>
      </el-table-column>
      <el-table-column label="IP" align="center" prop="ip" width="130" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.ip != null && scope.row.ip !== '' ? scope.row.ip : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="地点" align="center" prop="location" min-width="100" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.location != null && scope.row.location !== '' ? scope.row.location : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatDateTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="88" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
            v-hasPermi="['system:log:query']"
          >详细</el-button>
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

    <el-dialog
      title="日志详细"
      v-model="detailVisible"
      width="920px"
      append-to-body
      destroy-on-close
    >
      <el-form :model="detailData" label-width="100px" class="log-detail-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="日志编号">{{ detailData.logId ?? detailData.id ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日志类型">{{ logTypeDetailLabel(detailData) }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-tag :type="detailData.status == 0 ? 'success' : 'danger'" size="small">
                {{ detailData.status == 0 ? '成功' : '失败' }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户ID">{{ detailData.userId != null ? detailData.userId : '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户账号">{{ detailData.username ?? detailData.userName ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统模块">{{ detailData.module ?? detailData.title ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作类型">{{ detailData.operation ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方法">{{ detailData.method ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求方式">{{ detailData.requestMethod ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求地址">{{ detailData.url ?? detailData.operUrl ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求参数">
              <div class="detail-long-field">{{ formatDetailField(detailData.params ?? detailData.operParam) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="返回结果">
              <div class="detail-long-field">{{ formatDetailField(detailData.result ?? detailData.jsonResult) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="错误信息">
              <div class="detail-long-field">{{ formatDetailField(detailData.errorMsg) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行耗时">{{ detailData.executeTime ?? detailData.costTime ?? 0 }}ms</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建时间">{{ formatDateTime(detailData.createTime) }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IP">{{ detailData.ip ?? detailData.ipaddr ?? '—' }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点">{{ detailData.location ?? detailData.loginLocation ?? '—' }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import {
  listLog,
  mapSysLogToTableRow,
  buildLogListParams,
  batchDelOperLog,
  batchDelLoginLog,
  unlockLoginFail
} from '@/api/system/log'
import { formatDateTime } from '@/utils/dateTime'

/** 表单草稿：仅点击「搜索」后同步到 queryParams 并请求，避免改下拉即改列表/列 */
const DEFAULT_LOG_FILTERS = {
  logType: '',
  title: '',
  userName: '',
  ipaddr: '',
  businessType: undefined,
  status: ''
}

const dateRange = ref([])

async function fetchLogList(params) {
  const apiParams = buildLogListParams({
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    logType: params.logType,
    status: params.status,
    userName: params.userName,
    title: params.title,
    ipaddr: params.ipaddr,
    businessType: params.businessType,
    dateRange: dateRange.value
  })
  const res = await listLog(apiParams)
  return {
    ...res,
    data: (Array.isArray(res.data) ? res.data : []).map(mapSysLogToTableRow)
  }
}

const table = useTable(fetchLogList, {
  defaultParams: { ...DEFAULT_LOG_FILTERS },
  refreshOnActivated: true
})

const { loading, tableData, total, queryParams, getList } = table
const { filterForm, onSearch: handleQuery, onReset: resetFilters, initAndLoad } = useFilterTable(
  table,
  DEFAULT_LOG_FILTERS
)

function resetQuery() {
  dateRange.value = []
  resetFilters()
}
const multiple = ref(true)
const ids = ref([])
const detailVisible = ref(false)
const detailData = ref({})

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function logTypeTableLabel(row) {
  const n = row.logTypeRaw
  if (n === 1) return '登录日志'
  if (n === 2) return '操作日志'
  if (row.logType === 'login') return '登录日志'
  if (row.logType === 'oper') return '操作日志'
  return '—'
}

function logTypeDetailLabel(row) {
  const n = row.logTypeRaw
  if (n === 1) return '1（登录日志）'
  if (n === 2) return '2（操作日志）'
  if (row.logType === 'login') return '1（登录日志）'
  if (row.logType === 'oper') return '2（操作日志）'
  return '—'
}

function formatDetailField(val) {
  if (val === null || val === undefined || val === '') return '—'
  return typeof val === 'string' ? val : String(val)
}

function execTimeMs(row) {
  const v = row.executeTime ?? row.costTime
  if (v == null || v === '') return 0
  const n = Number(v)
  return Number.isNaN(n) ? 0 : n
}

function handleDetail(row) {
  detailData.value = { ...row }
  detailVisible.value = true
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确认删除选中的${ids.value.length}条日志吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    if (queryParams.logType === 'login') {
      await batchDelLoginLog(ids.value)
    } else {
      await batchDelOperLog(ids.value)
    }
    
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
}

async function handleUnlock() {
  try {
    await ElMessageBox.confirm('确认解锁选中的用户吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    for (const id of ids.value) {
      await unlockLoginFail(id)
    }
    
    ElMessage.success('解锁成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(() => initAndLoad())
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 8px;
}

.detail-long-field {
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.5;
  max-width: 100%;
}
</style>
