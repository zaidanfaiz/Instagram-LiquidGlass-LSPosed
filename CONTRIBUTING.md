# Contributing

Thanks for your interest in improving Instagram Liquid Glass.

This is a reverse-engineering-dependent LSPosed module. **Android 16 (API 36), screen/window 1440×3200, and Instagram 452.0.0.48.64** are documented development test conditions, **not a deliberate Android/Instagram-version lock**. Other devices and app versions may work. Changes should minimize risk to Instagram's normal UI, account state and Reels video playback.

## Before contributing

- Check open issues and `CHANGELOG.md` for known limitations.
- **Do not upload or commit Instagram APKs**, proprietary app resources, credentials, chat contents or screenshots with private data.
- Test with Instagram's own app and LSPosed enabled **only for `com.instagram.android`**.
- Avoid adding network analytics, injected ad/tracking code, or hooks unrelated to the navbar.
- Respect the MIT-licensed third-party shader copyright and `THIRD_PARTY_NOTICES.md`.
- Read the [custom source-available contribution terms](LICENSE). This is **not an unrestricted open-source license**. GitHub-hosted edits/forks for contribution are permitted; independent redistribution, publication of modified APKs, rebranding or mirrors require prior written permission. Public repository access cannot technically stop git cloning or forking.

## Pull request approval and `main` ownership

The project maintainer is **@zaidanfaiz**. Others are welcome to suggest fixes, adapt code in their GitHub-hosted forks for a contribution, and open a pull request, but **only an authorized maintainer can approve merging changes into the official `main` branch**.

Contribution steps:

1. Review the open Issues to avoid duplicate work. Prepare a small, focused patch using GitHub's web editor or other methods permitted by the [license](LICENSE). A public repository can be forked or cloned at the platform level, but **mirroring/rehosting and publishing independent derivatives is not authorized**.
2. Open a Pull Request to `main`, describing the problem, changes, tests and risks.
3. Await maintainer review. The maintainer may request changes, reject the PR, or merge it. An opened or even CI-passing PR **does not constitute approval**.
4. Official releases are published only by the maintainer's authorized workflow.

A repository [CODEOWNERS](.github/CODEOWNERS) file nominates @zaidanfaiz for all paths. **Branch protection is still required** to enforce code-owner review and PR-only merging. The maintainer must configure a GitHub Ruleset for `main` with *Require a pull request before merging*, *Require review from Code Owners*, and appropriate push/merge restrictions. This file alone does not enforce those rules.

## Development environment

Required: JDK 17, Android SDK 35, Gradle 8.9 and compile-only `app/libs/api-82.jar` (Xposed API 82). Build:

```bash
gradle --no-daemon :app:assembleDebug
```

`app-debug.apk` is generated in `app/build/outputs/apk/debug/`. GitHub Actions uses `.github/workflows/build-apk.yml` and installs the required build tools. Never commit your SDK path, local keystore or Xposed API JAR.

## Testing checklist

Please check **all** of these before submitting changes:

1. Home → Reels → DM → own Profile → Search, both tapping and dragging the capsule; Search must open the real Instagram screen.
2. Reels video image/audio play normally while changing clips, opening and dismissing comments repeatedly and performing normal scrolling.
3. The glass does not become a solid white/black pill in bright and dark scenes, and does not unexpectedly disappear during Home/DM scrolling.
4. Instagram content remains properly fitted after navigating back from Reels to Home.
5. Glass navigation hides on a real chat, Story and another person's profile, but remains on the Direct inbox and your own profile.
6. With LSPosed disabled, Instagram returns to its original unmodified behavior.

**Don't replace the native Instagram pager adapter or force window/system-bar geometry without device evidence.** PixelCopy/rendering changes should be asynchronous, bounded and fail back to clear glass.

## Bug reports and pull requests

Include the affected version of this module, Android/ROM, Instagram, LSPosed, expected vs actual behavior and reproducible steps. The module writes an optional local log in `Download/IGGlass-0.1-dev-*.txt`; review and redact it before uploading.

Keep pull requests focused, state the tested device/version combinations and explain whether video playback or navigation behavior changed. CI compilation is mandatory but **does not prove device compatibility**.
