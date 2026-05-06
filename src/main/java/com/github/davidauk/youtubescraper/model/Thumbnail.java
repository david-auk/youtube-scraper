package com.github.davidauk.youtubescraper.model;

import java.net.URI;

public record Thumbnail(
    Integer width_px,
    Integer height_px,
    URI thumbnail_url
) {}
