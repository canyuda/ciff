import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick, h } from 'vue'
import CiffTable from '../CiffTable.vue'

// Stub Element Plus table components to avoid deep rendering issues in jsdom
const ElTableStub = {
  template: '<div class="el-table"><slot /><slot name="empty" /></div>',
  props: ['data', 'loading'],
}

const ElTableColumnStub = {
  render() {
    return h('div', { class: 'el-table-column' }, this.$slots.default?.({ row: { name: 'OpenAI', type: 'openai' } }))
  },
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
  props: ['modelValue'],
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

describe('CiffTable', () => {
  const columns = [
    { label: '名称', prop: 'name' },
    { label: '类型', slot: 'type' },
  ]

  async function createWrapper(api: any, props = {}) {
    const wrapper = mount(CiffTable, {
      props: {
        columns,
        api,
        showPagination: true,
        immediate: true,
        ...props,
      },
      global: {
        stubs: {
          'el-table': ElTableStub,
          'el-table-column': ElTableColumnStub,
          'el-empty': ElEmptyStub,
          'el-pagination': ElPaginationStub,
          'el-select': ElSelectStub,
          'el-option': ElOptionStub,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('should auto load data on mount', async () => {
    const api = vi.fn().mockResolvedValue({ list: [{ name: 'OpenAI', type: 'openai' }], total: 1 })
    const wrapper = await createWrapper(api)

    expect(api).toHaveBeenCalledWith({ page: 1, pageSize: 10 })
    expect(wrapper.find('.el-table').exists()).toBe(true)
  })

  it('should render columns with correct labels', async () => {
    const api = vi.fn().mockResolvedValue({ list: [{ name: 'OpenAI', type: 'openai' }], total: 1 })
    const wrapper = await createWrapper(api)

    const columns = wrapper.findAll('.el-table-column')
    expect(columns).toHaveLength(2)
  })

  it('should show empty state when no data', async () => {
    const api = vi.fn().mockResolvedValue({ list: [], total: 0 })
    const wrapper = await createWrapper(api)

    expect(wrapper.find('.el-empty').exists()).toBe(true)
  })

  it('refresh should reset to page 1 and reload', async () => {
    const api = vi.fn().mockResolvedValue({ list: [{ name: 'A' }], total: 1 })
    const wrapper = await createWrapper(api)

    api.mockClear()
    ;(wrapper.vm as any).refresh()
    await nextTick()
    await flushPromises()

    expect(api).toHaveBeenCalledWith({ page: 1, pageSize: 10 })
  })
})
