package com.github.davidauk.youtubescraper;

import com.github.davidauk.youtubescraper.client.YoutubeClient;
import com.github.davidauk.youtubescraper.model.ChannelOverviewResponse;
import com.github.davidauk.youtubescraper.model.ChannelRequest;
import com.github.davidauk.youtubescraper.model.ChannelSort;
import com.github.davidauk.youtubescraper.model.content.ContentType;
import com.github.davidauk.youtubescraper.model.content.PartialVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class Main {

        private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

        public static void main(String[] args) throws Exception {
                YoutubeClient client = new YoutubeClient(LOGGER);

                ChannelOverviewResponse result =
                        client.getChannel("UCNz5474yx24nxVygk6kunLQ", new ChannelRequest(
                                5,
                                Duration.ofSeconds(1),
                                null,
                                ChannelSort.POPULAR,
                                ContentType.VIDEOS
                        ));

                for (PartialVideo video : result.videos()) {
                        video.getVideo(client);

                }

                System.out.println(result);
        }
}