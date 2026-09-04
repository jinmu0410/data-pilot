<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="角色名">
            <el-input v-model="query.name" placeholder="角色名" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" value="ENABLE" />
              <el-option label="禁用" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">新增角色</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="角色名" min-width="150" />
        <el-table-column prop="code" label="编码" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openAssign(row)">分配权限</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名" required>
          <el-input v-model="form.name" placeholder="角色名称" />
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

    <el-dialog v-model="assignVisible" :title="`分配权限 - ${assignRoleName}`" width="520px">
      <div v-loading="assignLoading" class="assign-body">
        <el-checkbox-group v-model="checkedPermissions">
          <el-checkbox v-for="p in allPermissions" :key="p.id" :value="p.id" class="assign-item">
            <span>{{ p.name }}</span>
            <span class="perm-code">{{ p.code }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  listRole,
  addRole,
  updateRole,
  deleteRole,
  listRolePermission,
  upsertRolePermission,
  type RoleItem
} from '../../../api/role'
import { listPermission, type PermissionItem } from '../../../api/permission'

const loading = ref(false)
const list = ref<RoleItem[]>([])
const query = reactive({ name: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(0)
const form = reactive({ name: '', status: 'ENABLE' })

const assignVisible = ref(false)
const assignLoading = ref(false)
const assignRoleId = ref(0)
const assignRoleName = ref('')
const allPermissions = ref<PermissionItem[]>([])
const checkedPermissions = ref<number[]>([])

async function load() {
  loading.value = true
  try {
    const res = await listRole(
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
  Object.assign(form, { name: '', status: 'ENABLE' })
  dialogVisible.value = true
}

function openEdit(row: RoleItem) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, status: row.status })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name) {
    ElMessage.warning('请填写角色名')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateRole({ id: editingId.value, name: form.name, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await addRole({ name: form.name, status: form.status })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function openAssign(row: RoleItem) {
  assignRoleId.value = row.id
  assignRoleName.value = row.name
  assignVisible.value = true
  assignLoading.value = true
  try {
    const [all, owned] = await Promise.all([
      listPermission({}, 1, 500),
      listRolePermission(row.id)
    ])
    allPermissions.value = all.records
    checkedPermissions.value = owned.map((p) => p.id)
  } finally {
    assignLoading.value = false
  }
}

async function handleAssign() {
  submitting.value = true
  try {
    await upsertRolePermission(assignRoleId.value, checkedPermissions.value)
    ElMessage.success('权限已更新')
    assignVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
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

.assign-body {
  max-height: 420px;
  overflow-y: auto;
}

.assign-item {
  display: flex;
  align-items: center;
  width: 100%;
  margin-right: 0;
  padding: 4px 0;
}

.perm-code {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
