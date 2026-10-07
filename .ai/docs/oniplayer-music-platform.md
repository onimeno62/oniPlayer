# oniPlayer Music Platform — Master Specification

Status: Approved planning baseline / documentation only
Branch: docs/music-platform-spec-kit

## 1. Vision

oniPlayer remains local-first, but gains a provider-independent music platform layer for discovery, recommendations, artist following, online playback, and provider-specific downloads.

The product boundary is:

- Local library is always first-class and must remain fully usable offline.
- Online sources are optional providers.
- Provider-specific behavior must never leak into UI or core playback models.
- YouTube Music extraction is explicitly deferred until the provider abstraction, Discover foundation, Audius integration, and MusicBrainz integration are stable.
- An unofficial provider must be removable without redesigning the rest of the application.

Target navigation direction:

Home | Library | Discover | Player | Settings

Search remains contextual: Library Search for the local library; Discover Search for online sources.

## 2. Product pillars

1. Local-first playback.
2. Unified music identity.
3. Provider-independent discovery.
4. Personal recommendations based on local listening behavior.
5. Followed-artist release tracking.
6. Smart radio and smart playlists.
7. Explicit source/capability labeling.
8. Offline resilience and cache-first behavior.
9. Skin-aware UI with no hardcoded visual system.
10. Replaceable integrations.

## 3. Reference research

Relevant projects/services researched:

- Poweramp: advanced DSP, ReplayGain, playback modes, smart playlists, Android Auto.
- Musicolet: queues, lyrics, folders, playlists, widgets, offline-first local playback.
- VLC: broad media formats, network sources, history and playback tools.
- Auxio: focused local architecture, Media3, gapless, ReplayGain, Android Auto/widgets.
- Namida: local + online discovery, smart playlists, statistics, downloads, lyrics, recommendations.
- Aurora: local/server/online providers, recommendations, automatic mixes, lyrics, downloads and scrobbling.
- Aetherfin: smart autoplay, queue history, smart playlists, DSP, lyrics and scrobbling.
- S2/Shuttle 2: local and server sources, ReplayGain/EQ, lyrics and artwork services.
- MusicBrainz: canonical artist/release/recording identity and release metadata.
- Audius: documented online music API with search, discovery, streaming and catalog resources.
- Last.fm / ListenBrainz: optional future recommendation/scrobbling inputs.
- NewPipe/InnerTube ecosystem: reference for isolating unofficial online providers.

Research must inform architecture, not copy another product's UI.

## 4. Architecture

UI -> ViewModel -> use cases -> repositories -> provider registry -> provider implementations.

Core abstractions should include conceptual contracts for:

- MusicProvider
- ProviderCapabilities
- MusicSource
- MusicIdentity
- OnlineMusicRepository
- DiscoveryRepository
- RecommendationEngine
- ArtistFollowRepository
- DownloadManager

The exact package names and types must be determined from the current source during implementation; this document is not permission to invent a parallel architecture.

## 5. Provider contract

Providers expose capabilities rather than forcing every provider to implement every operation.

Capabilities may include:

SEARCH, ARTIST, ALBUM, TRACK, PLAYLIST, STREAM, DOWNLOAD, RECOMMENDATIONS, NEW_RELEASES, TRENDING, RADIO, ARTWORK, LYRICS.

Provider failures must be isolated. A failed online provider must not break local playback, library scans, queue state, lyrics, or the player.

Provider identity must travel with remote results so UI can communicate whether content is local, remote, or downloaded.

## 6. Provider roadmap

Phase order is mandatory:

1. Provider abstraction and source model.
2. Discover foundation.
3. Audius provider.
4. MusicBrainz integration.
5. Personal recommendations.
6. Artist following/new releases.
7. Download system.
8. Optional YouTube Music provider.

Do not reverse this order.

### Audius

Use the documented Audius API as the first online provider because it provides a documented catalog/streaming integration and useful discovery endpoints.

Initial scope:

- search
- track
- artist/user
- playlist
- trending/discovery
- stream
- provider attribution
- caching
- capability reporting

Do not make Audius the core domain model.

### MusicBrainz

Use MusicBrainz primarily for canonical identity and release metadata:

