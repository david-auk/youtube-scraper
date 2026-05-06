package com.github.davidauk.youtubescraper.selector;

import com.github.davidauk.youtubescraper.model.content.Video;

public interface VideoSelectorItemNode extends SelectorItemNode {
    Video toVideo();
}
