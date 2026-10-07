# Task Master — oniPlayer Music Platform

Branch: docs/music-platform-spec-kit
Status: D0-D5 implemented; D4 artist following/release sync complete; D6-D10 remain

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

## Global gates

- Inspect current source before each phase.
- Read applicable docs before implementation.
- Do not introduce a second playback source of truth.
- Keep local playback functional without network.
- Provider failure must be isolated.
- UI must use active skin tokens.
- Composables remain pure.
- Each phase requires tests and explicit acceptance checks.

## Definition of done

- implementation complete
- unit/integration tests appropriate to the phase
- compile/build validation
- device validation where UI/background/storage is affected
- no stale task claims
- documentation updated with actual implementation
