package com.github.davidauk;

import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.*;
import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.content.Video;

import java.time.Duration;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        YoutubeClient client = new YoutubeClient();

        List<Video> videos = client.getChannel("lekkerspelen", new ChannelRequest(
                null,
                Duration.ofSeconds(1),
                null,
                ChannelSort.NEWEST,
                ContentType.STREAMS
        ));

        System.out.println("Channel results:");

        for (Video video : videos) {
                System.out.println(video);
        }
    }
}