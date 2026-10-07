# oniPlayer Music Platform Skill

Use this skill for provider, Discover, recommendation, artist-following, download, or online-music work.

## Required reading

Before changes, read:

- .ai/docs/oniplayer-music-platform.md
- .ai/docs/oniplayer-music-sources.md
- .ai/docs/oniplayer-music-data-model.md
- .ai/skills/oniplayer-ui-ux.md for UI work
- .ai/skills/oniplayer-default-skin-design-system.md for Default Skin work

## Rules

1. Inspect current source before proposing architecture.
2. Preserve existing playback/library source of truth.
3. UI must not call providers directly.
4. Provider-specific logic stays behind provider interfaces.
5. Capabilities determine available actions.
6. Local playback must remain functional offline.
7. Distinguish local, remote and downloaded items.
8. Keep downloads separate from playback.
9. Cache network metadata appropriately.
10. Do not silently overwrite local artwork/metadata.
11. Do not use string matching as a destructive identity merge.
12. Keep provider failures isolated.
13. Do not add YouTube Music extraction before D0-D8.
14. Unofficial providers are optional and removable.
15. Use current Android SDK/storage/background constraints.
16. Follow existing skin architecture; no hardcoded UI tokens.
17. Add tests at provider, repository and ranking boundaries.
18. Update task/spec status after implementation rather than leaving planning claims stale.
