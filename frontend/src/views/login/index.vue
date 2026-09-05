<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-noise" />
      <div class="brand-head">
        <span class="brand-mark"><i /><i /><i /><i /></span>
        <span><strong>DataPilot</strong><small>{{ t('login.brandSub') }}</small></span>
      </div>
      <div class="brand-message">
        <span class="message-label">DATA OPERATING SYSTEM</span>
        <h1>{{ t('login.tagline1') }}<br><em>{{ t('login.tagline2') }}</em></h1>
        <p>{{ t('login.taglineDesc') }}</p>
        <div class="feature-row">
          <span><el-icon><Connection /></el-icon>{{ t('login.featureConnect') }}</span>
          <span><el-icon><Operation /></el-icon>{{ t('login.featureOrchestrate') }}</span>
          <span><el-icon><DataAnalysis /></el-icon>{{ t('login.featurePublish') }}</span>
        </div>
      </div>
      <div class="data-visual" aria-hidden="true">
        <span class="data-line line-a" /><span class="data-line line-b" /><span class="data-line line-c" />
        <span class="data-node node-a"><el-icon><Coin /></el-icon></span>
        <span class="data-node node-b"><el-icon><Share /></el-icon></span>
        <span class="data-node node-c"><el-icon><Link /></el-icon></span>
      </div>
      <span class="brand-foot">DataPilot Platform · Enterprise Edition</span>
    </section>

    <main class="login-main">
      <div class="mobile-brand">
        <span class="brand-mark"><i /><i /><i /><i /></span>
        <strong>DataPilot</strong>
      </div>
      <div class="login-card">
        <div class="login-header">
          <span class="welcome-pill"><i /> WELCOME BACK</span>
          <h2>{{ t('login.title') }}</h2>
          <p>{{ t('login.subtitle') }}</p>
        </div>
        <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
          <el-form-item :label="t('login.username')">
            <el-input v-model="form.account" :placeholder="t('login.usernamePlaceholder')" size="large" :prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item :label="t('login.password')">
            <el-input
              v-model="form.password"
              type="password"
              :placeholder="t('login.passwordPlaceholder')"
              size="large"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <div class="form-options">
            <el-checkbox v-model="remember">{{ t('login.remember') }}</el-checkbox>
            <span>{{ t('login.secure') }}</span>
          </div>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            {{ loading ? t('login.loggingIn') : t('login.enter') }}
            <el-icon class="button-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>
        <div class="login-footer"><el-icon><Lock /></el-icon>{{ t('login.encrypted') }}</div>
      </div>
      <span class="copyright">© {{ new Date().getFullYear() }} DataPilot. All rights reserved.</span>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()
const rememberedAccount = localStorage.getItem('dp-remember-account') || 'admin'
const form = reactive({ account: rememberedAccount, password: 'admin' })
const remember = ref(Boolean(localStorage.getItem('dp-remember-account')))
const loading = ref(false)

async function handleLogin() {
  if (!form.account || !form.password) {
    ElMessage.warning(t('login.required'))
    return
  }
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    if (remember.value) localStorage.setItem('dp-remember-account', form.account)
    else localStorage.removeItem('dp-remember-account')
    ElMessage.success(t('login.welcomeBack'))
    router.push('/dashboard')
  } catch {
    // 统一请求拦截器负责展示错误。
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(520px, 1.12fr) minmax(420px, 0.88fr);
  min-height: 100%;
  background: #f7f8fc;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
  padding: 38px 54px;
  color: #fff;
  background:
    radial-gradient(circle at 78% 24%, rgba(132, 153, 255, 0.32), transparent 28%),
    radial-gradient(circle at 8% 92%, rgba(124, 88, 232, 0.28), transparent 34%),
    linear-gradient(145deg, #111a3a 0%, #263c93 50%, #4f6df5 100%);
}

.brand-noise {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.12) 0.7px, transparent 0.7px);
  background-size: 19px 19px;
  mask-image: linear-gradient(130deg, #000, transparent 70%);
}

