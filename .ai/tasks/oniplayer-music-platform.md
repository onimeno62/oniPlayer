# Task Master — oniPlayer Music Platform

Branch: feature/music-platform-d6-d10
Status: D0-D8 implemented; D9 optional metadata adapter implemented; D10 validation in progress

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
