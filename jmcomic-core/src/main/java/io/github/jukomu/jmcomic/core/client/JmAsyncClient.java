package io.github.jukomu.jmcomic.core.client;

import io.github.jukomu.jmcomic.api.enums.FavoriteFolderType;
import io.github.jukomu.jmcomic.api.enums.VoteType;
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * {@link JmComicClient} 的异步非阻塞客户端封装。
 * <p>
 * 所有方法均返回 {@link CompletableFuture} 且结果包装为 {@link JmResult}，
 * 与 picapi 的 `PicaAsyncClient` 设计完全对齐，专为现代 Java 17+ 异步响应式调用
 * 以及 Android 主线程调用设计，杜绝阻塞主线程导致 ANR 或 NetworkOnMainThreadException。
 */
public class JmAsyncClient {

    private final JmComicClient client;
    private final Executor executor;

    public JmAsyncClient(JmComicClient client) {
        this(client, ForkJoinPool.commonPool());
    }

    public JmAsyncClient(JmComicClient client, Executor executor) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    }

    /**
     * 获取关联的同步客户端。
     *
     * @return JmComicClient 实例
     */
    public JmComicClient sync() {
        return client;
    }

    // =========================================================================
    // 核心数据获取层 (Core Data Retrieval)
    // =========================================================================

    public CompletableFuture<JmResult<JmAlbum>> getAlbum(String albumId) {
        return CompletableFuture.supplyAsync(() -> client.getAlbum(albumId), executor);
    }

    public CompletableFuture<JmResult<JmAlbum>> getComicRead(String comicId) {
        return CompletableFuture.supplyAsync(() -> client.getComicRead(comicId), executor);
    }

    public CompletableFuture<JmResult<JmPhoto>> getPhoto(String photoId) {
        return CompletableFuture.supplyAsync(() -> client.getPhoto(photoId), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> search(SearchQuery query) {
        return CompletableFuture.supplyAsync(() -> client.search(query), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> search(String keyword) {
        return CompletableFuture.supplyAsync(() -> client.search(keyword), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> search(String keyword, int page) {
        return CompletableFuture.supplyAsync(() -> client.search(keyword, page), executor);
    }

    public CompletableFuture<JmResult<byte[]>> fetchImageBytes(JmImage image) {
        return CompletableFuture.supplyAsync(() -> client.fetchImageBytes(image), executor);
    }

    public CompletableFuture<JmResult<byte[]>> fetchDecodedImageBytes(JmImage image) {
        return CompletableFuture.supplyAsync(() -> client.fetchDecodedImageBytes(image), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> getCategories(SearchQuery query) {
        return CompletableFuture.supplyAsync(() -> client.getCategories(query), executor);
    }

    public CompletableFuture<JmResult<JmCategoryList>> getCategoriesList() {
        return CompletableFuture.supplyAsync(client::getCategoriesList, executor);
    }

    public CompletableFuture<JmResult<JmAlbumDownloadInfo>> getAlbumDownloadInfo(String albumId) {
        return CompletableFuture.supplyAsync(() -> client.getAlbumDownloadInfo(albumId), executor);
    }

    // =========================================================================
    // 会话与用户管理 (Session & User Management)
    // =========================================================================

    public CompletableFuture<JmResult<JmUserInfo>> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> client.login(username, password), executor);
    }

    public CompletableFuture<JmResult<Void>> logout() {
        return CompletableFuture.supplyAsync(client::logout, executor);
    }

    public CompletableFuture<JmResult<JmUserProfile>> getUserProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> client.getUserProfile(uid), executor);
    }

    public CompletableFuture<JmResult<JmUserProfile>> getUserProfile() {
        return CompletableFuture.supplyAsync(client::getUserProfile, executor);
    }

    public CompletableFuture<JmResult<JmUserProfile>> editUserProfile(String uid, Map<String, String> params) {
        return CompletableFuture.supplyAsync(() -> client.editUserProfile(uid, params), executor);
    }

    // =========================================================================
    // 评论系统 (Comment System)
    // =========================================================================

    public CompletableFuture<JmResult<JmCommentList>> getComments(ForumQuery query) {
        return CompletableFuture.supplyAsync(() -> client.getComments(query), executor);
    }

    public CompletableFuture<JmResult<JmComment>> postComment(String entityId, String commentText) {
        return CompletableFuture.supplyAsync(() -> client.postComment(entityId, commentText), executor);
    }

    public CompletableFuture<JmResult<JmComment>> replyToComment(String entityId, String commentText, String parentCommentId) {
        return CompletableFuture.supplyAsync(() -> client.replyToComment(entityId, commentText, parentCommentId), executor);
    }

    public CompletableFuture<JmResult<JmComment>> postBlogComment(String albumId, String blogId, String commentText) {
        return CompletableFuture.supplyAsync(() -> client.postBlogComment(albumId, blogId, commentText), executor);
    }

    public CompletableFuture<JmResult<JmComment>> replyToBlogComment(String albumId, String blogId, String commentText, String parentCommentId) {
        return CompletableFuture.supplyAsync(() -> client.replyToBlogComment(albumId, blogId, commentText, parentCommentId), executor);
    }

    @Deprecated
    public CompletableFuture<JmResult<JmVoteResult>> voteComment(String commentId, VoteType voteType) {
        return CompletableFuture.supplyAsync(() -> client.voteComment(commentId, voteType), executor);
    }

    // =========================================================================
    // 收藏系统 (Favorites)
    // =========================================================================

    public CompletableFuture<JmResult<JmFavoritePage>> getFavorites(FavoriteQuery query) {
        return CompletableFuture.supplyAsync(() -> client.getFavorites(query), executor);
    }

    public CompletableFuture<JmResult<JmFavoritePage>> getFavorites(int page) {
        return CompletableFuture.supplyAsync(() -> client.getFavorites(page), executor);
    }

    public CompletableFuture<JmResult<JmFavoriteFolderResult>> manageFavoriteFolder(FavoriteFolderType type, String folderId, String folderName, String albumId) {
        return CompletableFuture.supplyAsync(() -> client.manageFavoriteFolder(type, folderId, folderName, albumId), executor);
    }

    public CompletableFuture<JmResult<Void>> toggleAlbumFavorite(String albumId) {
        return CompletableFuture.supplyAsync(() -> client.toggleAlbumFavorite(albumId), executor);
    }

    public CompletableFuture<JmResult<Void>> toggleAlbumFavorite(String albumId, String folderId) {
        return CompletableFuture.supplyAsync(() -> client.toggleAlbumFavorite(albumId, folderId), executor);
    }

    public CompletableFuture<JmResult<List<JmTagFavorite>>> getTagsFavorite() {
        return CompletableFuture.supplyAsync(client::getTagsFavorite, executor);
    }

    public CompletableFuture<JmResult<Void>> addFavoriteTags(List<String> tags) {
        return CompletableFuture.supplyAsync(() -> client.addFavoriteTags(tags), executor);
    }

    public CompletableFuture<JmResult<Void>> removeFavoriteTags(List<String> tags) {
        return CompletableFuture.supplyAsync(() -> client.removeFavoriteTags(tags), executor);
    }

    // =========================================================================
    // 点赞与历史 (Likes & History)
    // =========================================================================

    public CompletableFuture<JmResult<Void>> toggleAlbumLike(String albumId) {
        return CompletableFuture.supplyAsync(() -> client.toggleAlbumLike(albumId), executor);
    }

    public CompletableFuture<JmResult<List<JmAlbumMeta>>> getWatchHistory(int page) {
        return CompletableFuture.supplyAsync(() -> client.getWatchHistory(page), executor);
    }

    public CompletableFuture<JmResult<Void>> deleteWatchHistory(String id) {
        return CompletableFuture.supplyAsync(() -> client.deleteWatchHistory(id), executor);
    }

    // =========================================================================
    // 发现与推荐 (Discovery & Recommendations)
    // =========================================================================

    public CompletableFuture<JmResult<List<String>>> getHotTags() {
        return CompletableFuture.supplyAsync(client::getHotTags, executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> getLatest(int page) {
        return CompletableFuture.supplyAsync(() -> client.getLatest(page), executor);
    }

    public CompletableFuture<JmResult<List<JmAlbumMeta>>> getRandomRecommend() {
        return CompletableFuture.supplyAsync(client::getRandomRecommend, executor);
    }

    public CompletableFuture<JmResult<List<JmPromoteCategory>>> getPromote() {
        return CompletableFuture.supplyAsync(client::getPromote, executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> getPromoteList(JmPromoteCategory category, int page) {
        return CompletableFuture.supplyAsync(() -> client.getPromoteList(category, page), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> getSerialization(int page) {
        return CompletableFuture.supplyAsync(() -> client.getSerialization(page), executor);
    }

    public CompletableFuture<JmResult<JmWeeklyPicksList>> getWeeklyPicksList() {
        return CompletableFuture.supplyAsync(client::getWeeklyPicksList, executor);
    }

    public CompletableFuture<JmResult<JmWeeklyPicksDetail>> getWeeklyPicksDetail(String categoryId) {
        return CompletableFuture.supplyAsync(() -> client.getWeeklyPicksDetail(categoryId), executor);
    }

    // =========================================================================
    // 通知与签到 (Notifications & Check-in)
    // =========================================================================

    public CompletableFuture<JmResult<JmNotificationPage>> getNotifications() {
        return CompletableFuture.supplyAsync(client::getNotifications, executor);
    }

    public CompletableFuture<JmResult<Void>> markNotification(String id, int read) {
        return CompletableFuture.supplyAsync(() -> client.markNotification(id, read), executor);
    }

    public CompletableFuture<JmResult<Map>> getUnreadCount() {
        return CompletableFuture.supplyAsync(client::getUnreadCount, executor);
    }

    public CompletableFuture<JmResult<Boolean>> getAlbumSertracking(String id) {
        return CompletableFuture.supplyAsync(() -> client.getAlbumSertracking(id), executor);
    }

    public CompletableFuture<JmResult<Void>> setAlbumSertracking(String id) {
        return CompletableFuture.supplyAsync(() -> client.setAlbumSertracking(id), executor);
    }

    public CompletableFuture<JmResult<JmTrackingPage>> getAlbumTrackingList(int page) {
        return CompletableFuture.supplyAsync(() -> client.getAlbumTrackingList(page), executor);
    }

    public CompletableFuture<JmResult<JmTaskList>> getTasks(String type, String filter) {
        return CompletableFuture.supplyAsync(() -> client.getTasks(type, filter), executor);
    }

    public CompletableFuture<JmResult<JmDailyCheckInStatus>> getDailyCheckInStatus(String userId) {
        return CompletableFuture.supplyAsync(() -> client.getDailyCheckInStatus(userId), executor);
    }

    public CompletableFuture<JmResult<JmDailyCheckInStatus>> getDailyCheckInStatus() {
        return CompletableFuture.supplyAsync(client::getDailyCheckInStatus, executor);
    }

    public CompletableFuture<JmResult<Void>> doDailyCheckin(String userId, String dailyId) {
        return CompletableFuture.supplyAsync(() -> client.doDailyCheckin(userId, dailyId), executor);
    }

    public CompletableFuture<JmResult<List>> getDailyCheckInOptions(String userId) {
        return CompletableFuture.supplyAsync(() -> client.getDailyCheckInOptions(userId), executor);
    }

    public CompletableFuture<JmResult<List>> filterDailyCheckInList(String filter) {
        return CompletableFuture.supplyAsync(() -> client.filterDailyCheckInList(filter), executor);
    }

    // =========================================================================
    // PicACG 对齐别名与便捷语义方法 (PicACG Semantic Parity Methods)
    // =========================================================================

    public CompletableFuture<JmResult<JmAlbum>> getComicDetail(String comicId) {
        return getAlbum(comicId);
    }

    public CompletableFuture<JmResult<List<JmPhotoMeta>>> getComicEpisodes(String comicId) {
        return CompletableFuture.supplyAsync(() -> client.getComicEpisodes(comicId), executor);
    }

    public CompletableFuture<JmResult<JmPhoto>> getComicEpisode(String episodeId) {
        return getPhoto(episodeId);
    }

    public CompletableFuture<JmResult<List<JmImage>>> getComicPages(String episodeId) {
        return CompletableFuture.supplyAsync(() -> client.getComicPages(episodeId), executor);
    }

    public CompletableFuture<JmResult<JmSearchPage>> searchComics(SearchQuery query) {
        return search(query);
    }

    public CompletableFuture<JmResult<JmSearchPage>> searchComics(String keyword) {
        return search(keyword);
    }

    public CompletableFuture<JmResult<JmSearchPage>> searchComics(String keyword, int page) {
        return search(keyword, page);
    }

    public CompletableFuture<JmResult<Void>> favouriteComic(String comicId) {
        return toggleAlbumFavorite(comicId);
    }

    public CompletableFuture<JmResult<Void>> likeComic(String comicId) {
        return toggleAlbumLike(comicId);
    }

    public CompletableFuture<JmResult<JmFavoritePage>> getFavouriteComics(int page) {
        return getFavorites(page);
    }

    public CompletableFuture<JmResult<JmFavoritePage>> getFavouriteComics(FavoriteQuery query) {
        return getFavorites(query);
    }

    public CompletableFuture<JmResult<JmComment>> postComicComment(String comicId, String content) {
        return postComment(comicId, content);
    }

    public CompletableFuture<JmResult<JmCommentList>> getComicComments(String comicId) {
        return CompletableFuture.supplyAsync(() -> client.getComicComments(comicId), executor);
    }

    public CompletableFuture<JmResult<JmCommentList>> getComicComments(String comicId, int page) {
        return CompletableFuture.supplyAsync(() -> client.getComicComments(comicId, page), executor);
    }

    public CompletableFuture<JmResult<Void>> punchIn() {
        return CompletableFuture.supplyAsync(client::punchIn, executor);
    }

    public CompletableFuture<JmResult<Void>> punchIn(String userId, String dailyId) {
        return doDailyCheckin(userId, dailyId);
    }
}
