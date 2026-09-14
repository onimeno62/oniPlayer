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
