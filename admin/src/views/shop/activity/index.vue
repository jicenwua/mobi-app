<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="留空查全部" clearable style="width: 140px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="活动状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="未开始" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
          <el-option label="手动停止" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动类型">
        <el-select v-model="filterForm.activityType" placeholder="全部" clearable style="width: 120px">
          <el-option label="充值满赠" :value="1" />
          <el-option label="消费满赠" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:shop:activity:add']">新增活动</el-button>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="活动ID" prop="activityId" width="90" align="center" />
      <el-table-column label="店铺" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="shop-cell">
            <span class="shop-cell__name">{{ row.shopName || '—' }}</span>
            <span class="shop-cell__id">ID: {{ row.shopId ?? '—' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="活动名称" prop="activityName" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">{{ row.activityType === 1 ? '充值满赠' : '消费满赠' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="activityStatusTag(row.status)" size="small">{{ activityStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)" v-hasPermi="['system:shop:activity:query']">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['system:shop:activity:edit']">修改</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 1"
            link
            type="warning"
            @click="handleStop(row)"
            v-hasPermi="['system:shop:activity:edit']"
          >手动停止</el-button>
          <el-button
            v-if="row.status === 3"
            link
            type="success"
            @click="handleEnable(row)"
            v-hasPermi="['system:shop:activity:edit']"
          >启用</el-button>
          <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['system:shop:activity:remove']">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="活动详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="店铺">{{ detail.shopName || '—' }}（ID: {{ detail.shopId ?? '—' }}）</el-descriptions-item>
        <el-descriptions-item label="活动名称">{{ detail.activityName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.activityType === 1 ? '充值满赠' : '消费满赠' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="activityStatusTag(detail.status)" size="small">{{ activityStatusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail?.rules?.length" :data="detail.rules" border class="mt12" size="small">
        <el-table-column label="门槛积分" prop="thresholdAmount" align="center" />
        <el-table-column label="赠送积分" prop="giftPoints" align="center" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="form.activityId ? '修改活动' : '新增活动'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item v-if="form.shopId" label="所属店铺">
          <span class="form-shop-hint">店铺 ID：{{ form.shopId }}</span>
        </el-form-item>
        <el-form-item label="活动名称" required>
          <el-input v-model="form.activityName" />
        </el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="form.activityType" style="width: 100%">
            <el-option label="充值满赠" :value="1" />
            <el-option label="消费满赠" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="满赠规则">
          <div class="rules-panel">
            <div class="rules-panel__head">
              <span class="rules-panel__title">满赠阶梯</span>
              <el-button type="primary" link @click="addRule">+ 添加规则</el-button>
            </div>
            <div v-if="!form.rules.length" class="rules-empty">暂无规则，点击右上角添加</div>
            <div v-else class="rules-list">
              <div v-for="(rule, idx) in form.rules" :key="idx" class="rule-card">
                <div class="rule-card__head">
                  <span class="rule-card__index">规则 {{ idx + 1 }}</span>
                  <el-button
                    v-if="form.rules.length > 1"
                    link
                    type="danger"
                    @click="form.rules.splice(idx, 1)"
                  >删除</el-button>
                </div>
                <div class="rule-card__body">
                  <div class="rule-field">
                    <span class="rule-field__label">满</span>
                    <el-input-number v-model="rule.thresholdAmount" :min="1" controls-position="right" class="rule-field__input" />
                    <span class="rule-field__unit">积分</span>
                  </div>
                  <span class="rule-arrow">→</span>
                  <div class="rule-field">
                    <span class="rule-field__label">赠</span>
                    <el-input-number v-model="rule.giftPoints" :min="1" controls-position="right" class="rule-field__input" />
                    <span class="rule-field__unit">积分</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFormLocked">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { useLockedFn } from '@/hooks/useLockedFn'
import { useConfirmAction } from '@/hooks/useConfirmAction'
import { formatDateTime } from '@/utils/dateTime'
import { shopIdFromRoute, parseOptionalShopId, parseRequiredShopId } from '@/utils/shopRoute'
import { listShopActivity, getShopActivity, saveShopActivity, delShopActivity, stopShopActivity, enableShopActivity } from '@/api/shop/activity'

const route = useRoute()
const filterDefaults = {
  shopId: shopIdFromRoute(route),
  status: undefined,
  activityType: undefined
}
const detailVisible = ref(false)
const detail = ref(null)
const formVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  activityId: null,
  shopId: null,
  activityName: '',
  activityType: 1,
  startTime: '',
  endTime: '',
  rules: [{ thresholdAmount: 100, giftPoints: 10 }]
})

const table = useTable((params) => {
  return listShopActivity({
    shopId: parseOptionalShopId(params.shopId),
    status: params.status,
    activityType: params.activityType,
    pageNum: params.pageNum,
    pageSize: params.pageSize
  })
}, { defaultParams: { ...filterDefaults }, refreshOnActivated: true })

const { loading, tableData, total, queryParams, getList } = table
const { filterForm, onSearch, onReset, initAndLoad } = useFilterTable(table, filterDefaults, {
  onResetExtra: () => {
    filterForm.shopId = shopIdFromRoute(route)
  }
})

const confirmAction = useConfirmAction({ onSuccess: getList })

function activityStatusLabel(status) {
  if (status === 0) return '未开始'
  if (status === 1) return '进行中'
  if (status === 2) return '已结束'
  if (status === 3) return '手动停止'
  return '—'
}

function activityStatusTag(status) {
  if (status === 1) return 'success'
  if (status === 0) return 'info'
  if (status === 3) return 'warning'
  return 'info'
}

function resetForm() {
  form.activityId = null
  form.shopId = parseOptionalShopId(filterForm.shopId) ?? null
  form.activityName = ''
  form.activityType = 1
  form.startTime = ''
  form.endTime = ''
  form.rules = [{ thresholdAmount: 100, giftPoints: 10 }]
}

function handleAdd() {
  const shopId = parseRequiredShopId(filterForm.shopId)
  if (!shopId) {
    ElMessage.warning('新增活动请先填写有效的店铺ID')
    return
  }
  resetForm()
  form.shopId = shopId
  formVisible.value = true
}

async function handleDetail(row) {
  const res = await getShopActivity(row.activityId)
  detail.value = res.data
  detailVisible.value = true
}

async function handleEdit(row) {
  const res = await getShopActivity(row.activityId)
  const d = res.data
  form.activityId = d.activityId
  form.shopId = d.shopId
  form.activityName = d.activityName
  form.activityType = d.activityType
  form.startTime = d.startTime
  form.endTime = d.endTime
  form.rules = (d.rules || []).map((r) => ({ thresholdAmount: r.thresholdAmount, giftPoints: r.giftPoints }))
  if (!form.rules.length) form.rules = [{ thresholdAmount: 100, giftPoints: 10 }]
  formVisible.value = true
}

function addRule() {
  form.rules.push({ thresholdAmount: 100, giftPoints: 10 })
}

async function submitForm() {
  if (!form.activityName || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (!form.shopId) {
    ElMessage.warning('缺少店铺ID')
    return
  }
  await saveShopActivity({ ...form })
  ElMessage.success('保存成功')
  formVisible.value = false
  getList()
}

const { run: submitFormLocked, loading: submitLoading } = useLockedFn(submitForm)

const handleDelete = (row) =>
  confirmAction.run({
    message: `确认删除活动「${row.activityName}」？`,
    action: () => delShopActivity(row.activityId),
    successMsg: '已删除'
  })

const handleStop = (row) =>
  confirmAction.run({
    message: `确认手动停止活动「${row.activityName}」？`,
    action: () => stopShopActivity(row.activityId),
    successMsg: '活动已停止'
  })

const handleEnable = (row) =>
  confirmAction.run({
    message: `启用后将按当前活动时间重新生效，确认启用「${row.activityName}」？`,
    action: () => enableShopActivity(row.activityId),
    successMsg: '活动已启用'
  })

onMounted(() => initAndLoad())
</script>

<style scoped>
.shop-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}
.shop-cell__name {
  color: var(--el-text-color-primary);
}
.shop-cell__id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.mt12 {
  margin-top: 12px;
}
.form-shop-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.rules-panel {
  width: 100%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  overflow: hidden;
}
.rules-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.rules-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.rules-empty {
  padding: 24px 14px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.rules-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.rule-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.rule-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
}
.rule-card__index {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.rule-card__body {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  flex-wrap: wrap;
}
.rule-field {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rule-field__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  min-width: 16px;
}
.rule-field__input {
  width: 120px;
}
.rule-field__unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.rule-arrow {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
