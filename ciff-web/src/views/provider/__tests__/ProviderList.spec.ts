import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ProviderList from '../ProviderList.vue'

// Stubs
const ElTableStub = {
  template: '<div class="el-table"><slot /><slot name="empty" /></div>',
  props: ['data', 'loading'],
}

const ElTableColumnStub = {
  template: '<div class="el-table-column"><slot v-bind="$attrs" /></div>',
  props: ['label', 'prop', 'width', 'minWidth', 'align', 'fixed'],
}

const ElEmptyStub = {
  template: '<div class="el-empty">暂无数据</div>',
}

const ElPaginationStub = {
  template: '<div class="el-pagination" />',
  props: ['currentPage', 'pageSize', 'total'],
}

const ElSelectStub = {
  template: '<select class="el-select"><slot /></select>',
  props: ['modelValue', 'disabled'],
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

const ElButtonStub = {
  template: '<button class="el-button" :class="typeClass" @click="$emit(\'click\')"><slot /></button>',
  props: ['type', 'link'],
  computed: {
    typeClass() {
      return this.type ? `el-button--${this.type}` : ''
    },
  },
}

const ElTagStub = {
  template: '<span class="el-tag"><slot /></span>',
  props: ['type', 'size', 'effect', 'color'],
}

const ElInputStub = {
  template: '<input class="el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  props: ['modelValue'],
}

const ElFormItemStub = {
  template: '<div class="el-form-item"><slot /></div>',
  props: ['label', 'prop'],
}

const CiffTableStub = {
  template: '<div class="ciff-table"><slot /><slot name="empty" /></div>',
  props: ['columns', 'api', 'showPagination', 'immediate'],
}

const CiffFormDialogStub = {
  template: '<div class="ciff-form-dialog" v-if="visible"><slot :data="formData" :isEdit="isEdit" /></div>',
  props: ['title', 'width', 'rules', 'submitHandler'],
  data() {
    return { visible: false, formData: {}, isEdit: false }
  },
  methods: {
    open(data?: any) {
      this.visible = true
      this.formData = data ? { ...data } : {}
      this.isEdit = !!data
    },
  },
}

const PageHeaderStub = {
  template: '<div class="page-header"><div class="title">{{ title }}</div><slot /></div>',
  props: ['title', 'description'],
}

const ElDialogStub = {
  template: '<div class="el-dialog" v-if="modelValue"><slot /></div>',
  props: ['modelValue', 'title', 'width', 'destroyOnClose'],
  emits: ['update:modelValue'],
}

const ElIconStub = {
  template: '<span class="el-icon"><slot /></span>',
}

// Mocks
vi.mock('@/api/provider', () => ({
  getProviders: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  getProviderList: vi.fn().mockResolvedValue([]),
  createProvider: vi.fn().mockResolvedValue({}),
  updateProvider: vi.fn().mockResolvedValue({}),
  deleteProvider: vi.fn().mockResolvedValue({}),
  testProvider: vi.fn().mockResolvedValue({ status: 'UP', lastLatencyMs: 100 }),
}))

vi.mock('@/api/model', () => ({
  getModels: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  createModel: vi.fn().mockResolvedValue({}),
  updateModel: vi.fn().mockResolvedValue({}),
  deleteModel: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/composables/useConfirm', () => ({
  useConfirm: () => ({
    confirm: vi.fn((msg, api) => api()),
  }),
}))

vi.mock('@/utils/notify', () => ({
  notifySuccess: vi.fn(),
}))

describe('ProviderList', () => {
  it('should render page header', async () => {
    const wrapper = mount(ProviderList, {
      global: {
        stubs: {
          'el-table': ElTableStub,
          'el-table-column': ElTableColumnStub,
          'el-empty': ElEmptyStub,
          'el-pagination': ElPaginationStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-button': ElButtonStub,
          'el-tag': ElTagStub,
          'el-input': ElInputStub,
          'el-form-item': ElFormItemStub,
          'el-dialog': ElDialogStub,
          'el-icon': ElIconStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.page-header .title').text()).toContain('供应商管理')
  })

  it('should open model dialog when click model management', async () => {
    const wrapper = mount(ProviderList, {
      global: {
        stubs: {
          'el-table': ElTableStub,
          'el-table-column': ElTableColumnStub,
          'el-empty': ElEmptyStub,
          'el-pagination': ElPaginationStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-button': ElButtonStub,
          'el-tag': ElTagStub,
          'el-input': ElInputStub,
          'el-form-item': ElFormItemStub,
          'el-dialog': ElDialogStub,
          'el-icon': ElIconStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    vm.handleOpenModels({ id: 1, name: 'OpenAI' })
    await nextTick()

    expect(vm.modelDialogVisible).toBe(true)
    expect(vm.currentProvider?.name).toBe('OpenAI')
  })
})
