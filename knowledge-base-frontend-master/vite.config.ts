import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // sockjs-client depends on the Node.js global; it needs a polyfill in the browser
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['echarts', 'echarts-for-react'],
  },
  server: {
    port: 3002,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Make sure request headers are forwarded correctly
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // Log the proxied request
            console.log('🔄 Proxying:', req.method, req.url, '->', options.target + proxyReq.path);
            console.log('🔄 Request Headers:', JSON.stringify(proxyReq.getHeaders(), null, 2));

            // Make sure the Origin and Referer headers are set correctly
            proxyReq.setHeader('Origin', 'http://localhost:8080');
            proxyReq.setHeader('Referer', 'http://localhost:8080/');
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('✅ Proxy response:', proxyRes.statusCode, req.url);
            console.log('✅ Response Headers:', JSON.stringify(proxyRes.headers, null, 2));

            // Make sure the CORS headers are returned correctly
            proxyRes.headers['Access-Control-Allow-Origin'] = '*';
            proxyRes.headers['Access-Control-Allow-Methods'] = 'GET,POST,PUT,DELETE,OPTIONS';
            proxyRes.headers['Access-Control-Allow-Headers'] = 'Content-Type,Authorization,X-Requested-With,X-CSRF-TOKEN';
          });
          proxy.on('error', (err, req, res) => {
            console.error('❌ Proxy error:', err.message);
          });
          proxy.on('proxyReqWs', (proxyReq, req, socket, options, head) => {
            console.log('🔄 Proxying WebSocket:', req.url);
          });
        }
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            // Forward the client's Authorization header to the gateway
            const authHeader = req.headers['authorization'];
            if (authHeader) {
              proxyReq.setHeader('Authorization', authHeader);
            }
          });
          proxy.on('error', (err, _req, _res) => {
            console.error('❌ WebSocket proxy error:', err.message);
          });
        }
      }
    }
  }
})
