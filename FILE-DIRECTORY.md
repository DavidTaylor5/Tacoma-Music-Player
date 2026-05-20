# File Directory

All source paths are relative to `app/src/main/java/com/andaagii/tacomamusicplayer/` unless noted otherwise. Resource paths are relative to `app/src/main/res/`.

---

## Build & Project Config

| File | Purpose | Modify when |
|---|---|---|
| `build.gradle.kts` (root) | Root Gradle build — declares plugin versions shared across modules | Adding a new Gradle plugin or changing a root-level version |
| `app/build.gradle.kts` | App module build config: SDK versions, signing, build types, APK naming, feature flags | Bumping SDK/version, adding a dependency, changing build types or ProGuard config |
| `settings.gradle.kts` | Declares the project name and included modules; configures plugin/dependency repositories | Adding a new Gradle module or a new repository |
| `gradle/libs.versions.toml` | Version catalog — single source of truth for all library versions and aliases | Upgrading any library version or adding a new dependency |
| `gradle.properties` | Gradle performance flags (daemon, parallel builds, caching) and JVM heap size | Tuning build performance or adding project-wide Gradle properties |
| `local.properties` | Machine-local paths (SDK location, signing credentials). Not committed to git | Setting up a new dev machine — never commit secrets here |
| `app/src/main/AndroidManifest.xml` | Declares the app's components (Activity, Service, Provider), permissions, and Android Auto metadata | Adding a new component, changing a permission, modifying Android Auto or FileProvider setup |

---

## Application Entry Point

| File | Purpose | Modify when |
|---|---|---|
| `TacomaMusicPlayerApplication.kt` | `@HiltAndroidApp` Application class. Configures Timber logging (file-based in debug) and WorkManager via `Configuration.Provider` | Changing logging strategy, WorkManager configuration, or adding app-wide initialization logic |

---

## activity/

| File | Purpose | Modify when |
|---|---|---|
| `MainActivity.kt` | The sole Activity. Sets up the NavController with a programmatic nav graph, handles gesture detection (double-tap, swipe), permission results, music cataloging trigger, and DataStore preference reads | Changing navigation destinations, adding new gesture handling, modifying permission request flow, or changing app startup behavior |

---

## adapter/

RecyclerView and ViewPager2 adapters. Each is passed click/action lambdas from its host Fragment — they contain no business logic.

| File | Purpose | Modify when |
|---|---|---|
| `AlbumListAdapter.kt` | `ListAdapter` for albums in linear list layout. Binds `viewholder_album.xml`. Handles album tap, play icon tap, and popup menu options | Changing album list row UI behavior or menu options |
| `AlbumGridAdapter.kt` | `ListAdapter` for albums in 2-column grid layout. Binds `viewholder_album_grid_layout.xml` | Changing album grid cell UI behavior or menu options |
| `PlaylistAdapter.kt` | `ListAdapter` for playlists in linear list layout. Binds `viewholder_playlist.xml` | Changing playlist list row UI behavior or menu options |
| `PlaylistGridAdapter.kt` | `ListAdapter` for playlists in 2-column grid layout. Binds `viewholder_playlist_grid_layout.xml` | Changing playlist grid cell UI behavior or menu options |
| `PlaylistPromptAdapter.kt` | `RecyclerView.Adapter` for the add-to-playlist dialog. Shows checkboxes next to playlist names | Changing how playlists are displayed in the add-to-playlist dialog |
| `QueueListAdapter.kt` | `RecyclerView.Adapter` for the current queue. Supports drag-to-reorder and shows a playing animation on the active track | Changing queue row layout, drag behavior, or the playing indicator |
| `SongListAdapter.kt` | `RecyclerView.Adapter` for the all-songs list and song-group detail views. Handles song tap, album tap, multi-select, and popup menu | Changing song row behavior, multi-select logic, or song menu options |
| `ScreenSlidePagerAdapter.kt` | `FragmentStateAdapter` for the `ViewPager2` in `PlayerDisplayFragment`. Returns the correct page Fragment for each index | Adding, removing, or reordering pages in the main swipe layout |
| `diff/MediaItemDiffCallback.kt` | `DiffUtil.ItemCallback<MediaItem>` used by `ListAdapter` instances to compute list diffs efficiently | Changing how `MediaItem` identity or content equality is determined |

