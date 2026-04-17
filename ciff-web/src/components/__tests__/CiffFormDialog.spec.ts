import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import CiffFormDialog from '../CiffFormDialog.vue'

const ElDialogStub = {
  template: `
    <div class="el-dialog" v-if="modelValue">
      <div class="el-dialog__header">{{ title }}</div>
      <div class="el-dialog__body"><slot /></div>
      <div class="el-dialog__footer"><slot name="footer" /></div>
    </div>
  `,
  props: ['modelValue', 'title', 'width', 'closeOnClickModal', 'destroyOnClose'],
  emits: ['update:modelValue', 'closed'],
}

const ElFormStub = {
  template: '<form class="el-form"><slot /></form>',
  props: ['model', 'rules'],
  methods: {
    validate() { return Promise.resolve(true) },
    resetFields() {},
  },
}

const ElButtonStub = {
  template: '<button class="el-button" @click="$emit(`click`)" :class="type"><slot /></button>',
  props: ['type', 'loading'],
}

describe('CiffFormDialog', () => {
  function createWrapper(props = {}, slots = {}) {
    return mount(CiffFormDialog, {
      props: {
        title: '供应商',
        ...props,
      },
      global: {
        stubs: {
          'el-dialog': ElDialogStub,
          'el-form': ElFormStub,
          'el-button': ElButtonStub,
        },
      },
      slots: {
        default: '<div class="form-content">{{ isEdit ? "编辑" : "新增" }}</div>',
        ...slots,
      },
    })
  }

  it('should show dialog when open is called', async () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.el-dialog').exists()).toBe(false)

    ;(wrapper.vm as any).open()
    await nextTick()

    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })

  it('should set isEdit false when open without data', async () => {
    const wrapper = createWrapper()
    ;(wrapper.vm as any).open()
    await nextTick()

    expect(wrapper.text()).toContain('新增')
  })

  it('should set isEdit true when open with data', async () => {
    const wrapper = createWrapper()
    ;(wrapper.vm as any).open({ name: 'OpenAI' })
    await nextTick()

    expect(wrapper.text()).toContain('编辑')
  })

  it('should call submitHandler and close on confirm', async () => {
    const submitHandler = vi.fn().mockResolvedValue(undefined)
    const wrapper = createWrapper({ submitHandler })
    ;(wrapper.vm as any).open({ name: 'Test' })
    await nextTick()

    const buttons = wrapper.findAll('.el-button')
    await buttons[buttons.length - 1].trigger('click')
    await flushPromises()

    expect(submitHandler).toHaveBeenCalled()
    expect(wrapper.find('.el-dialog').exists()).toBe(false)
  })

  it('should close on cancel', async () => {
    const wrapper = createWrapper()
    ;(wrapper.vm as any).open()
    await nextTick()

    const buttons = wrapper.findAll('.el-button')
    await buttons[0].trigger('click')
    await nextTick()

    expect(wrapper.find('.el-dialog').exists()).toBe(false)
  })
})
