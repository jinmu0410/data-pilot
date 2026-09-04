<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="名称">
            <el-input v-model="query.name" placeholder="数据源名称" clearable style="width: 170px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="t in DATASOURCE_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
              <el-option label="启用" value="ENABLE" />
              <el-option label="禁用" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button type="success" :icon="Plus" @click="openAdd">新增数据源</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="130" />
        <el-table-column prop="code" label="编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="url" label="连接地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openMeta(row)">元数据</el-button>
            <el-button link type="warning" @click="handleTest(row)">测试</el-button>
            <el-button link type="info" @click="handleCopy(row)">复制</el-button>
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

    <!-- 新建/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑数据源' : '新增数据源'" width="620px" top="6vh">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="32" placeholder="数据源名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
            <el-option v-for="t in DATASOURCE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <template v-if="isJdbc">
          <el-form-item label="JDBC URL" required>
            <el-input v-model="form.url" placeholder="jdbc:mysql://host:3306/db" />
          </el-form-item>
          <el-form-item label="驱动类">
            <el-input v-model="form.driver" :placeholder="driverPlaceholder" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item :label="form.type === 'Kafka' ? 'Bootstrap Servers' : '连接地址'" required>
            <el-input v-model="form.url" :placeholder="form.type === 'Kafka' ? 'host1:9092,host2:9092' : 'http://host:9200'" />
          </el-form-item>
        </template>

        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="editingId ? '留空则不修改密码' : ''" />
        </el-form-item>

        <el-form-item v-if="isJdbc" label="连接池大小">
          <el-input-number v-model="form.maxPoolSize" :min="1" :max="200" style="width: 100%" />
        </el-form-item>

        <template v-if="form.type === 'Doris'">
          <el-form-item label="FE 节点">
            <el-input v-model="form.feNodes" placeholder="逗号分隔，如 host1:8030,host2:8030" />
          </el-form-item>
          <el-form-item label="BE 节点">
            <el-input v-model="form.beNodes" placeholder="逗号分隔，如 host1:8040,host2:8040" />
          </el-form-item>
        </template>

        <el-form-item v-if="form.type === 'PostgreSQL'" label="分表规则">
          <el-input v-model="form.partitioningAlgorithm" placeholder="分表规则（可选）" />
        </el-form-item>

        <el-form-item label="健康检查">
          <el-radio-group v-model="form.healthCheck">
            <el-radio value="ENABLE">开启</el-radio>
            <el-radio value="DISABLE">关闭</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLE">启用</el-radio>
            <el-radio value="DISABLE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="testing" @click="handleTestInForm">测试连接</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 元数据浏览 -->
    <el-dialog v-model="metaVisible" :title="`元数据 - ${metaName}`" width="860px" top="6vh">
      <div class="meta-body">
        <div class="meta-tree">
          <el-tree
            v-loading="metaTreeLoading"
            :data="metaTree"
            node-key="key"
            :props="{ label: 'label', children: 'children' }"
            highlight-current
            :expand-on-click-node="false"
            @node-click="onMetaNodeClick"
          />
        </div>
        <div class="meta-detail">
          <template v-if="metaDetailLoading">
            <el-skeleton :rows="8" animated />
          </template>
          <template v-else-if="metaDetail">
            <div class="meta-detail-head">
              <span class="meta-table-name">{{ metaTable }}</span>
              <span v-if="metaDetail.comment" class="meta-table-comment">{{ metaDetail.comment }}</span>
            </div>
            <el-table :data="metaDetail.columns" border size="small" max-height="280">
              <el-table-column prop="name" label="字段名" min-width="130" />
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column label="主键" width="60">
                <template #default="{ row }">
                  <el-tag v-if="row.primaryKey" type="danger" size="small">PK</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="可空" width="60">
                <template #default="{ row }">{{ row.notNull ? '否' : '是' }}</template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" width="100" show-overflow-tooltip />
              <el-table-column prop="comment" label="注释" min-width="120" show-overflow-tooltip />
            </el-table>
            <div v-if="metaDetail.indexes?.length" class="meta-index-title">索引</div>
            <el-table v-if="metaDetail.indexes?.length" :data="metaDetail.indexes" border size="small" max-height="160">
              <el-table-column prop="name" label="索引名" min-width="140" />
              <el-table-column label="唯一" width="70">
                <template #default="{ row }">{{ row.unique ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="字段" min-width="160">
                <template #default="{ row }">{{ row.columns?.join(', ') }}</template>
              </el-table-column>
            </el-table>
          </template>
          <div v-else class="meta-empty">点击左侧表查看结构</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  DATASOURCE_TYPES,
  listDataSource,
  getDataSourceDetail,
  addDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSource,
  listSchemaTable,
  tableDetail,
  type DataSourceItem,
  type DataSourceForm,
  type SchemaTableMap,
  type TableDetail
} from '../../api/datasource'

