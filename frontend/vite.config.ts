import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 开发期代理（04-详细设计 v1.4 §5.5）：/api 同域代理到本地后端 8080，
// 前端代码始终以相对路径 /api/** 请求，与生产 Nginx 同域反代行为一致；
// http-proxy 对 SSE 为流式转发，无需额外关缓冲配置。
// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
