"use strict";
const common_vendor = require("../common/vendor.js");
const utils_avatar = require("../utils/avatar.js");
const PROFILE_KEY = "user_profile";
const PROFILE_SETUP_DONE_KEY = "profile_setup_done";
const defaultProfile = () => ({
  avatar: "",
  avatarUrl: "",
  nickname: "",
  isDefault: false
});
function getUserProfile() {
  try {
    const stored = common_vendor.index.getStorageSync(PROFILE_KEY);
    if (stored && typeof stored === "object") {
      const merged = { ...defaultProfile(), ...stored };
      const avatar = utils_avatar.resolveAvatarUrl(merged.avatar || merged.avatarUrl);
      return { ...merged, avatar, avatarUrl: avatar };
    }
  } catch {
  }
  return defaultProfile();
}
function setUserProfile(profile) {
  const avatar = utils_avatar.resolveAvatarUrl((profile == null ? void 0 : profile.avatar) || (profile == null ? void 0 : profile.avatarUrl) || "");
  const next = { ...defaultProfile(), ...profile, avatar, avatarUrl: avatar };
  common_vendor.index.setStorageSync(PROFILE_KEY, next);
  return next;
}
function hasCompletedProfileSetup() {
  try {
    return !!common_vendor.index.getStorageSync(PROFILE_SETUP_DONE_KEY);
  } catch {
    return false;
  }
}
function markProfileSetupDone() {
  common_vendor.index.setStorageSync(PROFILE_SETUP_DONE_KEY, true);
}
function applyLoginProfile({
  nickname = "",
  avatarUrl = "",
  usesDefaultAvatar = false
}) {
  const current = getUserProfile();
  const name = (nickname || "").trim() || current.nickname;
  const url = utils_avatar.resolveAvatarUrl(avatarUrl || current.avatarUrl || current.avatar);
  return setUserProfile({
    ...current,
    nickname: name,
    avatar: url,
    avatarUrl: url,
    isDefault: usesDefaultAvatar
  });
}
function applyManualProfile({ avatar = "", nickname = "", isDefault } = {}) {
  const name = (nickname || "").trim();
  if (!name)
    return null;
  const url = utils_avatar.resolveAvatarUrl(avatar);
  const profile = setUserProfile({
    avatar: url,
    avatarUrl: url,
    nickname: name,
    isDefault: isDefault !== void 0 ? !!isDefault : utils_avatar.isDefaultAvatarUrl(url)
  });
  markProfileSetupDone();
  return profile;
}
exports.applyLoginProfile = applyLoginProfile;
exports.applyManualProfile = applyManualProfile;
exports.getUserProfile = getUserProfile;
exports.hasCompletedProfileSetup = hasCompletedProfileSetup;
exports.markProfileSetupDone = markProfileSetupDone;
