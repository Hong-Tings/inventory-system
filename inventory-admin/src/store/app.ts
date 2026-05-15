import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type RouteMeta = {
  path: string
  title: string
  icon?: string
  children?: RouteMeta[]
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const menus = ref<RouteMeta[]>([
    {
      path: '/dashboard',
      title: '工作台',
      icon: 'Odometer',
    },
    {
      path: '/product',
      title: '商品管理',
      icon: 'Goods',
      children: [
        { path: '/product', title: '商品列表', icon: 'Goods' },
        { path: '/product/category', title: '商品分类', icon: 'Collection' },
      ],
    },
    {
      path: '/supplier',
      title: '供应商管理',
      icon: 'UserFilled',
    },
    {
      path: '/customer',
      title: '客户管理',
      icon: 'Avatar',
    },
    {
      path: '/warehouse',
      title: '仓库管理',
      icon: 'HomeFilled',
    },
    {
      path: '/purchase',
      title: '采购入库',
      icon: 'Download',
    },
    {
      path: '/sales',
      title: '销售出库',
      icon: 'Upload',
    },
    {
      path: '/inventory',
      title: '库存管理',
      icon: 'List',
      children: [
        { path: '/inventory', title: '库存列表', icon: 'List' },
        { path: '/inventory/log', title: '库存流水', icon: 'Document' },
      ],
    },
    {
      path: '/stocktake',
      title: '库存盘点',
      icon: 'Search',
    },
    {
      path: '/transfer',
      title: '库存调拨',
      icon: 'Refresh',
    },
    {
      path: '/report',
      title: '统计报表',
      icon: 'DataAnalysis',
    },
    {
      path: '/system',
      title: '系统管理',
      icon: 'Setting',
      children: [
        { path: '/system/user', title: '员工管理', icon: 'User' },
        { path: '/system/log', title: '操作日志', icon: 'Document' },
        { path: '/system/recycle', title: '已作废列表', icon: 'Delete' },
      ],
    },
    {
      path: '/help',
      title: '使用说明',
      icon: 'InfoFilled',
    },
  ])

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, menus, toggleSidebar }
})
