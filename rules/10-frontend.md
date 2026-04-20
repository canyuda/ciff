# 前端开发规范

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API + `<script setup>`) | 3.5+ |
| 语言 | TypeScript | 5.x |
| UI 库 | Element Plus | 2.x |
| 构建 | Vite | 5.x |
| 状态 | Pinia | 3.x |
| 路由 | Vue Router | 4.x |
| 测试 | Vitest | 4.x |

## 项目结构

```
ciff-web/
├── src/
│   ├── api/              # API 请求函数，按模块拆分
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件（CiffTable、CiffFormDialog、PageHeader、TopBar）
│   ├── composables/      # 组合式函数（useConfirm、useRequest）
│   ├── layouts/          # 布局组件（AppLayout）
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── styles/           # 全局样式（design-tokens.css、element-overrides.css、base.css）
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数（request.ts、notify.ts）
│   └── views/            # 页面组件，按功能模块建子目录
├── docs/                 # 组件使用文档
└── public/               # 不参与构建的静态资源
```

## 设计系统

### 样式导入顺序（main.ts 中不可更改）

1. `design-tokens.css` — 设计 token
2. `element-plus/dist/index.css` — Element Plus 原始样式
3. `element-overrides.css` — 主题覆盖
4. `base.css` — 全局重置

### 色彩体系

| 用途 | CSS 变量 | 色值 |
|------|----------|------|
| 主色 | `--ciff-primary` | `#6366F1` |
| 辅色 | `--ciff-accent` | `#10B981` |
| 页面背景 | `--ciff-bg-page` | `#F8FAFC` |
| 内容区背景 | `--ciff-bg-secondary` | `#F1F5F9` |
| 卡片背景 | `--ciff-bg-card` | `#FFFFFF` |
| 侧边栏 | `--ciff-sidebar-bg` | `#0F172A` |
| 危险 | `--ciff-danger` | `#EF4444` |

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

- 圆角: `--ciff-radius-sm(4px) → base(6px) → md(8px) → xl(12px) → 2xl(16px) → full`
- 阴影: `--ciff-shadow-xs → sm → base → md → lg → xl`
- 卡片: `--ciff-card-radius(--ciff-radius-xl) + --ciff-card-shadow(--ciff-shadow-sm)`
- 动效: `--ciff-duration-fast(120ms) → normal(200ms) → slow(300ms)`

### 按钮样式

| 操作类型 | Element Plus type | 视觉 |
|----------|-------------------|------|
| 主要操作 | `type="primary"` | 蓝紫渐变 + 阴影发光 |
| 次要操作 | `type="default"` | 白底 + 边框 |
| 危险操作 | `type="danger"` | 红色实心 |
| 文字按钮 | `link` 属性 | 无背景的链接样式 |

已在 `element-overrides.css` 中全局覆盖，无需额外写样式。

## 布局规范

### 页面结构

```
内容区(bg-secondary)
└── 页面容器(max-width: 1280px, padding: 24px)
    ├── PageHeader（标题 + 描述 ← → 操作按钮）
    └── .ciff-card（白底 + 阴影 + 圆角的卡片）
        └── 具体内容
```

### 公共组件清单

| 组件 | 路径 |
|------|------|
| `CiffTable` | `@/components/CiffTable.vue` |
| `CiffFormDialog` | `@/components/CiffFormDialog.vue` |
| `PageHeader` | `@/components/PageHeader.vue` |
| `TopBar` | `@/components/TopBar.vue` |
| `AppLayout` | `@/layouts/AppLayout.vue` |

### Composable 清单

| 名称 | 路径 |
|------|------|
| `useConfirm` | `@/composables/useConfirm` |
| `useRequest` | `@/composables/useRequest` |

### 工具函数清单

| 名称 | 路径 |
|------|------|
| `notifySuccess/Error/Warning` | `@/utils/notify` |
| `get/post/put/del` | `@/utils/request` |

### 类型清单

| 类型 | 路径 |
|------|------|
| `PageResult<T>` | `@/types/common` |
| `PageParams` | `@/types/common` |
| `TableColumn` | `@/types/common` |

详细用法见 `ciff-web/docs/components.md`。

## 编码规范

### 文件命名

- 组件：`PascalCase.vue`
- composable / 工具函数 / 类型：`camelCase.ts`
- 样式文件：`kebab-case.css`

### 组件写法顺序

1. 导入
2. 类型定义（简单类型直接写，复杂类型放 `types/`）
3. Props / Emits
4. 响应式状态
5. 计算属性
6. 方法
7. 生命周期（尽量少用）

### 禁止事项

- **禁止硬编码颜色值**：必须用 `--ciff-*` CSS 变量
- **禁止硬编码间距**：必须用 `--ciff-space-*` 变量
- **禁止直接调用 `ElMessage`**：统一用 `notifySuccess/Error/Warning`
- **禁止在组件内写 try-catch-finally 管理 loading**：用 `useRequest`
- **禁止自己写确认删除弹窗**：用 `useConfirm`
- **禁止直接操作 Element Plus 默认样式**：覆盖统一放 `element-overrides.css`
- **禁止安装新的 UI 库**：只用 Element Plus
- **禁止使用 `any`**：必须给明确类型
- **禁止使用 Options API**：全部用 Composition API + `<script setup>`

### 标准列表页模式

所有列表页遵循相同模式：表格 + 弹窗 + 删除确认。完整示例见 `docs/rules-snippets/10-frontend-examples.vue`。

## API 层规范

- 所有 API 请求放在 `src/api/` 下，按模块拆分文件
- 后端统一响应格式：`{ code: number, message: string, data: T }`
- `request.ts` 拦截器已处理：`code === 200` 提取 `data`，其他弹出错误

## Vite 代理配置

`vite.config.ts` 已配置 `/api` 代理到 `http://localhost:8080`。
