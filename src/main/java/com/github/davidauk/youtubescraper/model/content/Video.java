package com.github.davidauk.youtubescraper.model.content;

import com.github.davidauk.youtubescraper.model.Thumbnail;

import java.time.Instant;
import java.util.List;

public class Video extends Content {

    private final String description;
    private final Instant publishedAt;
    private final Integer durationSeconds;
    private final VideoType videoType;
    private final ContentAvailability contentAvailability;

    public Video(
            String id,
            String title,
            String description,
            Instant publishedAt,
            Integer durationSeconds,
            List<Thumbnail> thumbnailOptions,
            ContentAvailability contentAvailability,
            VideoType videoType
    ) {
        super(id, title, thumbnailOptions);
        this.description = description;
        this.publishedAt = publishedAt;
        this.durationSeconds = durationSeconds;
        this.videoType = videoType;
        this.contentAvailability = contentAvailability;
    }

    public VideoType getVideoType() {
        return videoType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public ContentAvailability getContentAvailability() {
        return contentAvailability;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", description='" + description + '\'' +
                ", publishedAt=" + publishedAt +
                ", durationSeconds='" + durationSeconds + '\'' +
                ", videoType=" + videoType +
                ", contentAvailability=" + contentAvailability;
    }
}
