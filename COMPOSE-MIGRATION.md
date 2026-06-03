# Compose Migration Guide

This document walks through migrating the Tacoma Music Player from Views + Fragments + ViewBinding to full Jetpack Compose. The approach is **inside-out and incremental** — leaf composables first, container screens last, with the app staying buildable and runnable after every phase.

---

## Current State

| Area | Status |
|---|---|
| Compose BOM + deps | ✅ Already configured in `app/build.gradle.kts` |
| `buildFeatures { compose = true }` | ✅ Already enabled |
| Leaf composables (`composables/`) | ✅ 16 files (~2 046 LOC) already written |
| Fragments + ViewBinding | Active for all 7 fragments |
| RecyclerView adapters | Active for all 8 adapters |
| Custom Views | Active for all 7 custom views |
| `MainActivity` | ViewBinding + NavHostFragment |

The composables that are already written cover all list items, dialogs, prompts, the navigation control bar, and the song-group info header. They just aren't wired into the live fragments yet.

---

## Strategy

```
Phase 1  Wire existing composables into fragments via ComposeView
Phase 2  Replace simple fragments with full Compose screens
Phase 3  Replace list-page fragments + RecyclerView adapters with Compose
Phase 4  Replace the container fragment (ViewPager2 → HorizontalPager)
Phase 5  Replace MainActivity with setContent { }
```

Each phase ends with a working, shippable build. Never let the app stop building between phases.

---

## Phase 1 — Wire Existing Composables into Fragments

**Goal:** Use the composables that already exist by embedding them in the still-living fragments via `ComposeView`. This removes the custom View wrappers and their XML layouts without touching fragment lifecycles yet.

### 1a. `PlayerDisplayFragment` — NavigationControl

Replace the `CustomNavigationControl` view with the `NavigationControl` composable.

**Steps:**
1. In `player_display_fragment.xml`, replace the `CustomNavigationControl` view element with a `androidx.compose.ui.platform.ComposeView` (give it the same id, e.g., `@+id/navigationControl`).
2. In `PlayerDisplayFragment.kt`:
   - Remove all calls to `binding.navigationControl.setQueueButtonOnClick(…)` etc.
   - Call `binding.navigationControl.setContent { NavigationControl(currentPage = …, onPageSelected = { … }) }` where `currentPage` is driven by the current `ViewPager2` page index mapped through `PageType.determinePageFromPosition()`.
3. Delete `CustomNavigationControl.kt` and `view_custom_navigation_control.xml` (+ landscape variant).

**Composable to use:** `composables/NavigationControl.kt`

**Verify:** App launches, tab bar renders and tapping each tab changes the pager page.

---

### 1b. `PlayerDisplayFragment` — SongGroupInfoView

The `CustomSongGroupInfoView` used in song-group detail views can be replaced the same way.

**Composable to use:** `composables/SongGroupInfoView.kt`

**Delete:** `CustomSongGroupInfoView.kt`, `custom_song_group_info_view.xml`

---

### 1c. Dialogs / Overlays inside fragments

Replace each custom-view overlay with a `ComposeView` calling the already-written composable:

| Custom View (delete after) | Composable to use | XML to delete |
|---|---|---|
| `CustomInputTextPrompt` | `composables/InputTextPrompt.kt` | `view_custom_input_text_prompt.xml` |
| `CustomMultiSelectPrompt` | `composables/MultiSelectPrompt.kt` | `view_custom_multi_select_prompt.xml` |
| `CustomSortingPrompt` | `composables/SortingPrompt.kt` | `view_custom_sorting_prompt.xml` |
| `CustomSettingsPrompt` | `composables/SettingsPrompt.kt` | `view_custom_settings_prompt.xml` |
| `CustomPlaylistPrompt` | `composables/PlaylistPrompt.kt` + `composables/PlaylistPromptItem.kt` | `view_custom_playlist_prompt.xml` |
| `CustomInformationScreen` | `composables/InformationScreen.kt` | `view_custom_information_screen.xml` (+ landscape) |

For each: find the fragment that creates the custom view, swap to a `ComposeView` with the matching composable, delete the old class + XML.

**Delete after phase 1c complete:** `view/` package can be entirely removed.

---

## Phase 2 — Migrate Simple Fragments to Full Compose Screens

**Goal:** Replace each fragment with a Composable function. The fragment is kept as a thin shell that calls `ComposeView` for the full layout, then the fragment class itself is removed once the parent is also on Compose (happens in Phase 4–5).

