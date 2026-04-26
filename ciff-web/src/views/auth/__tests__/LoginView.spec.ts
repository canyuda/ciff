import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import LoginView from '../LoginView.vue'

// ===== Stubs =====

const ElFormStub = {
  template: `
    <form class="el-form" @submit.prevent="$emit('submit')">
      <slot />
    </form>
  `,
  props: ['model', 'rules', 'labelPosition'],
  methods: {
    validate() {
      // Default: resolve true. Tests override via wrapper.vm to control behavior.
      return Promise.resolve(true)
    },
  },
}

const ElFormItemStub = {
  template: '<div class="el-form-item"><slot /></div>',
  props: ['label', 'prop'],
}

const ElInputStub = {
  template:
    '<input class="el-input" :value="modelValue" :type="type" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyupEnter\')" />',
  props: ['modelValue', 'type', 'placeholder', 'size', 'showPassword', 'prefixIcon'],
  emits: ['update:modelValue', 'keyupEnter'],
}

const ElButtonStub = {
  template:
    '<button class="el-button" :class="typeClass" :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
  props: ['type', 'size', 'loading'],
  computed: {
    typeClass() {
      return this.type ? `el-button--${this.type}` : ''
    },
  },
  emits: ['click'],
}

// ===== Mocks =====

const mockPush = vi.fn()
const mockQuery = vi.fn(() => ({}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
  useRoute: () => ({
    get query() {
      return mockQuery()
    },
  }),
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  setToken: vi.fn(),
  setUser: vi.fn(),
  getToken: vi.fn(() => null),
  isAuthenticated: vi.fn(() => false),
  getUser: vi.fn(() => null),
  removeToken: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual as any,
    ElMessage: { success: vi.fn(), error: vi.fn() },
  }
})

// Import mocked modules so we can reference them in tests
import { login } from '@/api/auth'
import { setToken, setUser } from '@/utils/auth'
import { ElMessage } from 'element-plus'

// ===== Helpers =====

