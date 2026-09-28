package io.github.jukomu.jmcomic.core;

import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.api.enums.FavoriteFolderType;
import io.github.jukomu.jmcomic.api.enums.VoteType;
import io.github.jukomu.jmcomic.api.exception.JmClientInitializationException;
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.core.client.AbstractJmClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import io.github.jukomu.jmcomic.core.net.provider.DomainProbe;
import io.github.jukomu.jmcomic.core.net.provider.JmDomainManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户端初始化状态机测试：
 * 验证瞬时失败自动重试、重试耗尽后失败、失败后可重新初始化、
 * 以及阻塞等待初始化的调用方可触发自愈。
 */
class ClientInitializationRetryTest {

    /**
     * 初始化行为可控的测试客户端（不产生任何真实网络请求）。
     */
    private static class FlakyInitClient extends AbstractJmClient {
        final AtomicInteger initializeCalls = new AtomicInteger();
        final AtomicInteger domainProbeCalls = new AtomicInteger();
        volatile int failuresBeforeSuccess;
        volatile boolean failAll;

        FlakyInitClient(int initRetryTimes, long initRetryBackoffMs) {
            super(JmConfiguration.builder()
                            .clientType(ClientType.API)
                            .initRetryTimes(initRetryTimes)
                            .initRetryBackoffMs(initRetryBackoffMs)
                            .build(),
                    new okhttp3.OkHttpClient(),
                    new java.net.CookieManager(),
                    new JmDomainManager(List.of("18comic.vip")));
        }

        JmDomainManager domainManagerForTest() {
            return domainManager;
        }

        @Override
        protected void initialize() {
            int call = initializeCalls.incrementAndGet();
            if (failAll || call <= failuresBeforeSuccess) {
                throw new RuntimeException("transient failure #" + call);
            }
        }

        @Override
        protected void updateDomains() {
        }

        @Override
        protected DomainProbe createDomainProbe() {
            return domain -> {
                domainProbeCalls.incrementAndGet();
                return true;
            };
        }

        @Override
        public JmAlbum getAlbum(String albumId) {
            return null;
        }

        @Override
        public JmAlbum getComicRead(String comicId) {
            return null;
        }

        @Override
        public JmPhoto getPhoto(String photoId) {
            return null;
        }

        @Override
        public JmSearchPage search(SearchQuery query) {
            return null;
        }

        @Override
        public JmSearchPage getCategories(SearchQuery query) {
            return null;
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
            return null;
        }

        @Override
        public void logout() {
        }

        @Override
        public void forgotPassword(String email) {
        }

        @Override
        public JmUserProfile getUserProfile(String uid) {
            return null;
        }

        @Override
        public JmUserProfile editUserProfile(String uid, Map<String, String> params) {
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
        public JmVoteResult voteComment(String commentId, VoteType voteType) {
            return null;
        }

        @Override
        public JmFavoritePage getFavorites(FavoriteQuery query) {
            return null;
        }

        @Override
        public JmFavoriteFolderResult manageFavoriteFolder(FavoriteFolderType type, String folderId, String folderName, String albumId) {
            return null;
        }

        @Override
        public void toggleAlbumFavorite(String albumId, String folderId) {
        }

        @Override
        public List<JmTagFavorite> getTagsFavorite() {
            return List.of();
        }

        @Override
        public void addFavoriteTags(List<String> tags) {
        }

        @Override
        public void removeFavoriteTags(List<String> tags) {
        }

        @Override
        public void toggleAlbumLike(String albumId) {
        }

        @Override
        public List<JmAlbumMeta> getWatchHistory(int page) {
            return List.of();
        }

        @Override
        public void deleteWatchHistory(String id) {
        }

        @Override
        public List<String> getHotTags() {
            return List.of();
        }

        @Override
        public JmSearchPage getLatest(int page) {
            return null;
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
            return null;
        }

        @Override
        public JmSearchPage getSerialization(int page) {
            return null;
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
        public void markNotification(String id, int read) {
        }

        @Override
        public Map getUnreadCount() {
            return Map.of();
        }

        @Override
        public boolean getAlbumSertracking(String id) {
            return false;
        }

        @Override
        public void setAlbumSertracking(String id) {
        }

        @Override
        public JmTrackingPage getAlbumTrackingList(int page) {
            return null;
        }

        @Override
        public JmTaskList getTasks(String type, String filter) {
            return null;
        }

        @Override
        public Map claimTask(Map<String, String> body) {
            return Map.of();
        }

        @Override
        public Map getCoinBuyList(Map<String, String> body) {
            return Map.of();
        }

        @Override
        public Map buyComicWithCoin(String comicId) {
            return Map.of();
        }

        @Override
        public Map chargeCoins() {
            return Map.of();
        }

        @Override
        public Map setAdFree(Map<String, String> body) {
            return Map.of();
        }

        @Override
        public JmDailyCheckInStatus getDailyCheckInStatus(String userId) {
            return null;
        }

        @Override
        public void doDailyCheckin(String userId, String dailyId) {
        }

        @Override
        public List getDailyCheckInOptions(String userId) {
            return List.of();
        }

        @Override
        public List filterDailyCheckInList(String filter) {
            return List.of();
        }

        @Override
        public Map register(String username, String password, String passwordConfirm, String email) {
            return Map.of();
        }
    }

    @Test
    @DisplayName("瞬时失败自动重试后初始化成功")
    void initializationRetriesTransientFailures() throws Exception {
        try (FlakyInitClient client = new FlakyInitClient(3, 10)) {
            client.failuresBeforeSuccess = 2;
            client.initializeAsync().get(30, TimeUnit.SECONDS);
            assertTrue(client.isInitialized());
            assertEquals(3, client.initializeCalls.get(), "应恰好尝试 3 次（前两次失败）");
        }
    }

    @Test
    @DisplayName("重试耗尽后初始化以 JmClientInitializationException 失败")
    void initializationFailsAfterRetriesExhausted() {
        try (FlakyInitClient client = new FlakyInitClient(2, 10)) {
            client.failAll = true;
            CompletableFuture<Void> future = client.initializeAsync();
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> future.get(30, TimeUnit.SECONDS));
            assertInstanceOf(JmClientInitializationException.class, error.getCause());
            assertEquals(3, client.initializeCalls.get(), "总尝试应为 1 + initRetryTimes = 3 次");
            assertFalse(client.isInitialized());
        }
    }

