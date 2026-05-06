package com.github.davidauk.youtubescraper;

import com.github.davidauk.youtubescraper.client.YoutubeClient;
import com.github.davidauk.youtubescraper.model.ChannelOverviewResponse;
import com.github.davidauk.youtubescraper.model.ChannelRequest;
import com.github.davidauk.youtubescraper.model.ChannelSort;
import com.github.davidauk.youtubescraper.model.content.ContentType;

import java.time.Duration;

public final class Main {

        public static void main(String[] args) throws Exception {
                YoutubeClient client = new YoutubeClient();

                ChannelOverviewResponse result =
                        client.getChannel("UCNz5474yx24nxVygk6kunLQ", new ChannelRequest(
                                5,
                                Duration.ofSeconds(1),
                                null,
                                ChannelSort.POPULAR,
                                ContentType.VIDEOS
                        ));

                System.out.println(result);
        }
}