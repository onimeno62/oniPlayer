# oniPlayer — Persistent Lyrics & Artwork Storage

Status: planned — follow-up after the current Now Playing polish session.

## Goal

Make user-downloaded lyrics and oniPlayer-managed online artwork survive app reinstall/redeployment where Android storage rules allow, instead of relying only on Room rows and Coil's disposable app-private cache.

## Scope

### Lyrics
- [ ] Define a durable lyrics storage model for downloaded online lyrics.
- [ ] Keep Room as the metadata/index source of truth.
- [ ] Persist downloaded lyrics in a deliberate durable location when the user chooses to keep them.
- [ ] Preserve synced LRC timing and plain-text lyrics.
- [ ] Avoid duplicating or overwriting user-provided local .lrc / .txt files.
- [ ] Define migration for lyrics already cached only in Room.
- [ ] Define uninstall/reinstall behavior explicitly in the UI and documentation.

### Artist / category artwork
- [ ] Keep the remote source URL/metadata in Room.
- [ ] Add a durable local artwork copy for oniPlayer-managed artist artwork.
- [ ] Use stable artist identity keys and deterministic filenames.
- [ ] Make Coil load durable local artwork first, with remote refresh/fallback.
- [ ] Prevent broken remote URLs from blanking an already saved artist image.
- [ ] Avoid duplicating album artwork that already exists in MediaStore/song metadata.
- [ ] Add cache invalidation and cleanup rules.

### Android storage
- [ ] Verify current minSdk/targetSdk and branch storage behavior by API level.
- [ ] Prefer MediaStore / app-owned public media storage where appropriate.
- [ ] Do not use raw File APIs for MediaStore-owned files on scoped-storage devices.
- [ ] Handle permissions and recoverable security flows correctly.
- [ ] Verify uninstall/reinstall behavior on representative Android API levels.

### Validation
- [ ] Download lyrics, force-stop, reopen, and verify persistence.
- [ ] Download lyrics, uninstall/reinstall, rescan, and verify intended persistence.
- [ ] Save artist artwork, force-stop, reopen, and verify persistence.
- [ ] Uninstall/reinstall and verify artwork recovery.
- [ ] Verify local .lrc files remain untouched.
- [ ] Verify library rescan does not create duplicate artwork/lyrics records.
- [ ] Verify offline playback still shows saved lyrics/artwork.

## Architectural constraint

Do not introduce a second source of truth. Room should continue to own metadata/index state; durable files should be represented by explicit local references and reconciled with Room.

## Follow-up

Implement only after the current Now Playing UI/session is finished and verified on main.
