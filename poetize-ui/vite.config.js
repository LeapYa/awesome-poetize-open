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
        // 确保 element-ui 和 app 使用同一个 Vue 实例
        dedupe: ['vue'],
        alias: {
            // 使用 runtime.common.js (CJS) 确保与 Element-UI (CJS) 的兼容性
            // 同时也解决了 Vite 开发/生产环境的双实例问题
            'vue': 'vue/dist/vue.runtime.common.js',
            '@': path.resolve(__dirname, 'src'),
            'static': path.resolve(__dirname, 'public'),
            'element-ui': 'element-ui-ce',
            // 排除 axios Node.js 专用模块
            './lib/adapters/http.js': path.resolve(__dirname, 'src/utils/empty-module.js'),
        },
        extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
        // 解决 element-ui-ce 入口解析问题
        mainFields: ['browser', 'module', 'jsnext:main', 'jsnext', 'main'],
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
        sourcemap: false, // 禁用 source map 以减少内存消耗
        minify: 'esbuild', // 使用 esbuild 替代 terser，内存效率更高
        assetsDir: 'static', // 静态资源输出目录，与原 Vue CLI 配置一致
        chunkSizeWarningLimit: 2000,
        // 处理 CommonJS 模块兼容性
        commonjsOptions: {
            transformMixedEsModules: true,
            requireReturnsDefault: 'auto',
        },
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        if (id.includes('highlight.js')) return 'highlight';
                        if (id.includes('vditor')) return 'vditor';
                        if (id.includes('mermaid')) return 'mermaid';
                        return 'vendors';
                    }
                },
            },
        },
    },
});
