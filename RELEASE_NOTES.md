# Instagram Liquid Glass — v0.1.0-beta.1

The first **public-ready beta build** of the independent Instagram Liquid Glass LSPosed module.

**Compatibility:** designed and tested during development on **Instagram 452.0.0.48.64 / Android 16**. LSPosed with scope `com.instagram.android` is required. Newer Instagram versions and other Android releases are not verified.

## What's included

- Floating frosted-glass navigation: **Home · Reels · DM · Profile** in the main capsule, with **Search** in a detached circular control.
- Logical swipe navigation order: **Home → Reels → DM → Profile → Search**.
- Native Instagram tab destinations, reflecting rim and animated active lens.
- Background blur on static tabs; beta **Reels video blur** with asynchronous PixelCopy sampling and a nearby-window fallback.
- Reels playback safeguards, comment-sheet visibility handling and guarded removal of the old bottom-navbar spacing.
- Locally generated diagnostic logs in `Download/IGGlass-0.1-dev-*.txt`.

## Install

1. Download the attached `Instagram-LiquidGlass-v0.1.0-beta.1.apk`.
2. Optionally verify its SHA-256 against the attached `SHA256SUMS.txt`.
3. Install the APK, enable the module in **LSPosed**, and scope it to **Instagram** only.
4. Force-stop Instagram and reopen it.

**APK identity:** Android package `dev.zaidan.igglass`. This is a **module APK**, not a modified Instagram APK.

## Please read before installing

- **Beta / debug-signed:** this build is signed with a CI debug key. A later APK may have a different signing certificate; in that case uninstall **only this module app**, never Instagram itself.
- The blur is an Android approximation, not Apple's live system-compositor Liquid Glass. Reels video blur updates at approximately **2 samples per second** and may not work for protected or inaccessible player surfaces.
- The content-area swipe remapper is best-effort; it does not replace Instagram's internal pager adapter.
- Built for the Instagram version above only. Updates to Instagram may break view hooks.
- If the player stutters or Instagram behaves incorrectly, disable the module in LSPosed and force-stop Instagram.

## Feedback

Report bugs with Android version, Instagram version, LSPosed version, reproducible steps, and a **redacted** log via [GitHub Issues](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues). Do not upload private chats, credentials or sensitive screenshots.

Read the [README](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed#readme), [Changelog](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/blob/main/CHANGELOG.md) and [Third-party Notices](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/blob/main/THIRD_PARTY_NOTICES.md).

*Independent community project. Not affiliated with Meta/Instagram, Apple or LSPosed.*
