const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { execSync } = require('child_process');

const distDir = path.join(__dirname, '../dist');

// 需要压缩的文件扩展名
const compressibleExtensions = ['.js', '.css', '.html', '.json', '.svg', '.xml', '.txt'];

// 检查是否安装了brotli命令行工具
function hasBrotliCLI() {
  try {
    execSync('brotli --version', { stdio: 'ignore' });
    return true;
  } catch (error) {
    console.warn('警告: brotli命令行工具未安装，跳过.br文件生成');
    console.warn('提示: 可通过 npm install -g brotli 安装');
    return false;
  }
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

// 生成Brotli文件
function generateBrotliFile(filePath) {
  try {
    const brPath = filePath + '.br';
    // 使用最高压缩级别11
    execSync(`brotli --quality=11 --output="${brPath}" "${filePath}"`, { stdio: 'ignore' });
    
    const originalSize = fs.statSync(filePath).size;
    const compressedSize = fs.statSync(brPath).size;
    const ratio = ((1 - compressedSize / originalSize) * 100).toFixed(1);
    
    console.log(`✓ Brotli: ${path.relative(distDir, filePath)} (${ratio}% 减少)`);
  } catch (error) {
    console.error(`✗ Brotli压缩失败: ${filePath}`);
  }
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
  
  const hasBrotli = hasBrotliCLI();
  let gzipCount = 0;
  let brotliCount = 0;
  
  for (const file of compressibleFiles) {
    // 生成GZIP文件
    try {
      generateGzipFile(file);
      gzipCount++;
    } catch (error) {
      console.error(`✗ GZIP压缩失败: ${file}`);
    }
    
    // 生成Brotli文件（如果可用）
    if (hasBrotli) {
      try {
        generateBrotliFile(file);
        brotliCount++;
      } catch (error) {
        console.error(`✗ Brotli压缩失败: ${file}`);
      }
    }
  }
  
  console.log('\n压缩完成!');
  console.log(`统计: ${gzipCount} 个 .gz 文件, ${brotliCount} 个 .br 文件`);
  
  if (!hasBrotli) {
    console.log('\n提示: 安装 brotli 命令行工具可获得更好的压缩效果:');
    console.log('   npm install -g brotli');
  }
}

// 运行压缩
compressBuildFiles(); 