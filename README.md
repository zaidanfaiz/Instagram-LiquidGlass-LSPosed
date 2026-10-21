# Instagram Liquid Glass — LSPosed

Experimental module for **Android 16 / Instagram 452.0.0.48.64**. Independent project; not affiliated with Instagram.

## v0.6 — remove legacy bottom bar slot (beta)

Fixes the issue visible in v0.5 where the frosted pill floats over a **second, opaque dark strip** at the bottom of Instagram.

- Keeps all **five original Instagram tab buttons** in the frosted overlay.
- After transferring the tab row to the overlay, walks upwards through the former bar layout and collapses **small bottom-aligned empty/decorative wrappers**. This allows the surrounding feed layout to reclaim the reserved bottom navigation area when Instagram's hierarchy supports it.
- **Does not hide Instagram's real feed, content panels, system navigation buttons, or any wrapper holding clickable or labelled children.** If a wrapper cannot be safely collapsed, it is left alone and noted in LSPosed logs.
- Floating insets: **16dp** horizontal, **12dp** bottom.
- Keeps scroll-driven frosted sampling from v0.5, capped near **12fps** while scrolling and refreshed at rest.

**Limits:** Instagram's internal view hierarchy is not public; the fix may leave a dark strip on certain layouts. Successful build does not mean compatibility is tested. The sampled blur is not physically refractive iOS Liquid Glass, and screenshots/video elements might not draw in Android's offscreen Canvas. Do not attempt to delete Android's *system* navigation bar; this mod only targets Instagram's legacy tab slot.

## Install from phone

1. Open the latest [GitHub Actions builds](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions).
2. Choose a **successful** `Build LSPosed APK` run for the current commit.
3. Under **Artifacts**, download `Instagram-LiquidGlass-LSPosed-debug`, unzip and install `app-debug.apk`.
4. Enable in LSPosed with **Instagram** as the only scope; force-stop and reopen Instagram.
5. Verify the feed fills the bottom area, then try **all five tabs**, scroll, Reels and profile.

GitHub Actions uses a new debug signing key on some runs. For Android's "not installed" signature mismatch, uninstall the **module app only**, not Instagram, install the new APK and re-enable the module.

## Recovery and diagnostics

If anything freezes, tabs disappear, or scrolling becomes unstable, **disable this module in LSPosed**, force-stop Instagram and reopen it (reboot if necessary). The code logs messages beginning with `IGGlass:`. If a dark bar remains, send the LSPosed log lines mentioning `old nav slot`, `collapsed` or `preserve live slot` so the hook can be adapted to your specific layout, instead of removing unidentified views.

## References

- [liquid-glass-react](https://github.com/rdev/liquid-glass-react)
- [liquidGL](https://github.com/naughtyduk/liquidGL)
- [Prismal](https://github.com/styropyr0/Prismal)

These are technical references only; no shader code has been incorporated.

[Source code](app/src/main/java/dev/zaidan/igglass/InstagramGlassHook.java) · [Build workflow](.github/workflows/build-apk.yml)
