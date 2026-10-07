# D0 — Provider Abstraction and Music Sources

## Objective

Introduce the provider-independent contracts required for local/remote/downloaded music.

## Scope

- provider registry
- capability model
- remote item identity
- source/provenance model
- domain error mapping
- repository boundaries
- tests

## Non-goals

- Audius implementation
- YouTube implementation
- downloads
- Discover redesign

## Acceptance

- Existing local playback is unchanged.
- Provider contracts are independent of any single service.
- UI has no provider-specific dependency.
- Capability checks are test-covered.
- Provider errors cannot corrupt local state.
