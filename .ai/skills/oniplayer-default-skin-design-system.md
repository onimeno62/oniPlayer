# oniPlayer Default Skin — Design System v1.0

## Purpose

This document defines the visual design system for the **oniPlayer Default Skin**.

The Default Skin is the built-in visual experience of oniPlayer and must work without any external skin installed.

It is also the reference implementation for the oniPlayer Skin System.

The provided visual reference image is used **only as visual inspiration** for the design language. It does not define screen layouts, navigation structure, component placement, or exact dimensions.

---

# 1. Design Identity

The Default Skin should feel:

**Clean · Soft · Modern · Musical · Premium · Calm · Responsive**

The visual language combines:

- Modern Android design
- Soft neumorphic depth
- Subtle frosted/glass surfaces
- Strong music-player hierarchy
- Artwork-focused presentation
- Restrained motion
- Clean typography
- Generous spacing

Avoid:

- Generic Material-app appearance
- Corporate visual language
- Excessive glassmorphism
- Excessive neumorphism
- Heavy shadows
- Excessive gradients
- Visual noise
- Overly futuristic decoration
- Skeuomorphic controls

### Core principle

> Soft surfaces + strong hierarchy + restrained accent color + beautiful artwork.

---

# 2. Color System

The Default Skin uses a soft neutral foundation with a configurable accent color.

## Default Light Palette

| Token | Default |
|---|---|
| Background | `#F7F5F1` |
| Surface | `#FCFBF9` |
| Elevated Surface | `#FFFFFF` |
| Primary Text | `#202124` |
| Secondary Text | `#737373` |
| Tertiary Text | `#A0A0A0` |
| Divider | `#E8E6E2` |
| Disabled | `#C8C8C8` |
| Primary Blue | `#3B73E3` |
| Bright Blue | `#4D8DFF` |
| Soft Blue | `#DCEAFF` |
| Blue Surface | `#EEF5FF` |

These values are defaults, not component-level hardcoded values.

The implementation must expose semantic tokens such as:

```text
primary
onPrimary
primaryContainer
onPrimaryContainer

background
onBackground

surface
onSurface
surfaceVariant
onSurfaceVariant

outline
outlineVariant

error
onError