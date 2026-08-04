import { reactive } from 'vue'

/**
 * 筛选表单与 useTable 的 queryParams 同步（搜索/重置）
 * @param {ReturnType<typeof import('./useTable').useTable>} table - useTable 返回值
 * @param {Object} filterDefaults - 筛选表单默认值
 * @param {{ beforeSearch?: () => void, onResetExtra?: () => void }} [options]
 */
export function useFilterTable(table, filterDefaults, options = {}) {
  const { beforeSearch, onResetExtra } = options
  const filterForm = reactive({ ...filterDefaults })

  function syncQuery() {
    Object.assign(table.queryParams, { ...filterForm })
  }

  function onSearch() {
    beforeSearch?.()
    syncQuery()
    table.queryParams.pageNum = 1
    table.getList()
  }

  function onReset() {
    Object.assign(filterForm, filterDefaults)
    onResetExtra?.()
    syncQuery()
    table.queryParams.pageNum = 1
    table.getList()
  }

  /** 首次进入或依赖 route.query 时同步筛选并拉数 */
  function initAndLoad() {
    syncQuery()
    table.getList()
  }

  return { filterForm, syncQuery, onSearch, onReset, initAndLoad }
}
