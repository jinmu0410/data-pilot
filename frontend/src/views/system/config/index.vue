<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAdd">{{ t('system.addConfig') }}</el-button>
        <el-button :icon="Refresh" @click="load">{{ t('system.refresh') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configKey" :label="t('system.configKey')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" :label="t('system.configValue')" min-width="240" show-overflow-tooltip />
        <el-table-column prop="description" :label="t('system.description')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" :label="t('system.updateTime')" width="170" />
        <el-table-column :label="t('system.actions')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('system.editConfig') : t('system.addConfig')" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="t('system.configKey')" required>
          <el-input v-model="form.configKey" :placeholder="t('system.configKeyPlaceholder')" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item :label="t('system.configValue')">
          <el-input v-model="form.configValue" :placeholder="t('system.configValue')" />
        </el-form-item>
        <el-form-item :label="t('system.description')">
          <el-input v-model="form.description" :placeholder="t('system.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listConfig, addConfig, updateConfig, deleteConfig, type SystemConfigItem } from '../../../api/config'

const { t } = useI18n()
const loading = ref(false)
const list = ref<SystemConfigItem[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({ id: 0, configKey: '', configValue: '', description: '' })

async function load() {
  loading.value = true
  try {
    list.value = await listConfig()
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.id = 0
  form.configKey = ''
  form.configValue = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(row: SystemConfigItem) {
  form.id = row.id
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.configKey.trim()) {
    ElMessage.warning(t('system.configKeyRequired'))
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateConfig({
        id: form.id,
        configKey: form.configKey,
        configValue: form.configValue,
        description: form.description
      })
      ElMessage.success(t('system.updated'))
    } else {
      await addConfig({
        configKey: form.configKey,
        configValue: form.configValue,
        description: form.description
      })
      ElMessage.success(t('system.added'))
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示已在拦截器处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SystemConfigItem) {
  await ElMessageBox.confirm(t('system.deleteConfigConfirm', { key: row.configKey }), t('system.prompt'), { type: 'warning' })
  try {
    await deleteConfig(row.id)
    ElMessage.success(t('system.deleted'))
    await load()
  } catch {
    // 错误提示已在拦截器处理
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
