# 前端开发规范

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API + `<script setup>`) | 3.5+ |
| 语言 | TypeScript | 5.x |
| UI 库 | Element Plus | 2.x |
| 构建 | Vite | 8.x |
| 状态 | Pinia | 3.x |
| 路由 | Vue Router | 4.x |
| 测试 | Vitest | 4.x |

## 项目结构

```
ciff-web/
├── src/
│   ├── api/              # API 请求函数，按模块拆分
│   ├── assets/           # 静态资源（图片、SVG）
│   ├── components/       # 公共组件（CiffTable、CiffFormDialog 等）
│   ├── composables/      # 组合式函数（useConfirm、useRequest 等）
│   ├── layouts/          # 布局组件（AppLayout — 侧边栏）
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── styles/           # 全局样式
│   │   ├── design-tokens.css    # 设计系统 CSS 变量（禁止修改，除非改设计）
│   │   ├── element-overrides.css # Element Plus 主题覆盖
│   │   └── base.css             # 全局重置 + 排版
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数（request.ts、notify.ts）
│   └── views/            # 页面组件，按功能模块建子目录
├── docs/                 # 组件使用文档
└── public/               # 不参与构建的静态资源
```

## 设计系统

### 样式导入顺序

`main.ts` 中的导入顺序不可更改：

```ts
import './styles/design-tokens.css'       // 1. 设计 token
import 'element-plus/dist/index.css'      // 2. Element Plus 原始样式
import './styles/element-overrides.css'   // 3. 主题覆盖（依赖 1 和 2）
import './styles/base.css'                // 4. 全局重置
```

### 色彩体系

| 用途 | CSS 变量 | 色值 | 说明 |
|------|----------|------|------|
| 主色 | `--ciff-primary` | `#6366F1` | Indigo，按钮/链接/活跃态 |
| 辅色 | `--ciff-accent` | `#10B981` | Mint，状态指示/数据高亮 |
| 页面背景 | `--ciff-bg-page` | `#F8FAFC` | 最外层背景 |
| 内容区背景 | `--ciff-bg-secondary` | `#F1F5F9` | 内容区底色，卡片坐其上 |
| 卡片背景 | `--ciff-bg-card` | `#FFFFFF` | 白色，和内容区形成层次 |
| 侧边栏 | `--ciff-sidebar-bg` | `#0F172A` | 深海军蓝 |
| 危险 | `--ciff-danger` | `#EF4444` | 删除/危险操作 |

主色和辅色各有 50-900 共 10 个色阶，用 `--ciff-primary-{n}` / `--ciff-accent-{n}` 引用。

### 文字

| 用途 | CSS 变量 | 色值 |
|------|----------|------|
| 标题/重要文本 | `--ciff-text-primary` | `#0F172A` |
| 描述/辅助文本 | `--ciff-text-secondary` | `#64748B` |
| 占位符/禁用 | `--ciff-text-tertiary` | `#94A3B8` |

### 字体

| 用途 | CSS 变量 | 字体 |
|------|----------|------|
| 标题 | `--ciff-font-heading` | Plus Jakarta Sans |
| 正文 | `--ciff-font-body` | DM Sans |
| 代码 | `--ciff-font-mono` | JetBrains Mono |

### 间距（4px 基数）

| 场景 | CSS 变量 | 值 |
|------|----------|-----|
| 页面内边距 | `--ciff-page-padding` | 24px |
| 卡片内边距 | `--ciff-card-padding` | 20px |
| 元素间间距 | `--ciff-card-gap` | 16px |

完整间距阶梯：`--ciff-space-{1|1.5|2|2.5|3|4|5|6|8|10|12|16|20}`，单位 px。

### 圆角 / 阴影 / 动效

```
圆角:  --ciff-radius-sm(4px) → base(6px) → md(8px) → xl(12px) → 2xl(16px) → full(圆)
阴影:  --ciff-shadow-xs → sm → base → md → lg → xl
卡片:  --ciff-card-radius(--ciff-radius-xl) + --ciff-card-shadow(--ciff-shadow-sm)
动效:  --ciff-duration-fast(120ms) → normal(200ms) → slow(300ms)
```

### 按钮样式

| 操作类型 | Element Plus type | 视觉 |
|----------|-------------------|------|
| 主要操作 | `type="primary"` | 蓝紫渐变 + 阴影发光 |
| 次要操作 | `type="default"` | 白底 + 边框，hover 变主色 |
| 危险操作 | `type="danger"` | 红色实心 |
| 文字按钮 | `link` 属性 | 无背景的链接样式 |

已在 `element-overrides.css` 中全局覆盖，无需额外写样式。

## 布局规范

### 页面结构

每个页面遵循统一结构：

```
内容区(bg-secondary)
└── 页面容器(max-width: 1280px, padding: 24px)
    ├── PageHeader（标题 + 描述 ← → 操作按钮）
    └── .ciff-card（白底 + 阴影 + 圆角的卡片）
        └── 具体内容
```

### 页面模板

```vue
<template>
  <div class="page-container">
    <PageHeader title="页面标题" description="页面描述">
      <el-button type="primary">主操作</el-button>
    </PageHeader>
    <div class="ciff-card">
      <!-- 内容 -->
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}
</style>
```

## 公共组件

### 组件清单

| 组件 | 路径 | 用途 |
|------|------|------|
| `CiffTable` | `@/components/CiffTable.vue` | 通用分页表格 |
| `CiffFormDialog` | `@/components/CiffFormDialog.vue` | 新增/编辑表单弹窗 |
| `PageHeader` | `@/components/PageHeader.vue` | 页面标题栏（标题+描述+操作按钮） |
| `TopBar` | `@/components/TopBar.vue` | 顶栏（面包屑+用户信息） |
| `AppLayout` | `@/layouts/AppLayout.vue` | 侧边栏布局 |

