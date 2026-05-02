package com.github.davidauk;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.*;
import com.github.davidauk.model.content.ContentAvailability;
import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.content.PartialVideo;
import com.github.davidauk.model.content.Video;
import com.github.davidauk.model.ChannelOverviewResponse;
import com.github.davidauk.node.VideoPrimaryInfoRendererNode;
import com.github.davidauk.node.WatchPageNode;

import java.time.Duration;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        YoutubeClient client = new YoutubeClient();
    }
}