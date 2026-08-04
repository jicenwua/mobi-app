<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="留空查全部" clearable style="width: 140px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:shop:coupon:add']">新增折扣券</el-button>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="模板ID" prop="templateId" width="90" align="center" />
      <el-table-column label="店铺" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="shop-cell">
            <span class="shop-cell__name">{{ row.shopName || '—' }}</span>
            <span class="shop-cell__id">ID: {{ row.shopId ?? '—' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="couponName" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">{{ row.type === 1 ? '折扣' : '满减' }}</template>
      </el-table-column>
      <el-table-column label="门槛/优惠" min-width="120" align="center">
        <template #default="{ row }">
          满{{ row.thresholdAmount }}{{ row.type === 1 ? '享' : '减' }}{{ row.discountValue }}{{ row.type === 1 ? '折' : '元' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="couponPhaseTag(row)" size="small">{{ couponPhaseLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已发/总量" width="120" align="center">
        <template #default="{ row }">{{ formatIssuedTotal(row) }}</template>
      </el-table-column>
      <el-table-column label="发放期" min-width="200" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.distributionStartTime) }} ~ {{ formatDateTime(row.distributionEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['system:shop:coupon:edit']">修改</el-button>
          <el-button
            v-if="canStop(row)"
            link
            type="warning"
            @click="handleStop(row)"
            v-hasPermi="['system:shop:coupon:edit']"
          >手动停止</el-button>
          <el-button
            v-if="row.distributionStatus === 4"
            link
            type="success"
            @click="handleResume(row)"
            v-hasPermi="['system:shop:coupon:edit']"
          >恢复发放</el-button>
          <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['system:shop:coupon:remove']">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="form.templateId ? '修改折扣券' : '新增折扣券'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item v-if="form.shopId" label="所属店铺">
          <span class="form-shop-hint">店铺 ID：{{ form.shopId }}</span>
        </el-form-item>
        <el-form-item label="券名称" required>
          <el-input v-model="form.couponName" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="折扣" :value="1" />
            <el-option label="满减" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="满额门槛" required>
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="form.type === 1 ? '折扣(如85)' : '减免金额'" required>
          <el-input-number v-model="form.discountValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="form.validDays" :min="0" placeholder="空=永久" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number
            v-model="form.totalQuantity"
            :controls="true"
            controls-position="right"
            placeholder="留空表示无限"
            style="width: 100%"
          />
          <div class="field-hint">不填表示无限发放</div>
        </el-form-item>
        <el-form-item label="发放开始">
          <el-date-picker v-model="form.distributionStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发放结束">
          <el-date-picker v-model="form.distributionEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
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
import {
  listShopCoupon,
  addShopCoupon,
  updateShopCoupon,
  delShopCoupon,
  stopShopCoupon,
  resumeShopCoupon
} from '@/api/shop/coupon'

const route = useRoute()
const filterDefaults = { shopId: shopIdFromRoute(route) }
const formVisible = ref(false)

const form = reactive({
  templateId: null,
  shopId: null,
  couponName: '',
  type: 1,
  thresholdAmount: 100,
  discountValue: 85,
  validDays: null,
  totalQuantity: null,
  distributionStartTime: '',
  distributionEndTime: ''
})

const table = useTable((params) => {
  return listShopCoupon({
    shopId: parseOptionalShopId(params.shopId),
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

function formatIssuedTotal(row) {
  const issued = row.issuedQuantity || 0
  if (row.totalQuantity == null) return `${issued}/无限`
  return `${issued}/${row.totalQuantity}`
}

function couponPhaseLabel(row) {
  if (row.distributionStatus === 0) return '禁用'
  if (row.distributionStatus === 1) return '未开始'
  if (row.distributionStatus === 2) return '发放中'
  if (row.distributionStatus === 3) return '已结束'
  if (row.distributionStatus === 4) return '手动停止'
  return '—'
}

function couponPhaseTag(row) {
  if (row.distributionStatus === 2) return 'success'
  if (row.distributionStatus === 1) return 'info'
  if (row.distributionStatus === 4) return 'warning'
  return 'info'
}

function canStop(row) {
  return row.distributionStatus === 1 || row.distributionStatus === 2
}

function resetForm() {
  form.templateId = null
  form.shopId = parseOptionalShopId(filterForm.shopId) ?? null
  form.couponName = ''
  form.type = 1
  form.thresholdAmount = 100
  form.discountValue = 85
  form.validDays = null
  form.totalQuantity = null
  form.distributionStartTime = ''
  form.distributionEndTime = ''
}

function handleAdd() {
  const shopId = parseRequiredShopId(filterForm.shopId)
  if (!shopId) {
    ElMessage.warning('新增折扣券请先填写有效的店铺ID')
    return
  }
  resetForm()
  form.shopId = shopId
  formVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, {
    templateId: row.templateId,
    shopId: row.shopId,
    couponName: row.couponName,
    type: row.type,
    thresholdAmount: row.thresholdAmount,
    discountValue: row.discountValue,
    validDays: row.validDays,
    totalQuantity: row.totalQuantity ?? null,
    distributionStartTime: row.distributionStartTime,
    distributionEndTime: row.distributionEndTime
  })
  formVisible.value = true
}

function buildPayload() {
  return {
    ...form,
    totalQuantity: form.totalQuantity == null || form.totalQuantity === '' ? null : form.totalQuantity,
    validDays: form.validDays || null
  }
}

async function submitForm() {
  if (!form.couponName) {
    ElMessage.warning('请填写券名称')
    return
  }
  if (!form.shopId) {
    ElMessage.warning('缺少店铺ID')
    return
  }
  const payload = buildPayload()
  if (form.templateId) {
    await updateShopCoupon(payload)
  } else {
    await addShopCoupon(payload)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  getList()
}

const { run: submitFormLocked, loading: submitLoading } = useLockedFn(submitForm)

const handleDelete = (row) =>
  confirmAction.run({
    message: `确认删除「${row.couponName}」？`,
    action: () => delShopCoupon(row.templateId),
    successMsg: '已删除'
  })

const handleStop = (row) =>
  confirmAction.run({
    message: `确认手动停止「${row.couponName}」的发放？`,
    action: () => stopShopCoupon(row.templateId),
    successMsg: '已停止发放'
  })

const handleResume = (row) =>
  confirmAction.run({
    message: `恢复后将按当前发放时间与库存继续，确认恢复「${row.couponName}」？`,
    action: () => resumeShopCoupon(row.templateId),
    successMsg: '已恢复发放'
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
.form-shop-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
