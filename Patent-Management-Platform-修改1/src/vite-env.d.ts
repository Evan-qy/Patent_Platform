/// <reference types="vite/client" />

// Vue组件类型声明
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_AI_MODEL?: string
  // 在这里添加其他环境变量
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
