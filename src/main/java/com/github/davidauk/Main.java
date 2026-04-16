package com.github.davidauk;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.*;

import java.time.Duration;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        YoutubeClient client = new YoutubeClient();

        List<Video> videos = client.getChannel("LinusTechTips", new ChannelRequest(
                5,
                Duration.ofSeconds(1),
                null,
                ChannelSort.NEWEST, // also supports POPULAR and OLDEST
                ContentType.STREAMS
        ));

        System.out.println("Channel results:");

        for (Video video : videos) {
                System.out.println(video.title());
        }

//        List<JsonNode> playlistVideos = client.getPlaylistRaw(new PlaylistRequest(
//                "PL8mG-RkN2uTw7PhlnAr4pZZz2QubIbujH",
//                5,
//                Duration.ofSeconds(1),
//                null
//        ));
//
//        System.out.println("\nPlaylist results:");
//        for (JsonNode video : playlistVideos) {
//            System.out.println(video);
//        }
//
//        JsonNode videoInfo = client.getVideo("dQw4w9WgXcQ");
//        System.out.println("\nSingle video:");
//        System.out.println(videoInfo);
    }
}