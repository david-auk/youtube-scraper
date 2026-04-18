package com.github.davidauk.model.content;

import com.github.davidauk.model.Thumbnail;

import java.util.List;

public abstract class Content {

    private final String id;
    private final String title;
    private final List<Thumbnail> thumbnailOptions;

    public Content(String id, String title, List<Thumbnail> thumbnailOptions) {
        this.id = id;
        this.title = title;
        this.thumbnailOptions = thumbnailOptions;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Thumbnail> getThumbnailOptions() {
        return thumbnailOptions;
    }

    @Override
    public String toString() {
        return "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", thumbnailOptions=" + thumbnailOptions +
                '}';
    }
}
