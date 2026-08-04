<template>
  <el-dialog
    v-model="dialogVisible"
    :title="(form.roleId ?? form.id) ? '修改角色' : '新增角色'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="请输入角色名称" />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
      </el-form-item>
      <el-form-item label="显示顺序" prop="roleSort">
        <el-input-number v-model="form.roleSort" :min="0" :max="999" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio value="0">正常</el-radio>
          <el-radio value="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useLockedFn } from '@/hooks/useLockedFn'
import { addRole, updateRole } from '@/api/system/role'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  formData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  id: undefined,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: '0',
  remark: ''
})

const rules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleKey: [
    { required: true, message: '请输入权限字符', trigger: 'blur' }
  ],
  roleSort: [
    { required: true, message: '请输入显示顺序', trigger: 'blur' }
  ]
}

function emptyFormFields() {
  return {
    id: undefined,
    roleName: '',
    roleKey: '',
    roleSort: 0,
    status: '0',
    remark: ''
  }
}

/** 打开弹窗时先清空再合并，避免上次编辑残留的 roleId 被带到新增请求里 */
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.formData) {
    Object.assign(form, emptyFormFields())
    delete form.roleId
    const rid = props.formData.roleId ?? props.formData.id
    if (rid != null && rid !== '') {
      Object.assign(form, props.formData)
      form.id = rid
      form.roleId = rid
    }
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

function handleClose() {
  formRef.value?.resetFields()
  Object.assign(form, emptyFormFields())
  delete form.roleId
}

const { run: handleSubmit, loading: submitLoading } = useLockedFn(async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const payload = {
    roleName: form.roleName,
    roleKey: form.roleKey,
    roleSort: form.roleSort,
    status: form.status,
    remark: form.remark ?? ''
  }
  const roleId = form.roleId ?? form.id
  if (roleId != null && roleId !== '') {
    await updateRole({ ...payload, roleId })
    ElMessage.success('修改成功')
  } else {
    await addRole(payload)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  emit('success')
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
