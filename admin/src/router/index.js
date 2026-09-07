import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

// 常量路由（不需要权限）
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true }
      }
    ]
  },
  // 店铺管理相关路由（不需要权限控制的详情页）
  {
    path: '/shop/manage/detail/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/shop/management/detail.vue'),
        name: 'ShopDetail',
        meta: { title: '店铺详情', noCache: true }
      }
    ]
  },
  // 添加店铺页面
  {
    path: '/shop/manage/add',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/shop/management/form.vue'),
        name: 'ShopAdd',
        meta: { title: '添加店铺', noCache: true }
      }
    ]
  },
  // 编辑店铺页面
  {
    path: '/shop/manage/edit/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/shop/management/form.vue'),
        name: 'ShopEdit',
        meta: { title: '编辑店铺', noCache: true }
      }
    ]
  },
  // 工单信息（只读）
  {
    path: '/ticket/info/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/ticket/info/index.vue'),
        name: 'TicketInfo',
        meta: { title: '工单详情', noCache: true }
      }
    ]
  },
  // 工单处理（对话回复）
  {
    path: '/ticket/handle/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/ticket/handle/index.vue'),
        name: 'TicketHandle',
        meta: { title: '工单处理', noCache: true }
      }
    ]
  },
  // 兼容旧链接
  {
    path: '/ticket/detail/:id',
    redirect: (to) => `/ticket/info/${to.params.id}`
  }
]

// 路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes
})

export default router