### Composable 清单

| 名称 | 路径 | 用途 |
|------|------|------|
| `useConfirm` | `@/composables/useConfirm` | 确认对话框 → 调 API → 成功提示 |
| `useRequest` | `@/composables/useRequest` | 请求 loading/data/error 三态管理 |

### 工具函数清单

| 名称 | 路径 | 用途 |
|------|------|------|
| `notifySuccess` | `@/utils/notify` | 成功通知 |
| `notifyError` | `@/utils/notify` | 错误通知 |
| `notifyWarning` | `@/utils/notify` | 警告通知 |
| `get/post/put/del` | `@/utils/request` | HTTP 请求封装 |

### 类型清单

| 类型 | 路径 | 用途 |
|------|------|------|
| `PageResult<T>` | `@/types/common` | 分页响应：`{ list: T[], total: number }` |
| `PageParams` | `@/types/common` | 分页请求：`{ page, pageSize }` |
| `TableColumn` | `@/types/common` | 表格列配置：`{ label, prop?, slot?, ... }` |

详细用法见 [docs/components.md](../ciff-web/docs/components.md)。

## 编码规范

### 文件命名

- 组件：`PascalCase.vue`（如 `CiffTable.vue`、`AgentList.vue`）
- composable：`camelCase.ts`（如 `useConfirm.ts`）
- 工具函数：`camelCase.ts`（如 `notify.ts`）
- 类型文件：`camelCase.ts`（如 `common.ts`）
- 样式文件：`kebab-case.css`（如 `design-tokens.css`）

### 组件写法

```vue
<script setup lang="ts">
// 1. 导入
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CiffTable from '@/components/CiffTable.vue'

// 2. 类型定义（简单类型直接写，复杂类型放 types/）
interface Foo {
  id: number
  name: string
}

// 3. Props / Emits
const props = defineProps<{ title: string }>()
const emit = defineEmits<{ change: [value: string] }>()

// 4. 响应式状态
const loading = ref(false)

// 5. 计算属性
const displayName = computed(() => props.title.toUpperCase())

// 6. 方法
function handleSubmit() { /* ... */ }

// 7. 生命周期（尽量少用）
onMounted(() => { /* ... */ })
</script>
```

### 禁止事项

- **禁止硬编码颜色值**：必须用 `--ciff-*` CSS 变量
- **禁止硬编码间距**：必须用 `--ciff-space-*` 变量
- **禁止直接调用 `ElMessage`**：统一用 `notifySuccess/notifyError/notifyWarning`
- **禁止在组件内写 try-catch-finally 管理 loading**：用 `useRequest`
- **禁止自己写确认删除弹窗**：用 `useConfirm`
- **禁止直接操作 Element Plus 默认样式**：覆盖统一放 `element-overrides.css`
- **禁止安装新的 UI 库**：只用 Element Plus
- **禁止使用 `any`**：必须给明确类型
- **禁止使用 Options API**：全部用 Composition API + `<script setup>`

### 标准列表页模式

所有列表页遵循相同模式：表格 + 弹窗 + 删除确认。

```vue
<template>
  <div class="page-container">
    <PageHeader title="xxx管理" description="xxx">
      <el-button type="primary" @click="dialogRef?.open()">新增</el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchList">
        <template #actions="{ row }">
          <el-button link type="primary" @click="dialogRef?.open(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog ref="dialogRef" title="xxx" :rules="rules" @submit="handleSubmit">
      <template #default="{ data }">
        <!-- 表单项 -->
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

const tableRef = ref()
const dialogRef = ref()
const { confirm } = useConfirm()

const columns: TableColumn[] = [
  /* ... */
]
const rules: FormRules = { /* ... */ }

async function fetchList(params: { page: number; pageSize: number }) {
  /* 调 API，返回 { list, total } */
}

async function handleSubmit(data: any, done: () => void) {
  /* 调新增/编辑 API → done() → refresh() */
}

async function handleDelete(id: number) {
  await confirm('确定删除？', () => /* 调删除 API */)
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container { max-width: var(--ciff-content-max-width) }
</style>
```

## API 层规范

### 请求函数

所有 API 请求放在 `src/api/` 下，按模块拆分文件：

```ts
// src/api/provider.ts
import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface Provider {
  id: number
  name: string
  baseUrl: string
  [key: string]: unknown
}

export function getProviders(params: { page: number; pageSize: number }) {
  return get<PageResult<Provider>>('/v1/providers', params)
}

export function createProvider(data: Partial<Provider>) {
  return post<Provider>('/v1/providers', data)
}

export function updateProvider(id: number, data: Partial<Provider>) {
  return put<Provider>(`/v1/providers/${id}`, data)
}

export function deleteProvider(id: number) {
  return del(`/v1/providers/${id}`)
}
```

### 响应约定

后端统一响应格式：`{ code: number, message: string, data: T }`。

`request.ts` 拦截器已处理：
- `code === 200` → 提取 `data` 返回
- 其他 code → `ElMessage.error` 弹出错误
- 网络异常 → `ElMessage.error` 弹出错误

## Vite 代理配置

`vite.config.ts` 已配置 `/api` 代理到 `http://localhost:8080`。

前端开发不需要启动后端服务。`npm run dev` 即可运行。未对接的页面用 mock 数据或空状态。
