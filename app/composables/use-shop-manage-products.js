import { ref, computed } from 'vue'
import { navigateWithShop } from '@/utils/shop-page-context.js'
import { pickImageFile } from '@/utils/pick-image.js'
import {
	fetchManageProducts,
	fetchManageProductCategories,
	addManageProduct,
	updateManageProduct,
	updateManageProductStock,
	parseProductPoints
} from '@/api/modules/shop-manage.js'

function emptyProductForm() {
	return {
		name: '',
		points: '',
		stock: '',
		addStock: '',
		description: '',
		imagePath: '',
		imageUrl: '',
		categoryIndex: 0
	}
}

/**
 * 店铺管理页 — 商品与分类
 */
export function useShopManageProducts(shopId, { shopInfo, listLoading }) {
	const products = ref([])
	const productCategories = ref([])
	const categories = ref([])
	const productFormVisible = ref(false)
	const categoryFormVisible = ref(false)
	const editingProductId = ref(null)
	const productForm = ref(emptyProductForm())
	const productSubmitting = ref(false)

	const categoryLabels = computed(() => categories.value.map((c) => c.categoryName))

	const currentProductList = computed(() =>
		productCategories.value.flatMap((c) => c.products || [])
	)

	async function loadCategories() {
		if (!shopId.value) return
		const res = await fetchManageProductCategories(Number(shopId.value))
		if (res.ok) {
			categories.value = res.rows || []
		}
	}

	async function loadProducts() {
		if (listLoading.value) return
		listLoading.value = true
		const res = await fetchManageProducts(Number(shopId.value), 1, 50)
		listLoading.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}
		products.value = res.rows
		productCategories.value = res.categories || []
	}

	function findCategoryIndex(categoryId) {
		if (categoryId == null || categoryId === 0) return 0
		const idx = categories.value.findIndex((c) => c.categoryId === categoryId)
		return idx >= 0 ? idx : 0
	}

	function onProductCategoryChange(e) {
		productForm.value.categoryIndex = Number(e.detail.value) || 0
	}

	function selectedCategoryId() {
		const cat = categories.value[productForm.value.categoryIndex]
		return cat?.categoryId ?? null
	}

	function promptAddProduct() {
		editingProductId.value = null
		productForm.value = emptyProductForm()
		void loadCategories()
		productFormVisible.value = true
	}

	function editProduct(item) {
		editingProductId.value = item.productId
		productForm.value = {
			name: item.productName || '',
			points: item.price != null ? String(item.price) : '',
			stock: '',
			addStock: '',
			description: item.description || '',
			imagePath: item.imageUrl || '',
			imageUrl: item.imageUrl || '',
			categoryIndex: findCategoryIndex(item.categoryId)
		}
		void loadCategories()
		productFormVisible.value = true
	}

	function openProductDetail(product, { onProductUpdated } = {}) {
		if (!product?.productId || !shopId.value) return
		navigateWithShop({
			url: `/pages/shop/product-detail?shopId=${shopId.value}&fromManage=1`,
			shop: shopInfo.value,
			success(res) {
				res.eventChannel?.emit('product', product)
				res.eventChannel?.on('product-updated', (patch) => {
					if (!patch?.productId) return
					productCategories.value = productCategories.value.map((cat) => ({
						...cat,
						products: (cat.products || []).map((p) =>
							p.productId === patch.productId ? { ...p, ...patch } : p
						)
					}))
					onProductUpdated?.(patch)
				})
			}
		})
	}

	async function pickProductImage() {
		const path = await pickImageFile()
		if (!path) return
		productForm.value.imagePath = path
		productForm.value.imageUrl = ''
	}

	function clearProductImage() {
		productForm.value.imagePath = ''
		productForm.value.imageUrl = ''
	}

	function previewProductImage() {
		const src = productForm.value.imagePath
		if (!src) return
		uni.previewImage({ urls: [src], current: src })
	}

	function openCategoryForm() {
		categoryFormVisible.value = true
	}

	async function onCategoriesSaved() {
		await loadCategories()
		await loadProducts()
		if (productForm.value.categoryIndex >= categories.value.length) {
			productForm.value.categoryIndex = 0
		}
	}

	function closeProductForm() {
		if (productSubmitting.value) return
		productFormVisible.value = false
		editingProductId.value = null
	}

	async function submitProductForm() {
		const name = productForm.value.name?.trim()
		if (!name) {
			uni.showToast({ title: '请输入商品名称', icon: 'none' })
			return
		}
		const points = parseProductPoints(productForm.value.points)
		if (!points) {
			uni.showToast({ title: '请输入有效积分', icon: 'none' })
			return
		}
		if (productSubmitting.value) return
		productSubmitting.value = true
		const descriptionRaw = productForm.value.description?.trim()
		const description = editingProductId.value ? (descriptionRaw ?? '') : descriptionRaw || null
		const categoryId = selectedCategoryId()
		const imagePath = productForm.value.imagePath?.trim() || ''
		const basePayload = {
			shopId: Number(shopId.value),
			productName: name,
			price: points,
			description,
			categoryId,
			imagePath: imagePath || undefined
		}
		if (editingProductId.value && !imagePath) {
			basePayload.imageUrl = ''
		}
		let res
		if (editingProductId.value) {
			res = await updateManageProduct(editingProductId.value, basePayload)
			if (res.ok) {
				const addRaw = productForm.value.addStock?.trim()
				if (addRaw !== '') {
					const addVal = parseInt(addRaw, 10)
					if (Number.isNaN(addVal) || addVal <= 0) {
						productSubmitting.value = false
						uni.showToast({ title: '追加数量须为正整数', icon: 'none' })
						return
					}
					const stockRes = await updateManageProductStock(editingProductId.value, addVal)
					if (!stockRes.ok) {
						productSubmitting.value = false
						uni.showToast({ title: stockRes.msg || '库存更新失败', icon: 'none' })
						return
					}
				}
			}
		} else {
			const stockRaw = productForm.value.stock?.trim()
			let stock = -1
			if (stockRaw !== '') {
				stock = parseInt(stockRaw, 10)
				if (Number.isNaN(stock) || stock < 0) {
					productSubmitting.value = false
					uni.showToast({ title: '库存须为非负整数，或留空表示无限', icon: 'none' })
					return
				}
			}
			res = await addManageProduct({ ...basePayload, stock })
		}
		productSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
			return
		}
		uni.showToast({ title: editingProductId.value ? '已保存' : '添加成功', icon: 'success' })
		productFormVisible.value = false
		editingProductId.value = null
		loadProducts()
	}

	return {
		products,
		productCategories,
		categories,
		categoryLabels,
		currentProductList,
		productFormVisible,
		categoryFormVisible,
		editingProductId,
		productForm,
		productSubmitting,
		loadCategories,
		loadProducts,
		promptAddProduct,
		editProduct,
		openProductDetail,
		onProductCategoryChange,
		pickProductImage,
		clearProductImage,
		previewProductImage,
		openCategoryForm,
		onCategoriesSaved,
		closeProductForm,
		submitProductForm
	}
}
