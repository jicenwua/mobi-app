<template>
  <div class="app-container">
    <el-form :model="filterForm" :inline="true" label-width="88px">
      <el-form-item label="店铺ID">
        <el-input v-model="filterForm.shopId" placeholder="留空查全部" clearable style="width: 140px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="商品名称">
        <el-input v-model="filterForm.productName" placeholder="模糊搜索" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="售完" :value="0" />
          <el-option label="出售中" :value="1" />
          <el-option label="下架" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">搜索</el-button>
        <el-button icon="Refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="商品ID" prop="productId" width="96" align="center" />
      <el-table-column label="展示图" width="88" align="center">
        <template #default="{ row }">
          <el-image
            v-if="productImageUrl(row.imageUrl)"
            class="product-thumb"
            :src="productImageUrl(row.imageUrl)"
            fit="cover"
            :preview-src-list="[productImageUrl(row.imageUrl)]"
            preview-teleported
          />
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="店铺" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="shop-cell">
            <span class="shop-cell__name">{{ row.shopName || '—' }}</span>
            <span class="shop-cell__id">ID: {{ row.shopId ?? '—' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="商品名称" prop="productName" min-width="140" show-overflow-tooltip />
      <el-table-column label="价格(积分)" width="110" align="center">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="productStatusTag(row.status)">{{ productStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存" width="90" align="center">
        <template #default="{ row }">{{ formatStock(row.stock) }}</template>
      </el-table-column>
      <el-table-column label="已售" prop="soldCount" width="80" align="center" />
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            link
            type="danger"
            v-hasPermi="['system:product:list']"
            @click="handleOffShelf(row)"
          >下架</el-button>
          <el-button
            v-else-if="row.status === 2"
            link
            type="primary"
            v-hasPermi="['system:product:list']"
            @click="handleOnSale(row)"
          >上架</el-button>
          <el-dropdown
            v-if="productDetailMenus.length"
            trigger="click"
            @command="(cmd) => onDetailCommand(cmd, row)"
          >
            <span class="el-button is-link el-button--primary">更多</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in productDetailMenus"
                  :key="item.command"
                  :command="item.command"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useFilterTable } from '@/hooks/useFilterTable'
import { useConfirmAction } from '@/hooks/useConfirmAction'
import { usePermissionStore } from '@/store/modules/permission'
import { listProduct, updateProductStatus } from '@/api/product/product'
import { formatDateTime } from '@/utils/dateTime'
import { shopIdFromRoute, parseOptionalShopId } from '@/utils/shopRoute'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const PRODUCT_DETAIL_MENU = [
  { command: 'points', label: '用户积分', perm: 'system:user:points:list' },
  { command: 'consume', label: '消费记录', perm: 'system:user:consume:list' }
]

const productDetailMenus = computed(() => {
  const perms = permissionStore.permissions
  return PRODUCT_DETAIL_MENU.filter((item) => perms.includes(item.perm))
})

const filterDefaults = {
  shopId: shopIdFromRoute(route),
  productName: '',
  status: undefined
}

const table = useTable((params) => {
  return listProduct({
    shopId: parseOptionalShopId(params.shopId),
    productName: params.productName || undefined,
    status: params.status,
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

function productImageUrl(url) {
  if (!url || typeof url !== 'string') return ''
  const t = url.trim()
  if (!t) return ''
  if (t.startsWith('http://') || t.startsWith('https://')) return t
  return t
}

function formatPrice(price) {
  if (price == null) return '—'
  return String(price)
}

function formatStock(stock) {
  if (stock == null) return '—'
  if (Number(stock) === -1) return '无限'
  return String(stock)
}

function productStatusLabel(status) {
  if (status === 1) return '出售中'
  if (status === 2) return '下架'
  return '售完'
}

function productStatusTag(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

function onDetailCommand(command, row) {
  const shopId = row.shopId
  if (!shopId) {
    ElMessage.warning('商品未关联店铺')
    return
  }
  if (command === 'points') {
    router.push({ path: '/user/points', query: { shopId: String(shopId) } })
  } else if (command === 'consume') {
    router.push({ path: '/user/consume', query: { shopId: String(shopId) } })
  }
}

const handleOffShelf = (row) =>
  confirmAction.run({
    message: `确认下架商品「${row.productName}」？`,
    title: '违规下架',
    action: () => updateProductStatus({ productId: row.productId, status: 2 }),
    successMsg: '已下架'
  })

const handleOnSale = (row) =>
  confirmAction.run({
    message: `确认恢复上架商品「${row.productName}」？`,
    title: '恢复出售',
    action: () => updateProductStatus({ productId: row.productId, status: 1 }),
    successMsg: '已恢复出售中'
  })

onMounted(() => initAndLoad())
</script>

<style scoped>
.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  cursor: zoom-in;
}

.shop-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.shop-cell__name {
  color: var(--el-text-color-primary);
}

.shop-cell__id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.text-muted {
  color: var(--el-text-color-placeholder);
}
</style>
