# oniPlayer Widget Rebuild Tasks

## Status

**W1/W2 + first renderer rebuild pushed to `main`. Phase 8 visual redesign implemented on `feat/widgets-visual-redesign` (not yet compiled or device-validated).**

The previous Compact Player renderer and Aurora widget style have been removed. The default pack now registers a new Mini Player, rebuilt Now Playing, new Dynamic Album, and rebuilt Lyrics family using a new Glance-safe skin-aware visual foundation. Build/test and launcher validation remain open.

## Phase W1 — Repository Reset and Audit

- [x] Inspect current `main` widget implementation before editing.
- [x] Trace `DefaultWidgetPack` registration.
- [x] Trace Widget Registry / Plugin API / State Adapter / Renderer boundaries.
- [x] Trace widget actions to the existing playback action infrastructure.
- [x] Trace artwork state and cache usage.
- [x] Preserve `WidgetUpdateManager` live progress/artwork pipeline.
- [x] Inventory current renderer and Aurora styling references.
- [x] Migrate the default pack to rebuilt renderer layer.
- [x] Delete obsolete Aurora widget style.
- [x] Delete obsolete Compact Player renderer.
- [x] Keep existing widget plumbing intact.

## Phase W2 — New Widget Visual Foundation

- [x] Create `OniWidgetVisualSystem` using active skin tokens as the source of truth.
- [x] Establish Glance-safe surface, control, typography, contrast, and progress rules.
- [x] Remove Aurora-specific palette dependencies from rebuilt renderers.
- [x] Keep widget styling independent from playback/state architecture.
- [x] Extract additional shared primitives only where they reduce real duplication (Phase 8).

## Phase W3 — Mini Player

- [x] Establish new `MiniPlayerWidgetPlugin` module.
- [x] Design 4x1 independently.
- [x] Design 4x2 independently.
- [x] Design 4x4 independently.
- [x] Keep transport control as the primary purpose.
- [x] Integrate existing previous/play-pause/next actions.
- [ ] Validate long titles and artists on device.

## Phase W4 — Now Playing

- [x] Rebuild Now Playing from scratch.
- [x] Design 4x1 compact flagship layout.
- [x] Design 4x2 flagship layout.
- [x] Design 4x4 artwork-led layout.
- [x] Integrate existing playback actions and progress state.
- [ ] Validate artwork loading/fallback on device.

## Phase W5 — Dynamic Album

- [x] Create distinct Dynamic Album plugin/renderer.
- [x] Design 4x1 artwork-first layout.
- [x] Design 4x2 artwork-dominant layout.
- [x] Design 4x4 album-art experience.
- [x] Keep controls intentionally minimal.
- [x] Ensure it is distinct from Now Playing.
- [ ] Validate missing-artwork fallback on device.

## Phase W6 — Lyrics

- [x] Rebuild Lyrics independently from player layouts.
- [x] Design 4x1 current-line glance.
- [x] Design 4x2 previous/current/next context.
- [x] Design 4x4 lyric stage.
- [x] Preserve existing lyric synchronization/state source.
- [x] Preserve supported playback actions.
- [x] Handle no-lyrics/unavailable states.
- [ ] Validate long lyric lines and multilingual text on device.

## Phase W7 — Functional Integration

- [x] Register rebuilt plugins in the default pack.
- [x] Preserve supported widget sizes.
- [x] Preserve existing playback action infrastructure.
- [x] Preserve existing widget playback state adapter.
- [x] Preserve existing artwork pipeline.
- [ ] Verify live progress updates after rebuild.
- [ ] Verify track-change updates.
- [ ] Verify play/pause and previous/next actions.
- [ ] Verify player opening behavior.
- [x] No duplicate playback state source introduced.

## Phase 8 — Premium Visual Redesign (`feat/widgets-visual-redesign`)

