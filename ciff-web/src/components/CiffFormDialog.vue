<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    :width="width"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="onClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="90px"
      label-position="left"
    >
      <slot :data="formData" :is-edit="isEdit" />
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" generic="T extends Record<string, unknown>">
import { ref, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = withDefaults(
  defineProps<{
    title: string
    width?: string | number
    rules?: FormRules
  }>(),
  {
    width: '520px',
  },
)

const emit = defineEmits<{
  submit: [data: T, done: () => void]
}>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const formData = ref<Record<string, unknown>>({}) as { value: T }
const isEdit = ref(false)
const submitting = ref(false)

const dialogTitle = computed(() => {
  const prefix = isEdit.value ? '编辑' : '新增'
  return `${prefix}${props.title}`
})

function open(data?: Partial<T>) {
  formData.value = data ? { ...data } as T : ({} as T)
  isEdit.value = !!data
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
    emit('submit', { ...formData.value } as T, () => {
      visible.value = false
    })
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<style>
.el-form--label-left .el-form-item__label {
  justify-content: flex-end;
}
</style>
