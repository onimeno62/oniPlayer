# oniPlayer Library Dashboard

Approved redesign of the Library tab home screen (dashboard = `activeCategoryIndex == null`).

## Layout (top to bottom)

Fixed:
1. Header: greeting + "Your Library", Scan button, menu (Layout & sorting, Customize home).
2. Search pill (opens the Search tab) + compact Shuffle (tonal) and Play All (filled accent) icon buttons.

Reorderable / hideable via **Customize home** (persisted in `LibraryPreferencesStore.DASHBOARD_SECTIONS`):
- Continue listening: `ContinueListeningHeroV2`, layout unchanged, no prev/next. Shows PLAY AGAIN (and restarts) when the track has finished. Optional "Jump back in" chip below (last playlist or album).
- Browse: 3x2 grid with counts (Songs, Albums, Artists, Folders, Genres, Playlists).
- Recently played: 140dp carousel, press overlay, long-press menu (Play next, Add to queue, Favorite).
- Made for you: 2x2 tinted `MixCard`s (Most Played, Favorites, Discover, High Rated) with one-tap play and empty-state prompts.
- Your listening: total time (play count x duration estimate), top artist, tracks played in the last 7 days. Hidden until something has been played.
- Recently added: carousel + "Scan for new music" when nothing was added in 7 days.

Extras: pull-to-refresh rescans; skeleton while the first scan runs on an empty library.

## Mini-player

`MiniPlayerBar` (ui/screens) renders the skin `OniMiniPlayer` above the floating nav on every tab except Player.
On the dashboard it is hidden while the Continue listening card is on screen and appears once it scrolls away.
The dashboard reports this through `LibraryHostScreen(onDashboardResumeVisibleChange)`; category screens report `false`.

## Rules
- One accent per screen: only Play All is filled; header buttons use neutral tint.
- Lists stay conventional (Material 3 Expressive guidance): expressive in color/shape, familiar in structure.
