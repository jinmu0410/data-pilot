<template>
  <el-dialog
    :model-value="modelValue"
    width="min(1120px, calc(100vw - 40px))"
    top="4vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :before-close="cancel"
    class="code-task-dialog"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="dialog-heading">
        <span class="engine-logo" :class="`is-${type.toLowerCase()}`">{{ engine.logo }}</span>
        <div>
          <div class="dialog-title">{{ engine.title }}</div>
          <div class="dialog-subtitle">{{ engine.subtitle }}</div>
        </div>
        <span class="engine-tag" :class="`is-${type.toLowerCase()}`">{{ engine.tag }}</span>
      </div>
    </template>

    <div class="wizard-shell">
      <aside class="wizard-nav">
        <button
          v-for="(item, index) in steps"
          :key="item.title"
          type="button"
          class="step-button"
          :class="{ active: step === index, done: step > index }"
          @click="goToStep(index)"
        >
          <span class="step-index">{{ step > index ? '✓' : index + 1 }}</span>
          <span><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
        </button>

        <div class="task-summary">
          <span class="summary-label">{{ t('task.overview') }}</span>
          <strong>{{ config.name || t('task.unnamed') }}</strong>
          <div class="summary-engine">
            <span class="summary-logo" :class="`is-${type.toLowerCase()}`">{{ engine.logo }}</span>
            <div><b>{{ engine.runtime }}</b><small>{{ executionMode }}</small></div>
          </div>
          <div class="summary-meta">
            <span v-if="isSql">{{ datasource?.name || t('task.datasourcePending') }}</span>
            <span>{{ t('task.codeLines', { n: codeLines }) }}</span>
            <span>{{ t('task.timeoutSec', { n: config.timeout || 30 }) }}</span>
          </div>
        </div>
      </aside>

      <main class="wizard-content">
        <section v-show="step === 0" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">BASIC</span><h3>{{ t('task.defineTask') }}</h3></div>
            <p>{{ t('task.defineTaskDesc') }}</p>
          </div>

          <div class="form-card">
            <label class="field-label">{{ t('task.name') }} <em>*</em></label>
            <el-input v-model="config.name" maxlength="80" show-word-limit :placeholder="namePlaceholder" size="large" />
            <label class="field-label field-gap">{{ t('task.description') }}</label>
            <el-input
              v-model="config.description"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              :placeholder="t('task.descriptionPlaceholder')"
            />
          </div>

          <div class="type-intro">
            <span class="intro-icon" :class="`is-${type.toLowerCase()}`">{{ engine.logo }}</span>
            <div><strong>{{ engine.runtime }}</strong><p>{{ engine.introduction }}</p></div>
            <span class="intro-badge">{{ engine.tag }}</span>
          </div>
        </section>

        <section v-show="step === 1" class="step-panel">
          <template v-if="isSql">
            <div class="section-heading">
              <div><span class="eyebrow">CONNECTION</span><h3>{{ t('task.selectDatasource') }}</h3></div>
              <p>{{ t('task.sqlConnDesc') }}</p>
            </div>

            <div class="connection-grid">
              <article class="connection-card">
                <div class="card-heading"><span class="card-number">01</span><div><strong>{{ t('task.execDatasource') }}</strong><small>Connection</small></div></div>
                <label class="field-label">{{ t('task.dataSource') }} <em>*</em></label>
                <el-select v-model="config.datasourceCode" :placeholder="t('task.selectDatasourcePlaceholder')" filterable size="large">
                  <el-option v-for="item in datasources" :key="item.code" :label="`${item.name} · ${item.type}`" :value="item.code" />
                </el-select>
                <div class="connection-status" :class="{ ready: datasource }">
                  <i></i>
                  <span>{{ datasource ? t('task.datasourceReady', { name: datasource.name, type: datasource.type }) : t('task.datasourceContinue') }}</span>
                </div>
              </article>

              <article class="connection-card">
                <div class="card-heading"><span class="card-number purple">02</span><div><strong>{{ t('task.statementType') }}</strong><small>Statement mode</small></div></div>
                <div class="mode-list">
                  <button type="button" class="mode-card" :class="{ selected: config.sqlType === 'QUERY' }" @click="config.sqlType = 'QUERY'">
                    <span class="mode-symbol">Q</span><span><strong>{{ t('task.queryStatement') }}</strong><small>{{ t('task.queryStatementDesc') }}</small></span><i>✓</i>
                  </button>
                  <button type="button" class="mode-card" :class="{ selected: config.sqlType === 'NON_QUERY' }" @click="config.sqlType = 'NON_QUERY'">
                    <span class="mode-symbol action">DML</span><span><strong>{{ t('task.nonQueryStatement') }}</strong><small>{{ t('task.nonQueryStatementDesc') }}</small></span><i>✓</i>
                  </button>
                </div>
              </article>
            </div>
          </template>

          <template v-else>
            <div class="section-heading code-heading">
              <div><span class="eyebrow">SCRIPT</span><h3>{{ t('task.writeScript', { name: engine.shortName }) }}</h3></div>
              <div class="code-stats"><span>{{ t('task.lines', { n: codeLines }) }}</span><span>{{ t('task.chars', { n: codeCharacters }) }}</span></div>
            </div>
            <div class="editor-card">
              <div class="editor-toolbar">
                <div><i class="window-dot red"></i><i class="window-dot yellow"></i><i class="window-dot green"></i></div>
                <span>{{ scriptFilename }}</span>
                <em>{{ engine.runtime }}</em>
              </div>
              <SqlEditor v-model="config.script" :language="editorLanguage" height="410px" />
            </div>
            <div class="code-tip"><strong>{{ t('task.writeTip') }}</strong><span>{{ engine.codeTip }}</span></div>
          </template>
        </section>

        <section v-show="step === 2" class="step-panel">
          <template v-if="isSql">
            <div class="section-heading code-heading">
              <div><span class="eyebrow">STATEMENT</span><h3>{{ t('task.writeSql') }}</h3></div>
              <div class="code-actions"><span>{{ t('task.lines', { n: codeLines }) }}</span><el-button size="small" @click="formatSql">{{ t('task.formatSql') }}</el-button></div>
            </div>
            <div class="editor-card sql-main-editor">
              <div class="editor-toolbar">
                <div><i class="window-dot red"></i><i class="window-dot yellow"></i><i class="window-dot green"></i></div>
                <span>task.sql</span>
                <em>{{ config.sqlType === 'NON_QUERY' ? 'NON QUERY' : 'QUERY' }}</em>
              </div>
              <SqlEditor ref="sqlEditorRef" v-model="config.sqlText" language="sql" height="330px" />
            </div>
            <div class="parameter-card">
              <div class="block-title"><span>{{ t('task.sqlParams') }}</span><small>{{ t('task.sqlParamsDesc') }}</small></div>
              <el-input v-model="config.sqlParams" :placeholder="t('task.sqlParamsPlaceholder')" size="large" clearable />
              <div class="parameter-preview">
                <span v-if="!sqlParameters.length">{{ t('task.noParams') }}</span>
                <el-tag v-for="item in sqlParameters" :key="item" size="small" effect="plain">{{ item }}</el-tag>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="section-heading">
              <div><span class="eyebrow">RUNTIME</span><h3>{{ t('task.runtimeStrategy') }}</h3></div>
              <p>{{ t('task.runtimeStrategyDesc') }}</p>
            </div>
            <div class="runtime-grid">
              <article class="settings-card">
                <div class="block-title"><span>{{ t('task.runtimeGuard') }}</span><small>Runtime guard</small></div>
                <label class="field-label">{{ t('task.timeoutSeconds') }} <em>*</em></label>
                <el-input-number v-model="config.timeout" :min="1" :max="86400" :controls="false" class="full-number" />
                <div class="range-hints"><button type="button" @click="config.timeout = 30">{{ t('task.sec30') }}</button><button type="button" @click="config.timeout = 300">{{ t('task.min5') }}</button><button type="button" @click="config.timeout = 1800">{{ t('task.min30') }}</button></div>
              </article>
              <article class="settings-card command-card">
                <div class="block-title"><span>{{ t('task.execMode') }}</span><small>Command preview</small></div>
                <div class="command-preview"><span>$</span><code>{{ commandPreview }}</code></div>
                <p>{{ t('task.execModeDesc') }}</p>
              </article>
            </div>
            <div class="safety-card"><span>✓</span><div><strong>{{ t('task.isolation') }}</strong><p>{{ t('task.isolationDesc') }}</p></div></div>
          </template>
        </section>

        <section v-show="step === 3" class="step-panel">
          <template v-if="isSql">
            <div class="section-heading">
              <div><span class="eyebrow">RUNTIME</span><h3>{{ t('task.runtimeExt') }}</h3></div>
              <p>{{ t('task.runtimeExtDesc') }}</p>
            </div>
            <div class="runtime-grid sql-runtime-grid">
              <article class="settings-card">
                <div class="block-title"><span>{{ t('task.runtimeGuard') }}</span><small>Runtime guard</small></div>
                <label class="field-label">{{ t('task.timeoutSeconds') }} <em>*</em></label>
                <el-input-number v-model="config.timeout" :min="1" :max="86400" :controls="false" class="full-number" />
                <div class="performance-tip">{{ t('task.performanceTip') }}</div>
              </article>
              <article class="settings-card review-card">
                <div class="block-title"><span>{{ t('task.configSummary') }}</span><small>Ready check</small></div>
                <div class="review-line"><span>{{ t('task.dataSource') }}</span><strong>{{ datasource?.name || t('task.notSelected') }}</strong></div>
                <div class="review-line"><span>{{ t('task.statementType') }}</span><strong>{{ executionMode }}</strong></div>
                <div class="review-line"><span>{{ t('task.sqlLines') }}</span><strong>{{ t('task.lines', { n: codeLines }) }}</strong></div>
                <div class="review-line"><span>{{ t('task.paramCount') }}</span><strong>{{ t('task.countUnit', { n: sqlParameters.length }) }}</strong></div>
              </article>
            </div>
            <el-collapse class="sql-collapse">
              <el-collapse-item name="pre"><template #title><strong>{{ t('task.preSql') }}</strong><span>{{ t('task.preSqlDesc') }}</span></template><SqlEditor v-model="config.preSql" language="sql" height="145px" /></el-collapse-item>
              <el-collapse-item name="post"><template #title><strong>{{ t('task.postSql') }}</strong><span>{{ t('task.postSqlDesc') }}</span></template><SqlEditor v-model="config.postSql" language="sql" height="145px" /></el-collapse-item>
            </el-collapse>
          </template>

          <template v-else>
            <div class="section-heading">
              <div><span class="eyebrow">REVIEW</span><h3>{{ t('task.reviewConfig') }}</h3></div>
              <p>{{ t('task.reviewConfigDesc') }}</p>
            </div>
            <div class="review-layout">
              <article class="review-overview">
                <span class="review-logo" :class="`is-${type.toLowerCase()}`">{{ engine.logo }}</span>
                <div><small>{{ engine.tag }}</small><strong>{{ config.name || t('task.unnamed') }}</strong><p>{{ config.description || t('task.noDescription') }}</p></div>
              </article>
              <div class="review-metrics">
                <div><strong>{{ codeLines }}</strong><span>{{ t('task.scriptLines') }}</span></div>
                <div><strong>{{ codeCharacters }}</strong><span>{{ t('task.charCount') }}</span></div>
                <div><strong>{{ config.timeout || 30 }}s</strong><span>{{ t('task.execTimeout') }}</span></div>
              </div>
              <div class="script-preview"><div><span>{{ scriptFilename }}</span><em>{{ t('task.readonlyPreview') }}</em></div><pre>{{ scriptPreview }}</pre></div>
            </div>
          </template>
        </section>
      </main>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="danger" link @click="emit('delete')">{{ t('task.deleteNode') }}</el-button>
        <span class="footer-spacer"></span>
        <el-button @click="cancelWithoutDone">{{ t('common.cancel') }}</el-button>
        <el-button v-if="step > 0" @click="step--">{{ t('task.prev') }}</el-button>
        <el-button v-if="step < steps.length - 1" type="primary" @click="next">{{ t('task.next') }}</el-button>
        <el-button v-else type="primary" @click="apply">{{ t('task.apply') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SqlEditor from '../../components/SqlEditor.vue'
import type { NodeConfig } from './nodes'

interface DataSourceOption {
  id: number
  code: string
  name: string
  type: string
}

const props = defineProps<{
  modelValue: boolean
  type: string
  config: NodeConfig
  datasources: DataSourceOption[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'delete'): void
  (event: 'closed'): void
}>()

const { t } = useI18n()
const step = ref(0)
const snapshot = ref('')
const sqlEditorRef = ref<{ format: () => void }>()
const isSql = computed(() => props.type === 'SQL')
const editorLanguage = computed(() => props.type === 'PYTHON' ? 'python' : 'shell')
const steps = computed(() => isSql.value
  ? [
      { title: t('task.stepBasic'), description: t('task.stepBasicDesc') },
      { title: t('task.stepConn'), description: t('task.stepConnDesc') },
      { title: t('task.stepSql'), description: t('task.stepSqlDesc') },
      { title: t('task.stepRuntime'), description: t('task.stepRuntimeDesc') }
    ]
  : [
      { title: t('task.stepBasic'), description: t('task.stepBasicDesc') },
      { title: t('task.stepScript'), description: props.type === 'PYTHON' ? t('task.stepScriptDescPython') : t('task.stepScriptDescShell') },
      { title: t('task.stepRuntime'), description: t('task.stepRuntimeScriptDesc') },
      { title: t('task.stepReview'), description: t('task.stepReviewDesc') }
    ])

const engine = computed(() => {
  if (props.type === 'SQL') return {
    logo: 'SQL', title: t('task.engineSqlTitle'), shortName: 'SQL', tag: t('task.engineSqlTag'), runtime: t('task.engineSqlRuntime'),
    subtitle: t('task.engineSqlSubtitle'), introduction: t('task.engineSqlIntro'),
    codeTip: ''
  }
  if (props.type === 'PYTHON') return {
    logo: 'PY', title: t('task.enginePythonTitle'), shortName: 'Python', tag: t('task.enginePythonTag'), runtime: t('task.enginePythonRuntime'),
    subtitle: t('task.enginePythonSubtitle'), introduction: t('task.enginePythonIntro'),
    codeTip: t('task.enginePythonCodeTip')
  }
  return {
    logo: 'SH', title: t('task.engineShellTitle'), shortName: 'Shell', tag: t('task.engineShellTag'), runtime: t('task.engineShellRuntime'),
    subtitle: t('task.engineShellSubtitle'), introduction: t('task.engineShellIntro'),
    codeTip: t('task.engineShellCodeTip')
  }
})

const datasource = computed(() => props.datasources.find((item) => item.code === props.config.datasourceCode))
const code = computed(() => isSql.value ? String(props.config.sqlText || '') : String(props.config.script || ''))
const codeLines = computed(() => code.value.trim() ? code.value.split(/\r?\n/).length : 0)
const codeCharacters = computed(() => code.value.length)
const sqlParameters = computed(() => String(props.config.sqlParams || '').split(';').map((item) => item.trim()).filter(Boolean))
const executionMode = computed(() => isSql.value
  ? (props.config.sqlType === 'NON_QUERY' ? t('task.nonQueryStatement') : t('task.queryStatement'))
  : t('task.scriptType', { name: engine.value.shortName }))
const scriptFilename = computed(() => props.type === 'PYTHON' ? 'task.py' : 'task.sh')
const commandPreview = computed(() => props.type === 'PYTHON' ? 'python3 task.py' : '/bin/sh task.sh')
const scriptPreview = computed(() => code.value.trim() || t('task.noScriptContent'))
const namePlaceholder = computed(() => {
  if (props.type === 'SQL') return t('task.namePlaceholderSql')
  if (props.type === 'PYTHON') return t('task.namePlaceholderPython')
  return t('task.namePlaceholderShell')
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    snapshot.value = JSON.stringify(props.config)
    initializeDefaults()
    step.value = 0
  },
  { immediate: true }
)

function initializeDefaults() {
  props.config.description ||= ''
  props.config.timeout ||= 30
  if (isSql.value) {
    props.config.sqlType ||= 'QUERY'
    props.config.sqlText ||= ''
    props.config.sqlParams ||= ''
    props.config.preSql ||= ''
    props.config.postSql ||= ''
  } else {
    props.config.script ||= ''
  }
}

function validate(targetStep: number, quiet = false) {
  let message = ''
  if (targetStep === 0 && !String(props.config.name || '').trim()) message = t('task.nameRequired')
  if (isSql.value) {
    if (targetStep === 1 && !props.config.datasourceCode) message = t('task.datasourceRequired')
    if (targetStep === 2 && !String(props.config.sqlText || '').trim()) message = t('task.sqlRequired')
    if (targetStep === 3 && (!props.config.timeout || props.config.timeout < 1)) message = t('task.timeoutPositive')
  } else {
    if (targetStep === 1 && !String(props.config.script || '').trim()) message = t('task.scriptRequired', { name: engine.value.shortName })
    if (targetStep === 2 && (!props.config.timeout || props.config.timeout < 1)) message = t('task.timeoutPositive')
  }
  if (message && !quiet) ElMessage.warning(message)
  return !message
}

function goToStep(target: number) {
  if (target <= step.value) {
    step.value = target
    return
  }
  for (let index = 0; index < target; index++) {
    if (!validate(index)) {
      step.value = index
      return
    }
  }
  step.value = target
}

function next() {
  if (!validate(step.value)) return
  step.value++
}

function formatSql() {
  sqlEditorRef.value?.format()
}

function apply() {
  for (let index = 0; index < steps.value.length; index++) {
    if (!validate(index)) {
      step.value = index
      return
    }
  }
  props.config.name = String(props.config.name).trim()
  snapshot.value = JSON.stringify(props.config)
  emit('update:modelValue', false)
  ElMessage.success(t('task.applied', { title: engine.value.title }))
}

function restoreSnapshot() {
  const original = JSON.parse(snapshot.value || '{}')
  for (const key of Object.keys(props.config)) delete props.config[key]
  Object.assign(props.config, original)
}

function cancel(done: () => void) {
  restoreSnapshot()
  done()
}

function cancelWithoutDone() {
  restoreSnapshot()
  emit('update:modelValue', false)
}
</script>

<style scoped>
.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }
.engine-logo, .summary-logo, .intro-icon, .review-logo { display: grid; place-items: center; color: #fff; font-weight: 800; letter-spacing: -.5px; background: linear-gradient(145deg, #3b82f6, #6366f1); }
.engine-logo { width: 40px; height: 40px; border-radius: 11px; font-size: 11px; box-shadow: 0 8px 18px rgba(59, 130, 246, .24); }
.engine-logo.is-python, .summary-logo.is-python, .intro-icon.is-python, .review-logo.is-python { background: linear-gradient(145deg, #3776ab, #f0b429); }
.engine-logo.is-shell, .summary-logo.is-shell, .intro-icon.is-shell, .review-logo.is-shell { background: linear-gradient(145deg, #334155, #64748b); }
.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }
.dialog-subtitle { margin-top: 3px; color: #8a94a6; font-size: 12px; }
.engine-tag { margin-left: auto; padding: 5px 10px; color: #2563eb; border: 1px solid #dbeafe; border-radius: 999px; background: #eff6ff; font-size: 12px; }
.engine-tag.is-python { color: #a16207; border-color: #fde68a; background: #fffbeb; }
.engine-tag.is-shell { color: #475569; border-color: #e2e8f0; background: #f8fafc; }
.wizard-shell { min-height: 610px; display: grid; grid-template-columns: 205px minmax(0, 1fr); border: 1px solid #e8ebf2; border-radius: 12px; overflow: hidden; background: #f7f8fb; }
.wizard-nav { display: flex; padding: 18px 14px; flex-direction: column; border-right: 1px solid #e8ebf2; background: #fbfbfd; }
.step-button { width: 100%; display: flex; align-items: flex-start; gap: 11px; padding: 12px 10px; border: 0; border-radius: 9px; color: #7b8495; background: transparent; text-align: left; cursor: pointer; transition: .18s ease; }
.step-button:hover { background: #f2f3f8; }
.step-button.active { color: #315fd4; background: #edf3ff; }
.step-button.done { color: #374151; }
.step-index { width: 25px; height: 25px; display: grid; place-items: center; flex: 0 0 auto; border: 1px solid #d7dbe5; border-radius: 50%; background: #fff; font-size: 11px; font-weight: 700; }
.step-button.active .step-index { color: #fff; border-color: #3b82f6; background: #3b82f6; }
.step-button.done .step-index { color: #fff; border-color: #10b981; background: #10b981; }
.step-button strong, .step-button small { display: block; }
.step-button strong { margin-top: 1px; font-size: 13px; }
.step-button small { margin-top: 4px; color: #9aa2b1; font-size: 11px; }
.task-summary { margin-top: auto; padding: 13px; border: 1px solid #e6e8ef; border-radius: 10px; background: #fff; }
.summary-label { display: block; margin-bottom: 7px; color: #9aa2b1; font-size: 10px; letter-spacing: 1px; }
.task-summary > strong { display: block; overflow: hidden; color: #293247; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.summary-engine { display: flex; align-items: center; gap: 8px; margin: 11px 0; }
.summary-logo { width: 29px; height: 29px; border-radius: 8px; font-size: 8px; }
.summary-engine b, .summary-engine small { display: block; }
.summary-engine b { color: #475569; font-size: 10px; }
.summary-engine small { margin-top: 2px; color: #9aa2b1; font-size: 9px; }
.summary-meta { display: flex; flex-wrap: wrap; gap: 5px; }
.summary-meta span { max-width: 100%; overflow: hidden; padding: 3px 6px; border-radius: 4px; color: #6b7280; background: #f4f5f8; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.wizard-content { min-width: 0; padding: 28px 30px; background: #fff; }
.step-panel { animation: panel-in .2s ease; }
@keyframes panel-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.section-heading h3 { margin: 3px 0 0; color: #20283a; font-size: 20px; font-weight: 700; }
.section-heading p { max-width: 390px; margin: 12px 0 0; color: #8c95a6; font-size: 11px; line-height: 1.6; text-align: right; }
.eyebrow { color: #4f6edb; font-size: 9px; font-weight: 800; letter-spacing: 1.8px; }
.form-card, .connection-card, .settings-card, .parameter-card { padding: 19px; border: 1px solid #e6e8ef; border-radius: 10px; background: #fff; }
.field-label { display: block; margin-bottom: 7px; color: #4b5568; font-size: 11px; font-weight: 650; }
.field-label em { color: #f05252; font-style: normal; }
.field-gap { margin-top: 17px; }
.type-intro { display: flex; align-items: center; gap: 13px; margin-top: 16px; padding: 15px; border: 1px solid #e4e9f5; border-radius: 10px; background: #f8faff; }
.intro-icon { width: 39px; height: 39px; flex-shrink: 0; border-radius: 10px; font-size: 10px; }
.type-intro > div { flex: 1; }
.type-intro strong { color: #334155; font-size: 12px; }
.type-intro p { margin: 4px 0 0; color: #7d8798; font-size: 10px; line-height: 1.55; }
.intro-badge { padding: 4px 8px; border-radius: 999px; color: #64748b; background: #fff; font-size: 9px; }
.connection-grid, .runtime-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.card-heading { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.card-heading strong, .card-heading small { display: block; }
.card-heading strong { color: #293247; font-size: 13px; }
.card-heading small { margin-top: 2px; color: #a0a7b5; font-size: 10px; }
.card-number { width: 31px; height: 31px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: #3b82f6; font-size: 10px; font-weight: 700; }
.card-number.purple { background: #8b5cf6; }
.connection-card :deep(.el-select) { width: 100%; }
.connection-status { display: flex; align-items: center; gap: 7px; margin-top: 16px; padding: 10px; border-radius: 7px; color: #8b95a6; background: #f6f7f9; font-size: 10px; }
.connection-status i { width: 6px; height: 6px; border-radius: 50%; background: #b7bdc8; }
.connection-status.ready { color: #08775a; background: #edf9f5; }
.connection-status.ready i { background: #10b981; }
.mode-list { display: grid; gap: 9px; }
.mode-card { position: relative; display: flex; align-items: center; gap: 10px; min-height: 65px; padding: 10px 12px; border: 1px solid #e4e7ed; border-radius: 9px; background: #fff; text-align: left; cursor: pointer; }
.mode-card.selected { border-color: #8cabfa; background: #f5f8ff; box-shadow: 0 0 0 2px rgba(59, 130, 246, .08); }
.mode-symbol { width: 31px; height: 31px; display: grid; place-items: center; flex-shrink: 0; border-radius: 8px; color: #2563eb; background: #eaf2ff; font-size: 11px; font-weight: 800; }
.mode-symbol.action { color: #7c3aed; background: #f3e8ff; font-size: 8px; }
.mode-card span:nth-child(2) { flex: 1; }
.mode-card strong, .mode-card small { display: block; }
.mode-card strong { color: #344054; font-size: 11px; }
.mode-card small { margin-top: 3px; color: #929aaa; font-size: 9px; }
.mode-card > i { display: none; color: #3b82f6; font-style: normal; }
.mode-card.selected > i { display: block; }
.code-heading { align-items: center; }
.code-stats, .code-actions { display: flex; align-items: center; gap: 7px; color: #8791a2; font-size: 10px; }
.code-stats span, .code-actions > span { padding: 5px 8px; border-radius: 5px; background: #f3f5f8; }
.editor-card { overflow: hidden; border: 1px solid #273449; border-radius: 10px; background: #1e1e1e; box-shadow: 0 10px 24px rgba(15, 23, 42, .11); }
.editor-toolbar { height: 36px; display: flex; align-items: center; padding: 0 13px; color: #aeb6c5; border-bottom: 1px solid #343d4d; background: #202837; font-size: 10px; }
.editor-toolbar > div { display: flex; gap: 5px; }
.editor-toolbar > span { margin-left: 13px; }
.editor-toolbar > em { margin-left: auto; color: #7f8ca1; font-size: 9px; font-style: normal; }
.window-dot { width: 7px; height: 7px; display: inline-block; border-radius: 50%; }
.window-dot.red { background: #ff6b67; }.window-dot.yellow { background: #f7c94b; }.window-dot.green { background: #52c77a; }
.code-tip, .safety-card { display: flex; align-items: flex-start; gap: 10px; margin-top: 15px; padding: 12px 14px; border-radius: 8px; color: #6f7d91; background: #f5f7fb; font-size: 10px; line-height: 1.6; }
.code-tip strong { flex-shrink: 0; color: #536176; }
.parameter-card { margin-top: 16px; }
.block-title { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.block-title span { color: #30384a; font-size: 13px; font-weight: 700; }
.block-title small { color: #929aab; font-size: 10px; }
.parameter-preview { min-height: 22px; display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 9px; color: #a0a7b5; font-size: 9px; }
.settings-card { min-width: 0; }
.full-number { width: 100%; }
.range-hints { display: flex; gap: 6px; margin-top: 10px; }
.range-hints button { padding: 4px 8px; border: 1px solid #e4e7ed; border-radius: 5px; color: #6b7485; background: #fff; font-size: 9px; cursor: pointer; }
.range-hints button:hover { color: #2563eb; border-color: #93b4ff; }
.command-preview { display: flex; align-items: center; gap: 8px; padding: 13px; border-radius: 7px; color: #d8dee9; background: #202837; font-size: 11px; }
.command-preview span { color: #65d69e; }
.command-card p { margin: 13px 0 0; color: #8791a2; font-size: 10px; line-height: 1.65; }
.safety-card > span { width: 21px; height: 21px; display: grid; place-items: center; flex-shrink: 0; border-radius: 50%; color: #fff; background: #10b981; font-size: 10px; }
.safety-card strong { color: #475569; font-size: 11px; }.safety-card p { margin: 3px 0 0; }
.performance-tip { margin-top: 16px; padding: 10px 12px; border-radius: 7px; color: #64748b; background: #f5f7fb; font-size: 10px; line-height: 1.55; }
.review-line { display: flex; align-items: center; justify-content: space-between; padding: 9px 0; border-bottom: 1px solid #f0f1f4; font-size: 10px; }
.review-line:last-child { border-bottom: 0; }.review-line span { color: #929aab; }.review-line strong { color: #475569; }
.sql-collapse { margin-top: 16px; padding: 0 15px; border: 1px solid #e5e8ef; border-radius: 10px; }
.sql-collapse :deep(.el-collapse-item__header) { gap: 9px; }
.sql-collapse :deep(.el-collapse-item__header span) { color: #969ead; font-size: 10px; font-weight: 400; }
.review-layout { display: grid; gap: 14px; }
.review-overview { display: flex; align-items: center; gap: 14px; padding: 17px; border: 1px solid #e4e9f5; border-radius: 10px; background: #f8faff; }
.review-logo { width: 45px; height: 45px; flex-shrink: 0; border-radius: 12px; font-size: 11px; }
.review-overview small, .review-overview strong { display: block; }.review-overview small { color: #8491a6; font-size: 9px; }.review-overview strong { margin-top: 3px; color: #293247; font-size: 14px; }.review-overview p { margin: 4px 0 0; color: #8791a2; font-size: 10px; }
.review-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px; }
.review-metrics > div { display: flex; align-items: center; padding: 12px; border-radius: 8px; flex-direction: column; background: #f6f7fa; }.review-metrics strong { color: #334155; font-size: 16px; }.review-metrics span { margin-top: 3px; color: #929aaa; font-size: 9px; }
.script-preview { overflow: hidden; border: 1px solid #273449; border-radius: 10px; background: #1e1e1e; }
.script-preview > div { height: 34px; display: flex; align-items: center; justify-content: space-between; padding: 0 13px; color: #abb4c3; border-bottom: 1px solid #343d4d; background: #202837; font-size: 10px; }.script-preview em { color: #768397; font-size: 9px; font-style: normal; }.script-preview pre { height: 245px; box-sizing: border-box; margin: 0; overflow: auto; padding: 15px; color: #d4d4d4; font: 11px/1.65 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; white-space: pre-wrap; }
.dialog-footer { display: flex; align-items: center; width: 100%; }.footer-spacer { flex: 1; }

@media (max-width: 900px) {
  .wizard-shell { grid-template-columns: 1fr; }
  .wizard-nav { flex-direction: row; gap: 4px; overflow-x: auto; border-right: 0; border-bottom: 1px solid #e8ebf2; }
  .step-button { min-width: 145px; }
  .task-summary { display: none; }
  .wizard-content { padding: 22px 18px; }
  .dialog-heading { padding-right: 28px; }.engine-tag { display: none; }
  .section-heading { display: block; }.section-heading p { margin: 7px 0 0; text-align: left; }
  .connection-grid, .runtime-grid { grid-template-columns: 1fr; }
}
</style>

<style>
.code-task-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.code-task-dialog .el-dialog__body { padding: 16px 20px; }
.code-task-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
.code-task-dialog.el-dialog { border-radius: 14px; }
</style>
