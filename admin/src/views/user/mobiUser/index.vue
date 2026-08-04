<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="88px">
      <el-form-item label="用户ID">
        <el-input v-model="queryParams.userId" placeholder="用户ID" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="queryParams.nickname" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="queryParams.phone" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="OpenID">
        <el-input v-model="queryParams.openid" placeholder="模糊搜索" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="用户ID" prop="userId" width="88" align="center" />
      <el-table-column label="头像" width="72" align="center">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatarUrl" />
        </template>
      </el-table-column>
      <el-table-column label="昵称" prop="nickname" min-width="110" show-overflow-tooltip />
      <el-table-column label="手机号" prop="phone" width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="OpenID" prop="openid" min-width="160" show-overflow-tooltip />
      <el-table-column label="支付密码" width="96" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.hasPassword ? 'success' : 'info'">
            {{ row.hasPassword ? '已设置' : '未设置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="注册时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)" v-hasPermi="['system:mobi:user:query']">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['system:mobi:user:edit']">编辑</el-button>
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

    <el-dialog v-model="detailVisible" title="用户详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="OpenID">{{ detail.openid }}</el-descriptions-item>
        <el-descriptions-item label="UnionID">{{ detail.unionid || '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付密码">{{ detail.hasPassword ? '已设置' : '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '正常' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="登录IP">{{ detail.loginIp || '—' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ formatDateTime(detail.lastLoginTime) }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑用户" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户ID">
          <el-input v-model="form.userId" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="15" clearable placeholder="可留空" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFormLocked">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { useLockedFn } from '@/hooks/useLockedFn'
import { formatDateTime } from '@/utils/dateTime'
import { listMobiUser, getMobiUser, updateMobiUser } from '@/api/user/mobiUser'

const detailVisible = ref(false)
const editVisible = ref(false)
const detail = ref(null)
const formRef = ref(null)

const form = reactive({
  userId: undefined,
  nickname: '',
  phone: '',
  status: 1
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const { loading, tableData, total, queryParams, getList, handleQuery, resetQuery } = useTable(
  (params) =>
    listMobiUser({
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      userId: params.userId ? Number(params.userId) : undefined,
      nickname: params.nickname || undefined,
      phone: params.phone || undefined,
      openid: params.openid || undefined,
      status: params.status
    }),
  {
    defaultParams: { userId: '', nickname: '', phone: '', openid: '', status: undefined }
  }
)

async function handleDetail(row) {
  const res = await getMobiUser(row.userId)
  detail.value = res.data
  detailVisible.value = true
}

function handleEdit(row) {
  form.userId = row.userId
  form.nickname = row.nickname
  form.phone = row.phone || ''
  form.status = row.status ?? 1
  editVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  await updateMobiUser({
    userId: form.userId,
    nickname: form.nickname,
    phone: form.phone,
    status: form.status
  })
  ElMessage.success('保存成功')
  editVisible.value = false
  getList()
}

const { run: submitFormLocked, loading: submitLoading } = useLockedFn(submitForm)

onMounted(() => {
  getList()
})
</script>
