package com.github.davidauk.model;

public record Channel(String channelId) {
    public String baseUrl() {
        return "https://www.youtube.com/channel/" + channelId;
    }
}
