<template>
  <div ref="container" class="sql-editor" :style="{ height }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/editor/editor.worker.js?worker'
import { format as formatSql, type SqlLanguage } from 'sql-formatter'

;(self as unknown as { MonacoEnvironment: unknown }).MonacoEnvironment = {
  getWorker() {
    return new editorWorker()
  }
}

const props = defineProps<{
  modelValue: string
  height?: string
  dialect?: SqlLanguage
  language?: 'sql' | 'python' | 'shell'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const container = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  editor = monaco.editor.create(container.value!, {
    value: props.modelValue,
    language: props.language ?? 'sql',
    theme: 'vs-dark',
    minimap: { enabled: false },
    automaticLayout: true,
    fontSize: 13,
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    tabSize: 2
  })
  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor!.getValue())
  })
})

watch(
  () => props.modelValue,
  (val) => {
    if (editor && val !== editor.getValue()) {
      editor.setValue(val)
    }
  }
)

watch(
  () => props.language,
  (language) => {
    const model = editor?.getModel()
    if (model) monaco.editor.setModelLanguage(model, language ?? 'sql')
  }
)

function format(): void {
  if (!editor) return
  const sql = editor.getValue()
  if (!sql.trim()) return
  try {
    // 保护 ${param} 占位符，避免被 SQL 格式化器解析报错
    const placeholders = new Map<string, string>()
    let idx = 0
    const protectedSql = sql.replace(/\$\{([a-zA-Z0-9_]+)\}/g, (m) => {
      const token = `__DP_PARAM_${idx++}__`
      placeholders.set(token, m)
      return token
    })
    let formatted = formatSql(protectedSql, {
      language: props.dialect ?? 'mysql',
      keywordCase: 'upper',
      tabWidth: 2
    })
    for (const [token, original] of placeholders) {
      formatted = formatted.split(token).join(original)
    }
    editor.setValue(formatted)
    emit('update:modelValue', formatted)
  } catch {
    /* 格式化失败时保持原样 */
  }
}

defineExpose({ format })

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})
</script>

<style scoped>
.sql-editor {
  width: 100%;
  height: 100%;
}
</style>
