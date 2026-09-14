# oniPlayer Widget Rebuild Tasks

## Status

**W1/W2 + first renderer rebuild implemented on `feat/widgets-rebuild-work6`.**

The old Aurora-based compositions have been replaced by a new Glance-safe visual foundation and distinct Mini Player, Now Playing, Dynamic Album, and Lyrics families. Final promotion requires build/test and launcher validation.

## Phase W1 — Repository Reset and Audit

- [x] Inspect current `main` widget implementation before editing.
- [x] Trace `DefaultWidgetPack` registration.
- [x] Trace Widget Registry / Plugin API / State Adapter / Renderer boundaries.
- [x] Trace widget actions to the existing playback action infrastructure.
- [x] Trace artwork state and cache usage.
- [x] Preserve `WidgetUpdateManager` live progress/artwork pipeline.
- [x] Inventory current renderer and Aurora styling references.
- [x] Migrate the default pack to rebuilt renderer layer.
- [ ] Delete obsolete Aurora renderer/style files after final reference verification.
- [x] Keep existing widget plumbing intact.

## Phase W2 — New Widget Visual Foundation

- [x] Create `OniWidgetVisualSystem` using active skin tokens as the source of truth.
- [x] Establish Glance-safe surface, control, typography, contrast, and progress rules.
- [x] Remove Aurora-specific palette dependencies from rebuilt renderers.
- [x] Keep widget styling independent from playback/state architecture.
- [ ] Extract additional shared primitives only where they reduce real duplication.

## Phase W3 — Mini Player

- [x] Rebuild Compact Player as a transport-first Mini Player.
- [x] Design 4x1 independently.
- [x] Design 4x2 independently.
- [x] Design 4x4 independently.
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

- [ ] No obsolete Aurora widget styling remains in active use.
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
