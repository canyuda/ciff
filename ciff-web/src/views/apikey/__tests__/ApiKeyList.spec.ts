import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ApiKeyList from '../ApiKeyList.vue'

// ===== Stubs =====

const ElTableStub = {
  template: '<div class="el-table"><slot /><slot name="empty" /></div>',
  props: ['data', 'loading'],
}

const ElTableColumnStub = {
  template:
    '<div class="el-table-column"><slot :row="defaultRow" /></div>',
  props: ['label', 'prop', 'width', 'minWidth', 'align', 'fixed'],
  data() {
    return {
      defaultRow: {
        id: 1,
        name: 'Test Key',
        keyPrefix: 'ciff_a1b2',
        agentId: 1,
        status: 'active',
        expiresAt: '2026-12-31T00:00:00Z',
        createTime: '2026-01-01T00:00:00Z',
      },
    }
  },
}

const ElTagStub = {
  template: '<span class="el-tag" :class="type"><slot /></span>',
  props: ['type', 'size', 'effect'],
}

const ElButtonStub = {
  template:
    '<button class="el-button" :class="typeClass" @click="$emit(\'click\')"><slot /></button>',
  props: ['type', 'link', 'size', 'loading'],
  computed: {
    typeClass() {
      return this.type ? `el-button--${this.type}` : ''
    },
  },
  emits: ['click'],
}

const ElDialogStub = {
  template: '<div class="el-dialog" v-if="modelValue"><slot /><slot name="footer" /></div>',
  props: ['modelValue', 'title', 'width', 'destroyOnClose', 'closeOnClickModal'],
  emits: ['update:modelValue'],
}

const ElFormStub = {
  template: '<form class="el-form"><slot /></form>',
  props: ['model', 'rules', 'labelPosition'],
  methods: {
    validate() {
      return Promise.resolve(true)
    },
  },
}

const ElFormItemStub = {
  template: '<div class="el-form-item"><slot /></div>',
  props: ['label', 'prop'],
}

const ElInputStub = {
  template:
    '<input class="el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
}

