# oniPlayer Recommendation Engine

## Goal

Provide useful, explainable recommendations without requiring an external ML service.

## Inputs

Local signals:

- play count
- completion ratio
- skip count
- last played
- recency
- favorite state
- rating
- artist affinity
- album affinity
- genre affinity
- repeated listening

External signals:

- provider recommendations
- similar tracks
- similar artists
- release recency
- trending candidates

## Ranking principles

Base score can combine:

1. relevance
2. personal affinity
3. novelty
4. freshness
5. diversity
6. availability

Apply penalties for:

- recently played repetition
- repeated same-artist saturation
- unavailable items
- explicit user exclusions

Exact weights must be tuned from real usage rather than invented permanently in the spec.

## Explainability

Recommendations should support a reason label where practical:

- Because you played...
- Similar to...
- From an artist you follow
- New release
- You haven't played this in a while
- Trending

## Cold start

With little/no listening history:

- use favorites
- use local library metadata
- use provider trending/new releases
- use explicit artist/genre selections

Never claim personalized recommendations when no personalization signal exists.

## Determinism

For a fixed input snapshot and seed, ranking should be deterministic to aid debugging and testing.

## Privacy

Local recommendation signals remain local by default.

External scrobbling/recommendation services require explicit opt-in.

## Testing

Cover:

- cold start
- one-song history
- heavy single-artist history
- repeated skips
- favorites
- unavailable remote candidates
- duplicate identities
- offline mode
