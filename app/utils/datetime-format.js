/**
 * ISO 日期时间展示：2024-01-01T12:00:00 → 2024-01-01 12:00:00
 * @param {string} iso
 * @param {{ empty?: string, maxLen?: number }} [opts]
 */
export function formatDateTime(iso, { empty = '—', maxLen = 19 } = {}) {
	if (!iso) return empty
	return String(iso).replace('T', ' ').slice(0, maxLen)
}
