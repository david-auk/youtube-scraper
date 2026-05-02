package com.github.davidauk.selector;

import com.fasterxml.jackson.databind.JsonNode;

public final class SelectorItemNodeFactory {
    private SelectorItemNodeFactory() {
    }

    public static SelectorItemNode from(String selectorItem, JsonNode node) {
        return switch (selectorItem) {
            case "videoRenderer" -> new VideoRendererSelectorItemNode(node);
            case "lockupViewModel" -> new LockupViewModelSelectorItemNode(node);
//            case "playlistVideoRenderer" -> new PlaylistVideoRendererSelectorItemNode(node);
            default -> throw new IllegalArgumentException("Unsupported selector item: " + selectorItem);
        };
    }
}