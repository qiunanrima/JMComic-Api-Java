package io.github.jukomu.jmcomic.core;

import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;
import io.github.jukomu.jmcomic.core.client.AbstractJmClient;
import io.github.jukomu.jmcomic.core.client.JmAsyncClient;
import io.github.jukomu.jmcomic.core.client.JmComicClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class UnifiedApiTest {

    @Test
    @DisplayName("测试 JmConfiguration 构建器与便捷工厂")
    public void testConfigurationBuilder() {
        JmConfiguration config = JmConfiguration.builder()
                .clientType(ClientType.API)
                .apiDomains(List.of("18comic.vip", "jmcomic.me"))
                .retryTimes(3)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Test-Agent")
                .build();

        assertEquals(ClientType.API, config.getClientType());
        assertEquals(2, config.getApiDomains().size());
        assertEquals("18comic.vip", config.getApiDomains().get(0));
        assertEquals(3, config.getRetryTimes());
        assertEquals(Duration.ofSeconds(15), config.getTimeout());
        assertEquals("Test-Agent", config.getHeaders().get("User-Agent"));

        JmConfiguration defaultConfig = JmConfiguration.defaultConfig();
        assertNotNull(defaultConfig);
        assertEquals(ClientType.API, defaultConfig.getClientType());
    }

    @Test
    @DisplayName("测试 JmResult 函数式与安全包裹操作")
    public void testJmResult() {
        // 成功分支
        JmResult<String> success = JmResult.success("comic_title_123");
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("comic_title_123", success.getOrNull());
        assertEquals("comic_title_123", success.getOrThrow());
        assertEquals("comic_title_123", success.getOrDefault("default"));
        assertEquals("comic_title_123", success.getOrElse("fallback"));
        assertEquals(Optional.of("comic_title_123"), success.toOptional());

        AtomicBoolean successRan = new AtomicBoolean(false);
        success.onSuccess(val -> {
            assertEquals("comic_title_123", val);
            successRan.set(true);
        }).onFailure(err -> fail("Should not reach onFailure"));
        assertTrue(successRan.get());

        // map 与 flatMap
        JmResult<Integer> lengthResult = success.map(String::length);
        assertEquals(15, lengthResult.getOrNull());

        JmResult<Integer> flatMapped = success.flatMap(s -> JmResult.success(s.length() * 2));
        assertEquals(30, flatMapped.getOrNull());

        // fold
        String folded = success.fold(s -> "SUCCESS: " + s, err -> "ERROR: " + err.getMessage());
        assertEquals("SUCCESS: comic_title_123", folded);

        // 失败分支
        JmResult<String> failure = JmResult.failure(new IllegalArgumentException("Album not found"));
        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
        assertNull(failure.getOrNull());
        assertEquals("default", failure.getOrDefault("default"));
        assertEquals("fallback", failure.getOrElse("fallback"));
        assertEquals(Optional.empty(), failure.toOptional());
        assertThrows(RuntimeException.class, failure::getOrThrow);

        AtomicBoolean failureRan = new AtomicBoolean(false);
        failure.onSuccess(val -> fail("Should not reach onSuccess"))
               .onFailure(err -> {
                   assertEquals("Album not found", err.getMessage());
                   failureRan.set(true);
               });
        assertTrue(failureRan.get());

        // runCatching 操作
        JmResult<Integer> caughtSuccess = JmResult.runCatching(() -> Integer.parseInt("42"));
        assertEquals(42, caughtSuccess.getOrNull());

        JmResult<Integer> caughtFailure = JmResult.runCatching(() -> Integer.parseInt("not_a_number"));
        assertTrue(caughtFailure.isFailure());
        assertNotNull(caughtFailure.exceptionOrNull());
    }

    @Test
    @DisplayName("测试 JmImages 工具方法")
    public void testJmImages() {
        JmImage image = new JmImage(
                "1000",
                "220980",
                "00001.webp",
                "https://cdn.example.com/media/photos/1000/00001.webp",
                "v=12345",
                1
        );

        assertEquals("https://cdn.example.com/media/photos/1000/00001.webp?v=12345", JmImages.pageUrl(image));
        assertEquals("https://cdn.example.com/media/photos/1000/00001.webp", JmImages.imageUrl(image));
        assertFalse(JmImages.isScrambled(image));

        JmImage scrambledImage = new JmImage(
                "300000",
                "220980",
                "00001.webp",
                "https://cdn.example.com/media/photos/300000/00001.webp",
                "v=12345",
                1
        );
        assertTrue(JmImages.isScrambled(scrambledImage));

        byte[] raw = new byte[]{1, 2, 3};
        byte[] decoded = JmImages.decodeImage(raw, image);
        assertArrayEquals(raw, decoded);
    }

    @Test
    @DisplayName("测试 JmComicClient 与 JmAsyncClient 的语义化适配与异步调用")
    public void testClientParityAndAsync() throws ExecutionException, InterruptedException {
        // 创建一个轻量 mock 底层客户端进行门面验证
        TestMockJmClient mockRaw = new TestMockJmClient();
        JmComicClient client = JmComicClient.wrap(mockRaw);

        // 语义别名调用测试
        JmResult<JmAlbum> detailRes = client.getComicDetail("12345");
        assertTrue(detailRes.isSuccess());
        assertEquals("Test Album", detailRes.getOrNull().title());

        JmResult<List<JmPhotoMeta>> episodesRes = client.getComicEpisodes("12345");
        assertTrue(episodesRes.isSuccess());
        assertEquals(2, episodesRes.getOrNull().size());

        JmResult<JmPhoto> episodeRes = client.getComicEpisode("photo_1");
        assertTrue(episodeRes.isSuccess());
        assertEquals("Chapter 1", episodeRes.getOrNull().title());

        JmResult<List<JmImage>> pagesRes = client.getComicPages("photo_1");
        assertTrue(pagesRes.isSuccess());
        assertEquals(1, pagesRes.getOrNull().size());

        JmResult<JmSearchPage> searchRes = client.searchComics("keyword", 1);
        assertTrue(searchRes.isSuccess());
        assertEquals(10, searchRes.getOrNull().totalItems());

        // 异步客户端测试
        JmAsyncClient asyncClient = client.async();
        CompletableFuture<JmResult<JmAlbum>> future = asyncClient.getComicDetail("12345");
        JmResult<JmAlbum> asyncDetail = future.get();
        assertTrue(asyncDetail.isSuccess());
        assertEquals("Test Album", asyncDetail.getOrNull().title());
    }

    @Test
    @DisplayName("测试 Jm 全局单例管理器状态与代理方法")
    public void testJmSingleton() {
        assertFalse(Jm.isInitialized());
        assertThrows(IllegalStateException.class, Jm::client);

        TestMockJmClient mockRaw = new TestMockJmClient();
        JmComicClient client = JmComicClient.wrap(mockRaw);
        Jm.setClient(client);

        assertTrue(Jm.isInitialized());
        assertSame(client, Jm.client());
        assertSame(client, Jm.getClient());
        assertNotNull(Jm.async());

        // 通过 Jm 直接调用常用方法
        JmResult<JmAlbum> detail = Jm.getComicDetail("12345");
        assertTrue(detail.isSuccess());
        assertEquals("Test Album", detail.getOrNull().title());
    }

    /**
     * 测试用的轻量级 AbstractJmClient 桩
     */
    private static class TestMockJmClient extends AbstractJmClient {
        public TestMockJmClient() {
            super(JmConfiguration.defaultConfig(), new okhttp3.OkHttpClient(), new java.net.CookieManager(), new io.github.jukomu.jmcomic.core.net.provider.JmDomainManager(List.of("18comic.vip")));
        }

        @Override
        protected void initialize() {}

        @Override
        protected void updateDomains() {}

        @Override
        public JmAlbum getAlbum(String albumId) {
            List<JmPhotoMeta> photos = List.of(
                    new JmPhotoMeta("photo_1", "Chapter 1", 1),
                    new JmPhotoMeta("photo_2", "Chapter 2", 2)
            );
            return new JmAlbum(
                    albumId, "Test Album", "Desc", "0", "123456", 20, "100", "500", 10,
                    "https://cdn.example.com/cover.jpg",
                    new JmCategoryMeta("cat1", "Cat 1"),
                    new JmCategoryMeta("sub1", "Sub 1"),
                    List.of("Author"), List.of(), List.of(), List.of("Tag1"),
                    List.of(), photos, "0", false, false, false, List.of(), "", ""
            );
        }

        @Override
        public JmAlbum getComicRead(String comicId) {
            return getAlbum(comicId);
        }

        @Override
        public JmPhoto getPhoto(String photoId) {
            List<JmImage> images = List.of(
                    new JmImage(photoId, "0", "001.webp", "https://cdn.example.com/001.webp", "", 1)
            );
            return new JmPhoto(photoId, "Chapter 1", "12345", "0", 1, "Author", List.of("Tag1"), images, false);
        }

        @Override
        public JmSearchPage search(SearchQuery query) {
            return new JmSearchPage(1, 10, 1, List.of());
        }

        @Override
        public JmSearchPage getCategories(SearchQuery query) {
            return new JmSearchPage(1, 10, 1, List.of());
        }

        @Override
        public JmCategoryList getCategoriesList() {
            return null;
        }

        @Override
        public JmAlbumDownloadInfo getAlbumDownloadInfo(String albumId) {
            return null;
        }

        @Override
        public JmUserInfo login(String username, String password) {
            cacheUsername(username);
            cacheUserId("u_123");
            return new JmUserInfo("u_123", username, "email@test.com", true, "", "", "", "", 0, 0, 1, "", 100, 50, 0.5, 100);
        }

        @Override
        public void logout() {
            clearLoginSession();
        }

        @Override
        public void forgotPassword(String email) {}

        @Override
        public JmUserProfile getUserProfile(String uid) {
            return null;
        }

        @Override
        public JmUserProfile editUserProfile(String uid, java.util.Map<String, String> params) {
            return null;
        }

        @Override
        public JmCommentList getComments(ForumQuery query) {
            return null;
        }

        @Override
        public JmComment postComment(String entityId, String commentText) {
            return null;
        }

        @Override
        public JmComment replyToComment(String entityId, String commentText, String parentCommentId) {
            return null;
        }

        @Override
        public JmComment postBlogComment(String albumId, String blogId, String commentText) {
            return null;
        }

        @Override
        public JmComment replyToBlogComment(String albumId, String blogId, String commentText, String parentCommentId) {
            return null;
        }

        @Override
        public JmVoteResult voteComment(String commentId, io.github.jukomu.jmcomic.api.enums.VoteType voteType) {
            return null;
        }

        @Override
        public JmFavoritePage getFavorites(FavoriteQuery query) {
            return null;
        }

        @Override
        public JmFavoriteFolderResult manageFavoriteFolder(io.github.jukomu.jmcomic.api.enums.FavoriteFolderType type, String folderId, String folderName, String albumId) {
            return null;
        }

        @Override
        public void toggleAlbumFavorite(String albumId, String folderId) {}

        @Override
        public List<JmTagFavorite> getTagsFavorite() {
            return List.of();
        }

        @Override
        public void addFavoriteTags(List<String> tags) {}

        @Override
        public void removeFavoriteTags(List<String> tags) {}

        @Override
        public void toggleAlbumLike(String albumId) {}

        @Override
        public List<JmAlbumMeta> getWatchHistory(int page) {
            return List.of();
        }

        @Override
        public void deleteWatchHistory(String id) {}

        @Override
        public List<String> getHotTags() {
            return List.of("tag1", "tag2");
        }

        @Override
        public JmSearchPage getLatest(int page) {
            return new JmSearchPage(1, 10, 1, List.of());
        }

        @Override
        public List<JmAlbumMeta> getRandomRecommend() {
            return List.of();
        }

        @Override
        public List<JmPromoteCategory> getPromote() {
            return List.of();
        }

        @Override
        public JmSearchPage getPromoteList(JmPromoteCategory category, int page) {
            return new JmSearchPage(1, 10, 1, List.of());
        }

        @Override
        public JmSearchPage getSerialization(int page) {
            return new JmSearchPage(1, 10, 1, List.of());
        }

        @Override
        public JmWeeklyPicksList getWeeklyPicksList() {
            return null;
        }

        @Override
        public JmWeeklyPicksDetail getWeeklyPicksDetail(String categoryId) {
            return null;
        }

        @Override
        public JmNotificationPage getNotifications() {
            return null;
        }

        @Override
        public void markNotification(String id, int read) {}

        @Override
        public java.util.Map getUnreadCount() {
            return java.util.Map.of("count", 0);
        }

        @Override
        public boolean getAlbumSertracking(String id) {
            return false;
        }

        @Override
        public void setAlbumSertracking(String id) {}

        @Override
        public JmTrackingPage getAlbumTrackingList(int page) {
            return null;
        }

        @Override
        public JmTaskList getTasks(String type, String filter) {
            return null;
        }

        @Override
        public java.util.Map claimTask(java.util.Map<String, String> body) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map getCoinBuyList(java.util.Map<String, String> body) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map buyComicWithCoin(String comicId) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map chargeCoins() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map setAdFree(java.util.Map<String, String> body) {
            return java.util.Map.of();
        }

        @Override
        public JmDailyCheckInStatus getDailyCheckInStatus(String userId) {
            return new JmDailyCheckInStatus(101, "0", "0", "0", "0", "Event", "", "", "0%", List.of());
        }

        @Override
        public void doDailyCheckin(String userId, String dailyId) {}

        @Override
        public List getDailyCheckInOptions(String userId) {
            return List.of();
        }

        @Override
        public List filterDailyCheckInList(String filter) {
            return List.of();
        }

        @Override
        public java.util.Map register(String username, String password, String passwordConfirm, String email) {
            return java.util.Map.of();
        }
    }
}
