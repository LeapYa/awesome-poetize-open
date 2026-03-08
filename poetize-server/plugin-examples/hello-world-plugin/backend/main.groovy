/**
 * Hello World 示例插件 - 后端 Groovy 脚本
 *
 * 可用的 ctx 字段（由 PluginHookManager 注入）：
 *   onArticleSave  → articleId, title, userId
 *   onCommentPublish → commentId, articleId, userId, content
 *   onUserRegister → userId, username
 *
 * 可以使用：
 *   log (SLF4J Logger)
 */

/**
 * 文章保存钩子
 * 每次有文章被保存时触发（新建或更新）
 */
def onArticleSave(ctx) {
    log.info("[HelloWorldPlugin] 文章已保存 → ID: {}, 标题: {}, 作者ID: {}",
        ctx.articleId, ctx.title, ctx.userId)
}

/**
 * 评论发布钩子
 * 每次有新评论发布时触发
 */
def onCommentPublish(ctx) {
    // 截取评论内容预览（最多 50 字）
    def preview = ctx.content
    if (preview && preview.length() > 50) {
        preview = preview.substring(0, 50) + '...'
    }
    log.info("[HelloWorldPlugin] 新评论 → 评论ID: {}, 文章ID: {}, 用户ID: {}, 内容预览: {}",
        ctx.commentId, ctx.articleId, ctx.userId, preview)
}

/**
 * 用户注册钩子
 * 每次新用户注册成功时触发
 */
def onUserRegister(ctx) {
    log.info("[HelloWorldPlugin] 新用户注册 → 用户ID: {}, 用户名: {}",
        ctx.userId, ctx.username)
}
