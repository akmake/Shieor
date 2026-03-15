import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'], // קבצים סטטיים מהתיקייה public
      manifest: {
        name: 'Shieor - לימוד יומי',
        short_name: 'Shieor',
        description: 'אפליקציה ללימוד יומי: חומש, רמב"ם, תניא ושניים מקרא',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone', // גורם לאתר להיראות כמו אפליקציה עצמאית בלי שורת כתובת
        dir: 'rtl',
        lang: 'he',
        icons: [
          {
            src: '/pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable'
          }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/three') || id.includes('@react-three') || id.includes('/three/')) {
            return 'three-vendor';
          }
          if (id.includes('node_modules/jspdf')) {
            return 'pdf-vendor';
          }
          if (id.includes('node_modules/file-saver')) {
            return 'file-vendor';
          }
          if (id.includes('node_modules/react') || id.includes('node_modules/react-dom') || id.includes('node_modules/react-router')) {
            return 'react-vendor';
          }
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:5000',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});