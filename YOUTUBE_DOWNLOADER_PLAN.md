# YouTube Downloader App - Development Plan

## Overview
A minimal YouTube downloader app inspired by Seal, built with Jetpack Compose and yt-dlp (via youtubedl-android library) without a backend.

---

## Architecture

### Tech Stack
| Component | Technology |
|-----------|------------|
| UI Framework | Jetpack Compose (Material Design 3) |
| Download Engine | youtubedl-android (yt-dlp wrapper) |
| Networking | Built-in (via yt-dlp) |
| Storage | Android Storage Access Framework (SAF) |
| State Management | ViewModel + StateFlow |
| DI (Optional) | Hilt |
| Async Operations | Kotlin Coroutines |

### Core Dependencies
```kotlin
// youtubedl-android - yt-dlp wrapper
implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
implementation("io.github.junkfood02.youtubedl-android:aria2c:0.18.1") // optional for faster downloads

// Jetpack Compose BOM
implementation(platform("androidx.compose:compose-bom:2025.02.00"))

// ViewModel & Coroutines
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

// Material Design 3
implementation("androidx.compose.material3:material3")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.0")

// Coil for image loading (thumbnails)
implementation("io.coil-kt:coil-compose:2.6.0")
```

---

## Project Structure

```
app/src/main/java/com/spyou/youtubedownload/
├── MainActivity.kt
├── YouTubeDownloaderApp.kt (Application class)
├── di/
│   └── AppModule.kt (if using Hilt)
├── data/
│   ├── repository/
│   │   └── DownloadRepository.kt
│   ├── model/
│   │   ├── VideoInfo.kt
│   │   ├── DownloadFormat.kt
│   │   ├── DownloadProgress.kt
│   │   └── DownloadTask.kt
│   └── local/
│       ├── database/
│       │   ├── DownloadDatabase.kt
│       │   ├── DownloadDao.kt
│       │   └── DownloadEntity.kt
│       └── preferences/
│           └── SettingsDataStore.kt
├── domain/
│   ├── usecase/
│   │   ├── GetVideoInfoUseCase.kt
│   │   ├── DownloadVideoUseCase.kt
│   │   ├── CancelDownloadUseCase.kt
│   │   └── GetDownloadsUseCase.kt
│   └── mapper/
│       └── VideoInfoMapper.kt
├── service/
│   └── DownloadService.kt (Foreground service for downloads)
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/
│   │   ├── common/
│   │   │   ├── LoadingIndicator.kt
│   │   │   ├── ErrorMessage.kt
│   │   │   └── EmptyState.kt
│   │   ├── VideoCard.kt
│   │   ├── FormatSelector.kt
│   │   ├── ProgressBar.kt
│   │   └── DownloadItem.kt
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── download/
│   │   │   ├── DownloadScreen.kt
│   │   │   └── DownloadViewModel.kt
│   │   ├── format/
│   │   │   ├── FormatScreen.kt
│   │   │   └── FormatViewModel.kt
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt
│   │   │   └── SettingsViewModel.kt
│   │   └── downloads/
│   │       ├── DownloadsScreen.kt
│   │       └── DownloadsViewModel.kt
│   └── navigation/
│       ├── NavGraph.kt
│       └── Screen.kt
└── util/
    ├── Extensions.kt
    ├── NotificationHelper.kt
    └── StorageHelper.kt
```

---

## Core Features

### Phase 1: MVP (Minimum Viable Product)
1. **URL Input & Video Info Fetching**
   - Paste YouTube URL
   - Fetch video title, thumbnail, duration, author
   - Show loading state

2. **Format Selection**
   - Video formats (MP4, WebM with quality options)
   - Audio formats (MP3, M4A, OPUS)
   - Best quality / Custom quality selection

3. **Download with Progress**
   - Download to Downloads folder
   - Real-time progress tracking
   - Cancel download
   - Notification with progress

4. **Download History**
   - List of completed downloads
   - Open downloaded file
   - Delete download

### Phase 2: Enhanced Features
1. **Playlist Support**
   - Detect playlist URLs
   - Select videos from playlist
   - Batch download

2. **Audio Extraction**
   - Extract audio only (MP3, M4A)
   - Embed thumbnail & metadata

3. **Custom Commands**
   - Advanced users can input custom yt-dlp options
   - Save command templates

4. **Settings**
   - Default download location
   - Default format preferences
   - Update yt-dlp binary
   - Theme (Light/Dark/System)

### Phase 3: Polish Features
1. **Subtitle Download**
   - Auto-embed subtitles
   - Language selection

2. **SponsorBlock Integration**
   - Skip sponsor segments

3. **Quick Share**
   - Share to app from YouTube

---

## UI Design

### Screens

#### 1. Home Screen (Main)
```
┌─────────────────────────────────────┐
│  ┌───────────────────────────────┐  │
│  │ 🔗 Paste YouTube URL          │  │
│  └───────────────────────────────┘  │
│           [Download Button]         │
│                                     │
│  Recent Downloads                   │
│  ┌───────────────────────────────┐  │
│  │ 🎬 Video Title          100% │  │
│  │ Author • Duration    ✓       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ 🎬 Another Video         45% │  │
│  │ Author • Duration    ⏸       │  │
│  └───────────────────────────────┘  │
│                                     │
│  [Bottom Navigation]                │
│  🏠    📥    ⚙️                      │
│ Home Downloads Settings             │
└─────────────────────────────────────┘
```

