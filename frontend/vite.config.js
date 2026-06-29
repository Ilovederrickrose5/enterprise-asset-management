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
      // 认证服务代理：转发到asset-auth服务（8081端口）
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('Auth proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('Auth Service Request:', req.method, req.url, '→ 8081');
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('Auth Service Response:', proxyRes.statusCode, req.url);
          });
        }
      },
      // 业务服务代理：转发到asset-business服务（8082端口）
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('Business proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('Business Service Request:', req.method, req.url, '→ 8082');
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('Business Service Response:', proxyRes.statusCode, req.url);
          });
        }
      }
    }
  }
})
