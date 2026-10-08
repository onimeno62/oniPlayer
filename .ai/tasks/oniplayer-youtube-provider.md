# D9 — Optional YouTube Music Provider

## Status

Implemented as an optional official YouTube Data API metadata provider.

## Scope completed

- provider adapter
- capability mapping
- search for tracks/videos and channels/artists
- track metadata lookup
- artwork mapping
- provider error mapping
- injectable API key
- optional registry inclusion

## Deliberate limitation

The official YouTube Data API does not expose a general-purpose playable/downloadable audio URL. Therefore this adapter does **not** claim STREAM or DOWNLOAD and cannot bypass that limitation.

Removing the API key leaves the core provider registry unchanged and oniPlayer remains fully functional.

## Acceptance

- Removing/disabling the provider leaves the app functional. [x]
- No YouTube-specific dependency exists in core UI/domain contracts. [x]
- Provider errors are recoverable. [x]
- Distribution/API-key configuration is isolated to the optional adapter. [x]
- Adapter behavior is unit-testable without live extraction. [x]
