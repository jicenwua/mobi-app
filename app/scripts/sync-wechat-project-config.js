/**
 * 修复微信开发者工具 project.config.json
 * HBuilderX 编译会把源码目录的 miniprogramRoot 复制到 mp-weixin，导致在编译目录打开时找不到 app.json
 */
const fs = require('fs')
const path = require('path')

const appRoot = path.resolve(__dirname, '..')
const distDir = path.join(appRoot, 'unpackage', 'dist', 'dev', 'mp-weixin')
const distTemplatePath = path.join(__dirname, 'wechat-dist.project.config.json')
const distConfigPath = path.join(distDir, 'project.config.json')

function readJson(filePath) {
	return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function writeJson(filePath, data) {
	fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, 'utf8')
}

function needsDistFix(config) {
	const root = config?.miniprogramRoot
	if (root == null) return false
	const s = String(root).trim()
	return s !== ''
}

function main() {
	if (!fs.existsSync(path.join(distDir, 'app.json'))) {
		console.error(
			'[fix:wechat] 未找到编译产物，请先执行：\n' +
				'  HBuilderX：运行 → 运行到小程序模拟器 → 微信开发者工具\n' +
				'  完成后再执行：npm run fix:wechat'
		)
		process.exit(1)
	}

	if (!fs.existsSync(distTemplatePath)) {
		console.error('[fix:wechat] 缺少模板 scripts/wechat-dist.project.config.json')
		process.exit(1)
	}

	let fixed = false
	if (fs.existsSync(distConfigPath)) {
		const current = readJson(distConfigPath)
		if (needsDistFix(current)) {
			const template = readJson(distTemplatePath)
			template.projectname = current.projectname || template.projectname
			template.appid = current.appid || template.appid
			writeJson(distConfigPath, template)
			fixed = true
			console.log('[fix:wechat] 已修正编译目录配置（miniprogramRoot → 空）')
		}
	} else {
		fs.copyFileSync(distTemplatePath, distConfigPath)
		fixed = true
		console.log('[fix:wechat] 已写入编译目录 project.config.json')
	}

	if (!fixed) {
		console.log('[fix:wechat] 编译目录配置已正确，无需修改')
	}

	console.log('[fix:wechat] 请在微信开发者工具中导入：')
	console.log(' ', distDir)
}

main()
