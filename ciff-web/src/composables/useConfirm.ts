import { ElMessageBox } from 'element-plus'
import {notifySuccess} from "@/utils/notify.ts";

/**
 * Delete confirmation composable.
 * Confirm dialog → API call → success toast, all in one line.
 */
export function useConfirm() {
  async function confirm<T>(
    message: string,
    api: () => Promise<T>,
    successText = '删除成功',
  ): Promise<T | undefined> {
    await ElMessageBox.confirm(message, '确认操作', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const result = await api()
    notifySuccess(successText)
    return result
  }

  return { confirm }
}
