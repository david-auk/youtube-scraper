package com.github.davidauk.youtubescraper.selector;

import com.github.davidauk.youtubescraper.model.content.ContentType;
import com.github.davidauk.youtubescraper.model.content.PartialVideo;

public interface PartialVideoSelectorItemNode extends SelectorItemNode {
    PartialVideo toPartialVideo(ContentType contentType);
}