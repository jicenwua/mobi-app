<template>
  <el-dialog
    :title="title"
    v-model="visible"
    width="680px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row>
        <el-col :span="24">
          <el-form-item label="上级菜单">
            <el-tree-select
              v-model="form.parentId"
              :data="menuOptions"
              :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
              value-key="menuId"
              placeholder="选择上级菜单"
              check-strictly
            />
          </el-form-item>
        </el-col>
        
        <el-col :span="24">
          <el-form-item label="菜单类型" prop="menuType">
            <el-radio-group v-model="form.menuType">
              <el-radio label="M">目录</el-radio>
              <el-radio label="C">菜单</el-radio>
              <el-radio label="F">按钮</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        
        <el-col :span="24">
          <el-form-item label="菜单名称" prop="menuName">
            <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
          </el-form-item>
        </el-col>
        
        <el-col :span="24">
          <el-form-item label="显示排序" prop="orderNum">
            <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="路由地址" prop="path">
            <el-input v-model="form.path" placeholder="请输入路由地址" />
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType === 'C'">
          <el-form-item label="组件路径" prop="component">
            <el-input v-model="form.component" placeholder="请输入组件路径" />
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'M'">
          <el-form-item label="权限标识" prop="perms">
            <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="50" />
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="菜单图标">
            <el-popover placement="bottom-start" :width="540" trigger="click">
              <template #reference>
                <el-button class="icon-picker-trigger" plain>
                  <el-icon v-if="resolveFormIcon(form.icon)" class="icon-picker-trigger__icon">
                    <component :is="resolveFormIcon(form.icon)" />
                  </el-icon>
                  <span class="icon-picker-trigger__text">{{ form.icon || '点击选择图标' }}</span>
                </el-button>
              </template>
              <el-input
                v-model="iconSearch"
                clearable
                placeholder="搜索图标名称"
                class="icon-search"
              />
              <div class="icon-grid">
                <div
                  v-for="name in filteredIconNames"
                  :key="name"
                  class="icon-cell"
                  :class="{ active: form.icon === name }"
                  :title="name"
                  @click="selectIcon(name)"
                >
                  <el-icon><component :is="name" /></el-icon>
                </div>
              </div>
              <div class="icon-footer">
                <el-button link type="danger" @click="clearIcon">清空</el-button>
              </div>
            </el-popover>
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="是否外链">
            <el-radio-group v-model="form.isFrame">
              <el-radio label="0">是</el-radio>
              <el-radio label="1">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="是否缓存">
            <el-radio-group v-model="form.isCache">
              <el-radio label="0">缓存</el-radio>
              <el-radio label="1">不缓存</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="显示状态">
            <el-radio-group v-model="form.visible">
              <el-radio label="0">显示</el-radio>
              <el-radio label="1">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        
        <el-col :span="12" v-if="form.menuType !== 'F'">
          <el-form-item label="菜单状态">
            <el-radio-group v-model="form.status">
              <el-radio label="0">正常</el-radio>
              <el-radio label="1">停用</el-radio>
            </el-radio-group>
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
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useLockedFn } from '@/hooks/useLockedFn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { addMenu, updateMenu, listAdminMenus } from '@/api/system/menu'
import { buildMenuTreeFromFlat } from '@/utils/menuTree'

const EP_ICON_NAMES = new Set(Object.keys(ElementPlusIconsVue))
const ALL_ICON_NAMES = Object.keys(ElementPlusIconsVue).sort()

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

const visible = ref(false)
const title = ref('')
const formRef = ref(null)
const menuOptions = ref([])
const iconSearch = ref('')

const filteredIconNames = computed(() => {
  const q = iconSearch.value.trim().toLowerCase()
  if (!q) return ALL_ICON_NAMES
  return ALL_ICON_NAMES.filter((n) => n.toLowerCase().includes(q))
})

function resolveFormIcon(icon) {
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

function selectIcon(name) {
  form.icon = name
}

function clearIcon() {
  form.icon = ''
}

const form = reactive({
  menuId: undefined,
  parentId: 0,
  menuName: '',
  menuType: 'M',
  orderNum: 1,
  path: '',
  component: '',
  perms: '',
  icon: '',
  isFrame: '1',
  isCache: '0',
  visible: '0',
  status: '0'
})

const rules = {
  menuName: [
    { required: true, message: '菜单名称不能为空', trigger: 'blur' }
  ],
  orderNum: [
    { required: true, message: '显示排序不能为空', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '路由地址不能为空', trigger: 'blur' }
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

async function initForm() {
  const res = await listAdminMenus()
  menuOptions.value = [
    {
      menuId: 0,
      menuName: '主类目',
      children: buildMenuTreeFromFlat(res.data || [])
    }
  ]

  if (props.formData && props.formData.menuId) {
    title.value = '修改菜单'
    Object.assign(form, props.formData)
    normalizeFormScalars()
  } else {
    title.value = '新增菜单'
    const presetParentId = props.formData?.parentId
    resetForm()
    if (presetParentId != null && presetParentId !== '') {
      form.parentId = presetParentId
    }
  }
  iconSearch.value = ''
}

/** 与 el-radio label 一致；接口可能返回 number */
function normalizeFormScalars() {
  ;['isCache', 'visible', 'status', 'isFrame'].forEach((k) => {
    const v = form[k]
    if (v !== undefined && v !== null && v !== '') {
      form[k] = String(v)
    }
  })
  if (form.isFrame === undefined || form.isFrame === null || form.isFrame === '') {
    form.isFrame = '1'
  }
  if (form.isCache === undefined || form.isCache === null || form.isCache === '') {
    form.isCache = '0'
  }
}

function resetForm() {
  Object.assign(form, {
    menuId: undefined,
    parentId: 0,
    menuName: '',
    menuType: 'M',
    orderNum: 1,
    path: '',
    component: '',
    perms: '',
    icon: '',
    isFrame: '1',
    isCache: '0',
    visible: '0',
    status: '0'
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
  if (form.menuId) {
    await updateMenu(form)
    ElMessage.success('修改成功')
  } else {
    await addMenu(form)
    ElMessage.success('新增成功')
  }
  visible.value = false
  emit('success')
})

function handleClose() {
  iconSearch.value = ''
  resetForm()
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}

.icon-picker-trigger {
  width: 100%;
  justify-content: flex-start;
}

.icon-picker-trigger__icon {
  margin-right: 8px;
  font-size: 18px;
}

.icon-picker-trigger__text {
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-search {
  margin-bottom: 10px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
  max-height: 280px;
  overflow-y: auto;
  padding: 2px 0;
}

.icon-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.icon-cell:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.icon-cell.active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-8);
}

.icon-footer {
  margin-top: 10px;
  text-align: right;
}
</style>
