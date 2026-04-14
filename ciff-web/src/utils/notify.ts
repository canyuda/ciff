import { ElMessage, type MessageHandler } from 'element-plus'

const DEFAULT_DURATION = 2500

export function notifySuccess(message: string): MessageHandler {
  return ElMessage({
    message,
    type: 'success',
    duration: DEFAULT_DURATION,
  })
}

export function notifyError(message: string): MessageHandler {
  return ElMessage({
    message,
    type: 'error',
    duration: 3500,
  })
}

export function notifyWarning(message: string): MessageHandler {
  return ElMessage({
    message,
    type: 'warning',
    duration: DEFAULT_DURATION,
  })
}
