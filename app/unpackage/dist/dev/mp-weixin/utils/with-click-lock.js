"use strict";
function withClickLock(fn) {
  let locked = false;
  return async function lockedFn(...args) {
    if (locked)
      return;
    locked = true;
    try {
      return await fn.apply(this, args);
    } finally {
      locked = false;
    }
  };
}
exports.withClickLock = withClickLock;
