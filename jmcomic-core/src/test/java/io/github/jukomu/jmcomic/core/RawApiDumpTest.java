package io.github.jukomu.jmcomic.core;

import io.github.jukomu.jmcomic.core.client.impl.JmApiClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import io.github.jukomu.jmcomic.core.constant.JmConstants;
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder;
import io.github.jukomu.jmcomic.core.net.model.JmApiResponse;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;

public class RawApiDumpTest {

    @Test
    public void dumpSearchRaw() throws Exception {
        JmConfiguration config = JmConfiguration.builder()
                .timeout(Duration.ofSeconds(20))
                .build();
        var ctx = OkHttpBuilder.build(config);
        JmApiClient client = new JmApiClient(config, ctx.getClient(), ctx.getCookieManager(), ctx.getDomainManager());
        client.initializeAsync().get();
        try {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("https")
                    .host(JmConstants.PLACEHOLDER_HOST)
                    .addPathSegment("search")
                    .addQueryParameter("search_query", "原神")
                    .addQueryParameter("page", "1")
                    .build();

            Method m = io.github.jukomu.jmcomic.core.client.impl.JmApiClient.class
                    .getDeclaredMethod("executeGetRequest", HttpUrl.class, String.class);
            m.setAccessible(true);
            JmApiResponse resp = (JmApiResponse) m.invoke(client, url, JmConstants.APP_TOKEN_SECRET);
            String data = resp.getDecodedData();
            System.out.println("### raw search json: " + data.substring(0, Math.min(2500, data.length())));
        } finally {
            client.close();
        }
    }
}
