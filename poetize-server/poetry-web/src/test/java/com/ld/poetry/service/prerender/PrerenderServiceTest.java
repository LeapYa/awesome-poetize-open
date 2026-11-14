package com.ld.poetry.service.prerender;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.dao.ResourcePathMapper;
import com.ld.poetry.dao.WeiYanMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.entity.WeiYan;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.SeoConfigService;
import com.ld.poetry.service.SeoMetaService;
import com.ld.poetry.service.TranslationService;
import com.ld.poetry.service.WebInfoService;
import com.ld.poetry.service.WeiYanService;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.mail.MailUtil;
import com.ld.poetry.vo.ResourcePathVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrerenderServiceTest {

    @Mock
    private PrerenderEngine engine;

    @Mock
    private ArticleService articleService;

    @Mock
    private SeoMetaService seoMetaService;

    @Mock
    private SeoConfigService seoConfigService;

    @Mock
    private WebInfoService webInfoService;

    @Mock
    private CacheService cacheService;

    @Mock
    private TranslationService translationService;

    @Mock
    private PrerenderLanguageSupport languageSupport;

    @Mock
    private CommonQuery commonQuery;

    @Mock
    private ResourcePathMapper resourcePathMapper;

    @Mock
    private MailUtil mailUtil;

    @Mock
    private WeiYanService weiYanService;

    @Mock
    private WeiYanMapper weiYanMapper;

    @InjectMocks
    private PrerenderService service;

    @Test
    void renderArticleShouldUseAnonymousPreviewForPaidTranslations() {
        Article article = createPaidArticle();

        when(articleService.getById(1)).thenReturn(article);
        when(translationService.getArticleTranslation(1, "en")).thenReturn(Map.of(
                "title", "Translated",
                "content", "abcdefghij"));
        when(seoMetaService.generateArticleMeta(1, "en")).thenReturn(new LinkedHashMap<>());
        when(cacheService.getCachedWebInfo()).thenReturn(createWebInfo());
        when(seoConfigService.getSeoConfigAsJson()).thenReturn(new LinkedHashMap<>());
        when(languageSupport.getSourceLanguage()).thenReturn("zh");
        when(languageSupport.resolveLanguages(List.of("en"))).thenReturn(List.of("en"));
        when(mailUtil.getSiteUrl()).thenReturn("https://example.com");
        when(engine.renderMarkdown(anyString(), any(), any())).thenReturn("<p>preview</p>");
        when(engine.buildPage(any())).thenReturn("<html>preview</html>");
        stubNoArticleNews();

        service.renderArticle(1, List.of("en"));

        verify(engine).renderMarkdown(eq("abc"), any(), any());
        verify(engine).writePage("article/1", "en", "<html>preview</html>");
    }

    @Test
    void renderArticleShouldSupportExtendedLanguages() {
        Article article = createPaidArticle();

        when(articleService.getById(1)).thenReturn(article);
        when(translationService.getArticleTranslation(1, "pt")).thenReturn(Map.of(
                "title", "Traduzido",
                "content", "conteudo-em-portugues"));
        when(seoMetaService.generateArticleMeta(1, "pt")).thenReturn(new LinkedHashMap<>());
        when(cacheService.getCachedWebInfo()).thenReturn(createWebInfo());
        when(seoConfigService.getSeoConfigAsJson()).thenReturn(new LinkedHashMap<>());
        when(languageSupport.getSourceLanguage()).thenReturn("zh");
        when(languageSupport.resolveLanguages(List.of("pt"))).thenReturn(List.of("pt"));
        when(mailUtil.getSiteUrl()).thenReturn("https://example.com");
        when(engine.renderMarkdown(anyString(), any(), any())).thenReturn("<p>preview</p>");
        when(engine.buildPage(any())).thenReturn("<html>preview</html>");
        stubNoArticleNews();

        assertDoesNotThrow(() -> service.renderArticle(1, List.of("pt")));

        verify(engine).writePage("article/1", "pt", "<html>preview</html>");
    }

    /**
     * buildArticleNewsHtml 会经 weiYanService.lambdaQuery() 链式查询文章"最新进展"；
     * 用真实 LambdaQueryChainWrapper（需 WeiYan 的 MP 元数据）+ mock 底层 mapper 让查询返回空列表
     */
    @SuppressWarnings("unchecked")
    private void stubNoArticleNews() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeiYan.class);
        when(weiYanService.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(weiYanMapper));
        when(weiYanMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    }

    private Article createPaidArticle() {
        Article article = new Article();
        article.setId(1);
        article.setViewStatus(true);
        article.setArticleTitle("Paid Article");
        article.setArticleContent("0123456789");
        article.setPayType(1);
        article.setFreePercent(30);
        return article;
    }

    private WebInfo createWebInfo() {
        WebInfo webInfo = new WebInfo();
        webInfo.setWebTitle("Example Site");
        webInfo.setSiteAddress("https://example.com");
        return webInfo;
    }

    @Test
    void renderHomePageShouldPreferHomeTitleOverWebTitle() {
        WebInfo webInfo = new WebInfo();
        webInfo.setWebTitle("网页标题");
        webInfo.setHomeTitle("首页标题");
        webInfo.setSiteAddress("https://example.com");

        stubRenderHomePageDependencies(webInfo);

        service.renderHomePage();

        PrerenderPageData pageData = captureHomePageBuildPageArgument();
        assertEquals("首页标题", pageData.getTitle(),
                "homeTitle 非空时首页 <title> 必须使用 homeTitle");
    }

    @Test
    void renderHomePageShouldFallbackToWebTitleWhenHomeTitleBlank() {
        WebInfo webInfo = new WebInfo();
        webInfo.setWebTitle("网页标题");
        webInfo.setSiteAddress("https://example.com");

        stubRenderHomePageDependencies(webInfo);

        service.renderHomePage();

        PrerenderPageData pageData = captureHomePageBuildPageArgument();
        assertEquals("网页标题", pageData.getTitle(),
                "homeTitle 为空时回退到 webTitle");
    }

    private void stubRenderHomePageDependencies(WebInfo webInfo) {
        when(cacheService.getCachedWebInfo()).thenReturn(webInfo);
        when(seoConfigService.getSeoConfigAsJson()).thenReturn(new LinkedHashMap<>());
        when(commonQuery.getSortInfo()).thenReturn(List.of());
        when(languageSupport.getSourceLanguage()).thenReturn("zh");
        when(mailUtil.getSiteUrl()).thenReturn("https://example.com");
        when(seoMetaService.generateSiteMeta("zh")).thenReturn(new LinkedHashMap<>());
        when(cacheService.getCachedPublicSysConfigMap()).thenReturn(Map.of());
        when(engine.buildPage(any())).thenReturn("<html></html>");
    }

    private PrerenderPageData captureHomePageBuildPageArgument() {
        ArgumentCaptor<PrerenderPageData> captor = ArgumentCaptor.forClass(PrerenderPageData.class);
        verify(engine).buildPage(captor.capture());
        return captor.getValue();
    }

    @Test
    void renderFriendGroupShouldDropPseudoProtocolAndNormalizeUrl() {
        ResourcePathVO injected = createResourcePath("坏链接", "含脚本的地址", "javascript:alert(1)");
        ResourcePathVO schemeless = createResourcePath("好链接", "正常站点", "example.com");

        String html = ReflectionTestUtils.invokeMethod(
                service, "renderFriendGroup", "🥇友情链接", List.of(injected, schemeless));

        // 历史脏数据不能写进 href，否则静态友人帐页被点到就在站点源里执行脚本
        assertFalse(html.contains("javascript:"), html);
        assertTrue(html.contains("<li>坏链接 - 含脚本的地址</li>"), html);
        // 缺 scheme 的地址补 https://，避免被当成站内相对路径
        assertTrue(html.contains("href=\"https://example.com\""), html);
        // 友链是互惠链接，必须 dofollow 递权重，不能套用评论的 nofollow/ugc 口径
        assertFalse(html.contains("nofollow"), html);
        assertFalse(html.contains("ugc"), html);
    }

    @Test
    void renderFavoriteItemShouldDropPseudoProtocolAndNormalizeUrl() {
        ResourcePathVO injected = createResourcePath("坏收藏", "含脚本的地址", "javascript:alert(1)");
        injected.setCover("/cover.png");
        ResourcePathVO schemeless = createResourcePath("好收藏", "正常站点", "example.com");
        schemeless.setCover("/cover.png");

        String injectedHtml = ReflectionTestUtils.invokeMethod(service, "renderFavoriteItem", injected);
        String schemelessHtml = ReflectionTestUtils.invokeMethod(service, "renderFavoriteItem", schemeless);

        // 收藏夹与友链同为 href sink，历史脏数据同样降级为纯文本
        assertFalse(injectedHtml.contains("javascript:"), injectedHtml);
        assertFalse(injectedHtml.contains("<a "), injectedHtml);
        assertTrue(injectedHtml.contains("<span>坏收藏</span>"), injectedHtml);
        assertTrue(schemelessHtml.contains("href=\"https://example.com\""), schemelessHtml);
    }

    private ResourcePathVO createResourcePath(String title, String introduction, String url) {
        ResourcePathVO resourcePath = new ResourcePathVO();
        resourcePath.setTitle(title);
        resourcePath.setIntroduction(introduction);
        resourcePath.setUrl(url);
        return resourcePath;
    }
}
