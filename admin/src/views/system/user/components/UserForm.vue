<template>
  <el-dialog
      :title="title"
      v-model="visible"
      width="600px"
      :close-on-click-modal="false"
      @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="nickName">
            <el-input v-model="form.nickName" placeholder="请输入用户昵称"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11"/>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="账号" prop="userName">
            <el-input v-model="form.userName" placeholder="请输入账号" :disabled="!!form.userId"/>
          </el-form-item>
        </el-col>

        <el-col :span="24" v-if="!form.userId">
          <el-form-item label="用户密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入用户密码" show-password/>
          </el-form-item>
        </el-col>

        <el-col :span="24" v-if="form.userId">
          <el-form-item label="修改密码">
            <el-input
              v-model="form.password"
              type="text"
              placeholder="请输入新密码（留空则不修改）"
              style="width: calc(100% - 100px)"
            >
              <template #append>
                <el-button @click="generatePassword" icon="Refresh">随机生成</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="用户性别">
            <el-select v-model="form.sex" placeholder="请选择">
              <el-option label="男" value="0"/>
              <el-option label="女" value="1"/>
              <el-option label="未知" value="2"/>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio label="0">正常</el-radio>
              <el-radio label="1">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item label="角色" prop="roleIds">
            <el-select
                v-model="form.roleIds"
                multiple
                :collapse-tags="false"
                placeholder="请选择角色"
                style="width: 100%"
            >
              <el-option
                  v-for="item in roleOptionsNormalized"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
              >
                <div style="display: flex; justify-content: flex-start; align-items: center; width: 100%;">
              <span
                  :style="{ color: getRoleColor(item.roleId), fontWeight: form.roleIds.includes(item.roleId) ? 'bold' : 'normal' }">
                {{ item.roleName }}
              </span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {ref, reactive, watch, computed} from 'vue'
import {ElMessage} from 'element-plus'
import { useLockedFn } from '@/hooks/useLockedFn'
import {addUser, updateUser} from '@/api/system/user'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  roleOptions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const title = ref('')
const formRef = ref(null)

const form = reactive({
  userId: undefined,
  userName: '',
  nickName: '',
  password: '',
  phonenumber: '',
  email: '',
  sex: '0',
  status: '0',
  remark: '',
  roleIds: []
})

const roleColors = {
  1: '#409EFF',
  2: '#67C23A',
  3: '#E6A23C',
  4: '#F56C6C',
  5: '#909399',
  6: '#FF9800',
  7: '#9C27B0',
  8: '#00BCD4',
  9: '#795548',
  10: '#607D8B'
}

/** 下拉选项：统一 roleId 数字，过滤无效项，避免 ElOption value/key 为 NaN */
const roleOptionsNormalized = computed(() =>
  (props.roleOptions || [])
    .map((item) => {
      const roleId = Number(item?.roleId ?? item?.id)
      if (!Number.isFinite(roleId)) return null
      return {
        roleId,
        roleName: item.roleName || `角色#${roleId}`
      }
    })
    .filter(Boolean)
)

function getRoleColor(roleId) {
  const idNum = Number(roleId) || 0
  if (roleColors[idNum]) return roleColors[idNum]
  const fallbackKey = (idNum % 10) + 1
  return roleColors[fallbackKey] || '#409EFF'
}

/** 将后端 roleIds 规范为数字数组 */
function parseRoleIds(raw) {
  if (raw == null || raw === '') return []
  if (Array.isArray(raw)) {
    return raw
      .map((entry) => {
        if (entry == null || entry === '') return NaN
        if (typeof entry === 'object') {
          const v = entry.roleId ?? entry.id
          return Number(v)
        }
        return Number(entry)
      })
      .filter((n) => Number.isFinite(n))
  }
  const s = raw.toString().trim()
  if (!s) return []
  return s
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
    .map((id) => Number(id))
    .filter((n) => Number.isFinite(n))
}

function generatePassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let password = ''
  for (let i = 0; i < 12; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  form.password = password
  ElMessage.success('已生成随机密码')
}

const rules = {
  userName: [
    {
      validator: (rule, value, callback) => {
        // 新增用户时必填，修改用户时可以为空
        if (!form.userId && (!value || value.trim() === '')) {
          callback(new Error('用户名称不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nickName: [
    {required: true, message: '用户昵称不能为空', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '用户密码不能为空', trigger: 'blur'}
  ],
  roleIds: [
    {
      validator: (rule, value, callback) => {
        if (!form.userId && (!value || value.length === 0)) {
          callback(new Error('请选择角色'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  email: [
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ],
  phonenumber: [
    {pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur'}
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    initForm()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function initForm() {
  if (props.formData && props.formData.userId) {
    title.value = '修改用户'
    const roleIds = parseRoleIds(props.formData.roleIds ?? props.formData.roleIdsStr)
    Object.assign(form, {
      ...props.formData,
      roleIds: roleIds
    })
  } else {
    title.value = '新增用户'
    resetForm()
  }
}

function resetForm() {
  Object.assign(form, {
    userId: undefined,
    userName: '',
    nickName: '',
    password: '',
    phonenumber: '',
    email: '',
    sex: '0',
    status: '0',
    remark: '',
    roleIds: []
  })

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const { run: handleSubmit, loading: submitLoading } = useLockedFn(async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (form.userId) {
    await updateUser(form)
    ElMessage.success('修改成功')
  } else {
    await addUser(form)
    ElMessage.success('新增成功')
  }
  visible.value = false
  emit('success')
})

function handleClose() {
  resetForm()
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}

/* 修复多选框标签样式 */
:deep(.el-select__tags) {
  flex-wrap: wrap;
  padding: 4px 0 0 4px;
  line-height: 1.5;
}

:deep(.el-select__tags .el-tag) {
  margin-bottom: 4px;
  margin-right: 4px;
}
</style>
