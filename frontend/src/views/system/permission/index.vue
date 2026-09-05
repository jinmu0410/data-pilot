<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('system.name')">
            <el-input v-model="query.name" :placeholder="t('system.permissionName')" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item :label="t('system.status')">
            <el-select v-model="query.status" :placeholder="t('system.all')" clearable style="width: 120px">
              <el-option :label="t('system.enable')" value="ENABLE" />
              <el-option :label="t('system.disable')" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">{{ t('system.addPermission') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('system.name')" min-width="150" />
        <el-table-column prop="code" :label="t('system.permissionCode')" min-width="200" />
        <el-table-column :label="t('system.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? t('system.enable') : t('system.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('system.createTime')" width="170" />
        <el-table-column :label="t('system.actions')" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="page.total"
        :page-sizes="[10, 20, 50]"
        @change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? t('system.editPermission') : t('system.addPermission')" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="t('system.name')" required>
          <el-input v-model="form.name" :placeholder="t('system.permissionName')" />
        </el-form-item>
        <el-form-item :label="t('system.permissionCode')" required>
          <el-input v-model="form.code" :placeholder="t('system.permissionCodePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLE">{{ t('system.enable') }}</el-radio>
            <el-radio value="DISABLE">{{ t('system.disable') }}</el-radio>
          </el-radio-group>
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
import { Search, Plus } from '@element-plus/icons-vue'
import {
  listPermission,
  addPermission,
  updatePermission,
  deletePermission,
  type PermissionItem
} from '../../../api/permission'

const { t } = useI18n()
const loading = ref(false)
const list = ref<PermissionItem[]>([])
const query = reactive({ name: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(0)
const form = reactive({ name: '', code: '', status: 'ENABLE' })

async function load() {
  loading.value = true
  try {
    const res = await listPermission(
      { name: query.name || undefined, status: query.status || undefined },
      page.current,
      page.size
    )
    list.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  load()
}

function openAdd() {
  editingId.value = 0
  Object.assign(form, { name: '', code: '', status: 'ENABLE' })
  dialogVisible.value = true
}

function openEdit(row: PermissionItem) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, code: row.code, status: row.status })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name || !form.code) {
    ElMessage.warning(t('system.nameAndCodeRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updatePermission({ id: editingId.value, name: form.name, code: form.code, status: form.status })
      ElMessage.success(t('system.updateSuccess'))
    } else {
      await addPermission({ name: form.name, code: form.code, status: form.status })
      ElMessage.success(t('system.addSuccess'))
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PermissionItem) {
  await ElMessageBox.confirm(t('system.deletePermissionConfirm', { name: row.name }), t('system.prompt'), { type: 'warning' })
  await deletePermission(row.id)
  ElMessage.success(t('system.deleteSuccess'))
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
