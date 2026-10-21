# Instagram Liquid Glass — v0.1.0-beta.1

The first **public-ready beta build** of the independent Instagram Liquid Glass LSPosed module.

**Compatibility:** this build is **not restricted to a single Android or Instagram version**. It may run on other versions when Instagram's view hierarchy is compatible; results beyond the developer's test combination remain unverified. The recorded test environment was **Android 16 (API 36), 1440×3200 window resolution, and Instagram 452.0.0.48.64**. Device model/ROM were not captured. A working LSPosed environment scoped to `com.instagram.android` is required.

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

## Update manually from GitHub

Updates are **manual**. Download the newer module APK from [GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases), open it with Android's installer and select **Update** when available, then force-stop and reopen Instagram. The update button works only with a compatible signing certificate, package name and version code. **This first prerelease uses a temporary CI debug certificate**; if a later signing key differs, disable and uninstall **only the module app**, install the new module and enable its LSPosed scope again. Never uninstall or clear Instagram data for a module signing mismatch.

## Please read before installing

- **Beta / debug-signed:** future update-over-install is not guaranteed until a persistent maintainer-owned signing key is configured.
- The blur is an Android approximation, not Apple's live system-compositor Liquid Glass. Reels video blur updates at approximately **2 samples per second** and may not work for protected or inaccessible player surfaces.
- The content-area swipe remapper is best-effort; it does not replace Instagram's internal pager adapter.
- Tested on the Instagram version above, **but not limited to it by a version whitelist**. Other builds may work, although Instagram updates can change view hooks.
- If the player stutters or Instagram behaves incorrectly, disable the module in LSPosed and force-stop Instagram.

## Code and contributions

The upstream owner **@zaidanfaiz** reviews changes and decides what enters the official `main` branch. GitHub-hosted edits and pull requests are welcome under [the limited source-available contribution terms](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/blob/main/LICENSE). There is **no general redistribution license** for independent modified APKs or mirror projects. A public GitHub repository can still be cloned or forked through platform functionality; this policy cannot technically block copying.

## Feedback

Report bugs with Android version, Instagram version, LSPosed version, reproducible steps, and a **redacted** log via [GitHub Issues](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues). Do not upload private chats, credentials or sensitive screenshots.

Read the [README](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed#readme), [Changelog](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/blob/main/CHANGELOG.md) and [Third-party Notices](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/blob/main/THIRD_PARTY_NOTICES.md).

*Independent community project. Not affiliated with Meta/Instagram, Apple or LSPosed.*
