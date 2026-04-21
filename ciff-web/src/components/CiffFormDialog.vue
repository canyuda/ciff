<template>
  <el-dialog
    v-model="visible"
    class="ciff-form-dialog"
    :title="dialogTitle"
    :width="width"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="onClosed"
  >
    <el-form
      ref="formRef"
      :model="currentFormData()"
      :rules="rules"
      label-width="90px"
      label-position="right"
    >
      <slot :data="currentFormData()" :is-edit="isEdit" />
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" generic="T extends object">
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = withDefaults(
  defineProps<{
    title: string
    width?: string | number
    rules?: FormRules
    submitHandler?: (data: T) => void | Promise<void>
  }>(),
  {
    width: '520px',
  },
)

defineSlots<{
  default?: (props: { data: T; isEdit: boolean }) => unknown
}>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const formData = ref<T>({} as T)
const isEdit = ref(false)
const submitting = ref(false)

const dialogTitle = computed(() => {
  const prefix = isEdit.value ? '编辑' : '新增'
  return `${prefix}${props.title}`
})

function open(data?: Partial<T>) {
  formData.value = data ? { ...data } as T : ({} as T)
  isEdit.value = data ? !!(data as any).id : false
  visible.value = true
}

function onClosed() {
  formRef.value?.resetFields()
  formData.value = {} as T
  isEdit.value = false
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await props.submitHandler?.(cloneFormData())
    visible.value = false
  } finally {
    submitting.value = false
  }
}

function cloneFormData() {
  return { ...formData.value } as T
}

function currentFormData() {
  return formData.value
}

defineExpose({ open })
</script>
