<div align="center">

# Instagram Liquid Glass

**A floating, frosted-glass navigation experience for Instagram on Android.**

An independent LSPosed module — no modified Instagram APK required.
<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Inter&amp;weight=600&amp;size=21&amp;duration=2800&amp;pause=1100&amp;color=91A0B3&amp;center=true&amp;vCenter=true&amp;width=780&amp;height=48&amp;lines=Floating+glass.+Native+Instagram.;Home+%E2%86%92+Reels+%E2%86%92+DM+%E2%86%92+Profile+%E2%86%92+Search;Built+for+Android+%26+LSPosed" alt="Animated project headline: floating glass navigation for Instagram" />
</p>


[![Build](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml/badge.svg)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/badge/release-v0.1.0--beta.1-F5A623?style=flat)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)
![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![LSPosed](https://img.shields.io/badge/Framework-LSPosed-5865F2)
![License](https://img.shields.io/badge/license-source--available-64748B)

**[Download](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1) · [Preview](#live-preview) · [Installation](#installation) · [Compatibility](#compatibility) · [Report a bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)**

**English** · [Bahasa Indonesia](README_ID.md)

</div>

---

## A fresh take on Instagram navigation

**Home → Reels → DM → Profile → Search**

A rounded glass capsule for Home, Reels, DM and Profile, with a separate floating Search control. Instagram's original app and tab actions stay in place.

<!-- MEDIA SLOT 01 — Replace the placeholder below after uploading your own assets/readme/cover.png.
<img src="assets/readme/cover.png" alt="Instagram Liquid Glass floating navigation preview" width="100%">
-->
<table>
  <tr>
    <td align="center">
      <strong>HERO IMAGE — YOUR SCREENSHOT GOES HERE</strong>
      <p>Reserved for the main product cover / visual</p>
      <code>assets/readme/cover.png</code>
      <p><sub>Recommended: 1600 × 900 • PNG/WebP</sub></p>
    </td>
  </tr>
</table>

<div align="center">
  <sub>All preview slots are intentionally empty so the maintainer can add original device screenshots and recordings.</sub>
</div>

### Highlights

| Experience | What it does |
| :--- | :--- |
| **Floating Liquid-Glass-inspired UI** | Rounded transparent capsules, reflective edges, animated selected lens and background blur |
| **Frosted Reels** | Experimental video-aware blur via limited asynchronous Android `PixelCopy` sampling |
| **Five destinations** | Home, Reels, Direct inbox, your profile and a standalone Search button |
| **Swipe navigation** | Glass-bar gestures plus best-effort remapping of Instagram's native horizontal page swipes |
| **Context-aware overlay** | Designed to hide when viewing Stories, Reels comments, opened DM chats or other users' profiles |
| **Native Instagram retained** | Runs through LSPosed on the installed Instagram app; no repackaged Instagram APK |

> [!NOTE]
> This is a **beta**, not Apple's native iOS Liquid Glass compositor. Rendering uses sampled/blurred Android content; some videos may temporarily show clear glass if sampling fails. Content-area swipe remapping can briefly pass through Instagram's native tab order.

## Live preview

The layout is ready for original media captured on the test device. **No mockups or borrowed Instagram screenshots are included.**

<table>
  <tr>
    <td width="50%" align="center">
      <strong>🎞 Navigation gesture demo</strong>
      <p>Animated GIF / recorded swipe between the five destinations</p>
      <code>assets/readme/navigation-demo.gif</code>
      <p><sub>Suggested: 900 × 500 • GIF</sub></p>
    </td>
    <td width="50%" align="center">
      <strong>🎞 Reels glass blur demo</strong>
      <p>Animated GIF / moving video visible through the glass</p>
      <code>assets/readme/reels-blur.gif</code>
      <p><sub>Suggested: 900 × 500 • GIF</sub></p>
    </td>
  </tr>
</table>

<!-- MEDIA SLOT 02 — After uploading your own GIFs, replace the placeholder table above with:
<table>
  <tr>
    <td align="center"><img src="assets/readme/navigation-demo.gif" alt="Glass navbar navigation demo" width="100%"></td>
    <td align="center"><img src="assets/readme/reels-blur.gif" alt="Reels frosted blur demo" width="100%"></td>
  </tr>
</table>
-->

### Screenshots

<table>
  <tr>
    <td align="center"><strong>Home</strong><p>📷 Screenshot slot</p><code>home.png</code></td>
    <td align="center"><strong>Reels</strong><p>📷 Screenshot slot</p><code>reels.png</code></td>
    <td align="center"><strong>Direct</strong><p>📷 Screenshot slot</p><code>dm.png</code></td>
  </tr>
  <tr>
    <td align="center"><strong>Profile</strong><p>📷 Screenshot slot</p><code>profile.png</code></td>
    <td align="center"><strong>Search</strong><p>📷 Screenshot slot</p><code>search.png</code></td>
    <td align="center"><strong>Device</strong><p>POCO F7 Ultra</p><sub>Original captures only</sub></td>
  </tr>
</table>

<!-- MEDIA SLOT 03 — Upload images to assets/readme/screenshots/ and replace the placeholders above with:
<table>
  <tr>
    <td align="center"><img src="assets/readme/screenshots/home.png" alt="Instagram Home glass navbar" width="100%"><br><strong>Home</strong></td>
    <td align="center"><img src="assets/readme/screenshots/reels.png" alt="Reels video glass effect" width="100%"><br><strong>Reels</strong></td>
    <td align="center"><img src="assets/readme/screenshots/dm.png" alt="Direct inbox glass navbar" width="100%"><br><strong>DM</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="assets/readme/screenshots/profile.png" alt="Own profile glass navbar" width="100%"><br><strong>Profile</strong></td>
    <td align="center"><img src="assets/readme/screenshots/search.png" alt="Separate Search glass button" width="100%"><br><strong>Search</strong></td>
  </tr>
</table>
-->

**Adding your media?** See the simple [preview asset guide](assets/readme/README.md). The still-image and GIF slots stay empty until you upload your own screenshots and recordings.

## Compatibility

**This module is not locked to one Instagram or Android version.** It hooks the installed `com.instagram.android` application and looks for navigation views. Other versions **may work**, but layout differences can affect fullscreen spacing, blur or gestures.

| Requirement | Details |
| :--- | :--- |
| **Android** | Android 10+ (APK `minSdk 29`); feature compatibility depends on Android and ROM |
| **Instagram** | Installed official `com.instagram.android` application |
| **Framework** | Working LSPosed environment, scoped to Instagram |
| **Device** | Root or another supported environment capable of using LSPosed |
| **Other app/OS builds** | Welcome to test; not all configurations have been verified |

### Verified development test device

| Item | Test configuration |
| :--- | :--- |
| **Device** | **POCO F7 Ultra** |
| **Android** | **Android 16 (API 36)** |
| **Instagram** | **452.0.0.48.64** |
| **Recorded window dimensions** | **1440 × 3200 pixels** |
| **Hook target** | `com.instagram.android` |
| **ROM / build identifier** | Not recorded |

This table describes the device used in development and user testing, **not a restriction preventing other devices from running the module**.

## Installation

1. Open **[GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)** and download the **module APK**. A `SHA256SUMS.txt` file is included for checksum verification.
2. Install it on your device with LSPosed.
3. In **LSPosed → Modules**, enable **Instagram Liquid Glass** and select **Instagram (`com.instagram.android`)** as its scope.
4. **Force-stop Instagram**, then reopen it.
5. Test the five navigation destinations, scrolling, Reels and the comment sheet.

> [!WARNING]
> This module runs inside Instagram through LSPosed. If playback, navigation or layout becomes unstable, disable the module in LSPosed and force-stop Instagram. **Do not delete Instagram or its app data** to troubleshoot this module.

### Updating

**Updates are manual — directly from GitHub.**

1. Download the newer module APK from [Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases).
2. Open the APK and tap **Update** if Android offers it.
3. Force-stop and reopen Instagram.

Android only permits an in-place update when the package, version code and **signing certificate** are compatible. The first beta is signed with a CI debug key; another build may be signed differently. If Android refuses the update, disable and uninstall **the module app only**, install the new module APK and enable its scope again. A consistent maintainer signing key will be needed for reliable future in-place updates.

## How it works

The module has three distinct layers:

```text
Instagram navigation & native click handlers
                ↓
  LSPosed navigation / visibility hooks
                ↓
   Floating glass & backdrop renderer
       ├── Static views: sampled frost
       └── Reels: limited async PixelCopy
```

- **Navigation:** original Instagram destinations remain in place. The requested page order is Home → Reels → DM → Profile → Search. Instagram's internal pager adapter is **not** rewritten, so content swipes are best-effort.
- **Rendering:** transparent rims and small background samples create a frosted appearance. Reels capture is limited to approximately **two samples per second** to avoid unnecessary video load; protected or unavailable frames fall back to clear glass.
- **Screen awareness:** visibility rules adapt the bar to the main tabs and certain nested screens.

## Troubleshooting & privacy

| Symptom | What to try |
| :--- | :--- |
| Overlay does not appear | Check LSPosed module activation/scope; force-stop Instagram |
| Blur temporarily disappears | Some views or video frames cannot be sampled; clear-glass fallback is expected |
| Reels slows or playback stalls | Disable module, relaunch Instagram and report steps to reproduce |
| Bottom gap / incorrect swipe destination | Record app/Android versions and share a redacted diagnostic log |
| APK cannot update | Check signing certificate; reinstall **only the module**, never Instagram |

Diagnostic text logs are generated locally at:

```text
Download/IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt
```

Small picture/video samples used for the blur are processed in memory and are **not intentionally uploaded or stored as image files**. The module has no separate Instagram login or telemetry endpoint. **Review and redact logs/screenshots** before posting them publicly.

## Build from source

Requires **JDK 17**, **Gradle 8.9**, **Android SDK Platform/Build Tools 35**, and the compile-only **Xposed API 82** dependency at `app/libs/api-82.jar`.

```bash
gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

For the CI build instructions, see [build-apk.yml](.github/workflows/build-apk.yml). This repository does **not** bundle Instagram's proprietary APK.

## Contributing & license

Suggestions and Pull Requests are welcome. **@zaidanfaiz** maintains the official project; changes enter `main` only with maintainer approval. `CODEOWNERS` identifies the reviewer, though GitHub branch protection must also be configured to enforce required review.

The original project is distributed under **[custom source-available contribution terms](LICENSE)** — not an OSI open-source license. Public mirrors, rebranded builds and redistribution of the original project require permission. A publicly visible GitHub repository can nevertheless be viewed, cloned and forked as allowed by GitHub; the license does not technically disable copying.

Read **[Contributing](CONTRIBUTING.md)** · **[Security](SECURITY.md)** · **[Changelog](CHANGELOG.md)** · **[Third-party notices](THIRD_PARTY_NOTICES.md)**.

---

<div align="center">

**Built for people who love a cleaner Android interface.**

<sub>Independent community project. Not affiliated with or endorsed by Instagram/Meta, Apple or LSPosed. Third-party code retains its respective license; see THIRD_PARTY_NOTICES.md.</sub>

</div>
