import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import KnowledgeList from '../KnowledgeList.vue'

// Stubs
const ElTableStub = {
  template: '<div class="el-table"><slot /><slot name="empty" /></div>',
  props: ['data', 'loading'],
}

const ElTableColumnStub = {
  template: '<div class="el-table-column"><slot :row="defaultRow" /></div>',
  props: ['label', 'prop', 'width', 'minWidth', 'align', 'fixed'],
  data() {
    return {
      defaultRow: { fileSize: 1024, status: 'ready', chunkCount: 5, id: 1, fileName: 'test.txt' },
    }
  },
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

const ElInputNumberStub = {
  template: '<input class="el-input-number" :value="modelValue" />',
  props: ['modelValue', 'min', 'max', 'step'],
}

const ElFormItemStub = {
  template: '<div class="el-form-item"><slot /></div>',
  props: ['label', 'prop'],
}

const ElDrawerStub = {
  template: '<div class="el-drawer" v-if="modelValue"><slot /></div>',
  props: ['modelValue', 'title', 'size', 'destroyOnClose'],
  emits: ['update:modelValue'],
}

const ElUploadStub = {
  template: '<div class="el-upload"><slot /></div>',
  props: ['autoUpload', 'limit', 'accept', 'fileList'],
}

const ElIconStub = {
  template: '<span class="el-icon"><slot /></span>',
}

const ElDialogStub = {
  template: '<div class="el-dialog" v-if="modelValue"><slot /></div>',
  props: ['modelValue', 'title', 'width', 'destroyOnClose'],
  emits: ['update:modelValue'],
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

// Mocks
vi.mock('@/api/knowledge', () => ({
  getKnowledgeList: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  getKnowledgeById: vi.fn().mockResolvedValue({
    id: 1,
    name: 'Test KB',
    description: 'desc',
    chunkSize: 700,
    embeddingModel: 'text-embedding-v3',
    status: 'active',
  }),
  createKnowledge: vi.fn().mockResolvedValue({}),
  updateKnowledge: vi.fn().mockResolvedValue({}),
  deleteKnowledge: vi.fn().mockResolvedValue({}),
  uploadDocument: vi.fn().mockResolvedValue({}),
  listDocuments: vi.fn().mockResolvedValue([]),
  deleteDocument: vi.fn().mockResolvedValue({}),
  processDocument: vi.fn().mockResolvedValue({}),
  rebuildVectors: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/composables/useConfirm', () => ({
  useConfirm: () => ({
    confirm: vi.fn((msg, api) => api()),
  }),
}))

vi.mock('@/utils/notify', () => ({
  notifySuccess: vi.fn(),
}))

describe('KnowledgeList', () => {
  it('should render page header', async () => {
    const wrapper = mount(KnowledgeList, {
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
          'el-input-number': ElInputNumberStub,
          'el-form-item': ElFormItemStub,
          'el-drawer': ElDrawerStub,
          'el-upload': ElUploadStub,
          'el-icon': ElIconStub,
          'el-dialog': ElDialogStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.page-header .title').text()).toContain('知识库管理')
  })

  it('should open document drawer when clicking doc count', async () => {
    const wrapper = mount(KnowledgeList, {
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
          'el-input-number': ElInputNumberStub,
          'el-form-item': ElFormItemStub,
          'el-drawer': ElDrawerStub,
          'el-upload': ElUploadStub,
          'el-icon': ElIconStub,
          'el-dialog': ElDialogStub,
          CiffTable: CiffTableStub,
          CiffFormDialog: CiffFormDialogStub,
          PageHeader: PageHeaderStub,
        },
      },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    vm.openDocPanel({ id: 1, name: 'Test KB' })
    await nextTick()

    expect(vm.docDrawerVisible).toBe(true)
    expect(vm.currentKnowledge?.name).toBe('Test KB')
  })
})
