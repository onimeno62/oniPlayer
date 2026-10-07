# D4 — Artist Following and New Releases

## Status

Artist-following persistence is implemented and the release-sync foundation is now implemented on feature/artist-release-sync.

## Implemented

- Follow state is persisted in Room using canonical MusicBrainz artist IDs.
- Database migration 5 -> 6 preserves existing data while adding followed_artists.
- Follow/unfollow repository is provider-independent.
- Discover exposes follow/unfollow controls whenever a result has a canonical artist identity.
- Followed state is exposed as immutable StateFlow and survives process/app restart.
- Follow records retain artist name/artwork plus release-check metadata.
- Providers expose ARTIST_RELEASES capability independently from generic NEW_RELEASES.
- MusicBrainz browses release groups by canonical artist MBID and returns deterministic release identities/dates.
- ArtistReleaseSync uses the canonical MusicBrainz identity and the persisted lastSeenReleaseId cursor.
- First successful sync establishes a baseline instead of announcing an artist's entire back catalog.
- Subsequent syncs return only releases newer than the cursor.
- Missing cursors fail closed: the catalog is not repeatedly re-announced.
- Release results are deduplicated by canonical release-group identity.
- Provider failures do not advance the followed artist's sync cursor.
- MusicBrainz requests continue to respect the one-request-per-second API limit.

## Remaining D4 work

- Resolve provider artist mappings with confidence thresholds for non-MusicBrainz providers.
- Persist/surface followed-artist release cards in Discover.
- Add low-frequency background synchronization using current Android background-work constraints.
- Optional release notifications.

## Acceptance

- [x] Follow state survives restart.
- [ ] Same artist across providers resolves to one followed identity when confidence is sufficient.
- [x] New releases are not repeatedly announced by the release-sync cursor.
- [ ] Background work respects current Android restrictions.
- [x] No high-frequency polling introduced.
