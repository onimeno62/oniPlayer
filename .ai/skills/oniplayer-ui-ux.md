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
3. Android/Jetpack Compose best practices
4. General UI/UX judgment

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
