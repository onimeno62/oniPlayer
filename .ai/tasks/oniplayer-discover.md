# D1 — Discover Foundation

## Objective

Replace the permanent Search navigation slot with a provider-independent Discover destination.

## Scope

- navigation destination
- Discover ViewModel/state
- independently loading sections
- local/offline sections
- online section placeholders through provider contracts
- contextual online search entry
- skin integration

## Acceptance

- Search remains available from Library.
- Discover has Loading/Success/Empty/Error handling.
- No network is required for local sections.
- One failing remote section does not blank Discover.
- UI contains no hardcoded provider assumptions.
