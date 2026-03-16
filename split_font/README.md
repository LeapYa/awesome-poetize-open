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

## 旧方案

`font_subset.py` 仍保留为旧版 4 大块切分工具，仅用于兼容和对比，不再作为默认推荐方案。
