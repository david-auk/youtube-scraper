package com.github.davidauk;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.ChannelRequest;
import com.github.davidauk.model.ChannelSort;
import com.github.davidauk.model.ContentType;
import com.github.davidauk.model.Video;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        YoutubeClient client = new YoutubeClient();

        List<Video> channelVideos = client.getChannel(new ChannelRequest(
                null,
                null,
                "lekkerspelen",
                5,
                Duration.ofSeconds(1),
                null,
                ChannelSort.NEWEST,
                ContentType.VIDEOS
        ));

        System.out.println("Channel results:");

        LinkedList<Video> memberOnlyVideos = new LinkedList<>();

        for (Video video : channelVideos) {
            if (video.membersOnly()) {
                System.out.println(video.title());
                memberOnlyVideos.add(video);
            }
        }

        System.out.println("Members only results:" + memberOnlyVideos.size() + "\n\n");


//        List<JsonNode> playlistVideos = client.getPlaylist(new PlaylistRequest(
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

        JsonNode videoInfo = client.getVideo("dQw4w9WgXcQ");
        System.out.println("\nSingle video:");
        System.out.println(videoInfo);
    }
}