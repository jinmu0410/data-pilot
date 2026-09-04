<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="名称">
            <el-input v-model="query.name" placeholder="工作空间名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" value="ENABLE" />
              <el-option label="禁用" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">新增工作空间</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="code" label="编码" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openMembers(row)">成员</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑工作空间' : '新增工作空间'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="工作空间名称" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="编码">
          <el-input v-model="form.code" placeholder="字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="form.secret" placeholder="访问密钥" />
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

    <el-dialog v-model="memberVisible" :title="`成员管理 - ${memberWorkspaceName}`" width="640px">
      <div class="member-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="openAddMember">添加成员</el-button>
      </div>
      <el-tabs v-model="memberTab" @tab-change="loadMembers">
        <el-tab-pane label="管理员" name="1" />
        <el-tab-pane label="普通用户" name="0" />
      </el-tabs>
      <el-table v-loading="memberLoading" :data="members" border size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="memberTab === '1'" link type="warning" @click="transfer(row, 0)">设为普通用户</el-button>
            <el-button v-else link type="primary" @click="transfer(row, 1)">设为管理员</el-button>
            <el-button link type="danger" @click="removeMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="memberPage.current"
        v-model:page-size="memberPage.size"
        class="pager"
        layout="total, prev, pager, next"
        :total="memberPage.total"
        @change="loadMembers"
      />
    </el-dialog>

    <el-dialog v-model="addMemberVisible" title="添加成员" width="520px">
      <el-input v-model="addMemberKeyword" placeholder="用户名模糊搜索" clearable style="margin-bottom: 12px" @keyup.enter="loadNotInMembers" />
      <el-table v-loading="addMemberLoading" :data="notInMembers" border size="small" max-height="360">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="bind(row)">添加</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="addMemberPage.current"
        v-model:page-size="addMemberPage.size"
        class="pager"
        layout="total, prev, pager, next"
        :total="addMemberPage.total"
        @change="loadNotInMembers"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  listWorkspace,
  addWorkspace,
  updateWorkspace,
  deleteWorkspace,
  listWorkspaceMembers,
  listNotInMembers,
  bindMember,
  deleteMember,
  permissionTransfer,
  type WorkspaceItem,
  type WorkspaceMember
} from '../../../api/workspace'

const loading = ref(false)
const list = ref<WorkspaceItem[]>([])
const query = reactive({ name: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(0)
const form = reactive({ name: '', code: '', secret: '', status: 'ENABLE' })

const memberVisible = ref(false)
const memberLoading = ref(false)
const memberWorkspaceId = ref(0)
const memberWorkspaceName = ref('')
const memberTab = ref('1')
const members = ref<WorkspaceMember[]>([])
const memberPage = reactive({ current: 1, size: 10, total: 0 })

const addMemberVisible = ref(false)
const addMemberLoading = ref(false)
const addMemberKeyword = ref('')
const notInMembers = ref<WorkspaceMember[]>([])
const addMemberPage = reactive({ current: 1, size: 10, total: 0 })

async function load() {
  loading.value = true
  try {
    const res = await listWorkspace(
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
  Object.assign(form, { name: '', code: '', secret: '', status: 'ENABLE' })
  dialogVisible.value = true
}

function openEdit(row: WorkspaceItem) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, code: row.code, secret: '', status: row.status })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name) {
    ElMessage.warning('请填写名称')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateWorkspace({ id: editingId.value, name: form.name, secret: form.secret || undefined, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await addWorkspace({
        name: form.name,
        code: form.code || undefined,
        secret: form.secret || undefined,
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

async function handleDelete(row: WorkspaceItem) {
  await ElMessageBox.confirm(`确认删除工作空间「${row.name}」？`, '提示', { type: 'warning' })
  await deleteWorkspace(row.id)
  ElMessage.success('删除成功')
  load()
}

function openMembers(row: WorkspaceItem) {
  memberWorkspaceId.value = row.id
  memberWorkspaceName.value = row.name
  memberTab.value = '1'
  memberPage.current = 1
  memberVisible.value = true
  loadMembers()
}

async function loadMembers() {
  memberLoading.value = true
  try {
    const res = await listWorkspaceMembers(
      memberWorkspaceId.value,
      Number(memberTab.value),
      undefined,
      memberPage.current,
      memberPage.size
    )
    members.value = res.records
    memberPage.total = res.total
  } finally {
    memberLoading.value = false
  }
}

async function transfer(row: WorkspaceMember, type: number) {
  await permissionTransfer(memberWorkspaceId.value, row.id, type)
  ElMessage.success('已更新')
  loadMembers()
}

async function removeMember(row: WorkspaceMember) {
  await ElMessageBox.confirm(`确认移除成员「${row.username}」？`, '提示', { type: 'warning' })
  await deleteMember(memberWorkspaceId.value, row.id)
  ElMessage.success('已移除')
  loadMembers()
}

function openAddMember() {
  addMemberKeyword.value = ''
  addMemberPage.current = 1
  addMemberVisible.value = true
  loadNotInMembers()
}

async function loadNotInMembers() {
  addMemberLoading.value = true
  try {
    const res = await listNotInMembers(
      memberWorkspaceId.value,
      addMemberKeyword.value || undefined,
      addMemberPage.current,
      addMemberPage.size
    )
    notInMembers.value = res.records
    addMemberPage.total = res.total
  } finally {
    addMemberLoading.value = false
  }
}

async function bind(row: WorkspaceMember) {
  await bindMember(row.id, memberWorkspaceId.value)
  ElMessage.success('已添加')
  loadNotInMembers()
  loadMembers()
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

.member-toolbar {
  margin-bottom: 8px;
}
</style>
