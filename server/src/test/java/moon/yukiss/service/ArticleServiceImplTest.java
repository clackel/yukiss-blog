package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceImplTest {
    private ArticleMapper articleMapper;
    private ArticleServiceImpl articleService;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        articleService = new ArticleServiceImpl(articleMapper);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void pageNormalizesSearchAndLegacyPopularSort() {
        ThreadLocalUtil.set(Map.of("id", 7));
        Article article = article(11, 3);
        when(articleMapper.countPage("Spring", "+\"Spring\"", null)).thenReturn(12L);
        when(articleMapper.page(7, "Spring", "+\"Spring\"", null, "likes", 10, 10)).thenReturn(List.of(article));

        PageResult<Article> result = articleService.page(2, 10, "  Spring  ", "POPULAR", null);

        assertEquals(12, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(2, result.getTotalPages());
        assertEquals(List.of(article), result.getItems());
    }

    @Test
    void pageEscapesLikeWildcardsSoTheyMatchLiterally() {
        articleService.page(1, 10, "50%_off\\", null, null);

        // LIKE 关键词按字面转义；全文条件只取字母数字片段（ngram 会忽略标点）
        verify(articleMapper).countPage("50\\%\\_off\\\\", "+\"50\" +\"off\"", null);
    }

    @Test
    void punctuationIsDroppedFromFulltextTermsButKeptForExactLikeMatch() {
        articleService.page(1, 10, "100%", null, null);
        articleService.page(1, 10, "c++", null, null);

        verify(articleMapper).countPage("100\\%", "+\"100\"", null);
        // "c" 只有 1 个字符，ngram 无法索引，只能用 LIKE
        verify(articleMapper).countPage("c++", null, null);
    }

    @Test
    void pageFallsBackToLikeForKeywordsTooShortForTheFulltextIndex() {
        articleService.page(1, 10, "猫", null, null);
        articleService.page(1, 10, "Vue 3", null, null);

        // ngram 最小 2 个字符：单字或包含单字片段的关键词不能走全文索引
        verify(articleMapper).countPage("猫", null, null);
        verify(articleMapper).countPage("Vue 3", null, null);
    }

    @Test
    void pageRequiresEveryFragmentInFulltextQuery() {
        articleService.page(1, 10, "say \"hi\" there", null, null);

        // 引号等布尔模式运算符不会进入全文条件
        verify(articleMapper).countPage("say \"hi\" there", "+\"say\" +\"hi\" +\"there\"", null);
    }

    @Test
    void pagePassesNormalizedTagFilter() {
        articleService.page(1, 10, null, null, "  #Vue ");

        verify(articleMapper).countPage(null, null, "Vue");
    }

    @Test
    void pageRejectsUnsupportedSort() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleService.page(1, 10, null, "oldest", null)
        );

        assertEquals(400, exception.getStatus().value());
        verify(articleMapper, never()).countPage(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"published", "commented", "likes", "comments"})
    void pageAcceptsEachSupportedSort(String sort) {
        when(articleMapper.countPage(null, null, null)).thenReturn(1L);
        when(articleMapper.page(null, null, null, null, sort, 0, 10)).thenReturn(List.of(article(11, 3)));

        PageResult<Article> result = articleService.page(1, 10, null, sort, null);

        assertEquals(1, result.getTotal());
        verify(articleMapper).page(null, null, null, null, sort, 0, 10);
    }

    @Test
    void draftIsHiddenFromOtherUsers() {
        ThreadLocalUtil.set(Map.of("id", 8));
        Article draft = article(21, 7);
        draft.setStatus(Article.STATUS_DRAFT);
        when(articleMapper.getById(21, 8)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> articleService.getById(21));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void draftIsVisibleToItsAuthor() {
        ThreadLocalUtil.set(Map.of("id", 7));
        Article draft = article(21, 7);
        draft.setStatus(Article.STATUS_DRAFT);
        when(articleMapper.getById(21, 7)).thenReturn(draft);

        assertEquals(draft, articleService.getById(21));
    }

    @Test
    void listMineIncludesDraftsButPublicListDoesNot() {
        ThreadLocalUtil.set(Map.of("id", 7));

        articleService.listMine();
        articleService.listByAuthor(7);

        verify(articleMapper).listByAuthor(7, 7, true);
        verify(articleMapper).listByAuthor(7, 7, false);
    }

    @Test
    void addTrimsContentSavesTagsAndReturnsCreatedArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        ArticleRequest request = articleRequest("  标题  ", "  正文  ");
        request.setTags(List.of(" Vue ", "#vue", "", "Spring"));
        Article created = article(21, 7);
        doAnswer(invocation -> {
            Article value = invocation.getArgument(0);
            value.setId(21);
            return 1;
        }).when(articleMapper).insert(any(Article.class));
        when(articleMapper.getById(21, 7)).thenReturn(created);

        Article result = articleService.add(request);

        assertEquals(created, result);
        ArgumentCaptor<Article> inserted = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).insert(inserted.capture());
        assertEquals("标题", inserted.getValue().getTitle());
        assertEquals("正文", inserted.getValue().getContent());
        assertEquals(Article.STATUS_PUBLISHED, inserted.getValue().getStatus());
        // 去掉 #、空白和空标签，按不区分大小写去重
        verify(articleMapper).insertTags(21, List.of("Vue", "Spring"));
    }

    @Test
    void draftMayHaveEmptyBodyButPublishedArticleMayNot() {
        ThreadLocalUtil.set(Map.of("id", 7));
        ArticleRequest draft = articleRequest("标题", "");
        draft.setStatus("draft");
        ArticleRequest published = articleRequest("标题", "   ");

        articleService.add(draft);
        BusinessException exception = assertThrows(BusinessException.class, () -> articleService.add(published));

        assertEquals("请填写文章正文", exception.getMessage());
        verify(articleMapper, never()).insertTags(anyInt(), anyList());
    }

    @Test
    void addRejectsUnknownStatusAndTooManyTags() {
        ThreadLocalUtil.set(Map.of("id", 7));
        ArticleRequest badStatus = articleRequest("标题", "正文");
        badStatus.setStatus("archived");
        ArticleRequest tooManyTags = articleRequest("标题", "正文");
        tooManyTags.setTags(List.of("a", "b", "c", "d", "e", "f"));
        ArticleRequest commaTag = articleRequest("标题", "正文");
        commaTag.setTags(List.of("a,b"));

        assertThrows(BusinessException.class, () -> articleService.add(badStatus));
        assertThrows(BusinessException.class, () -> articleService.add(tooManyTags));
        assertThrows(BusinessException.class, () -> articleService.add(commaTag));
        verify(articleMapper, never()).insert(any());
    }

    @Test
    void updateRejectsAnotherUsersArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(articleMapper.getById(21, 7)).thenReturn(article(21, 8));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleService.update(21, articleRequest("标题", "正文"))
        );

        assertEquals(403, exception.getStatus().value());
        verify(articleMapper, never()).update(any());
    }

    @Test
    void updateReplacesTags() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(articleMapper.getById(21, 7)).thenReturn(article(21, 7));
        ArticleRequest request = articleRequest("标题", "正文");

        articleService.update(21, request);

        InOrder order = inOrder(articleMapper);
        order.verify(articleMapper).update(any(Article.class));
        order.verify(articleMapper).deleteTagsByArticleId(21);
        verify(articleMapper, never()).insertTags(anyInt(), anyList());
    }

    @Test
    void deleteRemovesDependentDataBeforeArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(articleMapper.getById(21, 7)).thenReturn(article(21, 7));

        articleService.delete(21);

        InOrder order = inOrder(articleMapper);
        order.verify(articleMapper).deleteCommentLikesByArticleId(21);
        order.verify(articleMapper).detachCommentRepliesByArticleId(21);
        order.verify(articleMapper).deleteCommentsByArticleId(21);
        order.verify(articleMapper).deleteLikesByArticleId(21);
        order.verify(articleMapper).deleteTagsByArticleId(21);
        order.verify(articleMapper).deleteNotificationsByArticleId(21);
        order.verify(articleMapper).deleteById(21);
    }

    private Article article(int id, int authorId) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setStatus(Article.STATUS_PUBLISHED);
        return article;
    }

    private ArticleRequest articleRequest(String title, String content) {
        ArticleRequest request = new ArticleRequest();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }
}
