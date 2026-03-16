import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const [inputFile, outDir, fontFamily = 'MyAwesomeFont', cssFileName = 'font.css', chunkSizeArg = '49152'] = process.argv.slice(2)

if (!inputFile || !outDir) {
  console.error('Usage: node cn-font-split-runner.mjs <inputFile> <outDir> [fontFamily] [cssFileName] [chunkSize]')
  process.exit(1)
}

const modulePath = process.env.CN_FONT_SPLIT_MODULE_PATH
const wasmPath = process.env.CN_FONT_SPLIT_WASM_PATH

if (!modulePath) {
  console.error('Missing CN_FONT_SPLIT_MODULE_PATH environment variable')
  process.exit(1)
}

const normalizedModulePath = path.resolve(modulePath)
const cnFontSplitModule = await import(pathToFileURL(normalizedModulePath).href)

const chunkSize = Number.parseInt(chunkSizeArg, 10)

await fs.rm(outDir, { recursive: true, force: true })
await fs.mkdir(outDir, { recursive: true })

const input = new Uint8Array(await fs.readFile(inputFile))

const fontSplitOptions = {
  input,
  outDir,
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
  const { fontSplit, StaticWasm } = cnFontSplitModule
  if (typeof fontSplit !== 'function' || typeof StaticWasm !== 'function') {
    throw new Error(`WASM module does not expose expected API: ${normalizedModulePath}`)
  }

  const wasmBuffer = new Uint8Array(await fs.readFile(path.resolve(wasmPath)))
  const wasm = new StaticWasm(wasmBuffer)
  const outputs = await fontSplit(fontSplitOptions, wasm.WasiHandle, {
    logger() {},
  })

  await Promise.all(outputs.filter(Boolean).map((file) => fs.writeFile(path.join(outDir, file.name), file.data)))
} else {
  const { fontSplit } = cnFontSplitModule
  if (typeof fontSplit !== 'function') {
    throw new Error(`Module does not expose fontSplit(): ${normalizedModulePath}`)
  }

  await fontSplit(fontSplitOptions)
}

for (const extraFile of ['index.html', 'index.proto', 'reporter.bin']) {
  await fs.rm(path.join(outDir, extraFile), { force: true })
}

const fileNames = (await fs.readdir(outDir)).sort()
console.log(JSON.stringify({
  cssFile: cssFileName,
  chunkCount: fileNames.filter((name) => name.endsWith('.woff2')).length,
  files: fileNames,
}))