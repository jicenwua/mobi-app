"use strict";
const common_vendor = require("../common/vendor.js");
const THEME_KEY = "app_theme_is_dark";
function loadThemeFromStorage() {
  try {
    const stored = common_vendor.index.getStorageSync(THEME_KEY);
    if (stored === "dark")
      return true;
    if (stored === "light")
      return false;
  } catch {
  }
  return false;
}
function saveThemeToStorage(isDark) {
  try {
    common_vendor.index.setStorageSync(THEME_KEY, isDark ? "dark" : "light");
  } catch {
  }
}
function useTheme() {
  const isDark = common_vendor.ref(loadThemeFromStorage());
  function loadTheme() {
    isDark.value = loadThemeFromStorage();
  }
  function toggleTheme() {
    isDark.value = !isDark.value;
    saveThemeToStorage(isDark.value);
  }
  return { isDark, loadTheme, toggleTheme };
}
exports.useTheme = useTheme;
