// 构建包装脚本：vite build 成功后对同一产物目录执行预压缩。
// 与 poetize-admin/scripts/build-with-compression.cjs 仅差 compress 脚本文件名（.cjs/.js），修改需两端同步。
const { spawnSync } = require('child_process');
const path = require('path');

const projectDir = path.resolve(__dirname, '..');
const viteArgs = process.argv.slice(2);
const outDirIndex = viteArgs.indexOf('--outDir');
const outDirEqualsArg = viteArgs.find((arg) => arg.startsWith('--outDir='));
let outDir = path.join(projectDir, 'dist');

if (outDirIndex !== -1 && viteArgs[outDirIndex + 1]) {
  outDir = path.resolve(projectDir, viteArgs[outDirIndex + 1]);
} else if (outDirEqualsArg) {
  outDir = path.resolve(projectDir, outDirEqualsArg.slice('--outDir='.length));
}

// vite 的 exports 未暴露 ./bin/vite.js，不能直接 require.resolve；
// 通过包内 package.json 定位包根再拼 bin 路径，兼容不同的 node_modules 布局。
const viteBin = path.join(path.dirname(require.resolve('vite/package.json')), 'bin', 'vite.js');

const viteResult = spawnSync(
  process.execPath,
  [viteBin, 'build', ...viteArgs],
  {
    cwd: projectDir,
    stdio: 'inherit',
    shell: false,
  },
);

if (viteResult.error) {
  console.error(`Vite 构建启动失败: ${viteResult.error.message}`);
  process.exit(1);
}

if (viteResult.status !== 0) {
  process.exit(viteResult.status ?? 1);
}

const compressResult = spawnSync(
  process.execPath,
  [path.join(__dirname, 'compress-build.cjs'), outDir],
  {
    cwd: projectDir,
    stdio: 'inherit',
    shell: false,
  },
);

if (compressResult.error) {
  console.error(`压缩脚本启动失败: ${compressResult.error.message}`);
  process.exit(1);
}

process.exit(compressResult.status ?? 1);
