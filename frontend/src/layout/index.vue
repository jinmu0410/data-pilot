<template>
  <div class="app-shell" :class="{ 'is-collapsed': menuCollapsed }">
    <div v-if="isMobile && mobileOpen" class="mobile-mask" @click="mobileOpen = false" />

    <aside class="sidebar" :class="{ 'mobile-open': mobileOpen }">
      <div class="brand" @click="router.push('/dashboard')">
        <div class="brand-mark">
          <span class="brand-grid"><i /><i /><i /><i /></span>
        </div>
        <transition name="brand-fade">
          <div v-if="!menuCollapsed || isMobile" class="brand-copy">
            <strong>DataPilot</strong>
            <span>{{ t('layout.brandSub') }}</span>
          </div>
        </transition>
      </div>

      <el-dropdown
        v-if="!menuCollapsed || isMobile"
        trigger="click"
        class="workspace-switch"
        @command="handleSwitchWorkspace"
      >
        <div class="workspace-card">
          <span class="workspace-dot" />
          <div>
            <small>{{ t('layout.currentWorkspace') }}</small>
            <strong>{{ workspaceName }}</strong>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="ws in authStore.workspaces"
              :key="ws.id"
              :command="ws"
              :class="{ 'is-active': ws.id === authStore.workspace?.id }"
            >
              {{ ws.name }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-menu
        :default-active="activeMenu"
        :collapse="menuCollapsed && !isMobile"
        :collapse-transition="false"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="var(--dp-sidebar-text)"
        active-text-color="#ffffff"
        @select="mobileOpen = false"
      >
        <div v-if="!menuCollapsed || isMobile" class="menu-caption">{{ t('menu.workspace') }}</div>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>{{ t('menu.workspace') }}</template>
        </el-menu-item>

        <div v-if="!menuCollapsed || isMobile" class="menu-caption">{{ t('menu.dataAssets') }}</div>
        <el-sub-menu index="integration">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>{{ t('menu.integration') }}</span>
          </template>
          <el-menu-item index="/datasource">{{ t('menu.datasource') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="development">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>{{ t('menu.development') }}</span>
          </template>
          <el-menu-item index="/dataflow">{{ t('menu.dataflow') }}</el-menu-item>
          <el-menu-item index="/dataflow/instance">{{ t('menu.dataflowInstance') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="service">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>{{ t('menu.service') }}</span>
          </template>
          <el-menu-item index="/service/api">{{ t('menu.api') }}</el-menu-item>
        </el-sub-menu>

        <div v-if="!menuCollapsed || isMobile" class="menu-caption">{{ t('menu.platformSettings') }}</div>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>{{ t('menu.system') }}</span>
          </template>
          <el-menu-item index="/system/user">{{ t('menu.user') }}</el-menu-item>
          <el-menu-item index="/system/role">{{ t('menu.role') }}</el-menu-item>
          <el-menu-item index="/system/permission">{{ t('menu.permission') }}</el-menu-item>
          <el-menu-item index="/system/workspace">{{ t('menu.workspaceManage') }}</el-menu-item>
          <el-menu-item index="/system/loginlog">{{ t('menu.loginlog') }}</el-menu-item>
          <el-menu-item index="/system/config">{{ t('menu.config') }}</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <button class="collapse-button" type="button" @click="toggleSidebar">
        <el-icon><Fold v-if="!menuCollapsed" /><Expand v-else /></el-icon>
        <span v-if="!menuCollapsed">{{ t('layout.collapse') }}</span>
      </button>
    </aside>

    <main class="main-shell">
      <header class="topbar">
        <div class="topbar-left">
          <button class="mobile-menu" type="button" @click="mobileOpen = true">
            <el-icon><Menu /></el-icon>
          </button>
          <div class="page-heading">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>{{ t('layout.breadcrumbRoot') }}</el-breadcrumb-item>
              <el-breadcrumb-item>{{ parentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
            <strong>{{ pageTitle }}</strong>
          </div>
        </div>

        <div class="topbar-actions">
          <button class="search-trigger" type="button" @click="commandVisible = true">
            <el-icon><Search /></el-icon>
            <span>{{ t('layout.search') }}</span>
            <kbd>⌘ K</kbd>
          </button>
          <button class="icon-button lang-button" type="button" :title="locale === 'zh-CN' ? 'Switch to English' : '切换到中文'" @click="toggleLocale">
            {{ locale === 'zh-CN' ? 'EN' : '中' }}
          </button>
          <button class="icon-button" type="button" :title="isDark ? t('layout.themeLight') : t('layout.themeDark')" @click="toggleTheme">
            <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
          </button>
          <button class="icon-button notice-button" type="button" :title="t('layout.notice')">
            <el-icon><Bell /></el-icon>
            <span class="notice-dot" />
          </button>
          <span class="topbar-divider" />
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="34" class="avatar">{{ avatarText }}</el-avatar>
              <span class="user-copy">
                <strong>{{ username }}</strong>
                <small>{{ t('layout.admin') }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/system/user')">{{ t('layout.profile') }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">{{ t('layout.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="context-bar">
        <span class="context-home"><el-icon><House /></el-icon></span>
        <span class="context-tab"><i />{{ pageTitle }}</span>
        <div class="context-tail" />
      </div>

      <section class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </section>
    </main>

    <el-dialog v-model="commandVisible" :title="t('layout.quickNavigate')" width="520px" class="command-dialog">
      <el-input v-model="commandKeyword" size="large" :placeholder="t('layout.searchPlaceholder')" :prefix-icon="Search" autofocus />
      <div class="command-list">
        <button
          v-for="item in filteredCommands"
          :key="item.path"
          type="button"
          class="command-item"
          @click="jump(item.path)"
        >
          <span class="command-icon"><el-icon><Position /></el-icon></span>
          <span><strong>{{ item.name }}</strong><small>{{ item.group }}</small></span>
          <el-icon><ArrowRight /></el-icon>
        </button>
        <el-empty v-if="filteredCommands.length === 0" :description="t('layout.noMatch')" :image-size="72" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { getStoredLocale, setLocale, type Locale } from '../i18n'
import type { WorkspaceData } from '../api/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const locale = ref<Locale>(getStoredLocale())
const menuCollapsed = ref(localStorage.getItem('dp-menu-collapsed') === '1')
const isMobile = ref(window.innerWidth <= 900)
const mobileOpen = ref(false)
const isDark = ref(localStorage.getItem('dp-theme') === 'dark')
const commandVisible = ref(false)
const commandKeyword = ref('')

const commands = computed(() => [
  { name: t('menu.workspace'), group: t('menu.overview'), path: '/dashboard' },
  { name: t('menu.datasource'), group: t('menu.integration'), path: '/datasource' },
  { name: t('menu.dataflow'), group: t('menu.development'), path: '/dataflow' },
  { name: t('menu.dataflowInstance'), group: t('menu.development'), path: '/dataflow/instance' },
  { name: t('menu.api'), group: t('menu.service'), path: '/service/api' },
  { name: t('menu.user'), group: t('menu.system'), path: '/system/user' },
  { name: t('menu.role'), group: t('menu.system'), path: '/system/role' },
  { name: t('menu.permission'), group: t('menu.system'), path: '/system/permission' },
  { name: t('menu.workspaceManage'), group: t('menu.system'), path: '/system/workspace' },
  { name: t('menu.loginlog'), group: t('menu.system'), path: '/system/loginlog' },
  { name: t('menu.config'), group: t('menu.system'), path: '/system/config' }
])

const pageTitle = computed(() => {
  const title = route.meta.title as string
  return title ? t(title) : t('menu.workspace')
})
const parentTitle = computed(() => {
  if (route.path.startsWith('/system')) return t('menu.system')
  if (route.path.startsWith('/service')) return t('menu.service')
  if (route.path.startsWith('/dataflow')) return t('menu.development')
  if (route.path.startsWith('/datasource')) return t('menu.integration')
  return t('menu.workspace')
})
const activeMenu = computed(() => {
  if (route.path.startsWith('/dataflow/edit') || route.path.startsWith('/dataflow/history')) return '/dataflow'
  return route.path
})
const username = computed(() => authStore.user?.username || 'admin')
const avatarText = computed(() => username.value.charAt(0).toUpperCase())
const workspaceName = computed(() => authStore.workspace?.name || t('layout.defaultWorkspace'))
const filteredCommands = computed(() => {
  const keyword = commandKeyword.value.trim().toLowerCase()
  if (!keyword) return commands.value
  return commands.value.filter((item) => `${item.name}${item.group}`.toLowerCase().includes(keyword))
})

function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
}

function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('dp-theme', isDark.value ? 'dark' : 'light')
  applyTheme()
}

function toggleLocale() {
  locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  setLocale(locale.value)
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
    return
  }
  menuCollapsed.value = !menuCollapsed.value
  localStorage.setItem('dp-menu-collapsed', menuCollapsed.value ? '1' : '0')
}

function onResize() {
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) mobileOpen.value = false
}

function onHotkey(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    commandVisible.value = true
  }
}

function jump(path: string) {
  commandVisible.value = false
  commandKeyword.value = ''
  router.push(path)
}

onMounted(() => {
  applyTheme()
  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onHotkey)
  if (authStore.isLoggedIn && !authStore.user) authStore.loadUserAndWorkspace().catch(() => {})
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onHotkey)
})

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

function handleSwitchWorkspace(ws: WorkspaceData) {
  if (ws.id === authStore.workspace?.id) return
  authStore.switchWorkspace(ws)
  // 切换后重新加载当前页，以新工作空间重新拉取数据
  window.location.reload()
}
</script>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 238px minmax(0, 1fr);
  min-height: 100%;
  background: var(--dp-page-bg);
  transition: grid-template-columns 0.2s ease;
}

