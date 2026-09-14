# oniPlayer UI/UX Skill

## Purpose

This skill defines the UI/UX principles and skin system for oniPlayer.

The goal is to allow oniPlayer to have a strong default design while supporting extensive user customization and installable skins.

The skill controls presentation and UX design only. It must not change or interfere with core application functionality.

---

# 1. Core Design

oniPlayer is a music player first.

UI decisions should prioritize:

- Fast access to playback
- Clear music/library navigation
- Easy queue and playback control
- Strong album-art and music identity
- Touch-friendly interaction
- Clear visual hierarchy
- Accessibility
- Smooth performance
- Consistent Android/Compose behavior

The UI should feel modern, intentional, and distinctive to oniPlayer.

Avoid generic "default Material app" design unless Material behavior is required for usability or accessibility.

Use Material 3 principles and tokens where appropriate, while allowing the active skin to visually redefine them.

### Design hierarchy

When making UI decisions, follow this order:

1. Existing approved oniPlayer design decisions
2. This oniPlayer UI/UX skill
3. Applicable oniPlayer design/spec documents
4. Android/Jetpack Compose best practices
5. General UI/UX judgment

Never redesign an already-approved feature without a reason.

---

# 2. Skin System

oniPlayer uses a skin-based UI architecture.

The **Default Skin is itself a skin** and must use the same skin system as installed skins.

There must be no separate "hardcoded default UI" system.

### Default Skin

The Default Skin is always included and requires no installation.

It provides built-in customization such as:

- Colors
- Accent colors
- Light / dark / system appearance
- Fonts
- Typography
- Shapes
- Corner radius
- Backgrounds
- Surfaces
- Cards
- Album-art treatment
- Icons
- Playback control styles
- Sliders
- Visual effects
- Animations
- Component variations
- Supported layout variations

The user should be able to significantly personalize oniPlayer without installing another skin.

### Installed Skins

Installed skins may provide substantially different designs.

A skin may change:

- Overall visual identity
- Screen layouts
- Component layouts
- Navigation appearance
- Player controls
- Now Playing design
- Library design
- Queue design
- Search presentation
- Artwork treatment
- Typography
- Shapes
- Effects
- Animations
- Backgrounds
- Icons and assets

A sufficiently advanced skin may make oniPlayer look like a completely different music player while preserving the underlying application functionality.

---

# 3. Skin Architecture

Separate the application into:

```text
oniPlayer Core
│
├── Playback
├── Library
├── Queue
├── Search
├── Settings
├── Playback State
└── Other application logic
        │
        ▼
   Skin Engine
        │
        ├── Theme Tokens
        ├── Components
        ├── Layouts
        ├── Assets
        └── Motion
             │
        ┌────┴─────┐
        ▼          ▼
 Default Skin   Installed Skin
```

---

# 4. Widget Design Rules

Home-screen widgets are a distinct UI surface. They must be designed as glanceable products rather than miniature copies of app screens.

The widget rebuild specifications are defined in:

- `.ai/docs/oniplayer-widget-rebuild.md`
- `.ai/docs/oniplayer-widget-architecture.md`
- `.ai/docs/oniplayer-widget-design-research.md`
- `.ai/tasks/oniplayer-widgets-rebuild.md`

### Widget family principle

The Default Widget Pack must contain clearly differentiated product experiences:

- **Mini Player** — transport-first and compact
- **Now Playing** — flagship playback experience
- **Dynamic Album** — artwork-first experience
- **Lyrics** — lyric-first experience

Do not implement these as one generic player renderer with size/style flags when their information hierarchy is materially different.

### Size principle

4x1, 4x2, and 4x4 are product breakpoints, not simple scaling factors.

- 4x1: glance and primary action
- 4x2: richer context
- 4x4: immersive/expanded experience

When space decreases, remove secondary information before shrinking primary controls below usable sizes.

### Visual principle

Widgets should use the active skin as their visual source of truth for:

- colors
- typography
- shapes
- surfaces
- accent treatment
- artwork treatment
- control styling
- effects

Artwork should be treated as meaningful music content, not merely a thumbnail.

Use layered surfaces, restrained depth, subtle translucency, controlled highlights, and artwork-derived atmosphere only when they improve hierarchy and readability.

Avoid:

- identical layouts across widget families
- large opaque decorative blocks
- excessive gradients or glow
- fake blur unsupported by the widget rendering system
- dense control grids
- tiny touch targets
- generic Material cards with no oniPlayer identity

### Functional boundary

Widgets must use the existing widget state and action infrastructure. UI code must not create a second playback source or directly manipulate the player engine for convenience.

### Glance rule

Widget rendering must use APIs supported by the project's actual AndroidX Glance dependency. Do not assume ordinary Compose modifier parity.

---

# 5. Compose / UI Engineering

Composables are pure UI and should accept state and callbacks. Logic belongs in the ViewModel or appropriate state holder.

Keep composables small and stateless; hoist state upward.

Use:

- `rememberSaveable` for state surviving configuration changes
- `remember` for transient UI-only state
- `derivedStateOf` where it materially reduces recomposition
- immutable `StateFlow` exposed with `asStateFlow()`
- `collectAsStateWithLifecycle()` for lifecycle-aware state collection

Navigation is event-based. Do not drive navigation directly from replayable state.

Use `LaunchedEffect` with real keys and `DisposableEffect` with proper cleanup where observation or long-lived resources are involved.

Prefer flat layout trees and avoid unnecessary wrappers, nested scrolling, and redundant `fillMaxSize()`.

Centralize theme values through Material3/skin tokens rather than component-level hardcoded colors and dimensions.

Meaningful icons/images require content descriptions; custom controls require appropriate semantics.

Animation belongs in the UI layer, not the ViewModel.

---

# 6. Performance and Accessibility

UI must remain responsive and accessible.

- Preserve sufficient contrast.
- Maintain usable touch targets.
- Do not communicate essential state through color alone.
- Avoid expensive per-frame or per-composition work.
- Avoid unnecessary polling.
- Reuse existing caches and state pipelines.

---

# 7. Change Discipline

Before any UI change:

1. Inspect current source.
2. Identify the existing source of truth.
3. Check this skill and the applicable design/spec documents.
4. Preserve approved architecture and behavior.
5. Make the smallest architectural change that satisfies the design goal.
6. Validate compilation/tests and, for widgets, real launcher behavior.

For a full widget rewrite, follow the dedicated widget task checklist rather than treating the work as a cosmetic patch.
