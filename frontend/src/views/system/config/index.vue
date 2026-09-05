<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAdd">新增配置</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="240" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配置' : '新增配置'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="配置键" required>
          <el-input v-model="form.configKey" placeholder="如 sync.datax.home" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="配置值">
          <el-input v-model="form.configValue" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listConfig, addConfig, updateConfig, deleteConfig, type SystemConfigItem } from '../../../api/config'

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
    ElMessage.warning('请填写配置键')
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
      ElMessage.success('已更新')
    } else {
      await addConfig({
        configKey: form.configKey,
        configValue: form.configValue,
        description: form.description
      })
      ElMessage.success('已新增')
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
  await ElMessageBox.confirm(`确认删除配置「${row.configKey}」？`, '提示', { type: 'warning' })
  try {
    await deleteConfig(row.id)
    ElMessage.success('已删除')
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
