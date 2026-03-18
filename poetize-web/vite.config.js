import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteCompression from 'vite-plugin-compression'
import JavaScriptObfuscator from 'javascript-obfuscator'
import path from 'path'
import { fileURLToPath } from 'url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function captchaObfuscator() {
  return {
    name: 'captcha-obfuscator',
    apply: 'build',
    enforce: 'post',
    generateBundle(_options, bundle) {
      for (const fileName of Object.keys(bundle)) {
        const chunk = bundle[fileName]
        if (chunk.type !== 'chunk') continue
        if (!/^(static\/)?captcha-core-[\w-]+\.js$/.test(fileName)) continue

        const result = JavaScriptObfuscator.obfuscate(chunk.code, {
          compact: true,
          disableConsoleOutput: true,
          identifierNamesGenerator: 'hexadecimal',
          log: false,
          renameGlobals: false,
          rotateStringArray: true,
          stringArray: true,
          stringArrayEncoding: ['base64'],
          stringArrayThreshold: 0.35,
          unicodeEscapeSequence: false,
        })

        chunk.code = result.getObfuscatedCode()
      }
    },
  }
}

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
      globs: [
        'src/components/**/*.vue',
        '!src/components/im/common/commentBox.vue',
        '!src/components/im/common/emoji.vue',
        '!src/components/im/common/proButton.vue',
        '!src/components/im/common/uploadPicture.vue',
      ],
      resolvers: [
        // 配置 ElementPlusResolver 不自动导入图标
        // 图标由项目自行手动导入和注册
        ElementPlusResolver({
          importStyle: 'css',
          // 排除图标组件的自动导入（以 ElIcon 开头但不是 ElIcon 本身的）
          exclude: /^ElIcon(?!$)/,
        }),
      ],
      excludeNames: [
        /^(AsyncNotification|CaptchaContainer)$/,
        /^(Loader|Zombie|Printer|SortArticle|MyAside|Danmaku|Card|Process|VideoPlayer|Emoji|ProButton|UploadPicture|Live2DTips|Live2DCanvas|Live2DToolbar|Live2DToggle)$/,
      ],
      dts: false, // 不生成 d.ts 文件
    }),
    // Gzip compression
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 8192,
      deleteOriginFile: false
    }),
    captchaObfuscator(),
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
      '/ws/im': {
        target: 'ws://localhost:8081',
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
    chunkSizeWarningLimit: 2000, // 提高 chunk 大小警告阈值
    modulePreload: {
      polyfill: false, // 禁用 modulePreload polyfill
    },
    cssCodeSplit: true, // 保持 CSS 代码分割
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (
            normalizedId.includes('/src/utils/captchaUtil.js') ||
            normalizedId.includes('/src/utils/fingerprintUtil.js')
          ) {
            return 'captcha-core'
          }
          if (id.includes('node_modules')) {
            // 核心框架库 (仅 Vue 相关)
            if (id.match(/[\\/]node_modules[\\/](vue|@vue|pinia|vue-router)/)) {
              return 'framework';
            }
            // Element Plus 在不同路由和异步组件中复用很多。
            // 不再强制合并成单个超大共享 chunk，让 Vite / Rolldown
            // 根据实际引用关系自动拆分，减少文章页、首页等首屏共享负担。
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return;
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
            // 代码高亮库通过动态 import() 按需加载，交由 Vite/Rolldown 自动分块
            // 避免手动分块后在 Vite 8 / Rolldown 下出现导出绑定异常
            if (id.includes('highlight.js') || id.includes('highlightjs-line-numbers')) {
              return;
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
