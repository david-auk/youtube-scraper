package com.github.davidauk.youtubescraper.selector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.Thumbnail;
import com.github.davidauk.youtubescraper.model.content.ContentAvailability;
import com.github.davidauk.youtubescraper.model.content.Video;
import com.github.davidauk.youtubescraper.model.content.VideoType;
import com.github.davidauk.youtubescraper.node.PlayerMicroformatNode;
import com.github.davidauk.youtubescraper.node.PlayerResponseNode;
import com.github.davidauk.youtubescraper.node.PlayerVideoDetailsNode;
import com.github.davidauk.youtubescraper.node.WatchPageNode;

import java.time.Instant;
import java.util.List;

public record WatchPageSelectorItemNode(WatchPageNode watchPage) implements VideoSelectorItemNode {
        @Override
        public JsonNode raw() {
                return watchPage.initialData();
        }

        @Override
        public Video toVideo() {

                PlayerResponseNode player = watchPage.player();
                ContentAvailability contentAvailability =
                        player.playabilityStatus().contentAvailability(watchPage.primaryInfo());

                String id = null;
                String title = null;
                String description = null;
                Instant publishedAt = null;
                Integer durationSeconds = null;
                List<Thumbnail> thumbnailOptions = List.of();
                VideoType videoType = null;

                if (isAvailable(contentAvailability)) {
                        PlayerVideoDetailsNode details = player.videoDetails();
                        PlayerMicroformatNode microformat = player.microformat();

                        id = details.id();
                        title = details.title();
                        description = watchPage.secondaryInfo().description();
                        publishedAt = microformat.publishedAt();
                        durationSeconds = details.durationSeconds();
                        thumbnailOptions = details.thumbnails();
                        videoType = details.videoType(microformat);
                }

                return new Video(
                        id,
                        title,
                        description,
                        publishedAt,
                        durationSeconds,
                        thumbnailOptions,
                        contentAvailability,
                        videoType
                );
        }

        private boolean isAvailable(ContentAvailability contentAvailability) {
                return contentAvailability != ContentAvailability.PRIVATE
                        && contentAvailability != ContentAvailability.DELETED;
        }
}