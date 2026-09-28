# 版本说明

## v1.2.1

### 修复：HTML 客户端配置导致实例创建失败（根因）
- **`JmComicClient.create()/createAsync()` 现在按 `clientType` 自动分发**：此前无论配置如何都走 API 工厂，传入 `ClientType.HTML` 会直接抛出 `IllegalArgumentException: Cannot create ApiClient with HTML client type`，调用方（如 mihon 图源）表现为"初始化实例失败"。HTML 模式（含 `Jm.init(htmlConfig)`）现已正常工作。

### 修复：HTML 登录不校验服务端结果
- `JmHtmlClient.login()` 此前收到 HTTP 200 即视为登录成功，错误密码也返回成功（服务端实际返回 `{"status":2,"errors":["無效的用戶名和/或密碼！"]}`）。现已解析登录响应：`status != 1` 或返回 HTML 页面时抛出 `ResponseException` 并携带服务端错误信息。

### 修复：客户端初始化失败后实例永久不可用
- **初始化自动重试**：客户端初始化（域名更新、探活、`initialize()`）失败时按配置自动重试（新增 `initRetryTimes` 默认 2 次、`initRetryBackoffMs` 默认 1000ms 线性退避），瞬时网络故障（如 Android 冷启动时节点探测超时）不再导致实例报废。
- **失败后可重新初始化**：初始化最终失败后再次调用 `initializeAsync()` 会自动重建内部线程池、OkHttpClient 与下载管理器并重新初始化（会话 Cookie 与域名状态保留）。
- **请求路径自愈**：初始化最终失败后，阻塞等待初始化的请求会自动触发一次重新初始化，网络恢复后无需重建客户端实例即可恢复。
- **初始化状态机 Kotlin 重写**：新增 `JmClientInitializer`（状态 + Future + 可重启语义），替代原先不可逆的 `INITIALIZING/FAILED` 状态机。
- **新增可观察初始化 API**：`AbstractJmClient.initializationFuture()` / `awaitInitialized()`，`JmComicClient.initializationFuture()` / `awaitInitialized()`，`Jm.initAsync(config, tokenStore)`（Future 在完整初始化后完成）。

---

## v1.2.0

### 构建工程全面升级
- **构建系统重构**：从 Maven 全面迁移为 **Gradle** 多模块架构，规范模块管理与跨平台发布。
- **Kotlin 原生支持**：引入 `kotlin-jvm 2.2.21`，实现 Java 17 与 Kotlin 的无缝混编。
- **核心依赖对齐**：与主流漫画 API 库（如 `picapi`）对齐基础网络与序列化组件版本（OkHttp `5.3.2`、Gson `2.10.1`、JUnit `5.10.2` 等）。

### 现代化 API 与 PicACG 语义对齐
- **PicACG 语义对齐别名**：在 `JmComicClient` 中提供与 `picapi` 保持一致的别名方法：
  - `getComicDetail(comicId)`
  - `getComicEpisodes(comicId)`
  - `getComicEpisode(episodeId)`
  - `getComicPages(episodeId)`
  - `searchComics(keyword, page)`
  - `favouriteComic(comicId)`
  - `likeComic(comicId)`
  - `getFavouriteComics(page)`
  - `punchIn()`
- **函数式无异常结果模型 (`JmResult<T>`)**：全线 API 返回 `JmResult`，内置 `onSuccess`、`onFailure`、`map`、`flatMap`、`fold`、`getOrNull`、`getOrElse`，消除业务代码中的 `try-catch`。
- **全局单例管理器 (`Jm.kt`)**：支持 Kotlin DSL 配置初始化 (`Jm.init { ... }`) 与 Java 静态便捷方法，内置 `Jm.client`、`Jm.async` 属性访问。
- **异步非阻塞客户端 (`JmAsyncClient`)**：所有方法返回 `CompletableFuture<JmResult<T>>`。

### 会话持久化与时钟补偿
- **会话持久化框架 (`JmTokenStore` & `JmSession`)**：
  - 内置 `MemoryJmTokenStore`（内存存储）与 `FileJmTokenStore`（文件存储）。
  - 支持扩展至 Android `SharedPreferences` / Jetpack DataStore。
  - 启动时自动恢复 Cookie 与登录态，登录时自动持久化保存。
- **服务端时钟偏差补偿**：自动分析 HTTP 响应头中的 `Date` 计算本地设备与服务端的时间差，解决客户端时间错误导致的验签异常。

### 图片处理与 Kotlin 扩展
- **图片自动重组反混淆**：新增 `client.fetchDecodedImageBytes(image)`，自动完成网络下载并执行分块切片重组，直接输出可渲染的完整二进制。
- **Kotlin 扩展属性与方法**：`album.toCoverUrl()`、`image.toPageUrl()`、`image.isScrambled()`、`image.decode(bytes)`。

---

## v1.1.0

### 新增下载任务系统
- 全新的任务管理框架，支持暂停/恢复/取消、状态机、观察者模式、任务管理器
  - `BaseDownloadTask` — 下载任务基类，支持父子任务树、状态迁移、进度聚合
  - `IDownloadManager` — 任务管理器接口（submit / pause / resume / cancel / 查询）
  - `TaskObserver` — 观察者接口（onStateChanged / onProgressUpdate / onFinished / onError）
  - `TaskState` — 10 个任务状态（PENDING / QUEUED / RUNNING / PAUSED / CANCELLING / CANCELLED / COMPLETED / COMPLETED_WITH_ERRORS / FAILED / SKIPPED）
  - `TaskType` — 任务类型（ALBUM / PHOTO / IMAGE）
  - `JmDownloadClient` 新增 `createDownloadTask` 和 `downloadManager` 方法

---

## v1.0.0

首个正式版本。

### 功能
- **漫画**: 本子详情、章节阅读、搜索、分类排行、分类列表
- **下载**: 并发下载、链式 API（withPath / withProgress / withExecutor）、三层粒度路径策略
- **评论**: 支持漫画/小说/博客/用户等多实体评论，发表/回复
- **收藏**: 收藏夹管理、标签管理、文件夹增删改移
- **用户**: 登录/登出、个人资料查看与编辑
- **签到**: 每日签到、签到历史
- **小说**: 列表、详情、章节、搜索、评论/收藏
- **创作者**: 作者列表、作品浏览、作品详情
- **发现**: 热门标签、最新上架、随机推荐、每周必看
- **通知**: 通知列表、已读标记
- **追踪**: 连载追踪列表、追踪状态管理
- **双客户端**: API 客户端（推荐）+ HTML 客户端
- **模块化**: api 模块零依赖，core 模块含完整实现
- **Android 支持**: jmcomic-android-support 模块
