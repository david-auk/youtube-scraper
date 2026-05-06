package com.github.davidauk.youtubescraper.model.content;

import com.github.davidauk.youtubescraper.model.Thumbnail;

import java.util.List;

public class Short extends Content {

    public Short(String id, String title, List<Thumbnail> thumbnailOptions) {
        super(id, title, thumbnailOptions);
    }
}
