# 客户端体系与接口

JMComic-Api-Java 提供了多层客户端抽象：
- **`Jm`**: 全局静态单例管理器，跨页面或组件共享登录态与客户端。
- **`JmComicClient`**: 推荐的高级门面客户端，所有方法返回 `JmResult<T>`，内置 PicACG 语义对齐别名。
- **`JmAsyncClient`**: 异步非阻塞客户端（通过 `client.async()` 获取），所有方法返回 `CompletableFuture<JmResult<T>>`。
- **`AbstractJmClient` / `JmClient`**: 底层网络与协议客户端，包含直接与移动端 API 或 HTML 网页交互的原始实现。

---

## 1. 全局单例管理器 `Jm`

通过 `Jm` 可以极简地进行初始化、登录、数据拉取：

```kotlin
// 启动初始化（带配置块与会话存储）
Jm.init(tokenStore = FileJmTokenStore(File("cache/jm_session.json"))) {
    retryTimes(3)
}

// 获取门面客户端与异步客户端
val client: JmComicClient = Jm.client
val asyncClient: JmAsyncClient = Jm.async

// 检查状态
val isInit: Boolean = Jm.isInitialized
val isLogged: Boolean = Jm.isLoggedIn
```

---

## 2. JmComicClient 核心方法索引

### 漫画与阅读（对齐 PicACG）

| 方法名 | 返回类型 | 对齐说明与特性 |
| :--- | :--- | :--- |
| `getComicDetail(comicId)` | `JmResult<JmAlbum>` | 对齐 picapi `getComicDetail`（等价于 `getAlbum`） |
| `getComicEpisodes(comicId)` | `JmResult<List<JmPhotoMeta>>` | 对齐 picapi `getComicEpisodes`，直接提取所有章节列表 |
| `getComicEpisode(episodeId)` | `JmResult<JmPhoto>` | 获取某一章节的详细元数据与图片信息 |
| `getComicPages(episodeId)` | `JmResult<List<JmImage>>` | 对齐 picapi `getComicPages`，直接提取该话全部单页图片 |
| `searchComics(keyword)` | `JmResult<JmSearchPage>` | 对齐 picapi `searchComics`，便捷关键词搜索 |
| `searchComics(keyword, page)` | `JmResult<JmSearchPage>` | 便捷关键词带页码搜索 |
| `searchComics(query)` | `JmResult<JmSearchPage>` | 高级多维搜索（支持分类、标签、排序、时间范围） |
| `favouriteComic(comicId)` | `JmResult<Void>` | 对齐 picapi `favouriteComic`，收藏或取消收藏 |
| `likeComic(comicId)` | `JmResult<Void>` | 对齐 picapi `likeComic`，点赞或取消点赞 |
| `getFavouriteComics(page)` | `JmResult<JmFavoritePage>` | 对齐 picapi `getFavouriteComics`，获取收藏夹分页 |
| `punchIn()` | `JmResult<Void>` | 对齐 picapi `punchIn`，自动拉取活动状态并完成每日打卡 |
| `fetchDecodedImageBytes(image)` | `JmResult<byte[]>` | 自动判断切片混淆并完成图片重组解密，直接输出纯净字节数组 |

---

### 原生本子与章节接口

| 方法名 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `getAlbum(albumId)` | `JmResult<JmAlbum>` | 根据本子 ID 获取本子详情 |
| `getComicRead(comicId)` | `JmResult<JmAlbum>` | 获取阅读数据（含图片预加载列表） |
| `getPhoto(photoId)` | `JmResult<JmPhoto>` | 根据章节 ID 获取章节详情 |
| `search(query)` | `JmResult<JmSearchPage>` | 执行多维检索 |
| `fetchImageBytes(image)` | `JmResult<byte[]>` | 获取原始下载二进制字节数组（未反混淆） |
| `getCategories(query)` | `JmResult<JmSearchPage>` | 获取分类排行 |
| `getCategoriesList()` | `JmResult<JmCategoryList>` | 获取分类与标签树 |
| `getAlbumDownloadInfo(albumId)` | `JmResult<JmAlbumDownloadInfo>` | 获取本子下载相关信息 |