- artist IDs
- release groups
- releases
- recordings
- release dates
- labels/genres where useful
- artist/release matching

Use appropriate User-Agent identification, rate limiting and caching.

### Future YouTube Music provider

Explicitly optional and deferred.

It may use an unofficial extractor/internal API implementation, but it must implement the same provider contract as every other source. It must not be referenced directly by Composables, ViewModels, playback service code, or core domain models.

Because unofficial endpoints can change and may create distribution/terms constraints, the app must continue to function if the provider is disabled or unavailable.

## 7. Discovery

Discover is a product surface, not a provider screen.

Initial sections:

- Continue Listening
- Made for You
- Because You Played...
- New Releases
- From Your Artists
- Similar Music
- Radio
- Trending
- Online Search

Sections must be independently loadable so one failed provider does not blank the entire screen.

Each section needs Loading, Success, Empty and Error states.

Remote content must be visibly distinguishable from local content where ambiguity exists.

## 8. Recommendations

Recommendations are produced by a local ranking layer over available provider candidates.

Inputs may include:

- play count
- completion rate
- skip count
- recent plays
- favorites
- ratings
- artist affinity
- genre affinity
- novelty
- current track/artist similarity
- diversity penalties

The first implementation should be deterministic and explainable. Avoid opaque ML dependencies.

Candidate sources can include Audius, MusicBrainz-linked identities and future providers.

Never replace a local track with a remote track silently.

## 9. Artist following

Users can follow artists.

Persist a canonical artist identity where available, plus provider mappings.

Following enables:

- followed-artist page
- new release detection
- release notifications
- artist discovery
- artist radio

Release polling must be low-frequency, cache-aware and background-policy compliant.

## 10. Downloads

Downloads are a separate subsystem from playback.

Download lifecycle:

Queued -> Downloading -> Completed
Queued -> Failed
Downloading -> Paused
Downloading -> Cancelled

Persist enough state for recovery.

A downloaded remote track must retain source metadata and remain distinguishable from an originally local file.

Storage behavior must respect Android SDK-level storage rules and the project's existing MediaStore strategy.

No provider may bypass Android storage architecture with raw writes to protected media locations.

## 11. Cache

Separate:

- metadata cache
- artwork cache
- recommendation cache
- provider response cache
- download files

Define TTL/invalidation per data class during implementation.

Network failure should prefer stale-but-valid cached discovery data over an empty screen where safe.

## 12. Identity resolution

A music item may have:

- internal oniPlayer ID
- MediaStore/local identity
- MusicBrainz artist/release/recording IDs
- provider-specific IDs

Identity resolution must be explicit. Matching by title/artist strings alone must never silently overwrite a local item.

## 13. UI/UX

Read:

- .ai/skills/oniplayer-ui-ux.md
- .ai/skills/oniplayer-default-skin-design-system.md when applicable

Discover must use active skin tokens for surfaces, typography, colors, shapes, artwork treatment and motion.

Composables are pure UI. No provider calls from Composables.

Use lifecycle-aware StateFlow collection.

Navigation remains event-based.

Avoid creating a second playback state source.

## 14. Privacy and accounts

Basic local playback must not require an account.

Provider authentication, if ever required, must be provider-specific and optional.

Listening history used for local recommendations should remain local unless the user explicitly enables a scrobbling/integration service.

## 15. Distribution boundary

The architecture must support builds where optional providers are disabled.

The future unofficial YouTube provider is not part of the initial Play Store-compatible baseline.

Do not describe unsupported provider functionality as implemented.

## 16. Non-goals

Not part of the first implementation:

- replacing the local library
- building a full streaming-service clone
- mandatory user accounts
- opaque AI recommendations
- YouTube extractor first
- media-server integrations before the provider layer is proven
- provider-specific UI architecture

## 17. Acceptance gate

The platform architecture is ready for implementation only when:

- provider contract is documented
- capability model is documented
- local/remote/downloaded identity is defined
- Discover state model is defined
- provider failure behavior is defined
- caching boundaries are defined
- YouTube provider is explicitly isolated
- existing playback/library architecture has been inspected
- no duplicate source-of-truth is introduced
