# oniPlayer Widget Rebuild Specification

## Status

Planned — full replacement of the current Default Widget visual implementation.

## Objective

Replace the current music-player widget compositions from scratch. This is a product-level redesign, not a visual polish pass.

The new widgets must feel like three distinct widget products that share playback state and the Default Skin language. They must not be the same composition scaled to different sizes.

## Scope

### In scope

- Default Widget Pack visual/rendering layer
- Widget-specific layouts for 4x1, 4x2, and 4x4
- Shared widget visual primitives
- Album-art presentation and artwork-derived visual treatment
- Playback controls and progress presentation
- Lyrics widget composition
- Widget accessibility and touch-target behavior
- Skin-aware rendering
- Responsive behavior between launcher sizes
- Widget documentation and validation

### Out of scope

- Playback engine changes
- Queue architecture changes
- Lyrics synchronization engine changes
- Media/session architecture changes
- External APK skin/widget loading
- Replacement of the existing Widget Registry, Widget Plugin API, or Widget State Adapter unless required by a verified blocker

## Current Problem

The existing three widgets have converged visually and behave like variations of one composition. The rebuild must restore strong product differentiation:

1. Mini Player — fast transport control
2. Now Playing — flagship glanceable player
3. Dynamic Album — artwork-first music identity
4. Lyrics — lyric-first companion widget

## Widget Families

### 1. Mini Player

Primary purpose: immediate playback control with minimal footprint.

#### 4x1

- Album artwork at the leading edge
- Song title and artist
- Compact progress indicator
- Previous, play/pause, next
- Optional favorite only when space permits
- No decorative content that competes with transport controls

#### 4x2

- Expanded artwork
- Title and artist with stronger hierarchy
- Progress and time information
- Previous, play/pause, next
- Favorite/secondary action when space allows
- More breathing room than 4x1, but still transport-first

#### 4x4

- Expanded mini-player / album mode
- Artwork becomes visually important
- Full transport group
- Progress and time
- Optional secondary action area
- Must remain recognizably the Mini Player family, not a duplicate of Now Playing

### 2. Now Playing

Primary purpose: the flagship home-screen playback experience.

#### 4x1

- Compact flagship summary
- Artwork
- Song title and artist
- Primary play/pause action
- Progress
- Minimal secondary transport

#### 4x2

- Large album artwork
- Song title and artist
- Favorite action
- Progress and elapsed/remaining time
- Previous / play-pause / next
- Strong visual hierarchy and premium spacing

#### 4x4

- Large artwork-led composition
- Song information
- Progress and time
- Full transport controls
- Optional waveform/visual accent only when supported by available state
- Must feel like a premium Now Playing surface, not a larger control panel

### 3. Dynamic Album

Primary purpose: album-art and music identity.

This widget should be visually distinct from both Mini Player and Now Playing.

#### 4x1

- Artwork-forward compact identity
- Song/artist text
- One primary playback action
- Avoid dense transport controls

#### 4x2

- Artwork dominates the composition
- Song and artist remain legible
- Minimal playback interaction
- Album-art-derived glow/depth may be used

#### 4x4

- Dominant album artwork
- Title and artist integrated into the composition
- Large primary play/pause control
- Progress/times where space permits
- Minimal secondary controls
- The artwork is the hero, not a background behind a control grid

### 4. Lyrics

Primary purpose: glanceable lyric context.

#### 4x1

- Current lyric line
- Song identity when useful
- Playback state indicator
- No attempt to show a full lyric list

#### 4x2

- Previous/current/next lyric context when available
- Current line clearly dominant
- Subtle progress/playback cue
- Controls only if they improve usability without stealing space from lyrics

#### 4x4

- Multi-line lyric stage
- Current line visually dominant
- Previous/next context
- Song identity
- Playback controls placed as a secondary layer
- Empty/error/no-lyrics states must remain intentional and readable

## Visual Direction

The Default Skin widget language should be:

- Premium
- Soft
- Modern
- Musical
- Calm
- Artwork-focused
- Material 3 compatible
- Skin-driven

Use layered surfaces, restrained depth, controlled highlights, subtle translucency, and artwork-derived atmosphere where technically appropriate.

Avoid:

- Large opaque brown/purple blocks
- Identical layouts across widget families
- Excessive gradients
- Fake blur that is not supported by Glance
- Heavy shadows
- Decorative effects that reduce legibility
- Tiny or crowded touch targets
- Generic Material cards with no oniPlayer identity

## Skin Integration

The active skin remains the source of truth for:

- Colors
- Typography
- Shapes
- Surface treatment
- Accent color
- Artwork treatment
- Control styling
- Effects

The Default Widget Pack must not introduce a second hardcoded theme system.

## Responsive Rules

- Design each supported size intentionally.
- Do not merely scale a single layout.
- Prefer removing secondary information over shrinking primary content.
- Preserve minimum touch targets.
- Prevent text clipping and accidental overlap.
- Use stable launcher-friendly dimensions and Glance-compatible layout primitives.
- Artwork may crop intentionally, but must never crop unpredictably.

## Accessibility

- Every interactive element needs a meaningful semantic label.
- Touch targets must remain usable at the smallest supported size.
- Text must maintain sufficient contrast against artwork/surfaces.
- Do not communicate playback state by color alone.
- Content descriptions must describe the action or meaningful content, not implementation details.

## Performance

- Reuse cached artwork.
- Avoid expensive per-composition work.
- Do not perform blocking operations during Glance composition.
- Keep live progress updates bounded and event-driven.
- Avoid animation/polling that causes unnecessary widget updates.

## Acceptance Criteria

A rebuild is complete only when:

- The three player widget families are immediately distinguishable.
- Each family has an intentional 4x1, 4x2, and 4x4 composition where supported.
- Lyrics is independently designed rather than derived from player layouts.
- All rendering is skin-aware.
- Existing playback actions still operate through the existing widget action infrastructure.
- Artwork and playback state remain sourced from the existing widget state pipeline.
- No widget layout uses unsupported Glance APIs.
- No text or control clipping occurs at supported sizes.
- Accessibility labels are present and meaningful.
- Debug build and unit tests pass after implementation.
- Physical/emulator launcher validation confirms the widgets are visually distinct and usable.
