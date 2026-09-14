# oniPlayer Widget Architecture — Rebuild Rules

## Purpose

Define the implementation boundary for the widget rebuild so visual changes do not create a second playback architecture or bypass the existing skin/widget infrastructure.

## Existing Runtime Boundary

The widget system is conceptually:

```text
oniPlayer Core
    |
    +-- Playback / Media Session
    |
    +-- Playback State
    |
    v
Widget State Adapter
    |
    v
Widget Plugin
    |
    +-- Widget-specific renderer
    |
    v
Glance AppWidget
    |
    v
Active Skin
```

The rebuild must preserve this boundary unless source inspection proves that a change is necessary.

## Source of Truth Rules

### Playback

Widgets consume the existing widget playback state. They must not read `MusicPlayerViewModel` directly and must not create an independent player/session state source.

### Actions

Playback commands must continue through the existing widget action infrastructure. Do not implement direct player-engine calls inside widget renderers.

### Artwork

Use the existing artwork state/cache pipeline. Do not introduce a second artwork downloader/cache solely for the redesign.

### Skin

Resolve visual values through the active oniPlayer skin. The Default Skin is a skin implementation, not a special renderer bypass.

## Rendering Layer

Create a small shared widget visual layer for reusable primitives. Suggested responsibilities:

```text
widgets/defaultpack/
    shared/
        OniWidgetVisualSystem.kt
        OniWidgetSurface.kt
        OniWidgetArtwork.kt
        OniWidgetControls.kt
        OniWidgetProgress.kt
        OniWidgetTypography.kt

    miniplayer/
        MiniPlayerWidgetPlugin.kt
        MiniPlayerWidgetRenderer.kt

    nowplaying/
        NowPlayingWidgetPlugin.kt
        NowPlayingWidgetRenderer.kt

    dynamicalbum/
        DynamicAlbumWidgetPlugin.kt
        DynamicAlbumWidgetRenderer.kt

    lyrics/
        LyricsWidgetPlugin.kt
        LyricsWidgetRenderer.kt
```

The exact package/file structure may change after inspection. The principle is separation of widget family composition from shared visual primitives.

## Widget Family Independence

Each family owns its layout hierarchy and information density.

Do not create one generic `PlayerWidget` renderer with flags such as:

```text
isCompact
isNowPlaying
isAlbum
size
```

when those flags cause fundamentally different compositions.

Shared primitives are encouraged; shared product composition is not.

## Size Strategy

Treat launcher sizes as product breakpoints:

- 4x1 — glance / primary action
- 4x2 — richer context
- 4x4 — immersive or expanded experience

For each family, define explicit content priority before implementing the layout.

When space decreases:

1. Remove tertiary information.
2. Reduce secondary actions.
3. Reduce decorative treatment.
4. Preserve artwork, identity, and primary action.
5. Never shrink controls below usable touch targets.

## Glance Constraints

Use only Glance-compatible layout and modifier APIs.

Avoid relying on ordinary Compose UI assumptions that do not apply to Glance.

Prefer explicit:

- `Row`
- `Column`
- `Box`
- `Spacer`
- `Alignment`
- fixed/weighted dimensions supported by the current Glance version

Verify every new modifier against the project's actual Glance dependency rather than assuming Compose parity.

## State and Recomposition

Widget state should be a stable snapshot suitable for Glance composition.

Do not perform:

- Blocking I/O
- Network calls
- Player-engine operations
- Long-running calculations
- Direct ViewModel collection inside a widget renderer

## Progress Updates

Progress rendering must use the existing widget update mechanism.

The visual layer should not start its own high-frequency timer or polling loop.

Progress should degrade gracefully when duration/position is unavailable.

## Artwork Treatment

Artwork may drive:

- glow
- tonal background
- border/highlight
- contrast treatment
- scrim

However:

- Artwork must remain identifiable.
- Text must remain readable.
- Missing artwork must have a deliberate fallback.
- Image processing must respect widget performance constraints.

## Interaction Model

Each interactive target should map to one existing widget action.

Typical actions:

- Previous
- Play/Pause
- Next
- Open Player
- Favorite, if supported by the existing action/state contract
- Lyrics/Open Lyrics, if supported

Do not add new actions merely for visual completeness.

## Migration Rule

The rebuild may replace current renderer implementations completely. Preserve functional plumbing wherever it is correct.

Before deleting or renaming a renderer:

1. Find all references.
2. Update plugin registration.
3. Update imports.
4. Remove obsolete visual helpers.
5. Confirm no production code references the removed class.

## Validation Rule

The implementation is not considered complete from source inspection alone. Validate:

- compilation
- unit tests
- widget installation
- all supported sizes
- real playback state changes
- artwork changes
- play/pause/next/previous actions
- missing-artwork state
- long titles/artists
- lyrics availability/unavailability
- light/dark skin variants
- launcher resize behavior
