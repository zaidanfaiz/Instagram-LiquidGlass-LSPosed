# Publication checklist

This file is for the repository maintainer. **The GitHub repository must stay private until the maintainer explicitly chooses to make it public.** Creating a prerelease does **not** change its visibility.

## Packaging and release

- [ ] Confirm the build workflow passes for the release commit.
- [ ] Confirm [v0.1.0-beta.1](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1) exists as a prerelease and includes **only** the module APK and `SHA256SUMS.txt`.
- [ ] Compare the APK's SHA-256 with the checksum attachment.
- [ ] Confirm the older `instagram-452-analysis` research release is **draft**. It contains an Instagram APK owned by a third party and must **not** be visible when this repository becomes public. Never republish that proprietary APK.
- [ ] Confirm the `Analyze Instagram APK` workflow does not automatically trigger on the module's releases.

## User documentation and testing

- [ ] Check English/Indonesian README links, compatibility table and installation instructions.
- [ ] Test installing the actual **release asset** (not just the earlier Actions debug artifact) on the intended Android 16 / Instagram 452 configuration.
- [ ] Test Home, Reels, DM, Profile, Search, page swiping, glass animations, and Reels blur over video.
- [ ] Re-test opening/closing Reels comments without freezing the picture or stopping sound.
- [ ] Confirm the lower black strip does not return after navigating through all five tabs.
- [ ] Review the public screenshot choice: avoid showing a real Instagram account name, private messages or other personal data.
- [ ] Consider moving from ephemeral **CI debug signing** to a persistent maintainer-owned signing key stored as GitHub Actions Secrets before calling any release stable. Never commit a signing key.

## Rights and licensing

- [ ] **Choose and approve a project-wide license**, or consciously leave the original source with all rights reserved. The third-party MIT shader notice must remain in `THIRD_PARTY_NOTICES.md`.
- [ ] Ensure you have rights to any additional images, app icons or assets included in the repository or public screenshots.
- [ ] Do not distribute Instagram, Meta or Apple assets as part of the module release.

## Final manual GitHub settings

Only the maintainer can decide when these changes are appropriate:

- [ ] Set the repository **About** description (suggested): `Unofficial LSPosed module bringing floating frosted-glass navigation to Instagram 452 on Android 16.`
- [ ] Suggested topics: `android`, `lsposed`, `instagram`, `liquid-glass`, `xposed`.
- [ ] Consider enabling GitHub Security Advisories and private vulnerability reporting.
- [ ] Confirm the `instagram-452-analysis` release and its proprietary APK are hidden from unauthenticated visitors.
- [ ] **Only then**, if satisfied, use **Settings → General → Danger Zone → Change repository visibility → Public**.

Do not promise support for additional Instagram or Android versions until tested.
