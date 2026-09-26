package io.github.jukomu.jmcomic.core

import io.github.jukomu.jmcomic.api.model.JmAlbum
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmImage

/** 扩展：获取漫画本子封面完整 URL */
fun JmAlbum.toCoverUrl(): String? = JmImages.coverUrl(this)

/** 扩展：获取漫画本子摘要封面完整 URL */
fun JmAlbumMeta.toCoverUrl(): String? = JmImages.coverUrl(this)

/** 扩展：获取单页图片完整下载 URL（带 CDN 鉴权参数） */
fun JmImage.toPageUrl(): String? = JmImages.pageUrl(this)

/** 扩展：判断单页图片是否被切片混淆 */
fun JmImage.isScrambled(): Boolean = JmImages.isScrambled(this)

/** 扩展：解密重组混淆切片图片 */
fun JmImage.decode(rawBytes: ByteArray): ByteArray = JmImages.decodeImage(rawBytes, this)
