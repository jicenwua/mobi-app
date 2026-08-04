<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺名称">
        <el-input v-model="filterForm.shopName" placeholder="模糊搜索" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="法人">
        <el-input v-model="filterForm.legalPerson" placeholder="模糊搜索" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态">
        <el-select v-model="filterForm.auditStatus" placeholder="全部" clearable style="width: 150px">
          <el-option label="待审/驳回" :value="0" />
          <el-option label="已通过" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态">
        <el-select v-model="filterForm.isEnabled" placeholder="全部" clearable style="width: 120px">
          <el-option label="禁用" :value="0" />
          <el-option label="正常" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="行业类目">
        <el-select v-model="filterForm.categoryId" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
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
        <el-button type="success" icon="Plus" @click="goAdd" v-hasPermi="['system:shop:add']">添加店铺</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="店铺ID" prop="id" width="96" align="center" />
      <el-table-column label="店铺编码" prop="shopCode" width="150" show-overflow-tooltip />
      <el-table-column label="店铺名称" prop="shopName" min-width="140" show-overflow-tooltip />
      <el-table-column label="积分比率" width="100" align="center">
        <template #default="{ row }">
          {{ row.ratio != null ? row.ratio : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="法人" prop="legalPerson" width="100" show-overflow-tooltip />
      <el-table-column label="类目" width="100" align="center">
        <template #default="{ row }">
          {{ categoryName(row.categoryId) }}
        </template>
      </el-table-column>
      <el-table-column label="审核" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="auditTag(row.auditStatus)">{{ auditLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isEnabled === 1 ? 'success' : 'info'">{{ row.isEnabled === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <div class="table-row-actions">
            <el-button link type="primary" @click="goDetail(row)" v-hasPermi="['system:shop:query']">详情</el-button>
            <el-button link type="primary" @click="goEdit(row)" v-hasPermi="['system:shop:query']">编辑</el-button>
            <el-dropdown
              v-if="shopInfoMenus.length"
              trigger="click"
              @command="(cmd) => onShopInfoCommand(cmd, row)"
            >
              <span class="el-button is-link el-button--primary">信息</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in shopInfoMenus"
                    :key="item.command"
                    :command="item.command"
                  >
                    {{ item.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { usePermissionStore } from '@/store/modules/permission'
import { listShop } from '@/api/shop/shop'
import { formatDateTime } from '@/utils/dateTime'

const router = useRouter()
const permissionStore = usePermissionStore()

const categories = [
  { id: 1, name: '餐饮' },
  { id: 2, name: '零售' },
  { id: 3, name: '服务' },
  { id: 4, name: '其他' }
]

const DEFAULT_FILTERS = {
  shopName: '',
  legalPerson: '',
  auditStatus: undefined,
  isEnabled: undefined,
  categoryId: undefined
}
const dateRange = ref([])

async function fetchApi(params) {
  const query = {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    shopName: params.shopName,
    legalPerson: params.legalPerson,
    isEnabled: params.isEnabled,
    categoryId: params.categoryId,
    dateRange: dateRange.value
  }
  // 全部：不传 auditStatus（null）；待审/驳回：传非 1（0）；已通过：传 1
  if (params.auditStatus !== undefined && params.auditStatus !== null && params.auditStatus !== '') {
    query.auditStatus = params.auditStatus
  }
  return listShop(query)
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
  if (s === 0) return '待审'
  if (s === 1) return '通过'
  if (s === 2) return '驳回'
  return '—'
}

function auditTag(s) {
  if (s === 0) return 'warning'
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'info'
}

function categoryName(id) {
  const c = categories.find((x) => x.id === id)
  return c ? c.name : id ?? '—'
}

function goDetail(row) {
  router.push(`/shop/manage/detail/${row.id}`)
}

function goEdit(row) {
  router.push(`/shop/manage/edit/${row.id}`)
}

function goAdd() {
  router.push('/shop/manage/add')
}

/** 与 sys_menu.menu_id 对应的路由 name（Menu{menuId}），避免动态嵌套路由 path 不一致 */
const SHOP_INFO_MENU = [
  { command: 'product', label: '店铺商品', perm: 'system:product:list', routeName: 'Menu2110' },
  { command: 'activity', label: '店铺活动', perm: 'system:shop:activity:list', routeName: 'Menu2120' },
  { command: 'coupon', label: '店铺折扣券', perm: 'system:shop:coupon:list', routeName: 'Menu2130' },
  { command: 'points', label: '用户积分', perm: 'system:user:points:list', routeName: 'Menu2101' },
  { command: 'consume', label: '消费记录', perm: 'system:user:consume:list', routeName: 'Menu2102' }
]

const shopInfoMenus = computed(() => {
  const perms = permissionStore.permissions
  return SHOP_INFO_MENU.filter((item) => perms.includes(item.perm))
})

function onShopInfoCommand(command, row) {
  const item = SHOP_INFO_MENU.find((m) => m.command === command)
  if (!item || row.id == null) return
  if (!router.hasRoute(item.routeName)) {
    ElMessage.warning('目标页面未加载或无访问权限')
    return
  }
  router.push({ name: item.routeName, query: { shopId: String(row.id) } })
}

onMounted(() => initAndLoad())
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.table-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.table-row-actions :deep(.el-dropdown) {
  vertical-align: middle;
  line-height: normal;
}

.table-row-actions .el-button.is-link {
  padding: 2px 6px;
  height: auto;
  vertical-align: middle;
}
</style>
