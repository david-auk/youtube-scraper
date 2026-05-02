package com.github.davidauk.model.content;

import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.Thumbnail;

import java.io.IOException;
import java.util.List;

public record PartialVideo(
        String id,
        String title,
        Integer durationSeconds,
        List<Thumbnail> thumbnailOptions,
        ContentAvailability contentAvailability,
        VideoType videoType
//        boolean membersOnly,
//        boolean verified
        // Add any other attributes available in the overview batch
) {
    public Video getVideo(YoutubeClient youtubeClient) throws IOException, InterruptedException {
        return youtubeClient.getVideo(this);
    }
}