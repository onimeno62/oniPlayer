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
- [x] Run Android unit tests and assemble the debug APK on this branch; the latest CI retry passed. The initial run's two playback queue/transition failures did not reproduce on retry.
- [ ] Validate provider failure/offline behavior, download recovery/storage, followed-artist sync, and Smart Radio/Smart Playlist behavior on an emulator or device.
- [ ] Record runtime evidence before marking D10/device acceptance complete.

### Confirmed finding

The recommendation engine previously used the maximum `lastPlayedTimestamp` in the library as its reference time. When all history was old, the newest old play was treated as recent and could receive the wrong explanation/score. The engine now accepts an injectable wall-clock function (defaulting to the system clock), and a regression test checks that stale-history freshness affects the score as expected.

### Validation boundary

GitHub source inspection and the regression-test addition are complete. The latest branch CI run passed unit tests and debug APK assembly. An earlier run had two playback queue/transition test failures that did not reproduce on retry; the PR diff does not touch those playback files. Physical/emulator validation has not been performed and remains required.
