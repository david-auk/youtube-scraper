package com.github.davidauk.selector;

import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.content.PartialVideo;
import com.github.davidauk.model.content.Video;

public interface VideoSelectorItemNode extends SelectorItemNode {
    Video toVideo();
}
