<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('system.name')">
            <el-input v-model="query.name" :placeholder="t('system.workspaceName')" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item :label="t('system.status')">
            <el-select v-model="query.status" :placeholder="t('system.all')" clearable style="width: 120px">
              <el-option :label="t('system.enable')" value="ENABLE" />
              <el-option :label="t('system.disable')" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">{{ t('system.addWorkspace') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('system.name')" min-width="150" />
        <el-table-column prop="code" :label="t('system.code')" min-width="150" />
        <el-table-column :label="t('system.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? t('system.enable') : t('system.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('system.createTime')" width="170" />
        <el-table-column :label="t('system.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="warning" @click="openMembers(row)">{{ t('system.members') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? t('system.editWorkspace') : t('system.addWorkspace')" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="t('system.name')" required>
          <el-input v-model="form.name" :placeholder="t('system.workspaceName')" />
        </el-form-item>
        <el-form-item v-if="!editingId" :label="t('system.code')">
          <el-input v-model="form.code" :placeholder="t('system.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.secret')">
          <el-input v-model="form.secret" :placeholder="t('system.secretPlaceholder')" />
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

    <el-dialog v-model="memberVisible" :title="t('system.memberManage', { name: memberWorkspaceName })" width="640px">
      <div class="member-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="openAddMember">{{ t('system.addMember') }}</el-button>
      </div>
      <el-tabs v-model="memberTab" @tab-change="loadMembers">
        <el-tab-pane :label="t('system.admin')" name="1" />
        <el-tab-pane :label="t('system.normalUser')" name="0" />
      </el-tabs>
      <el-table v-loading="memberLoading" :data="members" border size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" :label="t('system.username')" min-width="140" />
        <el-table-column prop="email" :label="t('system.email')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('system.actions')" width="140">
          <template #default="{ row }">
            <el-button v-if="memberTab === '1'" link type="warning" @click="transfer(row, 0)">{{ t('system.setNormalUser') }}</el-button>
            <el-button v-else link type="primary" @click="transfer(row, 1)">{{ t('system.setAdmin') }}</el-button>
            <el-button link type="danger" @click="removeMember(row)">{{ t('system.remove') }}</el-button>
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

    <el-dialog v-model="addMemberVisible" :title="t('system.addMember')" width="520px">
      <el-input v-model="addMemberKeyword" :placeholder="t('system.usernameSearch')" clearable style="margin-bottom: 12px" @keyup.enter="loadNotInMembers" />
      <el-table v-loading="addMemberLoading" :data="notInMembers" border size="small" max-height="360">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" :label="t('system.username')" min-width="140" />
        <el-table-column prop="email" :label="t('system.email')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('system.actions')" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="bind(row)">{{ t('system.add') }}</el-button>
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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
    ElMessage.warning(t('system.nameRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateWorkspace({ id: editingId.value, name: form.name, secret: form.secret || undefined, status: form.status })
      ElMessage.success(t('system.updateSuccess'))
    } else {
      await addWorkspace({
        name: form.name,
        code: form.code || undefined,
        secret: form.secret || undefined,
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

async function handleDelete(row: WorkspaceItem) {
  await ElMessageBox.confirm(t('system.deleteWorkspaceConfirm', { name: row.name }), t('system.prompt'), { type: 'warning' })
  await deleteWorkspace(row.id)
  ElMessage.success(t('system.deleteSuccess'))
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
  ElMessage.success(t('system.updated'))
  loadMembers()
}

async function removeMember(row: WorkspaceMember) {
  await ElMessageBox.confirm(t('system.removeMemberConfirm', { username: row.username }), t('system.prompt'), { type: 'warning' })
  await deleteMember(memberWorkspaceId.value, row.id)
  ElMessage.success(t('system.removed'))
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
  ElMessage.success(t('system.added'))
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
