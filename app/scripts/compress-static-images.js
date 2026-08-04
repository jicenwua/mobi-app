/**
 * 压缩 static 目录下过大的 PNG，避免微信小程序主包超过 2MB。
 * 用法：node scripts/compress-static-images.js
 */
const fs = require('fs')
const path = require('path')
const sharp = require('sharp')

const STATIC_DIR = path.join(__dirname, '../static')

/** 按文件名关键字设定最长边（px） */
function targetMaxSide(filename) {
	if (/尚品|发艺/.test(filename)) return 192
	if (/券/.test(filename)) return 128
	if (/标签|下架|售罄/.test(filename)) return 128
	return 256
}

async function compressFile(filename) {
	const filePath = path.join(STATIC_DIR, filename)
	const before = fs.statSync(filePath).size
	if (before < 80 * 1024) {
		console.log(`skip ${filename} (${Math.round(before / 1024)}KB)`)
		return
	}

	const meta = await sharp(filePath).metadata()
	const maxSide = targetMaxSide(filename)
	const currentMax = Math.max(meta.width || 0, meta.height || 0)

	let pipeline = sharp(filePath)
	if (currentMax > maxSide) {
		pipeline = pipeline.resize(maxSide, maxSide, {
			fit: 'inside',
			withoutEnlargement: true
		})
	}

	const buf = await pipeline
		.png({ compressionLevel: 9, palette: true, quality: 80 })
		.toBuffer()

	if (buf.length >= before) {
		console.log(`keep ${filename} (${Math.round(before / 1024)}KB, no gain)`)
		return
	}

	fs.writeFileSync(filePath, buf)
	console.log(
		`${filename}: ${Math.round(before / 1024)}KB -> ${Math.round(buf.length / 1024)}KB`
	)
}

async function main() {
	const files = fs.readdirSync(STATIC_DIR).filter((f) => f.toLowerCase().endsWith('.png'))
	for (const file of files) {
		await compressFile(file)
	}
}

main().catch((err) => {
	console.error(err)
	process.exit(1)
})
