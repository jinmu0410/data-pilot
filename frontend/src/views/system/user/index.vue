<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('system.username')">
            <el-input v-model="query.username" :placeholder="t('system.username')" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item :label="t('system.status')">
            <el-select v-model="query.status" :placeholder="t('system.all')" clearable style="width: 120px">
              <el-option :label="t('system.enable')" value="ENABLE" />
              <el-option :label="t('system.disable')" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">{{ t('system.addUser') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" :label="t('system.username')" min-width="130" />
        <el-table-column prop="email" :label="t('system.email')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" :label="t('system.phone')" width="130" />
        <el-table-column :label="t('system.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? t('system.enable') : t('system.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('system.online')" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.online" type="success" size="small">{{ t('system.online') }}</el-tag>
            <el-tag v-else type="info" size="small">{{ t('system.offline') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('system.createTime')" width="170" />
        <el-table-column :label="t('system.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="warning" @click="openResetPwd(row)">{{ t('system.resetPassword') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? t('system.editUser') : t('system.addUser')" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="t('system.username')" required>
          <el-input v-model="form.username" :disabled="!!editingId" :placeholder="t('system.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="!editingId" :label="t('system.password')" required>
          <el-input v-model="form.password" type="password" show-password :placeholder="t('system.passwordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.email')" required>
          <el-input v-model="form.email" :placeholder="t('system.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.phone')">
          <el-input v-model="form.phone" :placeholder="t('system.phone')" />
        </el-form-item>
        <el-form-item :label="t('system.gender')">
          <el-select v-model="form.gender" clearable :placeholder="t('system.pleaseSelect')" style="width: 100%">
            <el-option :label="t('system.male')" value="男" />
            <el-option :label="t('system.female')" value="女" />
          </el-select>
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

    <el-dialog v-model="pwdVisible" :title="t('system.resetPassword')" width="420px">
      <el-form label-width="80px">
        <el-form-item :label="t('system.newPassword')" required>
          <el-input v-model="newPassword" type="password" show-password :placeholder="t('system.passwordPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPwd">{{ t('common.confirm') }}</el-button>
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
  listUser,
  addUser,
  updateUser,
  deleteUser,
  resetPassword,
  type UserItem,
  type UserUpdateRequest
} from '../../../api/user'

const { t } = useI18n()
const loading = ref(false)
const list = ref<UserItem[]>([])
const query = reactive({ username: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(0)
const form = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  gender: '',
  status: 'ENABLE'
})

const pwdVisible = ref(false)
const pwdUserId = ref(0)
const newPassword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listUser(
      { username: query.username || undefined, status: query.status || undefined },
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
  Object.assign(form, { username: '', password: '', email: '', phone: '', gender: '', status: 'ENABLE' })
  dialogVisible.value = true
}

function openEdit(row: UserItem) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    email: row.email,
    phone: row.phone,
    gender: '',
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.username) {
    ElMessage.warning(t('system.usernameRequired'))
    return
  }
  if (!editingId.value && !form.password) {
    ElMessage.warning(t('system.passwordRequired'))
    return
  }
  if (!form.email) {
    ElMessage.warning(t('system.emailRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      const data: UserUpdateRequest = {
        id: editingId.value,
        email: form.email,
        phone: form.phone,
        status: form.status
      }
      if (form.gender) data.gender = form.gender
      await updateUser(data)
      ElMessage.success(t('system.updateSuccess'))
    } else {
      await addUser({
        username: form.username,
        password: form.password,
        email: form.email,
        phone: form.phone || undefined,
        status: form.status
      })
      ElMessage.success(t('system.addSuccess'))
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openResetPwd(row: UserItem) {
  pwdUserId.value = row.id
  newPassword.value = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!newPassword.value || newPassword.value.length < 3) {
    ElMessage.warning(t('system.passwordTooShort'))
    return
  }
  submitting.value = true
  try {
    await resetPassword(pwdUserId.value, newPassword.value)
    ElMessage.success(t('system.passwordReset'))
    pwdVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(t('system.deleteUserConfirm', { username: row.username }), t('system.prompt'), { type: 'warning' })
  await deleteUser(row.id)
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
