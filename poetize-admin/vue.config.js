const CompressionPlugin = require('compression-webpack-plugin')
const path = require('path')
const TerserPlugin = require('terser-webpack-plugin')
const { WebpackManifestPlugin } = require('webpack-manifest-plugin')
const { WebpackBundleAnalyzer } = require('webpack-bundle-analyzer')

// 设置网站默认标题（实际运行时会从数据库获取）
const siteTitle = '';

module.exports = {
  devServer: {
    port: 5174,
    https: false,
    open: false
  },
  lintOnSave: false,
  productionSourceMap: false,
  // 让babel转译mermaid模块（使用了ES2020+语法）
  transpileDependencies: ['mermaid'],
  chainWebpack: config => {
    // 禁用默认的预加载和预取，减少不必要的资源
    config.plugins.delete('prefetch')
    config.plugins.delete('preload')

    config
      .plugin('html')
      .tap(args => {
        args[0].title = siteTitle
        // 使用默认的自动注入
        args[0].inject = true
        // 启用 HTML 压缩
        if (process.env.NODE_ENV === 'production') {
          args[0].minify = {
            removeComments: true,
            collapseWhitespace: true,
            removeAttributeQuotes: true,
            collapseBooleanAttributes: true,
            removeScriptTypeAttributes: true,
            removeStyleLinkTypeAttributes: true,
            minifyCSS: true,
            minifyJS: true,
            removeRedundantAttributes: true,
            useShortDoctype: true,
            removeEmptyAttributes: true,
            removeOptionalTags: false,
            minifyURLs: true
          }
        }
        return args
      })

    config.optimization.splitChunks({
      cacheGroups: {
        // 将node_modules中的模块打包到vendors~app.js中
        vendors: {
          name: 'chunk-vendors',
          test: /[\\/]node_modules[\\/]/,
          chunks: 'initial',
          priority: 10,
          reuseExistingChunk: true,
          enforce: true
        },
        // 将element-ui单独打包
        elementUI: {
          name: 'chunk-elementUI',
          priority: 20, // 权重必须大于 vendors
          test: /[\\/]node_modules[\\/]_?element-ui-ce(.*)/,
          chunks: 'all'
        },
        // 将highlight.js单独打包
        highlightJS: {
          name: 'chunk-highlightJS',
          priority: 20,
          test: /[\\/]node_modules[\\/]_?highlight.js(.*)/,
          chunks: 'all'
        },
        // 将vditor单独打包（编辑器体积较大，分离后可以按需加载）
        vditor: {
          name: 'chunk-vditor',
          priority: 20,
          test: /[\\/]node_modules[\\/]_?vditor(.*)/,
          chunks: 'all'
        }
      }
    })

    // 启用打包分析工具
    if (process.env.npm_config_report) {
      config.plugin('bundle-analyzer').use(WebpackBundleAnalyzer)
    }
  },
  configureWebpack: {
    plugins: [
      new CompressionPlugin({
        algorithm: 'gzip',
        test: /\.js$|\.html$|\.css$/,
        filename: '[path].gz[query]',
        minRatio: 0.8,
        threshold: 8192,
        deleteOriginalAssets: false
      }),
      new WebpackManifestPlugin({
        fileName: 'manifest.json',
        publicPath: '/',
        filter: (file) => !file.path.endsWith('.gz') // 不包含压缩文件
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        'static': path.resolve(__dirname, 'public')
      }
    },
    optimization: {
      minimize: process.env.VUE_APP_PRODUCTION_MODE === 'true',
      minimizer: [
        new TerserPlugin({
          terserOptions: {
            compress: {
              // 统一由 VUE_APP_PRODUCTION_MODE 控制（反调试、混淆、日志移除）
              drop_console: process.env.VUE_APP_PRODUCTION_MODE === 'true',
              drop_debugger: process.env.VUE_APP_PRODUCTION_MODE === 'true'
            },
            mangle: process.env.VUE_APP_PRODUCTION_MODE === 'true', // 变量名混淆
            keep_fnames: process.env.VUE_APP_PRODUCTION_MODE !== 'true' // 保留函数名
          }
        })
      ]
    }
  },
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static'
}
