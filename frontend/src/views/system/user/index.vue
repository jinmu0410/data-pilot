<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="query.username" placeholder="用户名" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" value="ENABLE" />
              <el-option label="禁用" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">新增用户</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.online" type="success" size="small">在线</el-tag>
            <el-tag v-else type="info" size="small">离线</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openResetPwd(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!editingId" placeholder="4-32位字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少3位" />
        </el-form-item>
        <el-form-item label="邮箱" required>
          <el-input v-model="form.email" placeholder="user@example.com" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" clearable placeholder="请选择" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLE">启用</el-radio>
            <el-radio value="DISABLE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" show-password placeholder="至少3位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
    ElMessage.warning('请填写用户名')
    return
  }
  if (!editingId.value && !form.password) {
    ElMessage.warning('请填写密码')
    return
  }
  if (!form.email) {
    ElMessage.warning('请填写邮箱')
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
      ElMessage.success('更新成功')
    } else {
      await addUser({
        username: form.username,
        password: form.password,
        email: form.email,
        phone: form.phone || undefined,
        status: form.status
      })
      ElMessage.success('新增成功')
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
    ElMessage.warning('新密码至少3位')
    return
  }
  submitting.value = true
  try {
    await resetPassword(pwdUserId.value, newPassword.value)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
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
