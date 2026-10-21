# Security Policy

## Supported versions

Best-effort maintenance is provided for this experimental LSPosed module. The developer has tested **Android 16 (API 36) with Instagram 452.0.0.48.64**, but the module is **not intentionally restricted to that combination**. Other Android/Instagram versions may work; security and compatibility behavior must be checked independently. This is not a security product or an officially supported Instagram client.

## Reporting a vulnerability

Please avoid publishing exploit details, account data, authentication tokens or private screenshots in a public issue.

If GitHub's **Private vulnerability reporting** option is enabled for this repository, use **Security → Report a vulnerability**. Otherwise, open a minimal issue requesting a private contact method **without including sensitive proof-of-concept material or user data**. The maintainer can coordinate the report from there.

Include the affected commit/release, device/OS, Instagram and LSPosed versions, a clear impact statement and non-sensitive reproduction information.

## Trust and scope

- The module runs through LSPosed in Instagram's process. Install it only if you understand and trust the source and release.
- Download APKs exclusively from this repository's official Releases page and verify the attached SHA-256 checksums.
- Keep LSPosed scope limited to `com.instagram.android`.
- This module does not request your Instagram password and does not provide a separate login interface.
- Instagram media samples used for frosted effects are processed in memory. Troubleshooting logs are local text files under Downloads; review them before sharing.

If unexpected behavior occurs, disable the module in LSPosed and force-stop Instagram. **Do not erase Instagram data as a first troubleshooting step.**

No warranty or guarantee of compatibility is provided.
