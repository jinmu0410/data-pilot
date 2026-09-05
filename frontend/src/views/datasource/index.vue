<template>
  <div class="datasource-page">
    <section class="page-hero">
      <div>
        <span class="page-eyebrow">DATA CONNECTIONS</span>
        <h2>{{ t('datasource.title') }}</h2>
        <p>{{ t('datasource.subtitle') }}</p>
      </div>
      <div class="hero-stats">
        <div><strong>{{ page.total }}</strong><span>{{ t('datasource.totalConnections') }}</span></div>
        <div><strong>{{ enabledCount }}</strong><span>{{ t('datasource.enabledThisPage') }}</span></div>
        <div><strong>{{ activeTypeCount }}</strong><span>{{ t('datasource.activeTypes') }}</span></div>
      </div>
      <el-button type="primary" :icon="Plus" size="large" @click="openAdd">{{ t('datasource.add') }}</el-button>
    </section>

    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item :label="t('datasource.name')"><el-input v-model="query.name" :placeholder="t('datasource.namePlaceholder')" clearable style="width: 200px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item :label="t('datasource.type')"><el-select v-model="query.type" :placeholder="t('datasource.allTypes')" clearable style="width: 150px"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item :label="t('datasource.status')"><el-select v-model="query.status" :placeholder="t('datasource.allStatus')" clearable style="width: 130px"><el-option :label="t('datasource.enable')" value="ENABLE" /><el-option :label="t('datasource.disable')" value="DISABLE" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button><el-button :icon="Refresh" @click="resetSearch">{{ t('datasource.reset') }}</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div class="list-heading"><div><h3>{{ t('datasource.connectionList') }}</h3><span>{{ t('datasource.connectedCount', { count: page.total }) }}</span></div><span class="support-tip">{{ t('datasource.supportTip') }}</span></div>
      <el-table v-loading="loading" :data="list" class="datasource-table">
        <el-table-column :label="t('datasource.colSource')" min-width="220"><template #default="{ row }"><div class="source-cell"><span class="source-logo" :class="typeClass(row.type)">{{ typeMeta(row.type).abbr }}</span><div><strong>{{ row.name }}</strong><small>{{ row.code }}</small></div></div></template></el-table-column>
        <el-table-column :label="t('datasource.type')" width="135"><template #default="{ row }"><span class="type-badge" :class="typeClass(row.type)">{{ typeMeta(row.type).label }}</span></template></el-table-column>
        <el-table-column :label="t('datasource.colConnection')" min-width="240"><template #default="{ row }"><div class="connection-cell"><strong>{{ row.url || '-' }}</strong><small>{{ row.username || t('datasource.noAuth') }}</small></div></template></el-table-column>
        <el-table-column :label="t('datasource.colHealth')" width="105"><template #default="{ row }"><span class="health-status" :class="{ enabled: row.healthCheck === 'ENABLE' }"><i></i>{{ row.healthCheck === 'ENABLE' ? t('datasource.healthOn') : t('datasource.healthOff') }}</span></template></el-table-column>
        <el-table-column :label="t('datasource.status')" width="90"><template #default="{ row }"><span class="status-badge" :class="{ enabled: row.status === 'ENABLE' }"><i></i>{{ row.status === 'ENABLE' ? t('datasource.enable') : t('datasource.disable') }}</span></template></el-table-column>
        <el-table-column prop="createTime" :label="t('datasource.colCreateTime')" width="165" />
        <el-table-column :label="t('datasource.colActions')" width="240" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button><el-button link type="success" :disabled="!supportsMetadata(row.type)" @click="openMeta(row)">{{ t('datasource.metadata') }}</el-button><el-button link type="warning" @click="handleTest(row)">{{ t('datasource.test') }}</el-button><el-dropdown trigger="click"><el-button link>{{ t('datasource.more') }}</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="handleCopy(row)">{{ t('datasource.copy') }}</el-dropdown-item><el-dropdown-item divided class="danger-menu-item" @click="handleDelete(row)">{{ t('datasource.delete') }}</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" class="pager" layout="total, prev, pager, next, sizes" :total="page.total" :page-sizes="[10, 20, 50]" @change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" width="min(1080px, calc(100vw - 40px))" top="4vh" append-to-body destroy-on-close :close-on-click-modal="false" class="datasource-config-dialog">
      <template #header><div class="dialog-heading"><span class="dialog-logo" :class="typeClass(form.type)">{{ currentType.abbr }}</span><div><div class="dialog-title">{{ editingId ? t('datasource.editTitle') : t('datasource.createDialogTitle') }}</div><div class="dialog-subtitle">{{ t('datasource.dialogSubtitle') }}</div></div><span class="dialog-mode">{{ editingId ? 'EDIT' : 'NEW CONNECTION' }}</span></div></template>
      <div class="config-shell">
        <aside class="type-sidebar">
          <span class="sidebar-label">{{ t('datasource.selectType') }}</span>
          <button v-for="item in typeOptions" :key="item.value" type="button" class="type-option" :class="[{ active: form.type === item.value }, typeClass(item.value)]" @click="selectType(item.value)"><span>{{ item.abbr }}</span><div><strong>{{ item.label }}</strong><small>{{ item.category }}</small></div><i>✓</i></button>
          <div class="connection-summary"><span>{{ t('datasource.currentConfig') }}</span><strong>{{ form.name || t('datasource.unnamed') }}</strong><p>{{ currentType.protocol }}</p><div><i :class="{ ready: form.url }"></i>{{ form.url ? t('datasource.urlFilled') : t('datasource.urlPending') }}</div></div>
        </aside>

        <main class="config-content">
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">01</span><div><h3>{{ t('datasource.basicInfo') }}</h3><p>{{ t('datasource.basicInfoDesc') }}</p></div></div></div>
            <div class="form-grid basic-grid">
              <div class="field wide-field"><label>{{ t('datasource.nameLabel') }} <em>*</em></label><el-input v-model="form.name" maxlength="32" show-word-limit :placeholder="t('datasource.nameFormPlaceholder')" size="large" /></div>
              <div class="field"><label>{{ t('datasource.runningStatus') }}</label><el-segmented v-model="form.status" :options="statusOptions" block /></div>
              <div class="field wide-field"><label>{{ t('datasource.description') }}</label><el-input v-model="form.description" type="textarea" :rows="2" maxlength="300" :placeholder="t('datasource.descriptionPlaceholder')" /></div>
            </div>
          </section>

          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">02</span><div><h3>{{ t('datasource.connectionConfig') }}</h3><p>{{ currentType.description }}</p></div></div><button type="button" class="recommend-button" @click="applyRecommendedConfig">{{ t('datasource.useRecommended') }}</button></div>
            <div class="connection-form-card">
              <div class="endpoint-strip"><span class="source-logo" :class="typeClass(form.type)">{{ currentType.abbr }}</span><div><strong>{{ currentType.label }}</strong><small>{{ currentType.protocol }}</small></div><span>{{ isJdbc ? 'JDBC' : currentType.value === 'Kafka' ? 'BROKER' : 'HTTP' }}</span></div>
              <div class="form-grid">
                <div class="field full-field"><label>{{ connectionLabel }} <em>*</em></label><el-input v-model="form.url" :placeholder="currentType.urlPlaceholder" size="large"><template #prepend>{{ connectionPrefix }}</template></el-input><small>{{ currentType.urlHint }}</small></div>
                <div v-if="isJdbc" class="field full-field"><label>{{ t('datasource.driverClass') }} <em>*</em></label><el-input v-model="form.driver" :placeholder="currentType.driver" /><small>{{ t('datasource.driverHint') }}</small></div>
                <div class="field"><label>{{ t('datasource.username') }}</label><el-input v-model="form.username" autocomplete="off" :placeholder="t('datasource.usernamePlaceholder')" /></div>
                <div class="field"><label>{{ t('datasource.password') }}</label><el-input v-model="form.password" type="password" show-password autocomplete="new-password" :placeholder="editingId ? t('datasource.passwordKeep') : t('datasource.passwordPlaceholder')" /></div>
              </div>
              <div v-if="form.type === 'Doris'" class="extra-config"><div class="extra-title"><strong>{{ t('datasource.dorisConfig') }}</strong><span>{{ t('datasource.dorisConfigDesc') }}</span></div><div class="form-grid"><div class="field"><label>{{ t('datasource.feNodes') }}</label><el-input v-model="form.feNodes" placeholder="fe-1:8030,fe-2:8030" /></div><div class="field"><label>{{ t('datasource.beNodes') }}</label><el-input v-model="form.beNodes" placeholder="be-1:8040,be-2:8040" /></div></div></div>
              <div v-if="form.type === 'Kafka'" class="config-hint"><span>i</span><p>{{ t('datasource.kafkaHint') }}</p></div>
              <div v-if="form.type === 'Elastic'" class="config-hint"><span>i</span><p>{{ t('datasource.elasticHint') }}</p></div>
            </div>
          </section>

          <section class="form-section settings-section">
            <div class="section-heading"><div><span class="section-index">03</span><div><h3>{{ t('datasource.runtimeSettings') }}</h3><p>{{ t('datasource.runtimeSettingsDesc') }}</p></div></div></div>
            <div class="settings-grid">
              <div v-if="isJdbc" class="setting-card"><div><strong>{{ t('datasource.poolSize') }}</strong><small>{{ t('datasource.poolSizeDesc') }}</small></div><el-input-number v-model="form.maxPoolSize" :min="1" :max="200" controls-position="right" /></div>
              <div class="setting-card"><div><strong>{{ t('datasource.healthCheck') }}</strong><small>{{ t('datasource.healthCheckDesc') }}</small></div><el-switch v-model="form.healthCheck" active-value="ENABLE" inactive-value="DISABLE" /></div>
              <div v-if="supportsPartitioning(form.type)" class="setting-card full-setting"><div><strong>{{ t('datasource.partitioning') }}</strong><small>{{ t('datasource.partitioningDesc') }}</small></div><el-input v-model="form.partitioningAlgorithm" :placeholder="t('datasource.partitioningPlaceholder')" /></div>
            </div>
          </section>
        </main>
      </div>
      <template #footer><div class="dialog-footer"><span class="footer-tip">{{ t('datasource.footerTip') }}</span><el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button><el-button :loading="testing" @click="handleTestInForm">{{ t('datasource.testConnection') }}</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingId ? t('datasource.saveChanges') : t('datasource.createDataSource') }}</el-button></div></template>
    </el-dialog>

    <el-dialog v-model="metaVisible" width="min(1000px, calc(100vw - 40px))" top="5vh" append-to-body class="metadata-dialog">
      <template #header><div class="dialog-heading"><span class="dialog-logo metadata-logo">META</span><div><div class="dialog-title">{{ t('datasource.metadataTitle') }}</div><div class="dialog-subtitle">{{ metaName }} · {{ t('datasource.metadataSubtitle') }}</div></div></div></template>
      <div class="meta-body">
        <aside class="meta-tree-panel"><div class="meta-panel-title"><strong>{{ t('datasource.schemaCatalog') }}</strong><span>{{ t('datasource.schemaCount', { count: metaTree.length }) }}</span></div><el-tree v-loading="metaTreeLoading" :data="metaTree" node-key="key" :props="{ label: 'label', children: 'children' }" highlight-current :expand-on-click-node="false" @node-click="onMetaNodeClick" /></aside>
        <main class="meta-detail">
          <el-skeleton v-if="metaDetailLoading" :rows="8" animated />
          <template v-else-if="metaDetail"><div class="meta-detail-head"><div><span>TABLE</span><strong>{{ metaTable }}</strong></div><p>{{ metaDetail.comment || t('datasource.noTableComment') }}</p></div><div class="meta-block-title"><strong>{{ t('datasource.fieldStructure') }}</strong><span>{{ t('datasource.fieldCount', { count: metaDetail.columns.length }) }}</span></div><el-table :data="metaDetail.columns" border size="small" max-height="285"><el-table-column prop="name" :label="t('datasource.fieldName')" min-width="140" /><el-table-column prop="type" :label="t('datasource.type')" width="125" /><el-table-column :label="t('datasource.constraint')" width="95"><template #default="{ row }"><el-tag v-if="row.primaryKey" type="danger" size="small">PK</el-tag><span v-else>{{ row.notNull ? 'NOT NULL' : '-' }}</span></template></el-table-column><el-table-column prop="defaultValue" :label="t('datasource.defaultValue')" width="110" show-overflow-tooltip /><el-table-column prop="comment" :label="t('datasource.comment')" min-width="140" show-overflow-tooltip /></el-table><template v-if="metaDetail.indexes?.length"><div class="meta-block-title index-title"><strong>{{ t('datasource.indexes') }}</strong><span>{{ t('datasource.indexCount', { count: metaDetail.indexes.length }) }}</span></div><el-table :data="metaDetail.indexes" border size="small" max-height="155"><el-table-column prop="name" :label="t('datasource.indexName')" min-width="150" /><el-table-column :label="t('datasource.unique')" width="75"><template #default="{ row }">{{ row.unique ? t('datasource.yes') : t('datasource.no') }}</template></el-table-column><el-table-column :label="t('datasource.fields')" min-width="180"><template #default="{ row }">{{ row.columns?.join(', ') }}</template></el-table-column></el-table></template></template>
          <div v-else class="meta-empty"><span>▦</span><strong>{{ t('datasource.selectTable') }}</strong><p>{{ t('datasource.selectTableHint') }}</p></div>
        </main>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addDataSource, deleteDataSource, getDataSourceDetail, listDataSource, listSchemaTable, tableDetail, testDataSource, updateDataSource, type DataSourceForm, type DataSourceItem, type SchemaTableMap, type TableDetail } from '../../api/datasource'

