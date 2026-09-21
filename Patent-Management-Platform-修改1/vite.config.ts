import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8080'
const wsProxyTarget = process.env.VITE_WS_PROXY_TARGET || 'ws://127.0.0.1:8080'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('echarts')) {
            return 'vendor-echarts'
          }

          if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) {
            return 'vendor-element-plus'
          }

          if (id.includes('@tiptap')) {
            return 'vendor-editor'
          }

          if (
            id.includes('/vue/') ||
            id.includes('vue-router') ||
            id.includes('axios')
          ) {
            return 'vendor-core'
          }

          return 'vendor-misc'
        },
      },
    },
  },
  plugins: [
    {
      name: 'spa-history-fallback',
      configureServer(server) {
        server.middlewares.use((req, _res, next) => {
          const url = req.url || ''
          const isPageRequest =
            req.method === 'GET' &&
            !url.startsWith('/api') &&
            !url.startsWith('/ws') &&
            !url.includes('.') &&
            !url.startsWith('/@')

          if (isPageRequest) {
            req.url = '/index.html'
          }
          next()
        })
      }
    },
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true
      },
      '/ws': {
        target: wsProxyTarget,
        changeOrigin: true,
        ws: true
      }
    }
  }
})
