# Instagram Liquid Glass — LSPosed

Experimental module for **Android 16 / Instagram 452.0.0.48.64**. Independent project; not affiliated with Instagram.

## Current test build: **0.1-dev-r3 frosted glass**

The last device screenshot (0.1-dev) was opaque white because the module
followed system night mode instead of Instagram's own near-black `tab_bar`
background. The device's Downloads log confirms that the 180px tab margin
was already reclaimed, but a smaller bottom region was still visible.

v0.1-dev-r3 includes all r2 fixes, plus a **small frosted snapshot layer**
behind the translucent dark glass. This captures the narrow union strip
behind the two pills **once after interactions settle**, rather than drawing
the whole Instagram screen twice at 12fps as in v0.9. The small bitmap is
softened by Android's RenderEffect on the GPU; the AGSL specular ripple and
draggable selected lens work separately. Captures are rate-limited (at most
one per 1200ms), automatically suspended on slow devices if a capture takes
over ~38ms, and have no ongoing per-frame polling. Reels/SurfaceView videos
may not appear in the off-screen sample.

Additional changes carried forward:
- **Dark adaptive glass**, 44dp lift, separated Search circle and unclipped
  `BOTTOM|LEFT` main pill.
- Slide-to-select over the four original buttons with native click/touch
  dispatch, preserving normal tap behavior.
- Targeted **180px feed margin correction**, and an extra reversible,
  ID-verified **60px root coordinator inset** correction to address the
  residual dark strip; applied only if Android reports a transparent
  system navbar with a 0px navigation inset.
- Story/profile detection now has a large-view ID fallback on Instagram
  versions that do not expose a normal AndroidX support fragment manager.
- **Downloads** log still created automatically as
  `IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt`, with lines for theme, layout,
  gesture, edge-space, screen state and frosted snapshot latency.

### Test it

Install the APK from the **latest successful GitHub Actions build** for the
current main commit, force-stop Instagram, reopen it and check:
1. Dark translucent/frosted background on both pills — no opaque white;
   enough clearance below both bars.
2. Drag the active highlight across Home / Reels / Send / Profile and release;
   the corresponding native tab should open. Test Search separately.
3. Swipe through the feed and DM, including quick scrolling. Check for
   freezes/stutters. The glass blur refreshes at rest, not on every frame.
4. Open a Story/status and a profile, then return. Note any failure to hide.
5. Check Downloads log for `theme=DARK`, `gesture: glass selection drag`,
   `edge-space: reclaimed` (or a reason it was skipped), `frost: ...ms`
   and any screen detection messages. Send this file with a screenshot.

The cached backdrop is **not real-time iOS refractive Liquid Glass**; the
effect is an experimental Android approximation. This build's appearance
and gesture behavior remain **untested on the user's device**.

## Current test: **0.1-dev-r2 dark glass + slide** (Android 16 / Instagram 452)

This patch responds to the user's **real 0.1-dev screenshot and Downloads
log**: capsules were opaque white on Instagram's dark theme, the main shell
was clipped against the left edge, the selected lens could not be dragged,
and a dark gap remained below the content.

- **Use Instagram's native `tab_bar` background** to determine the glass
  palette, not Android system night mode. Verified old IG background was
  `#FF0C1014`; the resulting glass is **dark and translucent**, not solid
  white.
- Raise the two pills **44dp above the bottom** and use
  `Gravity.BOTTOM | Gravity.LEFT` for the main shell instead of
  centered `MATCH_PARENT` with asymmetric margins. Search remains a
  separate right-side circular control.
- **Drag the active lens horizontally** across the left pill.
  The parent only intercepts after a genuine horizontal gesture; regular
  taps and vertical scrolling still use Instagram's own handlers.
  Releasing a drag activates the underlying original tab, preferring native
  `performClick()` and falling back to dispatching a normal touch pair to
  that real button. The low-overhead AGSL ripple remains enabled.
- In addition to the working **180px native navbar reservation fix**, try
  to reclaim Instagram's independently measured **60px root coordinator
  gap** (at 1440x3200) *only* when the exact root view IDs, small margin
  size and transparent Android navigation bar are verified. This adjustment
  is reversible and restored outside the main feed.
- Downloads auto-diagnostics remain active with entries for
  `native tab_bar background`, `theme=DARK`, `gesture:`,
  `pill geometry`, and `edge-space:`.

