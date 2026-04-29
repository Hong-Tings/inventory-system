// 统一 API 响应
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 分页请求参数
export interface PageParams {
  page: number
  size: number
  sort?: string
  order?: string
}

// 基础实体
export interface BaseEntity {
  id: number
  createTime: string
  updateTime: string
}

// 商品
export interface Product extends BaseEntity {
  code: string
  name: string
  categoryId: number | null
  categoryName?: string
  spec: string | null
  unit: string
  purchasePrice: number | null
  salePrice: number | null
  imageUrl: string | null
  minStock: number
  maxStock: number
  status: number
  remark: string | null
}

// 商品分类
export interface Category {
  id: number
  name: string
  parentId: number | null
  sort: number
  children?: Category[]
}

// 仓库
export interface Warehouse extends BaseEntity {
  code: string
  name: string
  contact: string | null
  phone: string | null
  address: string | null
  status: number
  remark: string | null
}

// 库位
export interface WarehouseLocation extends BaseEntity {
  warehouseId: number
  warehouseName?: string
  code: string
  name: string | null
  capacity: number | null
  status: number
  remark: string | null
}

// 供应商
export interface Supplier extends BaseEntity {
  code: string
  name: string
  contact: string | null
  phone: string | null
  address: string | null
  status: number
  remark: string | null
}

// 客户
export interface Customer extends BaseEntity {
  code: string
  name: string
  contact: string | null
  phone: string | null
  address: string | null
  status: number
  remark: string | null
}

// 采购入库单
export interface PurchaseOrder extends BaseEntity {
  orderNo: string
  supplierId: number | null
  supplierName?: string
  warehouseId: number
  warehouseName?: string
  locationId: number | null
  locationName?: string
  totalAmount: number
  totalQuantity: number
  status: number
  operatorId: number | null
  operatorName?: string
  orderDate: string
  remark: string | null
  items?: PurchaseOrderItem[]
}

export interface PurchaseOrderItem {
  id?: number
  orderId?: number
  productId: number
  productName?: string
  productCode?: string
  quantity: number
  unitPrice: number | null
  amount: number | null
  batchNo: string | null
  productionDate: string | null
  expiryDate: string | null
  remark: string | null
}

// 销售出库单
export interface SalesOrder extends BaseEntity {
  orderNo: string
  customerId: number | null
  customerName?: string
  warehouseId: number
  warehouseName?: string
  totalAmount: number
  totalQuantity: number
  salesman: string | null
  externalOrderNo: string | null
  status: number
  operatorId: number | null
  operatorName?: string
  orderDate: string
  remark: string | null
  items?: SalesOrderItem[]
}

export interface SalesOrderItem {
  id?: number
  orderId?: number
  productId: number
  productName?: string
  productCode?: string
  quantity: number
  unitPrice: number | null
  amount: number | null
  batchNo: string | null
  remark: string | null
}

// 库存
export interface Inventory {
  id: number
  productId: number
  productName?: string
  productCode?: string
  warehouseId: number
  warehouseName?: string
  locationId: number | null
  locationName?: string
  batchNo: string | null
  quantity: number
  lockedQty: number
  availableQty?: number
  remark: string | null
}

// 库存流水
export interface InventoryLog {
  id: number
  productId: number
  productName?: string
  warehouseId: number
  warehouseName?: string
  locationId: number | null
  batchNo: string | null
  changeType: string
  changeQty: number
  beforeQty: number
  afterQty: number
  refOrderNo: string | null
  operatorId: number | null
  operatorName?: string
  remark: string | null
  createTime: string
}

// 盘点单
export interface StockTake extends BaseEntity {
  orderNo: string
  warehouseId: number
  warehouseName?: string
  locationId: number | null
  locationName?: string
  takeType: number
  totalItems: number
  diffItems: number
  status: number
  operatorId: number | null
  operatorName?: string
  approverId: number | null
  approverName?: string
  remark: string | null
  items?: StockTakeItem[]
}

export interface StockTakeItem {
  id?: number
  stockTakeId?: number
  productId: number
  productName?: string
  productCode?: string
  locationId: number | null
  locationName?: string
  batchNo: string | null
  bookQty: number
  actualQty: number
  diffQty: number
  diffReason: string | null
}

// 用户
export interface SysUser extends BaseEntity {
  username: string
  realName: string
  position: string | null
  phone: string | null
  email: string | null
  status: number
  roleNames?: string[]
}

// 用户信息（登录返回）
export interface UserInfo {
  id: number
  username: string
  realName: string
  position: string | null
  token: string
  roles: string[]
  isAdmin: boolean
}
