<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="88px">
      <el-form-item label="权限标识">
        <el-input v-model="queryParams.roleKey" placeholder="如 wx:shop:list" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:mobi:role:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDeleteClick" v-hasPermi="['system:mobi:role:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Refresh" @click="handleReloadCache" v-hasPermi="['system:mobi:role:edit']">刷新缓存</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="权限标识" prop="roleKey" min-width="160" show-overflow-tooltip />
      <el-table-column label="描述" prop="roleDescription" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['system:mobi:role:edit']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['system:mobi:role:remove']">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改权限' : '新增权限'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="权限标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="须以 wx: 开头，最长30字符" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="描述" prop="roleDescription">
          <el-input v-model="form.roleDescription" type="textarea" :rows="2" placeholder="权限说明" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFormLocked">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useLockedFn } from '@/hooks/useLockedFn'
import { withClickLock } from '@/utils/withClickLock'
import { formatDateTime } from '@/utils/dateTime'
import { listMobiRole, addMobiRole, updateMobiRole, delMobiRole, reloadMobiRoleCache } from '@/api/system/mobiRole'

const dialogVisible = ref(false)
const formRef = ref(null)
const ids = ref([])
const multiple = ref(true)

const form = reactive({
  id: undefined,
  roleKey: '',
  roleDescription: '',
  status: true
})

const rules = {
  roleKey: [
    { required: true, message: '请输入权限标识', trigger: 'blur' },
    { pattern: /^wx:/, message: '须以 wx: 开头', trigger: 'blur' },
    { max: 30, message: '最长30个字符', trigger: 'blur' }
  ]
}

const { loading, tableData, total, queryParams, getList, handleQuery, resetQuery, handleBatchDelete } = useTable(
  (params) => listMobiRole(params),
  { roleKey: '', status: undefined, refreshOnActivated: true }
)

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id)
  multiple.value = !selection.length
}

function resetForm() {
  form.id = undefined
  form.roleKey = ''
  form.roleDescription = ''
  form.status = true
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  if (form.id) {
    await updateMobiRole({ ...form })
    ElMessage.success('修改成功')
  } else {
    await addMobiRole({ ...form })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  getList()
}

const { run: submitFormLocked, loading: submitLoading } = useLockedFn(submitForm)

const handleDelete = withClickLock(async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除权限「${row.roleKey}」？`, '提示', { type: 'warning' })
    await delMobiRole(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
})

const handleBatchDeleteClick = withClickLock(async () => {
  await handleBatchDelete(ids.value, '确认删除选中的权限？', (idList) => delMobiRole(idList.join(',')))
})

const handleReloadCache = withClickLock(async () => {
  await reloadMobiRoleCache()
  ElMessage.success('缓存已刷新')
})

onMounted(() => getList())
</script>
