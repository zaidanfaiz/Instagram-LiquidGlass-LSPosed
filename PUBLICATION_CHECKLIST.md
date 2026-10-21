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
- [x] Recorded the known testing facts: Android 16 / API 36, 1440×3200 window resolution and Instagram 452.0.0.48.64. README states that **other versions may work**.
- [ ] Obtain the **actual device manufacturer/model and ROM name**, which are not in the available diagnostics; do not guess them or publish a historical phone as the current test device.
- [ ] Confirm the expected update process is **manual GitHub APK download → Android installer Update → reopen Instagram**. The installer only supports updating in place when the module APK is signed with a compatible certificate and has an acceptable versionCode. The existing beta was debug-signed with a CI-generated key.
- [ ] If seamless future updates matter, configure a **persistent maintainer-owned signing certificate** through GitHub Actions Secrets before the next stable version. Never commit a private key. Switching away from the old ephemeral debug certificate may require a **one-time reinstall of the module only**.

## Rights and licensing

- [x] Added a custom, limited [source-available contribution license](LICENSE), distinct from MIT: editing via GitHub and proposing PRs is allowed; independent redistribution/rebranding/release builds require written permission. Third-party MIT shader attribution stays in `THIRD_PARTY_NOTICES.md`.
- [ ] **Review/approve the custom legal terms** with the maintainer (and legal counsel if necessary). These are **not** standard OSI open-source terms and do not make public source uncopyable.
- [x] Added [CODEOWNERS](.github/CODEOWNERS) naming `@zaidanfaiz` for every file.
- [ ] **Enforce review rules in GitHub settings:** `Settings → Rules → Rulesets → New branch ruleset`; target `main`, require PRs before merging, require review from Code Owners, and restrict direct pushes/merges as appropriate. CODEOWNERS alone is not a protection rule. **Currently not active:** the private repository's rulesets API returned `403: Upgrade to GitHub Pro or make this repository public to enable this feature`. After the maintainer switches visibility to public (or upgrades the GitHub plan), enable this rule before accepting outside contributions. If GitHub requires an independent approval on the owner's own PR, configure an appropriate maintainer bypass instead of locking yourself out.
- [ ] Check whether you want to keep the repository **private** instead. Once public, others can technically **clone and fork** it under GitHub functionality; neither CODEOWNERS nor custom copyright text prevents that.
- [ ] Ensure you have rights to any additional images, app icons or assets included in the repository or public screenshots.
- [ ] Do not distribute Instagram, Meta or Apple assets as part of the module release.

## Final manual GitHub settings

Only the maintainer can decide when these changes are appropriate:

- [ ] Set the repository **About** description (suggested): `Floating frosted-glass navigation for Instagram through LSPosed. Tested on Android 16; other versions may work.`
- [ ] Suggested topics: `android`, `lsposed`, `instagram`, `liquid-glass`, `xposed`.
- [ ] Consider enabling GitHub Security Advisories and private vulnerability reporting.
- [ ] Confirm the `instagram-452-analysis` release and its proprietary APK are hidden from unauthenticated visitors.
- [ ] **Only then**, if satisfied, use **Settings → General → Danger Zone → Change repository visibility → Public**.

Do not claim that additional Instagram/Android versions are **incompatible** without tests; likewise do not guarantee compatibility without evidence. Other versions may work.

GitHub branch rules protect upstream `main`, **not** the ability to clone a public repository. A maintainer-controlled contribution policy is distinct from a technical anti-cloning mechanism.
