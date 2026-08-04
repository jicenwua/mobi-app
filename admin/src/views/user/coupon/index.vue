<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="用户ID">
        <el-input v-model="filterForm.userId" placeholder="用户ID" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="店铺ID" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="折扣券ID">
        <el-input v-model="filterForm.templateId" placeholder="模板ID" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="未使用" :value="0" />
          <el-option label="已使用" :value="1" />
          <el-option label="已过期" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" plain icon="Plus" @click="grantVisible = true" v-hasPermi="['system:user:coupon:grant']">发放折扣券</el-button>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column label="记录ID" prop="userCouponId" width="96" align="center" />
      <el-table-column label="用户ID" prop="userId" width="96" align="center" />
      <el-table-column label="昵称" prop="nickname" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.nickname || '—' }}</template>
      </el-table-column>
      <el-table-column label="店铺ID" prop="shopId" width="96" align="center" />
      <el-table-column label="店铺" prop="shopName" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.shopName || '—' }}</template>
      </el-table-column>
      <el-table-column label="模板ID" prop="templateId" width="96" align="center" />
      <el-table-column label="券名称" prop="couponName" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.couponName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="领取时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.receiveTime) }}</template>
      </el-table-column>
      <el-table-column label="过期时间" width="170" align="center">
        <template #default="{ row }">{{ row.expireTime ? formatDateTime(row.expireTime) : '永久' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            :disabled="row.status === 1"
            @click="handleDelete(row)"
            v-hasPermi="['system:user:coupon:remove']"
          >删除</el-button>
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

    <el-dialog v-model="grantVisible" title="发放折扣券" width="480px" destroy-on-close>
      <el-form :model="grantForm" label-width="100px">
        <el-form-item label="用户ID" required>
          <el-input v-model="grantForm.userId" />
        </el-form-item>
        <el-form-item label="店铺ID" required>
          <el-input v-model="grantForm.shopId" />
        </el-form-item>
        <el-form-item label="模板ID" required>
          <el-input v-model="grantForm.templateId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantLoading" @click="submitGrantLocked">发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { useLockedFn } from '@/hooks/useLockedFn'
import { useConfirmAction } from '@/hooks/useConfirmAction'
import { formatDateTime } from '@/utils/dateTime'
import { parseOptionalShopId, parseOptionalPositiveInt, parseRequiredShopId } from '@/utils/shopRoute'
import { listUserCoupon, grantUserCoupon, delUserCoupon } from '@/api/user/coupon'

const filterDefaults = { userId: '', shopId: '', templateId: '', status: undefined }
const grantVisible = ref(false)
const grantForm = reactive({ userId: '', shopId: '', templateId: '' })

const table = useTable(
  (params) =>
    listUserCoupon({
      userId: parseOptionalPositiveInt(params.userId),
      shopId: parseOptionalShopId(params.shopId),
      templateId: parseOptionalPositiveInt(params.templateId),
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
const confirmAction = useConfirmAction({ onSuccess: getList })

function statusLabel(s) {
  if (s === 1) return '已使用'
  if (s === 2) return '已过期'
  return '未使用'
}

function statusTag(s) {
  if (s === 1) return 'info'
  if (s === 2) return 'warning'
  return 'success'
}

async function submitGrant() {
  const userId = parseOptionalPositiveInt(grantForm.userId)
  const shopId = parseRequiredShopId(grantForm.shopId)
  const templateId = parseOptionalPositiveInt(grantForm.templateId)
  if (!userId || !shopId || !templateId) {
    ElMessage.warning('请填写完整且有效的数字 ID')
    return
  }
  await grantUserCoupon({
    userId,
    shopId,
    templateId
  })
  ElMessage.success('发放成功')
  grantVisible.value = false
  getList()
}

const { run: submitGrantLocked, loading: grantLoading } = useLockedFn(submitGrant)

const handleDelete = (row) =>
  confirmAction.run({
    message: '确认删除该用户折扣券？',
    action: () => delUserCoupon(row.userCouponId),
    successMsg: '已删除'
  })

onMounted(() => initAndLoad())
</script>
