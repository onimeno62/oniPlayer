# oniPlayer Music Data and Identity Model

## Goal

Prevent local, remote and downloaded music from becoming a dual-source-of-truth problem.

## Identity layers

### Local identity

The existing library/media identity remains authoritative for local playback.

### Canonical identity

MusicBrainz IDs may provide:

- artist
- release group
- release
- recording

### Provider identity

Each online provider owns its own IDs.

A provider ID must never be treated as globally unique.

## Conceptual remote item

A remote music item should contain:

- provider ID
- provider item ID
- type
- title
- artist identity/display
- album identity/display
- artwork references
- duration when known
- canonical IDs when resolved
- capability snapshot
- availability

## Resolution

Resolution priority:

1. stable local identity
2. canonical IDs
3. provider IDs
4. conservative metadata matching

String matching is fallback only.

## Deduplication

Do not silently merge:

- same title/different recordings
- remixes
- live versions
- explicit/clean variants
- different editions

A human-visible merge decision or high-confidence identity match is preferable to destructive automatic merging.

## Artwork

Artwork source and cache identity must be explicit.

Do not overwrite user/local embedded artwork merely because an online provider has a newer image.

## Playback identity

The player consumes a resolved playable media reference.

It should not need to know whether resolution came from Audius, a future YouTube provider, or local MediaStore.

