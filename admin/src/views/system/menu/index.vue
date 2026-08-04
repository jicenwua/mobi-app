<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
      <el-form-item label="菜单名称" prop="menuName">
        <el-input
          v-model="queryParams.menuName"
          placeholder="请输入菜单名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:menu:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Sort"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
    </el-row>

    <el-table
      v-if="refreshTable"
      ref="tableRef"
      v-loading="loading"
      :data="tableData"
      row-key="menuId"
      :default-expand-all="isExpandAll"
      :indent="20"
      :tree-props="{ children: 'children' }"
      border
      @select="handleTreeSelect"
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column prop="menuName" label="菜单名称" width="160" />
      <el-table-column prop="icon" label="图标" align="center" width="100">
        <template #default="scope">
          <el-icon v-if="resolveMenuTableIcon(scope.row.icon)">
            <component :is="resolveMenuTableIcon(scope.row.icon)" />
          </el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="perms" label="权限标识" min-width="150" show-overflow-tooltip />
      <el-table-column prop="component" label="组件路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="path" label="请求路径" min-width="120" show-overflow-tooltip />
      <el-table-column label="是否隐藏" width="96" align="center">
        <template #default="scope">
          <el-tag
            v-if="scope.row.visible != null && scope.row.visible !== ''"
            :type="scope.row.visible === '0' ? 'success' : 'info'"
            size="small"
          >
            {{ scope.row.visible === '0' ? '显示' : '隐藏' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="是否缓存" width="96" align="center">
        <template #default="scope">
          <el-tag v-if="isCacheYes(scope.row.isCache)" type="success" size="small">缓存</el-tag>
          <el-tag v-else-if="isCacheNo(scope.row.isCache)" type="warning" size="small">不缓存</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="是否外链" width="96" align="center">
        <template #default="scope">
          <el-tag v-if="isFrameYes(scope.row.isFrame)" type="warning" size="small">外链</el-tag>
          <el-tag v-else-if="isFrameNo(scope.row.isFrame)" type="success" size="small">非外链</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <DictTag v-if="scope.row.status != null && scope.row.status !== ''" dict="sys_normal_disable" :value="scope.row.status" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建者" align="center" prop="createBy" min-width="100" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.createBy || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="170" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatDateTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="修改者" align="center" prop="updateBy" min-width="100" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.updateBy || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" align="center" width="170" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatDateTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:menu:edit']"
          >修改</el-button>
          <el-button
            link
            type="primary"
            icon="Plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:menu:add']"
          >新增</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:menu:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <MenuForm
      v-model="dialogVisible"
      :form-data="currentRow"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ref, onMounted, onActivated, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { withClickLock } from '@/utils/withClickLock'
import { listAdminMenus, delMenu } from '@/api/system/menu'
import MenuForm from './components/MenuForm.vue'
import { formatDateTime } from '@/utils/dateTime'
import { buildMenuTreeFromFlat } from '@/utils/menuTree'

/** 已注册的 @element-plus/icons-vue 组件名，避免 icon 为 #、空串等非法标签导致 createElement 报错 */
const EP_ICON_NAMES = new Set(Object.keys(ElementPlusIconsVue))

/** 后端：0 缓存 / 1 不缓存（与实体注释一致） */
function isCacheYes(v) {
  return v === 0 || v === '0'
}
function isCacheNo(v) {
  return v === 1 || v === '1'
}
/** 后端：0 是外链 / 1 否 */
function isFrameYes(v) {
  return v === 0 || v === '0'
}
function isFrameNo(v) {
  return v === 1 || v === '1'
}

function resolveMenuTableIcon(icon) {
  if (icon == null || typeof icon !== 'string') return null
  const s = icon.trim()
  if (!s || s === '#') return null
  if (/^[#./?]/.test(s)) return null
  if (EP_ICON_NAMES.has(s)) return s
  const pascal = s.charAt(0).toUpperCase() + s.slice(1)
  if (EP_ICON_NAMES.has(pascal)) return pascal
  const lower = s.toLowerCase()
  for (const k of EP_ICON_NAMES) {
    if (k.toLowerCase() === lower) return k
  }
  return null
}

/** 当前节点下所有子孙行（不含自身），用于多选联动 */
function flattenDescendantRows(node) {
  const out = []
  for (const c of node.children || []) {
    out.push(c)
    out.push(...flattenDescendantRows(c))
  }
  return out
}

/** 勾选/取消父级时，同步子级多选状态 */
function handleTreeSelect(selection, row) {
  const table = tableRef.value
  if (!table) return
  const isSelected = selection.some((r) => r.menuId === row.menuId)
  const descendants = flattenDescendantRows(row)
  nextTick(() => {
    for (const d of descendants) {
      table.toggleRowSelection(d, isSelected)
    }
  })
}

function collectSelfAndDescendantIds(node) {
  const ids = [node.menuId]
  for (const c of node.children || []) {
    ids.push(...collectSelfAndDescendantIds(c))
  }
  return ids
}

function filterMenuTree(nodes, menuName, status) {
  const q = (menuName || '').trim().toLowerCase()
  const wantStatus = status === '' || status == null ? null : String(status)

  function walk(list) {
    const out = []
    for (const n of list || []) {
      const rawChildren = Array.isArray(n.children) ? n.children : []
      const children = walk(rawChildren)
      const nameOk = !q || String(n.menuName || '').toLowerCase().includes(q)
      const st = n.status != null && n.status !== '' ? String(n.status) : '0'
      const statusOk = wantStatus == null || st === wantStatus
      if (children.length || (nameOk && statusOk)) {
        out.push({ ...n, children: children.length ? children : undefined })
      }
    }
    return out
  }
  return walk(nodes)
}

function countTreeNodes(nodes) {
  let n = 0
  for (const x of nodes || []) {
    n += 1
    if (x.children?.length) n += countTreeNodes(x.children)
  }
  return n
}

/** 菜单管理：扁平列表 + parentId 组树，确保隐藏项与子按钮挂在正确父级下 */
async function fetchMenuTreeForTable(params) {
  const res = await listAdminMenus()
  const tree = buildMenuTreeFromFlat(res.data || [])
  const rows = filterMenuTree(tree, params.menuName, params.status)
  return {
    code: 200,
    rows,
    total: countTreeNodes(rows)
  }
}

const {
  loading,
  tableData,
  queryParams,
  getList,
  handleQuery,
  resetQuery
} = useTable(fetchMenuTreeForTable, {
  defaultParams: {
    menuName: '',
    status: ''
  }
})

onMounted(() => {
  getList()
})

/** 与 keep-alive 搭配时，首次进入会先 onMounted 再 onActivated，避免连续请求两次 tree */
let skipFirstActivatedFetch = true
onActivated(() => {
  if (skipFirstActivatedFetch) {
    skipFirstActivatedFetch = false
    return
  }
  getList()
})

const tableRef = ref(null)
const refreshTable = ref(true)
const isExpandAll = ref(false)
const dialogVisible = ref(false)
const currentRow = ref({})

function toggleExpandAll() {
  isExpandAll.value = !isExpandAll.value
  refreshTable.value = false
  setTimeout(() => {
    refreshTable.value = true
  }, 50)
}

function handleAdd(row) {
  currentRow.value = {}
  if (row && row.menuId) {
    currentRow.value.parentId = row.menuId
  }
  dialogVisible.value = true
}

function handleUpdate(row) {
  currentRow.value = { ...row }
  dialogVisible.value = true
}

/** 级联删除当前菜单及全部子孙 */
const handleDelete = withClickLock(async (row) => {
  const idList = [...new Set(collectSelfAndDescendantIds(row))]
  const childCount = idList.length - 1
  const msg =
    childCount > 0
      ? `将同时删除「${row.menuName}」及其下 ${childCount} 个子菜单，确认删除吗？`
      : `确认删除菜单「${row.menuName}」吗？`
  try {
    await ElMessageBox.confirm(msg, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await delMenu(idList)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
})

function handleFormSuccess() {
  getList()
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
