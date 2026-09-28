# 获取数据示例

在全新设计的 API 体系中，数据获取既可以通过全局单例 `Jm`，也可以通过实例化的 `JmComicClient` 进行。所有接口统一返回函数式容器 `JmResult<T>`，消除了多层嵌套的 `try-catch`。

---

## 1. 获取漫画详情

=== "Kotlin"

    ```kotlin
    import io.github.jukomu.jmcomic.core.*

    // 使用全局单例直接获取
    val result = Jm.getComicDetail("540709")

    result.onSuccess { album ->
        println("标题: ${album.title()}")
        println("作者: ${album.authors()}")
        println("章节总数: ${album.photoMetas().size}")
        println("封面地址: ${album.toCoverUrl()}")
    }.onFailure { error ->
        System.err.println("获取失败: ${error.message}")
    }
    ```

=== "Java"

    ```java
    import io.github.jukomu.jmcomic.core.Jm;
    import io.github.jukomu.jmcomic.api.model.JmAlbum;
    import io.github.jukomu.jmcomic.api.result.JmResult;

    JmResult<JmAlbum> result = Jm.getComicDetail("540709");
    result.onSuccess(album -> {
        System.out.println("标题: " + album.title());
        System.out.println("作者: " + album.authors());
    }).onFailure(Throwable::printStackTrace);
    ```

---

## 2. 获取章节与单页图片列表

=== "Kotlin"

    ```kotlin
    // 1. 获取本子的所有章节列表
    val episodes = Jm.getComicEpisodes("540709").getOrThrow()
    val firstEpisode = episodes.first()
    println("第一话: ${firstEpisode.title()} (ID: ${firstEpisode.id()})")

    // 2. 获取第一话的全部图片
    val pages = Jm.getComicPages(firstEpisode.id()).getOrThrow()
    pages.forEach { image ->
        println("第 ${image.sortOrder()} 页: ${image.toPageUrl()}, 是否切片混淆: ${image.isScrambled()}")
    }
    ```

=== "Java"

    ```java
    import io.github.jukomu.jmcomic.core.Jm;
    import io.github.jukomu.jmcomic.api.model.*;
    import java.util.List;

    List<JmPhotoMeta> episodes = Jm.getComicEpisodes("540709").getOrThrow();
    List<JmImage> pages = Jm.getComicPages(episodes.get(0).id()).getOrThrow();

    for (JmImage img : pages) {
        System.out.printf("P%d: %s%n", img.sortOrder(), img.getDownloadUrl());
    }
    ```

---

## 3. 图片下载与自动解密切片

对于编号在 `220980` 之后的漫画，禁漫天堂对图片进行了切片乱序。使用 `fetchDecodedImageBytes` 可以自动完成网络下载与解密重组：

=== "Kotlin"

    ```kotlin
    val firstPage = pages.first()

    // 自动判断是否切片并完成反混淆，返回纯净的图片二进制数据
    Jm.client.fetchDecodedImageBytes(firstPage).onSuccess { imageBytes ->
        File("page_01.jpg").writeBytes(imageBytes)
        println("成功写入已还原的原始图片！")
    }
    ```

=== "Java"

    ```java
    byte[] decoded = Jm.client().fetchDecodedImageBytes(pages.get(0)).getOrThrow();
    Files.write(Path.of("page_01.jpg"), decoded);
    ```

---

## 4. 搜索漫画

=== "Kotlin"

    ```kotlin
    // 便捷搜索（带关键词与页码）
    Jm.searchComics("纯爱", page = 1).onSuccess { page ->
        println("总页数: ${page.pageCount()}, 总结果数: ${page.total()}")
        page.content().forEach { item ->
            println("[${item.id()}] ${item.title()} (作者: ${item.authors()})")
        }
    }
    ```

=== "Java"

    ```java
    Jm.searchComics("纯爱", 1).onSuccess(page -> {
        for (JmAlbumMeta meta : page.content()) {
            System.out.printf("[%s] %s%n", meta.id(), meta.title());
        }
    });
    ```

---

## 5. 发现、推荐与分类排行

```kotlin
// 最新上架
val latest = Jm.client.getLatest(page = 1).getOrNull()

// 随机推荐
val randomList = Jm.client.getRandomRecommend().getOrNull()

// 热门搜索标签
val hotTags = Jm.client.getHotTags().getOrNull()

// 每周必看
val weeklyPicks = Jm.client.getWeeklyPicksList().getOrNull()
```

---

## 6. 异步非阻塞调用

所有接口均可通过 `Jm.async` 或 `client.async()` 以非阻塞异步方式执行：

```kotlin
Jm.async.getComicDetail("540709").thenAccept { result ->
    result.onSuccess { album ->
        // 在异步回调中处理结果，不阻塞主线程
        updateUi(album)
    }
}
```
