# oniPlayer Online Discovery

## Product goal

Make Discover the place where oniPlayer turns a user's existing music taste into useful new listening options.

## Navigation

Recommended bottom navigation:

Home | Library | Discover | Player | Settings

Search is not a permanent bottom-navigation destination.

- Library Search searches the user's library.
- Discover Search searches configured online providers.

## Discover information hierarchy

1. Continue Listening
2. Made for You
3. New Releases
4. From Your Artists
5. Similar Music
6. Radio
7. Trending
8. Online Search

The actual order may be adjusted after device validation, but each section must have a clear reason to exist.

## Section contract

Every section exposes:

- title
- optional explanation
- loading state
- content
- empty state
- error state
- refresh/retry behavior
- item actions

Sections should load independently.

## Local-first behavior

Discover should remain useful with no network:

- recent listening
- favorites
- most played
- local similarity candidates
- recently added
- unfinished albums/playlists

When online sources are unavailable, remote sections show concise unavailable states rather than blocking the entire screen.

## Online search

Online search must:

- identify provider/source
- support provider capability filtering
- support pagination where available
- cache safe results
- avoid duplicating local search semantics
- expose actions only when supported

## UI rules

Discover is a music-first surface, not a dashboard full of generic cards.

Use album artwork, artist identity and meaningful hierarchy.

Follow active skin tokens and the existing oniPlayer UI/UX skill.

Do not hardcode colors, corner radii, or typography.

## Future sections

Potential later additions:

- Forgotten Favorites
- Deep Cuts
- Albums in Progress
- New to You
- Genre exploration
- Mood exploration
- Personal radio
- Year/period discovery
