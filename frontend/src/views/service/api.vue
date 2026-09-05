<template>
  <div class="api-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="page-eyebrow">DATA SERVICE</span>
        <h2>{{ t('api.title') }}</h2>
        <p>{{ t('api.subtitle') }}</p>
      </div>
      <div class="hero-stats">
        <div><strong>{{ page.total }}</strong><span>{{ t('api.total') }}</span></div>
        <div><strong>{{ publishedCount }}</strong><span>{{ t('api.published') }}</span></div>
        <div><strong>{{ enabledCount }}</strong><span>{{ t('api.enabled') }}</span></div>
      </div>
      <el-button type="primary" :icon="Plus" size="large" @click="openAdd">{{ t('api.create') }}</el-button>
    </section>

    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item>
            <el-input
              v-model="query.keyword"
              :placeholder="t('api.searchPlaceholder')"
              clearable
              :prefix-icon="Search"
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.status" :placeholder="t('api.status')" clearable style="width: 110px">
              <el-option :label="t('api.enable')" value="ENABLE" />
              <el-option :label="t('api.disable')" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="query.dataSourceCode"
              :placeholder="t('api.datasource')"
              clearable
              filterable
              style="width: 180px"
            >
              <el-option v-for="ds in jdbcDatasources" :key="ds.code" :label="ds.name" :value="ds.code" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="handleReset">{{ t('api.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div class="list-heading">
        <div><h3>{{ t('api.list') }}</h3><span>{{ t('api.listDesc') }}</span></div>
        <span class="support-tip">PREPARED SQL · SECRET · RATE LIMIT · LOG</span>
      </div>
      <el-table v-loading="loading" :data="list" class="api-table">
        <template #empty>
          <el-empty :description="t('api.empty')" :image-size="80" />
        </template>
        <el-table-column :label="t('api.title')" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="api-name-cell">
              <span class="api-logo">API</span>
              <div class="name-cell">
                <div class="name-text">{{ row.name }}</div>
                <div class="name-desc">{{ row.description || t('api.noDescription') }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('api.code')" min-width="180">
          <template #default="{ row }">
            <div class="code-cell">
              <span class="code-text" :title="row.code">{{ row.code }}</span>
              <el-icon class="code-copy" @click="copyText(row.code, t('api.codeCopied'))"><CopyDocument /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('api.datasource')" min-width="145" show-overflow-tooltip>
          <template #default="{ row }"><span class="datasource-pill">{{ row.dataSourceName }}</span></template>
        </el-table-column>
        <el-table-column :label="t('api.status')" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="{ enabled: row.status === 'ENABLE' }"><i></i>{{ row.status === 'ENABLE' ? t('api.enable') : t('api.disable') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('api.version')" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.publishVersion" class="version-badge">{{ row.publishVersion }}</span>
            <span v-else class="draft-badge">{{ t('api.draft') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" :label="t('api.updateTime')" width="170" />
        <el-table-column :label="t('api.actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="success" :icon="Promotion" @click="openPublish(row)">{{ t('common.publish') }}</el-button>
            <el-button link type="warning" :icon="VideoPlay" @click="openTest(row)">{{ t('api.test') }}</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button link type="info">
                {{ t('api.more') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="log"><el-icon><Document /></el-icon>{{ t('api.callLog') }}</el-dropdown-item>
                  <el-dropdown-item command="delete" divided><el-icon><Delete /></el-icon>{{ t('common.delete') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
    <el-dialog v-model="formVisible" width="min(1160px, calc(100vw - 40px))" top="3vh" append-to-body class="api-config-dialog" destroy-on-close :close-on-click-modal="false">
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-logo">API</span>
          <div><div class="dialog-title">{{ form.id ? t('api.editTitle') : t('api.createTitle') }}</div><div class="dialog-subtitle">{{ t('api.dialogSubtitle') }}</div></div>
          <span class="dialog-mode">{{ form.id ? 'EDIT SERVICE' : 'NEW SERVICE' }}</span>
        </div>
      </template>
      <div class="config-shell">
        <aside class="config-sidebar">
          <span class="sidebar-label">SERVICE BUILDER</span>
          <div class="step-card done"><span>01</span><div><strong>{{ t('api.basicInfo') }}</strong><small>{{ t('api.basicInfoDesc') }}</small></div></div>
          <div class="step-card" :class="{ done: !!form.template.trim() }"><span>02</span><div><strong>{{ t('api.queryLogic') }}</strong><small>{{ t('api.queryLogicDesc') }}</small></div></div>
          <div class="step-card" :class="{ done: templateParams.length > 0 }"><span>03</span><div><strong>{{ t('api.requestParams') }}</strong><small>{{ t('api.requestParamsDesc') }}</small></div></div>
          <div class="service-summary"><span>{{ t('api.configOverview') }}</span><strong>{{ form.name || t('api.unnamed') }}</strong><p>{{ selectedDatasource?.name || t('api.noDatasource') }}</p><div><i :class="{ ready: canDraftTest }"></i>{{ canDraftTest ? t('api.canDraftTest') : t('api.waitingConfig') }}</div></div>
        </aside>
        <main class="config-content">
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">01</span><div><h3>{{ t('api.basicInfo') }}</h3><p>{{ t('api.basicInfoSubtitle') }}</p></div></div></div>
            <div class="form-grid basic-grid">
              <div class="field"><label>{{ t('api.name') }} <em>*</em></label><el-input v-model="form.name" size="large" maxlength="200" show-word-limit :placeholder="t('api.namePlaceholder')" /></div>
              <div class="field"><label>{{ t('api.queryDatasource') }} <em>*</em></label><el-select v-model="form.dataSourceCode" size="large" :placeholder="t('api.selectDatasource')" filterable style="width:100%"><el-option v-for="ds in jdbcDatasources" :key="ds.code" :label="`${ds.name} (${ds.type})`" :value="ds.code" /></el-select></div>
              <div class="field wide-field"><label>{{ t('api.description') }}</label><el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" :placeholder="t('api.descriptionPlaceholder')" /></div>
            </div>
          </section>
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">02</span><div><h3>{{ t('api.sqlTemplate') }}</h3><p>{{ t('api.sqlTemplateDesc') }}</p></div></div><button type="button" class="format-action" @click="sqlEditorRef?.format()"><el-icon><MagicStick /></el-icon>{{ t('api.formatSql') }}</button></div>
            <div class="editor-panel">
          <div class="editor-toolbar">
            <span class="editor-lang">{{ editorDialect === 'postgresql' ? 'POSTGRESQL' : 'SQL' }}</span>
            <div class="editor-toolbar-actions">
              <span class="editor-toolbar-hint">{{ t('api.preparedHint') }}</span>
            </div>
          </div>
          <div class="editor-wrap">
            <SqlEditor ref="sqlEditorRef" v-model="form.template" :dialect="editorDialect" />
          </div>
        </div>
            <div class="sql-hint"><span>i</span><p>{{ t('api.sqlHint') }}</p></div>
          </section>
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">03</span><div><h3>{{ t('api.requestParams') }}</h3><p>{{ t('api.requestParamsSubtitle') }}</p></div></div><span class="param-counter">{{ templateParams.length }} PARAMETERS</span></div>
            <div v-if="parameterRows.length" class="parameter-table">
              <div class="parameter-head"><span>{{ t('api.paramName') }}</span><span>{{ t('api.paramType') }}</span><span>{{ t('api.testValue') }}</span><span>{{ t('api.paramDesc') }}</span></div>
              <div v-for="item in parameterRows" :key="item.name" class="parameter-row">
                <div class="parameter-name"><code v-text="'${' + item.name + '}'"></code><small>{{ t('api.required') }}</small></div>
                <el-select v-model="item.type"><el-option v-for="option in PARAM_TYPES" :key="option.value" :label="option.label" :value="option.value" /></el-select>
                <el-input v-model="item.value" :placeholder="parameterPlaceholder(item.type)" clearable />
                <el-input v-model="item.description" :placeholder="t('api.paramDescPlaceholder')" clearable />
              </div>
            </div>
            <div v-else class="parameter-empty"><span>{ }</span><div><strong>{{ t('api.noParams') }}</strong><p>{{ t('api.noParamsHint') }}</p></div></div>
          </section>
          <section class="form-section runtime-section">
            <div class="section-heading"><div><span class="section-index">04</span><div><h3>{{ t('api.runtime') }}</h3><p>{{ t('api.runtimeDesc') }}</p></div></div></div>
            <div class="runtime-grid"><div class="setting-card"><div><strong>{{ t('api.queryTimeout') }}</strong><small>{{ t('api.queryTimeoutDesc') }}</small></div><el-input-number v-model="form.timeout" :min="1" :max="3600" controls-position="right" /><span>{{ t('api.seconds') }}</span></div><div class="setting-card"><div><strong>{{ t('api.serviceStatus') }}</strong><small>{{ t('api.serviceStatusDesc') }}</small></div><el-switch v-model="form.status" active-value="ENABLE" inactive-value="DISABLE" :active-text="t('api.enable')" :inactive-text="t('api.disable')" /></div></div>
          </section>
        </main>
      </div>

      <template #footer>
        <div class="dialog-footer"><span class="footer-tip">{{ t('api.footerTip') }}</span><el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button><el-button :icon="VideoPlay" :loading="testingDraft" :disabled="!canDraftTest" @click="handleDraftTest">{{ t('api.testDraft') }}</el-button><el-button type="primary" :loading="saving" @click="handleSave">{{ form.id ? t('api.saveChanges') : t('api.create') }}</el-button></div>
      </template>
    </el-dialog>

    <!-- 发布 -->
    <el-dialog v-model="publishVisible" width="min(720px, calc(100vw - 40px))" top="4vh" append-to-body class="api-publish-dialog" destroy-on-close :close-on-click-modal="false">
      <template #header><div class="dialog-heading"><span class="dialog-logo publish-logo">PUB</span><div><div class="dialog-title">{{ t('api.publishTitle') }}</div><div class="dialog-subtitle">{{ t('api.publishSubtitle') }}</div></div><span class="dialog-mode">SECURITY POLICY</span></div></template>
      <div class="publish-target">
        <el-icon class="publish-target-icon"><Promotion /></el-icon>
        <div class="publish-target-info">
          <div class="publish-target-name">{{ publishTarget?.name }}</div>
          <div class="publish-target-code">{{ publishTarget?.code }}</div>
        </div>
      </div>

      <div class="section-title">{{ t('api.auth') }}</div>
      <div class="auth-options">
        <button v-for="item in AUTH_OPTIONS" :key="item.value" type="button" :class="{ active: publishForm.authType === item.value }" @click="publishForm.authType = item.value"><span>{{ item.icon }}</span><div><strong>{{ item.label }}</strong><small>{{ item.description }}</small></div><i>✓</i></button>
      </div>
      <div v-if="publishForm.authType !== 'PUBLIC'" class="secret-field">
        <div class="field-label"><span>{{ publishForm.authType === 'HMAC_SHA256' ? t('api.signSecret') : t('api.apiKey') }}</span><small>{{ publishForm.authType === 'HMAC_SHA256' ? t('api.aesStorage') : t('api.shaStorage') }}</small></div>
        <el-input v-model="publishForm.secret" maxlength="60" :placeholder="canKeepPublishedSecret ? t('api.secretKeepPlaceholder') : t('api.secretPlaceholder')" clearable show-password><template #append><el-button @click="generateSecret">{{ t('api.autoGenerate') }}</el-button></template></el-input>
        <div v-if="publishForm.authType === 'HMAC_SHA256'" class="security-hint">{{ t('api.hmacHint') }}</div>
      </div>

      <div class="section-title">{{ t('api.runtimeConfig') }}</div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">{{ t('api.cache') }}</div>
          <div class="switch-row-desc">{{ t('api.cacheDesc') }}</div>
        </div>
        <el-switch v-model="publishForm.cacheOn" />
      </div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">{{ t('api.limit') }}</div>
          <div class="switch-row-desc">{{ t('api.limitDesc') }}</div>
        </div>
        <el-switch v-model="publishForm.limitOn" />
      </div>
      <div v-if="publishForm.limitOn" class="limit-config">
        <div><label>{{ t('api.limitType') }}</label><el-segmented v-model="publishForm.limitType" :options="limitTypeOptions" block /></div>
        <div><label>{{ t('api.timeWindow') }}</label><div class="inline-control"><el-input-number v-model="publishForm.limitRefreshInterval" :min="1" :max="86400" controls-position="right" /><el-select v-model="publishForm.limitTimeUnit"><el-option :label="t('api.seconds')" value="SECONDS" /><el-option :label="t('api.minutes')" value="MINUTES" /><el-option :label="t('api.hours')" value="HOURS" /></el-select></div></div>
        <div><label>{{ t('api.maxRequests') }}</label><div class="inline-control"><el-input-number v-model="publishForm.limitRate" :min="1" :max="100000" controls-position="right" /><span>{{ t('api.times') }}</span></div></div>
        <p>{{ publishForm.limitType === 'IP' ? t('api.limitIpHint') : t('api.limitGlobalHint') }}</p>
      </div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">{{ t('api.log') }}</div>
          <div class="switch-row-desc">{{ t('api.logDesc') }}</div>
        </div>
        <el-switch v-model="publishForm.logOn" />
      </div>

      <template #footer>
        <el-button @click="publishVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">{{ t('common.publish') }}</el-button>
      </template>
    </el-dialog>

    <!-- 测试/预览 -->
    <el-dialog
      v-model="testVisible"
      width="min(980px, calc(100vw - 40px))"
      top="3vh"
      append-to-body
      class="api-test-dialog"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <template #header><div class="dialog-heading"><span class="dialog-logo test-logo">RUN</span><div><div class="dialog-title">{{ t('api.testTitle') }} · {{ testName }}</div><div class="dialog-subtitle">{{ t('api.testSubtitle') }}</div></div><span class="dialog-mode">{{ testCode }}</span></div></template>
      <div class="test-shell">
        <section class="test-params-panel">
          <div class="test-panel-head"><div><strong>{{ t('api.requestParams') }}</strong><span>{{ t('api.placeholderCount', { n: testParameterRows.length }) }}</span></div><el-switch v-model="advancedParams" active-text="JSON" /></div>
          <template v-if="!advancedParams">
            <div v-if="testParameterRows.length" class="test-param-list">
              <div v-for="item in testParameterRows" :key="item.name" class="test-param-item"><div class="test-param-label"><code>{{ item.name }}</code><span>{{ t('api.required') }}</span></div><div class="test-param-control"><el-select v-model="item.type" style="width:105px"><el-option v-for="option in PARAM_TYPES" :key="option.value" :label="option.label" :value="option.value" /></el-select><el-input v-model="item.value" :placeholder="parameterPlaceholder(item.type)" clearable /></div></div>
            </div>
            <div v-else class="parameter-empty compact"><span>{ }</span><div><strong>{{ t('api.noParamsNeeded') }}</strong><p>{{ t('api.noParamsNeededHint') }}</p></div></div>
          </template>
          <div v-else><el-input v-model="testParamsJson" type="textarea" :rows="8" class="test-params-input" placeholder='{"id": 1}' /><div class="json-hint">{{ t('api.jsonHint') }}</div></div>
          <div class="test-actions"><el-button type="primary" :icon="CaretRight" :loading="testing" @click="handleTest">{{ t('api.runQuery') }}</el-button><el-button @click="resetTestParams">{{ t('api.resetParams') }}</el-button></div>
        </section>
        <section class="test-result-panel">
          <div class="test-panel-head"><div><strong>{{ t('api.runResult') }}</strong><span v-if="testResult">{{ t('api.resultMeta', { rows: testResult.rowCount, ms: testResult.durationMs }) }}</span><span v-else>{{ t('api.notExecuted') }}</span></div><el-tag v-if="testResult?.truncated" type="warning" effect="light" size="small">{{ t('api.truncated') }}</el-tag></div>
          <el-table v-if="testResult?.columns?.length" :data="testResult.rows" border size="small" max-height="300" class="result-table"><el-table-column v-for="col in testResult.columns" :key="col" :prop="col" :label="col" min-width="120" show-overflow-tooltip /></el-table>
          <el-empty v-else :description="testResult ? t('api.noResult') : t('api.noResultHint')" :image-size="62" />
        </section>
      </div>
      <div class="curl-block">
        <div class="curl-head"><div><strong>{{ t('api.curlExample') }}</strong><span>{{ t('api.curlExampleDesc') }}</span></div><div class="curl-head-right"><el-select v-model="testMethod" size="small" style="width:105px"><el-option :label="t('api.methodOne')" value="one" /><el-option :label="t('api.methodCount')" value="count" /><el-option :label="t('api.methodList')" value="list" /><el-option :label="t('api.methodPage')" value="page" /></el-select><template v-if="testMethod === 'page'"><el-input-number v-model="testPageNum" size="small" :min="1" :max="100000" controls-position="right" style="width:95px" /><el-input-number v-model="testPageSize" size="small" :min="1" :max="1000" controls-position="right" style="width:95px" /></template><el-button link type="primary" size="small" @click="copyCurl">{{ t('api.copyCurl') }}</el-button></div></div>
        <div v-if="testAuthType !== 'PUBLIC'" class="test-credential"><span>{{ testAuthType === 'HMAC_SHA256' ? t('api.hmacSecret') : t('api.apiKey') }}</span><el-input v-model="testSecret" type="password" show-password :placeholder="t('api.testSecretPlaceholder')" /></div>
        <pre class="curl-code">{{ curlText }}</pre>
      </div>
    </el-dialog>

    <!-- 日志列表 -->
    <el-dialog v-model="logVisible" :title="`${t('api.logTitle')} - ${logName}`" width="960px" top="5vh" destroy-on-close>
      <el-table v-loading="logLoading" :data="logList" border size="small">
        <template #empty>
          <el-empty :description="t('api.noLog')" :image-size="70" />
        </template>
        <el-table-column prop="id" label="ID" width="64" align="center" />
        <el-table-column :label="t('api.method')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="methodType(row.method)" effect="light" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('api.status')" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.status === 'SUCCESS' ? t('api.success') : t('api.fail') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="number" :label="t('api.count')" width="72" align="center" />
        <el-table-column prop="cost" :label="t('api.cost')" width="90" align="center">
          <template #default="{ row }">{{ row.cost }} ms</template>
        </el-table-column>
        <el-table-column :label="t('api.cache')" width="72" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hitCache === 'YES'" type="warning" effect="light" size="small">{{ t('api.hit') }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="t('api.time')" width="170" />
        <el-table-column :label="t('api.actions')" width="70" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLogDetail(row)">{{ t('api.detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="logPage.current"
        v-model:page-size="logPage.size"
        class="pager"
        layout="total, prev, pager, next"
        :total="logPage.total"
        @change="loadLogs"
      />
    </el-dialog>

    <!-- 日志详情 -->
    <el-dialog v-model="logDetailVisible" :title="`${t('api.logDetailTitle')} #${logDetail?.id ?? ''}`" width="760px" top="5vh" destroy-on-close>
      <template v-if="logDetail">
        <el-descriptions :column="2" border size="small" class="log-desc">
          <el-descriptions-item :label="t('api.status')">
            <el-tag :type="logDetail.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ logDetail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('api.method')">{{ logDetail.method }}</el-descriptions-item>
          <el-descriptions-item :label="t('api.cost')">{{ logDetail.cost }} ms</el-descriptions-item>
          <el-descriptions-item :label="t('api.count')">{{ logDetail.number }}</el-descriptions-item>
          <el-descriptions-item :label="t('api.cache')">
            {{ logDetail.hitCache === 'YES' ? t('api.hit') : t('api.miss') }}
          </el-descriptions-item>
          <el-descriptions-item label="IP">{{ logDetail.ip }}</el-descriptions-item>
        </el-descriptions>
        <div class="log-block-title">{{ t('api.requestParams') }}</div>
        <pre class="log-block">{{ prettyJson(logDetail.requestArg) }}</pre>
        <div class="log-block-title">{{ t('api.responseParams') }}</div>
        <pre class="log-block">{{ prettyJson(logDetail.responseArg) }}</pre>
        <el-alert v-if="logDetail.exception" :title="logDetail.exception" type="error" :closable="false" show-icon />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  CaretRight,
  ArrowDown,
  Refresh,
  Edit,
  Promotion,
  VideoPlay,
  CopyDocument,
  Document,
  Delete,
  MagicStick
} from '@element-plus/icons-vue'
import SqlEditor from '../../components/SqlEditor.vue'
import { listDataSource, type DataSourceItem } from '../../api/datasource'
import {
  listTemplate,
  getTemplateDetail,
  addTemplate,
  updateTemplate,
  deleteTemplate,
  publishTemplate,
  testTemplate,
  listLog,
  getLogDetail,
  type QueryTemplateItem,
  type QueryTemplateForm,
  type QueryLogItem,
  type QueryLogDetail,
  type QueryExecuteResult
} from '../../api/service'

const { t } = useI18n()
const JDBC_TYPES = ['Doris', 'MySQL', 'TiDB', 'PostgreSQL']
type ParameterType = 'string' | 'number' | 'boolean' | 'null'
type AuthType = 'PUBLIC' | 'API_KEY' | 'HMAC_SHA256'
type LimitType = 'GLOBAL' | 'IP'
interface ParameterRow { name: string; type: ParameterType; value: string; description: string }
const PARAM_TYPES = computed<Array<{ label: string; value: ParameterType }>>(() => [
  { label: t('api.paramString'), value: 'string' },
  { label: t('api.paramNumber'), value: 'number' },
  { label: t('api.paramBoolean'), value: 'boolean' },
  { label: t('api.paramNull'), value: 'null' }
])
const AUTH_OPTIONS = computed<Array<{ value: AuthType; label: string; description: string; icon: string }>>(() => [
  { value: 'PUBLIC', label: t('api.authPublic'), description: t('api.authPublicDesc'), icon: 'OPEN' },
  { value: 'API_KEY', label: t('api.authApiKey'), description: t('api.authApiKeyDesc'), icon: 'KEY' },
  { value: 'HMAC_SHA256', label: t('api.authHmac'), description: t('api.authHmacDesc'), icon: 'SIGN' }
])
const limitTypeOptions = computed(() => [{ label: t('api.limitGlobal'), value: 'GLOBAL' }, { label: t('api.limitIp'), value: 'IP' }])

const loading = ref(false)
const list = ref<QueryTemplateItem[]>([])
const query = reactive({ keyword: '', status: '', dataSourceCode: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const publishedCount = computed(() => list.value.filter((item) => !!item.publishVersion).length)
const enabledCount = computed(() => list.value.filter((item) => item.status === 'ENABLE').length)

const allDatasources = ref<DataSourceItem[]>([])
const jdbcDatasources = computed(() =>
  allDatasources.value
    .filter((d) => JDBC_TYPES.includes(d.type))
    .sort((a, b) => (a.type === 'Doris' ? -1 : b.type === 'Doris' ? 1 : 0))
)

const formVisible = ref(false)
const saving = ref(false)
const testingDraft = ref(false)
const form = reactive<QueryTemplateForm>({ name: '', dataSourceCode: '', template: '', timeout: 30, status: 'ENABLE', description: '' })

const templateParams = computed(() => {
  const matches = form.template.match(/\$\{([a-zA-Z0-9_]+)\}/g) ?? []
  return [...new Set(matches.map((m) => m.slice(2, -1)))]
})
const parameterRows = ref<ParameterRow[]>([])
const selectedDatasource = computed(() => jdbcDatasources.value.find((item) => item.code === form.dataSourceCode))
const canDraftTest = computed(() => !!form.dataSourceCode && !!form.template.trim() && isReadOnlySql(form.template))

watch(templateParams, (names) => {
  const previous = new Map(parameterRows.value.map((item) => [item.name, item]))
  parameterRows.value = names.map((name) => previous.get(name) ?? { name, type: 'string', value: '', description: '' })
}, { immediate: true })

const sqlEditorRef = ref<{ format: () => void } | null>(null)

const editorDialect = computed<'mysql' | 'postgresql'>(() => {
  const ds = allDatasources.value.find((d) => d.code === form.dataSourceCode)
  return (ds?.type ?? '').toLowerCase() === 'postgresql' ? 'postgresql' : 'mysql'
})

const publishVisible = ref(false)
const publishing = ref(false)
const publishTarget = ref<QueryTemplateItem | null>(null)
const publishedAuthType = ref<AuthType>('PUBLIC')
const hasPublishedSecret = ref(false)
const publishForm = reactive({
  authType: 'API_KEY' as AuthType,
  secret: '',
  cacheOn: false,
  limitOn: false,
  limitRate: 10,
  limitRefreshInterval: 1,
  limitTimeUnit: 'SECONDS',
  limitType: 'GLOBAL' as LimitType,
  logOn: true
})
const canKeepPublishedSecret = computed(() => hasPublishedSecret.value && publishForm.authType === publishedAuthType.value)

const testVisible = ref(false)
const testing = ref(false)
const testId = ref(0)
const testName = ref('')
const testCode = ref('')
const testSecret = ref('')
const testAuthType = ref<AuthType>('PUBLIC')
const testParamsJson = ref('{}')
const testParameterRows = ref<ParameterRow[]>([])
const advancedParams = ref(false)
const testMethod = ref('list')
const testPageNum = ref(1)
const testPageSize = ref(20)
const testResult = ref<QueryExecuteResult | null>(null)

const logVisible = ref(false)
const logLoading = ref(false)
const logName = ref('')
const logTemplateCode = ref('')
const logList = ref<QueryLogItem[]>([])
const logPage = reactive({ current: 1, size: 10, total: 0 })

const logDetailVisible = ref(false)
const logDetail = ref<QueryLogDetail | null>(null)

const curlText = computed(() => {
  const origin = window.location.origin
  const body: Record<string, unknown> = { method: testMethod.value, params: currentTestParams(false) ?? {} }
  if (testMethod.value === 'page') {
    body.pageNum = testPageNum.value
    body.pageSize = testPageSize.value
  }
  const bodyJson = JSON.stringify(body)
  const credential = testSecret.value || (testAuthType.value === 'HMAC_SHA256' ? '<YOUR_HMAC_SECRET>' : '<YOUR_API_KEY>')
  const baseCurl = `curl -X POST '${origin}/dp-web/open/api/${testCode.value}' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    (testAuthType.value === 'API_KEY' ? `  -H ${shellSingleQuote(`X-Secret: ${credential}`)} \\\n` : '')
  if (testAuthType.value !== 'HMAC_SHA256') return baseCurl + `  -d ${shellSingleQuote(bodyJson)}`
  return `BODY=${shellSingleQuote(bodyJson)}
TIMESTAMP=$(date +%s)
NONCE=$(uuidgen | tr -d '-')
BODY_SHA=$(printf '%s' "$BODY" | shasum -a 256 | awk '{print $1}')
SIGNATURE=$(printf '%s\\n%s\\n%s' "$TIMESTAMP" "$NONCE" "$BODY_SHA" | openssl dgst -sha256 -hmac ${shellSingleQuote(credential)} | awk '{print $2}')

${baseCurl}  -H "X-Timestamp: $TIMESTAMP" \\\n+  -H "X-Nonce: $NONCE" \\\n+  -H "X-Signature: $SIGNATURE" \\\n+  -d "$BODY"`
})

function shellSingleQuote(value: string) {
  return `'${value.replace(/'/g, `'"'"'`)}'`
}

function isReadOnlySql(sql: string) {
  return /^(select|with)\b/i.test(sql.trim()) && !/;\s*\S/.test(sql.trim())
}

function parameterPlaceholder(type: ParameterType) {
  return type === 'number' ? t('api.phNumber') : type === 'boolean' ? t('api.phBoolean') : type === 'null' ? t('api.phNull') : t('api.phString')
}

function convertParameter(item: ParameterRow): unknown {
  if (item.type === 'null') return null
  if (item.type === 'number') {
    if (!item.value.trim() || Number.isNaN(Number(item.value))) throw new Error(t('api.paramNumberInvalid', { name: item.name }))
    return Number(item.value)
  }
  if (item.type === 'boolean') {
    if (!['true', 'false'].includes(item.value.trim().toLowerCase())) throw new Error(t('api.paramBooleanInvalid', { name: item.name }))
    return item.value.trim().toLowerCase() === 'true'
  }
  if (!item.value.trim()) throw new Error(t('api.paramRequired', { name: item.name }))
  return item.value
}

function rowsToParams(rows: ParameterRow[], notify = true): Record<string, unknown> | null {
  try {
    return Object.fromEntries(rows.map((item) => [item.name, convertParameter(item)]))
  } catch (error) {
    if (notify) ElMessage.warning(error instanceof Error ? error.message : t('api.paramFormatError'))
    return null
  }
}

function currentTestParams(notify = true): Record<string, unknown> | null {
  if (!advancedParams.value) return rowsToParams(testParameterRows.value, notify)
  try {
    const value = JSON.parse(testParamsJson.value || '{}')
    if (!value || Array.isArray(value) || typeof value !== 'object') throw new Error(t('api.jsonObjectRequired'))
    return value
  } catch (error) {
    if (notify) ElMessage.warning(error instanceof Error ? error.message : t('api.jsonFormatError'))
    return null
  }
}

function methodType(method: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    one: 'primary',
    count: 'success',
    list: 'info',
    page: 'warning'
  }
  return map[method] ?? 'info'
}

function prettyJson(raw?: string): string {
  if (!raw) return t('api.none')
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function copyText(text: string, tip = t('api.copied')) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(tip)
  } catch {
    ElMessage.warning(t('api.copyFailed'))
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listTemplate(
      {
        keyword: query.keyword || undefined,
        status: query.status || undefined,
        dataSourceCode: query.dataSourceCode || undefined
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

function handleReset() {
  query.keyword = ''
  query.status = ''
  query.dataSourceCode = ''
  page.current = 1
  load()
}

async function loadDatasources() {
  const res = await listDataSource({}, 1, 500)
  allDatasources.value = res.records
}

function openAdd() {
  form.id = undefined
  form.name = ''
  form.dataSourceCode = jdbcDatasources.value[0]?.code ?? ''
  form.template = ''
  form.timeout = 30
  form.status = 'ENABLE'
  form.description = ''
  parameterRows.value = []
  formVisible.value = true
}

async function openEdit(row: QueryTemplateItem) {
  const d = await getTemplateDetail(row.id)
  form.id = row.id
  form.name = row.name
  form.dataSourceCode = row.dataSourceCode
  form.template = d?.template ?? ''
  form.timeout = row.timeout
  form.status = row.status
  form.description = row.description
  parameterRows.value = []
  formVisible.value = true
}

async function handleDraftTest() {
  if (!canDraftTest.value) {
    ElMessage.warning(t('api.draftTestHint'))
    return
  }
  const params = rowsToParams(parameterRows.value)
  if (params === null) return
  testingDraft.value = true
  try {
    const result = await testTemplate({ dataSourceCode: form.dataSourceCode, template: form.template, params })
    ElMessage.success(t('api.draftTestSuccess', { rows: result.rowCount, ms: result.durationMs }))
  } finally {
    testingDraft.value = false
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('api.nameRequired'))
    return
  }
  if (!form.dataSourceCode) {
    ElMessage.warning(t('api.datasourceRequired'))
    return
  }
  if (!form.template.trim()) {
    ElMessage.warning(t('api.templateRequired'))
    return
  }
  if (!isReadOnlySql(form.template)) {
    ElMessage.warning(t('api.templateReadOnly'))
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      dataSourceCode: form.dataSourceCode,
      template: form.template,
      timeout: form.timeout,
      status: form.status,
      description: form.description || undefined
    }
    if (form.id) {
      await updateTemplate({ ...payload, id: form.id })
      ElMessage.success(t('api.updateSuccess'))
    } else {
      await addTemplate(payload)
      ElMessage.success(t('api.saveSuccess'))
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openPublish(row: QueryTemplateItem) {
  publishTarget.value = row
  try {
    const detail = await getTemplateDetail(row.id)
    publishForm.secret = ''
    publishForm.authType = detail?.authType ?? (detail?.secret ? 'API_KEY' : 'PUBLIC')
    publishedAuthType.value = publishForm.authType
    hasPublishedSecret.value = detail?.hasSecret ?? !!detail?.secret
  } catch {
    publishForm.secret = ''
    publishForm.authType = 'API_KEY'
    publishedAuthType.value = 'PUBLIC'
    hasPublishedSecret.value = false
  }
  publishForm.cacheOn = false
  publishForm.limitOn = false
  publishForm.limitRate = 10
  publishForm.limitRefreshInterval = 1
  publishForm.limitTimeUnit = 'SECONDS'
  publishForm.limitType = 'GLOBAL'
  publishForm.logOn = true
  publishVisible.value = true
}

function generateSecret() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
  const length = publishForm.authType === 'HMAC_SHA256' ? 48 : 32
  const random = new Uint32Array(length)
  crypto.getRandomValues(random)
  publishForm.secret = Array.from(random, (item) => alphabet[item % alphabet.length]).join('')
  ElMessage.success(t('api.secretGenerated'))
}

async function handlePublish() {
  if (!publishTarget.value) return
  if (publishForm.authType !== 'PUBLIC' && !publishForm.secret.trim() && !canKeepPublishedSecret.value) {
    ElMessage.warning(t('api.secretRequired'))
    return
  }
  if (publishForm.secret && new TextEncoder().encode(publishForm.secret).length < 16) {
    ElMessage.warning(t('api.secretTooShort'))
    return
  }
  publishing.value = true
  try {
    const res = await publishTemplate({
      id: publishTarget.value.id,
      authType: publishForm.authType,
      secret: publishForm.authType === 'PUBLIC' ? undefined : publishForm.secret,
      enableCache: publishForm.cacheOn ? 'ENABLE' : 'DISABLE',
      enableLimiting: publishForm.limitOn ? 'ENABLE' : 'DISABLE',
      limitType: publishForm.limitType,
      limitRate: publishForm.limitOn ? publishForm.limitRate : undefined,
      limitRefreshInterval: publishForm.limitOn ? publishForm.limitRefreshInterval : undefined,
      limitTimeUnit: publishForm.limitOn ? publishForm.limitTimeUnit : undefined,
      recordLog: publishForm.logOn ? 'ENABLE' : 'DISABLE'
    })
    ElMessage.success(t('api.publishSuccess', { version: res.version }))
    publishVisible.value = false
    load()
  } finally {
    publishing.value = false
  }
}

async function openTest(row: QueryTemplateItem) {
  let secret = ''
  let authType: AuthType = 'PUBLIC'
  let template = ''
  try {
    const d = await getTemplateDetail(row.id)
    secret = ''
    authType = d?.authType ?? (d?.secret ? 'API_KEY' : 'PUBLIC')
    template = d?.template ?? ''
  } catch {
    /* ignore */
  }
  testId.value = row.id
  testName.value = row.name
  testCode.value = row.code
  testSecret.value = secret
  testAuthType.value = authType
  const names = [...new Set((template.match(/\$\{([a-zA-Z0-9_]+)\}/g) ?? []).map((item) => item.slice(2, -1)))]
  testParameterRows.value = names.map((name) => ({ name, type: 'string', value: '', description: '' }))
  testParamsJson.value = JSON.stringify(Object.fromEntries(names.map((name) => [name, ''])), null, 2)
  advancedParams.value = false
  testMethod.value = 'list'
  testPageNum.value = 1
  testPageSize.value = 20
  testResult.value = null
  testVisible.value = true
}

async function handleTest() {
  const params = currentTestParams()
  if (params === null) return
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testTemplate({ id: testId.value, params })
  } finally {
    testing.value = false
  }
}

function resetTestParams() {
  testParameterRows.value.forEach((item) => { item.type = 'string'; item.value = '' })
  testParamsJson.value = JSON.stringify(Object.fromEntries(testParameterRows.value.map((item) => [item.name, ''])), null, 2)
  testResult.value = null
}

async function copyCurl() {
  await copyText(curlText.value, t('api.curlCopied'))
}

async function handleDelete(row: QueryTemplateItem) {
  await ElMessageBox.confirm(t('api.deleteConfirm', { name: row.name }), t('dataflow.prompt'), {
    type: 'warning',
    confirmButtonText: t('common.delete'),
    cancelButtonText: t('common.cancel')
  })
  await deleteTemplate(row.id)
  ElMessage.success(t('api.deleteSuccess'))
  load()
}

function handleCommand(cmd: string, row: QueryTemplateItem) {
  if (cmd === 'log') openLog(row)
  else if (cmd === 'delete') handleDelete(row)
}

function openLog(row: QueryTemplateItem) {
  logName.value = row.name
  logTemplateCode.value = row.code
  logPage.current = 1
  logVisible.value = true
  loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await listLog({ templateCode: logTemplateCode.value || undefined }, logPage.current, logPage.size)
    logList.value = res.records
    logPage.total = res.total
  } finally {
    logLoading.value = false
  }
}

async function openLogDetail(row: QueryLogItem) {
  logDetailVisible.value = true
  logDetail.value = null
  logDetail.value = await getLogDetail(row.id)
}

onMounted(async () => {
  await loadDatasources()
  load()
})
</script>

<style scoped>
.api-page {
  --radius: 8px;
}

.page-card {
  border: none;
  border-radius: var(--radius);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  font-size: 12px;
  color: #909399;
}

.filter-bar {
  margin-bottom: 16px;
}

.filter-bar :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 0;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 表格单元格 */
.name-cell {
  line-height: 1.4;
}

.name-text {
  color: #303133;
  font-weight: 500;
}

.name-desc {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.code-text {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-copy {
  flex-shrink: 0;
  cursor: pointer;
  color: #c0c4cc;
  font-size: 14px;
  transition: color 0.2s;
}

.code-copy:hover {
  color: var(--el-color-primary);
}

.muted {
  color: #c0c4cc;
}

/* 对话框通用 */
.form-dialog :deep(.el-dialog__body),
.test-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-header-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.dialog-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dialog-header-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.form-dialog .section-title:first-child {
  margin-top: 0;
}

.req {
  color: #f56c6c;
  margin-left: 4px;
}

/* 编辑器 */
.editor-panel {
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #252526;
  border-bottom: 1px solid #3c3c3c;
}

.editor-lang {
  font-size: 12px;
  font-weight: 600;
  color: #c9c3fa;
  letter-spacing: 0.6px;
}

.editor-toolbar-hint {
  font-size: 12px;
  color: #9a9a9a;
}

.editor-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-format-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #c9c3fa;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.editor-format-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.editor-wrap {
  width: 100%;
  height: 260px;
}

.param-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.param-chips-label {
  font-size: 12px;
  color: #909399;
}

/* 发布对话框 */
.publish-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 6px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-8);
  border-radius: 6px;
}

.publish-target-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.publish-target-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.publish-target-code {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.section-title {
  position: relative;
  margin: 18px 0 12px;
  padding-left: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--el-color-primary);
}

.field-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.switch-row + .switch-row {
  border-top: 1px solid #f0f1f5;
}

.switch-row-title {
  font-size: 14px;
  color: #303133;
}

.switch-row-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.limit-config {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-bottom: 4px;
  background: #f7f8fa;
  border-radius: 6px;
}

.limit-config span {
  color: #606266;
  font-size: 13px;
}

.limit-config :deep(.el-input-number) {
  width: 96px;
}

.limit-config :deep(.el-select) {
  width: 96px;
}

/* 测试对话框 */
.test-field-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.test-params-input :deep(textarea) {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.test-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.test-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #909399;
}

.result-table {
  width: 100%;
}

.curl-block {
  margin-top: 16px;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  overflow: hidden;
}

.curl-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f6fa;
  border-bottom: 1px solid #eef0f4;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.curl-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.curl-code {
  margin: 0;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background: #1e1e1e;
  color: #d4d4d4;
  overflow: auto;
}

/* 日志详情 */
.log-desc {
  margin-bottom: 6px;
}

.log-block-title {
  margin: 14px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.log-block {
  background: #f5f6fa;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0 0 6px;
  max-height: 220px;
  overflow: auto;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
}

/* API service refresh */
.api-page { display: grid; gap: 14px; }
.page-hero { min-height: 116px; display: flex; align-items: center; gap: 28px; padding: 22px 25px; border: 1px solid #e7e9f1; border-radius: 12px; background: linear-gradient(135deg, #fbfbff, #f5f7fc); }
.hero-copy { min-width: 280px; flex: 1; }.page-eyebrow { color: #7669ef; font-size: 9px; font-weight: 800; letter-spacing: 1.7px; }.page-hero h2 { margin: 5px 0 6px; color: #20283a; font-size: 22px; }.page-hero p { margin: 0; color: #8992a3; font-size: 11px; }
.hero-stats { display: flex; align-items: center; }.hero-stats > div { min-width: 92px; padding: 3px 20px; border-left: 1px solid #e0e3ea; text-align: center; }.hero-stats strong, .hero-stats span { display: block; }.hero-stats strong { color: #30394c; font-size: 21px; }.hero-stats span { margin-top: 3px; color: #9aa2b1; font-size: 9px; }
.filter-card :deep(.el-card__body) { padding: 16px 18px 0; }.filter-bar { margin: 0; }.filter-bar :deep(.el-form-item) { margin: 0 12px 16px 0; }
.list-card :deep(.el-card__body) { padding: 0; }.list-heading { height: 67px; display: flex; align-items: center; justify-content: space-between; padding: 0 19px; border-bottom: 1px solid #edf0f4; }.list-heading h3 { display: inline; margin: 0; color: #2c3548; font-size: 14px; }.list-heading > div > span { margin-left: 10px; color: #9aa2b1; font-size: 10px; }.support-tip { padding: 5px 9px; border-radius: 5px; color: #6e7790; background: #f4f5f8; font-size: 8px; letter-spacing: .3px; }
.api-name-cell { display: flex; align-items: center; gap: 11px; min-width: 0; }.api-logo, .dialog-logo { width: 36px; height: 36px; display: grid; place-items: center; flex-shrink: 0; border-radius: 10px; color: #fff; background: linear-gradient(145deg, #6c5ce7, #8b76f6); font-size: 8px; font-weight: 800; box-shadow: 0 7px 16px rgba(108, 92, 231, .2); }.name-cell { min-width: 0; }.name-text { color: #344054; font-size: 11px; font-weight: 650; }.name-desc { margin-top: 4px; color: #a0a7b5; font-size: 8px; }.code-text { color: #626d80; font-size: 10px; }.datasource-pill { padding: 4px 7px; border-radius: 5px; color: #326a94; background: #edf6fb; font-size: 9px; }.status-badge { display: inline-flex; align-items: center; gap: 6px; color: #929aaa; font-size: 9px; }.status-badge i { width: 6px; height: 6px; border-radius: 50%; background: #b9bec8; }.status-badge.enabled { color: #079669; }.status-badge.enabled i { background: #10b981; box-shadow: 0 0 0 3px rgba(16,185,129,.1); }.version-badge, .draft-badge { display: inline-flex; padding: 4px 8px; border-radius: 999px; font-size: 8px; font-weight: 700; }.version-badge { color: #6758e8; background: #f0eeff; }.draft-badge { color: #8a93a4; background: #f1f2f5; }.pager { margin: 0; justify-content: flex-end; padding: 16px 18px; border-top: 1px solid #edf0f4; }
.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }.dialog-logo { width: 40px; height: 40px; border-radius: 11px; }.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }.dialog-subtitle { margin-top: 3px; color: #8a94a6; font-size: 12px; }.dialog-mode { max-width: 260px; overflow: hidden; margin-left: auto; padding: 5px 9px; border: 1px solid #dedaff; border-radius: 999px; color: #6758e8; background: #f0efff; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.config-shell { height: min(680px, calc(94vh - 155px)); min-height: 520px; display: grid; grid-template-columns: 220px minmax(0, 1fr); overflow: hidden; border: 1px solid #e7e9f0; border-radius: 12px; background: #fafbfc; }.config-sidebar { display: flex; flex-direction: column; padding: 19px 14px; border-right: 1px solid #e7e9f0; background: #fbfbfd; }.sidebar-label { margin: 0 8px 10px; color: #9aa2b1; font-size: 9px; font-weight: 700; letter-spacing: .9px; }.step-card { display: flex; align-items: center; gap: 10px; padding: 11px 9px; border-radius: 8px; color: #7a8497; }.step-card + .step-card { margin-top: 3px; }.step-card > span { width: 29px; height: 29px; display: grid; place-items: center; flex-shrink: 0; border-radius: 8px; color: #9aa2b1; background: #eef0f4; font-size: 8px; font-weight: 800; }.step-card strong, .step-card small { display: block; }.step-card strong { color: #536075; font-size: 10px; }.step-card small { margin-top: 3px; color: #a0a7b5; font-size: 8px; }.step-card.done { background: #f2f0ff; }.step-card.done > span { color: #fff; background: linear-gradient(145deg,#6c5ce7,#8b76f6); }.step-card.done strong { color: #4c42a8; }
.service-summary { margin-top: auto; padding: 13px; border: 1px solid #e6e8ef; border-radius: 9px; background: #fff; }.service-summary > span { color: #9aa2b1; font-size: 8px; letter-spacing: .8px; }.service-summary > strong { display: block; overflow: hidden; margin-top: 6px; color: #364154; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.service-summary p { margin: 5px 0 10px; color: #8e97a7; font-size: 8px; }.service-summary div { display: flex; align-items: center; gap: 6px; color: #9aa2b1; font-size: 8px; }.service-summary div i { width: 6px; height: 6px; border-radius: 50%; background: #c0c5cf; }.service-summary div i.ready { background: #10b981; }
.config-content { min-width: 0; overflow-y: auto; padding: 24px 27px; background: #fff; }.form-section + .form-section { margin-top: 25px; padding-top: 23px; border-top: 1px solid #eceef3; }.section-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }.section-heading > div { display: flex; align-items: flex-start; gap: 10px; }.section-index { width: 25px; height: 25px; display: grid; place-items: center; flex-shrink: 0; border-radius: 7px; color: #635bff; background: #eeecff; font-size: 8px; font-weight: 800; }.section-heading h3 { margin: 0; color: #293247; font-size: 13px; }.section-heading p { margin: 4px 0 0; color: #929aaa; font-size: 9px; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 16px; }.field { min-width: 0; }.field label { display: block; margin-bottom: 7px; color: #4f596c; font-size: 10px; font-weight: 650; }.field label em { color: #ef5361; font-style: normal; }.wide-field { grid-column: 1 / -1; }.format-action { display: inline-flex; align-items: center; gap: 5px; padding: 6px 9px; border: 1px solid #dfe2e9; border-radius: 6px; color: #667085; background: #fff; font-size: 9px; cursor: pointer; }.format-action:hover { color: #635bff; border-color: #bdb7f8; }
.editor-panel { border: 1px solid #343542; border-radius: 9px; box-shadow: none; }.editor-toolbar { padding: 8px 12px; background: #252630; }.editor-lang { font-size: 9px; }.editor-toolbar-hint { font-size: 9px; }.editor-wrap { height: 245px; }.sql-hint { display: flex; align-items: flex-start; gap: 9px; margin-top: 10px; padding: 10px 12px; border-radius: 7px; color: #667085; background: #f5f7fb; }.sql-hint span { width: 17px; height: 17px; display: grid; place-items: center; flex-shrink: 0; border-radius: 50%; color: #fff; background: #7b72e8; font-size: 9px; font-weight: 700; }.sql-hint p { margin: 1px 0 0; font-size: 9px; line-height: 1.55; }.param-counter { padding: 4px 7px; border-radius: 5px; color: #6758e8; background: #f0efff; font-size: 8px; font-weight: 700; }
.parameter-table { overflow: hidden; border: 1px solid #e5e8ef; border-radius: 9px; }.parameter-head, .parameter-row { display: grid; grid-template-columns: 1.1fr .75fr 1.2fr 1.3fr; align-items: center; gap: 10px; padding: 9px 12px; }.parameter-head { color: #8992a3; background: #f7f8fa; font-size: 8px; font-weight: 700; }.parameter-row + .parameter-row { border-top: 1px solid #edf0f4; }.parameter-name { min-width: 0; }.parameter-name code { display: block; overflow: hidden; color: #635bff; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.parameter-name small { display: block; margin-top: 3px; color: #eb5967; font-size: 8px; }.parameter-empty { display: flex; align-items: center; gap: 13px; padding: 18px; border: 1px dashed #dfe3ea; border-radius: 9px; background: #fafbfc; }.parameter-empty > span { width: 37px; height: 37px; display: grid; place-items: center; border-radius: 9px; color: #7469df; background: #eeecff; font-family: monospace; font-size: 12px; }.parameter-empty strong, .parameter-empty p { display: block; }.parameter-empty strong { color: #566176; font-size: 10px; }.parameter-empty p { margin: 4px 0 0; color: #9aa2b1; font-size: 8px; }.runtime-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.setting-card { display: flex; align-items: center; gap: 9px; padding: 13px 14px; border: 1px solid #e8eaf0; border-radius: 8px; background: #fafbfc; }.setting-card > div { min-width: 0; flex: 1; }.setting-card strong, .setting-card small { display: block; }.setting-card strong { color: #4b5568; font-size: 10px; }.setting-card small { margin-top: 4px; color: #969ead; font-size: 8px; }.setting-card > span { color: #8f98a8; font-size: 9px; }.dialog-footer { width: 100%; display: flex; align-items: center; }.footer-tip { margin-right: auto; color: #8f98a8; font-size: 9px; }
.test-shell { display: grid; grid-template-columns: 320px minmax(0,1fr); overflow: hidden; min-height: 350px; border: 1px solid #e5e8ef; border-radius: 10px; }.test-params-panel, .test-result-panel { min-width: 0; padding: 16px; }.test-params-panel { border-right: 1px solid #e5e8ef; background: #fafbfc; }.test-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }.test-panel-head > div strong, .test-panel-head > div span { display: block; }.test-panel-head strong { color: #3c4659; font-size: 11px; }.test-panel-head > div span { margin-top: 3px; color: #9aa2b1; font-size: 8px; }.test-param-list { display: grid; gap: 12px; max-height: 245px; overflow-y: auto; padding-right: 3px; }.test-param-item { padding: 10px; border: 1px solid #e5e8ef; border-radius: 8px; background: #fff; }.test-param-label { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }.test-param-label code { color: #635bff; font-size: 10px; font-weight: 700; }.test-param-label span { color: #e45564; font-size: 8px; }.test-param-control { display: flex; gap: 7px; }.test-actions { margin: 14px 0 0; }.parameter-empty.compact { padding: 14px; }.json-hint { margin-top: 5px; color: #9aa2b1; font-size: 8px; }.test-logo { font-size: 7px; background: linear-gradient(145deg,#0ea5a4,#3b82f6); }.result-table { width: 100%; }.curl-block { margin-top: 14px; border-radius: 9px; }.curl-head { padding: 9px 12px; }.curl-head > div:first-child strong, .curl-head > div:first-child span { display: block; }.curl-head > div:first-child strong { font-size: 10px; }.curl-head > div:first-child span { margin-top: 3px; color: #929aaa; font-size: 8px; font-weight: 400; }.test-credential { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-top: 1px solid #eef0f4; background: #fafbfc; }.test-credential > span { min-width: 92px; color: #596579; font-size: 9px; font-weight: 650; }.curl-code { max-height: 145px; font-size: 10px; }
.publish-target { border-radius: 9px; }.publish-logo { font-size: 7px; background: linear-gradient(145deg,#6c5ce7,#ec5f88); }.auth-options { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 9px; }.auth-options button { position: relative; min-width: 0; display: flex; align-items: center; gap: 9px; padding: 11px 9px; overflow: hidden; border: 1px solid #e4e7ed; border-radius: 9px; color: #687286; text-align: left; background: #fff; cursor: pointer; }.auth-options button:hover { border-color: #cfcaf8; }.auth-options button.active { border-color: #bdb6ff; background: #f2f0ff; box-shadow: 0 0 0 2px rgba(99,91,255,.06); }.auth-options button > span { width: 31px; height: 31px; display: grid; place-items: center; flex-shrink: 0; border-radius: 8px; color: #fff; background: linear-gradient(145deg,#6976de,#8f75ed); font-size: 7px; font-weight: 800; }.auth-options button div { min-width: 0; }.auth-options strong, .auth-options small { display: block; }.auth-options strong { color: #465166; font-size: 10px; }.auth-options small { margin-top: 3px; overflow: hidden; color: #969ead; font-size: 8px; text-overflow: ellipsis; white-space: nowrap; }.auth-options button > i { display: none; position: absolute; top: 5px; right: 6px; color: #635bff; font-size: 9px; font-style: normal; }.auth-options button.active > i { display: block; }.secret-field { margin-top: 12px; padding: 12px; border-radius: 9px; background: #f7f8fb; }.secret-field .field-label { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }.secret-field .field-label span { color: #566176; font-size: 10px; font-weight: 650; }.secret-field .field-label small { color: #8c96a7; font-size: 8px; }.security-hint { margin-top: 7px; color: #8a6a25; font-size: 8px; line-height: 1.5; }.limit-config { display: grid; grid-template-columns: .9fr 1.15fr 1fr; align-items: end; gap: 11px; padding: 13px; }.limit-config > div > label { display: block; margin-bottom: 6px; color: #687286; font-size: 9px; font-weight: 650; }.limit-config :deep(.el-input-number), .limit-config :deep(.el-select) { width: 100%; }.inline-control { display: grid; grid-template-columns: 1fr 88px; align-items: center; gap: 6px; }.inline-control > span { color: #8c96a7; font-size: 9px; }.limit-config > p { grid-column: 1/-1; margin: 0; color: #929aaa; font-size: 8px; }.log-block { border-radius: 8px; }.muted { color: #c0c4cc; }
@media (max-width: 900px) { .page-hero { align-items: flex-start; flex-wrap: wrap; }.hero-stats { order: 3; width: 100%; }.hero-stats > div:first-child { border-left: 0; }.config-shell { grid-template-columns: 185px minmax(0,1fr); }.config-content { padding: 21px 18px; }.parameter-head, .parameter-row { grid-template-columns: 1fr 90px 1fr; }.parameter-head span:last-child, .parameter-row > :last-child { display: none; }.test-shell { grid-template-columns: 280px minmax(0,1fr); } }
@media (max-width: 680px) { .page-hero { gap: 16px; padding: 18px; }.hero-copy { min-width: 0; width: 100%; }.page-hero p, .support-tip { display: none; }.hero-stats > div { min-width: 0; flex: 1; padding: 3px 9px; }.list-heading { padding: 0 14px; }.dialog-heading { gap: 9px; padding-right: 28px; }.dialog-logo { width: 34px; height: 34px; }.dialog-subtitle, .dialog-mode { display: none; }.dialog-title { font-size: 15px; white-space: nowrap; }.config-shell { height: calc(94vh - 138px); min-height: 0; display: block; overflow-y: auto; }.config-sidebar { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 5px; padding: 10px; border-right: 0; border-bottom: 1px solid #e7e9f0; }.sidebar-label { grid-column: 1/-1; }.step-card { padding: 7px; }.step-card small, .service-summary { display: none; }.config-content { overflow: visible; padding: 18px 13px; }.form-grid, .runtime-grid { grid-template-columns: 1fr; }.wide-field { grid-column: auto; }.parameter-head { display: none; }.parameter-row { grid-template-columns: 1fr; gap: 7px; padding: 12px; }.parameter-row > :last-child { display: flex; }.parameter-row + .parameter-row { border-top: 1px solid #e7e9f0; }.footer-tip { display: none; }.dialog-footer { gap: 5px; }.dialog-footer .el-button { min-width: 0; margin-left: 0; padding: 8px 9px; font-size: 11px; }.auth-options { grid-template-columns: 1fr; }.auth-options button { padding: 8px; }.auth-options small { white-space: normal; }.limit-config { min-width: 0; grid-template-columns: 1fr; overflow: hidden; }.limit-config > div { min-width: 0; }.limit-config > p { grid-column: auto; }.limit-config .inline-control { grid-template-columns: minmax(0,1fr) 72px; }.test-shell { display: block; max-height: calc(92vh - 340px); overflow-y: auto; }.test-params-panel { border-right: 0; border-bottom: 1px solid #e5e8ef; }.curl-head { align-items: flex-start; gap: 9px; }.curl-head-right { flex-wrap: wrap; justify-content: flex-end; } }
</style>

<style>
.api-config-dialog.el-dialog, .api-test-dialog.el-dialog, .api-publish-dialog.el-dialog { overflow: hidden; border-radius: 14px; }
.api-config-dialog .el-dialog__header, .api-test-dialog .el-dialog__header, .api-publish-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.api-config-dialog .el-dialog__body, .api-test-dialog .el-dialog__body, .api-publish-dialog .el-dialog__body { max-height: calc(92vh - 145px); overflow-y: auto; padding: 16px 20px; }
.api-publish-dialog .el-dialog__body { overflow-x: hidden; }
.api-config-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
@media (max-width:680px) { .api-config-dialog .el-dialog__header, .api-test-dialog .el-dialog__header, .api-publish-dialog .el-dialog__header { padding: 14px 13px 11px; }.api-config-dialog .el-dialog__body, .api-test-dialog .el-dialog__body, .api-publish-dialog .el-dialog__body { padding: 10px; }.api-config-dialog .el-dialog__footer { padding: 10px 12px 12px; } }
</style>