---

## constants/

| File | Purpose | Modify when |
|---|---|---|
| `Const.kt` | App-wide string/int constants: special playlist names (queue, original order), shuffle strings, DataStore keys, etc. | Adding a new named constant that needs to be shared across multiple classes |

---

## data/

Plain data classes with no business logic. These are the domain model types passed between layers.

| File | Purpose | Modify when |
|---|---|---|
| `SongData.kt` | Serializable representation of a single track (URI, title, album, artist, artwork, duration). Used for queue persistence via Moshi JSON | Adding or changing a field that needs to be persisted in the queue |
| `SongGroup.kt` | Groups a `MediaItem` (the album/playlist header) with its list of track `MediaItem`s and a `SongGroupType` | Changing what data is bundled together when representing an album or playlist in the UI |
| `SearchData.kt` | Holds the three result buckets from a search query: songs, albums, and playlists | Changing the shape of search results |
| `DisplaySong.kt` | Wraps a `MediaItem` with a `showPlayIndicator` flag for the queue adapter | Changing what display-layer metadata is needed per queue row |
| `ScreenData.kt` | Wraps a `ScreenType` to represent a navigation event via LiveData | Changing navigation event structure |
| `AndroidAutoPlayData.kt` | Parsed result of an Android Auto media item ID: group type, title, position, and song title | Changing how Android Auto playback requests are interpreted |
| `ArtInfo.kt` | Holds original art path, custom art path, and which one to use | Changing the artwork selection model |
| `Playlist.kt` | Legacy Room `@Entity` for playlists (stores songs as a `PlaylistData` JSON blob). Superseded by `SongGroupEntity` + `SongGroupCrossRefEntity` | Not expected to be modified — kept for reference |
| `PlaylistData.kt` | Moshi-serializable wrapper around `List<SongData>` used by the legacy `Playlist` entity | Not expected to be modified |

---

## database/

| File | Purpose | Modify when |
|---|---|---|
| `PlayerDatabase.kt` | Room database singleton. Declares entities, version (currently 23), and uses `fallbackToDestructiveMigration()` | Adding a new entity, bumping the schema version, or changing migration strategy |
| `Converters.kt` | Room `TypeConverter`s for non-primitive column types (e.g., serializing enums or data classes to strings) | Adding a new entity field with a type Room cannot store natively |
| `entity/SongEntity.kt` | Room entity for a single track. PK is `searchDescription` (`songName_albumTitle_artist`). Stores URIs, artwork paths, and duration | Adding or changing a stored track field — remember to bump DB version |
| `entity/SongGroupEntity.kt` | Room entity for an album or playlist header. Stores type, title, artist, release year, and timestamps | Adding or changing album/playlist metadata — remember to bump DB version |
| `entity/SongGroupCrossRefEntity.kt` | Junction table linking a `SongGroupEntity` to its `SongEntity` rows, with a `position` column for ordering | Changing how playlist track ordering is stored |
| `dao/SongDao.kt` | DAO for `SongEntity`. Queries songs by album, artist, or search term; insert/upsert operations | Adding a new song query or changing how songs are fetched |
| `dao/SongGroupDao.kt` | DAO for `SongGroupEntity` and `SongGroupCrossRefEntity`. CRUD for albums and playlists; exposes `Flow`s for reactive updates | Adding a new album/playlist query, changing sort logic, or adding a new reactive observable |

---

## di/

Hilt dependency injection modules. Only touch these when changing how singletons are constructed or bound.

| File | Purpose | Modify when |
|---|---|---|
| `AppModule.kt` | Binds `MusicRepositoryImpl` to both `MusicRepository` and `MusicProviderRepository` as singletons | Changing which implementation backs the repository interfaces, or adding a new singleton binding |
| `DatabaseModule.kt` | Provides the `PlayerDatabase` singleton and extracts `SongDao` / `SongGroupDao` from it | Adding a new DAO or changing how the database is constructed |

---

## enumtype/

