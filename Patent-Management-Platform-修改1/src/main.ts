import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'

const savedTheme = localStorage.getItem('theme')
if (!savedTheme) {
  document.documentElement.setAttribute('data-theme', 'light')
}

const app = createApp(App)

app.use(router)

app.mount('#app')