For now, use this interop pattern inside `onCreateView`:

```kotlin
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MyScreen(state = state, onAction = viewModel::doSomething)
        }
    }
}
```

Collect ViewModel state inside `setContent` using `collectAsStateWithLifecycle()`. Remove all `viewLifecycleOwner.lifecycleScope.launch { … collect { … } }` blocks from the fragment — the composable handles observation.

---

### 2a. `PermissionDeniedFragment`

Simplest fragment (29 lines). Create `composables/PermissionDeniedScreen.kt` with a `@Composable fun PermissionDeniedScreen(onOpenSettings: () -> Unit)`. Replace `onCreateView` with the `ComposeView` pattern above.

**Delete:** `fragment_permission_denied.xml` (+ landscape variant)

---

### 2b. `MusicPlayingFragment`

Full-screen player. Create `composables/MusicPlayingScreen.kt`.

**Media3 / ExoPlayer interop:** The Media3 `StyledPlayerView` has no Compose equivalent yet. Wrap it in `AndroidView` for now:

```kotlin
AndroidView(
    factory = { ctx ->
        StyledPlayerView(ctx).also { it.player = controller }
    },
    update = { it.player = controller }
)
```

All other controls (play/pause button, skip, shuffle, loop, seek bar, artwork, metadata text) can be native Composables observing the ViewModels' `StateFlow`s via `collectAsStateWithLifecycle()`.

**Delete:** `fragment_music_playing.xml` (+ landscape variant)

> **Deferred:** Replacing the `AndroidView`-wrapped `StyledPlayerView` with a fully Compose player UI is a separate task after the core migration completes.

---

### 2c. `CurrentQueueFragment`

Drag-to-reorder is the one hard part. Migrate the display and controls first using the already-written `QueueSongItem` composable inside a `LazyColumn`. Wire drag callbacks using `androidx.compose.foundation`'s `detectDragGesturesAfterLongPress` + a `ReorderableLazyColumn` (the `sh.calvin.reorderable` library is the current community standard and integrates cleanly with Media3's queue).

**Composable to use:** `composables/QueueSongItem.kt`

**Delete:** `fragment_current_queue.xml`, `QueueListAdapter.kt`, `adapter/diff/MediaItemDiffCallback.kt` (if unused elsewhere after this)

---

## Phase 3 — Migrate List Pages

Replace each list-page fragment's `RecyclerView` + adapter pair with a `LazyColumn` or `LazyVerticalGrid` using the already-written item composables.

### 3a. `SongListFragment` + `SongListAdapter`

Create `composables/SongListScreen.kt`. Use `LazyColumn` with `SongItem` for each track:

```kotlin
LazyColumn {
    items(songs, key = { it.mediaId }) { song ->
        SongItem(song = song, onSongClick = { … }, …)
    }
}
```

Multi-select state is already tracked in `SongListViewModel` — pass it down as a parameter.

**Composable to use:** `composables/SongItem.kt`, `composables/SongGroupInfoView.kt` (for the header)

**Delete:** `fragment_songlist.xml`, `SongListAdapter.kt`, `viewholder_song.xml`

---

### 3b. `AlbumListFragment` + `AlbumListAdapter` / `AlbumGridAdapter`

Create `composables/AlbumListScreen.kt`. Toggle between `LazyColumn` (list) and `LazyVerticalGrid` (grid) based on `AlbumTabState.layoutType`:

```kotlin
when (state.layoutType) {
    LayoutType.LINEAR_LAYOUT -> LazyColumn { items(albums) { AlbumListItem(…) } }
    LayoutType.TWO_GRID_LAYOUT -> LazyVerticalGrid(columns = Fixed(2)) { items(albums) { AlbumGridItem(…) } }
}
```

**Composables to use:** `composables/AlbumListItem.kt`, `composables/AlbumGridItem.kt`

**Delete:** `fragment_albumlist.xml`, `AlbumListAdapter.kt`, `AlbumGridAdapter.kt`, `viewholder_album.xml`, `viewholder_album_grid_layout.xml`

---

### 3c. `PlaylistFragment` + `PlaylistAdapter` / `PlaylistGridAdapter`

Same pattern as albums.

**Composables to use:** `composables/PlaylistListItem.kt`, `composables/PlaylistGridItem.kt`

**Delete:** `fragment_playlist.xml`, `PlaylistAdapter.kt`, `PlaylistGridAdapter.kt`, `PlaylistPromptAdapter.kt`, `viewholder_playlist.xml`, `viewholder_playlist_grid_layout.xml`, `viewholder_playlist_prompt.xml`