#### 2. Format Selection Screen
```
┌─────────────────────────────────────┐
│  ← Select Format              ✓     │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │      [Video Thumbnail]        │  │
│  │                               │  │
│  └───────────────────────────────┘  │
│  Video Title                        │
│  Author • 10:24                     │
├─────────────────────────────────────┤
│  💾 Video                           │
│  ○ 1080p (MP4)              25 MB   │
│  ● 720p (MP4)               15 MB   │
│  ○ 480p (MP4)                8 MB   │
├─────────────────────────────────────┤
│  🎵 Audio Only                      │
│  ○ MP3 128kbps               5 MB   │
│  ○ M4A 128kbps               5 MB   │
│  ○ OPUS 128kbps              4 MB   │
├─────────────────────────────────────┤
│  [      Start Download      ]       │
└─────────────────────────────────────┘
```

#### 3. Downloads Screen
```
┌─────────────────────────────────────┐
│  Downloads                    🗑     │
├─────────────────────────────────────┤
│  ACTIVE                             │
│  ┌───────────────────────────────┐  │
│  │ 🎬 Video Title                │  │
│  │ ████████░░░░░░░░░░░░  45%     │  │
│  │ 2.5 MB/s • 1:23 remaining  ⏸ │  │
│  └───────────────────────────────┘  │
│                                     │
│  COMPLETED                          │
│  ┌───────────────────────────────┐  │
│  │ 🎬 Completed Video       ✓   │  │
│  │ Author • 10:24           📁 │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

---

## Implementation Plan

### Week 1: Foundation
- [ ] Set up project structure
- [ ] Integrate youtubedl-android library
- [ ] Initialize yt-dlp in Application class
- [ ] Create Room database for downloads
- [ ] Set up basic navigation

### Week 2: Core Download Functionality
- [ ] Implement video info fetching (GetVideoInfoUseCase)
- [ ] Create format selection UI
- [ ] Implement download with progress
- [ ] Create foreground service for downloads
- [ ] Add notification support

### Week 3: UI Polish & History
- [ ] Build Home screen
- [ ] Build Downloads screen
- [ ] Implement download history with Room
- [ ] Add file open/delete actions
- [ ] Error handling & loading states

### Week 4: Settings & Enhancements
- [ ] Settings screen with DataStore
- [ ] Audio extraction mode
- [ ] Default preferences
- [ ] yt-dlp update functionality
- [ ] Theme switching

### Week 5: Advanced Features
- [ ] Playlist support
- [ ] Custom command templates
- [ ] Subtitle download
- [ ] Quick share functionality

---

## Key Implementation Details

### 1. Initialize yt-dlp (in Application class)
```kotlin
class YouTubeDownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "Failed to initialize yt-dlp", e)
        }
    }
}
```

### 2. Fetch Video Info
```kotlin
class GetVideoInfoUseCase @Inject constructor() {
    suspend operator fun invoke(url: String): Result<VideoInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val info = YoutubeDL.getInstance().getInfo(url)
                Result.success(info.toDomainModel())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
```

### 3. Download with Progress
```kotlin
class DownloadVideoUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        format: DownloadFormat,
        outputDir: File,
        onProgress: (DownloadProgress) -> Unit
    ): Flow<DownloadResult> = callbackFlow {
        val request = YoutubeDLRequest(url).apply {
            addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")
            when (format) {
                is DownloadFormat.Video -> {
                    addOption("-f", "bestvideo[height<=${format.quality}]+bestaudio/best")
                }
                is DownloadFormat.Audio -> {
                    addOption("-x", "--audio-format", format.codec)
                    addOption("--embed-thumbnail")
                }
            }
        }
        
        YoutubeDL.getInstance().execute(request, processId) { progress, eta ->
            onProgress(DownloadProgress(progress, eta))
        }
        
        awaitClose { 
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
    }
}
```

### 4. Storage Permissions (Android 10+)
```kotlin
// AndroidManifest.xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

// For Android 10 (API 29)
android:requestLegacyExternalStorage="true"

// For Android 11+ (API 30+)
// Use Downloads/ or Documents/ folder only
val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
```

---

## Configuration Requirements

### build.gradle.kts (Module: app)
```kotlin
android {
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        
        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

### AndroidManifest.xml
```xml
<application
    android:name=".YouTubeDownloaderApp"
    android:extractNativeLibs="true"
    android:requestLegacyExternalStorage="true"
    ...>
    
    <service
        android:name=".service.DownloadService"
        android:foregroundServiceType="dataSync"
        android:exported="false" />
        
</application>
```

---

## Testing Strategy

1. **Unit Tests**: ViewModels, UseCases, Repository
2. **Integration Tests**: Database operations, yt-dlp commands
3. **UI Tests**: Compose screen navigation and interactions
4. **Manual Testing**: 
   - Different video formats (1080p, 4K, Shorts)
   - Audio extraction
   - Playlist downloads
   - Network interruption handling

---

## Future Enhancements

1. **Multi-platform support** (more than just YouTube)
2. **Download scheduler** (download later)
3. **Auto-download subscriptions**
4. **In-app video player**
5. **Chromecast support**
6. **Statistics & analytics**

---

## Resources

- **youtubedl-android**: https://github.com/JunkFood02/youtubedl-android
- **Seal (Reference App)**: https://github.com/JunkFood02/Seal
- **yt-dlp Documentation**: https://github.com/yt-dlp/yt-dlp
- **Material Design 3**: https://m3.material.io/

---

## Notes

- Ensure yt-dlp binary is kept up-to-date for YouTube compatibility
- Handle storage permission changes for Android 10+
- Use foreground service for reliable downloads
- Implement proper error handling for network issues
- Consider using aria2c for faster parallel downloads
