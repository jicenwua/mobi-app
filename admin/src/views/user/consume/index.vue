<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="可选，不填查全部" clearable style="width: 140px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="积分类型">
        <el-select v-model="filterForm.actionType" placeholder="全部" clearable style="width: 120px">
          <el-option label="增加" :value="1" />
          <el-option label="消耗" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="已核销" :value="1" />
          <el-option label="订单取消" :value="2" />
          <el-option label="订单过期" :value="3" />
          <el-option label="积分充值" :value="4" />
          <el-option label="后台添加" :value="6" />
          <el-option label="后台扣除" :value="7" />
        </el-select>
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="filterForm.nickname" placeholder="模糊搜索" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="filterForm.phone" placeholder="模糊搜索" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column label="记录ID" prop="logId" width="96" align="center" />
      <el-table-column label="店铺ID" prop="shopId" width="96" align="center" />
      <el-table-column label="店铺" prop="shopName" min-width="120" show-overflow-tooltip />
      <el-table-column label="昵称" prop="nickname" min-width="120" show-overflow-tooltip />
      <el-table-column label="手机号" prop="phone" width="130" />
      <el-table-column label="积分类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.actionType === 1 ? 'success' : 'warning'">
            {{ actionTypeLabel(row.actionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="108" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>
      <el-table-column label="消费时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.consumeTime || row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="消费积分" prop="consumePoints" width="100" align="center" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:user:consume:query']" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="消费记录详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border v-loading="detailLoading" class="consume-detail">
        <el-descriptions-item label="昵称">
          <span class="detail-emphasis">{{ detail.nickname || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          <span :class="{ 'detail-muted': !displayPhone(detail.phone) }">{{ displayPhone(detail.phone) || '未绑定' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="记录ID">
          <span class="detail-id">{{ detail.logId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="积分类型">
          <el-tag size="small" :type="detail.actionType === 1 ? 'success' : 'warning'">
            {{ actionTypeLabel(detail.actionType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag size="small" :type="statusTag(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.remark" label="备注">
          <span class="detail-remark">{{ detail.remark }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="消费时间">
          <span class="detail-time">{{ formatDateTime(detail.consumeTime || detail.createTime) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="基础积分变动">
          <span :class="pointsChangeClass(detail.changeBase)">{{ formatPointsChange(detail.changeBase) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="赠送积分变动">
          <span :class="pointsChangeClass(detail.changeBonus)">{{ formatPointsChange(detail.changeBonus) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更后基础积分">
          <span class="detail-balance">{{ detail.afterBasePoints ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更后赠送积分">
          <span class="detail-balance">{{ detail.afterBonusPoints ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="实付积分">
          <span class="detail-consume">{{ detail.consumePoints ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.originalPoints != null" label="商品原价合计">
          <span class="detail-muted">{{ detail.originalPoints }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.couponName" label="优惠券">
          <span class="detail-coupon">{{ detail.couponName }}</span>
          <el-tag size="small" type="info" class="coupon-type-tag">{{ couponTypeLabel(detail.couponType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.couponDiscountPoints != null" label="券抵扣积分">
          <span class="detail-discount">-{{ detail.couponDiscountPoints }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.items && detail.items.length" label="购买商品">
          <div v-for="item in detail.items" :key="item.receiptId" class="receipt-line">
            <span class="detail-product">{{ item.productName }}</span>
            <span class="detail-muted"> × {{ item.count }}</span>
            <span class="detail-price">（单价 {{ item.price }} 积分）</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { listConsumeLog, getConsumeLogDetail } from '@/api/user/points'
import { formatDateTime } from '@/utils/dateTime'
import { shopIdFromRoute, parseOptionalShopId, parseOptionalPositiveInt } from '@/utils/shopRoute'

const route = useRoute()
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const filterDefaults = {
  shopId: shopIdFromRoute(route),
  actionType: undefined,
  status: undefined,
  nickname: '',
  phone: ''
}

const table = useTable(
  (params) =>
    listConsumeLog({
      shopId: parseOptionalShopId(params.shopId),
      actionType: params.actionType,
      status: params.status,
      nickname: params.nickname || undefined,
      phone: params.phone || undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }),
  {
    defaultParams: { ...filterDefaults },
    refreshOnActivated: true
  }
)

const { loading, tableData, total, queryParams, getList } = table
const { filterForm, onSearch, onReset, initAndLoad } = useFilterTable(table, filterDefaults, {
  onResetExtra: () => {
    filterForm.shopId = shopIdFromRoute(route)
  }
})

function actionTypeLabel(type) {
  if (type === 1) return '增加'
  if (type === 2) return '消耗'
  return '—'
}

function statusLabel(status) {
  const map = {
    1: '已核销',
    2: '订单取消',
    3: '订单过期',
    4: '积分充值',
    6: '后台添加',
    7: '后台扣除'
  }
  return map[status] || '—'
}

function statusTag(status) {
  if (status === 1) return 'success'
  if (status === 6) return 'success'
  if (status === 7) return 'danger'
  if (status === 2 || status === 3) return 'info'
  if (status === 4) return 'primary'
  return 'info'
}

function couponTypeLabel(type) {
  if (type === 1) return '折扣'
  if (type === 2) return '满减'
  return '—'
}

/** 仅展示有效手机号，过滤「一」等非数字占位 */
function displayPhone(phone) {
  const value = String(phone || '').trim()
  if (!/^\d{7,15}$/.test(value)) return ''
  return value
}

function formatPointsChange(value) {
  if (value == null || value === '') return '—'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return n > 0 ? `+${n}` : String(n)
}

function pointsChangeClass(value) {
  if (value == null || value === '') return 'detail-muted'
  const n = Number(value)
  if (Number.isNaN(n) || n === 0) return 'detail-muted'
  return n > 0 ? 'detail-increase' : 'detail-decrease'
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getConsumeLogDetail(row.logId, row.shopId)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => initAndLoad())
</script>

<style scoped>
.receipt-line + .receipt-line {
  margin-top: 4px;
}

.consume-detail .detail-emphasis {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.consume-detail .detail-id {
  color: var(--el-color-info);
  font-family: ui-monospace, monospace;
}

.consume-detail .detail-time {
  color: var(--el-text-color-regular);
}

.consume-detail .detail-balance {
  color: var(--el-color-primary);
  font-weight: 500;
}

.consume-detail .detail-consume {
  color: var(--el-color-warning);
  font-weight: 600;
  font-size: 15px;
}

.consume-detail .detail-increase {
  color: var(--el-color-success);
  font-weight: 600;
}

.consume-detail .detail-decrease {
  color: var(--el-color-danger);
  font-weight: 600;
}

.consume-detail .detail-coupon {
  color: var(--el-color-primary);
  font-weight: 500;
}

.consume-detail .detail-discount {
  color: var(--el-color-success);
  font-weight: 600;
}

.consume-detail .detail-product {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.consume-detail .detail-price {
  color: var(--el-color-warning-dark-2);
}

.consume-detail .detail-muted {
  color: var(--el-text-color-secondary);
}

.consume-detail .detail-remark {
  color: var(--el-text-color-primary);
  line-height: 1.5;
  white-space: pre-wrap;
}

.consume-detail .coupon-type-tag {
  margin-left: 8px;
  vertical-align: middle;
}
</style>
