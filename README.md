# Eclipse

A clean, modern YouTube video and audio downloader for Android. Built with Kotlin and Jetpack Compose.

## Features

- **Video downloads** in multiple resolutions (360p, 480p, 720p, 1080p, 1440p, 4K, 8K)
- **Audio extraction** with support for MP3, M4A, OPUS, WAV, and FLAC
- **Background downloads** with progress notifications via WorkManager
- **Download queue** with concurrent download management
- **Share intent support** -- share YouTube links directly to Eclipse from any app
- **Download history** with playback and retry support
- **Playlist support** -- download entire playlists at once
- **Scheduled downloads** -- queue downloads for later
- **Cookie support** for age-restricted or region-locked content
- **aria2c integration** for faster multi-connection downloads
- **Material You** theming with dynamic colors and dark mode

## Tech Stack

- Kotlin
- Jetpack Compose with Material 3
- Room Database
- WorkManager
- Jetpack Navigation
- Coil for image loading
- DataStore Preferences

## Requirements

- Android 7.0 (API 24) or higher

## Build

```
git clone https://github.com/your-username/eclipse.git
cd eclipse
./gradlew assembleRelease
```

Release APKs will be in `app/build/outputs/apk/release/`.

## Acknowledgements

This project is built on top of the following open-source projects:

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) -- the core download engine that powers all video and audio extraction
- [youtubedl-android](https://github.com/yausername/youtubedl-android) -- Android bindings for yt-dlp
- [aria2](https://github.com/aria2/aria2) -- multi-connection download accelerator

## Disclaimer

This application is intended for personal use only. Users are responsible for complying with YouTube's Terms of Service and applicable copyright laws. The developers do not condone or encourage the unauthorized downloading of copyrighted material.

## License

This project is for personal/educational use.
