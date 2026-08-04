<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="账号" prop="username">
        <el-input
            v-model="queryParams.username"
            placeholder="请输入账号"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nickname">
        <el-input
            v-model="queryParams.nickname"
            placeholder="请输入用户昵称"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input
            v-model="queryParams.phonenumber"
            placeholder="请输入手机号码"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="OpenID" prop="openId">
        <el-input
            v-model="queryParams.openId"
            placeholder="请输入OpenID"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="正常" value="0"/>
          <el-option label="停用" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item label="登录时间">
        <el-date-picker
            v-model="loginTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
            v-model="createTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="排序方式">
        <el-select v-model="sortType" placeholder="请选择排序" clearable style="width: 180px">
          <el-option label="登录时间降序" value="loginTimeDesc"/>
          <el-option label="登录时间升序" value="loginTimeAsc"/>
          <el-option label="创建时间降序" value="createTimeDesc"/>
          <el-option label="创建时间升序" value="createTimeAsc"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="customHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="customResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['system:user:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleBatchDelete"
            v-hasPermi="['system:user:remove']"
        >批量删除
        </el-button>
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
        <el-table-column type="selection" width="50" align="center" fixed="left"/>

        <el-table-column label="用户编号" align="center" prop="userId" min-width="100" />
        <el-table-column label="账号" align="center" prop="userName" min-width="120" />
        <el-table-column label="用户昵称" align="center" prop="nickName" min-width="120" />
        <el-table-column label="邮箱" align="center" prop="email" min-width="180" />
        <el-table-column label="手机号码" align="center" prop="phonenumber" min-width="130" />
        <el-table-column label="OpenID" align="center" prop="openId" min-width="260" show-overflow-tooltip />
        <el-table-column label="性别" align="center" min-width="80">
          <template #default="scope">
            <DictTag dict="sys_user_sex" :value="scope.row.sex" :tag="false" />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" min-width="80">
          <template #default="scope">
            <DictTag dict="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="角色" align="center" prop="roleName" min-width="150">
          <template #default="scope">
            <span>{{ scope.row.roleName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="登录IP" align="center" prop="loginIp" min-width="140" />
        <el-table-column label="最后登录" align="center" prop="loginDate" min-width="170" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.loginDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="170" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="200" fixed="right">
          <template #default="scope">
            <el-button
                link
                type="primary"
                icon="Edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['system:user:edit']"
            >修改
            </el-button>
            <el-button
                link
                type="danger"
                icon="Delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['system:user:remove']"
            >删除
            </el-button>
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

    <UserForm
        v-model="dialogVisible"
        :form-data="currentRow"
        :role-options="roleOptions"
        @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import { ref, watch, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { withClickLock } from '@/utils/withClickLock'
import { listUser, delUser } from '@/api/system/user'
import { getRoleOptions } from '@/utils/roleCache'
import {formatDateTime} from '@/utils/dateTime'
import UserForm from './components/UserForm.vue'

const loginTimeRange = ref([])
const createTimeRange = ref([])
const sortType = ref('')

const {
  loading,
  tableData,
  total,
  queryParams,
  getList,
  handleQuery,
  resetQuery
} = useTable(listUser, {
  defaultParams: {
    username: '',
    nickname: '',
    phonenumber: '',
    openId: '',
    status: '',
    minLoginTime: '',
    maxLoginTime: '',
    minCreateTime: '',
    maxCreateTime: '',
    orderByLoginTime: false,
    orderByCreateTime: false,
    pageNum: 1,
    pageSize: 10
  }
})

watch(loginTimeRange, (val) => {
  if (val && val.length === 2) {
    queryParams.minLoginTime = val[0]
    queryParams.maxLoginTime = val[1]
  } else {
    queryParams.minLoginTime = ''
    queryParams.maxLoginTime = ''
  }
})

watch(createTimeRange, (val) => {
  if (val && val.length === 2) {
    queryParams.minCreateTime = val[0]
    queryParams.maxCreateTime = val[1]
  } else {
    queryParams.minCreateTime = ''
    queryParams.maxCreateTime = ''
  }
})

/** 排序下拉变更时同步 queryParams 排序字段 */
watch(sortType, (val) => {
  queryParams.orderByLoginTime = false
  queryParams.orderByCreateTime = false

  switch (val) {
    case 'loginTimeDesc':
      queryParams.orderByLoginTime = true
      break
    case 'loginTimeAsc':
      queryParams.orderByLoginTime = false
      break
    case 'createTimeDesc':
      queryParams.orderByCreateTime = true
      break
    case 'createTimeAsc':
      queryParams.orderByCreateTime = false
      break
    default:
      break
  }
})

const multiple = ref(true)
const ids = ref([])
const dialogVisible = ref(false)
const currentRow = ref({})
const roleOptions = ref([])

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.userId)
  multiple.value = !selection.length
}

function handleAdd() {
  currentRow.value = {}
  dialogVisible.value = true
}

function handleUpdate(row) {
  currentRow.value = {...row}
  dialogVisible.value = true
}

const handleDelete = withClickLock(async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.userName}”吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await delUser(row.userId)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
})

const handleBatchDelete = withClickLock(async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的${ids.value.length}个用户吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await delUser(ids.value)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或请求失败
  }
})

const originalHandleQuery = handleQuery

function customHandleQuery() {
  originalHandleQuery()
}

const originalResetQuery = resetQuery

function customResetQuery() {
  loginTimeRange.value = []
  createTimeRange.value = []
  sortType.value = ''
  originalResetQuery()
}

function handleFormSuccess() {
  getList()
}

async function initRoleOptions() {
  roleOptions.value = await getRoleOptions()
}

/** keep-alive 激活时刷新列表 */
onActivated(() => {
  getList()
})

initRoleOptions()
</script>
