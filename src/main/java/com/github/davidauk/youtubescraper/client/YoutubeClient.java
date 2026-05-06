package com.github.davidauk.youtubescraper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.davidauk.youtubescraper.model.*;
import com.github.davidauk.youtubescraper.model.content.PartialVideo;
import com.github.davidauk.youtubescraper.model.content.Video;
import com.github.davidauk.youtubescraper.node.WatchPageNode;
import com.github.davidauk.youtubescraper.node.YoutubeBadgeNode;
import com.github.davidauk.youtubescraper.node.YoutubeTextNode;
import com.github.davidauk.youtubescraper.selector.PartialVideoSelectorItemNode;
import com.github.davidauk.youtubescraper.selector.SelectorItemNodeFactory;
import com.github.davidauk.youtubescraper.selector.WatchPageSelectorItemNode;
import com.github.davidauk.youtubescraper.util.HtmlJsonExtractor;
import com.github.davidauk.youtubescraper.util.JsonNavigator;

import java.io.IOException;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class YoutubeClient {
        private static final String BROWSE_API_ENDPOINT = "https://www.youtube.com/youtubei/v1/browse";
        private static final int MAX_EMPTY_CONTINUATION_PAGES = 2;
        private final ObjectMapper objectMapper;

        /**
         * Creates a new client instance with its own {@link ObjectMapper} for parsing
         * YouTube HTML-embedded JSON responses and ajax browse payloads.
         */
        public YoutubeClient() {
                this.objectMapper = new ObjectMapper();
        }

        /**
         * Fetches channelId content according to the supplied request and converts the
         * raw YouTube renderer nodes into overview-level {@link PartialVideo} models.
         *
         * @param channelId the already resolved channelId reference
         * @param request   controls content type, sort order, limit, delay, and proxy usage
         * @return a ChannelOverviewResponse containing the channelId and its videos
         */
        public ChannelOverviewResponse getChannel(ChannelId channelId, ChannelRequest request) throws IOException, InterruptedException {
                List<PartialVideoSelectorItemNode> channelResponse = getChannelItems(channelId, request);

                Channel channel = getChannelMetadata(channelId, request.proxySelector());

                List<PartialVideo> videos = channelResponse.stream()
                        .map(videoNode -> videoNode.toPartialVideo(request.contentType()))
                        .filter(Objects::nonNull)
                        .toList();

                boolean isVerified = false;

                if (!videos.isEmpty()) {
                        JsonNode channelNode = channelResponse.getFirst().raw();
                        isVerified = new YoutubeBadgeNode(channelNode.get("ownerBadges")).hasLabel("Verified");
                }

                return new ChannelOverviewResponse(
                        channel,
                        isVerified,
                        videos,
                        channelResponse.stream().map(PartialVideoSelectorItemNode::raw).toList()
                );
        }

        /**
         * Resolves a channel from a user-facing differentiator such as a channel id,
         * handle, or URL-like value, then fetches the requested channel content.
         *
         * @param channelDifferentiator a value that can be resolved into a {@link ChannelId}
         * @param request               controls content type, sort order, limit, delay, and proxy usage
         * @return a ChannelOverviewResponse containing the channel and its videos
         */
        public ChannelOverviewResponse getChannel(String channelDifferentiator, ChannelRequest request) throws IOException, InterruptedException {
                return getChannel(ChannelFactory.buildChannel(channelDifferentiator, this), request);
        }

        /**
         * Fetches channel-level metadata from the public channel page.
         *
         * <p>YouTube exposes the most reliable channel title in the embedded
         * channelMetadataRenderer. The fallback paths make the method resilient when
         * YouTube changes the metadata shape or only exposes the title through header
         * renderers.
         */
        private Channel getChannelMetadata(ChannelId channelId, ProxySelector proxySelector) throws IOException, InterruptedException {
                YoutubeHttpClient session = new YoutubeHttpClient(proxySelector);
                String url = channelId.baseUrl();

                String html = session.get(url);
                validateYoutubeHtmlResponse(html, url);

                JsonNode initialData = objectMapper.readTree(
                        HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
                );

                String title = extractChannelTitle(initialData);

                return new Channel(
                        channelId,
                        title
                );
        }

        /**
         * Extracts the channel title from the known YouTube channel metadata/header
         * shapes, ordered from most canonical to most defensive fallback.
         */
        private String extractChannelTitle(JsonNode initialData) {
                JsonNode metadata = JsonNavigator.findFirst(initialData, "channelMetadataRenderer");
                if (metadata != null) {
                        String title = firstNonBlank(
                                textAt(metadata, "title"),
                                new YoutubeTextNode(metadata.get("title")).text()
                        );

                        if (title != null) {
                                return title;
                        }
                }

                JsonNode header = firstExistingNode(
                        JsonNavigator.findFirst(initialData, "pageHeaderRenderer"),
                        JsonNavigator.findFirst(initialData, "c4TabbedHeaderRenderer"),
                        JsonNavigator.findFirst(initialData, "carouselHeaderRenderer")
                );

                if (header != null) {
                        String title = firstNonBlank(
                                new YoutubeTextNode(header.get("title")).text(),
                                textAt(header.path("content"), "title"),
                                textAt(header, "title")
                        );

                        if (title != null) {
                                return title;
                        }
                }

                return null;
        }

        /**
         * Returns the first usable JsonNode from the supplied candidates.
         */
        private JsonNode firstExistingNode(JsonNode... nodes) {
                if (nodes == null) {
                        return null;
                }

                for (JsonNode node : nodes) {
                        if (node != null && !node.isMissingNode() && !node.isNull()) {
                                return node;
                        }
                }

                return null;
        }

//    /**
//     * Fetches playlist items and converts the raw YouTube renderer nodes into
//     * simplified {@link PartialVideo} models.
//     *
//     * @param request controls playlist id, limit, delay, and proxy usage
//     * @return a list of simplified videos extracted from the playlist page
//     */
//    public List<PartialVideo> getPlaylistVideos(PlaylistRequest request) throws IOException, InterruptedException {
//        return getPlaylistItems(request).stream()
//                .map(videoNode -> videoNode.toPartialVideo(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }

        /**
         * Fetches typed partial-video selector item nodes for a channel page.
         *
         * <p>The concrete implementation is chosen from the selector item that matched
         * the YouTube response, for example videoRenderer or lockupViewModel.
         */
        public List<PartialVideoSelectorItemNode> getChannelItems(ChannelId channelId, ChannelRequest request) throws IOException, InterruptedException {
                String url = channelId.baseUrl() + "/" + request.contentType().pathSegment() + "?view=0&flow=grid";

                return getSelectorItems(
                        url,
                        "contents",
                        request.contentType().selectorItems(),
                        request.limit(),
                        request.sleep(),
                        request.proxySelector(),
                        request.sortBy()
                );
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
        public List<JsonNode> getChannelVideosRaw(ChannelId channel, ChannelRequest request) throws IOException, InterruptedException {
                return getChannelItems(channel, request).stream()
                        .map(PartialVideoSelectorItemNode::raw)
                        .toList();
        }
//    /**
//     * Fetches typed selector item nodes for a playlist page.
//     */
//    public List<SelectorItemNode> getPlaylistItems(PlaylistRequest request) throws IOException, InterruptedException {
//        String url = "https://www.youtube.com/playlist?list=" + request.playlistId();
//
//        return getSelectorItems(
//                url,
//                "playlistVideoListRenderer",
//                List.of("playlistVideoRenderer"),
//                request.limit(),
//                request.sleep(),
//                request.proxySelector(),
//                null
//        );
//    }

        /**
         * Resolves a channel from a user-facing differentiator and fetches the raw
         * renderer nodes for the requested channel content.
         *
         * @param channelDifferentiator a value that can be resolved into a {@link ChannelId}
         * @param request               controls content type, sort order, limit, delay, and proxy usage
         * @return raw renderer nodes that match the requested channel content type
         */
        public List<JsonNode> getChannelVideosRaw(String channelDifferentiator, ChannelRequest request) throws IOException, InterruptedException {
                return getChannelVideosRaw(ChannelFactory.buildChannel(channelDifferentiator, this), request);
        }

//    /**
//     * Fetches raw playlist renderer nodes without converting them into domain models.
//     *
//     * @param request controls playlist id, limit, delay, and proxy usage
//     * @return raw renderer nodes extracted from the playlist page
//     */
//    public List<JsonNode> getPlaylistRaw(PlaylistRequest request) throws IOException, InterruptedException {
//        return getPlaylistItems(request).stream()
//                .map(SelectorItemNode::raw)
//                .toList();
//    }

        /**
         * Fetches a single video page and delegates conversion to the selector item node.
         *
         * @param videoId the YouTube video id
         * @return the Video object for the video page
         */
        public Video getVideo(String videoId) throws IOException, InterruptedException {
                return getWatchPageItem(videoId).toVideo();
        }

        /**
         * Fetches a typed selector item node for a single video page.
         */
        public WatchPageSelectorItemNode getWatchPageItem(String videoId) throws IOException, InterruptedException {
                return new WatchPageSelectorItemNode(getWatchPage(videoId));
        }

        public WatchPageNode getWatchPage(String videoId) throws IOException, InterruptedException {
                YoutubeHttpClient session = new YoutubeHttpClient(null);

                String html = session.get("https://www.youtube.com/watch?v=" + videoId);

                JsonNode initialData = objectMapper.readTree(
                        HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
                );

                JsonNode playerResponse = objectMapper.readTree(
                        HtmlJsonExtractor.extract(html, "var ytInitialPlayerResponse = ", 0, "};") + "}"
                );

                return new WatchPageNode(videoId, initialData, playerResponse);
        }

//    /**
//     * Fetches the primary information block for a single video page without converting it.
//     *
//     * @param videoId the YouTube video id
//     * @return the raw {@code videoPrimaryInfoRenderer} node for the video page
//     */
//    public JsonNode getVideoRaw(String videoId) throws IOException, InterruptedException {
//        YoutubeHttpClient session = new YoutubeHttpClient(null);
//
//        String url = "https://www.youtube.com/watch?v=" + videoId;
//        String html = session.get(url);
//
//        JsonNode client = objectMapper.readTree(
//                HtmlJsonExtractor.extract(html, "INNERTUBE_CONTEXT", 2, "\"}},") + "\"}}"
//        ).get("client");
//
//        session.setYoutubeClientVersion(client.get("clientVersion").asText());
//
//        JsonNode data = objectMapper.readTree(
//                HtmlJsonExtractor.extract(html, "var ytInitialData = ", 0, "};") + "}"
//        );
//
//        JsonNode result = JsonNavigator.findFirst(data, "videoPrimaryInfoRenderer");
//        if (result == null) {
//            throw new IllegalStateException("Could not find videoPrimaryInfoRenderer for video: " + videoId);
//        }
//
//        return result;
//    }

        public Video getVideo(PartialVideo video) throws IOException, InterruptedException {
                return getVideo(video.id());
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
        private List<PartialVideoSelectorItemNode> getSelectorItems(
                String url,
                String selectorList,
                List<String> selectorItems,
                Integer limit,
                Duration sleep,
                ProxySelector proxySelector,
                ChannelSort sortBy
        ) throws IOException, InterruptedException {

                YoutubeHttpClient session = new YoutubeHttpClient(proxySelector);
                List<PartialVideoSelectorItemNode> results = new ArrayList<>();

                boolean isFirst = true;
                JsonNode client = null;
                String apiKey = null;
                ContinuationData nextData = null;
                int emptyContinuationPages = 0;

                while (true) {
                        JsonNode data;

                        // The first iteration parses the full HTML page to bootstrap client metadata,
                        // the initial content tree, and the first continuation token.
                        if (isFirst) {
                                isFirst = false;

                                String html = session.get(url);
                                validateYoutubeHtmlResponse(html, url);

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

                                validateInitialContentTree(data, selectorList, selectorItems, url);

                                // Non-default sort actions are defined at page level, while regular
                                // pagination continuation usually lives inside the narrowed content tree.
                                JsonNode nextDataSource = (sortBy != null && sortBy != ChannelSort.NEWEST)
                                        ? initialData
                                        : data;

                                nextData = getNextData(nextDataSource, sortBy);

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
                                validateAjaxContentTree(data, selectorItems, url);
                                nextData = getNextData(data, null);
                        }

                        // Extract all matching renderer items from the current response chunk.
                        // The selector list is ordered by preference. For example, streams may
                        // primarily use videoRenderer, but sometimes YouTube returns lockupViewModel.
                        List<PartialVideoSelectorItemNode> items = findFirstMatchingSelectorItems(data, selectorItems);
                        for (PartialVideoSelectorItemNode item : items) {
                                results.add(item);
                                if (limit != null && results.size() >= limit) {
                                        return results;
                                }
                        }

                        if (nextData == null) {
                                break;
                        }

                        if (items.isEmpty()) {
                                emptyContinuationPages++;
                                if (emptyContinuationPages >= MAX_EMPTY_CONTINUATION_PAGES) {
                                        break;
                                }
                        } else {
                                emptyContinuationPages = 0;
                        }

                        Thread.sleep(sleep.toMillis());
                }

                if (results.isEmpty()) {
                        throw new IllegalStateException(
                                "YouTube response was parsed successfully, but no items matching selectors " + selectorItems + " were found for: " + url
                        );
                }

                return results;
        }

        /**
         * Returns typed selector items for the first non-empty selector result.
         */
        private List<PartialVideoSelectorItemNode> findFirstMatchingSelectorItems(JsonNode data, List<String> selectorItems) {
                SelectorItemMatch match = findFirstMatchingItems(data, selectorItems);
                if (match.items().isEmpty()) {
                        return List.of();
                }

                return match.items().stream()
                        .map(item -> (PartialVideoSelectorItemNode) SelectorItemNodeFactory.from(match.selectorItem(), item))
                        .toList();
        }

        /**
         * Returns the first non-empty result for the ordered selector list.
         *
         * <p>The order matters: callers can pass their preferred stable renderer first
         * and newer/less-specific renderer shapes as fallback selectors.
         */
        private SelectorItemMatch findFirstMatchingItems(JsonNode data, List<String> selectorItems) {
                if (selectorItems == null || selectorItems.isEmpty()) {
                        return SelectorItemMatch.empty();
                }

                for (String selectorItem : selectorItems) {
                        if (selectorItem == null || selectorItem.isBlank()) {
                                continue;
                        }

                        List<JsonNode> items = JsonNavigator.findAll(data, selectorItem);
                        if (!items.isEmpty()) {
                                return new SelectorItemMatch(selectorItem, items);
                        }
                }

                return SelectorItemMatch.empty();
        }

        /**
         * Detects common non-content YouTube pages before JSON extraction turns them
         * into confusing empty results.
         */
        private void validateYoutubeHtmlResponse(String html, String url) {
                if (html == null || html.isBlank()) {
                        throw new IllegalStateException("YouTube returned an empty HTML response for: " + url);
                }

                String lowerHtml = html.toLowerCase();
                if (lowerHtml.contains("consent.youtube.com") || lowerHtml.contains("before you continue to youtube")) {
                        throw new IllegalStateException("YouTube returned a consent page instead of channel content for: " + url);
                }

                if (lowerHtml.contains("our systems have detected unusual traffic")
                        || lowerHtml.contains("detected unusual traffic from your computer network")
                        || lowerHtml.contains("/sorry/index")) {
                        throw new IllegalStateException("YouTube returned an anti-bot / unusual traffic page instead of channel content for: " + url);
                }

                if (!html.contains("ytInitialData")) {
                        throw new IllegalStateException("YouTube response did not contain ytInitialData for: " + url);
                }

                if (!html.contains("INNERTUBE_CONTEXT")) {
                        throw new IllegalStateException("YouTube response did not contain INNERTUBE_CONTEXT for: " + url);
                }
        }

        /**
         * Detects an initial page that has the expected content list, but not any of
         * the renderer types this client was asked to extract.
         */
        private void validateInitialContentTree(JsonNode data, String selectorList, List<String> selectorItems, String url) {
                if (selectorItems == null || selectorItems.isEmpty()) {
                        return;
                }

                if (!findFirstMatchingItems(data, selectorItems).items().isEmpty()) {
                        return;
                }

                if (JsonNavigator.findFirst(data, "messageRenderer") != null
                        || JsonNavigator.findFirst(data, "itemSectionRenderer") != null
                        || JsonNavigator.findFirst(data, "backgroundPromoRenderer") != null) {
                        throw new IllegalStateException(
                                "YouTube returned a non-video content tree for selector list '" + selectorList + "'. "
                                        + "Expected one of item selectors " + selectorItems + " for: " + url
                        );
                }
        }

        /**
         * Detects ajax continuation responses that are valid JSON but contain no
         * useful renderer items for this request.
         */
        private void validateAjaxContentTree(JsonNode data, List<String> selectorItems, String url) {
                if (data == null || data.isNull() || data.isMissingNode()) {
                        throw new IllegalStateException("YouTube returned an empty ajax continuation response for: " + url);
                }

                JsonNode error = data.get("error");
                if (error != null && !error.isNull()) {
                        throw new IllegalStateException("YouTube ajax continuation returned an error: " + error);
                }

                if (selectorItems == null || selectorItems.isEmpty()) {
                        return;
                }

                boolean hasContinuation = JsonNavigator.findFirst(data, "continuationEndpoint") != null;
                boolean hasItems = !findFirstMatchingItems(data, selectorItems).items().isEmpty();
                if (!hasItems && !hasContinuation) {
                        throw new IllegalStateException(
                                "YouTube ajax continuation contained none of selectors " + selectorItems + " and no further continuation for: " + url
                        );
                }
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
         * Returns the first non-blank value from the supplied candidates.
         */
        private String firstNonBlank(String... values) {
                if (values == null) {
                        return null;
                }

                for (String value : values) {
                        if (value != null && !value.isBlank()) {
                                return value;
                        }
                }

                return null;
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

        // (collectSortCandidatesByLabel, collectSortCandidatesByLabelRecursive, extractSortCandidateLabel,
        //  extractContinuationDataFromSortCandidate, findContinuationDataRecursively) methods removed.

        /**
         * Extracts continuation data when both required fields live on the same node.
         */
        private ContinuationData continuationDataFromNode(JsonNode node) {
                if (node == null || node.isMissingNode() || node.isNull()) {
                        return null;
                }

                JsonNode continuationCommand = node.get("continuationCommand");
                JsonNode clickTrackingParams = node.get("clickTrackingParams");

                if (continuationCommand == null || clickTrackingParams == null) {
                        return null;
                }

                String token = continuationCommand.path("token").asText(null);
                String tracking = clickTrackingParams.asText(null);

                if (token == null || token.isBlank() || tracking == null || tracking.isBlank()) {
                        return null;
                }

                return new ContinuationData(token, tracking);
        }

        /**
         * Extracts continuation data from a command list such as
         * commandExecutorCommand.commands.
         */
        private ContinuationData continuationDataFromCommandList(JsonNode commands) {
                if (commands == null || commands.isMissingNode() || !commands.isArray()) {
                        return null;
                }

                for (JsonNode command : commands) {
                        ContinuationData continuationData = continuationDataFromNode(command);
                        if (continuationData != null) {
                                return continuationData;
                        }
                }

                return null;
        }

        /**
         * Resolves sort continuation data from chip-based sort controls.
         */
        private ContinuationData findSortContinuationInChipViewModels(JsonNode data, String expectedLabel) {
                List<JsonNode> chips = JsonNavigator.findAll(data, "chipViewModel");
                for (JsonNode chip : chips) {
                        String label = firstNonBlank(
                                textAt(chip, "accessibilityLabel"),
                                textAt(chip, "text"),
                                new YoutubeTextNode(chip.get("title")).text()
                        );

                        if (!expectedLabel.equals(label)) {
                                continue;
                        }

                        JsonNode commandRoot = chip.path("tapCommand").path("innertubeCommand");

                        ContinuationData direct = continuationDataFromNode(commandRoot);
                        if (direct != null) {
                                return direct;
                        }

                        ContinuationData nested = continuationDataFromCommandList(
                                commandRoot.path("commandExecutorCommand").path("commands")
                        );
                        if (nested != null) {
                                return nested;
                        }
                }

                return null;
        }

        /**
         * Resolves sort continuation data from sheet/list style sort controls.
         */
        private ContinuationData findSortContinuationInListItemViewModels(JsonNode data, String expectedLabel) {
                List<JsonNode> items = JsonNavigator.findAll(data, "listItemViewModel");
                for (JsonNode item : items) {
                        String label = firstNonBlank(
                                textAt(item.path("title"), "content"),
                                textAt(item, "accessibilityLabel"),
                                new YoutubeTextNode(item.get("title")).text(),
                                new YoutubeTextNode(item.get("text")).text()
                        );

                        if (!expectedLabel.equals(label)) {
                                continue;
                        }

                        JsonNode commandRoot = item.path("rendererContext")
                                .path("commandContext")
                                .path("onTap")
                                .path("innertubeCommand");

                        ContinuationData direct = continuationDataFromNode(commandRoot);
                        if (direct != null) {
                                return direct;
                        }

                        ContinuationData nested = continuationDataFromCommandList(
                                commandRoot.path("commandExecutorCommand").path("commands")
                        );
                        if (nested != null) {
                                return nested;
                        }
                }

                return null;
        }

        /**
         * Extracts the continuation token for a channel sort option such as
         * {@code Popular} or {@code Oldest}.
         */
        private ContinuationData getSortContinuationData(JsonNode data, ChannelSort sortBy) {
                String expectedLabel = switch (sortBy) {
                        case POPULAR -> "Popular";
                        case OLDEST -> "Oldest";
                        default -> throw new IllegalStateException("Unexpected value: " + sortBy);
                };

                ContinuationData fromChipViewModel = findSortContinuationInChipViewModels(data, expectedLabel);
                if (fromChipViewModel != null) {
                        return fromChipViewModel;
                }

                ContinuationData fromListItemViewModel = findSortContinuationInListItemViewModels(data, expectedLabel);
                if (fromListItemViewModel != null) {
                        return fromListItemViewModel;
                }

                throw new IllegalStateException("Could not resolve continuation data for sort option with label: " + expectedLabel);
        }

        private record SelectorItemMatch(String selectorItem, List<JsonNode> items) {
                private static SelectorItemMatch empty() {
                        return new SelectorItemMatch(null, List.of());
                }
        }
}