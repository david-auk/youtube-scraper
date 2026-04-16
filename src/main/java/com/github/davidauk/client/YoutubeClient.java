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

    /**
     * Creates a new client instance with its own {@link ObjectMapper} for parsing
     * YouTube HTML-embedded JSON responses and ajax browse payloads.
     */
    public YoutubeClient() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches channel content according to the supplied request and converts the
     * raw YouTube renderer nodes into simplified {@link Video} models.
     *
     * @param channel the already resolved channel reference
     * @param request controls content type, sort order, limit, delay, and proxy usage
     * @return a list of simplified videos extracted from the channel page
     */
    public List<Video> getChannel(Channel channel, ChannelRequest request) throws IOException, InterruptedException {
        return getChannelRaw(channel, request).stream()
                .map(this::toVideo)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Resolves a channel from a user-facing differentiator such as a channel id,
     * handle, or URL-like value, then fetches the requested channel content.
     *
     * @param channelDifferentiator a value that can be resolved into a {@link Channel}
     * @param request controls content type, sort order, limit, delay, and proxy usage
     * @return a list of simplified videos extracted from the channel page
     */
    public List<Video> getChannel(String channelDifferentiator, ChannelRequest request) throws IOException, InterruptedException {
        return getChannel(ChannelFactory.buildChannelRequest(channelDifferentiator, this), request);
    }

    /**
     * Fetches playlist items and converts the raw YouTube renderer nodes into
     * simplified {@link Video} models.
     *
     * @param request controls playlist id, limit, delay, and proxy usage
     * @return a list of simplified videos extracted from the playlist page
     */
    public List<Video> getPlaylistVideos(PlaylistRequest request) throws IOException, InterruptedException {
        return getPlaylistRaw(request).stream()
                .map(this::toVideo)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Resolves a canonical YouTube channel id from a public handle such as
     * {@code @lekkerspelen}. The method first checks channel metadata and then
     * falls back to any discovered browse id that looks like a channel id.
     *
     * @param channelUsername the public handle, with or without a leading {@code @}
     * @return the canonical YouTube channel id starting with {@code UC}
     */
    public String resolveChannelIdFromUsername(String channelUsername) throws IOException, InterruptedException {
        if (channelUsername == null || channelUsername.isBlank()) {
            throw new IllegalArgumentException("channelUsername is required.");
        }

        YoutubeHttpClient session = new YoutubeHttpClient(null);
        String normalizedUsername = channelUsername.startsWith("@")
                ? channelUsername.substring(1)
                : channelUsername;

        // Load the public channel page and inspect the embedded page data.
        String html = session.get("https://www.youtube.com/@" + normalizedUsername);
        JsonNode initialData = objectMapper.readTree(
                HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
        );

        // Prefer the canonical channel id from metadata when it is available.
        JsonNode metadata = JsonNavigator.findFirst(initialData, "channelMetadataRenderer");
        if (metadata != null) {
            JsonNode externalId = metadata.get("externalId");
            if (externalId != null && !externalId.isNull() && !externalId.asText().isBlank()) {
                return externalId.asText();
            }
        }

        // Fall back to a browse id if it looks like a real channel id.
        JsonNode browseIdNode = JsonNavigator.findFirst(initialData, "browseId");
        if (browseIdNode != null && !browseIdNode.isNull()) {
            String browseId = browseIdNode.asText();
            if (!browseId.isBlank() && browseId.startsWith("UC")) {
                return browseId;
            }
        }

        throw new IllegalStateException("Could not resolve channel id for username: " + channelUsername);
    }

    /**
     * Fetches raw channel renderer nodes without converting them into domain models.
     * This is useful when callers need direct access to the original YouTube payload.
     *
     * @param channel the already resolved channel reference
     * @param request controls content type, sort order, limit, delay, and proxy usage
     * @return raw renderer nodes that match the requested channel content type
     */
    public List<JsonNode> getChannelRaw(Channel channel, ChannelRequest request) throws IOException, InterruptedException {
        String url = channel.baseUrl() + "/" + request.contentType().pathSegment() + "?view=0&flow=grid";

        return getVideos(
                url,
                "contents",
                request.contentType().selectorItem(),
                request.limit(),
                request.sleep(),
                request.proxySelector(),
                request.sortBy()
        );
    }

    /**
     * Resolves a channel from a user-facing differentiator and fetches the raw
     * renderer nodes for the requested channel content.
     *
     * @param channelDifferentiator a value that can be resolved into a {@link Channel}
     * @param request controls content type, sort order, limit, delay, and proxy usage
     * @return raw renderer nodes that match the requested channel content type
     */
    public List<JsonNode> getChannelRaw(String channelDifferentiator, ChannelRequest request) throws IOException, InterruptedException {
        return getChannelRaw(ChannelFactory.buildChannelRequest(channelDifferentiator, this), request);
    }

    /**
     * Fetches raw playlist renderer nodes without converting them into domain models.
     *
     * @param request controls playlist id, limit, delay, and proxy usage
     * @return raw renderer nodes extracted from the playlist page
     */
    public List<JsonNode> getPlaylistRaw(PlaylistRequest request) throws IOException, InterruptedException {
        String url = "https://www.youtube.com/playlist?list=" + request.playlistId();

        return getVideos(
                url,
                "playlistVideoListRenderer",
                "playlistVideoRenderer",
                request.limit(),
                request.sleep(),
                request.proxySelector(),
                null
        );
    }

    /**
     * Fetches the primary information block for a single video page.
     *
     * @param videoId the YouTube video id
     * @return the raw {@code videoPrimaryInfoRenderer} node for the video page
     */
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

    /**
     * Shared pagination routine used by both channel and playlist requests.
     *
     * <p>On the first request this method downloads the HTML page, extracts the
     * embedded JSON structures, and finds the initial renderer list. On follow-up
     * requests it uses YouTube's browse ajax endpoint together with continuation
     * tokens to load more items.
     *
     * <p>For non-default channel sorting, the first page is only used to discover
     * the continuation token behind the selected sort option. The actual content is
     * then fetched through the ajax continuation flow.
     */
    private List<JsonNode> getVideos(
            String url,
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

            // The first iteration parses the full HTML page to bootstrap client metadata,
            // the initial content tree, and the first continuation token.
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

                // Non-default sort actions are defined at page level, while regular
                // pagination continuation usually lives inside the narrowed content tree.
                JsonNode nextDataSource = (sortBy != null && sortBy != ChannelSort.NEWEST)
                        ? initialData
                        : data;

                nextData = getNextData(nextDataSource, sortBy);

                isFirst = false;

                // For alternative sorting, the first page only discovers the ajax
                // continuation behind the selected sort option. The next loop iteration
                // performs the real data fetch for that sort order.
                if (sortBy != null && sortBy != ChannelSort.NEWEST) {
                    if (nextData == null) {
                        throw new IllegalStateException("Could not resolve continuation data for sort: " + sortBy);
                    }
                    continue;
                }
            } else {
                // Follow-up iterations use continuation-based ajax responses instead of
                // re-downloading the entire page HTML.
                data = getAjaxData(session, apiKey, nextData, client);
                nextData = getNextData(data, null);
            }

            // Extract all matching renderer items from the current response chunk.
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

    /**
     * Calls YouTube's internal browse endpoint using a continuation token so the
     * next page of content can be loaded without reloading the full HTML page.
     */
    private JsonNode getAjaxData(
            YoutubeHttpClient session,
            String apiKey,
            ContinuationData nextData,
            JsonNode client
    ) throws IOException, InterruptedException {

        if (nextData == null) {
            throw new IllegalArgumentException("nextData is required for ajax continuation.");
        }

        // Recreate the internal browse request shape expected by YouTube's ajax endpoint.
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode contextNode = requestBody.putObject("context");
        ObjectNode clickTrackingNode = contextNode.putObject("clickTracking");
        clickTrackingNode.put("clickTrackingParams", nextData.clickTrackingParams());
        contextNode.set("client", client);
        requestBody.put("continuation", nextData.token());

        String responseBody = session.postJson(YoutubeClient.BROWSE_API_ENDPOINT, apiKey, objectMapper.writeValueAsString(requestBody));
        return objectMapper.readTree(responseBody);
    }

    /**
     * Converts a raw YouTube renderer node into the library's simplified
     * {@link Video} representation.
     */
    private Video toVideo(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        // Pull a compact subset of fields that are stable enough for the public model.
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

    /**
     * Reads a direct text value from a field when that field exists and is not null.
     */
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

    /**
     * Extracts user-visible text from common YouTube text shapes such as
     * {@code simpleText}, {@code runs}, or accessibility labels.
     */
    private String extractText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        // YouTube uses multiple text representations depending on renderer type.
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

    /**
     * Returns the first thumbnail URL from a YouTube thumbnail block.
     */
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

    /**
     * Checks whether a badge array contains a renderer whose visible or accessible
     * label matches the expected value.
     */
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

    /**
     * Resolves the continuation payload needed for the next ajax request.
     *
     * <p>For regular pagination this reads the generic continuation endpoint from
     * the current renderer tree. For alternative channel sorting it delegates to a
     * helper that extracts the continuation hidden behind a sort menu option.
     */
    private ContinuationData getNextData(JsonNode data, ChannelSort sortBy) {
        // Alternative sort modes require a different continuation lookup path.
        if (sortBy != null && sortBy != ChannelSort.NEWEST) {
            return getSortContinuationData(data, sortBy);
        }

        JsonNode endpoint = JsonNavigator.findFirst(data, "continuationEndpoint");
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

    /**
     * Extracts the continuation token for a channel sort option such as
     * {@code Popular} or {@code Oldest}.
     *
     * <p>YouTube currently exposes these sort actions through list item view models
     * rather than the older chip renderer structure, so this method matches the
     * desired label and then scans the embedded command list for a
     * {@code continuationCommand}.
     */
    private ContinuationData getSortContinuationData(JsonNode data, ChannelSort sortBy) {
        String expectedLabel = switch (sortBy) {
            case POPULAR -> "Popular";
            case OLDEST -> "Oldest";
            default -> throw new IllegalStateException("Unexpected value: " + sortBy);
        };

        // Search all sort menu entries and match the human-readable label.
        List<JsonNode> items = JsonNavigator.findAll(data, "listItemViewModel");
        for (JsonNode item : items) {
            String label = item.path("title").path("content").asText(null);
            if (!expectedLabel.equals(label)) {
                continue;
            }

            // The selected sort action stores its continuation inside a nested command list.
            JsonNode commands = item.path("rendererContext")
                    .path("commandContext")
                    .path("onTap")
                    .path("innertubeCommand")
                    .path("commandExecutorCommand")
                    .path("commands");

            if (!commands.isArray()) {
                continue;
            }

            for (JsonNode command : commands) {
                JsonNode continuationCommand = command.get("continuationCommand");
                JsonNode clickTrackingParams = command.get("clickTrackingParams");

                if (continuationCommand != null && clickTrackingParams != null) {
                    return new ContinuationData(
                            continuationCommand.path("token").asText(),
                            clickTrackingParams.asText()
                    );
                }
            }
        }

        return null;
    }
}