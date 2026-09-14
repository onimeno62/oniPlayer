# oniPlayer Widget Rebuild Tasks

## Status

Planned — implementation has not started.

## Execution Rule

Complete tasks in order unless source inspection proves a dependency requires a different order. Do not leave `main` in a deliberately broken intermediate state.

## Phase W1 — Repository Reset and Audit

- [ ] Inspect current `main` widget implementation before editing.
- [ ] Trace `DefaultWidgetPack` registration.
- [ ] Trace Widget Registry / Plugin API / State Adapter / Renderer boundaries.
- [ ] Trace widget actions to the existing playback action infrastructure.
- [ ] Trace artwork state and cache usage.
- [ ] Trace `WidgetUpdateManager` live progress/artwork updates.
- [ ] Inventory every reference to the current Compact Player renderer.
- [ ] Inventory every reference to the current Now Playing renderer.
- [ ] Inventory every reference to the current Lyrics renderer.
- [ ] Inventory references to `AuroraWidgetStyle`.
- [ ] Remove obsolete renderer/style files only after references are migrated.
- [ ] Confirm the widget plumbing remains intact.

## Phase W2 — New Widget Visual Foundation

- [ ] Create shared widget visual tokens/primitives compatible with the active skin.
- [ ] Create the widget surface primitive.
- [ ] Create artwork presentation primitive.
- [ ] Create playback control primitives.
- [ ] Create progress primitive.
- [ ] Create typography helpers.
- [ ] Define spacing, radii, elevation, outline, scrim, and contrast rules.
- [ ] Remove dependence on the old Aurora-only widget visual layer.
- [ ] Verify no hardcoded widget palette bypasses the skin system.

## Phase W3 — Mini Player

- [ ] Implement a new Mini Player plugin/renderer.
- [ ] Design 4x1 independently.
- [ ] Design 4x2 independently.
- [ ] Design 4x4 independently.
- [ ] Keep transport control as the primary purpose.
- [ ] Add artwork and track identity with correct priority.
- [ ] Integrate existing previous/play-pause/next actions.
- [ ] Add favorite only if the existing action/state contract supports it.
- [ ] Validate long titles and artists.

## Phase W4 — Now Playing

- [ ] Implement a new Now Playing plugin/renderer.
- [ ] Design 4x1 compact flagship layout.
- [ ] Design 4x2 flagship layout.
- [ ] Design 4x4 premium artwork-led layout.
- [ ] Use artwork as a major visual element.
- [ ] Add progress and time information according to available space.
- [ ] Integrate existing playback actions.
- [ ] Validate artwork loading/fallback.

## Phase W5 — Dynamic Album

- [ ] Create a distinct Dynamic Album plugin/renderer.
- [ ] Design 4x1 artwork-first compact layout.
- [ ] Design 4x2 artwork-dominant layout.
- [ ] Design 4x4 album-art experience.
- [ ] Keep controls intentionally minimal.
- [ ] Ensure it is visually and functionally distinct from Now Playing.
- [ ] Validate missing-artwork fallback.

## Phase W6 — Lyrics

- [ ] Rebuild Lyrics plugin/renderer independently from player widget layouts.
- [ ] Design 4x1 current-line glance.
- [ ] Design 4x2 previous/current/next context.
- [ ] Design 4x4 lyric stage.
- [ ] Preserve existing lyrics state/synchronization source.
- [ ] Preserve supported lyrics actions.
- [ ] Handle no lyrics / loading / unavailable states.
- [ ] Validate long lyric lines and multilingual text.

## Phase W7 — Functional Integration

- [ ] Verify all plugins are registered correctly.
- [ ] Verify all supported widget sizes are declared correctly.
- [ ] Verify playback actions use existing action infrastructure.
- [ ] Verify state comes from the existing widget state adapter.
- [ ] Verify artwork uses the existing cache/update pipeline.
- [ ] Verify live progress updates still work.
- [ ] Verify widget updates after track changes.
- [ ] Verify play/pause state changes.
- [ ] Verify previous/next actions.
- [ ] Verify opening the player/lyrics where supported.
- [ ] Verify no duplicate playback state source was introduced.

## Phase W8 — Quality and Device Validation

- [ ] Run debug compilation.
- [ ] Run `:app:testDebugUnitTest`.
- [ ] Validate 4x1 widgets on a real launcher/emulator.
- [ ] Validate 4x2 widgets on a real launcher/emulator.
- [ ] Validate 4x4 widgets on a real launcher/emulator.
- [ ] Validate launcher resizing where supported.
- [ ] Validate light/default skin appearance.
- [ ] Validate dark skin appearance.
- [ ] Validate artwork-heavy and artwork-missing tracks.
- [ ] Validate long title/artist strings.
- [ ] Validate accessibility labels and touch targets.
- [ ] Validate no clipping, overlap, or unsupported Glance modifier usage.
- [ ] Compare all three player families side-by-side for visual differentiation.
- [ ] Confirm lyrics remains visually independent.

## Definition of Done

- [ ] No old widget renderer remains in active use.
- [ ] No obsolete Aurora widget styling remains in active use.
- [ ] Mini Player, Now Playing, and Dynamic Album are clearly different products.
- [ ] Lyrics is a separate lyric-first product.
- [ ] All widget families are skin-aware.
- [ ] Existing widget playback behavior remains intact.
- [ ] Build passes.
- [ ] Unit tests pass.
- [ ] Physical/emulator launcher validation passes.
- [ ] Documentation reflects the final implementation.

## Final Review

Before marking this task complete, inspect the final `main` source again and verify the implementation against:

- `.ai/skills/oniplayer-ui-ux.md`
- `.ai/skills/oniplayer-default-skin-design-system.md`
- `.ai/docs/oniplayer-widget-rebuild.md`
- `.ai/docs/oniplayer-widget-architecture.md`
- `.ai/docs/oniplayer-widget-design-research.md`

Do not mark a task complete solely because the code compiles. Visual differentiation and real launcher behavior are explicit acceptance criteria.
