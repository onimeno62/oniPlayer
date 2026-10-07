# oniPlayer Music Sources and Provider Architecture

## Purpose

Define the replaceable source layer for local and online music.

## Source types

### Local
MediaStore/filesystem-backed music already owned by the user.

### Remote
Catalog content available through a provider and not yet stored locally.

### Downloaded
A remote item that has been persisted locally through an approved download flow.

Downloaded content must retain its remote provenance.

## Provider abstraction

Conceptual contract:

- provider ID/name
- capabilities
- search
- resolve item
- artist
- album/release
- playlist
- stream
- download when supported
- recommendations when supported
- new releases when supported

Do not force optional capabilities into a single large implementation.

## Capability-driven UI

Actions must be derived from provider capabilities.

Examples:

- no DOWNLOAD -> do not show Download
- no STREAM -> do not show Play
- no NEW_RELEASES -> provider cannot populate New Releases
- no RECOMMENDATIONS -> do not synthesize provider-specific recommendations

The UI must never infer capability from provider name.

## Source identity

Every remote result must carry:

- provider ID
- provider item ID
- canonical identity where available
- source type
- availability
- capability snapshot

## Error isolation

Provider failures become domain-level errors:

- unavailable
- rate limited
- authentication required
- item unavailable
- unsupported operation
- transient network failure
- provider parsing/version failure

A provider error must not invalidate local library state.

## First providers

### Audius

First online implementation.

Use official documented API endpoints and respect API limits.

### MusicBrainz

Metadata/identity provider, not a playback provider.

### Future YouTube Music

Optional provider only. No extractor implementation in the initial phase.

## References

- https://docs.audius.co/
- https://musicbrainz.org/doc/MusicBrainz_API
- https://github.com/teamnewpipe/newpipe
