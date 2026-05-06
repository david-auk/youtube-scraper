package com.github.davidauk.youtubescraper.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.content.PartialVideo;

import java.util.List;

public record ChannelOverviewResponse(Channel channel, boolean isVerified, List<PartialVideo> videos,
                                      List<JsonNode> rawValues) {
}
