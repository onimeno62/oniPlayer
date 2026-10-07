# D4 — Artist Following and New Releases

## Status

Core artist following is implemented on feature/artist-following.

## Implemented

- Follow state is persisted in Room using canonical MusicBrainz artist IDs.
- Database migration 5 -> 6 preserves existing data while adding followed_artists.
- Follow/unfollow repository is provider-independent.
- Discover exposes follow/unfollow controls whenever a result has a canonical artist identity.
- Followed state is exposed as immutable StateFlow and survives process/app restart.
- Follow records retain artist name/artwork plus release-check metadata for the next synchronization phase.

## Remaining D4 work

- Resolve provider artist mappings with confidence thresholds.
- Synchronize releases for followed artists.
- Deduplicate releases and track lastSeenReleaseId.
- Add low-frequency background synchronization using current Android background-work constraints.
- Optional release notifications.

## Acceptance

- [x] Follow state survives restart.
- [ ] Same artist across providers resolves to one followed identity when confidence is sufficient.
- [ ] New releases are not repeatedly announced.
- [ ] Background work respects current Android restrictions.
- [x] No high-frequency polling introduced.
