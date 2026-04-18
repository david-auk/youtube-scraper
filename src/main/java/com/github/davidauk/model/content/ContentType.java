package com.github.davidauk.model.content;

public enum ContentType {
    VIDEOS("videos", "videoRenderer"),
    SHORTS("shorts", "reelWatchEndpoint"),
    STREAMS("streams", "videoRenderer");

    private final String pathSegment;
    private final String selectorItem;

    ContentType(String pathSegment, String selectorItem) {
        this.pathSegment = pathSegment;
        this.selectorItem = selectorItem;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public String selectorItem() {
        return selectorItem;
    }
}