Visual layer only. `OniWidgetPlaybackState`, `WidgetGlanceState`, `WidgetUpdateManager`, `WidgetPlaybackStateAdapter`, widget actions, Glance hosts, and plugin ids/sizes are unchanged.

### Skin / foundation

- [x] Add `OniWidgetTokens` to the skin (`OniSkinDefinition.widgets`, defaulted so other skins compile).
- [x] Default Skin supplies light/dark widget tokens.
- [x] `OniWidgetVisualSystem`: frosted / control / glass / tonal surfaces, text-derived rail track, skin artwork radius.
- [x] `WidgetArtworkAtmosphere`: cached (LRU, 8 MB) capped foreground, softened ambient wash, hero crop with baked skin scrim. Derived from the existing adapter artwork cache; no new decoding path, no network.
- [x] Shared primitives in `defaultpack/shared/`: canvas + panel, artwork + tinted fallback, primary/secondary/tertiary controls, transport capsule, progress rail / time row / inline progress, typography roles from skin tokens, copy helpers.
- [x] All widget icons tinted from the skin (previously untinted white vectors, invisible on light skins).
- [x] Unit tests for progress math, copy/lyric states, and widget tokens.

### Families

- [x] Mini Player: transport capsule signature. 4x1 strip, 4x2 control deck, 4x4 framed art + docked strip.
- [x] Now Playing: flagship. 4x1 edge rail, 4x2 art + knob rail + weighted transport, 4x4 centred stage.
- [x] Dynamic Album: 4x1 edge-bleed ribbon, 4x2 full-bleed poster, 4x4 full-bleed cover; glass controls.
- [x] Lyrics: accent-ticked current line, indented subdued context, tonal controls. 4x1 line, 4x2 verse, 4x4 stage.

### Open

- [ ] Debug compile and `:app:testDebugUnitTest` on the branch.
- [ ] Launcher validation of every family at 4x1 / 4x2 / 4x4, light and dark.
- [ ] Confirm layout thresholds (compact breakpoints) on small launchers and large font scale.
- [ ] Favorite control: not rendered, because the widget state/action contract has no favorite state or action.
- [ ] Rounded artwork/canvas clipping below API 31 (Glance `cornerRadius` is a no-op there).

## Phase W8 — Quality and Device Validation

- [ ] Run debug compilation after rebuild.
- [ ] Run `:app:testDebugUnitTest`.
- [ ] Validate 4x1 on launcher/emulator.
- [ ] Validate 4x2 on launcher/emulator.
- [ ] Validate 4x4 on launcher/emulator.
- [ ] Validate resizing where supported.
- [ ] Validate light/default and dark appearance.
- [ ] Validate artwork-heavy and artwork-missing tracks.
- [ ] Validate long text and multilingual lyrics.
- [ ] Validate accessibility labels and touch targets.
- [ ] Validate no clipping, overlap, or unsupported Glance APIs.
- [ ] Compare Mini Player, Now Playing, and Dynamic Album side-by-side.
- [ ] Confirm Lyrics remains visually independent.

## Definition of Done

- [x] No obsolete Aurora widget style remains in active use.
- [x] Mini Player, Now Playing, and Dynamic Album are distinct products.
- [x] Lyrics is a separate lyric-first product.
- [x] All rebuilt widget families are skin-aware.
- [x] Existing widget playback architecture remains the source of truth.
- [ ] Build passes after final implementation.
- [ ] Unit tests pass after final implementation.
- [ ] Physical/emulator launcher validation passes.
- [ ] Documentation reflects final implementation.

## Final Review

Inspect final `main` against:

- `.ai/skills/oniplayer-ui-ux.md`
- `.ai/skills/oniplayer-default-skin-design-system.md`
- `.ai/docs/oniplayer-widget-rebuild.md`
- `.ai/docs/oniplayer-widget-architecture.md`
- `.ai/docs/oniplayer-widget-design-research.md`

Do not mark complete from compilation alone. Visual differentiation and launcher behavior are explicit acceptance criteria.
