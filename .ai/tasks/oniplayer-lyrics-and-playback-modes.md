# Lyrics appearance + Poweramp-style repeat/shuffle

Branch: `feat/lyrics-appearance-poweramp-modes`
Status: implemented, **not compiled, not device-validated**.

## Request
1. Lyrics screen: non-highlighted lines a bit smaller than the highlighted line.
2. Lyrics settings: font size, colours (incl. accent), live preview, auto-download + over Wi-Fi only, animated icon in the player's lyrics card while auto-downloading, plus suggested extras.
3. Repeat and shuffle behave like Poweramp.

## Research (Poweramp knowledge base, forum.powerampapp.com)
- Repeat modes: Repeat off, Repeat list/category, Advance list/category, Repeat song, Play single song (stop after current).
- Shuffle modes: Off, Shuffle all songs, Shuffle songs (inside current list), Shuffle categories (e.g. albums, songs in order), Shuffle songs & categories.
- Tap cycles modes; long-press shows the full list; a toast names the new mode.
- Apple Music / Spotify style lyrics: current line full size + bold, context lines smaller and dimmer.

## Plan / what was built
### Playback
- `RepeatMode` = ALL, ONE, OFF, SINGLE (appended, persisted ordinals stay valid). Tap cycle OFF -> ALL -> ONE -> SINGLE -> OFF.
  - SINGLE = Media3 REPEAT_MODE_OFF + `pauseAtEndOfMediaItems`.
  - `RepeatMode.reconcile` keeps OFF/SINGLE through Media3 callbacks.
- `ShuffleType` = SONGS, CATEGORIES (albums), SONGS_AND_CATEGORIES, ALL (whole library). Tap cycle Off -> Songs -> Albums -> Songs & albums -> All -> Off.
  - Order built in the service by `ShuffleCalculator.calculateOrder`; category = album (folder fallback).
  - Shuffle all expands the queue to the library without interrupting the current song and restores the previous list when turned off.
- Next/previous from the UI go through the service so they follow the shuffled order.
- Skipped: "Advance list" (oniPlayer has no category navigation to advance through).

### UI
- `ShuffleModeButton` / `RepeatModeButton` (PlayerPlaybackModeButtons.kt): tap cycles + toast, long-press dropdown with every mode, badges ALB / MIX / ALL, SINGLE uses the LooksOne icon.
- `PlayerLyricsPreview(isFetchingLyrics)`: bouncing/pulsing cloud + "Searching lyrics online...".

### Lyrics
- `LyricsSettingsStore` (DataStore `oni_lyrics_prefs`): fontSizeSp 16-36 (24), inactiveScale 0.6-1.0 (0.82), highlightColorHex (null = accent), textColorHex (null = theme), boldActiveLine, alignment.
- `LyricLineContent`: non-active lines use an animated graphicsLayer scale, so row heights never change and auto-follow stays stable.
- Settings: Appearance section with self-advancing preview, sliders, colour swatches, bold, alignment, reset.
- Auto-download: `auto_download_lyrics_wifi_only` in `oni_settings` (VM store). `ensureLyricsFor()` runs on every track change: DB -> sidecar .lrc -> online; one attempt per song per session.

## Checklist
- [x] Engine / service repeat + shuffle modes
- [x] Mode buttons with long-press menus
- [x] Lyrics appearance store + viewport + settings preview
- [x] Wi-Fi only auto-download + fetch indicator
- [ ] `./gradlew assembleDebug` + unit tests
- [ ] Device check: each repeat mode at end of queue, SINGLE stops after the song
- [ ] Device check: each shuffle mode, shuffle all -> off restores the list
- [ ] Device check: widgets / notification still toggle shuffle + repeat
- [ ] Device check: lyrics settings preview, RTL lyrics with Start alignment
