# D2 — Audius Provider

## Objective

Implement the first documented online music provider.

## Scope

- API client
- search
- track
- artist
- playlist
- discovery/trending
- stream resolution
- capability declaration
- response mapping
- caching
- rate-limit/error handling

## Acceptance

- Provider can be enabled/disabled without changing Discover architecture.
- Remote items carry provider identity.
- Streaming is isolated behind playback resolution.
- Provider failures remain recoverable.
- Tests cover malformed/empty/error responses.

## Reference

https://docs.audius.co/
