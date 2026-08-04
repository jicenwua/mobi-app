<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="可选，不填查全部" clearable style="width: 140px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="用户ID">
        <el-input v-model="filterForm.userId" placeholder="用户ID" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="店铺名称">
        <el-input v-model="filterForm.shopName" placeholder="模糊搜索" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="filterForm.nickname" placeholder="模糊搜索" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="filterForm.phone" placeholder="模糊搜索" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="filterForm.sortType" style="width: 180px">
          <el-option label="剩余积分降序" :value="1" />
          <el-option label="剩余积分正序" :value="2" />
          <el-option label="累计已用降序" :value="3" />
          <el-option label="累计已用正序" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column label="店铺" prop="shopName" min-width="120" show-overflow-tooltip />
      <el-table-column label="用户ID" prop="userId" width="96" align="center" />
      <el-table-column label="昵称" prop="nickname" min-width="100" show-overflow-tooltip />
      <el-table-column label="手机号" prop="phone" width="130" />
      <el-table-column label="基础积分" prop="basePoints" width="100" align="center" />
      <el-table-column label="赠送积分" prop="bonusPoints" width="100" align="center" />
      <el-table-column label="剩余积分" prop="remainingPoints" width="100" align="center" />
      <el-table-column label="累计已用" prop="totalUsedPoints" width="100" align="center" />
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openAdjust(row)"
            v-hasPermi="['system:user:points:adjust']"
          >调积分</el-button>
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

    <el-dialog v-model="adjustVisible" title="调整积分" width="480px" destroy-on-close>
      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="用户">
          <span>{{ adjustTarget?.nickname || '—' }}（ID: {{ adjustTarget?.userId }}）</span>
        </el-form-item>
        <el-form-item label="店铺">
          <span>{{ adjustTarget?.shopName || '—' }}（ID: {{ adjustTarget?.shopId }}）</span>
        </el-form-item>
        <el-form-item label="当前积分">
          <span>
            基础 <strong class="points-base">{{ adjustTarget?.basePoints ?? 0 }}</strong>
            · 赠送 <strong class="points-bonus">{{ adjustTarget?.bonusPoints ?? 0 }}</strong>
          </span>
        </el-form-item>
        <el-form-item label="积分类型" prop="pointType">
          <el-radio-group v-model="adjustForm.pointType">
            <el-radio :value="1">基础积分</el-radio>
            <el-radio :value="2">赠送积分</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="操作" prop="direction">
          <el-radio-group v-model="adjustForm.direction">
            <el-radio :value="1">增加</el-radio>
            <el-radio :value="2">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量" prop="amount">
          <el-input-number v-model="adjustForm.amount" :min="1" :precision="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写调整原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确定</el-button>
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
import { listPointsAccount, adjustPointsAccount } from '@/api/user/points'
import { formatDateTime } from '@/utils/dateTime'
import { shopIdFromRoute, parseOptionalShopId, parseOptionalPositiveInt, parseRequiredShopId } from '@/utils/shopRoute'

const route = useRoute()
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustFormRef = ref(null)
const adjustTarget = ref(null)

const adjustForm = reactive({
  pointType: 1,
  direction: 1,
  amount: 1,
  remark: ''
})

const adjustRules = {
  pointType: [{ required: true, message: '请选择积分类型', trigger: 'change' }],
  direction: [{ required: true, message: '请选择操作', trigger: 'change' }],
  amount: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  remark: [{ required: true, message: '请填写备注', trigger: 'blur' }]
}

const filterDefaults = {
  shopId: shopIdFromRoute(route),
  userId: '',
  shopName: '',
  nickname: '',
  phone: '',
  sortType: 1
}

const table = useTable(
  (params) =>
    listPointsAccount({
      shopId: parseOptionalShopId(params.shopId),
      userId: parseOptionalPositiveInt(params.userId),
      shopName: params.shopName || undefined,
      nickname: params.nickname || undefined,
      phone: params.phone || undefined,
      sortType: params.sortType,
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

function openAdjust(row) {
  adjustTarget.value = row
  adjustForm.pointType = 1
  adjustForm.direction = 1
  adjustForm.amount = 1
  adjustForm.remark = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  await adjustFormRef.value?.validate()
  if (!adjustTarget.value) return
  adjustLoading.value = true
  try {
    await adjustPointsAccount({
      userId: adjustTarget.value.userId,
      shopId: adjustTarget.value.shopId,
      pointType: adjustForm.pointType,
      direction: adjustForm.direction,
      amount: adjustForm.amount,
      remark: adjustForm.remark.trim()
    })
    ElMessage.success('积分调整成功')
    adjustVisible.value = false
    getList()
  } finally {
    adjustLoading.value = false
  }
}

onMounted(() => initAndLoad())
</script>

<style scoped>
.points-base {
  color: var(--el-color-primary);
}

.points-bonus {
  color: var(--el-color-success);
}
</style>
