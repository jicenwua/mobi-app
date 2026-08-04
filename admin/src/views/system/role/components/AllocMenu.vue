<template>
  <el-dialog
    v-model="dialogVisible"
    title="分配菜单权限"
    width="min(92vw, 920px)"
    class="alloc-menu-dialog"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form label-width="100px">
      <el-form-item label="角色名称">
        <el-input v-model="roleData.roleName" disabled />
      </el-form-item>
      <el-form-item label="权限字符">
        <el-input v-model="roleData.roleKey" disabled />
      </el-form-item>
      <el-form-item label="菜单权限" class="menu-tree-form-item">
        <div class="menu-tree-wrap">
          <el-tree
            ref="treeRef"
            :data="menuOptions"
            show-checkbox
            node-key="menuId"
            :props="treeProps"
          />
        </div>
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
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useLockedFn } from '@/hooks/useLockedFn'
import { assignMenus } from '@/api/system/role'
import { listAdminMenus } from '@/api/system/menu'
import { buildMenuTreeFromFlat } from '@/utils/menuTree'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  roleData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = ref(false)
const treeRef = ref(null)
const menuOptions = ref([])

/** 解析接口返回的菜单 ID（逗号串 / 数组）；优先原始 menuIds，避免 sysMenuIds 为空数组时挡住有效字段 */
function parseMenuIdList(raw) {
  if (raw == null || raw === '') return []
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

function getRoleMenuIds(row) {
  const fromApi = parseMenuIdList(row?.menuIds)
  if (fromApi.length) return fromApi
  return parseMenuIdList(row?.sysMenuIds)
}

const treeProps = {
  children: 'children',
  label: 'menuName'
}

watch(() => props.modelValue, async (val) => {
  dialogVisible.value = val
  const roleId = props.roleData?.roleId ?? props.roleData?.id
  if (val && roleId) {
    await loadDialogData()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

function collectTreeMenuIds(nodes, set) {
  for (const n of nodes || []) {
    if (n.menuId != null && n.menuId !== '') {
      set.add(Number(n.menuId))
    }
    if (n.children?.length) collectTreeMenuIds(n.children, set)
  }
}

async function loadDialogData() {
  try {
    const res = await listAdminMenus()
    menuOptions.value = buildMenuTreeFromFlat(res.data || [])
    const wanted = getRoleMenuIds(props.roleData)
    const valid = new Set()
    collectTreeMenuIds(menuOptions.value, valid)
    const ids = wanted.filter((id) => valid.has(id))
    await nextTick()
    await nextTick()
    treeRef.value?.setCheckedKeys(ids, false)
  } catch {
    ElMessage.error('加载菜单树失败')
  }
}

function handleClose() {
  menuOptions.value = []
  treeRef.value?.setCheckedKeys([])
}

const { run: handleSubmit, loading: submitLoading } = useLockedFn(async () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() || []
  const menuIds = [...checkedKeys, ...halfCheckedKeys]

  await assignMenus({
    roleId: props.roleData.roleId ?? props.roleData.id,
    menuIds: menuIds
  })

  ElMessage.success('分配成功')
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

.alloc-menu-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.menu-tree-form-item :deep(.el-form-item__content) {
  flex: 1;
  width: 100%;
  max-width: 100%;
}

.menu-tree-wrap {
  width: 100%;
  box-sizing: border-box;
}

.menu-tree-wrap :deep(.el-tree) {
  width: 100%;
  min-width: 0;
  max-height: min(52vh, 520px);
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 10px 12px;
}

.menu-tree-wrap :deep(.el-tree-node__content) {
  min-height: 32px;
  padding-right: 8px;
}

.menu-tree-wrap :deep(.el-tree-node__label) {
  white-space: nowrap;
}
</style>
