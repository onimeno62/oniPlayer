# oniPlayer — Lyrics & Online Lyrics Search Redesign (Phase 9)

Status: implemented on `feature/lyrics-ux-redesign`, pending device validation.

## Principles
- Lyrics are the primary content; tools are contextual.
- Online search = "find the lyrics for this song", not "configure a multi-provider query".
- Default Skin tokens only (`OniSkin.*`). No parallel theme system.

## Architecture decisions
| Area | Decision |
|---|---|
| Lyrics screen owner | `PlayerKaraokeDialog` (same signature) is a thin orchestrator; components live in `ui/player/components/lyrics/` |
| Secondary surfaces | One `LyricsSurface?` enum replaces four independent booleans |
| Follow state | `LyricsFollowState` (UI-only, bound to `LazyListState`), modes `Following` / `Browsing`; not duplicated in any ViewModel |
| Focus position | `LYRICS_FOCUS_FRACTION = 0.35f` via symmetric `contentPadding`; `animateScrollToItem(index)` unchanged |
| Manual scroll | `DragInteraction.Start` -> Browsing (existing mechanism); Resume chip or line tap -> Following |
| Active line | `lyricActive` vs `lyricInactive` tokens: same size, different weight, so layout never jumps; color animates |
| Sync nudge | Moved to Tools -> Adjust timing sheet. Same `manualOffsetMs` model + `shiftSongLyricsTiming`. Unsaved offset shows as a status label |
| Translation | Moved from header globe to Tools -> Translate. Logic unchanged |
| Online search logic | `LyricsSearchViewModel` + sealed `LyricsSearchUiState` (was a direct service call from the composable) |
| Language selection | Removed from UI. `desiredLanguages` no longer passed (service default = no filtering). `LyricsLanguage` + `detectLanguage` kept as result metadata |
| Match score | Hidden. It is a provider/rank heuristic, not a confidence |
| RTL/LTR | `TextDirection.Content` per line/field; global layout direction not forced |

## New tokens
- `OniTypographyTokens.lyricActive`, `OniTypographyTokens.lyricInactive`
- `OniPlaybackControlTokens.compactPrimaryControlSize`

## Follow-ups (not in this phase)
- Remove the now-unused `desiredLanguages` parameter + filter block from `GeminiMusicService.searchRealLyricsOnline`.
- `FloatingLyricsService` computes the active index on the unfiltered parsed list (dialog filters blanks).
- Service dedup key (first 120 chars + language) can merge genuinely different lyrics.
- Provider exceptions are swallowed, so network failure looks like "no results".
- Repo hygiene: `.git_corrupt/`, `app_backup_before_sync_repair/`, `gradle_backup_before_sync_repair/`.

## Validation checklist
### Build
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest` (includes `LyricsFollowStateTest`)

### Lyrics (light + dark, custom accent)
- [ ] Synced: active line settles ~35% from top, previous line visible
- [ ] Plain: no mode selector, no seek, per-line direction
- [ ] No lyrics: empty state -> Search online / Paste manually
- [ ] Long (100+ lines), one-line, very long wrapped lines
- [ ] English, Persian (right-aligned in plain), Japanese, mixed RTL/LTR
- [ ] Drag pauses follow instantly, no snap-back; Resume chip appears/disappears
- [ ] Tap synced line seeks; fling does not seek
- [ ] Pause/resume playback does not change follow mode
- [ ] Track change resets to Following and clears timing preview
- [ ] Progress-bar seek follows when Following, stays put when Browsing
- [ ] Adjust timing: live preview, Apply persists, closing keeps unsaved preview label
- [ ] Sing along strip + Vocal cut from Tools; mic stops on close
- [ ] Translate from Tools unchanged

### Online search
- [ ] Title + artist, empty title (Search disabled), empty artist
- [ ] No results / one / many; synced + plain; mixed languages all shown
- [ ] Preview -> Use these lyrics updates the lyrics screen; tapping a result never overwrites
- [ ] Edit first, Paste manually, Search again, Source menu
- [ ] Back from preview returns to results; rotation keeps query
- [ ] No language picker, no AI translation controls

### Accessibility (TalkBack + Accessibility Scanner)
- [ ] All targets >= 48dp
- [ ] Synced/Plain announced as tabs with selected state
- [ ] Active line announces "Current line"; lines announce "Seek to m:ss"
- [ ] Tools toggles announce on/off; headings in sheets
- [ ] Result rows read as one item
- [ ] Grayscale check: active line, selected mode, disabled buttons still distinguishable
