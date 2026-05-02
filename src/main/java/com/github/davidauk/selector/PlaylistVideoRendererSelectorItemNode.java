//package com.github.davidauk.selector;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.github.davidauk.mapper.PartialVideoMapper;
//import com.github.davidauk.model.content.ContentType;
//import com.github.davidauk.model.content.PartialVideo;
//import com.github.davidauk.model.content.Video;
//import com.github.davidauk.node.PartialVideoRendererNode;
//
//public record PlaylistVideoRendererSelectorItemNode(JsonNode raw) implements SelectorItemNode {
//    @Override
//    public PartialVideo toPartialVideo(ContentType contentType) {
//        return PartialVideoMapper.toPartialVideo(new PartialVideoRendererNode(raw), contentType);
//    }
//
//    @Override
//    public Video toVideo() {
//        throw new UnsupportedOperationException("playlistVideoRenderer does not contain enough data for a full Video.");
//    }
//}