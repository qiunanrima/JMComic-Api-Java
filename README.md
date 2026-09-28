<p align="center">
  <a href="./README_en.md">English</a>
  <span>&nbsp;</span>
  <strong>中文</strong>
</p>

# Java & Kotlin API For JMComic (禁漫天堂)

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-purple.svg)
![Gradle](https://img.shields.io/badge/Gradle-8.0+-02303A.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
![Version](https://img.shields.io/badge/Version-1.1.11-brightgreen.svg)

**一个现代化、高可用、Java 与 Kotlin 双语友好的 JMComic (禁漫天堂) 数据与下载 API 库。**

---

## 🌟 核心特性

- ⚡ **Java & Kotlin 双语现代设计**：Kotlin 原生 DSL 配置与属性访问，Java 完整静态方法与流式调用支持。
- 🧩 **PicACG (picapi) 语义对齐**：内置与 PicACG 高度一致的别名方法（`getComicDetail`、`getComicEpisodes`、`getComicPages`、`searchComics`、`favouriteComic` 等），多图源漫画客户端适配成本趋近于零。
- 🛡️ **函数式无异常结果模型 (`JmResult<T>`)**：全线 API 返回 `JmResult`，彻底告别繁琐的 `try-catch`，提供 `onSuccess`、`onFailure`、`map`、`flatMap`、`fold`。
- 💾 **灵活的会话持久化 (`JmTokenStore`)**：内置 `MemoryJmTokenStore`（内存）和 `FileJmTokenStore`（文件），支持自定义（如 Android `SharedPreferences` / `DataStore`），启动自动恢复会话，登录自动持久化。
- 🖼️ **图片解密与混淆切片重组**：内置 CDN 地址拼装、混淆算法判定与切片重组方法（`fetchDecodedImageBytes`），无需上层关心切片分块逻辑。
- 🚀 **同步与异步双引擎**：内置同步门面客户端 `JmComicClient` 与异步非阻塞客户端 `JmAsyncClient`（所有方法返回 `CompletableFuture<JmResult<T>>`）。
- ⏱️ **网络时钟偏差补偿**：自动根据服务端响应头修正本地时钟偏移，防止设备时钟不准导致 API 鉴权或签名异常。
- 📦 **企业级下载与任务系统**：内置状态机任务管理（暂停/恢复/取消）、多级路径生成器、自定义线程池注入与实时进度监听。
- 📚 **全业务覆盖**：漫画详情与阅读、高级多维搜索、论坛与多实体评论、收藏夹增删改移、创作者与小说子系统、每日签到与打卡等。

---

## 📦 安装依赖 (Installation)

### Gradle (Kotlin DSL)
```kotlin
// 核心模块（推荐，包含完整实现并传递依赖 jmcomic-api）
implementation("io.github.jukomu:jmcomic-core:1.1.11")

// Android 平台推荐额外引入（包含 Android 特定优化与支持）
implementation("io.github.jukomu:jmcomic-android-support:1.1.11")

// 如果仅需纯数据模型与接口定义（零第三方依赖）
implementation("io.github.jukomu:jmcomic-api:1.1.11")
```

### Gradle (Groovy)
```groovy
implementation 'io.github.jukomu:jmcomic-core:1.1.11'
implementation 'io.github.jukomu:jmcomic-android-support:1.1.11' // 可选
```

### Maven
```xml
<dependency>
    <groupId>io.github.jukomu</groupId>
    <artifactId>jmcomic-core</artifactId>
    <version>1.1.11</version>
</dependency>
```

---

## 🚀 快速上手 (Quick Start)

### Kotlin 使用示例

```kotlin
import io.github.jukomu.jmcomic.core.*
import java.io.File

// 1. 初始化单例（支持 DSL 配置与文件会话持久化）
Jm.init(tokenStore = FileJmTokenStore(File("cache/jm_session.json"))) {
    retryTimes(3)
    proxy("127.0.0.1", 7890) // 可选
}

// 2. 登录（成功后会自动持久化存储会话及 Cookie）
val loginResult = Jm.login("my_username", "my_password")
loginResult.onSuccess { user ->
    println("登录成功: ${user.username}, UID: ${user.uid}")
}.onFailure { error ->
    System.err.println("登录失败: ${error.message}")
}

// 3. 获取漫画详情与章节
val detailResult = Jm.client.getComicDetail("540709")
detailResult.onSuccess { comic ->
    println("漫画标题: ${comic.title()}, 作者: ${comic.authors()}")
    println("封面地址: ${comic.toCoverUrl()}")
}

// 4. 获取某一章节的所有单页图片
val pagesResult = Jm.client.getComicPages("1064001")
pagesResult.onSuccess { pages ->
    pages.forEach { image ->
        println("第 ${image.sortOrder()} 页: ${image.toPageUrl()}, 是否切片混淆: ${image.isScrambled()}")
    }
}

// 5. 下载并自动解密重组混淆切片图片
val firstImage = pagesResult.getOrNull()?.firstOrNull()
if (firstImage != null) {
    val imageBytesResult = Jm.client.fetchDecodedImageBytes(firstImage)
    imageBytesResult.onSuccess { decodedBytes ->
        File("page_001.jpg").writeBytes(decodedBytes)
        println("图片已解密并成功保存!")
    }
}

// 6. 异步非阻塞调用
Jm.async.getComicDetail("540709").thenAccept { res ->
    res.onSuccess { comic -> println("异步获取成功: ${comic.title()}") }
}
```

---

### Java 使用示例

```java
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;
import io.github.jukomu.jmcomic.core.Jm;
import io.github.jukomu.jmcomic.core.FileJmTokenStore;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;

import java.io.File;

public class QuickStartJava {
    public static void main(String[] args) {
        // 1. 初始化全局单例
        JmConfiguration config = JmConfiguration.builder()
                .retryTimes(3)
                .build();
        Jm.init(config, new FileJmTokenStore(new File("cache/jm_session.json")));

        // 2. 登录
        Jm.login("my_username", "my_password")
                .onSuccess(user -> System.out.println("登录成功: " + user.getUsername()))
                .onFailure(Throwable::printStackTrace);

        // 3. 搜索漫画
        JmResult<JmSearchPage> searchResult = Jm.searchComics("纯爱", 1);
        searchResult.onSuccess(page -> {
            for (JmAlbumMeta meta : page.content()) {
                System.out.printf("[%s] %s%n", meta.id(), meta.title());
            }
        });

        // 4. 获取漫画详情与章节
        Jm.getComicDetail("540709").onSuccess(album -> {
            System.out.println("标题: " + album.title());
        });

        // 5. 每日打卡签到
        Jm.punchIn().onSuccess(v -> System.out.println("今日签到成功!"));
    }
}
```

---

## 🔄 与 PicACG (picapi) 调用对齐对比

本项目 API 语义与 [picapi](https://github.com/JUKOMU/picapi) 保持高度统一，在多图源聚合阅读器中几乎可以实现无差别调用：

| 业务场景 | PicACG (`picapi`) | JMComic (`JMComic-Api-Java`) |
| :--- | :--- | :--- |
| **单例初始化** | `Pica.init(PicaConfig(), tokenStore)` | `Jm.init(config, tokenStore)` |
| **Kotlin DSL 初始化** | `Pica.init { ... }` | `Jm.init { ... }` |
| **登录与持久化** | `Pica.login(account, pwd)` | `Jm.login(account, pwd)` |
| **获取详情** | `Pica.client.getComicDetail(id)` | `Jm.client.getComicDetail(id)` |
| **获取章节列表** | `Pica.client.getComicEpisodes(id)` | `Jm.client.getComicEpisodes(id)` |
| **获取章节图片** | `Pica.client.getComicPagesByOrder(id, order)` | `Jm.client.getComicPages(episodeId)` |
| **搜索漫画** | `Pica.client.searchComics(keyword, page)` | `Jm.client.searchComics(keyword, page)` |
| **收藏本子** | `Pica.client.favouriteComic(id)` | `Jm.client.favouriteComic(id)` |
| **点赞本子** | `Pica.client.likeComic(id)` | `Jm.client.likeComic(id)` |
| **每日签到** | `Pica.client.punchIn()` | `Jm.client.punchIn()` |
| **异步非阻塞** | `Pica.async.getComicDetail(id)` | `Jm.async.getComicDetail(id)` |

---

## 💡 核心设计与模块架构

### 1. 结果模型 `JmResult<T>`

`JmResult<T>` 是一个表示操作成功或失败的函数式单子：
```kotlin
val result: JmResult<JmAlbum> = Jm.getComicDetail("540709")

// 1. 函数式消费
result.onSuccess { album -> println(album.title()) }
      .onFailure { error -> println(error.message) }

// 2. 映射转换
val titleResult: JmResult<String> = result.map { it.title() }

// 3. 安全折叠取值
val displayText: String = result.fold(
    onSuccess = { "漫画: ${it.title()}" },
    onFailure = { "加载失败: ${it.message}" }
)

// 4. 取出数据或默认值
val albumOrNull: JmAlbum? = result.getOrNull()
val fallbackAlbum: JmAlbum = result.getOrElse { defaultAlbum }
```

### 2. 会话持久化接口 `JmTokenStore`

内置提供了两种会话存储实现，也可以轻松扩展 Android 或桌面应用：

```kotlin
// 1. 内存存储（默认，应用重启后丢失）
MemoryJmTokenStore

// 2. 文件持久化存储（以 JSON 文件持久化 Cookie 与用户标识）
FileJmTokenStore(File("cache/session.json"))

// 3. Android SharedPreferences 自定义示例
class AndroidJmTokenStore(private val sp: SharedPreferences) : JmTokenStore {
    private val gson = Gson()
    override fun loadSession(): JmSession? {
        val json = sp.getString("jm_session", null) ?: return null
        return gson.fromJson(json, JmSession::class.java)
    }
    override fun saveSession(session: JmSession?) {
        sp.edit().putString("jm_session", if (session != null) gson.toJson(session) else null).apply()
    }
}
```

### 3. 图片反混淆与自动解密

禁漫天堂自 220980 章节起采用了切片混淆算法。使用本库时开发者无需关心复杂的算法细节：

```kotlin
val pages = Jm.getComicPages("1064001").getOrThrow()

for (image in pages) {
    // 检查是否被混淆
    val isScrambled = image.isScrambled() // 或 JmImages.isScrambled(image)
    
    // 一键拉取已自动解密重组的图片二进制数据（适合直接给 Glide / Coil / ImageIO 渲染或保存）
    val decodedBytes = Jm.client.fetchDecodedImageBytes(image).getOrThrow()
    
    // 或者仅获取 CDN 地址
    val pageUrl = image.toPageUrl()
}
```

---

## 🛠️ 进阶功能与子系统

### 1. 配置项一览

```java
JmConfiguration config = JmConfiguration.builder()
        .clientType(ClientType.API)            // 客户端类型: API(推荐) 或 HTML
        .proxy("127.0.0.1", 7890)              // HTTP/SOCKS 代理
        .timeout(Duration.ofSeconds(60))        // 请求超时时间
        .imageTimeout(Duration.ofSeconds(120))  // 图片下载超时时间
        .retryTimes(5)                          // 请求重试次数
        .downloadThreadPoolSize(8)              // 下载并发线程池大小
        .cacheSize(100 * 1024 * 1024)           // 缓存大小 (100MB)
        .domainProbeIntervalMs(600_000)         // 域名后台探活间隔 (ms)
        .build();
```

### 2. 批量并发下载与进度监听

```java
// 链式调用下载整个本子，支持自定义进度回调与保存路径
client.download(album)
        .withPath(Path.of("downloads"))
        .withProgress(p -> System.out.printf("总进度: %d/%d 图片%n", p.completedImages(), p.totalImages()))
        .execute();
```

### 3. 基于状态机的任务系统 (Task System)

支持暂停、恢复、取消、观察者模式与并发任务管理：

```java
BaseDownloadTask task = client.createDownloadTask(album, Path.of("downloads"));
task.addObserver(new TaskObserver() {
    @Override
    public void onStateChanged(BaseDownloadTask t, TaskState state) {
        System.out.println("状态更新: " + state);
    }
    @Override
    public void onProgressUpdate(BaseDownloadTask t, DownloadProgress p) {
        System.out.printf("进度: %d/%d%n", p.completedImages(), p.totalImages());
    }
});

IDownloadManager manager = client.downloadManager();
manager.submit(task);
manager.pause(task.getTaskId());
manager.resume(task.getTaskId());
manager.cancel(task.getTaskId());
```

### 4. 评论系统与互动

```java
// 获取评论列表
JmCommentList comments = client.getComicComments("1173049", 1).getOrThrow();

// 发表评论（需先登录）
client.postComicComment("1173049", "画风很棒！");

// 收藏漫画
client.favouriteComic("1173049");

// 点赞漫画
client.likeComic("1173049");
```

### 5. 小说与创作者子系统

```java
// 小说阅读
JmNovelPage novels = client.getNovelList("mr", 1);
JmNovelDetail detail = client.getNovelDetail("novelId");
JmNovelChapter chapter = client.getNovelChapter("chapterId", "0");

// 创作者
JmCreatorPage authors = client.getCreatorAuthors(1, "");
JmCreatorWorkPage works = client.getCreatorWorks(1, "", "", "");
```

---

## 📊 API Client vs HTML Client

| 维度 | `API Client` (默认推荐) | `HTML Client` |
| :--- | :--- | :--- |
| **数据源** | 禁漫官方移动端 API | 网页端 HTML 渲染解析 |
| **稳定性与速度** | 🟢 极高，响应快，消耗流量小 | 🟡 受前端网页改版影响较大 |
| **Cloudflare 拦截** | 🟢 移动端接口不受人机验证拦截 | 🔴 频繁请求容易触发人机验证 |
| **推荐场景** | 99% 的移动端、桌面端及多图源阅读器 | 获取仅在网页端展示的临时冷门数据 |

---

## 🤝 贡献与反馈 (Contributing)

欢迎任何形式的贡献！如果您在使用中发现 BUG 或有新的功能想法，请随时提交 [Issues](https://github.com/JUKOMU/JMComic-Api-Java/issues) 或 Pull Request。

---

## 📜 许可证 (License)

本项目基于 [MIT License](LICENSE) 开源。
