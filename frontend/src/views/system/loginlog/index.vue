<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('system.username')">
            <el-input v-model="query.username" :placeholder="t('system.username')" clearable style="width: 160px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item :label="t('system.ip')">
            <el-input v-model="query.ip" :placeholder="t('system.ipPlaceholder')" clearable style="width: 150px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item :label="t('system.time')">
            <el-date-picker
              v-model="range"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              :start-placeholder="t('system.startTime')"
              :end-placeholder="t('system.endTime')"
              style="width: 360px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column :label="t('system.username')" min-width="120">
          <template #default="{ row }">{{ row.user?.username ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="ip" :label="t('system.ip')" width="140" />
        <el-table-column prop="browser" :label="t('system.browser')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="os" :label="t('system.os')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="platform" :label="t('system.platform')" width="110" />
        <el-table-column prop="createTime" :label="t('system.loginTime')" width="170" />
        <el-table-column :label="t('system.actions')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('system.detail') }}</el-button>
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

    <el-dialog v-model="detailVisible" :title="t('system.loginLogDetail')" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ detail?.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.requestId')">{{ detail?.requestId }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.username')">{{ detail?.username }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.ip')">{{ detail?.ip }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.browser')">{{ detail?.browser }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.os')">{{ detail?.os }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.platform')">{{ detail?.platform }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent">{{ detail?.userAgent }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.loginTime')">{{ detail?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  listLoginLog,
  getLoginLogDetail,
  deleteLoginLog,
  type LoginLogItem,
  type LoginLogDetail
} from '../../../api/loginlog'

const { t } = useI18n()
const loading = ref(false)
const list = ref<LoginLogItem[]>([])
const query = reactive({ username: '', ip: '' })
const range = ref<[string, string] | null>(null)
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const detail = ref<LoginLogDetail | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await listLoginLog(
      {
        username: query.username || undefined,
        ip: query.ip || undefined,
        startCreateTime: range.value?.[0],
        endCreateTime: range.value?.[1]
      },
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

async function openDetail(row: LoginLogItem) {
  detail.value = await getLoginLogDetail(row.id)
  detailVisible.value = true
}

async function handleDelete(row: LoginLogItem) {
  await ElMessageBox.confirm(t('system.deleteLoginLogConfirm'), t('system.prompt'), { type: 'warning' })
  await deleteLoginLog(row.id)
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