---

## Phase 4 — Migrate the Container (`PlayerDisplayFragment`)

**Goal:** Replace `ViewPager2` + `ScreenSlidePagerAdapter` with `HorizontalPager` from `androidx.compose.foundation.pager`.

Add the dependency if not yet present:
```toml
# gradle/libs.versions.toml
[libraries]
androidx-compose-foundation = { module = "androidx.compose.foundation", version.ref = "composeBom" }
```
(It is already included transitively via `compose-ui`, but explicit is cleaner.)

**Pattern:**

```kotlin
val pagerState = rememberPagerState(initialPage = PageType.PLAYER_PAGE.ordinal) { PageType.entries.size }

HorizontalPager(state = pagerState) { page ->
    when (PageType.determinePageFromPosition(page)) {
        PageType.QUEUE_PAGE    -> CurrentQueueScreen(…)
        PageType.PLAYER_PAGE   -> MusicPlayingScreen(…)
        PageType.PLAYLIST_PAGE -> PlaylistScreen(…)
        PageType.ALBUM_PAGE    -> AlbumScreen(…)
        PageType.SONG_PAGE     -> SongListScreen(…)
    }
}

NavigationControl(
    currentPage = PageType.determinePageFromPosition(pagerState.currentPage),
    onPageSelected = { page ->
        scope.launch { pagerState.animateScrollToPage(page.ordinal) }
    },
    …
)
```

Programmatic navigation from `MainViewModel.navigateToPage` (currently a `Channel<PageType>`) drives `pagerState.animateScrollToPage(…)` via `LaunchedEffect`.

All 5 page composables are kept "alive" by `HorizontalPager`'s `beyondBoundsPageCount = PageType.entries.size - 1`, matching the current `offscreenPageLimit = 4`.

At this point, `PlayerDisplayFragment` becomes a `ComposeView`-only fragment (or is removed in Phase 5).

**Delete:** `player_display_fragment.xml` (+ landscape), `ScreenSlidePagerAdapter.kt`

---

## Phase 5 — Migrate `MainActivity`

**Goal:** Replace `ActivityMainBinding` + `NavHostFragment` with `setContent { }` + Compose `NavHost`.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
                composable(ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
                    MusicChooserScreen(navController = navController)
                }
                composable(ScreenType.PERMISSION_DENIED_SCREEN.route()) {
                    PermissionDeniedScreen(onOpenSettings = { … })
                }
            }
        }
    }
}
```

`MusicChooserScreen` is the Compose equivalent of `PlayerDisplayFragment` — it contains the `HorizontalPager` + `NavigationControl` built in Phase 4.

Navigation events from `MainViewModel.screenState` (a `Channel<ScreenData>`) are consumed via a `LaunchedEffect` that calls `navController.navigate(…)`.

Permission checks and `CatalogMusicWorker` enqueue stay in `onCreate` before `setContent`, or are moved into `LaunchedEffect(Unit)` inside the root composable.

**Delete:** `activity_main.xml`, `fragment/PlayerDisplayFragment.kt`, `fragment/PermissionDeniedFragment.kt`, `fragment_permission_denied.xml` (if not already deleted in Phase 2a)

After this phase, the `fragment/` and `adapter/` packages can be fully removed.

---

## Interop Notes

### Edge-to-edge Insets

Replace all `ViewCompat.setOnApplyWindowInsetsListener` calls with Compose `WindowInsets` consumption:

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.systemBars
) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues)) { … }
}
```

Call `enableEdgeToEdge()` in `MainActivity.onCreate` before `setContent`. Remove all manual `View.setPadding` inset adjustments.

### Media3 PlayerView (AndroidView)

Keep the `AndroidView` wrapper for `StyledPlayerView` until a post-migration task specifically addresses the full Compose player UI. The `AndroidView` approach is fully supported and performant.

### Drag-to-Reorder Queue

The `sh.calvin.reorderable:reorderable` library (available on Maven Central) provides a `ReorderableColumn` / `ReorderableLazyColumn` that integrates with `ItemTouchHelper`-style callbacks. Wire its `onMove` callback to `MainViewModel`'s existing queue-reorder method.

```toml
# gradle/libs.versions.toml
[versions]
reorderable = "2.4.3"

[libraries]
reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }
```

### Coil in Compose

`coil-compose` is already a dependency. Use `AsyncImage` everywhere artwork is displayed:

```kotlin
AsyncImage(
    model = artUri,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(64.dp)
)
```

