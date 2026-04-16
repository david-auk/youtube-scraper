

# YouTube Scraper (Java)

A lightweight, dependency-minimal YouTube scraper written in Java.

This project allows you to retrieve:
- Channel videos
- Playlist videos
- Individual video metadata

It is designed to be:
- DRY
- Strongly typed where it matters
- Easy to integrate into existing Java backends

---

## Features

- Fetch videos from a channel (videos, shorts, streams)
- Fetch videos from a playlist
- Fetch metadata for a single video
- Clean mapping to a simple `Video` object
- Raw JSON access for advanced/custom use cases

---

## Installation (Soon)

I'm currently working on publishing this package to **Maven Central**, so it can be easily included in other Java projects.

Once available, you will be able to add it like:

```gradle
implementation("com.example:youtube-scraper:VERSION")
```

---

## Basic Usage

### 1. Create client

```java
YoutubeClient client = new YoutubeClient();
```

---

### 2. Fetch channel videos

```java
List<Video> videos = client.getChannel(new ChannelRequest(
        null,
        null,
        "LinusTechTips",
        5,
        Duration.ofSeconds(1),
        null,
        ChannelSort.NEWEST,
        ContentType.VIDEOS
));

for (Video video : videos) {
    System.out.println(video.title());
}
```

---

### 3. Fetch playlist videos

```java
List<YoutubeClient.Video> videos = client.getPlaylistVideos(new PlaylistRequest(
        "PLAYLIST_ID",
        5,
        Duration.ofSeconds(1),
        null
));
```

---

### 4. Fetch a single video

```java
JsonNode video = client.getVideo("VIDEO_ID");
System.out.println(video);
```

---

## Video Model

The scraper maps YouTube responses into a simplified `Video` object:

```java
public record Video(
    String id,
    String title,
    String description,
    String publishedTime,
    String duration,
    String thumbnailUrl,
    boolean membersOnly,
    boolean verified
) {}
```

This keeps the API clean while still allowing access to raw data when needed.

---

## Advanced Usage

If you need full flexibility, you can still use the raw methods:

```java
List<JsonNode> raw = client.getChannelRaw(request);
```

This is useful when YouTube changes structure or you need additional fields.

---

## ⚠️ Disclaimer ⚠️

This project relies on YouTube's internal APIs and page structure.

That means:
- It may break if YouTube changes their implementation
- It is not an official API

Use at your own risk.

---

## Future Plans

- Publish to Maven Central
- Add typed models (optional DTO layer)
- Improve resilience against YouTube changes
- Add caching support
- Add rate limiting utilities
- Possibly support search (currently intentionally excluded)

---

## Contributing

Feel free to open issues or contribute improvements.