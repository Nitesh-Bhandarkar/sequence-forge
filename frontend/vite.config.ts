import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/dev': { target: 'http://localhost:8080', changeOrigin: true },
      // ai chat SSE stream — must be proxied separately to disable buffering
      '/api/v1/ai/chat': { target: 'http://localhost:8080', changeOrigin: true },
      '/oauth2/authorization': { target: 'http://localhost:8080', changeOrigin: true },
      '/login/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
