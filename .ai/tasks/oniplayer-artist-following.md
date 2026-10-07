# D4 — Artist Following and New Releases

## Objective

Follow artists and detect new releases using stable identities.

## Scope

- follow/unfollow persistence
- MusicBrainz artist identity
- provider mappings
- release synchronization
- deduplication
- optional notifications
- background scheduling

## Acceptance

- Follow state survives restart.
- Same artist from multiple providers resolves to one followed identity when confidence is sufficient.
- New releases are not repeatedly announced.
- Background work respects current Android restrictions.
- No high-frequency polling.