    @Test
    @DisplayName("初始化失败后可重新初始化并恢复（重建被清理的资源）")
    void failedClientRecoversOnRestart() throws Exception {
        FlakyInitClient client = new FlakyInitClient(1, 10);
        try {
            client.failAll = true;
            CompletableFuture<Void> first = client.initializeAsync();
            assertThrows(ExecutionException.class, () -> first.get(30, TimeUnit.SECONDS));
            assertFalse(client.isInitialized());

            // 故障恢复后手动触发重新初始化
            client.failAll = false;
            client.initializeAsync().get(30, TimeUnit.SECONDS);
            assertTrue(client.isInitialized());
            assertEquals(3, client.initializeCalls.get(), "首轮 2 次尝试 + 重启后 1 次 = 3 次");
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("阻塞等待初始化的调用方在失败后可触发自愈")
    void blockedCallerTriggersSelfHealing() throws Exception {
        FlakyInitClient client = new FlakyInitClient(1, 10);
        try {
            client.failAll = true;
            assertThrows(ExecutionException.class,
                    () -> client.initializeAsync().get(30, TimeUnit.SECONDS));

            // 模拟故障恢复，随后像请求路径那样阻塞等待域名就绪（内部会触发重启）
            client.failAll = false;
            String domain = client.domainManagerForTest().getBestDomain();
            assertNotNull(domain, "getBestDomain 应在触发重新初始化后返回可用域名");
            assertTrue(client.isInitialized(), "重新初始化应已完成");
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("关闭后的客户端不再允许重新初始化")
    void closedClientStaysClosed() throws Exception {
        FlakyInitClient client = new FlakyInitClient(0, 10);
        client.failAll = true;
        assertThrows(ExecutionException.class,
                () -> client.initializeAsync().get(30, TimeUnit.SECONDS));
        client.close();
        client.failAll = false;

        CompletableFuture<Void> future = client.initializeAsync();
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS));
        assertInstanceOf(JmClientInitializationException.class, error.getCause());
        assertEquals(1, client.initializeCalls.get(), "关闭后不应再触发初始化");
    }

    @Test
    @DisplayName("awaitInitialized 成功与失败路径")
    void awaitInitializedSemantics() {
        FlakyInitClient failing = new FlakyInitClient(0, 10);
        try {
            failing.failAll = true;
            failing.initializeAsync();
            assertThrows(JmClientInitializationException.class, failing::awaitInitialized);
        } finally {
            failing.close();
        }

        try (FlakyInitClient healthy = new FlakyInitClient(0, 10)) {
            healthy.initializeAsync();
            assertDoesNotThrow(healthy::awaitInitialized);
            assertTrue(healthy.isInitialized());
        }
    }
}
