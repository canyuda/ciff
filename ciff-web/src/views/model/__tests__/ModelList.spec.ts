import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ModelList from '../ModelList.vue'

// Stubs
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
  props: ['type', 'size'],
}

const ElSelectStub = {
  template: '<select class="el-select"><slot /></select>',
  props: ['modelValue', 'disabled'],
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

const ElInputStub = {
  template: '<input class="el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  props: ['modelValue', 'type', 'rows', 'placeholder'],
}

const ElInputNumberStub = {
  template: '<input class="el-input-number" :value="modelValue" />',
  props: ['modelValue', 'min'],
}

const ElFormItemStub = {
  template: '<div class="el-form-item"><slot /></div>',
  props: ['label', 'prop'],
}

const CiffTableStub = {
  template: '<div class="ciff-table"><slot /></div>',
  props: ['columns', 'api'],
}

const CiffFormDialogStub = {
  template: '<div class="ciff-form-dialog" v-if="visible"><slot :data="formData" :isEdit="isEdit" /></div>',
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

const ElIconStub = {
  template: '<span class="el-icon"><slot /></span>',
}

// Mocks
vi.mock('@/api/model', () => ({
  getModels: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  createModel: vi.fn().mockResolvedValue({}),
  updateModel: vi.fn().mockResolvedValue({}),
  deleteModel: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/provider', () => ({
  getProviderList: vi.fn().mockResolvedValue([
    { id: 1, name: 'OpenAI' },
    { id: 2, name: 'Claude' },
  ]),
}))

vi.mock('@/composables/useConfirm', () => ({
  useConfirm: () => ({
    confirm: vi.fn((msg, api) => api()),
  }),
}))

vi.mock('@/utils/notify', () => ({
  notifySuccess: vi.fn(),
}))

describe('ModelList', () => {
  it('should render page header', async () => {
    const wrapper = mount(ModelList, {
      global: {
        stubs: {
          'el-button': ElButtonStub,
          'el-tag': ElTagStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-input': ElInputStub,
          'el-input-number': ElInputNumberStub,
          'el-form-item': ElFormItemStub,
          'el-icon': ElIconStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.page-header .title').text()).toContain('模型管理')
  })

  it('should load provider options on mount', async () => {
    const wrapper = mount(ModelList, {
      global: {
        stubs: {
          'el-button': ElButtonStub,
          'el-tag': ElTagStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-input': ElInputStub,
          'el-input-number': ElInputNumberStub,
          'el-form-item': ElFormItemStub,
          'el-icon': ElIconStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.providerOptions).toHaveLength(2)
    expect(vm.providerOptions[0].name).toBe('OpenAI')
  })

  it('should open dialog with row data for edit', async () => {
    const wrapper = mount(ModelList, {
      global: {
        stubs: {
          'el-button': ElButtonStub,
          'el-tag': ElTagStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
          'el-input': ElInputStub,
          'el-input-number': ElInputNumberStub,
          'el-form-item': ElFormItemStub,
          'el-icon': ElIconStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    const row = {
      id: 1,
      providerId: 1,
      name: 'gpt-4o',
      displayName: 'GPT-4o',
      maxTokens: 8192,
      defaultParams: { temperature: 0.7 },
    }
    vm.openDialog(row)
    await nextTick()

    expect(vm.dialogRef?.visible).toBe(true)
    expect(vm.dialogRef?.isEdit).toBe(true)
  })
})
