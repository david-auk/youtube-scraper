package com.github.davidauk.youtubescraper.model.content;

import java.util.List;

public enum ContentType {
    VIDEOS("videos", List.of("videoRenderer", "lockupViewModel")),
//    SHORTS("shorts", "shortsLockupViewModel", null),
    STREAMS("streams", List.of("videoRenderer", "lockupViewModel"));

    private final String pathSegment;
    private final List<String> selectorItems;

    ContentType(String pathSegment, List<String> selectorItems) {
        this.pathSegment = pathSegment;
        this.selectorItems = selectorItems;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public List<String> selectorItems() {
        return selectorItems;
    }
}