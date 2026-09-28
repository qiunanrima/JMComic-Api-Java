package io.github.jukomu.jmcomic.core;

import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.api.model.JmImage;
import io.github.jukomu.jmcomic.api.model.JmPhoto;
import io.github.jukomu.jmcomic.api.model.JmSearchPage;
import io.github.jukomu.jmcomic.api.model.SearchQuery;
import io.github.jukomu.jmcomic.core.client.JmComicClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

public class ImageLoadDiagTest {

    private static final String TEST_ALBUM = "422866";

    @Test
    public void diagApiImageLoading() throws Exception {
        JmConfiguration config = JmConfiguration.builder()
                .clientType(ClientType.API)
                .timeout(Duration.ofSeconds(20))
                .imageTimeout(Duration.ofSeconds(30))
                .build();
        try (JmComicClient client = JmComicClient.create(config)) {
            client.awaitInitialized();

            JmAlbum album = client.getComicDetail(TEST_ALBUM).getOrThrow();
            checkUrl("api album cover", album.image());

            JmSearchPage search = client.search(new SearchQuery("閸樼喓顨?, 1)).getOrThrow();
            if (!search.content().isEmpty()) {
                checkUrl("api search cover", search.content().get(0).image());
            }

            JmPhoto photo = client.getPhoto(album.photoMetas().get(0).id()).getOrThrow();
            List<JmImage> images = photo.getImages();
            System.out.println("### api photo images = " + images.size());
            for (JmImage img : images.subList(0, Math.min(2, images.size()))) {
                checkUrl("api photo url", img.getDownloadUrl());
                try {
                    byte[] bytes = client.getRawClient().fetchImageBytes(img);
                    System.out.println("### api photo OK bytes=" + bytes.length);
                } catch (Exception e) {
                    System.out.println("### api photo FAIL " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Test
    public void diagHtmlImageLoading() throws Exception {
        JmConfiguration config = JmConfiguration.builder()
                .clientType(ClientType.HTML)
                .timeout(Duration.ofSeconds(20))
                .imageTimeout(Duration.ofSeconds(30))
                .build();
        try (JmComicClient client = JmComicClient.create(config)) {
            client.awaitInitialized();

            JmAlbum album = client.getComicDetail(TEST_ALBUM).getOrThrow();
            System.out.println("### html album.image = " + album.image());
            checkUrl("html album cover", album.image());

            JmSearchPage search = client.search(new SearchQuery("閸樼喓顨?, 1)).getOrThrow();
            if (!search.content().isEmpty()) {
                checkUrl("html search cover", search.content().get(0).image());
            }

            JmPhoto photo = client.getPhoto(album.photoMetas().get(0).id()).getOrThrow();
            List<JmImage> images = photo.getImages();
            System.out.println("### html photo images = " + images.size());
            for (JmImage img : images.subList(0, Math.min(2, images.size()))) {
                checkUrl("html photo url", img.getDownloadUrl());
                try {
                    byte[] bytes = client.getRawClient().fetchImageBytes(img);
                    System.out.println("### html photo OK bytes=" + bytes.length);
                } catch (Exception e) {
                    System.out.println("### html photo FAIL " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void checkUrl(String label, String url) {
        System.out.println("### " + label + " = " + url);
        if (url == null || url.isBlank()) {
            System.out.println("### " + label + " -> EMPTY");
            return;
        }
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        Request req = new Request.Builder().url(url).head().build();
        try (okhttp3.Response resp = http.newCall(req).execute()) {
            System.out.println("### " + label + " -> HTTP " + resp.code());
        } catch (Exception e) {
            System.out.println("### " + label + " -> EX " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
