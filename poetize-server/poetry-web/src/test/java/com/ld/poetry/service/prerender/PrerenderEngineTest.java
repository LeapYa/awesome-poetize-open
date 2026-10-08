package com.ld.poetry.service.prerender;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrerenderEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void renderMarkdownMatchesFrontEndSoftBreaksWithoutAutolinkingBareUrls() {
        PrerenderEngine engine = createEngine();

        String markdown = "Line 1\nLine 2\n\nhttps://example.com\n\n| a | b |\n| - | - |\n| 1 | 2 |";
        String html = engine.renderMarkdown(markdown);

        assertTrue(html.contains("Line 1<br />\nLine 2"));
        assertTrue(html.contains("<p>https://example.com</p>"));
        assertFalse(html.contains("<a href=\"https://example.com\">https://example.com</a>"));
        assertTrue(html.contains("<table>"));
    }

    @Test
    void renderMarkdownDecoratesLinksWithTargetAndNofollow() {
        PrerenderEngine engine = createEngine();

        String markdown = "[外链](https://other.com/a) [站内相对](/article/1) [站内绝对](https://mysite.com/article/2) [锚点](#section)";
        String html = engine.renderMarkdown(markdown, "https://mysite.com");

        // 外链：新标签页 + nofollow 防权重稀释
        assertTrue(html.contains("<a href=\"https://other.com/a\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">外链</a>"));
        // 内链：新标签页但不加 nofollow，保留权重传递
        assertTrue(html.contains("<a href=\"/article/1\" target=\"_blank\" rel=\"noopener noreferrer\">站内相对</a>"));
        assertTrue(html.contains("<a href=\"https://mysite.com/article/2\" target=\"_blank\" rel=\"noopener noreferrer\">站内绝对</a>"));
        // 页内锚点保持原样
        assertTrue(html.contains("<a href=\"#section\">锚点</a>"));
    }

    @Test
    void replaceTitlePreservesQuotes() {
        PrerenderEngine engine = createEngine();
        String htmlTemplate = "<html><head><title>Old</title></head><body></body></html>";
        String title = "测试“双引号”与[方括号]是否会被转义 & < >";
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(engine, "replaceTitle", htmlTemplate, title);
        assertTrue(result.contains("<title>测试“双引号”与[方括号]是否会被转义 &amp; &lt; &gt;</title>"));
    }

    @Test
    void buildPageInjectsSeoTagsAndRenderedContent() throws IOException {
        Path templatePath = tempDir.resolve("index.html");
        Files.writeString(templatePath, """
                <!doctype html>
                <html>
                <head>
                  <meta name="description" content="old">
                  <link rel="icon" id="default-favicon" href="/favicon.ico">
                  <title>Old</title>
                </head>
                <body>
                  <div id="app"></div>
                </body>
                </html>
                """, StandardCharsets.UTF_8);

        PrerenderEngine engine = createEngine();
        ReflectionTestUtils.setField(engine, "templatePath", templatePath.toString());
        ReflectionTestUtils.setField(engine, "outputRoot", tempDir.resolve("prerender").toString());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("description", "new description");
        meta.put("canonical", "https://example.com/article/1");
        meta.put("site_icon", "/logo.png");
        meta.put("structured_data", "{\"@type\":\"WebSite\"}");
        meta.put("google_site_verification", "verify-token");
        meta.put("custom_head_code", "<meta name=\"custom-head\" content=\"1\">");

        String html = engine.buildPage(PrerenderPageData.builder()
                .title("Article Title")
                .meta(meta)
                .content("<section>hello</section>")
                .lang("en")
                .pageType("article")
                .build());

        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<title>Article Title</title>"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://example.com/article/1\">"));
        assertTrue(html.contains("href=\"/logo.png\""));
        assertTrue(html.contains("rel=\"icon\""));
        assertTrue(html.contains("sizes=\"16x16 32x32 48x48\""));
        assertTrue(html.contains("type=\"image/png\""));
        assertTrue(html.contains("<script type=\"application/ld+json\" data-prerender-structured-data=\"true\">{\"@type\":\"WebSite\"}</script>"));
        // 站点验证标签仅首页输出：文章页（子页面）不再携带，避免冗余
        assertFalse(html.contains("google-site-verification"),
                "文章页不应输出站点验证标签");
        assertTrue(html.contains("<meta name=\"custom-head\" content=\"1\">"));
        assertTrue(html.contains("<body data-prerender-type=\"article\" data-prerender-lang=\"en\">"));
        assertTrue(html.contains("<div id=\"prerender-container\" class=\"article-detail\"><main><article><section>hello</section></article></main></div>"));
        assertTrue(html.contains("<div id=\"app\"></div>"));
    }

    @Test
    void buildHomePageEmitsSiteVerificationTags() throws IOException {
        Path templatePath = tempDir.resolve("index.html");
        Files.writeString(templatePath, """
                <!doctype html>
                <html>
                <head>
                  <title>Old</title>
                </head>
                <body>
                  <div id="app"></div>
                </body>
                </html>
                """, StandardCharsets.UTF_8);

        PrerenderEngine engine = createEngine();
        ReflectionTestUtils.setField(engine, "templatePath", templatePath.toString());
        ReflectionTestUtils.setField(engine, "outputRoot", tempDir.resolve("prerender").toString());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("description", "home description");
        meta.put("google_site_verification", "google-token");
        meta.put("baidu_site_verification", "baidu-token");

        String html = engine.buildPage(PrerenderPageData.builder()
                .title("Home")
                .meta(meta)
                .content("<section>home</section>")
                .lang("zh")
                .pageType("home")
                .build());

        // 首页必须输出所有已填写的验证标签
        assertTrue(html.contains("<meta name=\"google-site-verification\" content=\"google-token\">"));
        assertTrue(html.contains("<meta name=\"baidu-site-verification\" content=\"baidu-token\">"));
        // 其余公共 SEO meta 行为不变
        assertTrue(html.contains("<meta name=\"description\" content=\"home description\">"));
    }

    @Test
    void writeAndDeleteRenderedPagesUseExpectedPaths() throws IOException {
        PrerenderEngine engine = createEngine();
        Path outputRoot = tempDir.resolve("prerender");
        ReflectionTestUtils.setField(engine, "outputRoot", outputRoot.toString());

        engine.writePage("article/42", "en", "<html>article</html>");
        engine.writePage("sort", "zh", "<html>sort</html>");

        Path articlePath = outputRoot.resolve("article/42/index-en.html");
        Path sortIndexPath = outputRoot.resolve("sort/index.html");
        assertTrue(Files.exists(articlePath));
        assertTrue(Files.exists(sortIndexPath));

        engine.deleteIndexFiles("sort");
        assertFalse(Files.exists(sortIndexPath));

        engine.deletePage("article/42");
        assertFalse(Files.exists(articlePath));
    }

    @Test
    void buildPageClearsResidualPbBootstrapPlaceholder() throws IOException {
        Path templatePath = tempDir.resolve("index.html");
        Files.writeString(templatePath, """
                <!doctype html>
                <html>
                <head>
                  <title>Old</title>
                </head>
                <body>
                  <!--PB_BOOTSTRAP-->
                  <div id="app"></div>
                </body>
                </html>
                """, StandardCharsets.UTF_8);

        PrerenderEngine engine = createEngine();
        ReflectionTestUtils.setField(engine, "templatePath", templatePath.toString());
        ReflectionTestUtils.setField(engine, "outputRoot", tempDir.resolve("prerender").toString());

        String html = engine.buildPage(PrerenderPageData.builder()
                .title("Home")
                .content("<section>hello</section>")
                .lang("zh")
                .pageType("home")
                .build());

        assertFalse(html.contains(PluginBootstrapMaterializer.PLUGIN_BOOTSTRAP_PLACEHOLDER),
                "残留的 PB 占位符必须被清除，避免输出到预渲染 HTML");
        assertTrue(html.contains("<title>Home</title>"));
    }

    @Test
    void renderMarkdownConvertsGitHubAlertsIntoMdAlertDiv() {
        PrerenderEngine engine = createEngine();

        String html = engine.renderMarkdown("> [!NOTE]\n> 记得先备份数据库", null, "zh");

        // 渲染成 div 而非 blockquote —— 绕开 article-style-protection.css 的引用块 !important 保护规则
        assertTrue(html.contains("<div class=\"md-alert md-alert-note\">"), html);
        assertFalse(html.contains("<blockquote>"), html);
        // 标题在开标签后紧接，与前端 githubAlerts.js 的产物同构
        assertTrue(html.contains("<p class=\"md-alert-title\">"), html);
        assertTrue(html.contains("md-alert-icon"), html);
        assertTrue(html.contains("注意</p>"), html);
        // 标记被摘掉，正文成为普通段落
        assertTrue(html.contains("<p>记得先备份数据库</p>"), html);
        assertFalse(html.contains("[!NOTE]"), html);
    }

    @Test
    void renderMarkdownSupportsAllFiveAlertTypesAndSameLineBody() {
        PrerenderEngine engine = createEngine();

        String html = engine.renderMarkdown(
                "> [!NOTE] 同行正文\n\n> [!TIP] 小技巧\n\n> [!IMPORTANT] 重要的事\n\n"
                        + "> [!WARNING] 注意风险\n\n> [!CAUTION] 危险操作",
                null, "zh");

        for (String type : new String[] { "note", "tip", "important", "warning", "caution" }) {
            assertTrue(html.contains("class=\"md-alert md-alert-" + type + "\""), type + " 未转换: " + html);
        }
        for (String label : new String[] { "注意", "提示", "重要", "警告", "小心" }) {
            assertTrue(html.contains(label + "</p>"), label + " 标题缺失: " + html);
        }
        assertTrue(html.contains("<p>同行正文</p>"), html);
        assertFalse(html.contains("[!"), html);
    }

    @Test
    void renderMarkdownAlertTitleFollowsContentLanguage() {
        PrerenderEngine engine = createEngine();
        String markdown = "> [!NOTE]\n> body";

        // 内容语言决定标题，而不是站点 UI 语言
        assertTrue(engine.renderMarkdown(markdown, null, "zh").contains("注意</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "en").contains("Note</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "ja").contains("補足</p>"));
        // 带地区码 / 大小写 / 空值都要能落对：主语言子标签 → 忽略大小写 → en 兜底
        assertTrue(engine.renderMarkdown(markdown, null, "zh-CN").contains("注意</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "EN").contains("Note</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "ja-JP").contains("補足</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "").contains("Note</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, null).contains("Note</p>"));
        assertTrue(engine.renderMarkdown(markdown, null, "xx-YY").contains("Note</p>"));
    }

    @Test
    void renderMarkdownIgnoresUnknownAlertTypeAndKeepsPlainBlockquote() {
        PrerenderEngine engine = createEngine();

        String html = engine.renderMarkdown("> [!DANGER]\n> 白名单外，保持原样\n\n> 普通引用块", null, "zh");

        // 白名单外不认，字面量原样保留（行为可预测比多认几个类型重要）
        assertTrue(html.contains("[!DANGER]"), html);
        assertFalse(html.contains("md-alert"), html);
        // 普通引用块照旧渲染成 blockquote
        assertTrue(html.contains("<blockquote>"), html);
        assertTrue(html.contains("<p>普通引用块</p>"), html);
    }

    @Test
    void renderMarkdownDropsEmptyParagraphForMarkerOnlyAlert() {
        PrerenderEngine engine = createEngine();

        String html = engine.renderMarkdown("> [!TIP]\n>\n> 正文在第二段", null, "zh");

        assertTrue(html.contains("md-alert-tip"), html);
        // 只有标记的那一段被整个删掉，不留下占一行的空 <p>
        assertFalse(html.contains("<p></p>"), html);
        assertTrue(html.contains("<p>正文在第二段</p>"), html);
    }

    @Test
    void renderMarkdownKeepsInlineFormattingAndHandlesNestedBlocks() {
        PrerenderEngine engine = createEngine();

        String html = engine.renderMarkdown(
                "> [!WARNING]\n> 正文里有 **加粗** 和 `代码` 以及 [链接](/article/1)\n\n"
                        + "> > [!NOTE]\n> > 嵌套在引用块里的提示",
                "https://mysite.com", "zh");

        // 只摘掉标记本身，段落里其余内联节点照常渲染
        assertTrue(html.contains("<strong>加粗</strong>"), html);
        assertTrue(html.contains("<code>代码</code>"), html);
        assertTrue(html.contains("<a href=\"/article/1\" target=\"_blank\" rel=\"noopener noreferrer\">链接</a>"), html);
        // 嵌套引用里的 alert 同样被转换；外层普通引用块保留
        assertTrue(html.contains("md-alert-note"), html);
        assertTrue(html.contains("<blockquote>"), html);
        assertFalse(html.contains("[!NOTE]"), html);
        assertFalse(html.contains("[!WARNING]"), html);
    }

    @Test
    void renderMarkdownKeepsEscapedAlertMarkerLiteral() {
        PrerenderEngine engine = createEngine();

        // `\[!NOTE]` 是前端约定的「字面显示标签」逃生口，两端行为必须一致
        String html = engine.renderMarkdown("> \\[!NOTE]\n> 这行只想显示方括号标签", null, "zh");

        assertFalse(html.contains("md-alert"), html);
        assertTrue(html.contains("[!NOTE]"), html);
        assertTrue(html.contains("<blockquote>"), html);
    }

    private PrerenderEngine createEngine() {
        return new PrerenderEngine(JsonMapper.builder().build());
    }
}
