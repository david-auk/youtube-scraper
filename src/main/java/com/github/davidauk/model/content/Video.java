package com.github.davidauk.model.content;

import com.github.davidauk.model.Thumbnail;

import java.sql.Timestamp;
import java.util.List;

public class Video extends Content {

    private final String description;
    private final Timestamp publishedAt;
    private final String duration;
    private final boolean membersOnly;
    private final boolean verified;

    public Video(
            String id,
            String title,
            String description,
            Timestamp publishedTime,
            String duration,
            List<Thumbnail> thumbnailOptions,
            boolean membersOnly,
            boolean verified
    ) {
        super(id, title, thumbnailOptions);
        this.description = description;
        this.publishedAt = publishedTime;
        this.duration = duration;
        this.membersOnly = membersOnly;
        this.verified = verified;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public String getDuration() {
        return duration;
    }

    public boolean isMembersOnly() {
        return membersOnly;
    }

    public boolean isVerified() {
        return verified;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", description='" + description + '\'' +
                ", publishedAt=" + publishedAt +
                ", duration='" + duration + '\'' +
                ", membersOnly=" + membersOnly +
                ", verified=" + verified;
    }
}
