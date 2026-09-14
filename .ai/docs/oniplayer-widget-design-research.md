# oniPlayer Widget Design Research — Direction for Rebuild

## Research Basis

The rebuild direction is informed by Android widget guidance and established Android music-player widget patterns, including Poweramp, Oto Music, Pulsar, and Musicolet.

The goal is not to copy another application's UI. The useful patterns are distilled into oniPlayer-specific rules.

## Product-Level Findings

### 1. A widget is a glanceable product, not a miniature screen

The strongest widgets expose one clear job immediately. Music widgets should make playback state and the most important action obvious without requiring the user to open the app.

### 2. Different widget families need different jobs

A compact controller, a flagship now-playing widget, an artwork-focused widget, and a lyrics widget should not share the same visual hierarchy.

### 3. Album artwork is a major differentiator

Music widgets benefit from treating artwork as content rather than a small thumbnail attached to a generic card.

### 4. Resizing should change information density

A larger widget should reveal more useful context, not merely enlarge the same controls. A smaller widget should remove secondary content rather than compress everything until it becomes crowded.

### 5. Touch targets remain important at every size

Controls need usable target areas even when the visual icon is small. Spacing must prevent accidental adjacent taps.

### 6. Visual cohesion matters

A widget should look native to its application and skin. Consistent typography, surfaces, shapes, artwork treatment, and accent behavior create identity.

## oniPlayer Translation

### Mini Player

Think: **transport first**.

The user should understand the current track and be able to change playback immediately.

### Now Playing

Think: **flagship playback companion**.

The 4x2 layout is the primary showcase: artwork, track identity, progress, and transport controls should form one hierarchy.

### Dynamic Album

Think: **music identity first**.

The artwork is the hero. Controls should be deliberately reduced so the widget does not become another Now Playing clone.

### Lyrics

Think: **lyric context first**.

The current lyric line should dominate. More space should reveal surrounding lyric context, not simply enlarge controls.

## Visual Direction

The Default Skin should combine:

- deep/soft neutral foundations according to the active skin mode
- restrained accent color
- layered surfaces
- subtle depth
- controlled translucency
- artwork-derived atmosphere
- clean typography
- generous spacing

The visual system must remain skin-driven and must not hardcode one global widget palette.

## Anti-Patterns

Reject designs where:

- all three player widgets look nearly identical
- every widget is a large opaque card
- every size contains the same controls
- artwork is reduced to an afterthought
- gradients dominate the interface
- decorative glow reduces text contrast
- controls are packed into tiny areas
- 4x4 is simply a scaled-up 4x2
- 4x1 is simply a cropped 4x4
- lyrics are rendered as a generic player with lyric text added

## Reference Hierarchy

When evaluating a design:

1. oniPlayer product identity
2. active skin rules
3. widget family purpose
4. launcher size constraints
5. Android widget guidance
6. general visual trends

Do not copy competitor layouts, assets, branding, or exact styling.
