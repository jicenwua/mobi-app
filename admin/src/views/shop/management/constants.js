export const SHOP_CATEGORIES = [
  { id: 1, name: '餐饮' },
  { id: 2, name: '零售' },
  { id: 3, name: '服务' },
  { id: 4, name: '其他' }
]

export const SHOP_UPLOAD_LAYOUT = [
  {
    cols: [
      { field: 'idCardFront', label: '身份证正面', required: true },
      { field: 'idCardBack', label: '身份证背面', required: true }
    ]
  },
  {
    cols: [{ field: 'businessLicensePic', label: '营业执照', required: true }]
  },
  {
    cols: [
      { field: 'shopExterior', label: '店铺外景', required: true },
      { field: 'shopInterior', label: '店铺内景', required: true }
    ]
  }
]

export const SHOP_IMAGE_FIELDS = SHOP_UPLOAD_LAYOUT.flatMap((row) => row.cols.map((c) => c.field))

export function createEmptyShopForm() {
  return {
    parentShopCode: '',
    shopName: '',
    ratio: 10,
    managerUserId: undefined,
    categoryId: undefined,
    province: '',
    city: '',
    district: '',
    address: '',
    legalPerson: '',
    idCardNo: '',
    businessLicenseNo: '',
    idCardFront: '',
    idCardBack: '',
    businessLicensePic: '',
    shopExterior: '',
    shopInterior: ''
  }
}
