import fs from 'node:fs/promises'
import path from 'node:path'
import { fontSplit } from 'cn-font-split'

const [inputFile, outDir, fontFamily = 'MyAwesomeFont', cssFileName = 'font.css', chunkSizeArg = '49152'] = process.argv.slice(2)

if (!inputFile || !outDir) {
  console.error('Usage: npm run split -- <inputFile> <outDir> [fontFamily] [cssFileName] [chunkSize]')
  process.exit(1)
}

const chunkSize = Number.parseInt(chunkSizeArg, 10)

await fs.rm(outDir, { recursive: true, force: true })
await fs.mkdir(outDir, { recursive: true })

const input = new Uint8Array(await fs.readFile(path.resolve(inputFile)))

await fontSplit({
  input,
  outDir: path.resolve(outDir),
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
})

for (const extraFile of ['index.html', 'index.proto', 'reporter.bin']) {
  await fs.rm(path.resolve(outDir, extraFile), { force: true })
}

const fileNames = (await fs.readdir(outDir)).sort()
console.log(JSON.stringify({
  cssFile: cssFileName,
  chunkCount: fileNames.filter((name) => name.endsWith('.woff2')).length,
  files: fileNames,
}, null, 2))