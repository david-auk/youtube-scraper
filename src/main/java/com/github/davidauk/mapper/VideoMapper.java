package com.github.davidauk.mapper;

import com.github.davidauk.model.Thumbnail;
import com.github.davidauk.model.content.ContentAvailability;
import com.github.davidauk.model.content.Video;
import com.github.davidauk.model.content.VideoType;
import com.github.davidauk.node.*;

import java.time.Instant;
import java.util.List;

public final class VideoMapper {

    private VideoMapper() {
    }

    public static Video toVideo(WatchPageNode watchPage) {
        PlayerResponseNode player = watchPage.player();
        ContentAvailability contentAvailability = player.playabilityStatus().contentAvailability(watchPage.primaryInfo());

        // Define the attributes we'll need to build the Video object.

        String id = null;
        String title = null;
        String description = null;
        Instant publishedAt = null;
        Integer durationSeconds = null;
        List<Thumbnail> thumbnailOptions = List.of();
        VideoType videoType = null;

        if (isAvailable(contentAvailability)) {

            // Get the nodes we need to build the Video object. (we can only do this if the video is available).

            PlayerVideoDetailsNode details = player.videoDetails();
            PlayerMicroformatNode microformat = player.microformat();

            // Populate the attributes with the values from the nodes.

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

    static private boolean isAvailable(ContentAvailability contentAvailability) {
        return contentAvailability != ContentAvailability.PRIVATE && contentAvailability != ContentAvailability.DELETED;
    }
}