---

### 用户、会话与 Cookie

| 方法名 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `login(username, password)` | `JmResult<JmUserInfo>` | 用户登录，生成并维护会话 Cookie |
| `logout()` | `JmResult<Void>` | 用户登出，清理 Cookie 与登录态 |
| `getUserProfile(uid)` | `JmResult<JmUserProfile>` | 获取指定用户的详细资料 |
| `getUserProfile()` | `JmResult<JmUserProfile>` | 获取当前已登录用户的详细资料 |
| `editUserProfile(uid, params)` | `JmResult<JmUserProfile>` | 更新用户昵称等个人信息 |
| `restoreSession(session)` | `void` | 恢复持久化的 `JmSession`（含 Cookie 列表） |
| `extractSession()` | `JmSession` | 提取当前会话快照以便持久化存储 |
| `extractCookies()` | `List<String>` | 提取当前 OkHttpClient 内部的所有 Cookie 字符串 |

---

### 评论与互动

| 方法名 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `getComments(query)` | `JmResult<JmCommentList>` | 查询评论列表（支持漫画/小说/博客） |
| `getComicComments(comicId, page)` | `JmResult<JmCommentList>` | 快捷获取指定漫画的评论分页 |
| `postComicComment(comicId, content)` | `JmResult<JmComment>` | 发表漫画评论 |
| `postComment(entityId, text)` | `JmResult<JmComment>` | 发表实体评论 |
| `replyToComment(entityId, text, parentId)` | `JmResult<JmComment>` | 回复指定评论 |
| `voteComment(commentId, voteType)` | `JmResult<JmVoteResult>` | 对评论投票点赞/点踩 |

---

### 收藏夹管理

| 方法名 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `getFavorites(query)` | `JmResult<JmFavoritePage>` | 获取收藏夹列表（支持按文件夹筛选） |
| `toggleAlbumFavorite(albumId, folderId)` | `JmResult<Void>` | 切换本子收藏状态或移动至指定文件夹 |
| `manageFavoriteFolder(type, id, name, albumId)` | `JmResult<JmFavoriteFolderResult>` | 新建、重命名、删除收藏夹或移动漫画 |
| `getTagsFavorite()` | `JmResult<List<JmTagFavorite>>` | 获取收藏的标签列表 |
| `addFavoriteTags(tags)` | `JmResult<Void>` | 添加收藏标签 |
| `removeFavoriteTags(tags)` | `JmResult<Void>` | 移除收藏标签 |

---

### 发现、连载追踪与历史

| 方法名 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `getLatest(page)` | `JmResult<JmSearchPage>` | 分页获取最新上架本子 |
| `getRandomRecommend()` | `JmResult<List<JmAlbumMeta>>` | 获取随机推荐本子 |
| `getHotTags()` | `JmResult<List<String>>` | 获取热门搜索关键词 |
| `getWeeklyPicksList()` | `JmResult<JmWeeklyPicksList>` | 获取每周必看期数列表 |
| `getWeeklyPicksDetail(categoryId)` | `JmResult<JmWeeklyPicksDetail>` | 获取每周必看单期详情 |
| `getSerialization(page)` | `JmResult<JmSearchPage>` | 分页获取连载中本子 |
| `getAlbumTrackingList(page)` | `JmResult<JmTrackingPage>` | 获取追更列表 |
| `setAlbumSertracking(albumId)` | `JmResult<Void>` | 切换漫画追更状态 |
| `getWatchHistory(page)` | `JmResult<List<JmAlbumMeta>>` | 获取观看历史记录 |
| `deleteWatchHistory(id)` | `JmResult<Void>` | 删除观看历史记录 |

---

## 3. 异步客户端 `JmAsyncClient`

通过 `client.async()` 或 `Jm.async` 访问。所有方法的参数与 `JmComicClient` 保持 100% 对齐，返回值为 `CompletableFuture<JmResult<T>>`：

```kotlin
val future = Jm.async.getComicDetail("540709")
future.thenAccept { res ->
    res.onSuccess { comic ->
        println("异步获取完成: ${comic.title()}")
    }
}
```
