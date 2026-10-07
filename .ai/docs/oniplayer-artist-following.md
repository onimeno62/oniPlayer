# oniPlayer Artist Following and New Releases

## Goal

Allow users to follow artists and receive useful new-release information.

## Identity

Prefer canonical MusicBrainz artist IDs where available.

Store provider-specific artist mappings separately.

Never merge artists solely because display names match.

## Follow state

Follow/unfollow must be local and account-free.

Followed artist data includes:

- canonical identity
- provider mappings
- display metadata
- last checked release timestamp/state

## New-release detection

Sources:

1. MusicBrainz release metadata
2. provider-specific release APIs where available

Release matching should use stable identities where possible.

## Notifications

Notifications must be:

- opt-in
- deduplicated
- low frequency
- actionable
- safe to disable

A new release should not create repeated notifications on every sync.

## Background work

Use Android-supported background scheduling and current target-SDK restrictions.

No high-frequency polling.

Use cached timestamps and incremental checks.

## UI

Artist pages may expose:

- Follow/Following
- New releases
- Discography
- Similar artists
- Radio
- Local library content
- Online content

Do not make provider-specific UI assumptions.

## Empty states

A followed artist with no detected releases is not an error.

