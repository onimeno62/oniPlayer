# D6 — Download Manager

## Status

Implemented provider-independent download pipeline.

## Implemented

- persistent Room download queue and state
- Queued / Downloading / Failed / Completed / Cancelled state model
- WorkManager network-constrained execution
- exponential retry and explicit cancellation/retry APIs
- progress and byte accounting
- MediaStore storage on API 29+
- SDK-specific legacy public Music storage path on API 24-28
- MediaScanner completion on legacy storage
- completion notification and foreground progress notification
- stable provider+remote identity for task deduplication
- explicit provider DOWNLOAD capability and explicit downloadUrl requirement
- no stream-to-download fallback
- download provenance persisted independently from playback/library state

## Acceptance

- Only providers advertising DOWNLOAD can expose Download. [x]
- Completed downloads are persisted into MediaStore/public music storage and remain local/offline. [x]
- Failed downloads can retry. [x]
- App restart restores required queue state from Room. [x]
- MediaStore/storage behavior is SDK-correct. [x]
- No duplicate download task is created for the same provider+remote identity. [x]

## Provider status

Audius currently exposes STREAM but not DOWNLOAD, so no Audius download action is presented. This is intentional; a stream URL is never treated as a downloadable source.
