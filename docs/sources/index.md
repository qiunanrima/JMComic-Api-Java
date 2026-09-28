# JMComic API for Java & Kotlin

> 一个现代化、高可用、Java 与 Kotlin 双语友好的 JMComic（禁漫天堂）数据与下载 API 库

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/Version-1.1.11-brightgreen.svg)](https://github.com/JUKOMU/JMComic-Api-Java)

---

## 项目简介

本项目封装了一套用于访问禁漫天堂数据的现代化 API 库，采用模块化设计，将公共接口与核心实现分离。

- **Java & Kotlin 双语友好**：提供原生 Kotlin DSL、属性访问及扩展函数，同时为 Java 提供完整的静态方法与流式调用。
- **PicACG (picapi) 语义对齐**：内置与 PicACG 命名与行为完全一致的高级门面（`getComicDetail`、`getComicEpisodes`、`getComicPages`、`searchComics` 等），大幅降低多图源漫画客户端的集成与维护成本。
- **函数式无异常单子 (`JmResult<T>`)**：彻底告别到处 `try-catch`，提供 `onSuccess`、`onFailure`、`map`、`fold` 链式调用。
- **自动会话持久化 (`JmTokenStore`)**：支持内存与文件存储，并可轻松扩展到 Android 的 SharedPreferences / DataStore，自动维护 Cookie 与登录态。
- **自动图片反混淆**：内置分块切片重组反混淆算法（`fetchDecodedImageBytes`），直接输出可供渲染或保存的完整图片。

## 模块架构

```
jmcomic-api             → 公共契约与数据模型（零第三方依赖，可独立集成）
jmcomic-core            → 核心实现（API/HTML 客户端、加解密、反混淆重组、单例管理器、下载引擎）
jmcomic-sample          → Java 与 Kotlin 综合调用示例
jmcomic-android-support → Android 平台适配与优化支持
```

## 功能概览

| 分类 | 核心功能 |
|------|----------|
| **漫画** | 本子详情、章节阅读、多维搜索、分类排行、分类列表 |
| **图片** | CDN 地址拼装、混淆判定、切片反混淆重组、单图/多图下载 |
| **下载** | 并发下载、状态机任务系统（暂停/恢复/取消）、观察者监听、自定义路径与线程池 |
| **会话** | 登录/登出、自动持久化、时钟偏差补偿、个人资料维护 |
| **互动** | 评论列表（漫画/小说/博客）、发表与回复评论、收藏夹增删改移、标签管理 |
| **小说** | 列表浏览、详情信息、章节阅读、小说搜索与评论 |
| **创作者**| 作者列表、创作者作品浏览、作品信息与详情 |
| **发现** | 热门标签、最新上架、随机推荐、每周必看、连载追踪 |
| **签到** | 每日签到打卡、签到历史查询 |

## 快速安装

=== "Gradle (Kotlin DSL)"

    ```kotlin
    implementation("io.github.jukomu:jmcomic-core:1.1.11")
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'io.github.jukomu:jmcomic-core:1.1.11'
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.jukomu</groupId>
        <artifactId>jmcomic-core</artifactId>
        <version>1.1.11</version>
    </dependency>
    ```

## 快速开始

=== "Kotlin"

    ```kotlin
    import io.github.jukomu.jmcomic.core.*

    // 1. 初始化
    Jm.init {
        retryTimes(3)
    }

    // 2. 获取详情与章节
    val detail = Jm.client.getComicDetail("540709").getOrThrow()
    println("漫画标题: ${detail.title()}, 封面: ${detail.toCoverUrl()}")

    // 3. 获取单页图片并下载解密
    val pages = Jm.client.getComicPages("1064001").getOrThrow()
    val decodedBytes = Jm.client.fetchDecodedImageBytes(pages.first()).getOrThrow()
    ```

=== "Java"

    ```java
    import io.github.jukomu.jmcomic.core.Jm;
    import io.github.jukomu.jmcomic.api.model.JmAlbum;

    // 1. 初始化
    Jm.init();

    // 2. 获取漫画详情
    Jm.getComicDetail("540709").onSuccess(album -> {
        System.out.println("漫画标题: " + album.title());
    });
    ```

## 参考与对齐项目

- 爬虫协议与逻辑参考：[JMComic-Crawler-Python](https://github.com/hect0x7/JMComic-Crawler-Python)
- 接口规范与语义对齐：[picapi](https://github.com/JUKOMU/picapi)
