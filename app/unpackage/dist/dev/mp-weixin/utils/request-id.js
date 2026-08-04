"use strict";
function createRequestId() {
  const template = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx";
  return template.replace(/[xy]/g, (char) => {
    const rand = Math.random() * 16 | 0;
    const value = char === "x" ? rand : rand & 3 | 8;
    return value.toString(16);
  });
}
exports.createRequestId = createRequestId;