const { t } = useI18n()

interface TypeOption { value: string; label: string; abbr: string; category: string; protocol: string; description: string; urlPlaceholder: string; urlHint: string; driver?: string }

interface TypeBase { value: string; label: string; abbr: string; categoryKey: string; protocol: string; descriptionKey: string; urlPlaceholder: string; urlHintKey: string; driver?: string }

const TYPE_BASE: TypeBase[] = [
  { value: 'MySQL', label: 'MySQL', abbr: 'MY', categoryKey: 'catRelational', protocol: 'MySQL JDBC', descriptionKey: 'descMysql', urlPlaceholder: 'jdbc:mysql://db-host:3306/database', urlHintKey: 'hintMysql', driver: 'com.mysql.cj.jdbc.Driver' },
  { value: 'TiDB', label: 'TiDB', abbr: 'TI', categoryKey: 'catDistributed', protocol: 'MySQL Compatible', descriptionKey: 'descTiDB', urlPlaceholder: 'jdbc:mysql://tidb-host:4000/database', urlHintKey: 'hintTiDB', driver: 'com.mysql.cj.jdbc.Driver' },
  { value: 'Doris', label: 'Doris', abbr: 'DO', categoryKey: 'catAnalytical', protocol: 'MySQL / FE / BE', descriptionKey: 'descDoris', urlPlaceholder: 'jdbc:mysql://fe-host:9030/database', urlHintKey: 'hintDoris', driver: 'com.mysql.cj.jdbc.Driver' },
  { value: 'PostgreSQL', label: 'PostgreSQL', abbr: 'PG', categoryKey: 'catRelational', protocol: 'PostgreSQL JDBC', descriptionKey: 'descPostgreSQL', urlPlaceholder: 'jdbc:postgresql://db-host:5432/database', urlHintKey: 'hintPostgreSQL', driver: 'org.postgresql.Driver' },
  { value: 'Kafka', label: 'Kafka', abbr: 'KF', categoryKey: 'catQueue', protocol: 'Kafka Brokers', descriptionKey: 'descKafka', urlPlaceholder: 'broker-1:9092,broker-2:9092', urlHintKey: 'hintKafka' },
  { value: 'Elastic', label: 'Elasticsearch', abbr: 'ES', categoryKey: 'catSearch', protocol: 'Elasticsearch HTTP', descriptionKey: 'descElastic', urlPlaceholder: 'http://es-host:9200', urlHintKey: 'hintElastic' }
]

