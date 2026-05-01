package com.github.davidauk;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.mapper.VideoMapper;
import com.github.davidauk.model.*;
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

//        Video video = client.getVideo("393kWVEMixM");
//
//        System.out.println(video);

        ChannelOverviewResponse channelOverviewResponse = client.getChannel("CaliPlanes", new ChannelRequest(
                2,
                Duration.ofSeconds(1),
                null,
                ChannelSort.NEWEST,
                ContentType.STREAMS
        ));

        System.out.println("Channel results for : " + channelOverviewResponse);

        for (PartialVideo video : channelOverviewResponse.videos()) {
            System.out.println(video);
        }
    }
}