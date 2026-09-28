package io.github.jukomu.jmcomic.core.client;

import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.api.enums.FavoriteFolderType;
import io.github.jukomu.jmcomic.api.enums.VoteType;
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;
import io.github.jukomu.jmcomic.core.JmComic;
import io.github.jukomu.jmcomic.core.JmSession;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import io.github.jukomu.jmcomic.core.crypto.JmImageTool;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * JMComic 面向 Java 与 Kotlin 开发者的高级客户端门面。
 * <p>
 * 特性：
 * <ul>
 *   <li>全面返回 {@link JmResult}，彻底消除繁琐的 try-catch，并提供函数式链式调用（onSuccess、onFailure、map、fold）。</li>
 *   <li>提供与 PicACG (picapi) 高度一致的语义化别名方法（如 getComicDetail、getComicEpisodes、getComicPages、searchComics、favouriteComic 等），大幅降低多图源客户端的集成和适配成本。</li>
 *   <li>内置 {@link #async()} 异步非阻塞客户端，所有方法均返回 {@link CompletableFuture}。</li>
 *   <li>内置自动解密重组图片方法 {@link #fetchDecodedImageBytes(JmImage)}。</li>
 * </ul>
 */
public class JmComicClient implements Closeable {

    private final AbstractJmClient rawClient;
    private final JmAsyncClient asyncClient;

    /**
     * 使用现有的 {@link AbstractJmClient} 构造门面客户端。
     *
     * @param rawClient 底层客户端实例
     */
    public JmComicClient(AbstractJmClient rawClient) {
        this.rawClient = Objects.requireNonNull(rawClient, "rawClient cannot be null");
        this.asyncClient = new JmAsyncClient(this);
    }

    /**
     * 使用默认配置创建一个新的同步客户端。
     *
     * @return JmComicClient 实例
     */
    public static JmComicClient create() {
        return create(JmConfiguration.defaultConfig());
    }

    /**
     * 根据指定配置创建一个新的同步客户端。
     * 按 {@code config.getClientType()} 自动选择底层实现（API 或 HTML）。
     *
     * @param config 客户端配置
     * @return JmComicClient 实例
     */
    public static JmComicClient create(JmConfiguration config) {
        AbstractJmClient client = config.getClientType() == ClientType.HTML
                ? JmComic.newHtmlClient(config)
                : JmComic.newApiClient(config);
        return new JmComicClient(client);
    }

    /**
     * 根据指定配置异步创建并完整初始化一个客户端。
     * 按 {@code config.getClientType()} 自动选择底层实现（API 或 HTML）。
     *
     * @param config 客户端配置
     * @return CompletableFuture 包装的 JmComicClient
     */
    public static CompletableFuture<JmComicClient> createAsync(JmConfiguration config) {
        CompletableFuture<? extends AbstractJmClient> future = config.getClientType() == ClientType.HTML
                ? JmComic.newHtmlClientAsync(config)
                : JmComic.newApiClientAsync(config);
        return future.thenApply(JmComicClient::new);
    }

    /**
     * 包装现有的底层客户端。
     *
     * @param rawClient 底层客户端实例
     * @return JmComicClient 包装实例
     */
    public static JmComicClient wrap(AbstractJmClient rawClient) {
        return new JmComicClient(rawClient);
    }

    /**
     * 获取异步非阻塞客户端。
     *
     * @return JmAsyncClient 实例
     */
    public JmAsyncClient async() {
        return asyncClient;
    }

    /**
     * 获取异步非阻塞客户端（Java getter 兼容）。
     *
     * @return JmAsyncClient 实例
     */
    public JmAsyncClient getAsync() {
        return asyncClient;
    }

    /**
     * 获取底层原生客户端实例（可调用下载管理器或底层扩展方法）。
     *
     * @return 底层 AbstractJmClient
     */
    public AbstractJmClient getRawClient() {
        return rawClient;
    }

    /**
     * 是否已经登录。
     *
     * @return true 表示已具备登录身份
     */
    public boolean isLoggedIn() {
        return rawClient.isLoggedIn();
    }

    /**
     * 是否已完成初始化。
     *
     * @return true 表示已完成初始化探测
     */
    public boolean isInitialized() {
        return rawClient.isInitialized();
    }

    /**
     * 获取初始化 Future：仅在客户端完整初始化后成功完成。
     * 初始化失败（含自动重试后仍失败）时以 {@link io.github.jukomu.jmcomic.api.exception.JmClientInitializationException} 异常完成。
     *
     * @return 初始化 Future
     */
    public CompletableFuture<Void> initializationFuture() {
        return rawClient.initializationFuture();
    }

    /**
     * 阻塞等待客户端完成初始化。
     * 适用于"创建后立即调用业务方法"前确保就绪的场景。
     *
     * @throws io.github.jukomu.jmcomic.api.exception.JmClientInitializationException 初始化失败时
     */
    public void awaitInitialized() {
        rawClient.awaitInitialized();
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名，未登录时为 null
     */
    public String getUsername() {
        return rawClient.getUsername();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未登录时为 null
     */
    public String getLoggedInUserId() {
        return rawClient.getLoggedInUserId();
    }

    // =========================================================================
    // 核心数据获取层 (Core Data Retrieval)
    // =========================================================================

    /**
     * 根据本子 ID 获取本子详情。
     *
     * @param albumId 本子 ID
     * @return JmResult 包装的本子详情
     */
    public JmResult<JmAlbum> getAlbum(String albumId) {
        return JmResult.runCatching(() -> rawClient.getAlbum(albumId));
    }

    /**
     * 获取漫画阅读数据（包含预加载的图片列表）。
     *
     * @param comicId 漫画 ID
     * @return JmResult 包装的包含图片列表的 JmAlbum
     */
    public JmResult<JmAlbum> getComicRead(String comicId) {
        return JmResult.runCatching(() -> rawClient.getComicRead(comicId));
    }

    /**
     * 根据章节 ID 获取章节详情（包含该章节的全部图片列表）。
     *
     * @param photoId 章节 ID
     * @return JmResult 包装的章节详情
     */
    public JmResult<JmPhoto> getPhoto(String photoId) {
        return JmResult.runCatching(() -> rawClient.getPhoto(photoId));
    }

    /**
     * 搜索本子。
     *
     * @param query 搜索参数对象
     * @return JmResult 包装的搜索结果页
     */
    public JmResult<JmSearchPage> search(SearchQuery query) {
        return JmResult.runCatching(() -> rawClient.search(query));
    }

    /**
     * 搜索本子（便捷关键词重载）。
     *
     * @param keyword 关键词
     * @return JmResult 包装的搜索结果页
     */
    public JmResult<JmSearchPage> search(String keyword) {
        return search(SearchQuery.of(keyword));
    }

    /**
     * 搜索本子（便捷关键词与页码重载）。
     *
     * @param keyword 关键词
     * @param page    页码（从 1 开始）
     * @return JmResult 包装的搜索结果页
     */
    public JmResult<JmSearchPage> search(String keyword, int page) {
        return search(SearchQuery.of(keyword, page));
    }

    /**
     * 下载获取单张图片的原始二进制数据。
     *
     * @param image 图片元数据对象
     * @return JmResult 包装的原始图片字节数组
     */
    public JmResult<byte[]> fetchImageBytes(JmImage image) {
        return JmResult.runCatching(() -> rawClient.fetchImageBytes(image));
    }

    /**
     * 下载获取单张图片并自动完成解密切片重组（如果需要）。
     * 直接可交给 ImageIO、Glide、Coil 等直接显示。
     *
     * @param image 图片元数据对象
     * @return JmResult 包装的已解密图片字节数组
     */
    public JmResult<byte[]> fetchDecodedImageBytes(JmImage image) {
        return JmResult.runCatching(() -> {
            byte[] bytes = rawClient.fetchImageBytes(image);
            return JmImageTool.decryptImage(bytes, image);
        });
    }

    /**
     * 获取分类排行。
     *
     * @param query 查询参数
     * @return JmResult 包装的搜索分页结果
     */
    public JmResult<JmSearchPage> getCategories(SearchQuery query) {
        return JmResult.runCatching(() -> rawClient.getCategories(query));
    }

    /**
     * 获取分类列表（包含分类树和标签块）。
     *
     * @return JmResult 包装的分类列表
     */
    public JmResult<JmCategoryList> getCategoriesList() {
        return JmResult.runCatching(rawClient::getCategoriesList);
    }

    /**
     * 获取本子下载信息。
     *
     * @param albumId 本子 ID
     * @return JmResult 包装的下载信息
     */
    public JmResult<JmAlbumDownloadInfo> getAlbumDownloadInfo(String albumId) {
        return JmResult.runCatching(() -> rawClient.getAlbumDownloadInfo(albumId));
    }

    // =========================================================================
    // 会话与用户管理 (Session & User Management)
    // =========================================================================

    /**
     * 登录。
     *
     * @param username 用户名
     * @param password 密码
     * @return JmResult 包装的用户信息
     */
    public JmResult<JmUserInfo> login(String username, String password) {
        return JmResult.runCatching(() -> rawClient.login(username, password));
    }

    /**
     * 登出并清除登录状态。
     *
     * @return JmResult 成功或失败
     */
    public JmResult<Void> logout() {
        return JmResult.runCatching(() -> {
            rawClient.logout();
            return null;
        });
    }

    /**
     * 恢复登录会话状态与 Cookies。
     *
     * @param username      用户名
     * @param userId        用户 ID
     * @param cookieStrings Cookie 列表
     */
    public void restoreSession(String username, String userId, List<String> cookieStrings) {
        rawClient.restoreSession(username, userId, cookieStrings);
    }

    /**
     * 恢复已保存的会话对象。
     *
     * @param session 会话信息
     */
    public void restoreSession(JmSession session) {
        if (session != null && session.isValid()) {
            rawClient.restoreSession(session.getUsername(), session.getUserId(), session.getCookies());
        }
    }

    /**
     * 提取当前会话快照。
     *
     * @return 当前 JmSession 实例
     */
    public JmSession extractSession() {
        return new JmSession(rawClient.getUsername(), rawClient.getLoggedInUserId(), rawClient.extractCookies());
    }

    /**
     * 提取当前会话中的所有 Cookie 字符串。
     *
     * @return Cookie 字符串列表
     */
    public List<String> extractCookies() {
        return rawClient.extractCookies();
    }

    /**
     * 获取指定用户的个人资料。
     *
     * @param uid 用户 ID
     * @return JmResult 包装的用户资料
     */
    public JmResult<JmUserProfile> getUserProfile(String uid) {
        return JmResult.runCatching(() -> rawClient.getUserProfile(uid));
    }

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return JmResult 包装的用户资料
     */
    public JmResult<JmUserProfile> getUserProfile() {
        String uid = rawClient.getLoggedInUserId();
        if (uid == null || uid.isBlank()) {
            return JmResult.failure(new IllegalStateException("Not logged in or user ID is unknown. Please login first."));
        }
        return getUserProfile(uid);
    }

    /**
     * 编辑用户个人资料。
     *
     * @param uid    用户 ID
     * @param params 要更新的字段键值对
     * @return JmResult 包装的更新后用户资料
     */
    public JmResult<JmUserProfile> editUserProfile(String uid, Map<String, String> params) {
        return JmResult.runCatching(() -> rawClient.editUserProfile(uid, params));
    }

    // =========================================================================
    // 评论系统 (Comment System)
    // =========================================================================

    /**
     * 获取论坛评论列表。
     *
     * @param query 查询参数
     * @return JmResult 包装的评论列表
     */
    public JmResult<JmCommentList> getComments(ForumQuery query) {
        return JmResult.runCatching(() -> rawClient.getComments(query));
    }

    /**
     * 发表本子评论。
     *
     * @param entityId    本子 ID
     * @param commentText 评论内容
     * @return JmResult 包装的评论对象
     */
    public JmResult<JmComment> postComment(String entityId, String commentText) {
        return JmResult.runCatching(() -> rawClient.postComment(entityId, commentText));
    }

    /**
     * 回复评论。
     *
     * @param entityId        本子 ID
     * @param commentText     回复内容
     * @param parentCommentId 被回复评论 ID
     * @return JmResult 包装的评论对象
     */
    public JmResult<JmComment> replyToComment(String entityId, String commentText, String parentCommentId) {
        return JmResult.runCatching(() -> rawClient.replyToComment(entityId, commentText, parentCommentId));
    }

    /**
     * 发表博客评论。
     *
     * @param albumId     博客所属本子 ID
     * @param blogId      博客 ID
     * @param commentText 评论内容
     * @return JmResult 包装的评论对象
     */
    public JmResult<JmComment> postBlogComment(String albumId, String blogId, String commentText) {
        return JmResult.runCatching(() -> rawClient.postBlogComment(albumId, blogId, commentText));
    }

    /**
     * 回复博客评论。
     *
     * @param albumId         博客所属本子 ID
     * @param blogId      博客 ID
     * @param commentText     回复内容
     * @param parentCommentId 被回复评论 ID
     * @return JmResult 包装的评论对象
     */
    public JmResult<JmComment> replyToBlogComment(String albumId, String blogId, String commentText, String parentCommentId) {
        return JmResult.runCatching(() -> rawClient.replyToBlogComment(albumId, blogId, commentText, parentCommentId));
    }

    /**
     * 对评论投票（点赞/点踩）。
     *
     * @param commentId 评论 ID
     * @param voteType  投票类型
     * @return JmResult 包装的投票结果
     */
    @Deprecated
    public JmResult<JmVoteResult> voteComment(String commentId, VoteType voteType) {
        return JmResult.runCatching(() -> rawClient.voteComment(commentId, voteType));
    }

    // =========================================================================
    // 收藏系统 (Favorites)
    // =========================================================================

    /**
     * 获取收藏夹本子列表。
     *
     * @param query 查询参数
     * @return JmResult 包装的收藏夹结果页
     */
    public JmResult<JmFavoritePage> getFavorites(FavoriteQuery query) {
        return JmResult.runCatching(() -> rawClient.getFavorites(query));
    }

    /**
     * 获取指定页码的收藏夹本子列表。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的收藏夹结果页
     */
    public JmResult<JmFavoritePage> getFavorites(int page) {
        return getFavorites(FavoriteQuery.ofPage(page));
    }

    /**
     * 管理收藏文件夹（添加、重命名、移动、删除）。
     *
     * @param type       操作类型
     * @param folderId   文件夹 ID
     * @param folderName 文件夹名称
     * @param albumId    本子 ID
     * @return JmResult 包装的文件夹操作结果
     */
    public JmResult<JmFavoriteFolderResult> manageFavoriteFolder(FavoriteFolderType type, String folderId, String folderName, String albumId) {
        return JmResult.runCatching(() -> rawClient.manageFavoriteFolder(type, folderId, folderName, albumId));
    }

    /**
     * 切换本子收藏状态（收藏/取消收藏）。
     *
     * @param albumId 本子 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> toggleAlbumFavorite(String albumId) {
        return toggleAlbumFavorite(albumId, null);
    }

    /**
     * 切换本子收藏状态到指定文件夹。
     *
     * @param albumId  本子 ID
     * @param folderId 文件夹 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> toggleAlbumFavorite(String albumId, String folderId) {
        return JmResult.runCatching(() -> {
            rawClient.toggleAlbumFavorite(albumId, folderId);
            return null;
        });
    }

    /**
     * 获取收藏标签列表。
     *
     * @return JmResult 包装的收藏标签列表
     */
    public JmResult<List<JmTagFavorite>> getTagsFavorite() {
        return JmResult.runCatching(rawClient::getTagsFavorite);
    }

    /**
     * 添加收藏标签。
     *
     * @param tags 标签列表
     * @return JmResult 成功或失败
     */
    public JmResult<Void> addFavoriteTags(List<String> tags) {
        return JmResult.runCatching(() -> {
            rawClient.addFavoriteTags(tags);
            return null;
        });
    }

    /**
     * 删除收藏标签。
     *
     * @param tags 标签列表
     * @return JmResult 成功或失败
     */
    public JmResult<Void> removeFavoriteTags(List<String> tags) {
        return JmResult.runCatching(() -> {
            rawClient.removeFavoriteTags(tags);
            return null;
        });
    }

    // =========================================================================
    // 点赞与历史 (Likes & History)
    // =========================================================================

    /**
     * 切换本子点赞状态。
     *
     * @param albumId 本子 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> toggleAlbumLike(String albumId) {
        return JmResult.runCatching(() -> {
            rawClient.toggleAlbumLike(albumId);
            return null;
        });
    }

    /**
     * 获取观看历史。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的观看历史列表
     */
    public JmResult<List<JmAlbumMeta>> getWatchHistory(int page) {
        return JmResult.runCatching(() -> rawClient.getWatchHistory(page));
    }

    /**
     * 删除观看历史记录。
     *
     * @param id 漫画 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> deleteWatchHistory(String id) {
        return JmResult.runCatching(() -> {
            rawClient.deleteWatchHistory(id);
            return null;
        });
    }

    // =========================================================================
    // 发现与推荐 (Discovery & Recommendations)
    // =========================================================================

    /**
     * 获取热门搜索标签。
     *
     * @return JmResult 包装的热门标签列表
     */
    public JmResult<List<String>> getHotTags() {
        return JmResult.runCatching(rawClient::getHotTags);
    }

    /**
     * 获取最新上架的本子。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的搜索分页结果
     */
    public JmResult<JmSearchPage> getLatest(int page) {
        return JmResult.runCatching(() -> rawClient.getLatest(page));
    }

    /**
     * 获取随机推荐。
     *
     * @return JmResult 包装的随机推荐列表
     */
    public JmResult<List<JmAlbumMeta>> getRandomRecommend() {
        return JmResult.runCatching(rawClient::getRandomRecommend);
    }

    /**
     * 获取首页推荐栏分类。
     *
     * @return JmResult 包装的推荐栏分类列表
     */
    public JmResult<List<JmPromoteCategory>> getPromote() {
        return JmResult.runCatching(rawClient::getPromote);
    }

    /**
     * 获取推荐栏分类详情列表。
     *
     * @param category 推荐分类
     * @param page     页码（从 1 开始）
     * @return JmResult 包装的分页结果
     */
    public JmResult<JmSearchPage> getPromoteList(JmPromoteCategory category, int page) {
        return JmResult.runCatching(() -> rawClient.getPromoteList(category, page));
    }

    /**
     * 获取连载列表。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的连载分页结果
     */
    public JmResult<JmSearchPage> getSerialization(int page) {
        return JmResult.runCatching(() -> rawClient.getSerialization(page));
    }

    /**
     * 获取每周必看列表。
     *
     * @return JmResult 包装的每周必看列表
     */
    public JmResult<JmWeeklyPicksList> getWeeklyPicksList() {
        return JmResult.runCatching(rawClient::getWeeklyPicksList);
    }

    /**
     * 获取每周必看详情。
     *
     * @param categoryId 期数 ID
     * @return JmResult 包装的每周必看详情
     */
    public JmResult<JmWeeklyPicksDetail> getWeeklyPicksDetail(String categoryId) {
        return JmResult.runCatching(() -> rawClient.getWeeklyPicksDetail(categoryId));
    }

    // =========================================================================
    // 通知与签到 (Notifications & Check-in)
    // =========================================================================

    /**
     * 获取通知列表。
     *
     * @return JmResult 包装的通知分页结果
     */
    public JmResult<JmNotificationPage> getNotifications() {
        return JmResult.runCatching(rawClient::getNotifications);
    }

    /**
     * 标记通知已读/未读。
     *
     * @param id   通知 ID
     * @param read 0=未读, 1=已读
     * @return JmResult 成功或失败
     */
    public JmResult<Void> markNotification(String id, int read) {
        return JmResult.runCatching(() -> {
            rawClient.markNotification(id, read);
            return null;
        });
    }

    /**
     * 获取未读通知数量。
     *
     * @return JmResult 包装的未读数量 Map
     */
    public JmResult<Map> getUnreadCount() {
        return JmResult.runCatching(rawClient::getUnreadCount);
    }

    /**
     * 获取本子的连载跟踪状态。
     *
     * @param id 本子 ID
     * @return JmResult 包装的跟踪状态
     */
    public JmResult<Boolean> getAlbumSertracking(String id) {
        return JmResult.runCatching(() -> rawClient.getAlbumSertracking(id));
    }

    /**
     * 设置本子的连载跟踪状态。
     *
     * @param id 本子 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> setAlbumSertracking(String id) {
        return JmResult.runCatching(() -> {
            rawClient.setAlbumSertracking(id);
            return null;
        });
    }

    /**
     * 获取连载跟踪列表。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的跟踪列表
     */
    public JmResult<JmTrackingPage> getAlbumTrackingList(int page) {
        return JmResult.runCatching(() -> rawClient.getAlbumTrackingList(page));
    }

    /**
     * 获取任务列表。
     *
     * @param type   任务类型
     * @param filter 筛选条件
     * @return JmResult 包装的任务列表
     */
    public JmResult<JmTaskList> getTasks(String type, String filter) {
        return JmResult.runCatching(() -> rawClient.getTasks(type, filter));
    }

    /**
     * 获取指定用户的当日签到状态。
     *
     * @param userId 用户 ID
     * @return JmResult 包装的当日签到状态
     */
    public JmResult<JmDailyCheckInStatus> getDailyCheckInStatus(String userId) {
        return JmResult.runCatching(() -> rawClient.getDailyCheckInStatus(userId));
    }

    /**
     * 获取当前登录用户的当日签到状态。
     *
     * @return JmResult 包装的当日签到状态
     */
    public JmResult<JmDailyCheckInStatus> getDailyCheckInStatus() {
        String uid = rawClient.getLoggedInUserId();
        if (uid == null || uid.isBlank()) {
            return JmResult.failure(new IllegalStateException("Not logged in or user ID is unknown. Please login first."));
        }
        return getDailyCheckInStatus(uid);
    }

    /**
     * 执行每日签到。
     *
     * @param userId  用户 ID
     * @param dailyId 签到活动 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> doDailyCheckin(String userId, String dailyId) {
        return JmResult.runCatching(() -> {
            rawClient.doDailyCheckin(userId, dailyId);
            return null;
        });
    }

    /**
     * 获取签到选项列表。
     *
     * @param userId 用户 ID
     * @return JmResult 包装的签到选项列表
     */
    public JmResult<List> getDailyCheckInOptions(String userId) {
        return JmResult.runCatching(() -> rawClient.getDailyCheckInOptions(userId));
    }

    /**
     * 筛选签到列表。
     *
     * @param filter 筛选条件
     * @return JmResult 包装的筛选列表
     */
    public JmResult<List> filterDailyCheckInList(String filter) {
        return JmResult.runCatching(() -> rawClient.filterDailyCheckInList(filter));
    }

    // =========================================================================
    // PicACG (picapi) 对齐别名与语义化适配方法
    // (PicACG Semantic Parity Methods for Seamless Multi-Source Adaptations)
    // =========================================================================

    /**
     * 获取漫画详情（等价于 {@link #getAlbum(String)}）。
     * 对齐 picapi 的 `getComicDetail(comicId)`。
     *
     * @param comicId 漫画/本子 ID
     * @return JmResult 包装的漫画详情对象
     */
    public JmResult<JmAlbum> getComicDetail(String comicId) {
        return getAlbum(comicId);
    }

    /**
     * 获取漫画章节列表。
     * 对齐 picapi 的 `getComicEpisodes(comicId)`。
     *
     * @param comicId 漫画/本子 ID
     * @return JmResult 包装的章节列表
     */
    public JmResult<List<JmPhotoMeta>> getComicEpisodes(String comicId) {
        return getAlbum(comicId).map(JmAlbum::photoMetas);
    }

    /**
     * 获取章节详情（等价于 {@link #getPhoto(String)}）。
     *
     * @param episodeId 章节 ID
     * @return JmResult 包装的章节详情
     */
    public JmResult<JmPhoto> getComicEpisode(String episodeId) {
        return getPhoto(episodeId);
    }

    /**
     * 获取漫画某一章节的全部单页图片列表。
     * 对齐 picapi 的 `getComicPages(comicId, episode)`。
     *
     * @param episodeId 章节/Photo ID
     * @return JmResult 包装的图片列表
     */
    public JmResult<List<JmImage>> getComicPages(String episodeId) {
        return getPhoto(episodeId).map(JmPhoto::images);
    }

    /**
     * 搜索漫画。
     * 对齐 picapi 的 `searchComics(query)`。
     *
     * @param query 搜索参数
     * @return JmResult 包装的搜索结果
     */
    public JmResult<JmSearchPage> searchComics(SearchQuery query) {
        return search(query);
    }

    /**
     * 搜索漫画（根据关键词）。
     * 对齐 picapi 的 `searchComics(keyword)`。
     *
     * @param keyword 关键词
     * @return JmResult 包装的搜索结果
     */
    public JmResult<JmSearchPage> searchComics(String keyword) {
        return search(keyword);
    }

    /**
     * 搜索漫画（根据关键词与页码）。
     * 对齐 picapi 的 `searchComics(keyword, page)`。
     *
     * @param keyword 关键词
     * @param page    页码
     * @return JmResult 包装的搜索结果
     */
    public JmResult<JmSearchPage> searchComics(String keyword, int page) {
        return search(keyword, page);
    }

    /**
     * 收藏或取消收藏漫画。
     * 对齐 picapi 的 `favouriteComic(comicId)`。
     *
     * @param comicId 漫画/本子 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> favouriteComic(String comicId) {
        return toggleAlbumFavorite(comicId);
    }

    /**
     * 点赞漫画。
     * 对齐 picapi 的 `likeComic(comicId)`。
     *
     * @param comicId 漫画/本子 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> likeComic(String comicId) {
        return toggleAlbumLike(comicId);
    }

    /**
     * 获取收藏夹漫画列表。
     * 对齐 picapi 的 `getFavouriteComics(page)`。
     *
     * @param page 页码（从 1 开始）
     * @return JmResult 包装的收藏夹分页
     */
    public JmResult<JmFavoritePage> getFavouriteComics(int page) {
        return getFavorites(page);
    }

    /**
     * 获取收藏夹漫画列表。
     * 对齐 picapi 的 `getFavouriteComics(query)`。
     *
     * @param query 收藏查询参数
     * @return JmResult 包装的收藏夹分页
     */
    public JmResult<JmFavoritePage> getFavouriteComics(FavoriteQuery query) {
        return getFavorites(query);
    }

    /**
     * 发表漫画评论。
     * 对齐 picapi 的 `postComicComment(comicId, content)`。
     *
     * @param comicId 漫画 ID
     * @param content 评论内容
     * @return JmResult 包装的评论对象
     */
    public JmResult<JmComment> postComicComment(String comicId, String content) {
        return postComment(comicId, content);
    }

    /**
     * 获取漫画评论列表。
     * 对齐 picapi 的 `getComicComments(comicId)`。
     *
     * @param comicId 漫画 ID
     * @return JmResult 包装的评论列表
     */
    public JmResult<JmCommentList> getComicComments(String comicId) {
        return getComments(ForumQuery.ofAlbum(comicId));
    }

    /**
     * 获取漫画评论列表（指定页码）。
     * 对齐 picapi 的 `getComicComments(comicId, page)`。
     *
     * @param comicId 漫画 ID
     * @param page    页码
     * @return JmResult 包装的评论列表
     */
    public JmResult<JmCommentList> getComicComments(String comicId, int page) {
        return getComments(ForumQuery.ofAlbum(comicId, page));
    }

    /**
     * 每日打卡签到（对齐 picapi 的 `punchIn()`）。
     * 自动通过当前用户状态获取活动 ID 并完成签到。
     *
     * @return JmResult 成功或失败
     */
    public JmResult<Void> punchIn() {
        return getDailyCheckInStatus().flatMap(status -> {
            String uid = rawClient.getLoggedInUserId();
            return doDailyCheckin(uid, String.valueOf(status.dailyId()));
        });
    }

    /**
     * 每日打卡签到（指定用户 ID 与活动 ID）。
     *
     * @param userId  用户 ID
     * @param dailyId 活动 ID
     * @return JmResult 成功或失败
     */
    public JmResult<Void> punchIn(String userId, String dailyId) {
        return doDailyCheckin(userId, dailyId);
    }

    @Override
    public void close() {
        rawClient.close();
    }
}
