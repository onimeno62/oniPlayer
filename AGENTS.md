# Agent Instructions

## oniPlayer UI/UX

For any UI/UX, Compose UI, visual design, skin, theming, animation, interaction-design, or home-screen widget task:

1. Read `.ai/skills/oniplayer-ui-ux.md` before making changes.
2. Read `.ai/skills/oniplayer-default-skin-design-system.md` for Default Skin work.
3. For widget work, read all of:
   - `.ai/docs/oniplayer-widget-rebuild.md`
   - `.ai/docs/oniplayer-widget-architecture.md`
   - `.ai/docs/oniplayer-widget-design-research.md`
   - `.ai/tasks/oniplayer-widgets-rebuild.md`
4. Treat the UI/UX skill and the applicable design-system/spec documents as authoritative project guidance.
5. Inspect the current `main` source before making architectural, rendering, or UI decisions.
6. Trace existing state, action, skin, artwork, and update flows before replacing implementation.
7. Preserve existing approved oniPlayer design decisions and functional architecture unless the task explicitly changes them.
8. Do not introduce UI architecture that conflicts with the oniPlayer skin system.
9. Do not create a second playback state source inside widgets.
10. Do not bypass the existing widget action/state infrastructure for visual convenience.
11. When replacing a widget renderer, inventory and migrate all references before deleting obsolete files.
12. Design widget sizes as intentional compositions; do not merely scale or crop one layout into another.
13. Validate Glance APIs against the project's actual dependency instead of assuming ordinary Compose APIs are supported.
14. Do not mark widget work complete from compilation alone; include launcher/device validation in the acceptance check.

## Widget Rebuild Guardrails

The current widget rebuild is a full visual/product rewrite. It must produce clearly differentiated:

- Mini Player — transport-first
- Now Playing — flagship playback
- Dynamic Album — artwork-first
- Lyrics — lyric-first

Do not regress to a shared generic player composition with size flags.

## Delivery

Default delivery should be PR-style: explain the change and root cause first, then provide the relevant files/hunks and validation status. If the project workflow requires a repository-ready implementation prompt or task file, update the applicable `.ai` documentation and task checklist as part of the change.


## Music Platform / Online Sources

For any provider, Discover, recommendation, artist-following, online-search, download, or online-music task:

1. Read `.ai/skills/oniplayer-music-platform.md`.
2. Read `.ai/docs/oniplayer-music-platform.md`.
3. Read `.ai/docs/oniplayer-music-sources.md` and `.ai/docs/oniplayer-music-data-model.md`.
4. For Discover UI work, also read `.ai/docs/oniplayer-online-discovery.md` and `.ai/skills/oniplayer-ui-ux.md`.
5. For Default Skin work, also read `.ai/skills/oniplayer-default-skin-design-system.md`.
6. Inspect the current source before making architectural decisions.
7. Keep local playback and local-library behavior fully functional without network access.
8. Provider-specific code must remain behind provider/repository boundaries; Composables and core UI must not call provider APIs directly.
9. Use provider capabilities to decide which actions are available; never infer capabilities from provider names.
10. Keep local, remote-streaming, and downloaded media explicitly distinguishable.
11. Do not create a second playback state source.
12. Downloads are a separate subsystem from playback and must use the existing Android storage/MediaStore architecture.
13. Do not silently overwrite local metadata or artwork with remote provider data.
14. Do not destructively merge music identities using title/artist string matching alone.
15. Isolate provider errors so an online failure cannot break local playback, library scans, queue state, lyrics, or the player.
16. Discover sections must support independent Loading/Success/Empty/Error states.
17. Do not add the YouTube Music extractor/provider before the prerequisite platform phases are implemented and validated.
18. YouTube Music, if added later, is an optional replaceable provider. The application must remain functional when it is disabled or unavailable.
19. Required implementation order: provider abstraction -> Discover foundation -> Audius -> MusicBrainz -> recommendations -> artist following/new releases -> downloads -> optional YouTube Music provider.
20. Do not mark a provider or feature complete from compilation alone; include relevant integration, offline, storage, background, and device validation.