| File | Purpose | Modify when |
|---|---|---|
| `ScreenType.kt` | Navigation destinations (`MUSIC_CHOOSER_SCREEN`, `MUSIC_PLAYING_SCREEN`, `PERMISSION_DENIED_SCREEN`, `MUSIC_QUEUE_SCREEN`). Each value provides a `route()` string | Adding a new top-level screen |
| `PageType.kt` | ViewPager2 page indices (0–4): Queue, Player, Playlist, Album, Song. `determinePageFromPosition()` maps int → enum | Adding or reordering pages in the main swipe layout |
| `SongGroupType.kt` | Categorises a group of songs: `ALBUM`, `PLAYLIST`, `SEARCH_LIST`, `QUEUE`, `UNKNOWN` | Adding a new song collection type |
| `ShuffleType.kt` | `NOT_SHUFFLED` / `SHUFFLED`. Serialised to/from DataStore via `Const` strings | Changing shuffle behavior or adding a new shuffle mode |
| `LayoutType.kt` | `LINEAR_LAYOUT` / `TWO_GRID_LAYOUT`. Serialised to/from DataStore | Adding a new layout mode for albums or playlists |
| `QueueAddType.kt` | How tracks are added to the queue: `QUEUE_DONT_ADD`, `QUEUE_CLEAR_ADD`, `QUEUE_END_ADD` | Adding a new queue insertion strategy |

---

## factory/

| File | Purpose | Modify when |
|---|---|---|
| `MediaBrowserFactory.kt` | Builds the `MediaBrowser` instance used by `MainViewModel` to connect to `MusicService` | Changing how the media browser session is constructed or configured |

---

## fragment/

| File | Purpose | Modify when |
|---|---|---|
| `PlayerDisplayFragment.kt` | Host fragment for the main `ViewPager2`. Wires up `ScreenSlidePagerAdapter`, the `CustomNavigationControl`, and gesture handling (swipe-down to expand player). Observes `MainViewModel` for screen navigation events | Changing the top-level page layout, swipe gesture behavior, or how the nav control interacts with the pager |
| `PermissionDeniedFragment.kt` | Shown when `READ_MEDIA_AUDIO` permission is denied. Provides a button to open system settings | Changing the permission denied UI or adding a permission rationale |
| `pages/AlbumListFragment.kt` | Displays the albums page. Switches between `AlbumListAdapter` and `AlbumGridAdapter` based on layout preference. Observes `AlbumTabViewModel`. Handles album click (drill-down) and play-icon click | Changing album browsing UI, sort/layout toggle behavior, or album menu actions |
| `pages/PlaylistFragment.kt` | Displays the playlists page. Mirrors `AlbumListFragment` for playlists. Observes `PlaylistTabViewModel` | Changing playlist browsing UI, sort/layout behavior, or playlist menu actions |
| `pages/SongListFragment.kt` | Displays all songs or songs within a selected album/playlist. Handles multi-select for batch add-to-playlist. Observes `SongListViewModel` and `MainViewModel` | Changing the song list UI, multi-select behavior, or song-level menu actions |
| `pages/CurrentQueueFragment.kt` | Displays the current playback queue with drag-to-reorder. Observes `MainViewModel.currentlyPlayingSongs` | Changing queue display or drag-reorder behavior |
| `pages/MusicPlayingFragment.kt` | The mini/full player UI: album art, song metadata, playback controls (play/pause, skip, shuffle, repeat, seek bar) | Changing the player controls, layout, or artwork display |

---

## observer/

| File | Purpose | Modify when |
|---|---|---|
| `MusicContentObserver.kt` | `ContentObserver` registered on the MediaStore audio URI. Triggers a library re-catalog when the device's music files change | Changing when or how library refresh is triggered in response to MediaStore changes |

---

## provider/

| File | Purpose | Modify when |
|---|---|---|
| `AlbumArtFileProvider.kt` | `FileProvider` subclass that enables secure `content://` URI sharing of local artwork files with Android Auto (which cannot access raw `file://` URIs) | Changing the FileProvider authority or adding new shareable file paths (also update `res/xml/file_paths.xml`) |

---

## repository/

