const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// 支持传入目标目录（验证构建产物用），默认 dist
function resolveDistDir(args) {
  const outDirIndex = args.indexOf('--outDir');
  const outDirEqualsArg = args.find(arg => arg.startsWith('--outDir='));

  if (outDirIndex !== -1 && args[outDirIndex + 1]) {
    return path.resolve(args[outDirIndex + 1]);
  }

  if (outDirEqualsArg) {
    return path.resolve(outDirEqualsArg.slice('--outDir='.length));
  }

  if (args[0] && !args[0].startsWith('--')) {
    return path.resolve(args[0]);
  }

  return path.join(__dirname, '../dist');
}

const distDir = resolveDistDir(process.argv.slice(2));

// 需要压缩的文件扩展名
const compressibleExtensions = ['.js', '.css', '.html', '.json', '.svg', '.xml', '.txt'];

// 生成Brotli文件（Node 内置 zlib，不依赖外部 brotli CLI；质量 11 = 最高级别）
function generateBrotliFile(filePath) {
  const brPath = filePath + '.br';
  const fileContent = fs.readFileSync(filePath);
  const brotlied = zlib.brotliCompressSync(fileContent, {
    params: {
      [zlib.constants.BROTLI_PARAM_QUALITY]: 11,
    },
  });
  fs.writeFileSync(brPath, brotlied);

  const ratio = ((1 - brotlied.length / fileContent.length) * 100).toFixed(1);
  console.log(`✓ Brotli: ${path.relative(distDir, filePath)} (${ratio}% 减少)`);
}

// 递归获取所有文件
function getAllFiles(dir, files = []) {
  const dirFiles = fs.readdirSync(dir);
  
  for (const file of dirFiles) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      getAllFiles(filePath, files);
    } else {
      files.push(filePath);
    }
  }
  
  return files;
}

// 生成GZIP文件
function generateGzipFile(filePath) {
  const fileContent = fs.readFileSync(filePath);
  const gzipped = zlib.gzipSync(fileContent, { level: 9 });
  const gzipPath = filePath + '.gz';
  
  fs.writeFileSync(gzipPath, gzipped);
  
  const originalSize = fileContent.length;
  const compressedSize = gzipped.length;
  const ratio = ((1 - compressedSize / originalSize) * 100).toFixed(1);
  
  console.log(`✓ GZIP: ${path.relative(distDir, filePath)} (${ratio}% 减少)`);
}

// 主函数
function compressBuildFiles() {
  console.log('开始生成预压缩文件...\n');

  if (!fs.existsSync(distDir)) {
    console.error('错误: dist目录不存在！请先运行 npm run build');
    process.exit(1);
  }

  const allFiles = getAllFiles(distDir);
  const compressibleFiles = allFiles.filter(file => {
    const ext = path.extname(file);
    const size = fs.statSync(file).size;
    // 只压缩大于1KB且为可压缩类型的文件
    return compressibleExtensions.includes(ext) && size > 1024;
  });

  if (compressibleFiles.length === 0) {
    console.log('没有找到需要压缩的文件');
    return;
  }

  console.log(`找到 ${compressibleFiles.length} 个文件需要压缩\n`);

  let gzipCount = 0;
  let brotliCount = 0;
  let failedCount = 0;

  for (const file of compressibleFiles) {
    // 生成GZIP文件
    try {
      generateGzipFile(file);
      gzipCount++;
    } catch (error) {
      failedCount++;
      console.error(`✗ GZIP压缩失败: ${file} (${error.message})`);
    }

    // 生成Brotli文件
    try {
      generateBrotliFile(file);
      brotliCount++;
    } catch (error) {
      failedCount++;
      console.error(`✗ Brotli压缩失败: ${file} (${error.message})`);
    }
  }

  console.log('\n压缩完成!');
  console.log(`统计: ${gzipCount} 个 .gz 文件, ${brotliCount} 个 .br 文件`);

  if (failedCount > 0) {
    console.error(`压缩失败: ${failedCount} 个文件格式`);
    process.exitCode = 1;
  }
}

// 运行压缩
compressBuildFiles(); 