const typeOptions = computed<TypeOption[]>(() => TYPE_BASE.map((item) => ({
  value: item.value,
  label: item.label,
  abbr: item.abbr,
  category: t(`datasource.${item.categoryKey}`),
  protocol: item.protocol,
  description: t(`datasource.${item.descriptionKey}`),
  urlPlaceholder: item.urlPlaceholder,
  urlHint: t(`datasource.${item.urlHintKey}`),
  driver: item.driver
})))

const loading = ref(false)
const list = ref<DataSourceItem[]>([])
const query = reactive({ name: '', type: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const enabledCount = computed(() => list.value.filter((item) => item.status === 'ENABLE').length)
const activeTypeCount = computed(() => new Set(list.value.map((item) => item.type)).size)
const dialogVisible = ref(false)
const submitting = ref(false)
const testing = ref(false)
const editingId = ref(0)
const form = reactive<DataSourceForm>(defaultForm())
const currentType = computed(() => typeMeta(form.type))
const isJdbc = computed(() => ['MySQL', 'TiDB', 'Doris', 'PostgreSQL'].includes(form.type))
const connectionLabel = computed(() => form.type === 'Kafka' ? 'Bootstrap Servers' : form.type === 'Elastic' ? t('datasource.elasticUrlLabel') : 'JDBC URL')
const connectionPrefix = computed(() => form.type === 'Kafka' ? 'BROKER' : form.type === 'Elastic' ? 'HTTP' : 'JDBC')
const statusOptions = computed(() => [{ label: t('datasource.enable'), value: 'ENABLE' }, { label: t('datasource.disable'), value: 'DISABLE' }])
const metaVisible = ref(false)
const metaName = ref('')
const metaId = ref(0)
const metaTreeLoading = ref(false)
const metaTree = ref<SchemaTableMap[]>([])
const metaDetailLoading = ref(false)
const metaDetail = ref<TableDetail | null>(null)
const metaTable = ref('')

function defaultForm(): DataSourceForm { return { name: '', type: 'MySQL', url: '', driver: 'com.mysql.cj.jdbc.Driver', username: '', password: '', maxPoolSize: 10, status: 'ENABLE', feNodes: '', beNodes: '', partitioningAlgorithm: '', healthCheck: 'ENABLE', description: '' } }
function typeMeta(type: string): TypeOption { return typeOptions.value.find((item) => item.value === type) ?? { ...typeOptions.value[0], value: type, label: type, abbr: type.slice(0, 2).toUpperCase() } }
function typeClass(type: string) { return `type-${type.toLowerCase().replace(/[^a-z0-9]/g, '')}` }
function supportsMetadata(type: string) { return ['MySQL', 'TiDB', 'Doris', 'PostgreSQL'].includes(type) }
function supportsPartitioning(type: string) { return ['MySQL', 'TiDB', 'PostgreSQL'].includes(type) }

async function load() { loading.value = true; try { const res = await listDataSource({ name: query.name || undefined, type: query.type || undefined, status: query.status || undefined }, page.current, page.size); list.value = res.records; page.total = res.total } finally { loading.value = false } }
function handleSearch() { page.current = 1; load() }
function resetSearch() { Object.assign(query, { name: '', type: '', status: '' }); handleSearch() }
function resetForm() { Object.assign(form, defaultForm()) }
function selectType(type: string) { if (form.type === type) return; form.type = type; form.url = ''; form.driver = typeMeta(type).driver ?? ''; form.feNodes = ''; form.beNodes = ''; form.partitioningAlgorithm = '' }
function applyRecommendedConfig() { form.driver = currentType.value.driver ?? ''; if (!form.url) form.url = currentType.value.urlPlaceholder; if (isJdbc.value && !form.maxPoolSize) form.maxPoolSize = 10; ElMessage.success(t('datasource.recommendedApplied')) }
function openAdd() { editingId.value = 0; resetForm(); dialogVisible.value = true }

async function openEdit(row: DataSourceItem) { editingId.value = row.id; resetForm(); const d = await getDataSourceDetail(row.id); Object.assign(form, { name: d.name, type: d.type, url: d.url, driver: d.driver, username: d.username, password: '', maxPoolSize: d.maxPoolSize, status: d.status, feNodes: d.feNodes, beNodes: d.beNodes, partitioningAlgorithm: d.partitioningAlgorithm, healthCheck: d.healthCheck || 'DISABLE', description: d.description }); dialogVisible.value = true }
async function handleCopy(row: DataSourceItem) { editingId.value = 0; resetForm(); const d = await getDataSourceDetail(row.id); Object.assign(form, { name: `${d.name}_copy`, type: d.type, url: d.url, driver: d.driver, username: d.username, password: '', maxPoolSize: d.maxPoolSize, status: d.status, feNodes: d.feNodes, beNodes: d.beNodes, partitioningAlgorithm: d.partitioningAlgorithm, healthCheck: d.healthCheck || 'DISABLE', description: d.description }); dialogVisible.value = true; ElMessage.info(t('datasource.copyInfo')) }
function buildTestPayload(): DataSourceForm { return { id: editingId.value || undefined, name: form.name || 'connection-test', type: form.type, url: form.url, driver: form.driver || '-', username: form.username, password: form.password, status: form.status } }
function validateForm() { if (!form.name?.trim()) return t('datasource.nameRequired'); if (!form.url?.trim()) return t('datasource.urlRequired', { label: connectionLabel.value }); if (isJdbc.value && !form.driver?.trim()) return t('datasource.driverRequired'); if (['MySQL', 'TiDB', 'Doris'].includes(form.type) && !form.url.startsWith('jdbc:mysql://')) return t('datasource.mysqlUrlHint', { label: currentType.value.label }); if (form.type === 'PostgreSQL' && !form.url.startsWith('jdbc:postgresql://')) return t('datasource.pgUrlHint'); if (form.type === 'Elastic' && !/^https?:\/\//i.test(form.url)) return t('datasource.elasticUrlHint'); return '' }
async function handleTestInForm() { const message = validateForm(); if (message) { ElMessage.warning(message); return }; testing.value = true; try { await testDataSource(buildTestPayload()); ElMessage.success(t('datasource.testSuccess')) } finally { testing.value = false } }
async function handleTest(row: DataSourceItem) { const d = await getDataSourceDetail(row.id); testing.value = true; try { await testDataSource({ id: d.id, name: d.name, type: d.type, url: d.url, driver: d.driver || '-', username: d.username, password: d.password, status: d.status }); ElMessage.success(t('datasource.testRowSuccess', { name: d.name })) } finally { testing.value = false } }
async function handleSubmit() { const message = validateForm(); if (message) { ElMessage.warning(message); return }; submitting.value = true; try { if (editingId.value) { await updateDataSource({ ...form, id: editingId.value }); ElMessage.success(t('datasource.updateSuccess')) } else { await addDataSource({ ...form }); ElMessage.success(t('datasource.createSuccess')) }; dialogVisible.value = false; load() } finally { submitting.value = false } }
async function handleDelete(row: DataSourceItem) { await ElMessageBox.confirm(t('datasource.deleteConfirm', { name: row.name }), t('datasource.deleteTitle'), { type: 'warning', confirmButtonText: t('datasource.confirmDelete'), cancelButtonText: t('common.cancel') }); await deleteDataSource(row.id); ElMessage.success(t('datasource.deleteSuccess')); load() }
async function openMeta(row: DataSourceItem) { if (!supportsMetadata(row.type)) { ElMessage.info(t('datasource.metadataUnsupported', { label: typeMeta(row.type).label })); return }; metaName.value = row.name; metaId.value = row.id; metaTable.value = ''; metaDetail.value = null; metaVisible.value = true; metaTreeLoading.value = true; try { const res = await listSchemaTable(row.id); metaTree.value = res.map((item) => ({ key: item.key, label: item.label, tag: item.tag, children: (item.children ?? []).map((child) => ({ key: `${child.schema}.${child.key}`, label: child.label || child.key, schema: child.schema, table: child.key })) })) } finally { metaTreeLoading.value = false } }
async function onMetaNodeClick(data: { schema?: string; table?: string; key: string }) { if (!data.schema || !data.table) return; metaTable.value = data.table; metaDetailLoading.value = true; try { metaDetail.value = await tableDetail(metaId.value, data.schema, data.table) } finally { metaDetailLoading.value = false } }
onMounted(load)
</script>

<style scoped>
.datasource-page { display: grid; gap: 14px; }
.page-hero { min-height: 116px; display: flex; align-items: center; gap: 28px; padding: 22px 25px; border: 1px solid #e7e9f1; border-radius: 12px; background: linear-gradient(135deg, #fbfbff, #f5f7fc); }
.page-hero > div:first-child { min-width: 280px; flex: 1; }.page-eyebrow { color: #7669ef; font-size: 9px; font-weight: 800; letter-spacing: 1.7px; }.page-hero h2 { margin: 5px 0 6px; color: #20283a; font-size: 22px; }.page-hero p { margin: 0; color: #8992a3; font-size: 11px; }.hero-stats { display: flex; align-items: center; }.hero-stats > div { min-width: 84px; padding: 3px 20px; border-left: 1px solid #e0e3ea; text-align: center; }.hero-stats strong, .hero-stats span { display: block; }.hero-stats strong { color: #30394c; font-size: 21px; }.hero-stats span { margin-top: 3px; color: #9aa2b1; font-size: 9px; }
.filter-card :deep(.el-card__body) { padding: 16px 18px 0; }.filter-card :deep(.el-form-item) { margin-bottom: 16px; }.list-card :deep(.el-card__body) { padding: 0; }.list-heading { height: 67px; display: flex; align-items: center; justify-content: space-between; padding: 0 19px; border-bottom: 1px solid #edf0f4; }.list-heading h3 { display: inline; margin: 0; color: #2c3548; font-size: 14px; }.list-heading > div > span { margin-left: 10px; color: #9aa2b1; font-size: 10px; }.support-tip { padding: 5px 9px; border-radius: 5px; color: #6e7790; background: #f4f5f8; font-size: 9px; }.datasource-table { width: 100%; }
.source-cell { display: flex; align-items: center; gap: 11px; min-width: 0; }.source-logo, .dialog-logo { display: grid; place-items: center; flex: 0 0 auto; color: #fff; background: linear-gradient(145deg, #3b82f6, #6366f1); font-weight: 800; }.source-logo { width: 34px; height: 34px; border-radius: 9px; font-size: 9px; }.source-cell > div, .connection-cell { min-width: 0; }.source-cell strong, .source-cell small, .connection-cell strong, .connection-cell small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.source-cell strong { color: #344054; font-size: 11px; }.source-cell small { margin-top: 4px; color: #a0a7b5; font-size: 8px; }.connection-cell strong { color: #536075; font-size: 10px; font-weight: 500; }.connection-cell small { margin-top: 4px; color: #a0a7b5; font-size: 9px; }
.type-badge { display: inline-flex; padding: 4px 8px; border-radius: 5px; color: #305fbd; background: #edf4ff; font-size: 9px; font-weight: 650; }.status-badge, .health-status { display: inline-flex; align-items: center; gap: 6px; color: #929aaa; font-size: 9px; }.status-badge i, .health-status i { width: 6px; height: 6px; border-radius: 50%; background: #b9bec8; }.status-badge.enabled, .health-status.enabled { color: #079669; }.status-badge.enabled i, .health-status.enabled i { background: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, .1); }.pager { justify-content: flex-end; padding: 16px 18px; border-top: 1px solid #edf0f4; }
.type-mysql, .type-tidb { background: linear-gradient(145deg, #2f80ed, #49a3ff); }.type-doris { background: linear-gradient(145deg, #7357e8, #9b6ff3); }.type-postgresql { background: linear-gradient(145deg, #336791, #4c8fbd); }.type-kafka { background: linear-gradient(145deg, #283342, #566273); }.type-elastic { background: linear-gradient(145deg, #00a9a5, #e7b63f); }.type-badge.type-mysql, .type-badge.type-tidb { color: #2765c7; background: #edf4ff; }.type-badge.type-doris { color: #7049cc; background: #f3edff; }.type-badge.type-postgresql { color: #326a94; background: #edf6fb; }.type-badge.type-kafka { color: #475569; background: #f0f2f4; }.type-badge.type-elastic { color: #087f7b; background: #eaf9f7; }
.type-option.type-mysql > span, .type-option.type-tidb > span { background: linear-gradient(145deg, #2f80ed, #49a3ff); }.type-option.type-doris > span { background: linear-gradient(145deg, #7357e8, #9b6ff3); }.type-option.type-postgresql > span { background: linear-gradient(145deg, #336791, #4c8fbd); }.type-option.type-kafka > span { background: linear-gradient(145deg, #283342, #566273); }.type-option.type-elastic > span { background: linear-gradient(145deg, #00a9a5, #e7b63f); }
.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }.dialog-logo { width: 40px; height: 40px; border-radius: 11px; font-size: 9px; box-shadow: 0 8px 18px rgba(59, 130, 246, .2); }.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }.dialog-subtitle { margin-top: 3px; color: #8a94a6; font-size: 12px; }.dialog-mode { margin-left: auto; padding: 5px 9px; border: 1px solid #dedaff; border-radius: 999px; color: #6758e8; background: #f0efff; font-size: 9px; }
.config-shell { height: min(610px, calc(92vh - 150px)); min-height: 500px; display: grid; grid-template-columns: 220px minmax(0, 1fr); overflow: hidden; border: 1px solid #e7e9f0; border-radius: 12px; background: #fafbfc; }.type-sidebar { display: flex; flex-direction: column; padding: 17px 13px; border-right: 1px solid #e7e9f0; background: #fbfbfd; }.sidebar-label { margin: 0 7px 9px; color: #9aa2b1; font-size: 9px; font-weight: 700; letter-spacing: .8px; }.type-option { width: 100%; display: flex; align-items: center; gap: 10px; padding: 9px 8px; border: 1px solid transparent; border-radius: 8px; color: #687286; text-align: left; background: transparent; cursor: pointer; transition: .18s ease; }.type-option:hover { background: #f2f3f7; }.type-option.active { border-color: #d9d5ff; background: #f0eeff; }.type-option > span { width: 30px; height: 30px; display: grid; place-items: center; flex-shrink: 0; border-radius: 8px; color: #fff; font-size: 8px; font-weight: 800; }.type-option div { min-width: 0; flex: 1; }.type-option strong, .type-option small { display: block; }.type-option strong { color: #3b4558; font-size: 10px; }.type-option small { margin-top: 3px; color: #9ba3b1; font-size: 8px; }.type-option > i { display: none; color: #635bff; font-size: 11px; font-style: normal; }.type-option.active > i { display: block; }
.connection-summary { margin-top: auto; padding: 13px; border: 1px solid #e6e8ef; border-radius: 9px; background: #fff; }.connection-summary > span { color: #9aa2b1; font-size: 8px; letter-spacing: .8px; }.connection-summary > strong { display: block; overflow: hidden; margin-top: 6px; color: #364154; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.connection-summary p { margin: 5px 0 10px; color: #8e97a7; font-size: 8px; }.connection-summary div { display: flex; align-items: center; gap: 6px; color: #9aa2b1; font-size: 8px; }.connection-summary div i { width: 6px; height: 6px; border-radius: 50%; background: #c0c5cf; }.connection-summary div i.ready { background: #10b981; }
.config-content { min-width: 0; overflow-y: auto; padding: 24px 27px; background: #fff; }.form-section + .form-section { margin-top: 25px; padding-top: 23px; border-top: 1px solid #eceef3; }.section-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }.section-heading > div { display: flex; align-items: flex-start; gap: 10px; }.section-index { width: 25px; height: 25px; display: grid; place-items: center; flex-shrink: 0; border-radius: 7px; color: #635bff; background: #eeecff; font-size: 8px; font-weight: 800; }.section-heading h3 { margin: 0; color: #293247; font-size: 13px; }.section-heading p { margin: 4px 0 0; color: #929aaa; font-size: 9px; }.recommend-button { padding: 5px 8px; border: 1px solid #dfe2e9; border-radius: 5px; color: #667085; background: #fff; font-size: 9px; cursor: pointer; }.recommend-button:hover { color: #635bff; border-color: #bdb7f8; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 16px; }.basic-grid { grid-template-columns: 1.5fr .8fr; }.field { min-width: 0; }.field label { display: block; margin-bottom: 7px; color: #4f596c; font-size: 10px; font-weight: 650; }.field label em { color: #ef5361; font-style: normal; }.field > small { display: block; margin-top: 5px; color: #9aa2b1; font-size: 8px; }.wide-field, .full-field { grid-column: 1 / -1; }.connection-form-card { padding: 17px; border: 1px solid #e6e8ef; border-radius: 10px; background: #fff; }.endpoint-strip { display: flex; align-items: center; gap: 10px; margin-bottom: 17px; padding-bottom: 13px; border-bottom: 1px solid #edf0f4; }.endpoint-strip div { flex: 1; }.endpoint-strip strong, .endpoint-strip small { display: block; }.endpoint-strip strong { color: #344054; font-size: 11px; }.endpoint-strip small { margin-top: 3px; color: #9aa2b1; font-size: 8px; }.endpoint-strip > span:last-child { padding: 3px 6px; border-radius: 4px; color: #7669ef; background: #f0eeff; font-size: 8px; font-weight: 700; }
.extra-config { margin-top: 16px; padding: 13px; border-radius: 8px; background: #f7f8fb; }.extra-title { display: flex; justify-content: space-between; margin-bottom: 12px; }.extra-title strong { color: #4b5568; font-size: 10px; }.extra-title span { color: #969ead; font-size: 8px; }.config-hint { display: flex; align-items: flex-start; gap: 9px; margin-top: 15px; padding: 10px 12px; border-radius: 7px; color: #667085; background: #f5f7fb; }.config-hint span { width: 17px; height: 17px; display: grid; place-items: center; flex-shrink: 0; border-radius: 50%; color: #fff; background: #7b72e8; font-size: 9px; font-weight: 700; }.config-hint p { margin: 1px 0 0; font-size: 9px; line-height: 1.55; }
.settings-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.setting-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 13px 14px; border: 1px solid #e8eaf0; border-radius: 8px; background: #fafbfc; }.setting-card > div { min-width: 0; }.setting-card strong, .setting-card small { display: block; }.setting-card strong { color: #4b5568; font-size: 10px; }.setting-card small { margin-top: 4px; color: #969ead; font-size: 8px; }.full-setting { grid-column: 1 / -1; }.full-setting :deep(.el-input) { width: 48%; }.dialog-footer { width: 100%; display: flex; align-items: center; }.footer-tip { margin-right: auto; color: #8f98a8; font-size: 9px; }
.metadata-logo { font-size: 7px; background: linear-gradient(145deg, #0ea5a4, #3b82f6); }.meta-body { height: 520px; display: grid; grid-template-columns: 270px minmax(0, 1fr); overflow: hidden; border: 1px solid #e7e9f0; border-radius: 10px; }.meta-tree-panel { min-width: 0; padding: 15px 11px; overflow-y: auto; border-right: 1px solid #e7e9f0; background: #fafbfc; }.meta-panel-title { display: flex; justify-content: space-between; padding: 0 7px 12px; }.meta-panel-title strong { color: #404a5d; font-size: 11px; }.meta-panel-title span { color: #9aa2b1; font-size: 8px; }.meta-detail { min-width: 0; overflow-y: auto; padding: 19px; }.meta-detail-head { display: flex; align-items: flex-end; justify-content: space-between; padding-bottom: 14px; border-bottom: 1px solid #eceef3; }.meta-detail-head > div { display: flex; align-items: center; gap: 8px; }.meta-detail-head span { padding: 3px 5px; border-radius: 4px; color: #087f7b; background: #eaf9f7; font-size: 8px; font-weight: 800; }.meta-detail-head strong { color: #30394c; font-size: 14px; }.meta-detail-head p { margin: 0; color: #929aaa; font-size: 9px; }.meta-block-title { display: flex; justify-content: space-between; margin: 16px 0 9px; }.meta-block-title strong { color: #475166; font-size: 10px; }.meta-block-title span { color: #9aa2b1; font-size: 8px; }.index-title { margin-top: 18px; }.meta-empty { height: 100%; display: flex; align-items: center; justify-content: center; flex-direction: column; color: #9aa2b1; }.meta-empty > span { width: 45px; height: 45px; display: grid; place-items: center; border-radius: 11px; background: #f1f2f6; font-size: 20px; }.meta-empty strong { margin-top: 10px; color: #606a7e; font-size: 11px; }.meta-empty p { margin: 5px 0 0; font-size: 9px; }
@media (max-width: 900px) { .page-hero { align-items: flex-start; flex-wrap: wrap; }.hero-stats { order: 3; width: 100%; }.hero-stats > div:first-child { border-left: 0; }.config-shell { grid-template-columns: 185px minmax(0, 1fr); }.config-content { padding: 21px 18px; }.form-grid, .basic-grid { grid-template-columns: 1fr; }.wide-field, .full-field { grid-column: auto; }.settings-grid { grid-template-columns: 1fr; }.full-setting { grid-column: auto; }.full-setting :deep(.el-input) { width: 100%; } }
@media (max-width: 680px) {
  .page-hero { gap: 16px; padding: 18px; }.page-hero > div:first-child { min-width: 0; width: 100%; }.page-hero p, .support-tip { display: none; }.hero-stats > div { min-width: 0; flex: 1; padding: 3px 9px; }.list-heading { padding: 0 14px; }
  .dialog-heading { gap: 9px; padding-right: 28px; }.dialog-logo { width: 34px; height: 34px; border-radius: 9px; }.dialog-subtitle, .dialog-mode { display: none; }.dialog-title { font-size: 15px; white-space: nowrap; }
  .config-shell { height: calc(92vh - 135px); min-height: 0; display: block; overflow-y: auto; }.type-sidebar { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; padding: 12px; border-right: 0; border-bottom: 1px solid #e7e9f0; }.sidebar-label { grid-column: 1 / -1; }.type-option { min-width: 0; padding: 7px 6px; }.type-option > span { width: 25px; height: 25px; }.type-option small { display: none; }.connection-summary { display: none; }.config-content { overflow: visible; padding: 18px 13px; }.footer-tip { display: none; }
  .meta-body { height: calc(92vh - 120px); grid-template-columns: 1fr; overflow-y: auto; }.meta-tree-panel { max-height: 220px; border-right: 0; border-bottom: 1px solid #e7e9f0; }.meta-detail { min-height: 330px; overflow: visible; }
}
</style>

<style>
.datasource-config-dialog.el-dialog, .metadata-dialog.el-dialog { overflow: hidden; border-radius: 14px; }
.datasource-config-dialog .el-dialog__header, .metadata-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.datasource-config-dialog .el-dialog__body, .metadata-dialog .el-dialog__body { padding: 16px 20px; }
.datasource-config-dialog .el-dialog__footer, .metadata-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
.danger-menu-item { color: #dc4452 !important; }
@media (max-width: 680px) {
  .datasource-config-dialog .el-dialog__header, .metadata-dialog .el-dialog__header { padding: 14px 13px 11px; }
  .datasource-config-dialog .el-dialog__body, .metadata-dialog .el-dialog__body { padding: 10px; }
  .datasource-config-dialog .el-dialog__footer, .metadata-dialog .el-dialog__footer { padding: 10px 12px 12px; }
}
</style>
