# D9 — Optional YouTube Music Provider

## Prerequisite

Do not start this phase until D0-D8 are stable and validated.

## Objective

Add an optional provider for mainstream catalog discovery/playback where technically and legally appropriate for the chosen distribution channel.

## Scope

- provider adapter
- extractor/internal-client integration
- capability mapping
- stream resolution
- optional download integration
- provider health/version handling
- feature flag/build separation if required

## Non-goals

- changing the core music model
- changing Discover architecture
- making YouTube mandatory
- bypassing provider limitations

## Acceptance

- Removing/disabling the provider leaves the app functional.
- No YouTube-specific code exists in core UI/domain contracts.
- Provider breakage produces a recoverable error.
- Distribution-specific restrictions are documented.
- Automated tests cover provider adapter behavior that can be tested without live extraction.

## Maintenance warning

Unofficial/internal endpoints may change without notice. Treat this provider as replaceable integration code.