| File | Purpose | Modify when |
|---|---|---|
| `MusicProviderRepository.kt` | Read-only interface used by `MusicService` for Android Auto browsing and search. Defines queries for albums, artists, playlists, and songs | Adding a new read query that `MusicService` needs to serve Android Auto |
| `MusicRepository.kt` | Full interface extending `MusicProviderRepository`. Adds playlist CRUD, custom image management, and `Flow`-based observables for reactive UI | Adding a new data operation that ViewModels need to perform |
| `MusicRepositoryImpl.kt` | The single implementation of both repository interfaces, bound as a singleton by Hilt. Converts Room entities to `MediaItem`s via `MediaItemUtil`; coordinates `SongDao` and `SongGroupDao` | Implementing any new method added to either interface, or changing how entities are converted to `MediaItem`s |

---

## service/

| File | Purpose | Modify when |
|---|---|---|
| `MusicService.kt` | `MediaLibraryService` that owns the `ExoPlayer` instance and `MediaLibrarySession`. Implements the Android Auto browsing hierarchy (`onGetChildren`, `onGetLibraryRoot`), search (`onSearch`, `onGetSearchResult`), and playback request handling (`onAddMediaItems`). Also manages audio focus and "audio becoming noisy" | Changing the Android Auto browsing structure, adding new media session callbacks, modifying player configuration, or changing how playback requests from Auto are handled |

---

## state/

Lightweight data classes that combine multiple pieces of ViewModel state into a single `StateFlow` emission.

| File | Purpose | Modify when |
|---|---|---|
| `AlbumTabState.kt` | Bundles the album list, current sort order, and layout type into one object observed by `AlbumListFragment` | Adding new state that the albums page needs to react to as a unit |
| `PlaylistTabState.kt` | Same as `AlbumTabState` but for the playlists page | Adding new state that the playlists page needs to react to as a unit |

---

## util/

| File | Purpose | Modify when |
|---|---|---|
| `MediaItemUtil.kt` | Converts `SongEntity` and `SongGroupEntity` to `MediaItem` objects. Contains `determineArtUri()` which selects original vs. custom artwork. Also builds `MediaItem`s with Android Auto–compatible metadata | Changing how database entities map to `MediaItem`s, or how artwork URIs are resolved |
| `MediaStoreUtil.kt` | Queries Android MediaStore to discover audio files and albums on device storage. Used by `CatalogMusicWorker` during library scanning | Changing which MediaStore columns are fetched or how albums/tracks are discovered |
| `DataStoreUtil.kt` | Centralises all DataStore reads and writes: layout, sort order, shuffle mode, repeat mode, saved playback position and queue index | Adding or changing a persisted user preference or saved playback state key |
| `AppPermissionUtil.kt` | Helpers for checking and requesting `READ_MEDIA_AUDIO` (API 33+) and `READ_EXTERNAL_STORAGE` (API <33). Defines permission request codes | Adding a new runtime permission or changing permission request logic |
| `MenuOptionUtil.kt` | Defines the `MenuOption` sealed class/enum used across adapters to communicate which popup menu action was selected back to the Fragment | Adding a new popup menu action type |
| `SortingUtil.kt` | Pure sorting functions for `MediaItem` lists: by title, artist, release year, or creation date | Adding a new sort order or changing sort logic |
| `UtilImpl.kt` | Miscellaneous shared utilities: duration formatting, string helpers, and other one-off functions that don't belong elsewhere | Adding a small shared utility that multiple classes need |
| `FileLoggingTree.kt` | Timber `Tree` that writes log output to a file on device storage in debug builds | Changing the log file format, location, or retention policy |

---

## view/

Custom `View` subclasses used as reusable UI components across fragments. Each pairs with an XML layout file of the same name.

