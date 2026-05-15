import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/Login.vue'),
      meta: { title: '登录', noLayout: true },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/dashboard/Dashboard.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'product',
          name: 'Product',
          component: () => import('../views/product/ProductList.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'product/category',
          name: 'ProductCategory',
          component: () => import('../views/product/CategoryList.vue'),
          meta: { title: '商品分类' },
        },
        {
          path: 'warehouse',
          name: 'Warehouse',
          component: () => import('../views/warehouse/WarehouseList.vue'),
          meta: { title: '仓库管理' },
        },
{
          path: 'purchase',
          name: 'Purchase',
          component: () => import('../views/purchase/PurchaseList.vue'),
          meta: { title: '采购入库' },
        },
        {
          path: 'purchase/create',
          name: 'PurchaseCreate',
          component: () => import('../views/purchase/PurchaseForm.vue'),
          meta: { title: '新建入库单' },
        },
        {
          path: 'purchase/:id',
          name: 'PurchaseDetail',
          component: () => import('../views/purchase/PurchaseDetail.vue'),
          meta: { title: '入库单详情' },
        },
        {
          path: 'sales',
          name: 'Sales',
          component: () => import('../views/sales/SalesList.vue'),
          meta: { title: '销售出库' },
        },
        {
          path: 'sales/create',
          name: 'SalesCreate',
          component: () => import('../views/sales/SalesForm.vue'),
          meta: { title: '新建出库单' },
        },
        {
          path: 'sales/:id',
          name: 'SalesDetail',
          component: () => import('../views/sales/SalesDetail.vue'),
          meta: { title: '出库单详情' },
        },
        {
          path: 'inventory',
          name: 'Inventory',
          component: () => import('../views/inventory/InventoryList.vue'),
          meta: { title: '库存查询' },
        },
        {
          path: 'inventory/log',
          name: 'InventoryLog',
          component: () => import('../views/inventory/InventoryLog.vue'),
          meta: { title: '库存流水' },
        },
        {
          path: 'stocktake',
          name: 'StockTake',
          component: () => import('../views/stocktake/StockTakeList.vue'),
          meta: { title: '库存盘点' },
        },
        {
          path: 'stocktake/create',
          name: 'StockTakeCreate',
          component: () => import('../views/stocktake/StockTakeForm.vue'),
          meta: { title: '新建盘点单' },
        },
        {
          path: 'stocktake/:id',
          name: 'StockTakeDetail',
          component: () => import('../views/stocktake/StockTakeDetail.vue'),
          meta: { title: '盘点单详情' },
        },
        {
          path: 'transfer',
          name: 'Transfer',
          component: () => import('../views/transfer/TransferList.vue'),
          meta: { title: '库存调拨' },
        },
        {
          path: 'transfer/create',
          name: 'TransferCreate',
          component: () => import('../views/transfer/TransferForm.vue'),
          meta: { title: '新建调拨单' },
        },
        {
          path: 'transfer/:id',
          name: 'TransferDetail',
          component: () => import('../views/transfer/TransferDetail.vue'),
          meta: { title: '调拨单详情' },
        },
        {
          path: 'report',
          name: 'Report',
          component: () => import('../views/report/ReportView.vue'),
          meta: { title: '统计报表' },
        },
        {
          path: 'system',
          redirect: '/system/user',
        },
        {
          path: 'system/user',
          name: 'SysUser',
          component: () => import('../views/system/UserList.vue'),
          meta: { title: '用户管理', requireAdmin: true },
        },
        {
          path: 'system/log',
          name: 'SysLog',
          component: () => import('../views/system/OperationLog.vue'),
          meta: { title: '操作日志', requireAdmin: true },
        },
        {
          path: 'system/config',
          name: 'SysConfig',
          component: () => import('../views/system/ConfigList.vue'),
          meta: { title: '系统配置', requireAdmin: true },
        },
        {
          path: 'system/recycle',
          name: 'SysRecycle',
          component: () => import('../views/system/RecycleBin.vue'),
          meta: { title: '已作废列表', requireAdmin: true },
        },
        {
          path: 'supplier',
          name: 'Supplier',
          component: () => import('../views/product/SupplierList.vue'),
          meta: { title: '供应商管理' },
        },
        {
          path: 'customer',
          name: 'Customer',
          component: () => import('../views/product/CustomerList.vue'),
          meta: { title: '客户管理' },
        },
        {
          path: 'help',
          name: 'Help',
          component: () => import('../views/help/HelpGuide.vue'),
          meta: { title: '使用说明' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login') return next()

  if (!userStore.isLoggedIn) return next('/login')

  // 刷新页面后 token 还在但 userInfo 丢失，重新获取
  if (!userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch {
      userStore.logout()
      return next('/login')
    }
  }

  // 需要管理员权限的页面
  if (to.meta?.requireAdmin && !userStore.isAdmin) {
    return next('/dashboard')
  }

  next()
})

export default router
