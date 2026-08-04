"use strict";
const config_env = require("../config/env.js");
function isEmptyAvatar(url) {
  return !url || !String(url).trim();
}
function resolveAvatarUrl(url) {
  return isEmptyAvatar(url) ? config_env.DEFAULT_AVATAR_URL : String(url).trim();
}
function isDefaultAvatarUrl(url) {
  const u = String(url || "").trim();
  if (isEmptyAvatar(u))
    return true;
  if (u === config_env.DEFAULT_AVATAR_URL || u.includes("default-avatar"))
    return true;
  if (u === config_env.SERVER_DEFAULT_AVATAR_URL)
    return true;
  try {
    const path = u.startsWith("http") ? new URL(u).pathname : u;
    const serverPath = new URL(config_env.SERVER_DEFAULT_AVATAR_URL).pathname;
    if (path === serverPath)
      return true;
  } catch {
  }
  return false;
}
function toServerAvatar(url) {
  return isDefaultAvatarUrl(url) ? "" : String(url).trim();
}
exports.isDefaultAvatarUrl = isDefaultAvatarUrl;
exports.isEmptyAvatar = isEmptyAvatar;
exports.resolveAvatarUrl = resolveAvatarUrl;
exports.toServerAvatar = toServerAvatar;
