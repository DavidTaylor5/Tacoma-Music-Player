# Tacoma Music Player — Developer Guide

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Module & Package Structure](#module--package-structure)
4. [Architecture](#architecture)
5. [Database Layer](#database-layer)
6. [Repository Layer](#repository-layer)
7. [ViewModels](#viewmodels)
8. [Navigation & Screens](#navigation--screens)
9. [Media Playback (MusicService)](#media-playback-musicservice)
10. [Key Features](#key-features)
11. [Dependency Injection](#dependency-injection)
12. [Preferences & Persistence](#preferences--persistence)
13. [Android Auto](#android-auto)
14. [Background Work](#background-work)
15. [Adding a New Feature — Checklist](#adding-a-new-feature--checklist)

---

## Project Overview

Tacoma Music Player is a local-library Android music player. It scans device storage via MediaStore, catalogs songs and albums into a Room database, and plays audio through Media3/ExoPlayer. The app supports Android Auto, playlist management, custom album art, and queue persistence across restarts.

**Build info:**
- Min SDK: 30 (Android 11)
- Target/Compile SDK: 36
- Kotlin: 2.1.0
- Version: v2.0.1 (versionCode: 201)

---

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin 2.x |
| UI | Views + Fragments (ViewBinding) |
| Architecture | MVVM + Repository |
| Async | Coroutines + StateFlow / LiveData |
| DI | Dagger Hilt |
| Database | Room |
| Media | Media3 (ExoPlayer + MediaSession) |
| Background work | WorkManager |
| Preferences | DataStore |
| Image loading | Coil |
| Image cropping | uCrop |
| Logging | Timber |
| Build | Gradle Kotlin DSL + Version Catalogs |

> **Note:** The CLAUDE.md references Jetpack Compose, but the current UI is Views + Fragments. New UI work should match the existing fragment/ViewBinding pattern unless a deliberate migration is underway.

---

## Module & Package Structure

The project is a single-module app. All source lives under:

```
app/src/main/java/com/andaagii/tacomamusicplayer/
```

```
tacomamusicplayer/
├── activity/           # MainActivity (sole Activity)
├── adapter/            # ViewPager2 adapter, DiffCallback
├── constants/          # Const.kt — app-wide string/int constants
├── data/               # Pure data classes (SongData, SongGroup, etc.)
├── database/
│   ├── dao/            # SongDao, SongGroupDao
│   ├── entity/         # SongEntity, SongGroupEntity, SongGroupCrossRefEntity
│   └── PlayerDatabase.kt
├── di/                 # Hilt modules (AppModule, DatabaseModule)
├── enumtype/           # ScreenType, PageType, SongGroupType, ShuffleType, etc.
├── factory/            # MediaBrowserFactory
├── fragment/
│   ├── pages/          # AlbumListFragment, PlaylistFragment, SongListFragment, etc.
│   ├── PlayerDisplayFragment.kt
│   └── PermissionDeniedFragment.kt
├── observer/           # MusicContentObserver (MediaStore changes)
├── provider/           # AlbumArtFileProvider (FileProvider for artwork)
├── repository/         # MusicRepository interface + MusicRepositoryImpl
├── service/            # MusicService (MediaLibraryService)
├── state/              # AlbumTabState, PlaylistTabState (StateFlow holders)
├── util/               # MediaItemUtil, MediaStoreUtil, DataStoreUtil, UtilImpl
├── view/               # Custom views/dialogs
├── viewmodel/          # MainViewModel, AlbumTabViewModel, etc.
├── worker/             # CatalogMusicWorker (WorkManager)
└── TacomaMusicPlayerApplication.kt
```

---

## Architecture

The app follows MVVM with a repository layer, wired together by Hilt.

```
UI (Fragments / Views)
        │  events (lambdas / clicks)
        ▼
  ViewModel  ◄──── StateFlow / LiveData ──── ViewModel
        │
        ▼
  Repository (MusicRepository / MusicProviderRepository)
        │
        ├── Room DAOs (SongDao, SongGroupDao)
        └── MediaStore / File system

  MusicService (MediaLibraryService)
        │
        ├── ExoPlayer (audio engine)
        └── MediaLibrarySession (Android Auto + MediaController)

  MainViewModel ◄──── MediaController ───► MusicService
```

**Unidirectional data flow:**
- Fragments observe `StateFlow`/`LiveData` on ViewModels.
- User interactions call ViewModel methods, which update state or delegate to the repository.
- The repository is the single source of truth for library data.
- Playback state flows from `MusicService` → `MediaController` → `MainViewModel` → UI.

---

## Database Layer

**`PlayerDatabase`** (Room, version 23) has three entities:

### SongEntity (`song_table`)
Represents one playable track.

| Column | Notes |
|---|---|
| `searchDescription` | Primary key — composite `songName_albumTitle_artist` |
| `albumTitle` | Album this song belongs to |
| `artist` | Track artist |
| `name` | Track name |
| `uri` | MediaStore content URI string |
| `songDuration` | Duration in ms |
| `artFileOriginal` | Path to extracted cover art |
| `artFileCustom` | Path to user-uploaded cover art |
| `useCustomArt` | Whether to show custom art |

Indices: `song_artist`, `album_title`, `song_name`

### SongGroupEntity (`song_group_table`)
Represents an album or playlist.

| Column | Notes |
|---|---|
| `groupId` | Auto-increment PK |
| `songGroupType` | `ALBUM` or `PLAYLIST` |
| `groupTitle` | Display title |
| `groupArtist` | Primary artist |
| `releaseYear` | For albums |
| timestamps | `createdAt`, `updatedAt` |

### SongGroupCrossRefEntity (`song_ref_table`)
Junction table linking playlists to songs (with ordering).

| Column | Notes |
|---|---|
| `id` | Auto-increment PK |
| `groupId` | FK → SongGroupEntity (cascade delete) |
| `searchDescription` | FK → SongEntity (cascade delete) |
| `position` | Track order within the playlist |

> The database uses `fallbackToDestructiveMigration()`. Any schema change bumps the version and wipes existing data — keep this in mind during development.

---

## Repository Layer

Two interfaces, one implementation:

**`MusicProviderRepository`** — read-only queries (used by `MusicService` for browsing/search).

**`MusicRepository`** — extends `MusicProviderRepository`; adds write operations: playlist create/delete/reorder, custom image management, Flow-based observables.

**`MusicRepositoryImpl`** — the single concrete class, bound as a singleton to both interfaces via Hilt. It:
- Converts `SongEntity`/`SongGroupEntity` to `MediaItem` objects via `MediaItemUtil`.
- Delegates all DB access to `SongDao` and `SongGroupDao`.
- Manages timestamps when playlists are modified.

---

## ViewModels

### MainViewModel (`viewmodel/MainViewModel.kt`) — ~1,050 lines

The central ViewModel. Every fragment that needs playback or library data accesses this.

**Responsibilities:**
- Permission checks (`READ_MEDIA_AUDIO`)
- `MediaBrowser` initialization (library browsing)
- `MediaController` setup (playback commands)
- Queue management: build, shuffle, save, restore
- Playlist CRUD operations
- Search queries
- Screen navigation state

**Key exposed state:**
```kotlin
val mediaController: LiveData<MediaController>
val currentSongGroup: LiveData<SongGroup>
val isPlaying: LiveData<Boolean>
val shuffleMode: LiveData<ShuffleType>
val loopMode: LiveData<Int>
val currentlyPlayingSongs: StateFlow<List<MediaItem>>
val screenState: LiveData<ScreenData>
val availablePlaylists: StateFlow<List<SongGroup>>
val currentPlayingSongInfo: LiveData<MediaMetadata>
```

**Key methods:**
```kotlin
initializeMusicPlaying()              // sets up MediaSession connection
playSongGroupAtPosition(group, pos)   // replaces queue, starts playback
addTracksSaveTrackOrder(items, pos)   // centralized queue-building with shuffle support
flipShuffleState()                    // toggle shuffle
flipLoopMode()                        // cycle repeat modes
flipPlayingState()                    // play/pause
saveQueue()                           // persist queue to DB before app closes
```

### AlbumTabViewModel (`viewmodel/AlbumTabViewModel.kt`)
Manages albums page state: sort order (title / artist / release year) and layout (list vs grid). Exposes a single `StateFlow<AlbumTabState>`.

### PlaylistTabViewModel
Mirror of `AlbumTabViewModel` for the playlists page.

### SongListViewModel (`viewmodel/SongListViewModel.kt`)
Handles multi-select state for songs (e.g., batch add-to-playlist). Tracks selected songs and which playlists are checked.

---

## Navigation & Screens

### Activity

`MainActivity` is the sole Activity. It sets up a `NavController` with a programmatic nav graph (no XML):

```kotlin
navController.graph = navController.createGraph(
    startDestination = ScreenType.MUSIC_CHOOSER_SCREEN.route()
) {
    fragment<PlayerDisplayFragment>(ScreenType.MUSIC_CHOOSER_SCREEN.route())
    fragment<PermissionDeniedFragment>(ScreenType.PERMISSION_DENIED_SCREEN.route())
}
```

### Screen types (`ScreenType` enum)

| Value | Route | Description |
|---|---|---|
| `MUSIC_CHOOSER_SCREEN` | default start | Main browsing screen |
| `MUSIC_PLAYING_SCREEN` | — | Full-screen player |
| `MUSIC_QUEUE_SCREEN` | — | Queue display |
| `PERMISSION_DENIED_SCREEN` | — | Permission request |

### Pages within the chooser screen (`PageType` enum)

`PlayerDisplayFragment` wraps a `ViewPager2`. Page indices map to `PageType`:

| Index | Page | Fragment |
|---|---|---|
| 0 | `QUEUE_PAGE` | `CurrentQueueFragment` |
| 1 | `PLAYER_PAGE` | `MusicPlayingFragment` (mini player) |
| 2 | `PLAYLIST_PAGE` | `PlaylistFragment` |
| 3 | `ALBUM_PAGE` | `AlbumListFragment` |
| 4 | `SONG_PAGE` | `SongListFragment` |

Users swipe between pages. A double-tap or swipe-down on the player expands to the full-screen player.

---

## Media Playback (MusicService)

`MusicService` extends `MediaLibraryService` (Media3) and owns the `ExoPlayer` instance.

### Session & player setup
- Creates a `MediaLibrarySession` with a unique session ID.
- Configures `ExoPlayer` with audio focus coordination and "audio becoming noisy" handling.
- `MainActivity` connects via `MediaBrowser`; `MainViewModel` connects via `MediaController`.

### Android Auto browsing hierarchy

```
root/
├── albums/
│   └── album:<title>/    → songs in that album
├── artists/
│   └── artist:<name>/    → albums by that artist
└── playlists/
    └── playlist:<title>/ → songs in that playlist
```

`onGetLibraryRoot()`, `onGetChildren()`, `onSearch()`, and `onGetSearchResult()` implement the `MediaLibrarySession.Callback` to serve this tree.

### Playback requests from Android Auto

When a user taps a song in Android Auto, `onAddMediaItems()` is called. The service parses the media item ID (e.g., `album:Thriller`) to determine which group to load, builds the full queue, and sets a `pendingSeek` to jump to the right track.

### `PlayerEventListener`
Tracks playback state transitions and updates internal state that `MainViewModel` observes via `MediaController`.

---

## Key Features

### Queue Management
- The current queue is stored as a special playlist identified by `Const.PLAYLIST_QUEUE_TITLE`.
- The pre-shuffle song order is stored separately under `Const.ORIGINAL_QUEUE_ORDER`.
- On app start, `MainViewModel.restoreQueue()` reloads the last queue and seeks to the saved position (stored in DataStore).
- On app exit, `saveQueue()` persists the current queue to the DB.

### Custom Album Art
- Original art is extracted from MediaStore during cataloging and cached on disk.
- Users can upload custom images (cropped via uCrop).
- `MediaItemUtil.determineArtUri()` selects between original and custom art.
- `AlbumArtFileProvider` (FileProvider) shares artwork URIs securely with Android Auto.

### Search
- Full-text search via `MusicRepository.searchMusic()`.
- Queries both `SongEntity` and `SongGroupEntity` with case-insensitive LIKE.
- Returns up to 25 results, sorted by match position (best match first).

### Playlist Management
- Users can create, rename, delete, and reorder playlists.
- Playlists are stored in `SongGroupEntity` (type = `PLAYLIST`) with cross-references in `SongGroupCrossRefEntity`.
- Reordering updates the `position` column in the cross-ref table.

### Music Cataloging
`CatalogMusicWorker` (WorkManager) runs on first launch and on manual refresh:
1. Queries all albums from MediaStore.
2. For each album, extracts cover art and catalogs tracks.
3. Inserts/updates `SongEntity` and `SongGroupEntity` rows in Room.

---

## Dependency Injection

Hilt is configured via `@HiltAndroidApp` on `TacomaMusicPlayerApplication`.

**`AppModule`** (`di/AppModule.kt`):
- Binds `MusicRepositoryImpl` → `MusicRepository` (singleton)
- Binds `MusicRepositoryImpl` → `MusicProviderRepository` (singleton)

**`DatabaseModule`** (`di/DatabaseModule.kt`):
- Provides `PlayerDatabase` singleton
- Provides `SongDao` and `SongGroupDao` from the database

Inject dependencies in ViewModels with `@HiltViewModel` + `@Inject constructor(...)`, and in Workers with `@HiltWorker` + `@AssistedInject`.

---

## Preferences & Persistence

DataStore (Preferences) is used for lightweight user settings and playback state:

| Key | Purpose |
|---|---|
| Layout preference | Grid vs linear list |
| Sort preference | Title / artist / release year / created date |
| Shuffle mode | `ShuffleType` enum |
| Repeat mode | Loop mode integer |
| Playback position | Byte offset to restore on restart |
| Song index | Index in queue to restore on restart |

`DataStoreUtil` centralizes all read/write operations.

---

## Android Auto

Android Auto support requires:
- `automotive_app_desc.xml` declaring the media app type.
- `MusicService` implementing `MediaLibrarySession.Callback` with the browsing hierarchy above.
- FileProvider URIs for artwork (plain `file://` URIs are blocked by Auto).
- Search support via `onSearch()` / `onGetSearchResult()` for voice commands.

Test with the Desktop Head Unit (DHU) emulator from the Android SDK.

---

## Background Work

`CatalogMusicWorker` is a `CoroutineWorker` injected via Hilt (`@HiltWorker`). It is enqueued as a one-time `WorkRequest` from `MainActivity` on first launch or refresh. WorkManager is configured in `TacomaMusicPlayerApplication` via `Configuration.Provider`.

---

## Adding a New Feature — Checklist

1. **Data model** — add/update an entity in `database/entity/`. Bump the Room DB version in `PlayerDatabase.kt`. Decide on migration vs. destructive migration.
2. **DAO** — add query methods in `SongDao` or `SongGroupDao`.
3. **Repository** — add the method signature to the relevant interface and implement it in `MusicRepositoryImpl`.
4. **ViewModel** — expose new state as `StateFlow` or `LiveData` from the appropriate ViewModel. Use `viewModelScope` for coroutines.
5. **UI** — update or add a Fragment. Observe ViewModel state; send events up via method calls.
6. **DI** — if you introduce a new singleton, add a `@Provides` binding in a Hilt module.
7. **Android Auto** — if the feature affects browsable content, update `MusicService` callbacks.
8. **Persistence** — if state needs to survive restarts, add a DataStore key in `DataStoreUtil`.
