# D5 — Recommendation Engine

## Objective

Rank local and provider candidates into explainable recommendations.

## Scope

- listening signals
- candidate aggregation
- ranking
- diversity/repetition controls
- reason labels
- cold-start behavior
- deterministic tests

## Acceptance

- Cold start has useful non-personalized candidates.
- Favorites and listening history influence ranking.
- Repeated artists/tracks do not dominate indefinitely.
- Recommendation generation works offline for local candidates.
- External recommendations are optional inputs.
