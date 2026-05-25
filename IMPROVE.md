# IMPROVE.md — Code Improvement Backlog

> A living checklist of concrete, actionable improvements identified by a full codebase audit.
> Items are grouped by category and ordered **Critical → High → Medium → Low** within each section.
> File paths are relative to `app/src/main/java/com/andaagii/tacomamusicplayer/` unless noted otherwise.

---

## Table of Contents

1. [Architecture & Separation of Concerns](#1-architecture--separation-of-concerns)
2. [State Management](#2-state-management)
3. [Concurrency & Threading](#3-concurrency--threading)
4. [Null Safety & Crash Risks](#4-null-safety--crash-risks)
5. [Resource Leaks](#5-resource-leaks)
6. [Error Handling](#6-error-handling)
7. [Code Duplication (DRY)](#7-code-duplication-dry)
8. [Modern Android APIs](#8-modern-android-apis)
9. [Documentation (APP-COMMENTS.md Compliance)](#9-documentation-app-commentsmd-compliance)
10. [Minor / Low Priority](#10-minor--low-priority)

---

## 1. Architecture & Separation of Concerns

### 🔴 Critical — Split `MainViewModel` (God Object)

**File:** `viewmodel/MainViewModel.kt` (~1,050 lines)

`MainViewModel` handles at least eight distinct concerns: `MediaController`/`MediaBrowser` lifecycle, playback controls, queue build/save/restore, shuffle logic, permission checks, screen navigation, playlist CRUD, and search. This makes it impossible to unit-test individual concerns and makes the class fragile to change.

**Recommended split:**

| New ViewModel | Responsibilities |
|---|---|
| `PlaybackViewModel` | `MediaController`/`MediaBrowser` setup, play/pause/skip/seek, loop mode, shuffle |
| `QueueViewModel` | Queue build (`addTracksSaveTrackOrder`), save, restore, current queue `StateFlow` |
| `PlaylistViewModel` | Playlist CRUD, `availablePlaylists` `StateFlow` |
| `PermissionViewModel` | `READ_MEDIA_AUDIO` check, `screenState` navigation event |
| `MainViewModel` (thin) | Wire the above together; expose only what `PlayerDisplayFragment` needs directly |

---

### 🔴 Critical — Business Logic Inside `SongListFragment`

**File:** `fragment/pages/SongListFragment.kt` (~827 lines)

Two methods that belong in `SongListViewModel` are currently inline in the fragment:

- `savePlaylistChanges()` — persists a reordered playlist to the repository.
- `determineIfPlaylistSongsHaveChanged()` — computes equality between two lists of `MediaItem`.

Both are pure domain operations. Moving them to the ViewModel makes them testable and removes the 827-line fragment's most avoidable responsibility.

---

### 🟡 High — `SongGroup.songs` Should Be Immutable

**File:** `data/SongGroup.kt` (line ~24)

`songs` is declared `var` to allow in-place shuffling. Mutating shared state in a data class breaks referential integrity when multiple observers hold the same `SongGroup` instance.

**Fix:** Change to `val`. On shuffle, produce a new `SongGroup` via `copy(songs = shuffled)` instead of mutating the original.

---

## 2. State Management

### 🔴 Critical — Enable Hilt Injection on `SongListViewModel`

**File:** `viewmodel/SongListViewModel.kt` (line ~26)

`@HiltViewModel` is commented out, forcing all host fragments to instantiate the ViewModel manually. This breaks the Hilt DI contract and prevents proper scoping.

**Fix:** Re-enable `@HiltViewModel` and inject via `by viewModels()` in the host fragment.

---

### 🟡 High — Standardize on `StateFlow` (Remove `LiveData` from ViewModels)

**File:** `viewmodel/MainViewModel.kt` (lines ~99–271), `viewmodel/SongListViewModel.kt`

`MainViewModel` mixes `MutableLiveData` (most properties) with `MutableStateFlow` (`availablePlaylists`, `currentlyPlayingSongs`). `SongListViewModel` uses only `MutableLiveData`. This inconsistency increases cognitive load and prevents collecting state in coroutine scopes.

**Fix:** Migrate all `MutableLiveData<T>` → `MutableStateFlow<T>` with sensible initial values. In fragments, replace `observe {}` with `viewLifecycleOwner.lifecycleScope.launch { repeatOnLifecycle(STARTED) { collect {} } }`.

**Note:** Ensure all `StateFlow` backing fields are initialized with a non-null default so observers never receive `null`.

---

### 🟡 High — Async Callback State Stored in Mutable Fragment Fields

**Files:**
- `fragment/pages/AlbumListFragment.kt` — `albumCustomImageName: String?`, `selectedAlbumName: String?`
- `fragment/pages/SongListFragment.kt` — `currentSongGroup`, `lastDisplaySongGroup`

These fields store state across async boundaries (e.g., the user selects an album, then a uCrop `ActivityResult` fires and reads the field). If the user selects a different album before the crop result arrives, the wrong image is applied.

**Fix:** Route this state through the ViewModel. Create a `StateFlow<PendingImageSelection?>` in `AlbumTabViewModel` that is set when an album is chosen and cleared when the crop result is applied.

---

## 3. Concurrency & Threading

### 🔴 Critical — `Handler` in `MainViewModel` Bypasses Coroutine Cancellation

**File:** `viewmodel/MainViewModel.kt` (line ~252)

```kotlin
// Current — Runnable is not cancelled when ViewModel is cleared
loadingHandler = Handler(Looper.getMainLooper())
loadingHandler.postDelayed({ _showLoadingScreen.postValue(false) }, 500)
```

If the ViewModel is cleared before the 500 ms fires, the `Runnable` still executes and posts to a dead `LiveData`.

**Fix:**
```kotlin
viewModelScope.launch {
    delay(500)
    _showLoadingScreen.value = false
}
```

---

### 🔴 Critical — Missing `Dispatchers.IO` in `MusicRepositoryImpl`

**File:** `repository/MusicRepositoryImpl.kt`

Several `suspend` functions perform Room queries or disk I/O without `withContext(Dispatchers.IO)`:

- `searchMusic()` (line ~31)
- `createInitialQueueIfEmpty()` (line ~160)
- `getSongsByAlbum()` / `getSongsByArtist()` (lines ~175, ~202)

Because Room suspend functions are safe to call on any thread, these won't crash — but the work runs on the calling coroutine's dispatcher (often `Main`), which can cause jank.

**Fix:** Wrap each database call in `withContext(Dispatchers.IO) { ... }` or annotate the function with `@WorkerThread` at minimum.

---

### 🟡 High — `postValue()` Called Inside `Dispatchers.IO` Block

**File:** `viewmodel/MainViewModel.kt` (`querySearchData`, `querySongsFromPlaylist`)

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    _currentSearchList.postValue(musicRepo.searchMusic(search)) // misleading
}
```

`postValue` is safe from any thread, but mixing `Dispatchers.IO` and `LiveData` updates is a smell. When `LiveData` is fully replaced by `StateFlow`, `emit` must be called on Main (or use `MutableStateFlow.value` which is thread-safe).

---

### 🟡 High — `pendingSeek` Race Condition in `MusicService`

**File:** `service/MusicService.kt` (line ~143)

`pendingSeek: Int?` is written from `onAddMediaItems()` (which can be called from a binder thread) and read from `PlayerEventListener` callbacks. The comment states "must only be written from the main thread" but there is no enforcement.

**Fix:** Annotate write sites with `@MainThread`, add a `check(Looper.myLooper() == Looper.getMainLooper())` assertion in debug builds, or guard with `AtomicInteger`.

---

## 4. Null Safety & Crash Risks

### 🔴 Critical — SQL Bug: `getAllSongsFromArtist` Queries Wrong Column

**File:** `database/dao/SongDao.kt` (line ~43)

```kotlin
// Current — queries album_title, not song_artist
@Query("SELECT * FROM song_table WHERE album_title = :artist")
suspend fun getAllSongsFromArtist(artist: String): List<SongEntity>
```

Every call to this DAO method silently returns songs whose **album title** matches the artist name — correct results only by coincidence.

**Fix:**
```kotlin
@Query("SELECT * FROM song_table WHERE song_artist = :artist")
suspend fun getAllSongsFromArtist(artist: String): List<SongEntity>
```

---

### 🔴 Critical — `foundSongs[0]` Without Bounds Check

**File:** `repository/MusicRepositoryImpl.kt` (line ~235)

`foundSongs[0]` is accessed directly after a database query. If the query returns an empty list, this throws `IndexOutOfBoundsException`.

**Fix:** Replace with `foundSongs.firstOrNull() ?: return <appropriate default>`.

---

### 🔴 Critical — `indexOfFirst` Result Used as Array Index Without Guard

**File:** `adapter/QueueListAdapter.kt` (line ~79)

`indexOfFirst { ... }` returns `-1` when no match is found. The result is immediately used as an array index (`dataSet[currSongPos]`), which crashes with `ArrayIndexOutOfBoundsException`.

**Fix:**
```kotlin
val currSongPos = dataSet.indexOfFirst { it.showPlayIndicator }
if (currSongPos >= 0) dataSet[currSongPos].showPlayIndicator = false
```

---

### 🟡 High — `split()` Result Accessed Without Bounds Check

**File:** `util/SortingUtil.kt` (lines ~143–160)

```kotlin
val timestamps = description.split(":")
val creation = timestamps[0]   // crashes if split produces < 1 element
val modified = timestamps[1]   // crashes if split produces < 2 elements
```

**Fix:** Use `getOrNull`:
```kotlin
val creation = timestamps.getOrNull(0) ?: ""
val modified = timestamps.getOrNull(1) ?: ""
```

---

### 🟡 High — `mediaItem.mediaId` Used Without Null Check

**File:** `util/MediaItemUtil.kt` (line ~277)

`mediaItem.mediaId` is a nullable `String?`. Calling `.split()` directly on it will throw if the ID is null.

**Fix:** Guard with `?: return null` or use safe-call: `mediaItem.mediaId?.split(":") ?: return null`.

---

### 🟡 High — `uri.path.toString()` Converts `null` to the String `"null"`

**File:** `util/UtilImpl.kt` (line ~321)

`Uri.path` returns `String?`. Calling `.toString()` on a null returns the string literal `"null"`, which is then used as a file path and silently fails downstream.

**Fix:** Use `uri.path ?: return` or `uri.path.orEmpty()` with an appropriate guard.

---

## 5. Resource Leaks

### 🔴 Critical — `MediaMetadataRetriever` Never Released

**Files:**
- `util/MediaStoreUtil.kt` (line ~113)
- `util/UtilImpl.kt` (line ~300)

`MediaMetadataRetriever` holds a native handle. If `setDataSource` or any subsequent call throws, `release()` is never called.

**Fix:**
```kotlin
val retriever = MediaMetadataRetriever()
try {
    retriever.setDataSource(context, uri)
    // ... use retriever
} finally {
    retriever.release()
}
```

Or use Kotlin's `use {}` with a wrapper since `MediaMetadataRetriever` implements `AutoCloseable` on API 29+.

---

### 🟡 High — ExoPlayer Listener Accumulates on Service Restart

**File:** `service/MusicService.kt` (line ~641)

`player.addListener(PlayerEventListener())` is called in `initializePlayer()` but `removeListener()` is never called in `onDestroy()`. If `MusicService` is restarted by the system, a new listener is added each time without removing the old one.

**Fix:** Store the listener instance and remove it:
```kotlin
private val playerEventListener = PlayerEventListener()
// in initializePlayer():
player.addListener(playerEventListener)
// in onDestroy():
player.removeListener(playerEventListener)
```

---

### 🟡 High — Temp Files Created but Never Deleted

**File:** `util/UtilImpl.kt` (`uriToFile`, line ~456)

`File.createTempFile(...)` writes to `context.cacheDir` or a temp directory but there is no corresponding cleanup. On low-storage devices, these accumulate.

**Fix:** Either delete the file after use, or implement a cleanup pass (e.g., delete cache files older than 24 hours) in `CatalogMusicWorker`.

---

## 6. Error Handling

### 🔴 Critical — `CatalogMusicWorker` Swallows All Exceptions

**File:** `worker/CatalogMusicWorker.kt` (line ~35)

`doWork()` calls `catalogMusic()` with no try-catch. Any thrown exception causes the coroutine to fail silently and `Result.success()` is never reached — WorkManager marks the work as failed but the app has no record of what went wrong.

**Fix:**
```kotlin
override suspend fun doWork(): Result {
    return try {
        catalogMusic()
        Result.success()
    } catch (e: Exception) {
        Timber.e(e, "Music catalog failed")
        Result.failure()
    }
}
```

---

### 🔴 Critical — `MusicService.onPlayerError()` Is Empty

**File:** `service/MusicService.kt` (line ~727)

`onPlayerError()` contains only a TODO comment. Playback errors (corrupt file, codec unsupported, MediaStore URI revoked) are silently ignored, leaving the user with a frozen player and no feedback.

**Fix:** At minimum, log with Timber and emit an error state that `MainViewModel` can surface as a Snackbar or toast. For auto-recoverable errors (e.g., `ERROR_CODE_BEHIND_LIVE_WINDOW`), call `player.prepare()`.

---

### 🟡 High — `MediaController`/`MediaBrowser` Future Has No Error Handling

**File:** `viewmodel/MainViewModel.kt` (line ~1005)

```kotlin
controllerFuture.addListener({
    val controller = controllerFuture.get() // no try-catch
    _mediaController.value = controller
}, MoreExecutors.directExecutor())
```

If `controllerFuture.get()` throws (e.g., `CancellationException`, `ExecutionException`), the exception is silently swallowed and `_mediaController` is never set.

**Fix:** Wrap in try-catch and emit a failure state or retry.

---

### 🟡 High — `workManager.cancelAllWork()` Is Too Broad

**File:** `activity/MainActivity.kt` (line ~217)

`cancelAllWork()` cancels every enqueued `WorkRequest` in the app, not just the music catalog worker. This is dangerous if other workers are ever added.

**Fix:** Use the unique work name: `workManager.cancelUniqueWork("catalog_music")` (matching the enqueue call).

---

## 7. Code Duplication (DRY)

### 🟡 High — `SongListAdapter` Constructed 3× With Identical Parameters

**File:** `fragment/pages/SongListFragment.kt` (lines ~217, ~255, ~505)

The `SongListAdapter(...)` constructor call with the same 6–8 lambda parameters is copy-pasted three times. Any change to the adapter's constructor requires three edits.

**Fix:** Extract a private `buildSongListAdapter(): SongListAdapter` helper that captures the fragment's lambdas once and returns a configured instance.

---

### 🟡 High — Queue Adapter Rebuilt With Duplicate Code

**File:** `fragment/pages/CurrentQueueFragment.kt` (lines ~118–158)

Two separate `observe {}` / `collect {}` branches initialize `QueueListAdapter` with identical setup code.

**Fix:** Merge into one observer or extract a `buildQueueAdapter()` helper called from both branches.

---

### 🟡 High — Artwork URI Resolution Logic Duplicated

**File:** `util/MediaItemUtil.kt`

The `determineArtUri()` method's logic (check `useCustomArt`, resolve `customArtPath` vs. `originalArtPath`) is duplicated inside at least `createAlbumMediaItemFromSongGroupEntity` (lines ~128–140) and one other creation method.

**Fix:** Consolidate all artwork resolution into the single `determineArtUri()` method and call it from all construction paths.

---

## 8. Modern Android APIs

### 🟡 High — Deprecated Permission Handling

**File:** `activity/MainActivity.kt`

`onRequestPermissionsResult()` is deprecated in API 33+ and requires manually correlating request codes. The modern replacement is type-safe and lifecycle-aware.

**Fix:** Replace with `registerForActivityResult(ActivityResultContracts.RequestPermission())` in `onCreate()`. Remove the `onRequestPermissionsResult()` override and the `REQUEST_CODE_*` constants it uses.

---

### 🟡 High — RecyclerView Adapters Lack DiffUtil

**Files:** `adapter/SongListAdapter.kt`, `adapter/QueueListAdapter.kt`

Both adapters call `notifyDataSetChanged()` on every data update, which redraws every visible row even when only one item changed. This causes janky scrolling on large song lists.

The project already has `adapter/diff/MediaItemDiffCallback.kt` — it is used by `AlbumListAdapter` and `AlbumGridAdapter` but not by these two adapters.

**Fix:** Change both to extend `ListAdapter<MediaItem, *>` and pass `MediaItemDiffCallback()` to the superclass constructor. Replace manual `dataSet` management with `submitList()`.

---

## 9. Documentation (APP-COMMENTS.md Compliance)

### 🟡 High — TODO Comments Without Ticket References

**Files:** `viewmodel/MainViewModel.kt`, `service/MusicService.kt`, `repository/MusicRepositoryImpl.kt`, `worker/CatalogMusicWorker.kt` (and others)

Per `APP-COMMENTS.md §9`, `// TODO` with no ticket number is an anti-pattern because it is orphaned intent that will never be acted on. Dozens of such TODOs exist throughout the codebase.

**Fix:** For each TODO, either:
1. Resolve it immediately, or
2. Replace it with a reference to a tracked issue: `// TODO(#123): description`.

---

### 🟡 High — Commented-Out Production Code

**File:** `repository/MusicRepositoryImpl.kt` (lines ~89–102)

`removeSongsFromPlaylist` is entirely commented out with a note indicating uncertainty about the implementation.

Per `APP-COMMENTS.md §9`, commented-out code pollutes history. Git exists for recovery.

**Fix:** Either implement the method (it is declared on the `MusicRepository` interface) or delete the commented block and keep a reference to the git commit in the issue tracker.

---

### 🟡 High — Missing Class-Level KDoc

Per `APP-COMMENTS.md §2.4`, every class must have a KDoc covering responsibility, lifecycle/threading constraints, and exposed state. The following classes are missing it:

| File | Class |
|---|---|
| `activity/MainActivity.kt` | `MainActivity` |
| `adapter/SongListAdapter.kt` | `SongListAdapter` |
| `adapter/QueueListAdapter.kt` | `QueueListAdapter` |
| `worker/CatalogMusicWorker.kt` | `CatalogMusicWorker` |
| `fragment/pages/CurrentQueueFragment.kt` | `CurrentQueueFragment` |
| `service/MusicService.kt` | `MusicService` |

---

### 🟡 High — Hardcoded Debug File Path in Production Code

**File:** `util/UtilImpl.kt` (`drawMp3agicBitmap`, line ~116)

A hardcoded path to a specific local file (`/storage/emulated/0/Music/Clipse/...`) exists in what appears to be debug-only code that was never removed.

**Fix:** Delete the method or gate it behind `if (BuildConfig.DEBUG)` with a clear explanation of its purpose.

---

## 10. Minor / Low Priority

### 🟢 Low — `AlbumTabViewModel._albums` Declared as `var`

**File:** `viewmodel/AlbumTabViewModel.kt` (line ~72)

`_albums` is a `StateFlow` backing field that is assigned once in the constructor and never reassigned. Declaring it `var` instead of `val` incorrectly signals mutability.

**Fix:** Change to `private val _albums`.

---

### 🟢 Low — Sentinel Strings in `SongData` Should Be Constants

**File:** `data/SongData.kt` (lines ~53–54)

The string literals `"null"` and `"UNKNOWN"` are used as sentinel values. If the same strings need to be checked elsewhere, the comparison must be duplicated.

**Fix:** Extract to a companion object:
```kotlin
companion object {
    const val UNKNOWN_VALUE = "UNKNOWN"
    const val NULL_SENTINEL = "null"
}
```

---

### 🟢 Low — `Const.kt` Should Be Organized by Domain

**File:** `constants/Const.kt`

All constants live in a single flat `companion object`. As the app grows, constants for unrelated domains (media IDs, DataStore keys, playlist names, storage paths) are indistinguishable at a glance.

**Fix:** Group into nested objects or separate files:
```kotlin
object MediaIds { ... }
object DataStoreKeys { ... }
object PlaylistNames { ... }
```

---

### 🟢 Low — Magic Numbers in Fragments Should Be Named Constants

**Files:**
- `fragment/PlayerDisplayFragment.kt` — `offscreenPageLimit = 4`, swipe velocity threshold `500`
- `fragment/pages/MusicPlayingFragment.kt` — hardcoded animation durations

**Fix:** Extract to companion object constants with descriptive names:
```kotlin
private const val VIEWPAGER_OFFSCREEN_LIMIT = 4
private const val MIN_FLING_VELOCITY_DP_PER_S = 500
```

---

*Last audited: 2026-05-25*
