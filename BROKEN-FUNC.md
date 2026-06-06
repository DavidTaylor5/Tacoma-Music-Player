# Broken Functionality — Post-Compose Migration Audit

This document records all features that regressed after Phases 4 and 5 of the Jetpack
Compose migration (HorizontalPager + Compose NavHost). It is intended to guide the fix
work needed before the app reaches feature parity with its pre-migration state.

---

## Root Cause

Every breakage traces back to a single architectural mistake: **ViewModel scoping**.

`MusicChooserScreen.kt` (lines 66–68) calls `hiltViewModel()` inside a `composable {}`
block within a `NavHost`:

```kotlin
val mainViewModel: MainViewModel = hiltViewModel()
val albumViewModel: AlbumTabViewModel = hiltViewModel()
val playlistViewModel: PlaylistTabViewModel = hiltViewModel()
```

When `hiltViewModel()` is called inside a NavHost destination composable, Hilt uses the
**`NavBackStackEntry`** as the `ViewModelStoreOwner`. This creates instances that are
**completely separate** from the Activity-scoped `MainViewModel` that `MainActivity` creates
with `by viewModels()` and passes to `TacomaMusicPlayerApp`.

The consequence:

- `TacomaMusicPlayerApp` calls `viewModel.initializeMusicPlaying()` on the **Activity-scoped**
  `MainViewModel` → that VM connects the `MediaController` and attaches the `playerListener`.
- `MusicChooserScreen` holds a **NavBackStackEntry-scoped** `MainViewModel` where
  `_mediaController` is permanently `null` and `playerListener` is never attached.
- Every call that flows through `mediaController.value?.let { … }` in the
  NavBackStackEntry-scoped VM silently does nothing.

`AlbumTabViewModel` and `PlaylistTabViewModel` are **less affected** because both observe
Room Flows (`getAllAvailableAlbumsFlow()` / `getAllAvailablePlaylistFlow()`) that
auto-update from the database the moment `MainActivity.queryMusic()` writes to it — even
across different ViewModel instances.

---

## Required Fix

Change `MusicChooserScreen` to accept the three Activity-scoped ViewModels as parameters
instead of creating NavBackStackEntry-scoped instances via `hiltViewModel()`:

```kotlin
// TacomaMusicPlayerApp.kt — acquire at Activity scope and pass down
val mainViewModel: MainViewModel  // already passed as a parameter
val albumViewModel: AlbumTabViewModel = hiltViewModel()
val playlistViewModel: PlaylistTabViewModel = hiltViewModel()

composable(ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
    MusicChooserScreen(
        mainViewModel = mainViewModel,
        albumViewModel = albumViewModel,
        playlistViewModel = playlistViewModel
    )
}
```

```kotlin
// MusicChooserScreen.kt — accept as parameters, remove internal hiltViewModel() calls
@Composable
fun MusicChooserScreen(
    mainViewModel: MainViewModel,
    albumViewModel: AlbumTabViewModel,
    playlistViewModel: PlaylistTabViewModel
) { … }
```

`SongListViewModel` is **not** affected; `viewModel()` inside `MusicChooserScreen` is
acceptable because `SongListViewModel` holds only transient multi-select UI state and does
not depend on `MediaController`.

---

## Broken Features

### 1. Tapping any song does not start playback

**Where:** SongListScreen, any song row tap.

**Why:** `onSongClick` in `MusicChooserScreen` (line 304–307) calls
`mainViewModel.playSongGroupAtPosition(it, pos)`. Internally this does
`mediaController.value?.let { … }` on the NavBackStackEntry-scoped VM where
`mediaController` is `null` → silent no-op.

Also broken by the same null controller:
- Header "Play" button (`onHeaderPlayClick`, line 317–319)
- Search song result tap (`onSearchSongClick` → `playAlbumAtSongPosition`, line 349–352)

---

### 2. Mini-player never appears

**Where:** Bottom of `MusicChooserScreen`, below the pager.

**Why:** `showMiniPlayer` (line 189–190) requires `songInfo != null`. `songInfo` is
collected from `mainViewModel.currentPlayingSongInfo` on the NavBackStackEntry-scoped VM.
Because `playerListener` was only attached to the Activity-scoped VM, `_currentPlayingSongInfo`
on the NavBackStackEntry-scoped VM is never updated → always `null` →
`showMiniPlayer` is always `false`.

---

### 3. MusicPlayingScreen shows no song information

**Where:** Page 1 (Player page).

**Why:** `MusicPlayingScreen` receives `songInfo` from the NavBackStackEntry-scoped VM →
always `null` → no title, no artist, no artwork rendered.

---

### 4. All playback controls on MusicPlayingScreen are non-functional

**Where:** Page 1 (Player page), all transport controls.

**Why:** Every control passes through the null `controller` reference:

