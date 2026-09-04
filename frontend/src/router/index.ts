import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '../api/request'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/index.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'datasource',
          name: 'datasource',
          component: () => import('../views/datasource/index.vue'),
          meta: { title: '数据源' }
        },
        {
          path: 'dataflow',
          name: 'dataflow',
          component: () => import('../views/dataflow/index.vue'),
          meta: { title: '任务流' }
        },
        {
          path: 'dataflow/edit/:id',
          name: 'dataflow-editor',
          component: () => import('../views/dataflow/editor.vue'),
          meta: { title: '任务流画布' }
        },
        {
          path: 'dataflow/history',
          name: 'dataflow-history',
          component: () => import('../views/dataflow/history.vue'),
          meta: { title: '发布记录' }
        },
        {
          path: 'dataflow/instance',
          name: 'dataflow-instance',
          component: () => import('../views/dataflow/instance.vue'),
          meta: { title: '任务流实例' }
        },
        {
          path: 'service/api',
          name: 'service-api',
          component: () => import('../views/service/api.vue'),
          meta: { title: 'API 管理' }
        },
        {
          path: 'system/user',
          name: 'system-user',
          component: () => import('../views/system/user/index.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'system/role',
          name: 'system-role',
          component: () => import('../views/system/role/index.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'system/permission',
          name: 'system-permission',
          component: () => import('../views/system/permission/index.vue'),
          meta: { title: '权限管理' }
        },
        {
          path: 'system/workspace',
          name: 'system-workspace',
          component: () => import('../views/system/workspace/index.vue'),
          meta: { title: '工作空间' }
        },
        {
          path: 'system/loginlog',
          name: 'system-loginlog',
          component: () => import('../views/system/loginlog/index.vue'),
          meta: { title: '登录日志' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  return true
})

export default router
