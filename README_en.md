<p align="center">
  <a href="./README.md">中文</a>
  <span>&nbsp;</span>
  <strong>English</strong>
</p>

# Java & Kotlin API For JMComic

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-purple.svg)
![Gradle](https://img.shields.io/badge/Gradle-8.0+-02303A.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
![Version](https://img.shields.io/badge/Version-1.1.11-brightgreen.svg)

**A modern, robust, Java and Kotlin friendly API and download library for JMComic.**

---

## 🌟 Highlights & Key Features

- ⚡ **First-Class Java & Kotlin Interop**: Native Kotlin DSL builders and properties, alongside idiomatic Java static getters and fluent APIs.
- 🧩 **Semantic Parity with PicACG (`picapi`)**: Provides aligned aliases (`getComicDetail`, `getComicEpisodes`, `getComicPages`, `searchComics`, `favouriteComic`, `likeComic`, `punchIn`), minimizing multi-source manga integration overhead.
- 🛡️ **Functional Exception-Free Result Model (`JmResult<T>`)**: Complete monad pipeline (`onSuccess`, `onFailure`, `map`, `flatMap`, `fold`), eliminating verbose `try-catch` boilerplate.
- 💾 **Session Persistence Framework (`JmTokenStore`)**: Built-in `MemoryJmTokenStore` and `FileJmTokenStore`, easily extensible to Android `SharedPreferences` / `DataStore`. Auto-restores sessions on startup and saves on login.
- 🖼️ **Image De-obfuscation & Auto Slicing Reassembly**: Built-in CDN URL resolver, scramble detector, and automated image reassembly (`fetchDecodedImageBytes`).
- 🚀 **Dual Sync & Async Engines**: Synchronous facade client `JmComicClient` and non-blocking asynchronous client `JmAsyncClient` (returning `CompletableFuture<JmResult<T>>`).
- ⏱️ **Server Clock Skew Compensation**: Calculates time offsets from HTTP response headers, preventing signature or auth failures caused by out-of-sync local clocks.
- 📦 **Download Task System**: State-machine task lifecycle (pause/resume/cancel), multi-tier path generators, custom executor injection, and progress observers.

---

## 📦 Installation

### Gradle (Kotlin DSL)
```kotlin
// Core module (Recommended, includes complete implementation and transitive jmcomic-api)
implementation("io.github.jukomu:jmcomic-core:1.1.11")

// Android support (Optional, for Android-specific WebP / Bitmap optimizations)
implementation("io.github.jukomu:jmcomic-android-support:1.1.11")

// Pure domain models and interfaces only (zero third-party dependencies)
implementation("io.github.jukomu:jmcomic-api:1.1.11")
```

### Gradle (Groovy)
```groovy
implementation 'io.github.jukomu:jmcomic-core:1.1.11'
implementation 'io.github.jukomu:jmcomic-android-support:1.1.11' // optional
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

## 🚀 Quick Start

### Kotlin Example

```kotlin
import io.github.jukomu.jmcomic.core.*
import java.io.File

// 1. Initialize singleton with Kotlin DSL & persistent session
Jm.init(tokenStore = FileJmTokenStore(File("cache/jm_session.json"))) {
    retryTimes(3)
    proxy("127.0.0.1", 7890) // optional
}

// 2. Login (automatically persists cookies and session)
Jm.login("my_username", "my_password")
    .onSuccess { user -> println("Welcome, ${user.username} (UID: ${user.uid})") }
    .onFailure { err -> System.err.println("Login error: ${err.message}") }

// 3. Fetch comic details
val detailResult = Jm.client.getComicDetail("540709")
detailResult.onSuccess { comic ->
    println("Title: ${comic.title()}, Author: ${comic.authors()}")
    println("Cover URL: ${comic.toCoverUrl()}")
}

// 4. Fetch pages for an episode
val pagesResult = Jm.client.getComicPages("1064001")
pagesResult.onSuccess { pages ->
    pages.forEach { img ->
        println("Page ${img.sortOrder()}: ${img.toPageUrl()}, scrambled: ${img.isScrambled()}")
    }
}

// 5. Download and automatically reassemble scrambled sliced images
val firstImage = pagesResult.getOrNull()?.firstOrNull()
if (firstImage != null) {
    Jm.client.fetchDecodedImageBytes(firstImage).onSuccess { bytes ->
        File("page_001.jpg").writeBytes(bytes)
        println("Image decoded and saved successfully!")
    }
}

// 6. Asynchronous non-blocking call
Jm.async.getComicDetail("540709").thenAccept { res ->
    res.onSuccess { comic -> println("Async comic title: ${comic.title()}") }
}
```

---

### Java Example

```java
import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;
import io.github.jukomu.jmcomic.core.Jm;
import io.github.jukomu.jmcomic.core.FileJmTokenStore;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;

import java.io.File;

public class QuickStartJava {
    public static void main(String[] args) {
        // 1. Initialize global singleton
        JmConfiguration config = JmConfiguration.builder()
                .retryTimes(3)
                .build();
        Jm.init(config, new FileJmTokenStore(new File("cache/jm_session.json")));

        // 2. Login
        Jm.login("my_username", "my_password")
                .onSuccess(user -> System.out.println("Login success: " + user.getUsername()))
                .onFailure(Throwable::printStackTrace);

        // 3. Search comics
        JmResult<JmSearchPage> searchResult = Jm.searchComics("Romance", 1);
        searchResult.onSuccess(page -> {
            for (JmAlbumMeta meta : page.content()) {
                System.out.printf("[%s] %s%n", meta.id(), meta.title());
            }
        });

        // 4. Fetch comic detail
        Jm.getComicDetail("540709").onSuccess(album -> {
            System.out.println("Title: " + album.title());
        });

        // 5. Daily check-in
        Jm.punchIn().onSuccess(v -> System.out.println("Punch-in succeeded!"));
    }
}
```

---

## 🔄 Semantic Parity with PicACG (`picapi`)

The API signatures closely align with [picapi](https://github.com/JUKOMU/picapi), enabling effortless drop-in multi-source client integration:

| Action | PicACG (`picapi`) | JMComic (`JMComic-Api-Java`) |
| :--- | :--- | :--- |
| **Singleton Init** | `Pica.init(PicaConfig(), tokenStore)` | `Jm.init(config, tokenStore)` |
| **Kotlin DSL Init** | `Pica.init { ... }` | `Jm.init { ... }` |
| **Login & Persist** | `Pica.login(user, pwd)` | `Jm.login(user, pwd)` |
| **Comic Detail** | `Pica.client.getComicDetail(id)` | `Jm.client.getComicDetail(id)` |
| **Episode List** | `Pica.client.getComicEpisodes(id)` | `Jm.client.getComicEpisodes(id)` |
| **Episode Pages** | `Pica.client.getComicPagesByOrder(id, order)` | `Jm.client.getComicPages(episodeId)` |
| **Search** | `Pica.client.searchComics(keyword, page)` | `Jm.client.searchComics(keyword, page)` |
| **Favorite** | `Pica.client.favouriteComic(id)` | `Jm.client.favouriteComic(id)` |
| **Like** | `Pica.client.likeComic(id)` | `Jm.client.likeComic(id)` |
| **Daily Check-in** | `Pica.client.punchIn()` | `Jm.client.punchIn()` |
| **Async Call** | `Pica.async.getComicDetail(id)` | `Jm.async.getComicDetail(id)` |

---

## 💡 Architecture & Key Concepts

### 1. Result Model `JmResult<T>`

`JmResult<T>` is a monadic container representing either a successful result or a failure without throwing exceptions:
```kotlin
val result: JmResult<JmAlbum> = Jm.getComicDetail("540709")

// Functional consumption
result.onSuccess { album -> println(album.title()) }
      .onFailure { error -> println(error.message) }

// Functional mapping
val title: JmResult<String> = result.map { it.title() }

// Safe fold
val text: String = result.fold(
    onSuccess = { "Comic: ${it.title()}" },
    onFailure = { "Error: ${it.message}" }
)
```

### 2. Session Persistence with `JmTokenStore`

```kotlin
// Memory-only (default, lost on process termination)
MemoryJmTokenStore

// File-based persistence
FileJmTokenStore(File("cache/session.json"))

// Android SharedPreferences / DataStore implementation
class AndroidJmTokenStore(private val sp: SharedPreferences) : JmTokenStore {
    private val gson = Gson()
    override fun loadSession(): JmSession? {
        val json = sp.getString("jm_session", null) ?: return null
        return gson.fromJson(json, JmSession::class.java)
    }
    override fun saveSession(session: JmSession?) {
        sp.edit().putString("jm_session", session?.let { gson.toJson(it) }).apply()
    }
}
```

### 3. De-obfuscation and Image Slicing Reassembly

Comics from photo ID `220980` upwards utilize slicing obfuscation:

```kotlin
val pages = Jm.getComicPages("1064001").getOrThrow()

for (image in pages) {
    // Check if scrambled
    val scrambled = image.isScrambled()

    // Fetch and auto-reassemble decoded bytes ready for Glide / Coil / ImageIO
    val decodedBytes = Jm.client.fetchDecodedImageBytes(image).getOrThrow()

    // Or get direct CDN URL
    val pageUrl = image.toPageUrl()
}
```

---

## 🛠️ Advanced Features

### Batch Downloads with Progress Observers

```java
client.download(album)
        .withPath(Path.of("downloads"))
        .withProgress(p -> System.out.printf("Progress: %d/%d images%n", p.completedImages(), p.totalImages()))
        .execute();
```

### State-Machine Task System

```java
BaseDownloadTask task = client.createDownloadTask(album, Path.of("downloads"));
task.addObserver(new TaskObserver() {
    @Override
    public void onStateChanged(BaseDownloadTask t, TaskState state) {
        System.out.println("State: " + state);
    }
    @Override
    public void onProgressUpdate(BaseDownloadTask t, DownloadProgress p) {
        System.out.printf("%d/%d%n", p.completedImages(), p.totalImages());
    }
});

IDownloadManager manager = client.downloadManager();
manager.submit(task);
manager.pause(task.getTaskId());
manager.resume(task.getTaskId());
manager.cancel(task.getTaskId());
```

---

## 📊 API Client vs HTML Client

| Dimension | `API Client` (Default & Recommended) | `HTML Client` |
| :--- | :--- | :--- |
| **Data Source** | Official Mobile App REST API | Web HTML Scraping |
| **Speed & Bandwidth** | 🟢 Very Fast, minimal JSON payloads | 🟡 Heavier HTML parsing |
| **Cloudflare** | 🟢 Not affected by Cloudflare bot challenge | 🔴 Prone to CAPTCHA/CF challenge |
| **Use Case** | 99% of Android/Desktop/Backend clients | Fallback for web-only pages |

---

## 🤝 Contributing

Contributions, bug reports, and pull requests are warmly welcome! Please visit the [GitHub Issues](https://github.com/JUKOMU/JMComic-Api-Java/issues) page.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).
