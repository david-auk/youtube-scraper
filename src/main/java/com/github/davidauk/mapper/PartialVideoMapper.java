package com.github.davidauk.mapper;

import com.github.davidauk.model.content.ContentAvailability;
import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.content.PartialVideo;
import com.github.davidauk.model.content.VideoType;
import com.github.davidauk.node.PartialVideoRendererNode;

public final class PartialVideoMapper {

    private PartialVideoMapper() {
    }

    public static PartialVideo toPartialVideo(PartialVideoRendererNode node, ContentType contentType) {
        if (!node.hasEnoughData()) {
            return null;
        }

        System.out.println(node);

        ContentAvailability contentAvailability =
                node.membersOnly() ? ContentAvailability.MEMBERS_ONLY : ContentAvailability.PUBLIC;



        return new PartialVideo(
                node.id(),
                node.title(),
                node.descriptionSnippet(),
                node.durationSeconds(),
                node.thumbnails(),
                contentAvailability,
                detectVideoType(node, contentType)
        );
    }

    private static VideoType detectVideoType(PartialVideoRendererNode node, ContentType contentType) {

        if (contentType == ContentType.VIDEOS) {
            return VideoType.UPLOADED_VIDEO;
        } else if (contentType == ContentType.STREAMS) {
            String value = node.toString();

            if (value.contains("\"style\":\"LIVE\"")
                    || value.contains("\"iconType\":\"LIVE\"")
                    || value.contains(" watching")) {
                return VideoType.CURRENTLY_LIVE_STREAM;
            }

            return VideoType.PAST_LIVE_STREAM;
        }

        return null;
    }
}