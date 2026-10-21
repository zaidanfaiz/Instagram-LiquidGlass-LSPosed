# Instagram Liquid Glass — LSPosed

An unofficial **LSPosed module** that replaces Instagram's bottom navigation appearance with a floating, frosted-glass bar while keeping Instagram's original tab destinations.

[![Build](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml/badge.svg)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/badge/release-v0.1.0--beta.1-orange)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20LSPosed-blue)

> [!IMPORTANT]
> **Early beta.** Built for **Instagram 452.0.0.48.64 on Android 16**. Other Instagram versions, Android versions, ROMs and device configurations are **not verified**. This project is not affiliated with, sponsored by, or endorsed by Instagram, Meta, Apple or LSPosed.

[English](README.md) · [Bahasa Indonesia](README_ID.md) · [Downloads](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases) · [Changelog](CHANGELOG.md) · [Report a bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)

## Features

- **Floating navigation:** four icons in a rounded glass capsule (Home, Reels, Direct messages and your profile), with **Search** in a separate circular glass button.
- **Five-page order:** Home → Reels → DM → Profile → Search. Tap the icons or drag the selected glass lens. Content-area swipe remapping is **best-effort** and depends on Instagram's internal pager.
- **Frosted glass:** blurred background, subtle rim highlights and an animated selection lens. The material is theme-neutral rather than a solid light/dark bar.
- **Reels blur (beta):** bounded, asynchronous `PixelCopy` sampling from a video surface when available, or an adjacent strip of the Instagram window when it is not. The result is a frosted approximation, **not** native iOS live refraction.
- **Context-aware visibility:** the overlay is intended to show on the main tabs and DM inbox, and hide in opened chats, Reels comment sheets, Stories and other users' profiles.
- **Native app retained:** uses the installed Instagram app and its original tab click handlers. **No patched Instagram APK is distributed.**

## Compatibility

| Requirement | Status |
| --- | --- |
| Instagram **452.0.0.48.64** | Primary supported target; tested during development |
| Android **16 / API 36** | Primary tested environment |
| **LSPosed** with Instagram in module scope | Required |
| Rooted device / LSPosed-compatible environment | Required |
| Android 10–15, newer Instagram builds, other ROMs | **Not verified** |
| iOS / non-root Android | Not supported |

Instagram is updated frequently and may change its internal views or navigation logic without notice. Disable this module before testing an unverified Instagram build.

## Download and install

1. Download the APK asset from the [latest project Releases page](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases). Download **the module APK**, not Instagram itself. Verify its SHA-256 against the attached `SHA256SUMS.txt` if you can.
2. Install the APK on your rooted Android device with a working LSPosed environment.
3. Open **LSPosed → Modules → Instagram Liquid Glass**, enable it, and select **Instagram (`com.instagram.android`)** as the app scope. Avoid enabling unrelated apps.
4. **Force-stop Instagram**, then open it again. Reboot if your LSPosed environment requires it.
5. Verify Home, Reels, DM, your profile and Search. Test Reels comment playback and the glass blur before relying on the beta.

> [!NOTE]
> The first beta is **debug-signed** by GitHub Actions. A later rebuild may have a different signing certificate and Android may refuse to install it as an update. In that case, disable and uninstall **the module app only**, then install the new module APK and re-enable its LSPosed scope. **Do not uninstall Instagram or clear Instagram data** for a signing mismatch.

### How the gestures work

The main capsule shows Home → Reels → DM → Profile. Search is the detached circle on its right. You can tap any destination, drag across the main capsule toward Search, or drag back from Search toward Profile.

Horizontal swipes **on the content itself** use a compatibility layer that observes Instagram's native page changes and redirects them where possible. This does **not** rewrite Instagram's pager adapter, so some transitions may briefly pass through another native page or be unavailable. Vertical Reels scrolling and media carousels should continue using Instagram's native gestures.

## Limitations

- The glass is an **Android frosted-backdrop approximation**, not Apple's system-compositor Liquid Glass. Reels video sampling is capped at roughly **2 frames per second** to limit overhead.
- Video sampling can be unavailable on particular player surfaces or protected content. The module falls back to transparent glass instead of displaying an opaque placeholder.
- Background blur may lag fast-moving content; it is intentionally lighter during some transitions.
- Fullscreen spacing and tab detection depend on Instagram 452's specific view hierarchy. A changed app build may bring back bottom gaps or incorrect tab routing.
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

Check the [issues](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues) before opening a new one. Useful reports include the Android version, Instagram version, LSPosed version, module version, clear reproduction steps, whether the problem disappears with the module disabled, and a **redacted** `IGGlass` log.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. See [SECURITY.md](SECURITY.md) for security reports.

## Credits, copyright and status

This is an **independent community project**. Instagram and Meta are trademarks of their respective owners; iOS and Liquid Glass are associated with Apple. Those names describe compatibility or visual inspiration only.

The repository includes an MIT-licensed, adapted Android liquid-glass shader; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for its author, copyright notice and license. The adapted shader is not necessarily enabled in the current safe render path.

**Project licensing:** no license has yet been granted for the project's original source code. Viewing a public repository is not by itself permission to redistribute, sublicense or publish modified builds. A project-wide license will be added only with the maintainer's approval.

For release history and known beta limitations see [CHANGELOG.md](CHANGELOG.md).
