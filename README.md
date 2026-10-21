# Instagram Liquid Glass — LSPosed

Experimental module for **Android 16 / Instagram 452.0.0.48.64**. Independent project; not affiliated with Instagram.

## v0.7 — runtime hierarchy diagnostics (no visual changes)

The uploaded Instagram 452 APK was decoded successfully. Static XML/DEX
candidates (including `fixed_tabbar_layout`, `floating_tab_bar_shadow`,
and `navigationBarColor`) are not proof of which layer draws the black strip.
v0.7 adds read-only runtime logs before and after floating-bar relocation.

### Reproduce and collect logs

1. Download the latest successful **Build LSPosed APK** artifact for the
   v0.7 commit and install the debug APK.
2. In LSPosed, enable the module and scope it to **Instagram** only.
3. Force-stop Instagram and reopen it on the home/feed tab, wait 2 seconds,
   then switch tabs once. If possible, capture a screenshot of the dark strip.
4. Open **LSPosed > Logs > Module logs**, export/share the logs and keep the
   lines beginning `IGGlass-DIAG:` and `IGGlass:`. They include both
   `BEGIN before-floating` and `BEGIN after-floating` when relocation runs.
5. Share the filtered text output (and screenshot if available) for diagnosis.

Logs describe only view class names, resource IDs, positions, background types,
and Android system-bar insets; view text and account content are intentionally
excluded. If `IGGlass-DIAG` never appears, verify the module scope and
whether Instagram's 452 navigation was detected.

This is a **diagnostic build**, not a confirmed fix for the black strip.
Disable the module and force-stop Instagram if any tab breaks.

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

## Private APK analysis using GitHub Actions

The 452.0.0.48.64 APK is too large to commit to Git, and this project's
**GitHub repository must be private**. **Make it private BEFORE attaching
an Instagram APK** (Settings > General > Danger Zone > Change visibility).
Do not publish a third-party APK as a public GitHub release.

1. In the now-private repository, open **Releases > Draft a new release**.
2. Use tag `instagram-452-analysis`, attach the original `.apk` or `.apks`
   file as a **release asset** and publish it. Releases support large assets;
   normal Git commits do not support files over 100 MB.
3. The **Analyze Instagram APK (no build)** workflow starts from the published
   release. Alternatively, choose its **Run workflow** action and provide the
   existing release tag.
4. In the completed run, download **instagram-452-nav-analysis** in Artifacts.
   It contains only text: DEX string candidates, decoded Android XML resource
   candidates, and a decoding status summary. The binary is NOT uploaded as
   a workflow artifact.
5. Read the analysis first; identify specific navigation classes/containers
   and check LSPosed runtime logs before changing the hooks and rebuilding.

Limitations: XML/DEX names do not prove which Android View produces the black
strip at runtime, particularly when Instagram obfuscates code. A runtime
view-hierarchy dump/log may still be necessary.

## Recovery and diagnostics

If anything freezes, tabs disappear, or scrolling becomes unstable, **disable this module in LSPosed**, force-stop Instagram and reopen it (reboot if necessary). The code logs messages beginning with `IGGlass:`. If a dark bar remains, send the LSPosed log lines mentioning `old nav slot`, `collapsed` or `preserve live slot` so the hook can be adapted to your specific layout, instead of removing unidentified views.

## References

- [liquid-glass-react](https://github.com/rdev/liquid-glass-react)
- [liquidGL](https://github.com/naughtyduk/liquidGL)
- [Prismal](https://github.com/styropyr0/Prismal)

These are technical references only; no shader code has been incorporated.

[Source code](app/src/main/java/dev/zaidan/igglass/InstagramGlassHook.java) · [Build workflow](.github/workflows/build-apk.yml)
