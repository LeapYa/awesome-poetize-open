# 中文字体分片工具

当前默认方案已切换为 `cn-font-split`，用于生成：

- `font.css`
- 多个细粒度 `.woff2` 分片

浏览器会基于 `@font-face + unicode-range` 自动按需下载真正命中的小分片，而不是一次性拉取超大的一级/二级汉字包。

## 推荐方案：cn-font-split

### 安装依赖

```bash
npm install
```

### 使用方法

```bash
npm run split -- ./font.ttf ./font_chunks MyAwesomeFont font.css 49152
```

参数说明：

1. 输入字体文件路径
2. 输出目录
3. `font-family` 名称，默认 `MyAwesomeFont`
4. CSS 文件名，默认 `font.css`
5. 目标分片大小，默认 `49152` 字节（48KB）

### 高级选项（环境变量）

| 环境变量 | 说明 |
|---|---|
| `CN_FONT_SPLIT_MODULE_PATH` | 手动指定 `cn-font-split` 模块入口路径（`dist/node/index.mjs`），未设置时自动使用本地 `node_modules` |
| `CN_FONT_SPLIT_WASM_PATH` | 指定 WASM 文件路径以启用 WASM 加速，通常在离线或受限环境下使用 |

**示例：使用自定义模块路径**

```bash
CN_FONT_SPLIT_MODULE_PATH=/opt/cn-font-split/dist/node/index.mjs \
  npm run split -- ./font.ttf ./font_chunks
```

**示例：启用 WASM 加速**

```bash
CN_FONT_SPLIT_WASM_PATH=./node_modules/cn-font-split/dist/node/index_bg.wasm \
  npm run split -- ./font.ttf ./font_chunks
```

> **注意**：`CN_FONT_SPLIT_MODULE_PATH` 与服务端部署保持一致，方便在 Docker 等隔离环境中统一配置。

## 旧方案（已升级）

`font_subset.py` 使用 Python + `pyftsubset` 将字体切分为固定的 4 个块，现已升级为同样生成 `font.css`（`font-family: "MyAwesomeFont"`，包含 `unicode-range`），输出与主方案兼容，`unicode_ranges.json` 作为兼容回退保留。

**依赖安装：**

```bash
pip install -r requirements.txt
```

**使用方法：**

```bash
python font_subset.py
```

将 `font.ttf` 放在当前目录，运行后在 `font_chunks/` 下生成：

- `font.base.woff2` / `font.level1.woff2` / `font.level2.woff2` / `font.other.woff2`
- `font.css`（主方案，`@font-face + unicode-range`）
- `unicode_ranges.json`（兼容回退）
