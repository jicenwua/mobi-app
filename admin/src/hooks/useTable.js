import { ref, reactive, toRaw, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const TABLE_OPTION_KEYS = new Set(['defaultParams', 'dataTransform', 'onSuccess', 'onError', 'refreshOnActivated'])

function resolveDefaultParams(options) {
  if (options.defaultParams != null) {
    return options.defaultParams
  }
  const shorthand = {}
  for (const [key, value] of Object.entries(options)) {
    if (!TABLE_OPTION_KEYS.has(key)) {
      shorthand[key] = value
    }
  }
  return shorthand
}

/**
 * 表格通用逻辑 Hook
 * @param {Function} api - API 请求函数
 * @param {Object} options - 配置项；筛选默认值可写 defaultParams，或直接平铺在 options 中
 */
export function useTable(api, options = {}) {
  const defaultParams = resolveDefaultParams(options)
  const {
    dataTransform = (data) => data, // 数据转换函数
    onSuccess = null, // 成功回调
    onError = null, // 错误回调
    refreshOnActivated = false
  } = options

  // 状态
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)

  // 查询参数
  const queryParams = reactive({
    pageNum: 1,
    pageSize: 10,
    ...defaultParams
  })

  // 获取列表数据（进行中不重复发起）
  async function getList() {
    if (loading.value) return
    loading.value = true
    try {
      // 转为普通对象，避免 reactive Proxy 在 GET params 序列化时偶发丢字段
      const res = await api({ ...toRaw(queryParams) })

      // 分页列表：兼容 data / rows / records / list 等常见后端字段
      const list =
        (Array.isArray(res?.data) ? res.data : null) ||
        (Array.isArray(res?.rows) ? res.rows : null) ||
        res?.data?.records ||
        res?.data?.list ||
        []
      tableData.value = dataTransform(list)
      total.value = res?.total || res?.data?.total || 0


      // 成功回调
      if (onSuccess) {
        onSuccess(res)
      }
    } catch (error) {
      // 错误回调
      if (onError) {
        onError(error)
      }
    } finally {
      loading.value = false
    }
  }

  // 搜索
  function handleQuery() {
    queryParams.pageNum = 1
    getList()
  }

  // 重置
  function resetQuery() {
    // 重置分页（若 defaultParams 指定了 pageSize，则与其保持一致）
    queryParams.pageNum = 1
    queryParams.pageSize = defaultParams.pageSize ?? 10

    // 重置其他参数
    Object.keys(defaultParams).forEach(key => {
      queryParams[key] = defaultParams[key]
    })

    getList()
  }

  // 分页改变
  function handlePageChange(page) {
    queryParams.pageNum = page
    getList()
  }

  // 每页条数改变
  function handleSizeChange(size) {
    queryParams.pageSize = size
    queryParams.pageNum = 1
    getList()
  }

  // 批量删除（防连点重复弹窗）
  async function handleBatchDelete(ids, confirmMessage = '确认删除选中的数据吗？', deleteApi) {
    if (loading.value) return
    if (!ids || ids.length === 0) {
      ElMessage.warning('请选择要删除的数据')
      return
    }

    try {
      await ElMessageBox.confirm(confirmMessage, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      await deleteApi(ids)
      ElMessage.success('删除成功')

      // 刷新列表
      getList()
    } catch {
      /* 用户取消或删除失败，request 拦截器已提示 */
    }
  }

  if (refreshOnActivated) {
    let skipFirstActivated = true
    onActivated(() => {
      if (skipFirstActivated) {
        skipFirstActivated = false
        return
      }
      getList()
    })
  }

  return {
    loading,
    tableData,
    total,
    queryParams,
    getList,
    handleQuery,
    resetQuery,
    handlePageChange,
    handleSizeChange,
    handleBatchDelete
  }
}