.app-shell.is-collapsed {
  grid-template-columns: 72px minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 4%, rgba(91, 115, 247, 0.2), transparent 27%),
    var(--dp-sidebar-bg);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.brand {
  display: flex;
  align-items: center;
  min-height: 68px;
  padding: 0 19px;
  cursor: pointer;
}

.brand-mark {
  display: grid;
  flex: 0 0 34px;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #fff;
  background: linear-gradient(145deg, #7690ff, #4f6df5 55%, #7c56e8);
  border-radius: 10px;
  box-shadow: 0 7px 20px rgba(79, 109, 245, 0.35);
}

.brand-grid {
  display: grid;
  grid-template-columns: repeat(2, 5px);
  gap: 3px;
}

.brand-grid i {
  width: 5px;
  height: 5px;
  background: #fff;
  border-radius: 2px;
}

.brand-grid i:nth-child(2),
.brand-grid i:nth-child(3) {
  opacity: 0.6;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: 11px;
  line-height: 1.15;
}

.brand-copy strong {
  color: #f8faff;
  font-size: 16px;
  font-weight: 720;
  letter-spacing: 0.2px;
}

.brand-copy span {
  margin-top: 4px;
  color: #7f8ba1;
  font-size: 10px;
  letter-spacing: 2.5px;
}

.workspace-switch {
  display: block;
}

.workspace-card {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 4px 13px 13px;
  padding: 11px 10px;
  color: #dbe3f2;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.workspace-card:hover {
  border-color: rgba(255, 255, 255, 0.18);
}

.workspace-card > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.workspace-card small {
  color: #748198;
  font-size: 10px;
}

.workspace-card strong {
  margin-top: 2px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 560;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-dot {
  width: 7px;
  height: 7px;
  background: #42d69b;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(66, 214, 155, 0.12);
}

.sidebar-menu {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  border-right: 0;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.menu-caption {
  padding: 14px 22px 6px;
  color: #59657a;
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 1.3px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  margin: 2px 11px;
  padding: 0 13px !important;
  border-radius: 8px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: #e9eeff !important;
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: linear-gradient(100deg, rgba(91, 115, 247, 0.95), rgba(91, 115, 247, 0.72)) !important;
  box-shadow: 0 7px 18px rgba(31, 52, 155, 0.25);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: 0;
  padding-left: 47px !important;
  color: #8d99ad;
  background: transparent;
}

.sidebar-menu.el-menu--collapse :deep(.el-menu-item),
.sidebar-menu.el-menu--collapse :deep(.el-sub-menu__title) {
  justify-content: center;
  margin-right: 10px;
  margin-left: 10px;
  padding: 0 !important;
}

.collapse-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 49px;
  color: #79869b;
  background: rgba(0, 0, 0, 0.1);
  border: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
}

.collapse-button:hover {
  color: #e7ecf5;
}

.main-shell {
  min-width: 0;
  min-height: 100vh;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 24px;
  background: var(--dp-header-bg);
  border-bottom: 1px solid var(--dp-border);
  backdrop-filter: blur(14px);
}

.topbar-left,
.topbar-actions,
.user-info {
  display: flex;
  align-items: center;
}

.page-heading {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.page-heading :deep(.el-breadcrumb__inner) {
  color: var(--dp-text-muted);
  font-size: 10px;
  font-weight: 450;
}

.page-heading strong {
  color: var(--dp-text);
  font-size: 16px;
  font-weight: 680;
}

.topbar-actions {
  gap: 8px;
}

.search-trigger,
.icon-button,
.mobile-menu {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  color: var(--dp-text-secondary);
  background: var(--dp-surface-soft);
  border: 1px solid var(--dp-border);
  border-radius: 8px;
  cursor: pointer;
}

.search-trigger {
  gap: 8px;
  width: 188px;
  padding: 0 9px;
}

.search-trigger span {
  flex: 1;
  text-align: left;
}

.search-trigger kbd {
  padding: 2px 6px;
  color: var(--dp-text-muted);
  background: var(--dp-surface);
  border: 1px solid var(--dp-border);
  border-radius: 5px;
  font-family: inherit;
  font-size: 10px;
}

.icon-button,
.mobile-menu {
  width: 36px;
  font-size: 16px;
}

.lang-button {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.icon-button:hover,
.search-trigger:hover,
.mobile-menu:hover {
  color: var(--dp-primary);
  border-color: #cbd3fb;
}

.notice-button {
  position: relative;
}

.notice-dot {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 6px;
  height: 6px;
  background: var(--dp-danger);
  border: 1px solid var(--dp-surface);
  border-radius: 50%;
}

.topbar-divider {
  width: 1px;
  height: 24px;
  margin: 0 4px;
  background: var(--dp-border);
}

.user-info {
  gap: 9px;
  color: var(--dp-text-secondary);
  cursor: pointer;
  outline: none;
}

.avatar {
  color: #fff;
  background: linear-gradient(145deg, #657ff9, #7656e8);
  box-shadow: 0 5px 12px rgba(79, 109, 245, 0.24);
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.18;
}

.user-copy strong {
  color: var(--dp-text);
  font-size: 12px;
  font-weight: 650;
}

.user-copy small {
  margin-top: 3px;
  color: var(--dp-text-muted);
  font-size: 9px;
}

.context-bar {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 18px;
  background: var(--dp-surface);
  border-bottom: 1px solid var(--dp-border);
}

.context-home {
  display: grid;
  width: 28px;
  color: var(--dp-text-muted);
  place-items: center;
}

.context-tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  height: 27px;
  padding: 0 12px;
  color: var(--dp-primary);
  background: var(--dp-primary-soft);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 550;
}

.context-tab i {
  width: 5px;
  height: 5px;
  background: var(--dp-primary);
  border-radius: 50%;
}

.context-tail {
  flex: 1;
}

.page-content {
  min-height: calc(100vh - 106px);
  padding: 20px;
}

.mobile-menu {
  display: none;
  margin-right: 12px;
}

.command-list {
  display: grid;
  gap: 7px;
  max-height: 390px;
  margin-top: 14px;
  overflow-y: auto;
}

.command-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 10px;
  color: var(--dp-text-secondary);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 9px;
  cursor: pointer;
}

.command-item:hover {
  color: var(--dp-primary);
  background: var(--dp-primary-soft);
  border-color: rgba(79, 109, 245, 0.14);
}

.command-item > span:nth-child(2) {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.command-item strong {
  color: var(--dp-text);
  font-size: 13px;
}

.command-item small {
  margin-top: 2px;
  color: var(--dp-text-muted);
  font-size: 10px;
}

.command-icon {
  display: grid;
  width: 34px;
  height: 34px;
  color: var(--dp-primary);
  background: var(--dp-primary-soft);
  border-radius: 8px;
  place-items: center;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.page-fade-leave-to {
  opacity: 0;
}

.brand-fade-enter-active,
.brand-fade-leave-active {
  transition: opacity 0.12s ease;
}

.brand-fade-enter-from,
.brand-fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .app-shell,
  .app-shell.is-collapsed {
    grid-template-columns: minmax(0, 1fr);
  }

  .sidebar {
    position: fixed;
    left: 0;
    width: 238px;
    transform: translateX(-102%);
    transition: transform 0.22s ease;
  }

  .sidebar.mobile-open {
    transform: translateX(0);
  }

  .mobile-mask {
    position: fixed;
    z-index: 29;
    width: 100%;
    height: 100%;
    background: rgba(5, 11, 24, 0.5);
    backdrop-filter: blur(2px);
  }

  .mobile-menu {
    display: inline-flex;
  }

  .collapse-button {
    display: none;
  }

  .topbar {
    height: 62px;
    padding: 0 14px;
  }

  .search-trigger,
  .topbar-divider,
  .user-copy,
  .user-info > .el-icon {
    display: none;
  }

  .context-bar {
    height: 36px;
    padding: 0 10px;
  }

  .page-content {
    min-height: calc(100vh - 98px);
    padding: 12px;
  }
}

@media (max-width: 520px) {
  .page-heading :deep(.el-breadcrumb) {
    display: none;
  }

  .topbar-actions .notice-button {
    display: none;
  }
}
</style>
