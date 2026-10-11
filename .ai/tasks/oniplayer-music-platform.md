# Task Master — oniPlayer Music Platform

Implementation branch: merged to main
QA follow-up branch: qa/music-platform-integration
Status: D0-D10 merged and CI-validated; runtime/device validation remains open

## Dependency graph

D0 Provider contracts
  -> D1 Discover foundation
  -> D2 Audius provider
  -> D3 MusicBrainz identity
D2 + D3
  -> D4 Artist following/new releases
D0 + D2 + D3
  -> D5 Recommendation engine
D0
  -> D6 Download manager
D5
  -> D7 Smart radio
D5 + D6
  -> D8 Smart playlists / personal discovery
D0-D8
  -> D9 Optional YouTube Music provider
D0-D9
  -> D10 Integration/QA

## Current implementation

- D6: persistent capability-gated downloads using WorkManager and MediaStore.
- D7: deterministic smart-radio queue engine reusing RecommendationEngine.
- D8: deterministic smart-playlist rules engine.
- D9: optional official YouTube Data API metadata/search adapter. It is intentionally metadata-only because the official API does not provide a general-purpose playable/downloadable audio URL; STREAM/DOWNLOAD are not falsely claimed.
- D10: final CI/build/test validation and stale-task cleanup.

## Global gates

- Inspect current source before each phase.
- No second playback source of truth.
- Local playback remains independent of network.
- Provider failure is isolated.
- UI uses active skin tokens.
- Composables remain pure.
- Tests and build validation required.

## Definition of done

- implementation complete
- appropriate unit/integration tests
- compile/build validation
- device validation where UI/background/storage is affected
- no stale task claims
- documentation matches actual implementation

## Integration QA follow-up

- [x] Audit current Discover and recommendation implementation against the current specs.
- [x] Fix recommendation freshness to compare listening timestamps with the current wall clock rather than the newest timestamp in the library.
- [x] Add a deterministic regression test for old listening history.
- [x] Fix Smart Radio remote seed exclusion when `providerItem` is provided in `RadioSeed`.
- [x] Add deterministic regression tests for Smart Radio remote seed exclusion and Smart Playlist filtering/limits.
- [x] Run Android unit tests and assemble the debug APK on this branch; all 162 unit tests passed and debug APK assembled cleanly.
- [ ] Physical/emulator runtime validation (blocked by headless execution environment).

### Confirmed findings and fixes

1. **Recommendation Freshness (D5):** The recommendation engine previously used the maximum `lastPlayedTimestamp` in the library as its reference time. When all history was old, the newest old play was treated as recent and could receive the wrong explanation/score. The engine now accepts an injectable wall-clock function (defaulting to the system clock), and a regression test checks that stale-history freshness affects the score as expected.
2. **Smart Radio Remote Seed Exclusion (D7):** When `RadioSeed` specified a `providerItem` (remote seed item), `SmartRadioEngine` previously only checked `seed.songId` and failed to exclude the remote seed candidate from the resulting queue. The filter was updated to verify both local and remote seed identities, preventing self-recommendation of the seed track.

### Runtime QA status breakdown

- **Passed automated checks:**
  - Full Android unit test suite passed (`gradle :app:testDebugUnitTest`): 162 tests passing, 0 failures, 0 errors, 0 skipped.
  - Targeted test suites verified: `DownloadManagerTest` (4 tests), `ArtistReleaseSyncPolicyTest` (4 tests), `MusicProviderContractTest` (4 tests), `YouTubeMusicProviderTest` (2 tests), `SmartPlaylistEngineTest` (6 tests), `SmartRadioEngineTest` (4 tests), `RecommendationEngineTest` (5 tests), `PlaybackQueueConsistencyTest` (16 tests), `PlaybackTransitionListenerAuditTest` (6 tests).
  - Debug APK assembly passed (`gradle :app:assembleDebug`): Successful build.
  - Remote CI status on PR #25 head (`a3ceab8bcb52c8415c2d081d866ec5c71434e202`): GitHub Actions `verify` workflow passed.
- **Passed emulator/device checks:**
  - None (headless server execution environment; no Android emulator or physical device available).
- **Failed checks and confirmed fixes:**
  - Recommendation engine freshness reference time against wall clock (fixed & tested).
  - Smart Radio candidate filter failing to exclude remote seed item (fixed & tested).
- **Blocked or unverified checks (runtime device / emulator execution required):**
  - Offline mode & provider error handling in live UI (device flight mode / simulated network drop).
  - Real background WorkManager download execution, interrupted downloads, process restarts, and MediaStore storage persistence.
  - Live Followed Artist background sync with MusicBrainz API and notification emission on device.
  - End-to-end audio streaming through Media3 / ExoPlayer on hardware for Smart Radio and remote discovery playback.
