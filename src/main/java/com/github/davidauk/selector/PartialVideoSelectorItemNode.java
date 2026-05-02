package com.github.davidauk.selector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.content.PartialVideo;

public interface PartialVideoSelectorItemNode extends SelectorItemNode {
    PartialVideo toPartialVideo(ContentType contentType);
}