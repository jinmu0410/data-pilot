import { createI18n } from 'vue-i18n'
import { computed } from 'vue'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'
import elementZhCn from 'element-plus/es/locale/lang/zh-cn'
import elementEn from 'element-plus/es/locale/lang/en'

export type Locale = 'zh-CN' | 'en-US'

const LOCALE_KEY = 'dp-locale'

export function getStoredLocale(): Locale {
  return localStorage.getItem(LOCALE_KEY) === 'en-US' ? 'en-US' : 'zh-CN'
}

export const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_KEY, locale)
}

// Element Plus 组件内部文案（分页、弹窗按钮、日期选择等）
export const elementLocale = computed(() =>
  i18n.global.locale.value === 'en-US' ? elementEn : elementZhCn
)
