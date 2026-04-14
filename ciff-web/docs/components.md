# Ciff 公共组件文档

## 目录

- [CiffTable — 通用分页表格](#cifftable)
- [CiffFormDialog — 通用表单弹窗](#ciffformdialog)
- [useConfirm — 删除确认](#useconfirm)
- [useRequest — 请求状态管理](#userequest)
- [notify — 统一通知](#notify)
- [类型定义](#类型定义)
- [完整示例：模型管理页面](#完整示例)

---

## CiffTable

通用列表页表格组件。传入 columns 配置和 API 方法，自动管理 loading、分页、数据请求。

### 导入

```vue
import CiffTable from '@/components/CiffTable.vue'
```

### Props

| Prop | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `columns` | `TableColumn[]` | 是 | — | 列配置，见下方 TableColumn |
| `api` | `(params: PageParams) => Promise<{ list: T[], total: number }>` | 是 | — | 请求数据的方法，返回分页结果 |
| `showPagination` | `boolean` | 否 | `true` | 是否显示底部分页 |
| `immediate` | `boolean` | 否 | `true` | 挂载时是否立即请求数据 |

### TableColumn 配置

```ts
interface TableColumn {
  label: string             // 列标题
  prop?: string             // 数据字段名（纯文本列）
  width?: number | string   // 固定宽度
  minWidth?: number | string // 最小宽度
  slot?: string             // 插槽名（自定义列内容）
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
}
```

### 暴露的方法

通过 `ref` 调用：

| 方法 | 说明 |
|------|------|
| `refresh()` | 重置到第 1 页并重新请求 |
| `fetchData()` | 用当前页码重新请求（不重置页码） |

### 用法示例

```vue
<template>
  <CiffTable
    ref="tableRef"
    :columns="columns"
    :api="fetchProviders"
  >
    <!-- 自定义列：用 slot 名对应 columns 中的 slot 字段 -->
    <template #status="{ row }">
      <el-tag :type="row.enabled ? 'success' : 'info'">
        {{ row.enabled ? '启用' : '禁用' }}
      </el-tag>
    </template>

    <template #actions="{ row }">
      <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
      <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
    </template>
  </CiffTable>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CiffTable from '@/components/CiffTable.vue'
import type { TableColumn } from '@/types/common'
import { getProviders } from '@/api/provider'

interface Provider {
  id: number
  name: string
  type: string
  enabled: boolean
  [key: string]: unknown
}

const tableRef = ref()

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 120 },
  { label: '类型', prop: 'type', width: 120 },
  { label: '状态', slot: 'status', width: 100, align: 'center' },
  { label: '操作', slot: 'actions', width: 150, fixed: 'right' },
]

// api 方法必须返回 { list, total } 格式
async function fetchProviders(params: { page: number; pageSize: number }) {
  const res = await getProviders(params)
  return { list: res.list, total: res.total }
}

// 外部触发刷新（比如新增/删除后）
function handleSomethingChanged() {
  tableRef.value?.refresh()
}
</script>
```

---

## CiffFormDialog

通用表单弹窗。新增和编辑复用同一个弹窗，通过 `open()` 方法打开并区分模式。

### 导入

```vue
import CiffFormDialog from '@/components/CiffFormDialog.vue'
```

### Props

| Prop | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `title` | `string` | 是 | — | 弹窗标题，自动拼接"新增"/"编辑"前缀 |
| `width` | `string \| number` | 否 | `'520px'` | 弹窗宽度 |
| `rules` | `FormRules` | 否 | — | Element Plus 表单校验规则 |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `submit` | `(data: T, done: () => void)` | 表单校验通过后触发。`done()` 调用后关闭弹窗 |

### 暴露的方法

| 方法 | 说明 |
|------|------|
| `open(data?)` | 打开弹窗。传 data 为编辑模式，不传为新增模式 |

### 用法示例

```vue
<template>
  <!-- 1. 放在模板中 -->
  <CiffFormDialog
    ref="dialogRef"
    title="供应商"
    :rules="rules"
    @submit="handleSubmit"
  >
    <!-- 作用域插槽：data 是表单数据，isEdit 是编辑模式标志 -->
    <template #default="{ data, isEdit }">
      <el-form-item label="名称" prop="name">
        <el-input v-model="data.name" placeholder="请输入供应商名称" />
      </el-form-item>

      <el-form-item label="API 地址" prop="baseUrl">
        <el-input v-model="data.baseUrl" placeholder="https://api.example.com" />
      </el-form-item>

      <el-form-item label="API Key" prop="apiKey">
        <el-input v-model="data.apiKey" type="password" show-password />
      </el-form-item>

      <!-- 编辑模式下显示额外字段 -->
      <el-form-item v-if="isEdit" label="状态">
        <el-switch v-model="data.enabled" />
      </el-form-item>
    </template>
  </CiffFormDialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import type { FormRules } from 'element-plus'

interface ProviderForm {
  name: string
  baseUrl: string
  apiKey: string
  enabled: boolean
  [key: string]: unknown
}

const dialogRef = ref()

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
}

// 新增：不传参数
function handleAdd() {
  dialogRef.value?.open()
}

// 编辑：传入已有数据
function handleEdit(row: ProviderForm) {
  dialogRef.value?.open(row)
}

// 提交处理：调用 API，成功后调 done() 关闭弹窗
async function handleSubmit(data: ProviderForm, done: () => void) {
  try {
    if (data.id) {
      await updateProvider(data.id, data)  // 编辑
    } else {
      await createProvider(data)           // 新增
    }
    done()               // 关闭弹窗
    tableRef.value?.refresh()  // 刷新列表
  } catch {
    // 错误已由 request.ts 拦截器处理
  }
}
</script>
```

---

## useConfirm

删除确认 composable。弹出确认框 → 调 API → 显示成功提示，一行代码完成。

### 导入

```ts
import { useConfirm } from '@/composables/useConfirm'
```

### 返回值

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `confirm` | `(message, api, successText?)` | `Promise<T \| undefined>` | 确认后调 api，成功后弹提示 |

参数说明：
- `message` — 确认框的提示文案，如 `"确定删除该供应商？"`
- `api` — 返回 Promise 的函数，如 `() => deleteProvider(id)`
- `successText` — 成功提示文案，默认 `"删除成功"`

### 用法示例

```vue
<script setup lang="ts">
import { useConfirm } from '@/composables/useConfirm'
import { deleteProvider } from '@/api/provider'

const { confirm } = useConfirm()

// 点击删除按钮
async function handleDelete(id: number) {
  await confirm(
    '确定要删除该供应商吗？删除后不可恢复。',
    () => deleteProvider(id),
    '供应商已删除',
  )
  // 到这里说明删除成功，刷新列表
  tableRef.value?.refresh()
}
</script>
```

---

## useRequest

请求状态管理 composable。自动管理 `loading` / `data` / `error` 三态。

### 导入

```ts
import { useRequest } from '@/composables/useRequest'
```

### 返回值

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | `Ref<T \| undefined>` | 响应数据 |
| `loading` | `Ref<boolean>` | 是否请求中 |
| `error` | `Ref<Error \| undefined>` | 错误信息 |
| `execute` | `(...args) => Promise<T \| undefined>` | 发起请求，参数透传给 apiFn |

### 用法示例

```vue
<template>
  <div v-loading="loading">
    <div v-if="data">
      <!-- 使用 data -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRequest } from '@/composables/useRequest'
import { getProviderDetail } from '@/api/provider'

// 泛型自动推断：data 的类型是 Provider | undefined
const { data, loading, execute } = useRequest(getProviderDetail)

onMounted(() => {
  execute(123)  // 等同于调用 getProviderDetail(123)
})
</script>
```

---

## notify

统一通知封装。替代直接调用 `ElMessage`，统一 duration 和风格。

### 导入

```ts
import { notifySuccess, notifyError, notifyWarning } from '@/utils/notify'
```

### 方法

| 方法 | 参数 | duration | 说明 |
|------|------|----------|------|
| `notifySuccess(message)` | 成功提示文案 | 2.5s | 绿色成功提示 |
| `notifyError(message)` | 错误提示文案 | 3.5s | 红色错误提示 |
| `notifyWarning(message)` | 警告提示文案 | 2.5s | 黄色警告提示 |

### 用法示例

```ts
import { notifySuccess, notifyError } from '@/utils/notify'

notifySuccess('保存成功')
notifyError('网络请求失败，请稍后重试')
notifyWarning('该操作可能影响在线用户')
```

---

## 类型定义

所有公共类型定义在 `src/types/common.ts`：

```ts
// 分页请求参数
interface PageParams {
  page: number
  pageSize: number
}

// 分页响应结果
interface PageResult<T> {
  list: T[]
  total: number
}

// 表格列配置
interface TableColumn {
  label: string
  prop?: string
  width?: number | string
  minWidth?: number | string
  slot?: string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
}
```

---

## 完整示例

一个完整的模型管理页面，展示所有公共组件如何配合使用：

```vue
<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <PageHeader title="模型管理" description="配置模型供应商及其接入参数">
      <el-button type="primary" @click="handleAdd">添加供应商</el-button>
    </PageHeader>

    <!-- 表格 -->
    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchProviders">
        <template #status="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </CiffTable>
    </div>

    <!-- 表单弹窗 -->
    <CiffFormDialog
      ref="dialogRef"
      title="供应商"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ data }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" />
        </el-form-item>
        <el-form-item label="API 地址" prop="baseUrl">
          <el-input v-model="data.baseUrl" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="data.apiKey" type="password" show-password />
        </el-form-item>
      </template>
    </CiffFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import type { TableColumn } from '@/types/common'
import type { FormRules } from 'element-plus'

// ---------- 类型 ----------
interface Provider {
  id: number
  name: string
  baseUrl: string
  apiKey: string
  enabled: boolean
  [key: string]: unknown
}

// ---------- 表格配置 ----------
const tableRef = ref()
const dialogRef = ref()

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 140 },
  { label: 'API 地址', prop: 'baseUrl', minWidth: 200 },
  { label: '状态', slot: 'status', width: 100, align: 'center' },
  { label: '操作', slot: 'actions', width: 140, fixed: 'right' },
]

async function fetchProviders(params: { page: number; pageSize: number }) {
  // 替换为真实 API
  return { list: [] as Provider[], total: 0 }
}

// ---------- 表单配置 ----------
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
}

function handleAdd() {
  dialogRef.value?.open()
}

function handleEdit(row: Provider) {
  dialogRef.value?.open(row)
}

async function handleSubmit(data: Provider, done: () => void) {
  try {
    if (data.id) {
      // await updateProvider(data.id, data)
    } else {
      // await createProvider(data)
    }
    done()
    tableRef.value?.refresh()
  } catch {
    // 已由拦截器处理
  }
}

// ---------- 删除 ----------
const { confirm } = useConfirm()

async function handleDelete(id: number) {
  await confirm(
    '确定删除该供应商？删除后不可恢复。',
    () => Promise.resolve(), // 替换为 () => deleteProvider(id)
  )
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}
</style>
```
