# oniPlayer Download System

## Purpose

Define downloads as a first-class subsystem independent from playback.

## Responsibilities

DownloadManager owns:

- queue
- scheduling
- progress
- pause/resume where supported
- retry
- cancellation
- persistence
- storage destination
- verification
- completion notification
- provenance

Provider implementations only provide a downloadable source/capability.

## States

Queued
Downloading
Paused
Completed
Failed
Cancelled

State transitions must be explicit and persisted where recovery matters.

## Storage

Use the project's existing Android storage/MediaStore architecture.

SDK-specific storage behavior must be respected.

Never silently fall back to raw filesystem operations when MediaStore owns the destination.

## Library integration

Completed downloads may become visible to the local library after successful persistence and metadata synchronization.

Do not rescan before the write is complete.

Do not create duplicate library entries when a downloaded item already has a stable local identity.

## Provenance

Persist:

- provider
- remote ID
- original metadata
- downloaded time
- local URI
- optional source reference
- download status

## Provider limitations

The Download action is available only when the provider explicitly exposes DOWNLOAD.

The app must not turn a stream URL into a download capability by assumption.

## Offline

Completed downloads must play without network.

Expired/unavailable remote metadata must not make a completed local download unplayable.

## Future YouTube provider

Must use this same subsystem and must not write directly into the library from extractor code.