| Control | Code path | Result |
|---|---|---|
| Previous song | `controller?.seekToPrevious()` | no-op |
| Seek back | `controller?.seekBack()` | no-op |
| Play / Pause | `mainViewModel.flipPlayingState()` → `_mediaController.value?.pause()/play()` | no-op |
| Seek forward | `controller?.seekForward()` | no-op |
| Next song | `controller?.seekToNextMediaItem()` | no-op |
| Shuffle | `mainViewModel.flipShuffleState()` → `_mediaController.value?.…` | no-op |
| Loop mode | `mainViewModel.flipLoopMode()` → `_mediaController.value?.repeatMode = …` | no-op |

---

### 5. Queue screen is empty and non-interactive

**Where:** Page 0 (Queue page).

**Why:** `queueSongs` is collected from `mainViewModel.currentlyPlayingSongs` on the
NavBackStackEntry-scoped VM. Because `playerListener` was only attached to the Activity-scoped
VM, the queue list is never populated → always empty.

Even if songs were visible, all queue interactions call through the null `controller`:

| Action | Code path | Result |
|---|---|---|
| Tap to seek to position | `controller?.seekTo(pos, 0L)` + `play()` | no-op |
| Remove song | `controller?.removeMediaItem(pos)` | no-op |
| Drag to reorder | `controller?.moveMediaItem(from, to)` | no-op |
| Clear queue | `mainViewModel.clearQueue()` → internally uses `_mediaController.value` | no-op |

---

### 6. "Play playlist" and "Add playlist to queue" do nothing

**Where:** PlaylistScreen overflow menu and long-press menu.

**Why:** Both callbacks reference functions on the NavBackStackEntry-scoped `MainViewModel`:
- `mainViewModel::playPlaylist` (MainViewModel line 878) → `mediaController.value?.let { … }` → null
- `mainViewModel::addPlaylistToBackOfQueue` (MainViewModel line 901) → null controller

---

### 7. "Play album" and "Add album to queue" do nothing

**Where:** AlbumListScreen overflow menu.

**Why:**
- `mainViewModel::playAlbum` (MainViewModel line 1313, also carries a developer
  `// TODO I don't think this is working…` comment — may be a pre-existing issue) → null controller
- `onMenuOption → addAlbumToBackOfQueue` (line 1324) → null controller

---

### 8. "Add to queue" from multi-select does nothing

**Where:** SongListScreen multi-select action bar.

**Why:** `mainViewModel.addSongsToEndOfQueue(songs)` (MainViewModel line 920) uses
`mediaController.value?.let { … }` → null → no-op.

---

### 9. Playlist page → song list: songs display but won't play

**Where:** PlaylistScreen → tap a playlist → SongListScreen.

**Why:** `onPlaylistClick` (MusicChooserScreen line 234–236) calls
`mainViewModel.querySongsFromPlaylist(playlist)` and `mainViewModel.setPage(SONG_PAGE)` on
the NavBackStackEntry-scoped VM. Navigation to the song list and the song list population
both work correctly (no controller needed). However, `onSongClick` then calls
`mainViewModel.playSongGroupAtPosition()` → null controller → nothing plays.

This is likely what the user observed as "PlaylistScreen shows songScreen functionality":
the playlist correctly navigates to a song list, but the song list is non-functional.

---

### 10. Search: song result taps do not start playback

**Where:** SongListScreen in search mode, song result rows.

**Why:** `onSearchSongClick` → `mainViewModel.playAlbumAtSongPosition(song)` (MainViewModel
line 1123) → `mediaController.value?.play()` → null → no-op.

Album and playlist search result taps work correctly (they only call `querySongsFromAlbum`
/ `querySongsFromPlaylist` and cancel search — no controller needed).

---

## What Still Works

| Feature | Why it works |
|---|---|
| Swipe and tab navigation between pages | Handled by `pagerState` and `navigateToPage` channel, both on NavBackStackEntry-scoped VM |
| Album list loading | `AlbumTabViewModel` observes a Room Flow; auto-updates from DB |
| Playlist list loading | `PlaylistTabViewModel` observes a Room Flow; auto-updates from DB |
| "Add to playlist" available-playlists dropdown | `availablePlaylists` is a Room Flow in `MainViewModel`; auto-updates from DB |
| Playlist CRUD (create, rename, delete) | DB-only operations; no controller needed |
| Playlist song reorder (drag) | DB write via `updatePlaylistOrder`; no controller needed |
| Album / playlist image picker | `rememberLauncherForActivityResult` wired correctly |
| Search results display | `querySearchData` only queries the DB; no controller needed |
| Permission request flow | Handled entirely in `TacomaMusicPlayerApp` via Activity-scoped VM |
| Permission-denied screen navigation | `screenState` channel on Activity-scoped VM |
| Loading screen | `showLoadingScreen` collected from Activity-scoped VM in `TacomaMusicPlayerApp` |
| Multi-select UI state | `SongListViewModel` is independently scoped; no controller dependency |
