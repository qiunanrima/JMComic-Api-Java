package io.github.jukomu.jmcomic.core;

import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta;
import io.github.jukomu.jmcomic.api.model.JmImage;
import io.github.jukomu.jmcomic.core.constant.JmConstants;
import io.github.jukomu.jmcomic.core.crypto.JmImageTool;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JMComic 图片 URL 构建与解密辅助工具类，设计与 picapi 的 PicaImages 保持一致。
 * 提供静态方法以便 Java 与 Kotlin 开发者便捷地获取封面、分页以及解密重组图片。
 */
public final class JmImages {

    private JmImages() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取漫画本子详情的封面完整 URL。
     *
     * @param album 本子详情对象
     * @return 封面图片 URL，如果为 null 则返回 null
     */
    public static String coverUrl(JmAlbum album) {
        return album != null ? album.image() : null;
    }

    /**
     * 获取漫画本子摘要（列表中）的封面完整 URL。
     *
     * @param meta 本子摘要对象
     * @return 封面图片 URL，如果为 null 则返回 null
     */
    public static String coverUrl(JmAlbumMeta meta) {
        return meta != null ? meta.image() : null;
    }

    /**
     * 缩略图完整 URL 别名方法，与封面一致。
     *
     * @param album 本子详情对象
     * @return 缩略图 URL
     */
    public static String thumbnailUrl(JmAlbum album) {
        return coverUrl(album);
    }

    /**
     * 缩略图完整 URL 别名方法，与封面一致。
     *
     * @param meta 本子摘要对象
     * @return 缩略图 URL
     */
    public static String thumbnailUrl(JmAlbumMeta meta) {
        return coverUrl(meta);
    }

    /**
     * 获取漫画单页图片的完整下载地址（包含 CDN 鉴权查询参数）。
     *
     * @param image 图片元数据对象
     * @return 完整的图片下载 URL
     */
    public static String pageUrl(JmImage image) {
        return image != null ? image.getDownloadUrl() : null;
    }

    /**
     * 获取漫画单页图片的基础 URL（不包含查询参数）。
     *
     * @param image 图片元数据对象
     * @return 基础 URL
     */
    public static String imageUrl(JmImage image) {
        return image != null ? image.url() : null;
    }

    /**
     * 判断图片是否需要解密（混淆切片重组）。
     *
     * @param image 图片元数据对象
     * @return true 表示被混淆需重组，false 表示为普通图片
     */
    public static boolean isScrambled(JmImage image) {
        if (image == null) return false;
        try {
            long scrambleId = Long.parseLong(image.scrambleId());
            long photoId = Long.parseLong(image.photoId());
            return JmImageTool.calculateNumSegments(scrambleId, photoId, image.getFilenameWithoutSuffix()) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 解密重组切片混淆的图片二进制字节数组。
     * 如果该图片无需解密，将直接返回原始字节数组。
     *
     * @param rawBytes 原始下载的图片二进制数据
     * @param image    图片元数据对象
     * @return 解密重组后的图片二进制数据
     */
    public static byte[] decodeImage(byte[] rawBytes, JmImage image) {
        if (rawBytes == null || image == null) {
            return rawBytes;
        }
        return JmImageTool.decryptImage(rawBytes, image);
    }

    /**
     * 构建图片下载的候选域名列表（保序去重，最多 {@code limit} 个）。
     * 顺序为：当前 URL 的域名 -> 动态 img_host -> 默认图片域名列表。
     * 用于图片下载失败时跨 CDN 域名重试。
     *
     * @param imageUrl 当前图片 URL
     * @param limit    最多返回的候选域名数量
     * @return 候选域名列表
     */
    public static List<String> candidateDomains(String imageUrl, int limit) {
        Set<String> domains = new LinkedHashSet<>();
        HttpUrl parsed = HttpUrl.parse(imageUrl);
        if (parsed != null) {
            domains.add(parsed.host());
        }
        if (StringUtils.isNotBlank(JmConstants.CURRENT_IMAGE_HOST)) {
            domains.add(JmConstants.CURRENT_IMAGE_HOST);
        }
        domains.addAll(JmConstants.DEFAULT_IMAGE_DOMAINS);

        List<String> result = new ArrayList<>(domains);
        return result.size() <= limit ? result : result.subList(0, limit);
    }

    /**
     * 将图片 URL 的域名替换为指定域名，保留路径与查询参数。
     *
     * @param imageUrl 原图片 URL
     * @param domain   新域名（不带协议）
     * @return 替换域名后的 URL，解析失败时返回原 URL
     */
    public static String replaceDomain(String imageUrl, String domain) {
        HttpUrl parsed = HttpUrl.parse(imageUrl);
        if (parsed == null || parsed.host().equals(domain)) {
            return imageUrl;
        }
        HttpUrl newUrl = parsed.newBuilder().host(domain).build();
        return newUrl.toString();
    }
}
