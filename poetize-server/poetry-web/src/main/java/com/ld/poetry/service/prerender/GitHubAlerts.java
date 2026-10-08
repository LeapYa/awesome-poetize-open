package com.ld.poetry.service.prerender;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Alerts 语法兼容（服务端预渲染版本）。
 *
 * <p>⚠️ <b>本站有两套独立的 markdown 渲染器，本类是服务端那一套。</b>前端在
 * {@code poetize-web/src/utils/githubAlerts.js}（markdown-it 插件，另有 admin 侧同名副本），
 * 服务端就是这里（flexmark）。两者<b>无共享代码</b>，改了一边务必检查另一边，
 * 否则会出现「浏览器里是彩色提示框、预渲染静态页还是旧的丑状态」——SEO/爬虫/首屏拿到的恰好是后者。
 *
 * <p>背景：GitHub 2023-05 上线的私有扩展，在引用块首行写 {@code > [!NOTE]} 等标签即渲染成彩色提示框。
 * 它不在 CommonMark、也不在 GFM 正式规范里，任何按规范实现的渲染器都不会认识它，但 AI 写文章爱用。
 * 「不管」在标准渲染器下只是退化成普通引用块，本站的引用块被
 * {@code article-style-protection.css} 用 {@code !important} 锁成「居中 + 两侧橙色引号」的大字引言，
 * 于是会渲染成一段语义全丢的怪东西，故必须做兼容。
 *
 * <p>实现方式与前端对齐，产物 HTML 逐字节同构：
 * 把「首个块级元素是以 {@code [!TYPE]} 开头的段落」的引用块改写成
 * {@code <div class="md-alert md-alert-{type}"><p class="md-alert-title">…图标…标题</p>正文</div>}。
 * <ul>
 *   <li><b>渲染成 div，不复用 blockquote</b> —— 直接绕开上面那套 {@code !important} 引用块保护规则。</li>
 *   <li>只摘掉标记本身（{@code [!TYPE]} 及其后的空白与紧随的软换行），段落里其余的内联节点原样保留，
 *       所以正文里的加粗、行内代码、链接照常生效。</li>
 *   <li>白名单只有五种，{@code [!DANGER]} 之类一律不动，保持原样 —— 行为可预测比多认几个类型重要。</li>
 * </ul>
 *
 * <p>标题文案跟随<b>内容语言</b>（不是站点 UI 语言）。语言码集合与前端
 * {@code utils/languageUtils.js} 的 {@code TOC_TITLE_MAP} 以及后端
 * {@link PrerenderLanguageSupport} 的白名单一致（站点支持的那 14 种）。
 * ⚠️ 这些译法是我们自己的自然译法，<b>不是</b>照抄 GitHub 的界面文案。
 */
public final class GitHubAlerts {

    private GitHubAlerts() {
    }

    /** 五种类型，顺序与 TITLES 每行的五个标题严格对应 */
    private static final List<String> ALERT_TYPES = List.of("note", "tip", "important", "warning", "caution");

    /** 缺省语言：既用于未指定语言，也用于语言码认不出来时的兜底 */
    private static final String FALLBACK_LANG = "en";

    /**
     * 标题文案表：顺序 = note / tip / important / warning / caution。
     * 语言码与 {@code PrerenderLanguageSupport.SUPPORTED_LANGUAGES} 一致。
     * zh 与 zh-TW 这五个词恰好同形（注意/提示/重要/警告/小心），故两行内容相同；
     * 想把 CAUTION 在繁体下换成「謹慎」之类，直接改 zh-TW 那一行即可。
     */
    private static final Map<String, String[]> TITLES = buildTitles();

    /** 图形只保留本体：无外圈、无底盘，描边统一 currentColor（暗色模式下才不会变成亮斑） */
    private static final Map<String, String> ICONS = buildIcons();

    /** 引用块首行标记；用 {@link Matcher#lookingAt()} 锚定在段落文本开头 */
    private static final Pattern MARKER_PATTERN =
            Pattern.compile("\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]", Pattern.CASE_INSENSITIVE);

    /**
     * 转换整篇文档里的 GitHub Alerts。
     *
     * @param root 已解析的 AST 根节点（原地修改）
     * @param lang 内容语言码，决定提示框标题文案；为空或认不出时回退 en
     */
    public static void apply(Node root, String lang) {
        if (root == null) {
            return;
        }
        walk(root, resolveTitles(lang));
    }

