# D4 — Artist Following and New Releases

## Objective

Follow artists and detect new releases using stable identities.

## Implemented

- follow/unfollow persistence keyed by canonical MusicBrainz artist ID
- Room 5 -> 6 migration for followed artists
- provider capability for per-artist release lookup
- MusicBrainz release-group browsing by canonical artist ID
- persisted release-sync cursor and safe first-sync baseline
- deterministic release ordering and deduplication
- persisted followed-artist release feed
- Discover "From Your Artists" surface
- low-frequency background synchronization through WorkManager
- network constraint and exponential retry/backoff
- provider failures isolated from local playback/library behavior
- provider mapping remains conservative: only items carrying a canonical artist identity can be followed

## Acceptance

- Follow state survives restart. [x]
- Same artist from multiple providers resolves to one followed identity when confidence is sufficient. [x] — only canonical identities are accepted; ambiguous provider names are not merged.
- New releases are not repeatedly announced. [x] — cursor plus persisted release identity prevents duplicate announcements.
- Background work respects current Android restrictions. [x] — WorkManager periodic work is scheduled every 12 hours with network constraints.
- No high-frequency polling. [x]

## Optional

- User-visible notifications for newly detected releases remain optional and are intentionally not enabled until the notification UX/permission flow is defined.
