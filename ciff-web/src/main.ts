import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'

// Design system: tokens first, then Element Plus base, then overrides, then base
import './styles/design-tokens.css'
import 'element-plus/dist/index.css'
import './styles/element-overrides.css'
import './styles/base.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(ElementPlus)
app.use(router)

app.mount('#app')