Remove all direct `Coil.load(…)` calls that targeted Views.

---

## Code Conventions for New Composables

Follow `APP-COMMENTS.md` Section 5 for all new composable KDoc. Key points:
- Declare whether the composable is **stateless** (all state passed in) or **stateful** (uses `remember`/`rememberSaveable`).
- Document every `@param` whose purpose isn't obvious from its type and name.
- Document every lambda callback with `@param`.
- Add inline comments for animation state derivations or non-obvious rendering conditions.

---

## File Deletion Checklist

Track deleted files here as migration progresses. A file in this list should not exist in the repo once the corresponding phase is complete.

### Phase 1
- [x] `view/CustomNavigationControl.kt`
- [x] `res/layout/view_custom_navigation_control.xml`
- [x] `res/layout-land/view_custom_navigation_control.xml`
- [x] `view/CustomSongGroupInfoView.kt`
- [x] `res/layout/custom_song_group_info_view.xml`
- [x] `view/CustomInputTextPrompt.kt`
- [x] `res/layout/view_custom_input_text_prompt.xml`
- [x] `view/CustomMultiSelectPrompt.kt`
- [x] `res/layout/view_custom_multi_select_prompt.xml`
- [x] `view/CustomSortingPrompt.kt`
- [x] `res/layout/view_custom_sorting_prompt.xml`
- [x] `view/CustomSettingsPrompt.kt`
- [x] `res/layout/view_custom_settings_prompt.xml`
- [x] `view/CustomPlaylistPrompt.kt`
- [x] `res/layout/view_custom_playlist_prompt.xml`
- [x] `view/CustomInformationScreen.kt`
- [x] `res/layout/view_custom_information_screen.xml`
- [x] `res/layout-land/view_custom_information_screen.xml`
- [x] `view/CustomPlaylistModPrompt.kt`

### Phase 2
- [x] `res/layout/fragment_permission_denied.xml`
- [x] `res/layout-land/fragment_permission_denied.xml`
- [ ] `res/layout/fragment_music_playing.xml`
- [ ] `res/layout-land/fragment_music_playing.xml`
- [ ] `res/layout/fragment_current_queue.xml`
- [ ] `adapter/QueueListAdapter.kt`

### Phase 3
- [ ] `res/layout/fragment_songlist.xml`
- [ ] `adapter/SongListAdapter.kt`
- [ ] `res/layout/viewholder_song.xml`
- [ ] `res/layout/fragment_albumlist.xml`
- [ ] `adapter/AlbumListAdapter.kt`
- [ ] `adapter/AlbumGridAdapter.kt`
- [ ] `res/layout/viewholder_album.xml`
- [ ] `res/layout/viewholder_album_grid_layout.xml`
- [ ] `res/layout/fragment_playlist.xml`
- [ ] `adapter/PlaylistAdapter.kt`
- [ ] `adapter/PlaylistGridAdapter.kt`
- [ ] `adapter/PlaylistPromptAdapter.kt`
- [ ] `res/layout/viewholder_playlist.xml`
- [ ] `res/layout/viewholder_playlist_grid_layout.xml`
- [ ] `res/layout/viewholder_playlist_prompt.xml`

### Phase 4
- [ ] `res/layout/player_display_fragment.xml`
- [ ] `res/layout-land/player_display_fragment.xml`
- [ ] `adapter/ScreenSlidePagerAdapter.kt`

### Phase 5
- [ ] `res/layout/activity_main.xml`
- [ ] `fragment/PlayerDisplayFragment.kt`
- [ ] `fragment/PermissionDeniedFragment.kt`
- [ ] `fragment/MusicPlayingFragment.kt`
- [ ] `fragment/CurrentQueueFragment.kt`
- [ ] `fragment/pages/AlbumListFragment.kt`
- [ ] `fragment/pages/PlaylistFragment.kt`
- [ ] `fragment/pages/SongListFragment.kt`
- [ ] `adapter/diff/MediaItemDiffCallback.kt` (if unused)

---

## Dependencies Already in Place

No new dependencies are needed for Phases 1–4. For Phase 2c (drag-to-reorder) and Phase 4 (if the pager BOM entry needs an explicit version), add:

```toml
# gradle/libs.versions.toml — add only if needed
reorderable = { module = "sh.calvin.reorderable:reorderable", version = "2.4.3" }
```

Everything else — Material3, activity-compose, coil-compose, the Compose BOM, and `compose-foundation` (for `HorizontalPager`) — is already declared.
