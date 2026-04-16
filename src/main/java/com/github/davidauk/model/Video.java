package com.github.davidauk.model;

public record Video(
        String id,
        String title,
        String description,
        String publishedTime,
        String duration,
        String thumbnailUrl,
        boolean membersOnly,
        boolean verified
) {
}