const ElSelectStub = {
  template:
    '<select class="el-select" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

const ElDatePickerStub = {
  template: '<input class="el-date-picker" :value="modelValue" />',
  props: ['modelValue', 'type', 'placeholder'],
}

const ElAlertStub = {
  template: '<div class="el-alert"><slot /></div>',
  props: ['type', 'title', 'closable', 'showIcon'],
}

const ElPopconfirmStub = {
  template: '<div class="el-popconfirm"><slot /><slot name="reference" /></div>',
  props: ['title'],
  emits: ['confirm'],
}

const ElIconStub = {
  template: '<span class="el-icon"><slot /></span>',
}

const PageHeaderStub = {
  template: '<div class="page-header"><div class="title">{{ title }}</div><slot /></div>',
  props: ['title', 'description'],
}

// ===== Mocks =====

vi.mock('@/api/apiKey', () => ({
  listApiKeys: vi.fn().mockResolvedValue([
    {
      id: 1,
      name: 'Test Key',
      keyPrefix: 'ciff_a1b2',
      agentId: 1,
      status: 'active',
      expiresAt: '2026-12-31T00:00:00Z',
      createTime: '2026-01-01T00:00:00Z',
    },
    {
      id: 2,
      name: 'Revoked Key',
      keyPrefix: 'ciff_c3d4',
      agentId: 2,
      status: 'revoked',
      expiresAt: null,
      createTime: '2026-02-01T00:00:00Z',
    },
  ]),
  createApiKey: vi.fn().mockResolvedValue({
    id: 3,
    name: 'New Key',
    keyPrefix: 'ciff_e5f6',
    agentId: 1,
    status: 'active',
    expiresAt: null,
    createTime: '2026-04-25T00:00:00Z',
    rawKey: 'ciff_e5f6g7h8i9j0k1l2m3n4o5p6',
  }),
  revokeApiKey: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/agent', () => ({
  listAgents: vi.fn().mockResolvedValue([
    { id: 1, name: 'Test Agent' },
    { id: 2, name: 'Another Agent' },
  ]),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual as any,
    ElMessage: { success: vi.fn(), error: vi.fn() },
  }
})

import { listApiKeys, createApiKey, revokeApiKey } from '@/api/apiKey'
import { ElMessage } from 'element-plus'

// ===== Helpers =====

async function createWrapper() {
  const wrapper = mount(ApiKeyList, {
    global: {
      stubs: {
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub,
        'el-tag': ElTagStub,
        'el-button': ElButtonStub,
        'el-dialog': ElDialogStub,
        'el-form': ElFormStub,
        'el-form-item': ElFormItemStub,
        'el-input': ElInputStub,
        'el-select': ElSelectStub,
        'el-option': ElOptionStub,
        'el-date-picker': ElDatePickerStub,
        'el-alert': ElAlertStub,
        'el-popconfirm': ElPopconfirmStub,
        'el-icon': ElIconStub,
        PageHeader: PageHeaderStub,
      },
    },
  })
  await flushPromises()
  return wrapper
}

describe('ApiKeyList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- Rendering ---

  it('should render page header with title', async () => {
    const wrapper = await createWrapper()

    expect(wrapper.find('.page-header .title').text()).toContain('API Key')
  })

  it('should render API key table', async () => {
    const wrapper = await createWrapper()

    expect(wrapper.find('.el-table').exists()).toBe(true)
    expect(listApiKeys).toHaveBeenCalled()
  })

  it('should load keys on mount', async () => {
    await createWrapper()

    expect(listApiKeys).toHaveBeenCalledTimes(1)
  })

  it('should render create button', async () => {
    const wrapper = await createWrapper()

    const createBtn = wrapper.find('.el-button--primary')
    expect(createBtn.exists()).toBe(true)
    expect(createBtn.text()).toContain('创建 API Key')
  })

  // --- Create dialog ---

  it('should open create dialog on button click', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    expect(vm.dialogVisible).toBe(false)

    vm.openCreateDialog()
    await nextTick()

    expect(vm.dialogVisible).toBe(true)
    expect(vm.form.name).toBe('')
    expect(vm.form.agentId).toBeNull()
    expect(vm.form.expiresAt).toBeNull()
  })

  it('should display raw key after creation', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // Open dialog
    vm.openCreateDialog()
    await nextTick()

    // Fill form
    vm.form.name = 'New Key'
    vm.form.agentId = 1
    await nextTick()

    // Ensure formRef.validate resolves true
    const formRef = vm.formRef
    if (formRef) {
      formRef.validate = vi.fn().mockResolvedValue(true)
    }

    await vm.handleCreate()
    await flushPromises()

    expect(createApiKey).toHaveBeenCalledWith({
      name: 'New Key',
      agentId: 1,
      expiresAt: null,
    })
    expect(vm.dialogVisible).toBe(false)
    expect(vm.showKeyVisible).toBe(true)
    expect(vm.createdKey).toBe('ciff_e5f6g7h8i9j0k1l2m3n4o5p6')
  })

  // --- Revoke ---

  it('should show revoke confirmation for active keys', async () => {
    const wrapper = await createWrapper()

    // el-popconfirm is stubbed, just verify it exists in the DOM
    expect(wrapper.find('.el-popconfirm').exists()).toBe(true)
  })

  it('should call revokeApiKey and reload list on confirm', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    await vm.handleRevoke(1)
    await flushPromises()

    expect(revokeApiKey).toHaveBeenCalledWith(1)
    expect(ElMessage.success).toHaveBeenCalledWith('API key revoked')
    // fetchKeys is called after revoke
    expect(listApiKeys).toHaveBeenCalled()
  })

  // --- Copy key ---

  it('should copy raw key to clipboard', async () => {
    // Mock navigator.clipboard
    const mockWriteText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, {
      clipboard: { writeText: mockWriteText },
    })

    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.createdKey = 'ciff_e5f6g7h8i9j0k1l2m3n4o5p6'
    vm.copyKey()

    expect(mockWriteText).toHaveBeenCalledWith('ciff_e5f6g7h8i9j0k1l2m3n4o5p6')
    expect(ElMessage.success).toHaveBeenCalledWith('Copied to clipboard')
  })

  // --- Loading state ---

  it('should set loading true while fetching keys', async () => {
    // Keep the promise pending to observe loading state
    vi.mocked(listApiKeys).mockReturnValue(new Promise(() => {}))

    const wrapper = mount(ApiKeyList, {
      global: {
        stubs: {
          'el-table': ElTableStub,
          'el-table-column': ElTableColumnStub,
          'el-tag': ElTagStub,
          'el-button': ElButtonStub,
          'el-dialog': ElDialogStub,
          'el-form': ElFormStub,
          'el-form-item': ElFormItemStub,
          'el-input': ElInputStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-date-picker': ElDatePickerStub,
          'el-alert': ElAlertStub,
          'el-popconfirm': ElPopconfirmStub,
          'el-icon': ElIconStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await nextTick()

    expect((wrapper.vm as any).loading).toBe(true)
  })
})
