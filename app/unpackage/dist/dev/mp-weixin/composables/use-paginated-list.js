"use strict";
const common_vendor = require("../common/vendor.js");
function usePaginatedList({ fetchPage, pageSize = 20 }) {
  const rows = common_vendor.ref([]);
  const listLoading = common_vendor.ref(false);
  const loadingMore = common_vendor.ref(false);
  const refreshing = common_vendor.ref(false);
  const pageNum = common_vendor.ref(1);
  const total = common_vendor.ref(0);
  const finished = common_vendor.computed(() => rows.value.length >= total.value);
  async function loadPage(page, append) {
    if (append) {
      loadingMore.value = true;
    } else {
      listLoading.value = true;
    }
    const res = await fetchPage(page, pageSize);
    if (append) {
      loadingMore.value = false;
    } else {
      listLoading.value = false;
    }
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
      return false;
    }
    pageNum.value = page;
    const nextRows = res.rows || [];
    const reportedTotal = Number(res.total);
    if (reportedTotal > 0) {
      total.value = reportedTotal;
    } else if (!append) {
      total.value = nextRows.length;
    }
    rows.value = append ? rows.value.concat(nextRows) : nextRows;
    return true;
  }
  async function reloadList() {
    pageNum.value = 1;
    total.value = 0;
    rows.value = [];
    return loadPage(1, false);
  }
  async function onRefresh() {
    refreshing.value = true;
    await reloadList();
    refreshing.value = false;
  }
  async function loadMore() {
    if (listLoading.value || loadingMore.value || finished.value)
      return;
    await loadPage(pageNum.value + 1, true);
  }
  return {
    rows,
    listLoading,
    loadingMore,
    refreshing,
    pageNum,
    total,
    finished,
    reloadList,
    onRefresh,
    loadMore
  };
}
exports.usePaginatedList = usePaginatedList;
