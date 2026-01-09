/**
 * 修复 element-ui-ce 包的 package.json 入口问题
 * 
 * 问题：element-ui-ce 的 package.json 声明 main 为 "lib/element-ui.common.js"
 * 但实际文件名是 "lib/element-ui-ce.common.js"
 * 这导致 Vite 无法正确解析包入口
 */

const fs = require('fs');
const path = require('path');

const packageJsonPath = path.join(__dirname, '../node_modules/element-ui-ce/package.json');

// 检查文件是否存在
if (!fs.existsSync(packageJsonPath)) {
    console.log('[INFO] element-ui-ce 未安装，跳过修复');
    process.exit(0);
}

try {
    // 读取 package.json
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

    // 检查是否需要修复
    const wrongMain = 'lib/element-ui.common.js';
    const correctMain = 'lib/element-ui-ce.common.js';

    if (packageJson.main === wrongMain) {
        // 检查正确的文件是否存在
        const correctFilePath = path.join(__dirname, '../node_modules/element-ui-ce', correctMain);
        if (fs.existsSync(correctFilePath)) {
            // 修复 main 字段
            packageJson.main = correctMain;

            // 写回 package.json
            fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
            console.log('[SUCCESS] 已修复 element-ui-ce package.json');
            console.log(`   main: "${wrongMain}" -> "${correctMain}"`);
        } else {
            console.error('[ERROR] 正确的入口文件不存在:', correctFilePath);
            process.exit(1);
        }
    } else if (packageJson.main === correctMain) {
        console.log('[INFO] element-ui-ce package.json 已经是正确的，无需修复');
    } else {
        console.log('[INFO] element-ui-ce main 字段值为:', packageJson.main);
        console.log('[INFO] 跳过修复');
    }
} catch (error) {
    console.error('[ERROR] 修复失败:', error.message);
    process.exit(1);
}