    /**
     * 渲染器工厂：把 {@link GitHubAlertNode} 渲染成 {@code div.md-alert}。
     * 只接管自定义节点，其它节点（含普通引用块）的渲染完全不变。
     */
    public static NodeRendererFactory rendererFactory() {
        return new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return () -> Set.of(new NodeRenderingHandler<>(GitHubAlertNode.class,
                        GitHubAlerts::renderAlert));
            }
        };
    }

    /**
     * 提示框节点：承载类型与已解析好的标题文案。
     *
     * <p>标题在<b>转换阶段</b>（那时才有 lang）就解析好挂进节点，渲染器因此完全不依赖语言，
     * 不需要按语言建多份 HtmlRenderer —— 与前端把标题写进 {@code token.meta} 的思路一致。
     */
    static final class GitHubAlertNode extends Block {

        private final String type;
        private final String label;

        GitHubAlertNode(String type, String label) {
            super(BasedSequence.of(""));
            this.type = type;
            this.label = label;
        }

        @Override
        public BasedSequence[] getSegments() {
            // 节点是解析后改写出来的，没有对应的源码行区间
            return new BasedSequence[0];
        }

        String getType() {
            return type;
        }

        String getLabel() {
            return label;
        }
    }

    private static void renderAlert(GitHubAlertNode node, NodeRendererContext context, HtmlWriter html) {
        html.raw("<div class=\"md-alert md-alert-" + node.getType() + "\">");
        html.raw(buildTitleHtml(node.getType(), node.getLabel()));
        context.renderChildren(node);
        html.raw("</div>\n");
    }

    private static String buildTitleHtml(String type, String label) {
        return "<p class=\"md-alert-title\">"
                + "<svg class=\"md-alert-icon\" viewBox=\"0 0 16 16\" width=\"16\" height=\"16\""
                + " aria-hidden=\"true\" focusable=\"false\" fill=\"none\" stroke=\"currentColor\""
                + " stroke-width=\"1.4\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
                + ICONS.getOrDefault(type, "")
                + "</svg>"
                + label
                + "</p>\n";
    }

    /**
     * 深度优先遍历；命中引用块则尝试转换，并<b>继续往里走</b>
     * —— 嵌套引用（{@code > > [!NOTE]}）与提示框内部的嵌套引用都要能覆盖到。
     */
    private static void walk(Node parent, String[] titles) {
        Node child = parent.getFirstChild();
        while (child != null) {
            // 提前取下一个兄弟：转换会改动兄弟链
            Node next = child.getNext();
            if (child instanceof BlockQuote quote) {
                walk(convertIfAlert(quote, titles), titles);
            } else {
                walk(child, titles);
            }
            child = next;
        }
    }

    /**
     * 若引用块符合 alert 形状则改写为 {@link GitHubAlertNode} 并返回新节点，否则原样返回引用块。
     *
     * <p>⚠️ 形状与前端不同：markdown-it 里 {@code [!NOTE]} 只是一段普通文本，而 flexmark 会把它
     * 解析成 {@link LinkRef}（真正的文本挂在其子节点上）。所以首个子节点可能是 LinkRef 也可能是
     * Text，两种都要认 —— 这是后端这条链路最容易踩空的地方。
     */
    private static Node convertIfAlert(BlockQuote quote, String[] titles) {
        Node quoteFirst = quote.getFirstChild();
        if (!(quoteFirst instanceof Paragraph paragraph)) {
            return quote;
        }

        Node marker = paragraph.getFirstChild();
        String markerText = markerText(marker);
        if (markerText == null) {
            return quote;
        }
        Matcher matcher = MARKER_PATTERN.matcher(markerText);
        if (!matcher.lookingAt()) {
            return quote;
        }

        String type = matcher.group(1).toLowerCase(Locale.ROOT);
        int consumed = matcher.end();
        int nodeTextLength = markerText.length();
        // 吃掉标记后的空白：`> [!NOTE] 同行正文` 的正文直接从空格后开始
        while (consumed < nodeTextLength) {
            char c = markerText.charAt(consumed);
            if (c != ' ' && c != '\t') {
                break;
            }
            consumed++;
        }

        if (consumed < nodeTextLength) {
            // 标记只是节点文本的前缀（标记与正文同处一个文本节点）：就地截断
            BasedSequence chars = marker.getChars();
            marker.setChars(chars.subSequence(consumed, chars.length()));
        } else {
            // 整个节点就是标记：摘掉它，并处理它后面残留的分隔
            Node next = marker.getNext();
            marker.unlink();
            trimFollowingSeparator(next);
        }

        if (!paragraph.hasChildren()) {
            // 只有标签没有正文：整段删掉，否则会多出一个占一行的空 <p>
            paragraph.unlink();
        }

        GitHubAlertNode alert = new GitHubAlertNode(type, titles[ALERT_TYPES.indexOf(type)]);
        Node child = quote.getFirstChild();
        while (child != null) {
            Node moving = child.getNext();
            alert.appendChild(child);
            child = moving;
        }
        quote.insertBefore(alert);
        quote.unlink();
        return alert;
    }

    /**
     * 取节点承载的标记文本，其它类型一律不认。
     *
     * <p>⚠️ 必须用节点**自身**的 chars：flexmark 把 {@code [!NOTE]} 解析成 {@link LinkRef}，
     * 它的 {@code getChars()} 才是含方括号的 {@code [!NOTE]}；而它的子节点 Text 只有括号
     * **内部**的 {@code !NOTE}（实测 len=5）—— 顺着子节点取会把方括号丢掉，正则就永远匹配不上。
     */
    private static String markerText(Node node) {
        if (node instanceof Text || node instanceof LinkRef) {
            BasedSequence chars = node.getChars();
            return chars == null ? null : chars.toString();
        }
        return null;
    }

    /**
     * 摘掉标记节点后，清理它后面残留的分隔：
     * 软换行要一并去掉（否则会渲染出多余的前导 {@code <br />}），
     * 文本节点则左裁掉前导空白（{@code > [!NOTE] 正文} 的空格留在下一个文本节点里）。
     */
    private static void trimFollowingSeparator(Node next) {
        if (next instanceof SoftLineBreak) {
            next.unlink();
            return;
        }
        if (next instanceof Text nextText) {
            BasedSequence chars = nextText.getChars();
            if (chars == null) {
                return;
            }
            int start = 0;
            while (start < chars.length()) {
                char c = chars.charAt(start);
                if (c != ' ' && c != '\t') {
                    break;
                }
                start++;
            }
            if (start >= chars.length()) {
                nextText.unlink();
            } else if (start > 0) {
                nextText.setChars(chars.subSequence(start, chars.length()));
            }
        }
    }

    /**
     * 语言码 → 标题数组。依次尝试：精确匹配 → 忽略大小写 → 取主语言子标签（zh-CN→zh、pt-BR→pt），
     * 都不中则兜底 en。这样带地区码的写法（en-US / zh-Hans / ja-JP）也能落到正确那行。
     */
    private static String[] resolveTitles(String lang) {
        String[] fallback = TITLES.get(FALLBACK_LANG);
        if (lang == null || lang.trim().isEmpty()) {
            return fallback;
        }

        String raw = lang.trim();
        String[] exact = TITLES.get(raw);
        if (exact != null) {
            return exact;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String[]> entry : TITLES.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                return entry.getValue();
            }
        }

        String base = lower.split("[-_]", 2)[0];
        for (Map.Entry<String, String[]> entry : TITLES.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).equals(base)) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static Map<String, String[]> buildTitles() {
        Map<String, String[]> titles = new LinkedHashMap<>();
        titles.put("zh", new String[] { "注意", "提示", "重要", "警告", "小心" });
        titles.put("zh-TW", new String[] { "注意", "提示", "重要", "警告", "小心" });
        titles.put("en", new String[] { "Note", "Tip", "Important", "Warning", "Caution" });
        titles.put("ja", new String[] { "補足", "ヒント", "重要", "警告", "注意" });
        titles.put("ko", new String[] { "참고", "팁", "중요", "경고", "주의" });
        titles.put("fr", new String[] { "Remarque", "Astuce", "Important", "Avertissement", "Attention" });
        titles.put("de", new String[] { "Hinweis", "Tipp", "Wichtig", "Warnung", "Vorsicht" });
        titles.put("es", new String[] { "Nota", "Sugerencia", "Importante", "Advertencia", "Precaución" });
        titles.put("ru", new String[] { "Примечание", "Совет", "Важно", "Предупреждение", "Осторожно" });
        titles.put("pt", new String[] { "Nota", "Dica", "Importante", "Aviso", "Cuidado" });
        titles.put("it", new String[] { "Nota", "Suggerimento", "Importante", "Avviso", "Attenzione" });
        titles.put("ar", new String[] { "ملاحظة", "نصيحة", "مهم", "تحذير", "تنبيه" });
        titles.put("th", new String[] { "หมายเหตุ", "เคล็ดลับ", "สำคัญ", "คำเตือน", "ข้อควรระวัง" });
        titles.put("vi", new String[] { "Ghi chú", "Mẹo", "Quan trọng", "Cảnh báo", "Thận trọng" });
        return Map.copyOf(titles);
    }

    private static Map<String, String> buildIcons() {
        Map<String, String> icons = new LinkedHashMap<>();
        icons.put("note", "<circle cx=\"8\" cy=\"8\" r=\"6.4\"/><path d=\"M8 7.5v4\"/><path d=\"M8 4.7h.01\"/>");
        icons.put("tip", "<path d=\"M8 1.9a4.1 4.1 0 0 0-2.4 7.5v1.5h4.8V9.4A4.1 4.1 0 0 0 8 1.9Z\"/>"
                + "<path d=\"M6.4 12.8h3.2\"/><path d=\"M7 14.4h2\"/>");
        icons.put("important", "<rect x=\"1.9\" y=\"1.9\" width=\"12.2\" height=\"12.2\" rx=\"3.2\"/>"
                + "<path d=\"M8 5.2v3.6\"/><path d=\"M8 10.8h.01\"/>");
        icons.put("warning", "<path d=\"M8 2.1 14.5 13.2H1.5Z\"/><path d=\"M8 6.3v3.3\"/><path d=\"M8 11.5h.01\"/>");
        icons.put("caution", "<path d=\"M5.7 1.9h4.6l3.8 3.8v4.6l-3.8 3.8H5.7L1.9 10.3V5.7Z\"/>"
                + "<path d=\"M8 4.9v3.4\"/><path d=\"M8 10.9h.01\"/>");
        return Map.copyOf(icons);
    }
}
