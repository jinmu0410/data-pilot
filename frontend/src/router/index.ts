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
          meta: { title: 'page.dashboard' }
        },
        {
          path: 'datasource',
          name: 'datasource',
          component: () => import('../views/datasource/index.vue'),
          meta: { title: 'page.datasource' }
        },
        {
          path: 'dataflow',
          name: 'dataflow',
          component: () => import('../views/dataflow/index.vue'),
          meta: { title: 'page.dataflow' }
        },
        {
          path: 'dataflow/edit/:id',
          name: 'dataflow-editor',
          component: () => import('../views/dataflow/editor.vue'),
          meta: { title: 'page.dataflowEditor' }
        },
        {
          path: 'dataflow/history',
          name: 'dataflow-history',
          component: () => import('../views/dataflow/history.vue'),
          meta: { title: 'page.dataflowHistory' }
        },
        {
          path: 'dataflow/instance',
          name: 'dataflow-instance',
          component: () => import('../views/dataflow/instance.vue'),
          meta: { title: 'page.dataflowInstance' }
        },
        {
          path: 'service/api',
          name: 'service-api',
          component: () => import('../views/service/api.vue'),
          meta: { title: 'page.api' }
        },
        {
          path: 'system/user',
          name: 'system-user',
          component: () => import('../views/system/user/index.vue'),
          meta: { title: 'page.user' }
        },
        {
          path: 'system/role',
          name: 'system-role',
          component: () => import('../views/system/role/index.vue'),
          meta: { title: 'page.role' }
        },
        {
          path: 'system/permission',
          name: 'system-permission',
          component: () => import('../views/system/permission/index.vue'),
          meta: { title: 'page.permission' }
        },
        {
          path: 'system/workspace',
          name: 'system-workspace',
          component: () => import('../views/system/workspace/index.vue'),
          meta: { title: 'page.workspace' }
        },
        {
          path: 'system/loginlog',
          name: 'system-loginlog',
          component: () => import('../views/system/loginlog/index.vue'),
          meta: { title: 'page.loginlog' }
        },
        {
          path: 'system/config',
          name: 'system-config',
          component: () => import('../views/system/config/index.vue'),
          meta: { title: 'page.config' }
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
