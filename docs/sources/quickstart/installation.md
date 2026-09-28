# 安装与依赖配置

## 环境要求

- **Java 17** 或更高版本
- **Kotlin 1.9+ / 2.0+**（如果使用 Kotlin 开发）
- **Gradle 8.0+** 或 **Maven 3.8+** 构建工具

---

## 引入依赖

### 1. 标准安装（推荐）

大多数情况下，直接引入 `jmcomic-core` 即可，它会自动引入 `jmcomic-api` 以及核心网络与解密依赖：

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("io.github.jukomu:jmcomic-core:1.1.11")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'io.github.jukomu:jmcomic-core:1.1.11'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.jukomu</groupId>
        <artifactId>jmcomic-core</artifactId>
        <version>1.1.11</version>
    </dependency>
    ```

---

### 2. 仅使用接口与实体模型

如果你在开发自定义底层客户端或多模块项目，只需要纯数据模型与接口定义（零第三方依赖），可以只引入 `jmcomic-api`：

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("io.github.jukomu:jmcomic-api:1.1.11")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'io.github.jukomu:jmcomic-api:1.1.11'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.jukomu</groupId>
        <artifactId>jmcomic-api</artifactId>
        <version>1.1.11</version>
    </dependency>
    ```

---

### 3. Android 平台专属支持

在 Android 项目中，推荐额外引入 `jmcomic-android-support`，它针对 Android 的位图解码与生命周期提供了专门优化：

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("io.github.jukomu:jmcomic-core:1.1.11")
        implementation("io.github.jukomu:jmcomic-android-support:1.1.11")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'io.github.jukomu:jmcomic-core:1.1.11'
        implementation 'io.github.jukomu:jmcomic-android-support:1.1.11'
    }
    ```

---

## 传递依赖一览

`jmcomic-core` 已与 `picapi` 保持完全一致的核心依赖版本：

| 依赖组件 | 统一版本 | 用途 |
| :--- | :--- | :--- |
| **OkHttp** | `5.3.2` | 高性能 HTTP/2 网络传输 |
| **OkHttp URLConnection** | `4.12.0` | Cookie 存储与会话兼容 |
| **Gson** | `2.10.1` | JSON 数据序列化与反序列化 |
| **Jsoup** | `1.17.2` | HTML 解析（HTML 客户端） |
| **webp-imageio** | `0.3.3` | WebP 格式图片切片重组与解码 |
| **Commons Lang3** | `3.16.0` | 高性能字符串与反射辅助 |
| **SLF4J API** | `2.0.12` | 统一日志门面（可自由绑定 Logback / Android Log） |

---

## 下一步

- [快速数据获取与单例使用](fetch-data.md)
- [下载第一个本子](first-download.md)
- [客户端完整配置指南](../configuration.md)
