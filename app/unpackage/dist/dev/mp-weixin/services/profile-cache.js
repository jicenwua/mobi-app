"use strict";
let editCache = null;
function setProfileEditCache(profile) {
  if (!profile || typeof profile !== "object") {
    editCache = null;
    return;
  }
  editCache = {
    nickname: profile.nickname || "",
    avatar: profile.avatar || "",
    avatarUrl: profile.avatarUrl || profile.avatar || "",
    isDefault: !!profile.isDefault
  };
}
function takeProfileEditCache() {
  const data = editCache;
  editCache = null;
  return data;
}
exports.setProfileEditCache = setProfileEditCache;
exports.takeProfileEditCache = takeProfileEditCache;