.brand-head {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  grid-template-columns: repeat(2, 6px);
  flex: 0 0 40px;
  gap: 3px;
  width: 40px;
  height: 40px;
  padding: 11px;
  background: linear-gradient(145deg, #7f98ff, #5b73f7 55%, #8b5cf6);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 11px;
  box-shadow: 0 10px 28px rgba(2, 9, 36, 0.24);
}

.brand-mark i {
  width: 6px;
  height: 6px;
  background: #fff;
  border-radius: 2px;
}

.brand-mark i:nth-child(2),
.brand-mark i:nth-child(3) { opacity: 0.56; }

.brand-head > span:last-child {
  display: flex;
  flex-direction: column;
}

.brand-head strong { font-size: 17px; letter-spacing: 0.3px; }
.brand-head small { margin-top: 3px; color: rgba(255, 255, 255, 0.46); font-size: 9px; letter-spacing: 3px; }

.brand-message {
  position: relative;
  z-index: 2;
  max-width: 630px;
  margin: auto 0;
}

.message-label {
  display: inline-flex;
  padding: 6px 10px;
  color: #b7c4ff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 999px;
  font-size: 9px;
  font-weight: 650;
  letter-spacing: 1.7px;
}

.brand-message h1 {
  margin: 24px 0 18px;
  font-size: clamp(36px, 4.3vw, 58px);
  font-weight: 720;
  line-height: 1.25;
  letter-spacing: -1.4px;
}

.brand-message h1 em {
  color: #9ff3d2;
  font-style: normal;
}

.brand-message p {
  max-width: 560px;
  margin: 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 14px;
  line-height: 1.85;
}

.feature-row {
  display: flex;
  gap: 10px;
  margin-top: 30px;
}

.feature-row span {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  color: rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  font-size: 10px;
}

.brand-foot {
  position: relative;
  z-index: 2;
  color: rgba(255, 255, 255, 0.35);
  font-size: 9px;
  letter-spacing: 0.7px;
}

.data-visual {
  position: absolute;
  right: -90px;
  bottom: -70px;
  width: 430px;
  height: 330px;
  opacity: 0.72;
}

.data-visual::before,
.data-visual::after {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  content: '';
}

.data-visual::before { inset: 10px; }
.data-visual::after { inset: 66px; }

.data-node {
  position: absolute;
  z-index: 2;
  display: grid;
  width: 48px;
  height: 48px;
  color: #5068d7;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 14px;
  box-shadow: 0 13px 28px rgba(5, 13, 52, 0.28);
  place-items: center;
}

.node-a { top: 78px; left: 26px; }
.node-b { top: 16px; left: 178px; color: #815adb; }
.node-c { top: 135px; left: 282px; color: #10a9b8; }

.data-line {
  position: absolute;
  height: 1px;
  background: rgba(255, 255, 255, 0.3);
  transform-origin: left;
}

.line-a { top: 101px; left: 70px; width: 126px; transform: rotate(-20deg); }
.line-b { top: 62px; left: 215px; width: 113px; transform: rotate(47deg); }
.line-c { top: 122px; left: 67px; width: 235px; transform: rotate(12deg); opacity: 0.35; }

.login-main {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px;
  background:
    radial-gradient(circle at 90% 10%, rgba(79, 109, 245, 0.08), transparent 28%),
    #f7f8fc;
}

.login-card {
  width: min(400px, 100%);
  padding: 40px 38px 32px;
  background: #fff;
  border: 1px solid #e9ecf3;
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(33, 44, 80, 0.09);
}

.login-header { margin-bottom: 28px; }
.welcome-pill { display: flex; align-items: center; gap: 7px; color: #6879d7; font-size: 9px; font-weight: 680; letter-spacing: 1.4px; }
.welcome-pill i { width: 16px; height: 2px; background: #4f6df5; }
.login-header h2 { margin: 13px 0 7px; color: #1d2638; font-size: 24px; font-weight: 720; }
.login-header p { margin: 0; color: #929baa; font-size: 12px; }

.login-card :deep(.el-form-item) { margin-bottom: 18px; }
.login-card :deep(.el-form-item__label) { padding-bottom: 7px; color: #525d70; font-size: 11px; font-weight: 580; }
.login-card :deep(.el-input__wrapper) { min-height: 43px; background: #fafbfc; border-radius: 8px; box-shadow: 0 0 0 1px #e4e8f0 inset; }
.login-card :deep(.el-input__wrapper.is-focus) { background: #fff; box-shadow: 0 0 0 1px #4f6df5 inset, 0 0 0 3px rgba(79, 109, 245, 0.08); }

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -1px 0 20px;
  color: #9aa3b2;
  font-size: 10px;
}

.form-options :deep(.el-checkbox__label) { color: #727c8e; font-size: 10px; }

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  box-shadow: 0 9px 20px rgba(79, 109, 245, 0.25);
}

.button-arrow { margin-left: 8px; }
.login-footer { display: flex; align-items: center; justify-content: center; gap: 5px; margin-top: 24px; color: #a2aab8; font-size: 9px; }
.mobile-brand { display: none; }
.copyright { position: absolute; bottom: 24px; color: #a5adba; font-size: 9px; }

@media (max-width: 960px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
  .login-main { padding: 28px 20px; }
  .mobile-brand { position: absolute; top: 25px; left: 25px; display: flex; align-items: center; gap: 10px; color: #1f2940; }
  .mobile-brand .brand-mark { flex-basis: 34px; width: 34px; height: 34px; padding: 8px; }
  .login-card { padding: 36px 28px 30px; }
}

@media (max-width: 480px) {
  .login-main { justify-content: flex-start; padding-top: 112px; }
  .login-card { box-shadow: 0 14px 42px rgba(33, 44, 80, 0.08); }
}
</style>
