"use strict";
const TAB_KEYS = ["unused", "verified"];
function emptySnapshot() {
  return { loaded: false, rows: [], finished: false, pageNum: 1 };
}
const store = {
  unused: emptySnapshot(),
  verified: emptySnapshot()
};
let cacheEpoch = 0;
function getMemberOrdersCacheEpoch() {
  return cacheEpoch;
}
function isMemberOrdersCacheReady(tabKey) {
  var _a;
  return !!((_a = store[tabKey]) == null ? void 0 : _a.loaded);
}
function getMemberOrdersCache(tabKey) {
  const cached = store[tabKey];
  if (!(cached == null ? void 0 : cached.loaded))
    return null;
  return {
    rows: [...cached.rows],
    finished: cached.finished,
    pageNum: cached.pageNum
  };
}
function setMemberOrdersCache(tabKey, { rows, finished, pageNum }) {
  if (!store[tabKey])
    return;
  store[tabKey] = {
    loaded: true,
    rows: Array.isArray(rows) ? [...rows] : [],
    finished: !!finished,
    pageNum: Number(pageNum) > 0 ? Number(pageNum) : 1
  };
}
function invalidateMemberOrdersCache(tabKey) {
  cacheEpoch += 1;
  if (tabKey) {
    store[tabKey] = emptySnapshot();
    return;
  }
  for (const key of TAB_KEYS) {
    store[key] = emptySnapshot();
  }
}
exports.getMemberOrdersCache = getMemberOrdersCache;
exports.getMemberOrdersCacheEpoch = getMemberOrdersCacheEpoch;
exports.invalidateMemberOrdersCache = invalidateMemberOrdersCache;
exports.isMemberOrdersCacheReady = isMemberOrdersCacheReady;
exports.setMemberOrdersCache = setMemberOrdersCache;
