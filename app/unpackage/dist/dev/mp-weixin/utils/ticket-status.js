"use strict";
function ticketStatusClass(status) {
  if (status === 1)
    return "status--processing";
  if (status === 2)
    return "status--done";
  return "status--pending";
}
exports.ticketStatusClass = ticketStatusClass;
