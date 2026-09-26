package io.github.jukomu.jmcomic.sample;

import io.github.jukomu.jmcomic.api.model.*;
import io.github.jukomu.jmcomic.api.result.JmResult;
import io.github.jukomu.jmcomic.core.Jm;
import io.github.jukomu.jmcomic.core.JmImages;
import io.github.jukomu.jmcomic.core.client.JmComicClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 演示与 PicACG (picapi) 高度对齐的统一 API 调用风格。
 * <p>
 * 无论是 Java 还是 Kotlin 调用，均具备以下一致的体验：
 * 1. 结果安全包裹于 JmResult（支持 onSuccess、onFailure、map、fold、getOrNull、getOrThrow）。
 * 2. 具备全局单例 Jm（对齐 picapi 的 Pica）。
 * 3. 具备语义对齐的方法（getComicDetail、getComicEpisodes、getComicPages、searchComics、favouriteComic 等）。
 * 4. 具备非阻塞异步客户端 client.async()。
 * 5. 具备便捷图片链接与混淆解密工具 JmImages（对齐 picapi 的 PicaImages）。
 */
public class UnifiedApiUsageSample {

    public static void main(String[] args) {
        // -------------------------------------------------------------
        // 方式 1: 使用 Jm 全局单例直接调用（极简，与 Pica 风格完全一致）
        // -------------------------------------------------------------
        System.out.println("=== 方式 1: Jm 全局单例调用 ===");

        // 启动时初始化
        Jm.init(JmConfiguration.builder()
                .retryTimes(3)
                .build());

        // 检查初始化状态
        System.out.println("Jm isInitialized: " + Jm.isInitialized());

        // 获取漫画/本子详情
        JmResult<JmAlbum> comicResult = Jm.getComicDetail("540709");
        comicResult.onSuccess(album -> {
            System.out.println("获取漫画成功: " + album.title());
            System.out.println("封面图片 URL: " + JmImages.coverUrl(album));

            // 获取章节列表
            JmResult<List<JmPhotoMeta>> episodesResult = Jm.getComicEpisodes(album.id());
            episodesResult.onSuccess(episodes -> {
                System.out.println("章节数量: " + episodes.size());
                if (!episodes.isEmpty()) {
                    String firstEpisodeId = episodes.get(0).id();

                    // 获取某一章节的分页图片
                    JmResult<List<JmImage>> pagesResult = Jm.getComicPages(firstEpisodeId);
                    pagesResult.onSuccess(pages -> {
                        System.out.println("单页数量: " + pages.size());
                        if (!pages.isEmpty()) {
                            JmImage firstPage = pages.get(0);
                            System.out.println("第1页下载地址: " + JmImages.pageUrl(firstPage));
                            System.out.println("是否被混淆切片: " + JmImages.isScrambled(firstPage));
                        }
                    });
                }
            });
        }).onFailure(err -> {
            System.err.println("调用失败: " + err.getMessage());
        });

        // -------------------------------------------------------------
        // 方式 2: 使用 JmComicClient 实例调用（适合依赖注入或多账号管理）
        // -------------------------------------------------------------
        System.out.println("\n=== 方式 2: JmComicClient 实例调用 ===");

        JmComicClient client = JmComicClient.create();

        // 搜索漫画（关键词或分页）
        JmResult<JmSearchPage> searchResult = client.searchComics("科幻", 1);
        searchResult.onSuccess(page -> {
            System.out.println("搜索结果本子数: " + page.content().size());
            for (JmAlbumMeta meta : page.content()) {
                System.out.println(" - [" + meta.id() + "] " + meta.title() + " 封面: " + JmImages.coverUrl(meta));
            }
        });

        // -------------------------------------------------------------
        // 方式 3: 异步非阻塞调用（对齐 PicaAsyncClient）
        // -------------------------------------------------------------
        System.out.println("\n=== 方式 3: 异步非阻塞调用 (CompletableFuture) ===");

        CompletableFuture<JmResult<JmAlbum>> future = client.async().getComicDetail("540709");
        future.thenAccept(res -> {
            res.onSuccess(album -> System.out.println("异步获取成功: " + album.title()))
               .onFailure(err -> System.err.println("异步获取失败: " + err.getMessage()));
        });

        // -------------------------------------------------------------
        // 方式 4: 函数式转换与安全提取 (Result Pattern)
        // -------------------------------------------------------------
        System.out.println("\n=== 方式 4: 函数式转换 (map, fold) ===");

        String titleOrFallback = client.getComicDetail("540709")
                .map(JmAlbum::title)
                .getOrElse("未知漫画");
        System.out.println("获取漫画标题: " + titleOrFallback);
    }
}
