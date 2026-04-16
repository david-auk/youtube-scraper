# YouTube Scraper (Java)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.david-auk/youtube-scraper?logo=apachemaven&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.david-auk/youtube-scraper)
[![GitHub License](https://img.shields.io/github/license/david-auk/youtube-scraper)](https://github.com/david-auk/youtube-scraper/blob/main/LICENSE)

A lightweight, dependency-minimal YouTube scraper written in Java.

This project allows you to retrieve:
- Channel videos
- Playlist videos
- Individual video metadata

It is designed to be:
- Plug and play
- API independent

Heavily inspired by the python package [Scrapetube](https://github.com/dermasmid/scrapetube), but rewritten in Java for better typing & Java integration

---

## Features

- Fetch videos from a channel (videos, shorts, streams)
- Fetch videos from a playlist
- Fetch metadata for a single video
- Resolve channel IDs from public usernames/handles (e.g. `@username`)
- Support for channel sorting (NEWEST, POPULAR, OLDEST)
- Clean mapping to a simple `Video` object
- Raw JSON access for advanced/custom use cases

---
## [Install the Maven package](https://central.sonatype.com/artifact/io.github.david-auk/youtube-scraper)


## Basic Usage

### 1. Create client

```java
YoutubeClient client = new YoutubeClient();
```

---

### 2. Fetch channel videos

```java
// The third argument can be a channel ID, username, or handle (e.g. "@LinusTechTips")
List<Video> videos = client.getChannel("LinusTechTips", new ChannelRequest(
        5,
        Duration.ofSeconds(1),
        null,
        ChannelSort.NEWEST, // also supports POPULAR and OLDEST
        ContentType.VIDEOS
));
```

---

### Resolving a channel ID from a username

```java
String channelId = client.resolveChannelIdFromUsername("@LinusTechTips");
System.out.println(channelId);
```

This is useful if you want to normalize all requests to a canonical channel ID (`UC...`).

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
List<JsonNode> raw = client.getChannelRaw(
        "LinusTechTips",
        request
);
```

This is useful when YouTube changes structure, or you need additional fields.

---

## ⚠️ Disclaimer ⚠️

This project relies on YouTube's internal APIs and page structure.

That means:
- It may break if YouTube changes their implementation
- It is not an official API

Use at your own risk.

---

## Future Plans

- Add typed models (optional DTO layer)
- Further improve resilience against YouTube changes
- Add caching support
- Add rate limiting utilities
- Possibly support search (currently intentionally excluded)

---

## Contributing

Feel free to open issues or contribute improvements.