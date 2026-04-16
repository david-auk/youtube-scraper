package com.github.davidauk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.davidauk.model.*;
import com.github.davidauk.util.HtmlJsonExtractor;
import com.github.davidauk.util.JsonNavigator;

import java.io.IOException;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class YoutubeClient {
    private static final String BROWSE_API_ENDPOINT = "https://www.youtube.com/youtubei/v1/browse";

    private final ObjectMapper objectMapper;

    public YoutubeClient() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Video> getChannel(ChannelRequest request) throws IOException, InterruptedException {
        return getChannelRaw(request).stream()
                .map(this::toVideo)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Video> getPlaylistVideos(PlaylistRequest request) throws IOException, InterruptedException {
        return getPlaylist(request).stream()
                .map(this::toVideo)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<JsonNode> getChannelRaw(ChannelRequest request) throws IOException, InterruptedException {
        String url = request.baseUrl() + "/" + request.contentType().pathSegment() + "?view=0&flow=grid";

        return getVideos(
                url,
                BROWSE_API_ENDPOINT,
                "contents",
                request.contentType().selectorItem(),
                request.limit(),
                request.sleep(),
                request.proxySelector(),
                request.sortBy()
        );
    }

    public List<JsonNode> getPlaylist(PlaylistRequest request) throws IOException, InterruptedException {
        String url = "https://www.youtube.com/playlist?list=" + request.playlistId();

        return getVideos(
                url,
                BROWSE_API_ENDPOINT,
                "playlistVideoListRenderer",
                "playlistVideoRenderer",
                request.limit(),
                request.sleep(),
                request.proxySelector(),
                null
        );
    }

    public JsonNode getVideo(String videoId) throws IOException, InterruptedException {
        YoutubeHttpClient session = new YoutubeHttpClient(null);

        String url = "https://www.youtube.com/watch?v=" + videoId;
        String html = session.get(url);

        JsonNode client = objectMapper.readTree(
                HtmlJsonExtractor.extract(html, "INNERTUBE_CONTEXT", 2, "\"}},") + "\"}}"
        ).get("client");

        session.setYoutubeClientVersion(client.get("clientVersion").asText());

        JsonNode data = objectMapper.readTree(
                HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
        );

        JsonNode result = JsonNavigator.findFirst(data, "videoPrimaryInfoRenderer");
        if (result == null) {
            throw new IllegalStateException("Could not find videoPrimaryInfoRenderer for video: " + videoId);
        }

        return result;
    }

    private List<JsonNode> getVideos(
            String url,
            String apiEndpoint,
            String selectorList,
            String selectorItem,
            Integer limit,
            Duration sleep,
            ProxySelector proxySelector,
            ChannelSort sortBy
    ) throws IOException, InterruptedException {

        YoutubeHttpClient session = new YoutubeHttpClient(proxySelector);
        List<JsonNode> results = new ArrayList<>();

        boolean isFirst = true;
        JsonNode client = null;
        String apiKey = null;
        ContinuationData nextData = null;

        while (true) {
            JsonNode data;

            if (isFirst) {
                String html = session.get(url);

                client = objectMapper.readTree(
                        HtmlJsonExtractor.extract(html, "INNERTUBE_CONTEXT", 2, "\"}},") + "\"}}"
                ).get("client");

                apiKey = HtmlJsonExtractor.extract(html, "innertubeApiKey", 3, "\"");
                session.setYoutubeClientVersion(client.get("clientVersion").asText());

                JsonNode initialData = objectMapper.readTree(
                        HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
                );

                data = JsonNavigator.findFirst(initialData, selectorList);
                if (data == null) {
                    throw new IllegalStateException("Could not find selector list: " + selectorList);
                }

                nextData = getNextData(data, sortBy);
                isFirst = false;

                if (sortBy != null && sortBy != ChannelSort.NEWEST) {
                    continue;
                }
            } else {
                data = getAjaxData(session, apiEndpoint, apiKey, nextData, client);
                nextData = getNextData(data, null);
            }

            List<JsonNode> items = JsonNavigator.findAll(data, selectorItem);
            for (JsonNode item : items) {
                results.add(item);
                if (limit != null && results.size() >= limit) {
                    return results;
                }
            }

            if (nextData == null) {
                break;
            }

            Thread.sleep(sleep.toMillis());
        }

        return results;
    }

    private JsonNode getAjaxData(
            YoutubeHttpClient session,
            String apiEndpoint,
            String apiKey,
            ContinuationData nextData,
            JsonNode client
    ) throws IOException, InterruptedException {

        if (nextData == null) {
            throw new IllegalArgumentException("nextData is required for ajax continuation.");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode contextNode = requestBody.putObject("context");
        ObjectNode clickTrackingNode = contextNode.putObject("clickTracking");
        clickTrackingNode.put("clickTrackingParams", nextData.clickTrackingParams());
        contextNode.set("client", client);
        requestBody.put("continuation", nextData.token());

        String responseBody = session.postJson(apiEndpoint, apiKey, objectMapper.writeValueAsString(requestBody));
        return objectMapper.readTree(responseBody);
    }

    private Video toVideo(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        String id = textAt(node, "videoId");
        String title = extractText(node.get("title"));
        String description = extractText(node.get("descriptionSnippet"));
        String publishedTime = extractText(node.get("publishedTimeText"));
        String duration = extractText(node.get("lengthText"));
        String thumbnailUrl = extractFirstThumbnailUrl(node.get("thumbnail"));
        boolean membersOnly = hasBadgeLabel(node.get("badges"), "Members only");
        boolean verified = hasBadgeLabel(node.get("ownerBadges"), "Verified");

        if (id == null && title == null) {
            return null;
        }

        return new Video(
                id,
                title,
                description,
                publishedTime,
                duration,
                thumbnailUrl,
                membersOnly,
                verified
        );
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        JsonNode field = node.get(fieldName);
        if (field == null || field.isMissingNode() || field.isNull()) {
            return null;
        }

        return field.asText();
    }

    private String extractText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode simpleText = node.get("simpleText");
        if (simpleText != null && !simpleText.isNull()) {
            return simpleText.asText();
        }

        JsonNode runs = node.get("runs");
        if (runs != null && runs.isArray() && !runs.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode run : runs) {
                JsonNode text = run.get("text");
                if (text != null && !text.isNull()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(text.asText());
                }
            }

            return builder.isEmpty() ? null : builder.toString();
        }

        JsonNode accessibilityLabel = node.path("accessibility")
                .path("accessibilityData")
                .path("label");
        if (!accessibilityLabel.isMissingNode() && !accessibilityLabel.isNull()) {
            return accessibilityLabel.asText();
        }

        return null;
    }

    private String extractFirstThumbnailUrl(JsonNode thumbnailNode) {
        if (thumbnailNode == null || thumbnailNode.isMissingNode() || thumbnailNode.isNull()) {
            return null;
        }

        JsonNode thumbnails = thumbnailNode.get("thumbnails");
        if (thumbnails == null || !thumbnails.isArray() || thumbnails.isEmpty()) {
            return null;
        }

        JsonNode firstThumbnail = thumbnails.get(0);
        if (firstThumbnail == null || firstThumbnail.isNull()) {
            return null;
        }

        JsonNode url = firstThumbnail.get("url");
        return url == null || url.isNull() ? null : url.asText();
    }

    private boolean hasBadgeLabel(JsonNode badgesNode, String expectedLabel) {
        if (badgesNode == null || badgesNode.isMissingNode() || !badgesNode.isArray()) {
            return false;
        }

        for (JsonNode badge : badgesNode) {
            JsonNode renderer = badge.get("metadataBadgeRenderer");
            if (renderer == null || renderer.isNull()) {
                continue;
            }

            JsonNode label = renderer.get("label");
            if (label != null && !label.isNull() && expectedLabel.equals(label.asText())) {
                return true;
            }

            JsonNode accessibilityLabel = renderer.path("accessibilityData").path("label");
            if (!accessibilityLabel.isMissingNode() && !accessibilityLabel.isNull()
                    && expectedLabel.equals(accessibilityLabel.asText())) {
                return true;
            }

            JsonNode tooltip = renderer.get("tooltip");
            if (tooltip != null && !tooltip.isNull() && expectedLabel.equals(tooltip.asText())) {
                return true;
            }
        }

        return false;
    }

    private ContinuationData getNextData(JsonNode data, ChannelSort sortBy) {
        JsonNode endpoint;

        if (sortBy != null && sortBy != ChannelSort.NEWEST) {
            JsonNode feedFilterChipBar = JsonNavigator.findFirst(data, "feedFilterChipBarRenderer");
            if (feedFilterChipBar == null || !feedFilterChipBar.has("contents")) {
                return null;
            }

            int index = switch (sortBy) {
                case NEWEST -> 0;
                case POPULAR -> 1;
                case OLDEST -> 2;
            };

            JsonNode contents = feedFilterChipBar.get("contents");
            if (!contents.isArray() || contents.size() <= index) {
                return null;
            }

            endpoint = contents.get(index)
                    .path("chipCloudChipRenderer")
                    .path("navigationEndpoint");
        } else {
            endpoint = JsonNavigator.findFirst(data, "continuationEndpoint");
        }

        if (endpoint == null || endpoint.isMissingNode()) {
            return null;
        }

        JsonNode continuationCommand = endpoint.get("continuationCommand");
        JsonNode clickTrackingParams = endpoint.get("clickTrackingParams");

        if (continuationCommand == null || clickTrackingParams == null) {
            return null;
        }

        return new ContinuationData(
                continuationCommand.get("token").asText(),
                clickTrackingParams.asText()
        );
    }
}