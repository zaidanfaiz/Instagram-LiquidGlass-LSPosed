## v0.1.0-beta.3 — Native-only correction (development candidate)

- Removed the screenshot-coordinate-based Reels action circles and inferred Home-feed controls. Those were **extra visual overlays**, not changes to Instagram's original buttons.
- Disabled static screenshot frosting and Reels PixelCopy background sampling to avoid unintended blur, smeared text and extra GPU work.
- Preserved the separate existing bottom navigation glass material, original navigation targets, gestures and root viewport corrections.
- Added **passive native View hierarchy diagnostics** for Like, Comment, Repost, Share and Save. These diagnostics log only View classes, resource IDs, sizes and clickable flags—no accounts, captions or media.
- **No native Reels action-button styling is claimed yet.** The last device log showed zero individually detected native controls; the exact renderer must be identified before a safe in-place background/drawable patch can be made.
- This candidate needs real-device testing and must not be released as a complete Reels Liquid Glass upgrade.

## v0.1.0-beta.2 — Glass chrome expansion (candidate)

- Add lightweight glass accents to named Instagram UI controls: search bars, inbox filters, profile action buttons, composer surfaces and selected header actions.
- Keep existing floating glass navigation and Reels backdrop behavior unchanged.
- Use bounded, throttled view traversal and draw-only ViewOverlay accents; no input interception, player changes, background replacement or content capture.
- Skip unidentified/obfuscated controls, feed media, full-screen views and unsupported layouts. Android/Instagram builds beyond the recorded development configuration are not verified.
- Provide cleanup on pause, Activity destroy and navigation reinflation.

# Changelog

Changes in this project are recorded here. Beta releases may contain breaking changes because Instagram's internal UI is not a stable public API.

## [Unreleased] — documentation and contribution policy

- Clarified that the module can **potentially** run on multiple Android and Instagram versions: **POCO F7 Ultra**, Android 16 / API 36, a **1440×3200 recorded window** and Instagram 452.0.0.48.64 are the **documented test setup, not installation restrictions**.
- Documented **manual updates from GitHub Releases** using Android's installer, and the signing-certificate requirement for seamless in-place updates.
- Added a custom, limited **source-available contribution license**, maintainer code ownership and pull request review guidance. Redistribution is not generally permitted; public GitHub cloning cannot be technically prevented.

## [0.1.0-beta.1] — 2026-10-22

**First GitHub pre-release for the LSPosed module.** Bundles the previously tested r19 feature set. Device testing used a **POCO F7 Ultra**, Android 16 (API 36) and Instagram `452.0.0.48.64`; **these are not hardcoded compatibility restrictions**.

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
- The implementation relies on internal Instagram view identifiers. It may work on newer/older versions if their layouts match, or fail when those views change.
- This prerelease is debug-signed; updates built with different keys may require reinstalling **the module only**.

### Package information

- GitHub tag: `v0.1.0-beta.1`
- Android app ID: `dev.zaidan.igglass`
- Module scope: `com.instagram.android`
- APK minimum install SDK: 29 (Android 10); **POCO F7 Ultra / Android 16 is the observed development test device**, other versions are not yet verified
- APK and SHA-256 checksums: attached to the GitHub Release

---

## Prior development builds

Internal builds `r1`–`r19` were iterative testing versions rather than independent public releases. The released beta retains r19's navigation, Reels playback safeguards, fullscreen spacing fixes, and experimental window-strip blur fallback.