function createWrapper() {
  return mount(LoginView, {
    global: {
      stubs: {
        'el-form': ElFormStub,
        'el-form-item': ElFormItemStub,
        'el-input': ElInputStub,
        'el-button': ElButtonStub,
        'el-icon': { template: '<span class=\"el-icon\"><slot /></span>' },
      },
    },
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockQuery.mockReturnValue({})
  })

  // --- Rendering ---

  it('should render login form with username, password fields and GitHub button', () => {
    const wrapper = createWrapper()

    expect(wrapper.find('.login-form').exists()).toBe(true)
    expect(wrapper.findAll('.el-form-item')).toHaveLength(3)
    expect(wrapper.findAll('.el-input')).toHaveLength(2)
    expect(wrapper.find('.login-btn').exists()).toBe(true)
    expect(wrapper.find('.login-btn').text()).toContain('登录')
    expect(wrapper.find('.github-btn').exists()).toBe(true)
    expect(wrapper.find('.github-btn').text()).toContain('GitHub 登录')
  })

  it('should show GitHub OAuth button with correct href', () => {
    const wrapper = createWrapper()

    const githubBtn = wrapper.find('.github-btn')
    expect(githubBtn.exists()).toBe(true)
    expect(githubBtn.attributes('href')).toBe('/api/auth/github')
  })

  it('should render two inputs (username and password)', () => {
    const wrapper = createWrapper()

    const inputs = wrapper.findAll('.el-input')
    // Two inputs: username and password
    expect(inputs).toHaveLength(2)
    // Password input has type="password"
    expect(inputs[1].attributes('type')).toBe('password')
  })

  // --- Validation ---

  it('should not call login when form validation fails', async () => {
    const wrapper = createWrapper()
    const formRef = (wrapper.vm as any).formRef

    // Override validate to reject
    if (formRef) {
      formRef.validate = vi.fn().mockRejectedValue(new Error('validation failed'))
    }

    await wrapper.find('.login-btn').trigger('click')
    await flushPromises()

    expect(login).not.toHaveBeenCalled()
  })

  // --- Login success ---

  it('should call login API and redirect on success', async () => {
    const mockResponse = {
      token: 'jwt-token-123',
      user: { id: 1, username: 'admin', role: 'admin' },
    }
    vi.mocked(login).mockResolvedValue(mockResponse as any)

    const wrapper = createWrapper()
    const vm = wrapper.vm as any

    // Fill form
    vm.form.username = 'admin'
    vm.form.password = 'password123'
    await nextTick()

    // Ensure formRef.validate resolves true
    const formRef = vm.formRef
    if (formRef) {
      formRef.validate = vi.fn().mockResolvedValue(true)
    }

    await wrapper.find('.login-btn').trigger('click')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({
      username: 'admin',
      password: 'password123',
    })
    expect(setToken).toHaveBeenCalledWith('jwt-token-123')
    expect(setUser).toHaveBeenCalledWith({ id: 1, username: 'admin', role: 'admin' })
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
    expect(mockPush).toHaveBeenCalledWith('/')
  })

  it('should redirect to query.redirect after login', async () => {
    mockQuery.mockReturnValue({ redirect: '/settings' })
    vi.mocked(login).mockResolvedValue({
      token: 'jwt-token-123',
      user: { id: 1, username: 'admin', role: 'admin' },
    } as any)

    const wrapper = createWrapper()
    const vm = wrapper.vm as any

    vm.form.username = 'admin'
    vm.form.password = 'password123'
    await nextTick()

    const formRef = vm.formRef
    if (formRef) {
      formRef.validate = vi.fn().mockResolvedValue(true)
    }

    await wrapper.find('.login-btn').trigger('click')
    await flushPromises()

    expect(mockPush).toHaveBeenCalledWith('/settings')
  })

  // --- Login failure ---

  it('should set loading false on login failure', async () => {
    vi.mocked(login).mockRejectedValue(new Error('Unauthorized'))

    const wrapper = createWrapper()
    const vm = wrapper.vm as any

    vm.form.username = 'admin'
    vm.form.password = 'wrong'
    await nextTick()

    const formRef = vm.formRef
    if (formRef) {
      formRef.validate = vi.fn().mockResolvedValue(true)
    }

    await wrapper.find('.login-btn').trigger('click')
    await flushPromises()

    expect(vm.loading).toBe(false)
    // Router should not navigate on failure
    expect(mockPush).not.toHaveBeenCalled()
  })

  // --- OAuth callback ---

  it('should handle OAuth callback with token in query params', async () => {
    mockQuery.mockReturnValue({
      token: 'oauth-token-xyz',
      username: 'githubuser',
      role: 'user',
    })

    mount(LoginView, {
      global: {
        stubs: {
          'el-form': ElFormStub,
          'el-form-item': ElFormItemStub,
          'el-input': ElInputStub,
          'el-button': ElButtonStub,
          'el-icon': { template: '<span class=\"el-icon\"><slot /></span>' },
        },
      },
    })
    await flushPromises()

    expect(setToken).toHaveBeenCalledWith('oauth-token-xyz')
    expect(setUser).toHaveBeenCalledWith({
      id: 0,
      username: 'githubuser',
      role: 'user',
    })
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
    expect(mockPush).toHaveBeenCalledWith('/')
  })

  it('should handle OAuth callback with defaults when username/role missing', async () => {
    mockQuery.mockReturnValue({
      token: 'oauth-token-xyz',
    })

    mount(LoginView, {
      global: {
        stubs: {
          'el-form': ElFormStub,
          'el-form-item': ElFormItemStub,
          'el-input': ElInputStub,
          'el-button': ElButtonStub,
          'el-icon': { template: '<span class=\"el-icon\"><slot /></span>' },
        },
      },
    })
    await flushPromises()

    expect(setUser).toHaveBeenCalledWith({
      id: 0,
      username: 'User',
      role: 'user',
    })
  })

  // --- Loading state ---

  it('should show loading state on login button while logging in', async () => {
    // Keep the login promise pending
    vi.mocked(login).mockReturnValue(new Promise(() => {}))

    const wrapper = createWrapper()
    const vm = wrapper.vm as any

    vm.form.username = 'admin'
    vm.form.password = 'password123'
    await nextTick()

    const formRef = vm.formRef
    if (formRef) {
      formRef.validate = vi.fn().mockResolvedValue(true)
    }

    await wrapper.find('.login-btn').trigger('click')
    await nextTick()

    expect(vm.loading).toBe(true)
  })
})
