import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
// 微服务架构：多代理配置
// /api/auth → asset-auth服务（8081端口）
// 其他/api → asset-business服务（8082端口）
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 用户/角色由 asset-auth 直接提供（管理页面需要写操作）
      '/api/users': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/roles': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/departments': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      // 认证服务代理：转发到asset-auth服务（8081端口）
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            console.error('Auth proxy error', err.message);
          });
        }
      },
      // 业务服务代理：转发到asset-business服务（8082端口）
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            console.error('Business proxy error', err.message);
          });
        }
      }
    }
  }
})