| File | XML layout | Purpose | Modify when |
|---|---|---|---|
| `CustomNavigationControl.kt` | `view_custom_navigation_control.xml` | Tab bar at the bottom of `PlayerDisplayFragment`. Highlights the active `PageType` and forwards tab taps to the `ViewPager2` | Changing the tab bar appearance or adding/removing tabs |
| `CustomSongGroupInfoView.kt` | `custom_song_group_info_view.xml` | Header view shown above a song list when browsing an album or playlist. Displays artwork, title, and play/menu buttons | Changing the song-group header layout or its button actions |
| `CustomPlaylistPrompt.kt` | `view_custom_playlist_prompt.xml` | Overlay dialog for adding selected songs to a playlist. Embeds `PlaylistPromptAdapter` | Changing the add-to-playlist dialog layout or behavior |
| `CustomInputTextPrompt.kt` | `view_custom_input_text_prompt.xml` | Generic two-button dialog with a text input field. Used for naming new playlists or renaming existing ones | Changing the text input dialog layout or button labels |
| `CustomMultiSelectPrompt.kt` | `view_custom_multi_select_prompt.xml` | Toolbar overlay shown during multi-select mode. Has a confirm (menu) icon and a close icon | Changing the multi-select toolbar appearance or button actions |
| `CustomSortingPrompt.kt` | `view_custom_sorting_prompt.xml` | Dialog for choosing sort order (title, artist, release year, etc.) | Changing the sorting dialog layout |
| `CustomSettingsPrompt.kt` | `view_custom_settings_prompt.xml` | Settings/options dialog (currently a shell for future settings) | Adding settings options |
| `CustomInformationScreen.kt` | `view_custom_information_screen.xml` | Empty-state / informational screen shown when a list has no content | Changing the empty-state appearance |
| `CustomPlaylistModPrompt.kt` | _(none)_ | Empty placeholder class. Not yet implemented | When implementing playlist modification dialog |

---

## viewmodel/

| File | Purpose | Modify when |
|---|---|---|
| `MainViewModel.kt` | Central ViewModel (~1,050 lines). Owns the `MediaController` / `MediaBrowser` connection to `MusicService`, all playback controls, queue build/save/restore, playlist CRUD, and search. Exposes `StateFlow`/`LiveData` for every piece of UI-relevant state | Adding new playback features, changing queue logic, adding playlist operations, or modifying how media session state is observed |
| `AlbumTabViewModel.kt` | Manages albums page state: loads albums from repository, applies sort order and layout preference, emits a single `StateFlow<AlbumTabState>` | Adding new state or behavior specific to the albums browsing page |
| `PlaylistTabViewModel.kt` | Same role as `AlbumTabViewModel` for the playlists page | Adding new state or behavior specific to the playlists browsing page |
| `SongListViewModel.kt` | Manages multi-select state for the song list: tracks selected songs and which playlists are checked for batch add-to-playlist | Changing multi-select behavior or the add-to-playlist selection flow |

---

## worker/

| File | Purpose | Modify when |
|---|---|---|
| `CatalogMusicWorker.kt` | `CoroutineWorker` (Hilt-injected via `@HiltWorker`). Scans device storage via `MediaStoreUtil`, extracts album art, and upserts discovered songs and albums into Room. Enqueued once on first launch and on manual refresh | Changing how the music library is scanned, what metadata is extracted, or how artwork is saved |

---

## res/layout/

Each layout file is listed with the class that inflates it.

| File | Used by |
|---|---|
| `activity_main.xml` | `MainActivity` — root layout containing the `NavHostFragment` |
| `player_display_fragment.xml` / `layout-land/` | `PlayerDisplayFragment` — contains `ViewPager2` and `CustomNavigationControl` |
| `fragment_albumlist.xml` | `AlbumListFragment` |
| `fragment_playlist.xml` | `PlaylistFragment` |
| `fragment_songlist.xml` | `SongListFragment` |
| `fragment_current_queue.xml` | `CurrentQueueFragment` |
| `fragment_music_playing.xml` / `layout-land/` | `MusicPlayingFragment` — player controls and album art |
| `fragment_permission_denied.xml` / `layout-land/` | `PermissionDeniedFragment` |
| `viewholder_album.xml` | `AlbumListAdapter` row |
| `viewholder_album_grid_layout.xml` | `AlbumGridAdapter` cell |
| `viewholder_playlist.xml` | `PlaylistAdapter` row |
| `viewholder_playlist_grid_layout.xml` | `PlaylistGridAdapter` cell |
| `viewholder_playlist_prompt.xml` | `PlaylistPromptAdapter` row |
| `viewholder_song.xml` | `SongListAdapter` row |
| `viewholder_queue_song.xml` | `QueueListAdapter` row |
| `view_custom_navigation_control.xml` / `layout-land/` | `CustomNavigationControl` |
| `custom_song_group_info_view.xml` | `CustomSongGroupInfoView` |
| `view_custom_playlist_prompt.xml` | `CustomPlaylistPrompt` |
| `view_custom_input_text_prompt.xml` | `CustomInputTextPrompt` |
| `view_custom_multi_select_prompt.xml` | `CustomMultiSelectPrompt` |
| `view_custom_sorting_prompt.xml` | `CustomSortingPrompt` |
| `view_custom_settings_prompt.xml` | `CustomSettingsPrompt` |
| `view_custom_information_screen.xml` / `layout-land/` | `CustomInformationScreen` |
| `custom_exo_player_update.xml` | Custom ExoPlayer surface layout |
| `custom_exo_controller_update.xml` | Custom ExoPlayer controls layout |

