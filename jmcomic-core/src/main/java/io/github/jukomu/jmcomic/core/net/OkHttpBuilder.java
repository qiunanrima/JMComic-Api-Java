package io.github.jukomu.jmcomic.core.net;

import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import io.github.jukomu.jmcomic.core.constant.JmConstants;
import io.github.jukomu.jmcomic.core.net.interceptor.RetryAndDomainRedirectInterceptor;
import io.github.jukomu.jmcomic.core.net.interceptor.UserAgentInterceptor;
import io.github.jukomu.jmcomic.core.net.provider.JmDomainManager;
import okhttp3.OkHttpClient;
import okhttp3.java.net.cookiejar.JavaNetCookieJar;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;

/**
 * @author JUKOMU
 * @Description: 内部工厂类，负责根据 JmConfiguration 构建和组装一个完整的 OkHttpClient 实例
 * 它封装了所有关于拦截器、Cookie管理、代理、超时等配置的细节
 * @Project: jmcomic-api-java
 * @Date: 2025/10/28
 */
public final class OkHttpBuilder {

    private OkHttpBuilder() {
    }

    /**
     * 根据给定的配置创建一个新的 OkHttpClient 实例
     * 每个创建的实例都拥有独立的 CookieJar 和域名管理器，确保客户端之间的状态隔离
     *
     * @param config 用户的配置对象
     * @return 一个配置好的 OkHttpClient 实例
     */
    public static HttpClientContext build(JmConfiguration config) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        List<String> initialDomains;
        if (config.getClientType() == ClientType.API) {
            initialDomains = config.getApiDomains().isEmpty() ? JmConstants.DEFAULT_API_DOMAINS : config.getApiDomains();
        } else {
            initialDomains = config.getHtmlDomains().isEmpty() ? List.of("18comic.vip") : config.getHtmlDomains();
        }
        JmDomainManager domainManager = new JmDomainManager(initialDomains);

        OkHttpClient client = buildClient(config, cookieManager, domainManager);
        return new HttpClientContext(client, domainManager, cookieManager);
    }

    /**
     * 根据配置构建 OkHttpClient，并复用调用方提供的 Cookie 管理器与域名管理器。
     * 用于客户端初始化失败重启时，在保留会话与域名状态的前提下重建底层 HTTP 客户端。
     *
     * @param config        用户的配置对象
     * @param cookieManager 复用的 Cookie 管理器
     * @param domainManager 复用的域名管理器
     * @return 配置好的 OkHttpClient 实例
     */
    public static OkHttpClient buildClient(JmConfiguration config, CookieManager cookieManager, JmDomainManager domainManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 配置 proxy, timeout, cookieJar
        builder.proxy(config.getProxy());
        builder.connectTimeout(config.getTimeout());
        builder.readTimeout(config.getTimeout());
        builder.writeTimeout(config.getTimeout());
        builder.addInterceptor(new UserAgentInterceptor(config));
        builder.cookieJar(new JavaNetCookieJar(cookieManager));

        builder.addInterceptor(new RetryAndDomainRedirectInterceptor(config.getRetryTimes(), domainManager));
        builder.retryOnConnectionFailure(false);
        return builder.build();
    }

    /**
     * 内部数据类，用于捆绑 OkHttpClient 及其关联的有状态组件
     */
    public static class HttpClientContext {
        private final OkHttpClient client;
        private final JmDomainManager domainManager;
        private final CookieManager cookieManager;

        HttpClientContext(OkHttpClient client, JmDomainManager domainManager, CookieManager cookieManager) {
            this.client = client;
            this.domainManager = domainManager;
            this.cookieManager = cookieManager;
        }

        public OkHttpClient getClient() {
            return client;
        }

        public JmDomainManager getDomainManager() {
            return domainManager;
        }

        public CookieManager getCookieManager() {
            return cookieManager;
        }
    }
}
