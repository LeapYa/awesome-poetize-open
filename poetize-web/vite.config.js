import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteCompression from 'vite-plugin-compression'
import path from 'path'
import { fileURLToPath } from 'url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需自动导入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router'],
      dts: false, // 不生成 d.ts 文件
    }),
    Components({
      resolvers: [
        // 配置 ElementPlusResolver 不自动导入图标
        // 图标由项目自行手动导入和注册
        ElementPlusResolver({
          importStyle: 'css',
          // 排除图标组件的自动导入（以 ElIcon 开头但不是 ElIcon 本身的）
          exclude: /^ElIcon(?!$)/,
        }),
      ],
      dts: false, // 不生成 d.ts 文件
    }),
    // Gzip compression
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 8192,
      deleteOriginFile: false
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      'static': path.resolve(__dirname, 'public')
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  server: {
    port: 5173,
    host: true,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/socket': {
        target: 'ws://localhost:9324',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false,
    minify: 'esbuild', // 使用 esbuild 替代 terser，内存效率更高
    chunkSizeWarningLimit: 2000, // 提高 chunk 大小警告阈值
    modulePreload: {
      polyfill: false, // 禁用 modulePreload polyfill
    },
    cssCodeSplit: true, // 保持 CSS 代码分割
    rollupOptions: {
      output: {
        // 确保动态导入的模块不会被意外合并
        inlineDynamicImports: false,
        manualChunks(id) {
          if (id.includes('node_modules')) {
            // 核心框架库 (仅 Vue 相关)
            if (id.match(/[\\/]node_modules[\\/](vue|@vue|pinia|vue-router)/)) {
              return 'framework';
            }
            // Element Plus 图标库
            if (id.includes('@element-plus/icons-vue')) {
              return 'element-plus-icons';
            }
            // UI 组件库 (Element Plus)
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'element-plus';
            }
            // ECharts 图表库
            if (id.includes('echarts') || id.includes('zrender')) {
              return 'echarts';
            }
            // Vditor 编辑器
            if (id.includes('vditor')) {
              return 'vditor';
            }
            // 数学公式库
            if (id.includes('katex')) {
              return 'katex';
            }
            // Mermaid 及其依赖 - 不指定 chunk，让 Vite 自动处理动态导入
            // 这些库只通过动态 import() 加载，不需要预先打包
            if (id.includes('mermaid') || id.includes('cytoscape') || id.includes('elkjs')) {
              return; // 返回 undefined，不分配到任何 manual chunk
            }
            // 其他 node_modules 库也不分配到 vendors，让它们合并到使用它们的页面 chunk 中
          }
        }
      }
    }
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      'axios'
    ]
  }
})
