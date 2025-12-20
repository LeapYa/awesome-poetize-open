import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue2';
import path from 'path';
import viteCompression from 'vite-plugin-compression';
import envCompatible from 'vite-plugin-env-compatible';

export default defineConfig({
    plugins: [
        vue(),
        viteCompression({
            algorithm: 'gzip',
            threshold: 8192,
        }),
        envCompatible(),
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src'),
            'static': path.resolve(__dirname, 'public'),
            'element-ui': 'element-ui-ce',
        },
        extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
    },
    server: {
        port: 80,
        host: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8081', // 默认后端端口，根据实际需要调整
                changeOrigin: true,
            },
        },
    },
    define: {
        'process.env': {}
    },
    build: {
        chunkSizeWarningLimit: 2000,
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        if (id.includes('element-ui-ce')) return 'element-ui';
                        if (id.includes('highlight.js')) return 'highlight.js';
                        if (id.includes('vditor')) return 'vditor';
                        if (id.includes('mermaid')) return 'mermaid';
                        return 'vendors';
                    }
                },
            },
        },
    },
});