const loading = ref(false)
const list = ref<DataSourceItem[]>([])
const query = reactive({ name: '', type: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const testing = ref(false)
const editingId = ref(0)
const form = reactive<DataSourceForm>({
  name: '',
  type: 'MySQL',
  url: '',
  driver: '',
  username: '',
  password: '',
  maxPoolSize: 10,
  status: 'ENABLE',
  feNodes: '',
  beNodes: '',
  partitioningAlgorithm: '',
  healthCheck: 'ENABLE',
  description: ''
})

const isJdbc = computed(() => ['MySQL', 'Doris', 'PostgreSQL'].includes(form.type))
const driverPlaceholder = computed(
  () =>
    ({
      MySQL: 'com.mysql.cj.jdbc.Driver',
      Doris: 'com.mysql.cj.jdbc.Driver',
      PostgreSQL: 'org.postgresql.Driver'
    })[form.type] ?? ''
)

const metaVisible = ref(false)
const metaName = ref('')
const metaId = ref(0)
const metaTreeLoading = ref(false)
const metaTree = ref<SchemaTableMap[]>([])
const metaDetailLoading = ref(false)
const metaDetail = ref<TableDetail | null>(null)
const metaTable = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listDataSource(
      { name: query.name || undefined, type: query.type || undefined, status: query.status || undefined },
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

function resetForm() {
  Object.assign(form, {
    name: '',
    type: 'MySQL',
    url: '',
    driver: '',
    username: '',
    password: '',
    maxPoolSize: 10,
    status: 'ENABLE',
    feNodes: '',
    beNodes: '',
    partitioningAlgorithm: '',
    healthCheck: 'ENABLE',
    description: ''
  })
}

function onTypeChange() {
  form.url = ''
  form.driver = ''
  form.feNodes = ''
  form.beNodes = ''
  form.partitioningAlgorithm = ''
}

function openAdd() {
  editingId.value = 0
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: DataSourceItem) {
  editingId.value = row.id
  resetForm()
  const d = await getDataSourceDetail(row.id)
  Object.assign(form, {
    name: d.name,
    type: d.type,
    url: d.url,
    driver: d.driver,
    username: d.username,
    password: '',
    maxPoolSize: d.maxPoolSize,
    status: d.status,
    feNodes: d.feNodes,
    beNodes: d.beNodes,
    partitioningAlgorithm: d.partitioningAlgorithm,
    healthCheck: d.healthCheck || 'DISABLE',
    description: d.description
  })
  dialogVisible.value = true
}

async function handleCopy(row: DataSourceItem) {
  editingId.value = 0
  resetForm()
  const d = await getDataSourceDetail(row.id)
  Object.assign(form, {
    name: `${d.name}_copy`,
    type: d.type,
    url: d.url,
    driver: d.driver,
    username: d.username,
    password: '',
    maxPoolSize: d.maxPoolSize,
    status: d.status,
    feNodes: d.feNodes,
    beNodes: d.beNodes,
    partitioningAlgorithm: d.partitioningAlgorithm,
    healthCheck: d.healthCheck || 'DISABLE',
    description: d.description
  })
  dialogVisible.value = true
  ElMessage.info('已复制配置，密码请重新填写')
}

function buildTestPayload(): DataSourceForm {
  return {
    name: form.name || 'test',
    type: form.type,
    url: form.url,
    driver: form.driver || '-',
    username: form.username,
    password: form.password,
    status: form.status
  }
}

async function handleTestInForm() {
  if (!form.url) {
    ElMessage.warning('请先填写连接地址')
    return
  }
  testing.value = true
  try {
    await testDataSource(buildTestPayload())
    ElMessage.success('连接成功')
  } finally {
    testing.value = false
  }
}

async function handleTest(row: DataSourceItem) {
  const d = await getDataSourceDetail(row.id)
  testing.value = true
  try {
    await testDataSource({
      name: d.name,
      type: d.type,
      url: d.url,
      driver: d.driver || '-',
      username: d.username,
      password: d.password,
      status: d.status
    })
    ElMessage.success(`「${d.name}」连接成功`)
  } finally {
    testing.value = false
  }
}

async function handleSubmit() {
  if (!form.name) {
    ElMessage.warning('请填写名称')
    return
  }
  if (!form.url) {
    ElMessage.warning('请填写连接地址')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateDataSource({ ...form, id: editingId.value })
      ElMessage.success('更新成功')
    } else {
      await addDataSource({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: DataSourceItem) {
  await ElMessageBox.confirm(`确认删除数据源「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDataSource(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openMeta(row: DataSourceItem) {
  metaName.value = row.name
  metaId.value = row.id
  metaTable.value = ''
  metaDetail.value = null
  metaVisible.value = true
  metaTreeLoading.value = true
  try {
    const res = await listSchemaTable(row.id)
    metaTree.value = res.map((item) => ({
      key: item.key,
      label: `${item.label}${item.tag ? ` (${item.tag})` : ''}`,
      tag: item.tag,
      children: (item.children ?? []).map((c) => ({
        key: `${c.schema}.${c.key}`,
        label: c.label || c.key,
        schema: c.schema,
        table: c.key
      }))
    }))
  } finally {
    metaTreeLoading.value = false
  }
}

async function onMetaNodeClick(data: { schema?: string; table?: string; key: string }) {
  if (!data.schema || !data.table) return
  metaTable.value = data.table
  metaDetailLoading.value = true
  try {
    metaDetail.value = await tableDetail(metaId.value, data.schema, data.table)
  } finally {
    metaDetailLoading.value = false
  }
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

.meta-body {
  display: flex;
  height: 480px;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  overflow: hidden;
}

.meta-tree {
  width: 260px;
  border-right: 1px solid #eef0f4;
  padding: 8px;
  overflow-y: auto;
}

.meta-detail {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.meta-detail-head {
  margin-bottom: 10px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.meta-table-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.meta-table-comment {
  font-size: 12px;
  color: #909399;
}

.meta-index-title {
  margin: 12px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.meta-empty {
  color: #c0c4cc;
  text-align: center;
  margin-top: 120px;
}
</style>
