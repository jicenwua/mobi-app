<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input
          v-model="queryParams.roleName"
          placeholder="请输入角色名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input
          v-model="queryParams.roleKey"
          placeholder="请输入权限字符"
          clearable
          style="width: 180px"
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
          v-hasPermi="['system:role:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleBatchDelete"
          v-hasPermi="['system:role:remove']"
        >批量删除</el-button>
      </el-col>
    </el-row>

    <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        border
        style="width: 100%"
        height="calc(100vh - 320px)"
        :max-height="800"
      >
        <el-table-column type="selection" width="50" align="center" fixed="left" />

        <el-table-column label="角色编号" align="center" prop="roleId" min-width="100" />
        <el-table-column label="角色名称" align="center" prop="roleName" min-width="120" show-overflow-tooltip />
        <el-table-column label="权限字符" align="center" prop="roleKey" min-width="120" show-overflow-tooltip />
        <el-table-column label="显示顺序" align="center" prop="roleSort" min-width="100" />
        <el-table-column label="角色状态" align="center" min-width="100">
          <template #default="scope">
            <DictTag dict="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>

        <el-table-column label="创建者" align="center" prop="createBy" min-width="100" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.createBy || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="170">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新者" align="center" prop="updateBy" min-width="100" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.updateBy || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" align="center" prop="updateTime" min-width="170">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.remark || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="250" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:role:edit']"
            >修改</el-button>
            <el-button
              link
              type="primary"
              icon="CircleCheck"
              @click="handleAllocMenu(scope.row)"
              v-hasPermi="['system:role:assign']"
            >分配菜单</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:role:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @pagination="getList"
    />

    <RoleForm
      v-model="dialogVisible"
      :form-data="currentRow"
      @success="handleFormSuccess"
    />

    <AllocMenu
      v-model="menuDialogVisible"
      :role-data="currentRole"
      @success="handleMenuSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { withClickLock } from '@/utils/withClickLock'
import { listRole, delRole } from '@/api/system/role'
import { formatDateTime } from '@/utils/dateTime'
import { clearRoleCache } from '@/utils/roleCache'
import RoleForm from './components/RoleForm.vue'
import AllocMenu from './components/AllocMenu.vue'

const {
  loading,
  tableData,
  total,
  queryParams,
  getList,
  handleQuery,
  resetQuery
} = useTable(listRole, {
  defaultParams: {
    pageNum: 1,
    pageSize: 10,
    roleName: '',
    roleKey: '',
    status: ''
  },
  dataTransform: (list) =>
    (list || []).map((row) => ({
      ...row,
      id: row.roleId ?? row.id,
      sysMenuIds: normalizeMenuIds(row)
    }))
})

const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const currentRow = ref({})
const currentRole = ref({})
const multiple = ref(true)
const ids = ref([])

/** 列表接口返回的菜单 ID：VO.sysMenuIds 或 SQL 别名 menuIds（GROUP_CONCAT 字符串） */
function normalizeMenuIds(row) {
  const raw = row.sysMenuIds ?? row.menuIds
  if (Array.isArray(raw)) {
    return raw.map((id) => Number(id)).filter((n) => !Number.isNaN(n))
  }
  if (typeof raw === 'string' && raw.trim()) {
    return raw
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((n) => !Number.isNaN(n))
  }
  return []
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.roleId ?? item.id)
  multiple.value = !selection.length
}

async function handleBatchDelete() {
  if (!ids.value.length) {
    ElMessage.warning('请选择要删除的角色')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.value.length} 个角色吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delRole(ids.value.join(','))
    ElMessage.success('删除成功')
    clearRoleCache()
    getList()
  } catch {
    // 用户取消或请求失败
  }
}

function handleAdd() {
  currentRow.value = {}
  dialogVisible.value = true
}

function handleUpdate(row) {
  currentRow.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = withClickLock(async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除角色"${row.roleName}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await delRole(row.roleId ?? row.id)
    ElMessage.success('删除成功')
    clearRoleCache()
    getList()
  } catch {
    // 用户取消或请求失败
  }
})

function handleAllocMenu(row) {
  currentRole.value = { ...row }
  menuDialogVisible.value = true
}

function handleFormSuccess() {
  clearRoleCache()
  getList()
}

function handleMenuSuccess() {
  getList()
}

onActivated(() => {
  getList()
})
</script>
