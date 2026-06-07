# IMPROVE.md — Code Improvement Backlog

> A living checklist of concrete, actionable improvements identified by a full codebase audit.
> Items are grouped by category and ordered **Critical → High → Medium → Low** within each section.
> File paths are relative to `app/src/main/java/com/andaagii/tacomamusicplayer/` unless noted otherwise.

> **Context (2026-06-06 re-audit):** The UI has now been **migrated from Views + Fragments to
> Jetpack Compose**. The old `fragment/` and `adapter/` packages are empty leftovers; UI now lives
> in `screen/` (full screens) and `composables/` (reusable widgets), wired with Navigation-Compose
> from `composables/TacomaMusicPlayerApp.kt`. `MainViewModel` has been migrated off `LiveData` to
> `StateFlow` + `Channel`. **Section 0 below is the post-migration backlog** — the highest-value
> work right now. Later sections are the original backend audit; most of those files (util, repo,
> dao, service) were untouched by the UI migration, so they still apply — re-verify line numbers
> before acting.

---

## Table of Contents

0. [Post-Compose-Migration Opportunities](#0-post-compose-migration-opportunities) ⭐ **start here**
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

## 0. Post-Compose-Migration Opportunities

These are the improvements unlocked (or newly exposed) by moving to Compose. They are the
fastest path to less code and cleaner state.

### 🔴 Critical — Adopt a Design System (no `MaterialTheme`, hardcoded dp/colors everywhere)

**Files:** all of `screen/` and `composables/`

There is currently **no theme package and zero `MaterialTheme` usage**. Instead the UI hardcodes:

- ~185 raw `.dp` literals
- ~37 raw `.sp` literals
- ~70+ `Color.White` / `Color.Black` / `Color(0x…)` literals

This directly violates the `CLAUDE.md` UI guideline ("Use custom design system tokens (Colors,
Typography, Shapes) rather than hardcoded values") and makes dark mode, restyling, and visual
consistency nearly impossible. A partial helper already exists in `composables/ListItemStyle.kt` —
formalize it.

**Fix:** Create a `ui/theme/` package with `Color.kt`, `Type.kt`, `Shape.kt`, and a
`TacomaTheme { }` wrapper backed by `MaterialTheme`. Wrap the root in `TacomaMusicPlayerApp`.
Then replace literals with tokens:

```kotlin
// ❌ Before
Text(color = Color.White, fontSize = 16.sp)
Spacer(Modifier.height(8.dp))

// ✅ After
Text(color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
Spacer(Modifier.height(Dimens.SpacingSmall))
```

Do this incrementally, one screen at a time, starting with the most-reused tokens (spacing scale,
the two or three brand colors, the text styles).

---

### 🟡 High — Deduplicate the List/Grid Item Composables

**Files:** `composables/AlbumListItem.kt`, `AlbumGridItem.kt`, `PlaylistListItem.kt`,
`PlaylistGridItem.kt` (and `SongItem.kt` / `QueueSongItem.kt`)

`AlbumListItem` and `PlaylistListItem` are near-identical (artwork + title + subtitle + overflow
menu in a row), as are `AlbumGridItem` and `PlaylistGridItem`. Four files encode two layouts.

**Fix:** Extract two generic composables driven by parameters, not type:

```kotlin
@Composable
fun MediaListItem(
    modifier: Modifier = Modifier,
    artUri: Uri?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    menu: @Composable () -> Unit,
)

@Composable
fun MediaGridItem( /* same params */ )
```

Album- and playlist-specific behavior (menu contents, subtitle source) is passed in. This collapses
four files to two and means a styling change happens once.

---

### 🟡 High — Collapse `AlbumTab*` / `PlaylistTab*` Mirror Triplets into One Generic

**Files:**
- `viewmodel/AlbumTabViewModel.kt` ↔ `viewmodel/PlaylistTabViewModel.kt`
- `state/AlbumTabState.kt` ↔ `state/PlaylistTabState.kt`
- `screen/AlbumListScreen.kt` ↔ `screen/PlaylistScreen.kt`

A structural diff of the two ViewModels shows they differ only in the words "album"/"playlist" and
which repository flow they read. The two state classes are byte-for-byte equivalent in shape. The
two screens follow the same sort/layout-toggle + list/grid pattern.

**Fix:** Introduce a single parameterized layer:

```kotlin
data class GroupTabState(
    val items: List<MediaItem>,
    val sort: SortingUtil.SortingOption,
    val layout: LayoutType,
)

@HiltViewModel
class GroupTabViewModel @AssistedInject constructor(
    private val musicRepo: MusicRepository,
    @Assisted private val groupType: SongGroupType, // ALBUM or PLAYLIST
) : ViewModel() { /* one implementation */ }
```

A single `GroupListScreen(state, onSort, onLayout, itemContent)` composable then renders both pages.
This removes ~250 lines of duplicated ViewModel/state/screen code and guarantees the two pages stay
in sync. (Note: `PlaylistTabViewModel` currently declares its backing flow `private val` while
`AlbumTabViewModel` uses `private var` — exactly the drift a shared base prevents.)

---

### 🟡 High — Remove `Context` Parameters from ViewModels

**Files:** `viewmodel/MainViewModel.kt` (`setMusicPlayingPrefs`, `determineLoopingPref`,
`determineShufflePref`, `saveLoopingPref`, `saveShufflePref`),
`viewmodel/AlbumTabViewModel.kt` (`saveAlbumLayout`, `saveAlbumSorting`),
`viewmodel/PlaylistTabViewModel.kt` (`savePlaylistLayout`, `savePlaylistSorting`)

Nine ViewModel methods take a `context: Context` purely to reach DataStore. Passing `Context` (often
the Compose `LocalContext`) into ViewModel methods couples the ViewModel to the UI layer, risks
leaks, and makes these methods awkward to unit-test.

**Fix:** Inject the DataStore access object once. Either `@Inject` `DataStoreUtil` (backed by an
`@ApplicationContext`-scoped DataStore provided by Hilt) into each ViewModel, or move these reads/
writes behind the repository. Method signatures then drop the `Context` entirely:

```kotlin
fun saveAlbumLayout(layout: LayoutType) {
    viewModelScope.launch { dataStore.setAlbumLayout(layout) }
}
```

---

### 🟡 High — Type-Safe Navigation-Compose Routes

**File:** `composables/TacomaMusicPlayerApp.kt`

Navigation currently builds string routes from `ScreenType.route()` and calls
`navController.navigate(stringRoute)`. String routes are stringly-typed and error-prone.

**Fix:** Adopt type-safe Navigation-Compose (2.8+) with `@Serializable` route objects:

```kotlin
@Serializable object MusicChooser
@Serializable object PermissionDenied

NavHost(navController, startDestination = MusicChooser) {
    composable<MusicChooser> { MusicChooserScreen(...) }
    composable<PermissionDenied> { PermissionDeniedScreen(...) }
}
navController.navigate(PermissionDenied)
```

Arguments (if any are added later) become typed constructor parameters instead of string-encoded
query params.

---

### 🟢 Medium — Delete Dead Migration Leftovers

- `fragment/` and `fragment/pages/` are **empty directories** — delete them.
- `adapter/` is an **empty directory** — delete it (and confirm `adapter/diff/MediaItemDiffCallback`
  is gone or relocated; it was View-RecyclerView-only).
- `composables/PlaylistModPrompt.kt` (26 lines) is an unfinished placeholder — implement or delete.
- `view/CustomPlaylistModPrompt.kt` was listed as an empty placeholder pre-migration; confirm it is
  removed.
- Update `DEV-GUIDE.md` and `FILE-DIRECTORY.md` — both still describe the Views + Fragments
  architecture and list `fragment/`, `adapter/`, `view/`, and XML layouts as current. They are now
  the single biggest source of stale onboarding info.

---

### 🟢 Medium — Add `@Immutable` / `@Stable` to UI State & Data Classes

**Files:** `state/AlbumTabState.kt`, `state/PlaylistTabState.kt`, `data/SongGroup.kt`,
`data/DisplaySong.kt`, etc.

Compose skips recomposition for composables whose inputs are stable. Plain data classes that hold a
`List<MediaItem>` are treated as **unstable** (because `List` and `MediaItem` aren't provably
stable), forcing extra recompositions on every state emission.

**Fix:** Mark UI-facing state classes `@Immutable` (per `CLAUDE.md`'s recomposition guideline), and
prefer `ImmutableList<…>` from `kotlinx.collections.immutable` for list fields. Verify gains with
the Compose compiler stability report or Layout Inspector recomposition counts.

---

### 🟢 Low — Prefer `rememberSaveable` for UI-Local State

**Files:** `screen/` and `composables/` (currently only 2 `rememberSaveable` vs scattered `remember`)

Most state is correctly hoisted to ViewModels (good). For the small amount of genuinely UI-local
state (expanded/collapsed toggles, scroll-driven flags, in-progress text fields not yet committed),
prefer `rememberSaveable` over `remember` so it survives configuration changes and process death, as
called out in `CLAUDE.md`.

---

## 1. Architecture & Separation of Concerns

### 🔴 Critical — Split `MainViewModel` (God Object)

**File:** `viewmodel/MainViewModel.kt` (~1,429 lines — *grew during migration*)

`MainViewModel` handles at least eight distinct concerns: `MediaController`/`MediaBrowser` lifecycle,
playback controls, queue build/save/restore, shuffle logic, permission checks, screen navigation,
playlist CRUD, and search. This makes it impossible to unit-test individual concerns and makes the
class fragile to change.

**Recommended split:**

| New ViewModel | Responsibilities |
|---|---|
| `PlaybackViewModel` | `MediaController`/`MediaBrowser` setup, play/pause/skip/seek, loop mode, shuffle |
| `QueueViewModel` | Queue build (`addTracksSaveTrackOrder`), save, restore, current queue `StateFlow` |
| `PlaylistViewModel` | Playlist CRUD, `availablePlaylists` `StateFlow` |
| `PermissionViewModel` | `READ_MEDIA_AUDIO` check, `screenState` navigation event |
| `MainViewModel` (thin) | Wire the above together; expose only what `MusicChooserScreen` needs directly |

In a Compose world, individual screens can collect from the narrower ViewModels via
`hiltViewModel()`, which also reduces unnecessary recomposition scope.

---

### 🟡 High — `SongGroup.songs` Should Be Immutable

**File:** `data/SongGroup.kt`

`songs` is declared `var` to allow in-place shuffling. Mutating shared state in a data class breaks
referential integrity when multiple observers hold the same `SongGroup` instance — and defeats
Compose stability (see §0).

**Fix:** Change to `val`. On shuffle, produce a new `SongGroup` via `copy(songs = shuffled)` instead
of mutating the original.

---

## 2. State Management

### 🔴 Critical — Enable Hilt Injection on `SongListViewModel`

**File:** `viewmodel/SongListViewModel.kt` (line ~25)

`@HiltViewModel` is still commented out (`//@HiltViewModel`), forcing host screens to instantiate the
ViewModel manually instead of via `hiltViewModel()`. This breaks the Hilt DI contract and prevents
proper scoping.

**Fix:** Re-enable `@HiltViewModel` + `@Inject constructor(...)` and obtain it with `hiltViewModel()`
in `SongListScreen`.

---

### ✅ Done — Standardize on `StateFlow`

`MainViewModel` has been migrated from `LiveData` to `MutableStateFlow` (continuous state) plus
`Channel` (one-off events like navigation, keyboard-hide, clear-queue). `SongListViewModel`,
`AlbumTabViewModel`, and `PlaylistTabViewModel` are `StateFlow`-only. Screens collect with
`collectAsStateWithLifecycle()` (16 call sites). **This item from the previous audit is resolved.**

**Remaining nit:** confirm every exposed `StateFlow` has a non-null initial value so a screen never
renders a `null` flash on first composition (several are typed `StateFlow<T?>` with `null` initial —
e.g. `shuffleMode`, `loopMode`, `currentSongGroup` — make sure each screen handles the `null`/loading
case explicitly rather than crashing or showing empty content).

---

### 🟡 High — Async Callback State (Pending Image Selection)

**File:** `screen/AlbumListScreen.kt` / `screen/SongListScreen.kt` (was `AlbumListFragment` /
`SongListFragment` pre-migration)

The pre-migration audit flagged mutable fragment fields (`albumCustomImageName`, `selectedAlbumName`)
holding state across the uCrop `ActivityResult` boundary, causing the wrong image to be applied if
the user re-selects mid-crop. **Re-verify how this is handled now in Compose** — the uCrop launch is
likely a `rememberLauncherForActivityResult`. The selected target must live in the ViewModel as a
`StateFlow<PendingImageSelection?>`, set when an album is chosen and cleared when the crop result is
applied — not in a `remember {}` that can desync from the in-flight crop.

---

## 3. Concurrency & Threading

> These files were not touched by the UI migration. Verify line numbers, then act.

### 🔴 Critical — Missing `Dispatchers.IO` in `MusicRepositoryImpl`

**File:** `repository/MusicRepositoryImpl.kt`

Several `suspend` functions perform Room queries or disk I/O without `withContext(Dispatchers.IO)`
(`searchMusic()`, `createInitialQueueIfEmpty()`, `getSongsByAlbum()`, `getSongsByArtist()`). Room
suspend functions won't crash off-thread, but the work runs on the caller's dispatcher (often Main),
causing jank.

**Fix:** Wrap each DB/disk call in `withContext(Dispatchers.IO) { ... }`.

---

### 🟡 High — `pendingSeek` Race Condition in `MusicService`

**File:** `service/MusicService.kt`

`pendingSeek: Int?` is written from `onAddMediaItems()` (callable from a binder thread) and read from
`PlayerEventListener` callbacks. The comment claims "main thread only" but nothing enforces it.

**Fix:** Annotate write sites with `@MainThread`, add a debug `Looper` assertion, or guard with
`AtomicInteger`.

---

## 4. Null Safety & Crash Risks

> Backend/util files — re-verify each line number against current code.

### 🔴 Critical — Verify SQL Bug: `getAllSongsFromArtist`

**File:** `database/dao/SongDao.kt` (~line 43)

The previous audit found this `@Query` selecting `WHERE album_title = :artist` instead of
`WHERE song_artist = :artist`, silently returning wrong results. **Confirm the `@Query` annotation
above the function** and fix to `song_artist` if still wrong.

---

### 🔴 Critical — Unguarded Index Access

- `repository/MusicRepositoryImpl.kt` — `foundSongs[0]` without an empty-list check → use
  `foundSongs.firstOrNull() ?: return …`.
- `util/SortingUtil.kt` — `timestamps[0]` / `timestamps[1]` from a `split(":")` → use `getOrNull`.
- `util/MediaItemUtil.kt` — `mediaItem.mediaId` (nullable) used with `.split()` directly → guard with
  `?: return null`.
- `util/UtilImpl.kt` — `uri.path.toString()` turns `null` into the literal `"null"` → use
  `uri.path ?: return`.

*(The `QueueListAdapter.indexOfFirst` crash from the prior audit is moot — that adapter was deleted in
the migration. Confirm the equivalent guard exists in `QueueSongItem` / `CurrentQueueScreen`'s
playing-indicator logic.)*

---

### 🟢 Low — Remove Remaining `!!` Double-Bangs

Three `!!` assertions remain in `app/src/main/java`. `CLAUDE.md` says avoid them "at all costs."
Replace with `?.`, `?:`, or explicit null handling.

---

## 5. Resource Leaks

> Backend files — unaffected by UI migration.

### 🔴 Critical — `MediaMetadataRetriever` Never Released

**Files:** `util/MediaStoreUtil.kt`, `util/UtilImpl.kt`

`MediaMetadataRetriever` holds a native handle; if `setDataSource` or a later call throws, `release()`
is never reached.

**Fix:** Wrap in `try { … } finally { retriever.release() }` (or `use {}` — it is `AutoCloseable` on
API 29+; min SDK here is 30, so `use {}` is safe).

---

### 🟡 High — ExoPlayer Listener Accumulates on Service Restart

**File:** `service/MusicService.kt`

`player.addListener(PlayerEventListener())` is never paired with `removeListener()` in `onDestroy()`.
Store the instance and remove it on teardown.

---

### 🟡 High — Temp Files Created but Never Deleted

**File:** `util/UtilImpl.kt` (`uriToFile`)

`File.createTempFile(...)` output is never cleaned up. Delete after use, or add a periodic cache-prune
pass in `CatalogMusicWorker`.

---

## 6. Error Handling

### 🔴 Critical — `CatalogMusicWorker` Swallows Exceptions

**File:** `worker/CatalogMusicWorker.kt`

`doWork()` has no try-catch; a thrown exception fails the coroutine silently. Wrap `catalogMusic()` in
`try/catch`, log with Timber, and return `Result.failure()`.

---

### 🔴 Critical — `MusicService.onPlayerError()` Is Empty

**File:** `service/MusicService.kt`

Playback errors (corrupt file, unsupported codec, revoked URI) are silently ignored. Log them and emit
an error state the UI can surface (Snackbar). For recoverable errors call `player.prepare()`.

---

### 🟡 High — `MediaController`/`MediaBrowser` Future Has No Error Handling

**File:** `viewmodel/MainViewModel.kt` (~line 1005)

`controllerFuture.get()` inside the listener has no try-catch; a `CancellationException` /
`ExecutionException` leaves `_mediaController` permanently null. Wrap and emit a failure/retry state.

---

### 🟡 High — `workManager.cancelAllWork()` Is Too Broad

**File:** `activity/MainActivity.kt`

Cancels every enqueued worker, not just cataloging. Use `cancelUniqueWork("catalog_music")` matching
the enqueue name.

---

## 7. Code Duplication (DRY)

> The pre-migration adapter/fragment duplication items (`SongListAdapter` built 3×, queue adapter
> rebuilt twice, RecyclerView DiffUtil) are **obsolete** — those classes were deleted. The live
> duplication targets are now in **§0** (item composables, Album/Playlist triplets). One backend item
> survives:

### 🟡 High — Artwork URI Resolution Logic Duplicated

**File:** `util/MediaItemUtil.kt`

`determineArtUri()`'s logic (check `useCustomArt`, resolve custom vs. original path) appears to be
re-implemented inside more than one `MediaItem` construction method. Consolidate so every construction
path calls the single `determineArtUri()`.

---

## 8. Modern Android APIs

### ✅ Likely Done — Permission Handling

The pre-migration audit flagged the deprecated `onRequestPermissionsResult()` override in
`MainActivity`. With the Compose migration, `MainActivity` is now ~101 lines and uses `setContent`.
**Verify** permissions now go through `rememberLauncherForActivityResult` /
`ActivityResultContracts.RequestPermission()`; if any `onRequestPermissionsResult` / `REQUEST_CODE_*`
remnants survive, remove them.

---

### ✅ Done — RecyclerView DiffUtil

Obsolete — RecyclerView adapters are gone. Compose `LazyColumn`/`LazyVerticalGrid` handle diffing via
stable `key = { it.mediaId }` lambdas. **Verify** every `items(...)` call in `screen/` passes a stable
`key` so item identity survives reorders (important for the drag-to-reorder queue).

---

## 9. Documentation (APP-COMMENTS.md Compliance)

### 🟡 High — Refresh `DEV-GUIDE.md` & `FILE-DIRECTORY.md` for Compose

Both guides describe the old Views + Fragments architecture (they list `fragment/`, `adapter/`,
`view/`, ViewBinding, and every XML layout as current). Update the tech-stack table, the package
tree (`screen/`, `composables/`), the navigation section (Navigation-Compose, not programmatic
`NavController.createGraph`), and remove the now-empty packages. This is the highest-leverage doc fix.

---

### 🟡 High — TODO Comments Without Ticket References

Per `APP-COMMENTS.md §9`, bare `// TODO` is an anti-pattern. Several remain (e.g.
`MainViewModel.playAlbum()` carries `//TODO I don't think this is working...`). Resolve, or convert to
`// TODO(#123): …`.

---

### 🟡 High — Commented-Out Production Code

**File:** `repository/MusicRepositoryImpl.kt`

`removeSongsFromPlaylist` (declared on the interface) was commented out. Implement it or delete the
block — git covers recovery.

---

### 🟡 High — `@Preview` Coverage & Class-Level KDoc

- 31 `@Preview`s exist — good. Ensure each new `screen/` and `composables/` file has at least one
  preview with dummy data (`CLAUDE.md` guideline), especially the large screens
  (`SongListScreen`, `PlaylistScreen`).
- Add missing class/composable KDoc per `APP-COMMENTS.md §2.4 / §5` to: `MainActivity`,
  `CatalogMusicWorker`, `MusicService`, and any `screen`/`composable` lacking a top-level doc that
  notes stateless-vs-stateful and documents each state param / lambda callback.

---

### 🟡 High — Hardcoded Debug File Path in Production Code

**File:** `util/UtilImpl.kt` (`drawMp3agicBitmap`)

A hardcoded path to a local file (`/storage/emulated/0/Music/...`) remains in apparent debug code.
Delete it or gate behind `BuildConfig.DEBUG`.

---

## 10. Minor / Low Priority

### 🟢 Low — `AlbumTabViewModel._albums` Declared as `var`

Assigned once, never reassigned — declare `private val`. (`PlaylistTabViewModel` already uses `val`;
a shared `GroupTabViewModel` per §0 removes the inconsistency entirely.)

---

### 🟢 Low — Sentinel Strings in `SongData` Should Be Constants

**File:** `data/SongData.kt`

`"null"` and `"UNKNOWN"` sentinels should be `const val` in a companion object so comparisons aren't
duplicated.

---

### 🟢 Low — `Const.kt` Should Be Organized by Domain

Flat companion object — group into nested `object MediaIds`, `object DataStoreKeys`,
`object PlaylistNames`, etc.

---

### 🟢 Low — Magic Numbers Should Be Named Constants

Pager offscreen limits, swipe/fling velocity thresholds, and animation durations (now in
`MusicChooserScreen` / `MiniPlayer` / `MusicPlayingScreen`) should be named `private const val`s or
design-system tokens (§0).

---

*Last audited: 2026-06-06 (post-Compose-migration re-audit). Prior backend audit: 2026-05-25.*
