# Changelog

Changes in this project are recorded here. Beta releases may contain breaking changes because Instagram's internal UI is not a stable public API.

## [0.1.0-beta.1] — 2026-10-22

**First GitHub pre-release for the LSPosed module.** Bundles the previously tested r19 feature set. Android 16 with Instagram `452.0.0.48.64` is the primary development target.

### Features
- Floating main capsule with **Home, Reels, Direct and Profile**, plus a separate circular **Search** button.
- Tap and drag navigation, including the requested logical order: **Home → Reels → DM → Profile → Search**.
- Theme-neutral reflective rim and frosted, captured-backdrop material.
- Static-content blur on Home/DM/Profile/Search where the view hierarchy permits a valid capture.
- Experimental video-aware blur on Reels: asynchronous `SurfaceView` sampling, with an adjacent-window-strip `PixelCopy` fallback when native video surfaces cannot be located.
- Hiding on selected nested screens, including Reels comments, opened DM conversations, Stories and profiles belonging to other users.
- Guarded correction of Instagram 452's obsolete bottom-tab spacing.
- Automatic local troubleshooting logs in the Android Downloads folder.

### Engineering improvements in the beta series
- Keeps video player geometry untouched while Reels comments open/close.
- Captures touches within the glass bar without passing a long press to Reels' speed-up gesture.
- Uses a bounded small-bitmap video sampling loop (approximately two frames per second); rejected frames return to a clear material.
- Preserves a previous valid static blur during transient overlapping video views.
- Disables video glass sampling on hidden screens and when the Activity is paused.

### Known limitations
- **Not** native iOS Liquid Glass or compositor-quality refraction.
- Native Instagram pager order is not rewritten; content-area horizontal swipes are remapped on a best-effort basis and may briefly expose intermediate tabs.
- Protected or unusual video/player surfaces may not offer capturable pixels. Blur may lag scrolling or fall back to clear.
- The implementation relies on specific Instagram 452 view identifiers and may break on later Instagram versions.
- This prerelease is debug-signed; updates built with different keys may require reinstalling **the module only**.

### Package information

- GitHub tag: `v0.1.0-beta.1`
- Android app ID: `dev.zaidan.igglass`
- Module scope: `com.instagram.android`
- Minimum compiled Android SDK: 29; **only Android 16 is verified**
- APK and SHA-256 checksums: attached to the GitHub Release

---

## Prior development builds

Internal builds `r1`–`r19` were iterative testing versions rather than independent public releases. The released beta retains r19's navigation, Reels playback safeguards, fullscreen spacing fixes, and experimental window-strip blur fallback.
