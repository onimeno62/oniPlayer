# D6 — Download Manager

## Objective

Create a provider-independent download pipeline.

## Scope

- persistent queue
- states
- progress
- cancellation/retry
- storage
- provenance
- library synchronization
- notifications

## Acceptance

- Only providers advertising DOWNLOAD can expose Download.
- Completed downloads play offline.
- Failed downloads can retry.
- App restart restores required download state.
- MediaStore/storage behavior is SDK-correct.
- No duplicate local library entries are created.
