import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const [inputFile, outDir, fontFamily = 'MyAwesomeFont', cssFileName = 'font.css', chunkSizeArg = '49152'] = process.argv.slice(2)

if (!inputFile || !outDir) {
  console.error('Usage: npm run split -- <inputFile> <outDir> [fontFamily] [cssFileName] [chunkSize]')
  process.exit(1)
}

// 可选：通过环境变量指定 cn-font-split 模块路径（与服务端部署保持一致；未设置则直接使用本地 node_modules）
const modulePath = process.env.CN_FONT_SPLIT_MODULE_PATH
// 可选：通过环境变量指定 WASM 文件路径，以启用 WASM 加速（如 cn-font-split 的 .wasm 产物）
const wasmPath = process.env.CN_FONT_SPLIT_WASM_PATH

let cnFontSplitModule
if (modulePath) {
  const normalizedModulePath = path.resolve(modulePath)
  cnFontSplitModule = await import(pathToFileURL(normalizedModulePath).href)
} else {
  cnFontSplitModule = await import('cn-font-split')
}

const chunkSize = Number.parseInt(chunkSizeArg, 10)

const resolvedOutDir = path.resolve(outDir)
await fs.rm(resolvedOutDir, { recursive: true, force: true })
await fs.mkdir(resolvedOutDir, { recursive: true })

const input = new Uint8Array(await fs.readFile(path.resolve(inputFile)))

const fontSplitOptions = {
  input,
  outDir: resolvedOutDir,
  css: {
    fontFamily,
    fontDisplay: 'swap',
    fileName: cssFileName,
    compress: true,
    commentBase: false,
    commentNameTable: false,
    commentUnicodes: false,
  },
  languageAreas: true,
  autoSubset: true,
  reduceMins: true,
  chunkSize: Number.isFinite(chunkSize) && chunkSize > 0 ? chunkSize : 48 * 1024,
  chunkSizeTolerance: 8 * 1024,
  renameOutputFont: '[hash:8].[ext]',
  testHtml: false,
  reporter: false,
  silent: true,
}

if (wasmPath) {
  // WASM 加速路径：使用 StaticWasm 句柄调用 fontSplit
  const { fontSplit, StaticWasm } = cnFontSplitModule
  if (typeof fontSplit !== 'function' || typeof StaticWasm !== 'function') {
    throw new Error(`WASM 模块未暴露预期 API (fontSplit / StaticWasm)，请确认 WASM 版本与 cn-font-split 版本匹配`)
  }

  const wasmBuffer = new Uint8Array(await fs.readFile(path.resolve(wasmPath)))
  const wasm = new StaticWasm(wasmBuffer)
  const outputs = await fontSplit(fontSplitOptions, wasm.WasiHandle, {
    logger() {},
  })

  await Promise.all(
    outputs
      .filter(Boolean)
      .map((file) => fs.writeFile(path.join(resolvedOutDir, file.name), file.data)),
  )
} else {
  // 默认路径：直接调用 fontSplit
  const { fontSplit } = cnFontSplitModule
  if (typeof fontSplit !== 'function') {
    throw new Error(`模块未暴露 fontSplit() 函数，请检查 cn-font-split 安装是否正确`)
  }

  await fontSplit(fontSplitOptions)
}

for (const extraFile of ['index.html', 'index.proto', 'reporter.bin']) {
  await fs.rm(path.join(resolvedOutDir, extraFile), { force: true })
}

const fileNames = (await fs.readdir(resolvedOutDir)).sort()
console.log(JSON.stringify({
  cssFile: cssFileName,
  chunkCount: fileNames.filter((name) => name.endsWith('.woff2')).length,
  files: fileNames,
}, null, 2))