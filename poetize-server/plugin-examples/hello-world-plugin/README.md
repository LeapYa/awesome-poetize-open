# Hello World 示例插件

这是一个演示 **Poetize 插件系统** 各项能力的参考实现。

## 目录结构

```
hello-world-plugin/
├── manifest.json           必须：插件元数据
├── frontend/
│   ├── index.js            可选：前端 JS（通过 PoetizePlugin SDK 运行）
│   └── style.css           可选：前端 CSS（自动注入 <head>）
├── backend/
│   └── main.groovy         可选：后端脚本（Groovy）
└── sql/
    ├── install.sql         可选：安装时执行的 SQL
    └── uninstall.sql       可选：卸载时执行的 SQL
```

## 打包方法

在 `hello-world-plugin/` 目录下执行：

```bash
# Windows
Compress-Archive -Path * -DestinationPath ../hello-world-plugin-1.0.0.zip

# Linux / macOS
zip -r ../hello-world-plugin-1.0.0.zip .
```

## 安装方法

1. 登录后台管理 → 插件管理
2. 点击「安装插件」按钮或将 `.zip` 文件拖入上传区
3. 预览 manifest 后点击「确认安装」
4. 刷新页面查看效果

## 钩子说明

| 钩子 | 触发时机 | ctx 字段 |
|------|----------|----------|
| `onArticleRender` | 前端文章内容 DOM 渲染完成 | `articleId`, `title`, `element` |
| `onArticleSave` | 后端文章保存成功 | `articleId`, `title`, `userId` |
| `onCommentPublish` | 后端评论入库成功 | `commentId`, `articleId`, `userId`, `content` |
| `onUserRegister` | 后端新用户注册完成 | `userId`, `username` |

## 效果预览

- **前端**：文章底部出现一个带蓝色左边框的打招呼横幅
- **后端**：插件日志（日志级别 INFO）在 Spring Boot 控制台输出事件记录
