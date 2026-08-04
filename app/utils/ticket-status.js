/** 工单状态 → CSS class */
export function ticketStatusClass(status) {
	if (status === 1) return 'status--processing'
	if (status === 2) return 'status--done'
	return 'status--pending'
}
