import { ref, type Ref } from 'vue'

/**
 * Request state management composable.
 * Eliminates try-catch-finally boilerplate on every page.
 */
export function useRequest<T, A extends unknown[] = []>(
  apiFn: (...args: A) => Promise<T>,
) {
  const data: Ref<T | undefined> = ref()
  const loading = ref(false)
  const error: Ref<Error | undefined> = ref()

  async function execute(...args: A): Promise<T | undefined> {
    loading.value = true
    error.value = undefined
    try {
      const result = await apiFn(...args)
      data.value = result
      return result
    } catch (e) {
      error.value = e instanceof Error ? e : new Error(String(e))
      return undefined
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}
