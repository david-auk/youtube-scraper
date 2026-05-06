package com.github.davidauk.youtubescraper.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class YoutubeHttpClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private String youtubeClientVersion;

    public YoutubeHttpClient(ProxySelector proxySelector) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        cookieManager.getCookieStore().add(
                URI.create("https://www.youtube.com"),
                new HttpCookie("CONSENT", "YES+cb")
        );

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookieManager);

        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }

        this.httpClient = builder.build();
    }

    public void setYoutubeClientVersion(String youtubeClientVersion) {
        this.youtubeClientVersion = youtubeClientVersion;
    }

    public String get(String url) throws IOException, InterruptedException {
        String finalUrl = url + (url.contains("?") ? "&" : "?") + "ucbcb=1";

        HttpRequest request = HttpRequest.newBuilder(URI.create(finalUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), "GET", url);
        return response.body();
    }

    public String postJson(String url, String apiKey, String jsonBody) throws IOException, InterruptedException {
        String fullUrl = url + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(fullUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .header("Content-Type", "application/json");

        if (youtubeClientVersion != null && !youtubeClientVersion.isBlank()) {
            builder.header("X-YouTube-Client-Name", "1");
            builder.header("X-YouTube-Client-Version", youtubeClientVersion);
        }

        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), "POST", url);
        return response.body();
    }

    private static void ensureSuccess(int statusCode, String method, String url) throws IOException {
        if (statusCode < HttpURLConnection.HTTP_OK || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new IOException(method + " " + url + " failed with status " + statusCode);
        }
    }
}
