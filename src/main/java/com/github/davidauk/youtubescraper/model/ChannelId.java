package com.github.davidauk.youtubescraper.model;

public record ChannelId(String channelId) {
        public String baseUrl() {
                return "https://www.youtube.com/channel/" + channelId;
        }
}