**Important:** the shader now adds GPU-rendered, reactive highlights and a
translucent dark surface; it is **not** live refraction or live background
blur. The original repeated full-screen content capture, which caused
stutters in v0.9, remains disabled. Actual appearance and native tab
dispatch must be tested on the phone; a successful build is not a UX test.

### Test / rollback

1. Download the latest **successful** Build LSPosed APK from GitHub Actions
   for the `0.1-dev-r2-dark-glass` version and install its `app-debug.apk`.
2. Keep LSPosed scoped to Instagram, force-stop Instagram, reopen it.
3. Test normal taps on all five tabs, then **drag the highlighted lens**
   between different icons in the left pill. Test vertical feed scrolling
   and swiping into Direct messages without slowing down.
4. Open Story/status and your profile; return using the native Back gesture.
   Verify the floating pills are hidden on supported screens.
5. Locate `IGGlass-0.1-dev-*.txt` in **Downloads**, and share it with a
   screenshot if the dark region remains. Check the log for
   `edge-space: reclaimed`, `gesture: ... END`, and `theme=DARK`.
   The file name uses the original development series label intentionally.
6. If touch/navigation is broken, disable this module in LSPosed and
   force-stop Instagram. You can revert to the successful v0.8 artifact
   (commit `97504f480f809b87da90eb0802c347efd78522cd`).

## Current: **0.1-dev clean restart** (October 2026)

Rebuilt from scratch after v0.9 testing showed black regions, stuttering
during swipes and lost glass state after navigation. This is an experimental
development version, **not** a proven liquid refraction shader.

- Retains **the real Instagram 452 tab buttons**, separate Search circle and
  a four-button main pill. Glass surfaces now use GPU-friendly translucent
  gradients, highlight layers and a moving selection lens.
- Raises navbar by **26dp**; no extra full-window bitmap render, Canvas host
  screenshots or double 12fps blur sampling. This should reduce jank, at the
  expense of true content-backdrop blur/refraction.
- Restores the device-tested v0.8 targeted **180px bottom margin recovery**,
  and re-applies it if Instagram reintroduces the reservation after switching
  screens. Keeps the legacy 1px `tab_bar_shadow` hidden.
- Does not swallow gestures or invent replacement navigation actions:
  the native Instagram tab views keep their click handlers.
- Watches Instagram's own tab visibility and large Story/profile fragments,
  hiding both floating glass controls on those screens and showing them on
  return to the feed.
- Automatically creates a UTF-8 diagnostic file named
  **`Download/IGGlass-0.1-dev-YYYYMMDD-HHMMSS.txt`** via Android MediaStore.
  Android 10+ allows creating app-owned Downloads files without storage-wide
  permissions; because hooks run inside Instagram, the file is created from
  its process. Logs contain only view/layout and screen class names, actions
  by tab index, timings and errors — no post text or account labels.
  File writes happen off the UI thread and are capped at 512KiB. If
  MediaStore denies creation, the `IGGlass-DEV: DOWNLOAD LOG ERROR` line
  appears in LSPosed and there may be no file to share.

### Install and test

1. Open [GitHub Actions](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions),
   find the newest **successful** `Build LSPosed APK` run on `main` and
   download `Instagram-LiquidGlass-LSPosed-debug`.
2. Extract `app-debug.apk`, install/update the module, enable Instagram-only
   scope in LSPosed and force-stop Instagram. If signing key mismatch prevents
   an update, uninstall the **module only**, never Instagram.
3. Test opening Home, Reels, Share, Search circle, Profile; back from Profile;
   swipe to Direct messages and return; scroll feed and play video; open a
   Story/status, exit and ensure the floating controls reappear.
4. Open **Files > Downloads** and find the newly created
   `IGGlass-0.1-dev-...txt` (one per Instagram process launch). Check it
   contains `glass installed`, `fixed reserved bottom margin`,
   `screen=` and tab action lines. Upload it plus a screenshot if anything
   is broken.

**Known limits:** the effect is optical *glass styling*, not Apple's native
iOS Liquid Glass physics. Instagram may alter private view hierarchies or
ignore file creation via MediaStore; runtime tests remain essential.
Other LSPosed modules such as InstaEclipse can affect navigation behavior.

### Rollback

