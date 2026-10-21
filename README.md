# Instagram Liquid Glass — LSPosed

An unofficial **LSPosed module** that replaces Instagram's bottom navigation appearance with a floating, frosted-glass bar while keeping Instagram's original tab destinations.

[![Build](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml/badge.svg)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/badge/release-v0.1.0--beta.1-orange)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20LSPosed-blue)

> [!IMPORTANT]
> **Early beta.** The module is **not intentionally restricted** to one Instagram or Android version. It may work on other versions, but its compatibility hooks can differ. The developer's documented test environment is **Android 16 (API 36) and Instagram 452.0.0.48.64**; results on other versions have not yet been verified. Independent project—not affiliated with Instagram, Meta, Apple or LSPosed.

[English](README.md) · [Bahasa Indonesia](README_ID.md) · [Downloads](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases) · [Changelog](CHANGELOG.md) · [Report a bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)

## Features

- **Floating navigation:** four icons in a rounded glass capsule (Home, Reels, Direct messages and your profile), with **Search** in a separate circular glass button.
- **Five-page order:** Home → Reels → DM → Profile → Search. Tap the icons or drag the selected glass lens. Content-area swipe remapping is **best-effort** and depends on Instagram's internal pager.
- **Frosted glass:** blurred background, subtle rim highlights and an animated selection lens. The material is theme-neutral rather than a solid light/dark bar.
- **Reels blur (beta):** bounded, asynchronous `PixelCopy` sampling from a video surface when available, or an adjacent strip of the Instagram window when it is not. The result is a frosted approximation, **not** native iOS live refraction.
- **Context-aware visibility:** the overlay is intended to show on the main tabs and DM inbox, and hide in opened chats, Reels comment sheets, Stories and other users' profiles.
- **Native app retained:** uses the installed Instagram app and its original tab click handlers. **No patched Instagram APK is distributed.**

## Compatibility and tested device

There is **no hardcoded Instagram-version whitelist** in the module: it activates for the `com.instagram.android` package and looks for recognizable navigation views. Thus **other Instagram and Android versions may also work**, but it is not possible to guarantee this without testing their view hierarchies and rendering behavior.

| Category | Details |
| --- | --- |
| Android APK minimum | **Android 10 (API 29)**, as declared by `minSdk`; this does not guarantee that every feature works on API 29 |
| Instagram application | Installed `com.instagram.android`; internal navigation layouts can vary between versions |
| Required framework | Working **LSPosed** environment, scoped to Instagram |
| Root / hooking | Root or an environment capable of running LSPosed |
| Other Android and Instagram versions | **Potentially compatible; not yet independently verified** |
| iOS | Not compatible |

### Developer's device test data

These values were directly observed in local Android diagnostics; unrecorded hardware details are **not guessed**.

| Test field | Observed value |
| --- | --- |
| Android | **Android 16 / API 36** |
| Instagram version | **452.0.0.48.64** |
| Screen/window resolution reported by Android | **1440 × 3200 pixels** |
| Hooked Instagram package | `com.instagram.android` |
| Module package | `dev.zaidan.igglass` |
| Manufacturer / model | **Not yet recorded in the available diagnostics** |
| ROM/build identifier | **Not yet recorded** |

This is **a record of development testing, not a compatibility lock**. Reports from other phones, ROMs, Android versions and Instagram builds are welcome. A newer Instagram update may change view names, gesture handling or bottom margins, so test carefully before relying on the module.

## Installation and manual updates

1. Download the module APK from the [GitHub Releases page](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases). Verify its SHA-256 with the attached `SHA256SUMS.txt` if desired. **This is not a modified Instagram APK.**
2. Install the APK on an Android device with LSPosed.
3. In **LSPosed → Modules**, enable Instagram Liquid Glass and scope it only to **Instagram (`com.instagram.android`)**.
4. Force-stop Instagram and reopen it, or reboot if your environment requires a restart.
5. Test all five tabs, search, Reels comments, fullscreen and the frosted blur.

### Updating the module (manual by design)

The module **does not auto-download or auto-install updates**. When a new release appears:

1. Visit [GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases) and download the new module APK.
2. Open the downloaded file with Android's package installer and choose **Update** if offered.
3. Force-stop Instagram and reopen it. Keep the Instagram app and its data intact.

