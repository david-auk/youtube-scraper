package com.github.davidauk.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.content.PartialVideo;

import java.util.List;

public record ChannelOverviewResponse(Channel channel, boolean isVerified, List<PartialVideo> videos, List<JsonNode> rawValues) {}