The last visually confirmed black-strip fix was **v0.8** (Git commit
`97504f480f809b87da90eb0802c347efd78522cd`). To compare, use
its successful GitHub Actions artifact or restore the corresponding source
commit. Don't install an APK signed with a different debug key without
uninstalling the old module app first.

## v0.9 — raised split Liquid Glass and Story/Profile auto-hide (beta)

Target: **Instagram 452.0.0.48.64 / Android 16**. Builds on the device-tested
v0.8 fix that reclaimed the original 180px Instagram navbar reservation.

- Raises the two floating controls from **12dp to 30dp** above the bottom.
- Keeps four **original Instagram buttons** (Home / Reels / Share / Profile)
  inside the left glass pill, with a glowing sliding selection lens.
- Relocates Instagram's **original Search tab** into its own circular glass
  control on the right. This preserves original listeners instead of replacing
  them with simulated navigation. If the verified five-button hierarchy cannot
  be found, safely retains one pill rather than detaching arbitrary views.
- Independently samples/softens the background behind both glass controls.
- **Auto-hides both controls** when Instagram hides the original tab view,
  or when the foreground screen is recognized as a Story/ReelViewer or user
  profile fragment/activity; shows them again on return to the feed.
- Leaves the tested 180px feed-margin fix intact and does not touch Android's
  system navigation bar or permanently hide content views.

### Test checklist

1. Download a **successful v0.9 Build LSPosed APK** run and install its
   `app-debug.apk`; keep LSPosed scoped only to Instagram.
2. Force-stop/reopen Instagram. Confirm you see a **four-icon left pill**
   and an **independent Search circle on the right**, raised from the bottom.
3. Tap each main icon and the separate Search circle. Verify selection lens,
   tab navigation, icons/badges, long press, scrolling and touches.
4. Open a **Story/status**, return to the feed; open **your profile and another
   user's profile**, then return. Both pills should disappear on those pages
   and reappear on the home feed.
5. If either page keeps the floating bar, send screenshot + LSPosed logs with
   `IGGlass-SCREEN:` and `IGGlass v0.9:` lines. These log only screen class
   names, not usernames/content.

**Compatibility note:** GitHub Actions success verifies Java compilation,
not whether Instagram's internal fragment names matched the device. This beta
may need one additional runtime log cycle for Story/profile detection.
If navigation becomes unusable, disable the module in LSPosed, force-stop
Instagram and re-enable v0.8.

## v0.8 — targeted 180px navbar-space fix (beta)

The runtime logs from Instagram 452.0.0.48.64 show that the original
`tab_bar` is 180px high, with an opaque `#FF0C1014` background. After v0.7
moves it into the floating glass shell, two **real content views** retain
`bottomMargin=180`:

- `layout_container_main` — live feed container.
- `swipeable_tab_view_pager` — the swipeable feed tabs.

The adjacent 1px `tab_bar_shadow` also remains visible. This makes the
previous strategy of collapsing small, empty wrappers ineffective.

v0.8 checks the verified `layout_container_main_wrapper` and both child IDs
with their exact reserved margins, then changes the **two content bottom
margins from 180px to 0**, optionally releases the matching hidden bottom
searchbar stub margin, and hides the old 1px shadow. It never hides the
feed or ViewPager, touches unrelated layouts, or changes the system navbar.
If the runtime layout differs, it falls back to v0.6's conservative behavior.

### Test on Instagram 452 / Android 16

1. Download the **latest successful Build LSPosed APK** on main, install
   `app-debug.apk`, and enable it with Instagram-only scope.
2. Force-stop and reopen Instagram. Inspect the area around the floating pill
   on the home feed and swipe up/down.
3. Verify all five tabs and their click/long-press behavior, Reels playback,
   profile and explore. Watch for content being covered unexpectedly.
4. In LSPosed module logs, confirm `IGGlass: released 180px reserved bottom
   space for feed and pager` and the `after-floating` report showing
   `bottomMargin=0` and the shadow gone.
5. Send a screenshot of the result and log lines beginning `IGGlass:` if a
   dark band remains or the page overlaps incorrectly.

This is a **testable hypothesis from a real device log, not a visually
verified fix**. The last ~60px of screen space in the diagnostic log may still
be managed separately by Instagram/Android; v0.8 does not override that.
Disable the module and force-stop Instagram if behavior is broken.

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