---

## res/menu/

XML popup/context menu definitions. Modify when adding or removing options from a popup menu.

| File | Used by |
|---|---|
| `album_options.xml` | Album row popup menu (play, add to queue, set image, etc.) |
| `multi_select_album_options.xml` | Multi-select mode menu for albums |
| `playlist_options.xml` | Playlist row popup menu |
| `multi_select_playlist_options.xml` | Multi-select mode menu for playlists |
| `queue_overall_options.xml` | Queue page header menu (clear queue, save as playlist, etc.) |
| `queue_song_options.xml` | Individual queue item popup menu |
| `songlist_album_options.xml` | Song row popup when viewing an album |
| `songlist_playlist_options.xml` | Song row popup when viewing a playlist |
| `songlist_songgroup_options.xml` | Song row popup for generic song group views |
| `sorting_options.xml` | Generic sort order selection menu |
| `sorting_options_album.xml` | Album-specific sort options |
| `sorting_options_playlist.xml` | Playlist-specific sort options |

---

## res/xml/

| File | Purpose | Modify when |
|---|---|---|
| `automotive_app_desc.xml` | Declares this app as a media app to Android Auto | Never — fixed requirement for Auto support |
| `file_paths.xml` | Defines which directories `AlbumArtFileProvider` can share | Adding a new directory that needs to be shared via FileProvider |
| `locales_config.xml` | Lists supported locales for per-app language settings (Android 13+) | Adding a new translation language |
| `backup_rules.xml` | Controls which files are included in Android Auto Backup | Changing what user data is backed up |
| `data_extraction_rules.xml` | Controls data extraction for Android 12+ backup | Changing data extraction behavior |

---

## res/values/

| File | Purpose | Modify when |
|---|---|---|
| `strings.xml` | All user-visible strings (English). Also has `values-es/` (Spanish) and `values-fr/` (French) counterparts | Adding or changing any user-visible text — always update translations too |
| `colors.xml` | App color palette | Changing a color used across the app |
| `themes.xml` | Light theme definition. `values-night/themes.xml` overrides for dark mode | Changing the app theme, accent color, or adding theme attributes |
| `attrs.xml` | Custom XML attributes for custom views | Adding a new styleable attribute to a custom view |

---

## res/drawable/

Drawables fall into three categories — only the animated and vector ones are typically modified:

- **Animated drawables** (`playing_animation.xml`, `favorite_animation.xml`, `library_animation.xml`, `unfavorite_animation.xml`) — frame-by-frame animations. Modify when changing animation speed or frames. The frame PNGs (`playing_01.png` … `playing_25.png`, `fav_00.png` … etc.) are the source frames.
- **Vector icons** (`*.xml` drawables) — modify when changing icon appearance.
- **Static images** (`*.png`, `*.jpg`) — app icon variants, placeholder art, and one-off images. Replace when updating branding.

---

## res/anim/ and res/transition/

Fragment enter/exit animations. Modify when changing how screens or fragments animate in and out.

| File | Used for |
|---|---|
| `anim/fly_*.xml`, `anim/slide_in.xml`, `anim/fade_out.xml` | Fragment transition animations |
| `transition/fade.xml`, `slide_down.xml`, `slide_up.xml`, `slide_right.xml` | Shared element / activity transitions |

---

## res/raw/

| File | Purpose | Modify when |
|---|---|---|
| `earth.mp3` | Bundled test/demo audio track | Replacing with a different demo track or removing it |

---

## res/mipmap-*/

Launcher icon assets at each DPI bucket (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`). The `mipmap-anydpi-v26/` folder holds the adaptive icon XML. Replace all when updating the app icon.