**Signing compatibility matters:** Android can update the existing module **only when the package name and signing certificate are compatible, and the new APK has an acceptable version code**. The current `v0.1.0-beta.1` APK is signed with a CI debug certificate; a future CI build may use a different key, causing **Update** to fail. In that case, disable and uninstall the **LSPosed module app only**, install the newer APK, and re-enable its scope. We recommend a stable maintainer-controlled signing key for future in-place updates. Never uninstall Instagram to fix a module signature mismatch.

### How the gestures work

The main capsule shows Home → Reels → DM → Profile. Search is the detached circle on its right. You can tap any destination, drag across the main capsule toward Search, or drag back from Search toward Profile.

Horizontal swipes **on the content itself** use a compatibility layer that observes Instagram's native page changes and redirects them where possible. This does **not** rewrite Instagram's pager adapter, so some transitions may briefly pass through another native page or be unavailable. Vertical Reels scrolling and media carousels should continue using Instagram's native gestures.

## Limitations

- The glass is an **Android frosted-backdrop approximation**, not Apple's system-compositor Liquid Glass. Reels video sampling is capped at roughly **2 frames per second** to limit overhead.
- Video sampling can be unavailable on particular player surfaces or protected content. The module falls back to transparent glass instead of displaying an opaque placeholder.
- Background blur may lag fast-moving content; it is intentionally lighter during some transitions.
- Fullscreen spacing and tab detection depend on specific Instagram view hierarchies. Different app builds may work, or may produce bottom gaps or incorrect routing until their layouts are supported.
- On the first beta, the module may need reinstallation between differently signed builds, as described above.

If Instagram video stalls, tabs stop responding, or the UI breaks: **disable the module in LSPosed and force-stop Instagram**. The original app can then run without the overlay.

## Diagnostics and privacy

On supported Android versions the module writes a local text troubleshooting log to:

```text
Download/IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt
```

The log records technical navigation state, view geometry and rendering failures. The module may temporarily sample small on-screen image/video regions **in memory** to draw the frosted effect. It does **not** intentionally write those sampled pixels to the log or upload them to a server. The module has no analytics endpoint or login flow of its own.

Before attaching screenshots or logs to a **public** GitHub issue, review them and remove personal information, account details or message content.

## Build from source

Prerequisites: **JDK 17**, **Android SDK Platform/Build Tools 35**, **Gradle 8.9**, and the compile-only **Xposed API 82** JAR in `app/libs/api-82.jar`.

```bash
gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The repository's [GitHub Actions build workflow](.github/workflows/build-apk.yml) documents the exact CI setup and produces the APK as a build artifact. The project does not bundle Instagram's proprietary APK or the Xposed API JAR.

## Contributing and reporting bugs

Propose improvements through a **GitHub Pull Request**. Contributors may edit a GitHub-hosted fork to prepare a proposal, but changes enter **`main` only after explicit maintainer review and approval**; opening a PR is not permission to merge or publish an official APK. The maintainer retains the final decision. See [CONTRIBUTING.md](CONTRIBUTING.md) and [LICENSE](LICENSE).

**Source-available, not unrestricted open source:** the original project code is offered under limited contribution terms; public redistribution, mirrors, rebranded modules and independent derivative releases need prior written authorization. **Public GitHub repositories can still be cloned and forked technically, and GitHub's Terms allow on-platform forking**; neither a license nor branch protection can make publicly visible source impossible to copy.

For bug reports include the Android/ROM and Instagram versions, LSPosed and module versions, reproduction steps and a **redacted** `IGGlass` log. See [SECURITY.md](SECURITY.md) for security-related reports.

## Credits, copyright and status

This is an **independent community project**. Instagram and Meta are trademarks of their respective owners; iOS and Liquid Glass are associated with Apple. Those names describe compatibility or visual inspiration only.

The repository includes an MIT-licensed, adapted Android liquid-glass shader; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for its author, copyright notice and license. The adapted shader is not necessarily enabled in the current safe render path.

**Project licensing:** see the custom [source-available contribution terms](LICENSE). Personal experimentation and GitHub-hosted proposed modifications are permitted under their limited conditions; **official upstream merges require maintainer approval**, and there is **no general right to redistribute the original project or independent modified APKs**. These terms are not an OSI-approved open-source license. Third-party shader components retain their own MIT license as described in the notices.

For release history and known beta limitations see [CHANGELOG.md](CHANGELOG.md).
