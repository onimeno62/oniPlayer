# D3 — MusicBrainz Identity Provider

## Objective

Add MusicBrainz as the canonical metadata and identity-resolution source without treating it as a playback provider.

## Implemented foundation

- Artist search and lookup
- Recording search and lookup
- Release search and lookup
- Canonical artist/release/recording IDs mapped into provider-neutral models
- Meaningful User-Agent header
- Provider capability declaration
- Rate-limit, HTTP, unavailable, and not-found error mapping

## Remaining hardening

- Explicit request throttling/caching
- Identity-resolution repository/use-case
- Conservative matching tests
- Artwork/release-group enrichment where needed

## Acceptance

- MusicBrainz never controls playback.
- Canonical IDs remain separate from provider IDs.
- No destructive title/artist string merge occurs.
- Provider failure cannot affect local library/playback.
