# Instagram Liquid Glass — LSPosed

Experimental module for **Android 16 / Instagram 452.0.0.48.64**. Independent project; not affiliated with Instagram.

## Latest experimental test: **0.1-dev-r15 — fullscreen margin reconciliation + visible frost**

Device feedback after r14: **both the Home feed and Reels still show an
unwanted bottom gap after navigating tabs, and the pill looks clear rather
than distinctly blurred**. The user-supplied 22:41 device log proves:

- App/window/decor/content are still **1440x3200**; navigation inset is
  **zero**. It is not a smaller window or Android not in immersive mode.
- The r14 hook logs **repeated 60px coordinator margin interceptions**,
  but NOT a single 180px native feed/pager interception. Thus assuming the
  intercepted `View.setLayoutParams` path reliably protects both native
  reservations was unfounded.
- The r14 screenshot sampling reports a valid **305x41px 58%/5dp sample**.
  Its pending capture timer was reset for every scroll event, leaving the
  effect absent during continuous feed scrolling.

### Changes

1. **Native pager margins:** while on a visible, focused NON-Reels main
   tab, verify and repair precisely those previously confirmed
   `layout_container_main` / `swipeable_tab_view_pager` / optional
   `layout_bottom_searchbar` **180px** margins if IG reapplies them.
   Poll no faster than 1.1 seconds; change nothing if margins are already
   zero or unexpected. A partially restored wrapper (one holder zero,
   the other 180px) no longer skips the entire fix. Never relayout while
   Reels is selected or an opened chat/comments/modal hides the glass.
   The 60px root coordinator remains covered by the proactive margin hook.
2. **Observed fullscreen dimensions:** periodic `r15 feed geometry`
   messages now record the native wrapper height and each known pager's
   measured height/bottomMargin, not only the 1440x3200 system bounds.
   This can identify the exact culprit if the gap persists.
3. **Visible content-aware blur on nonvideo tabs:** sample increased from
   23% to **30%** of the narrow glass strip, blur RenderEffect radius from
   5dp to **14dp**, and real backdrop contribution from 58% to **84%**.
   New samples use a **750ms minimum interval** and pending captures
   are no longer repeatedly postponed by scroll events. Old images fade
   during scroll and are replaced with a new verified background.
   Flat/blank captures and slow operations still fall back to the
   known-good transparent rim.
4. Reels **NEVER receives** live video sampling; no new
   SurfaceView/TextureView capture, video relayout, player hooks or
   immersive window flags. The r12 video playback stabilization,
   separate Search button, custom icon sizes, and r14 gesture shield
   remain intact.

### Device acceptance (this has NOT been visually verified yet)

- Home → Reels → DM inbox → own profile → Home, then scroll the feed.
  Confirm bottom of the content is not padded to make room for Instagram's
  old 180px tab bar.
- On static colorful photos, the glass should visibly soften the image
  behind it. During sustained scroll, it should refill rather than remain
  permanently clear. On Reels it is intentionally clear until a
  separate video-safe optical compositor is designed and tested.
- Reels -> open/close comments 3x: verify audio and picture remain in sync,
  and dragging on the capsule does not activate Reel fast-forward.
- If any check fails, send the latest `Downloads/IGGlass-0.1-dev-*.txt`,
  especially `r15 fixed reserved bottom margin`,
  `r15 feed geometry` and `r15 glass-frost sample`, and one screenshot
  after the tab transition.
- Disable the LSPosed module and force-stop Instagram to rollback without
  clearing personal app data.

**Limitation:** Android `RenderEffect` on our sampled View is a delayed
frosted-background approximation. There is no verified live
system-compositor refractive blur of SurfaceView Reels and no claim of
pixel-identical iOS 27 Liquid Glass.

## Latest engineering test: **0.1-dev-r14 — gesture isolation + proactive fullscreen margins**

Based on the user's Android 16 screenshots and the two logs from
22:12:52 and 22:13:07 on October 21, 2026:

- The 1440×3200 window/decor/content reported full dimensions and the
  navigation inset was **0**, so the black bar is NOT explained by a
  smaller Android window. IG452 itself reinstated **180px native tab
  reservation and 60px root margin** after navigation; this was visible
  in the r13 log when the late repair occurred after returning to a
  static tab. The 6.5s periodic fix can be postponed by an active
  SurfaceView and is an inadequate permanent solution.
- A Reels drag can leak `ACTION_DOWN` to the playback surface before the
  old horizontal gesture detector takes ownership, triggering Instagram's
  hold-to-fast-forward action instead of sliding the selection.
- The Home glass had a technically valid tiny background sample, but its
  blur was visually subtle; Reels remained clear by design to protect
  video.

### r14 implementation

**Fullscreen: guard the layout at its source.** Only verified IG452
`layout_container_main`, `swipeable_tab_view_pager`,
`layout_bottom_searchbar` and
`layout_container_center_right_coordinator_layout` View objects are
registered with `DevNativeMarginGuard`. It intercepts their exact
`View.setLayoutParams` call only if the bottom margin equals the
verified 180px or 60px native reservation and changes the incoming
margin to zero **BEFORE Instagram performs its own layout**. There is no
second post-layout correction while Reels is decoding, and no global
fullscreen/window flags are modified. Hooks are deregistered on teardown,
preserving original margins.

**Reels touch conflict:** `DevGesturePill` captures touch DOWN immediately
inside its rectangular capsule instead of waiting for horizontal motion
to exceed touch slop and potentially dispatching a seek/fast-forward
hold into the playing Reel. It supports horizontal slide and tapping;
both actions call the original Instagram tab listener via
`performClick`/native fallback. The actual search pill remains a
separate native target. Finger movements outside the capsule remain
owned by Instagram. Avoid routing a touch both through the capsule and
the Activity-level UP hook.

**Frost / optical:** the Home-only cached bitmap renderer retains the
non-video 23% sample, slow-capture rejection, scroll-frame invalidation,
and blank-frame rejection from r13. It now blends **58%** of the sampled
GPU-blurred view (5dp blur radius on the already-downscaled frame),
instead of 42%/11dp. Since actual pixels are sampled, there is no
white/black material fill. Reels video remains **UNSAMPLED**; preserving
working playback is the priority.

**HyperLight research:** HyperLight 1.1.7 (API 101) documents a
multi-stage liquid-glass pipeline: background-capture scaling,
configurable frame-refresh budget, chromatic dispersion, refraction,
outline, and touch halo. The public repository currently publishes
documentation and release APKs for a HyperOS/SystemUI-specific module
with no directly portable Instagram implementation. We do not copy
non-licensed binary/decompiled code into this module. The uploaded
binary has **not** been decompiled/inspected in the sandbox. The
supported public docs informed the design, not an assertion of access
to HyperLight's proprietary renderer.

### Mandatory device acceptance tests

1. Home → Reels → DM inbox → own Profile → Home. The Home feed should
   continue to fill the entire 1440×3200 app window, without waiting
   6.5s for the empty bottom area to disappear.
2. Start a Reel. Drag a finger horizontally across the GLASS PILL
   (not the video). The selected lens should switch tab in one attempt.
   Reels should **not** enter hold-to-speed mode.
3. Reels comments open → close 3×: both audio and picture must continue.
4. Check the Home effect over a detailed static picture: fine
   content-aware blur, not a white slab, and no leftover ghosted
   image while scrolling. Reels remains clear with a rim.
5. Attach the Downloads log. Check for `r14 prevented native bottom
   reservation`, `r14 isolated pill tap`, and
   `gesture: glass selection drag END`.
6. If video playback or navigation regresses, disable the LSPosed
   module and force-stop Instagram to restore the original experience.

*This is not a verified iOS 27 live compositor recreation.* It is a
conservative prototype preserving user-confirmed video stability.

## Latest test: **0.1-dev-r13 — safe fullscreen recovery + optical frost**

Target: Instagram 452.0.0.48.64, Android 16, LSPosed. The r12 device
feedback **confirms Reels picture + audio continue normally** after comments.
Two remaining requests are stronger iOS-style liquid-glass blur and Instagram
no longer filling the screen.

### Fullscreen / content viewport

The r12 log shows Instagram's old **180px bottom tab-bar reservation** being
removed and a separate app-owned **60px root coordinator gap** being reclaimed
once at installation. In r12, all subsequent reconciliation was disabled to
protect Reel video surfaces. Instagram 452 can reset these margins during
tab/page navigation; that can reintroduce the bottom content gap.

r13 now *checks and repairs* only those same previously verified **60px and
180px Instagram-owned margins**, at most once every 6.5 seconds while a
non-Reels main tab is shown **and no large visible SurfaceView/TextureView is
active**. It never changes layout mid-Reels or during a comment overlay, so
the r12 frozen-video mitigation remains. If margins are already correct,
the guard does not cause a relayout. This is a conservative fullscreen/content
recovery, **not forced Android immersive mode**: the status bar and system
navigation gestures remain managed by Instagram/Android.

On every safe check, the Downloads logger adds a `viewport [safe-static-tab]`
line containing decor/content geometry, system navigation/status insets and
window flags (no personal data). This lets us distinguish an Instagram
padding regression from an actual Android edge-to-edge/window problem.

### Blur / Liquid Glass

- The original r10 successful **fully transparent glass shell**, narrow
  reflective rim, compact original Instagram icons and moving active lens
  remain unchanged.
- On **static Home/feed content only**, a tiny cached screenshot is drawn
  behind the glass via Android GPU `RenderEffect`: blur radius **11dp**
  and **42%** blended actual backdrop pixels. Unlike an opaque white
  rectangle, this is a blurred version of the sampled content rather than
  a fixed white/dark material. It only appears after a high-confidence,
  nonuniform backdrop capture.
- Old samples are discarded immediately while feed content scrolls, so
  stale frames cannot remain behind the bar.
- A real video SurfaceView/TextureView in the capture strip suspends the
  effect, and a too-slow sample disables it for the session.
- On **Reels**, the video SurfaceView is never captured, resized or
  manipulated. The user-confirmed transparent rim-only fallback remains,
  including when comments open. Do **not** mistake this for iOS's genuine
  live video compositor blur/refraction; that remains an unimplemented
  limitation of this safe variant.

### Tests

1. Install the **r13** debug APK from the latest successful `Build LSPosed
   APK` run. Force-stop Instagram and reopen.
2. From Home open a static photo. The pill should show a visible but subtle
   blurry background, still showing the photograph through it.
3. Swipe quickly between Home, Reels, DM inbox and your own profile. Verify
   content returns to the **bottom of the display** without a fresh black
   region, and glass remains in front of content.
4. On Reels repeatedly open comments and close them (3+ cycles). The navbar
   should hide during comments, and picture/audio should not freeze. This
   regression must remain fixed.
5. Upload the latest `Download/IGGlass-0.1-dev-*.txt` and one screenshot
   showing any remaining not-fullscreen area. Compare
   `viewport [install]`, `viewport [safe-static-tab]`, system insets,
   `edge-space:` and `fixed reserved bottom margin`.
6. If the app still doesn't fill the screen, **do not enable forced immersive
   flags blindly**: first use those measurements and the screenshot to
   distinguish Android navigation-bar safe area, Instagram layout padding,
   and video letterboxing.

The shader libraries researched for later refraction remain documented in
`THIRD_PARTY_NOTICES.md`. This build intentionally prioritizes video
stability over real-time video glass to avoid regressions.

## Latest experimental build: **0.1-dev-r12 — Reels comments video safety + subtle frosted glass**

The reported r11 Android 16 / Instagram 452 bug: a Reel video plays correctly,
but opening its comments can leave the custom navbar visible and, after the
comments close, the video image may freeze while audio keeps playing. There is
no hard evidence the decoder crashed. Device log shows repeated application
and restoration of a **verified 60px root coordinator margin** as the main
Activity loses focus and the nav bar becomes hidden. On some SurfaceView
stacks, changing the parent geometry during a dialog/sheet transition is
a plausible source of stale video presentation. **This build is a mitigation
to be tested on device, not a confirmed fix yet.**

### Changes

- Instagram's verified 60px app-owned root gap and 180px native tab reservation
  are fixed ONCE during installation. The controller does NOT repeatedly call
  `setLayoutParams` every 500ms when the focus or comment sheet changes,
  does NOT restore the root margin in `onPause`, and restores root state
  only on final safe teardown. Preserves native SurfaceView geometry.
- Floating glass pills hide with `View.INVISIBLE` rather than `View.GONE`.
  Their size and location remain fixed during comments and dialog transitions.
- Added separate `REELS_COMMENTS` page classification. Requires independent
  comments-specific signals: a large comments bottom sheet plus header/input
  or a comments heading plus an active comment composer/list. A tiny comments
  button/caption is NOT enough. Comments are not logged or recorded.
  The floating nav stays **hidden while comments are open**, and returns
  after the sheet closes.
- **Subtle GPU-frosted blur for static Home content only.** A tiny
  `23%`-resolution offscreen sample is drawn at most once every 1.8 seconds,
  after feed scrolling settles; ImageView's RenderEffect adds 6dp GPU blur.
  The sampled layer has a **hard alpha of only 16%**, never fully white/
  black. Uniform, blank or transparent captures are rejected, and captures
  taking longer than 28ms permanently disable frosting for the session.
- **Reels stays the safe r10/r11 fully transparent rim-and-lens material.**
  Video SurfaceViews and TextureViews are *never captured or drawn*, and the
  frosted layer is cleared when Reels or a comment sheet is active.
  There is NO active video PixelCopy or AGSL compositor sampling in r12.
- Maintains r11's ~22dp icons, separate Search button, swipe-to-select,
  own profile and DM inbox visible, other people's profile / opened DM chat
  / Story hidden.

### On-device regression checklist

1. Install the latest successful r12 debug APK from GitHub Actions.
2. Play a Reel -> open the comment sheet -> verify BOTH pills disappear ->
   close comments -> verify the video *picture and audio* are still moving
   together. Repeat 3 times, swipe to the next Reel, and also test while audio
   is playing. If the issue persists, compare with the module disabled;
   this distinguishes Instagram playback from an LSPosed regression.
3. Swipe Home -> Reels -> own Profile -> DM inbox -> Home. Native buttons,
   sliding lens, original icons and Search should still work.
4. Check Home on a *static photo*: the frost should be very subtle, no white
   rectangle. Reels remains clear with a narrow glass highlight.
5. Attach the automatically saved `Download/IGGlass-0.1-dev-*.txt`.
   Look for `r12 playback safety`, `page routing ... REELS_COMMENTS`,
   `screen=hidden:reels-comments-overlay`, and `r12 safe-frost`.
   A missing comments detection line means Instagram's comments hierarchy
   differs; provide a screenshot with the sheet visibly open for a targeted
   view-tree probe.

### Technical limitation

The r12 blur is not the actual iOS system-compositor backdrop filter. Proper
real-time refractive blur of Instagram Reels (especially SurfaceView video)
needs verified frame access / a suitable GPU pipeline. Re-enabling live
SurfaceView capture before the frozen-frame bug is solved is intentionally
avoided. The r12 priority is correct and safe Reels playback and UI hiding.

## Latest test build: **0.1-dev-r11 — compact native icons**

The user's r10 device screenshot confirms that **both capsules now remain
transparent**, with a subtle white glass rim and draggable selected lens.
However, the icon artwork is much too large.

This is a surgical adjustment **only to visual artwork dimensions**:
- The original Instagram ImageView drawable was previously rendered at
  `side = 1.42 × visualRadius`, yielding an approximately **46dp-wide** image.
  r11 now renders it at `side = 0.86 × 12.8dp`, or about **22dp across**.
- Hand-drawn fallback icons and the actual profile picture are also
  scaled down proportionally, instead of the previously exaggerated outlines.
- The separately floating Search magnifier is reduced from `20.5%` to
  `15.5%` of the circular touch target's width for a balanced appearance.
- Native tab hitboxes, native clicks, swipe-to-select, page visibility,
  and **r10 rim-only fully transparent material** are preserved without
  restoring any cached bitmap, AGSL backdrop, color fill or blur.
- The `Download/IGGlass-0.1-dev-*.txt` diagnostics remain enabled.

Test Home, Reels, DM inbox, own profile, and separate Search: icons should
look much closer to Instagram's native sizes without sacrificing the clear
pill or interaction. Device rendering has not yet been verified.

## Current diagnostic build: **0.1-dev-r10 — rim-only optical isolation**

The r9 phone screenshots and `IGGlass-0.1-dev-20261021-204619.txt`
showed a **white / pale-cyan full-width bottom capsule** across Home,
Reels, Direct inbox and the owner's profile, despite:
- `r9 CLEAR BASELINE: snapshot/SDF PixelCopy layers disabled`
- functional native tab click and drag logs, including successful transitions
  to index 1, 2, 3 and 0
- the original Instagram bar already hidden visually via `alpha=0`

That rules out the **snapshot/SDF code** as the sole cause of the r9
opaque surface; it does **not** establish which other child rendered white.

### Stricter visual isolation

- Removed `DevLiquidFx` entirely from the **rendered shell hierarchy**.
  Even a transparent AGSL shader compiled into a full-rect hardware layer
  is not allowed to participate in this diagnostic test.
- Removed the passive full-sized `DevGlassSurface` layer. Both the main
  capsule and Search circle now contain only a narrow **`DevGlassRim`
  paint-stroke**, with **no filled background**, no gradient, no passive
  bitmap, no per-frame sampling and no full-screen or full-pill shimmer.
- The moving **selected lens** is the only component with a very low-alpha
  wash. It still slides between original Instagram tab hit targets.
- Independent icon renderer uses narrow **white single-stroke glyphs**
  rather than r8/r9's thick black double outlines; native Instagram
  `ImageView` drawables remain preferred where compatible.
  Search uses a thin white magnifier outline.
- Original click targets, detached Search, owner-profile/DM-inbox visibility
  routing, hidden other-user profile / opened chat / Story rules, and the
  already-working 180px and guarded 60px bottom-margin fixes are preserved.
- The Downloads logger now records
  `r10 STRICT_GLASS: null shell background, rim-only children`
  and `r10 VISUAL ISOLATION` so a report can be attributed to the build.

**Do not call r10 finished iOS Liquid Glass.** This is an isolation experiment:
without a captured live backdrop it can show a truly clear base with bright
reflections and a sliding lens, but cannot reproduce the reference's
content-aware blur/refraction. If the pill **still turns white** despite
only stroked rims, the source is likely an Instagram/window layer or another
unexpected compositing interaction; use fresh diagnostics before another
shader revision.

### Reproduction

1. Install the successful `0.1-dev-r10-rim-only-isolation` debug APK,
   force-stop Instagram, reopen with LSPosed scope on Instagram only.
2. On a bright Home post, swipe Home → Reels → DM inbox → own Profile
   → Home → Explore. Return and scroll Home 3+ times.
3. Check BOTH main bar and Search circle. The pixels inside their
   unselected areas should be **exactly the native Instagram background**
   visible around them; no milky white rectangle should follow a tab.
4. Verify native tap, slide-to-select and avatar, profile routing and
   search. Upload screenshots + `Download/IGGlass-0.1-dev-*.txt`.
5. If new icon rendering causes accessibility or navigation issues,
   disable the LSPosed module and force-stop Instagram; no app data reset
   is required.

## Current test: **0.1-dev-r9 — true-clear baseline + real native icon drawables**

After the r8 phone screenshot showed **the pill still filling white after a DM /
Profile tab transition**, despite `nav.setAlpha(0f)`, the hypothesis that
the sole cause was Instagram's native tab rendering was disproved.

### Exact isolation applied

- Temporarily **DISABLE the entire cached `DevGlassBackdrop` capture and
  `DevRefractiveBackdrop` AGSL image layer** at module install time. None
  of the frosted/refractive screenshot surfaces are inserted into either
  pill in this diagnostic build. This is deliberate so we can verify that
  changing tabs can no longer make a white rectangle.
- The **main shell has no body fill at all**. Only the thin glass rim is
  painted. The moving selected lens has a very low-alpha translucent
  wash (not white); its specular glint remains.
- Remove the whole-pill permanent white gradient and idle AGSL ripple.
  The touch ripple briefly appears **only while touched**, then fades.
- Maintain the original Instagram View hierarchy as **touch-only with
  alpha=0**, preserving native taps and sliding navigation. The native
  alpha is restored if the module tears down the injected scene.
- Restore **native Instagram icon assets where possible**: the independent
  `DevNavGlyphs` layer now attempts to reuse `ImageView.getDrawable()`
  from each actual Instagram tab, rather than always drawing the enlarged
  custom outline from r8. Non-ImageView icons receive smaller,
  better-proportioned fallback paths. The original profile avatar is still
  attempted before a generic profile glyph.
- Keep r7's verified Home / Reels / DM-inbox / own-profile page routing and
  hide on opened conversations, other profiles and Story.

**The tradeoff:** This is a **transparency control build**. It demonstrates
whether the white slab came from snapshot/shader rendering, but it cannot
produce accurate real-time iOS content refraction while the sampling layer
is disabled. It still has specular glass edges and a draggable active lens.
Once actual on-device screenshots confirm the shell is clear on BOTH
bright feed and dark Reels after several tab changes, a correctly clipped,
low-alpha and self-resetting glass refraction can be restored without
reintroducing opaque captures. Do not claim this is already equal to
the supplied Dribbble Liquid Glass concept.

### r9 testing (important)

1. Install the latest r9 debug APK and force-stop Instagram.
2. Open Home and Reels. Slide to Direct inbox, own profile, then back to
   Home, repeatedly. Confirm **no white capsule persists**.
3. Check that original icon assets (where available) show instead of r8's
   comically thick double outlines, and every native tap works.
4. Confirm own profile and inbox stay visible, but other's profile and
   opened DM chat hide both pills.
5. Send screenshots + the newest `Download/IGGlass-0.1-dev-*.txt`.
   This build writes `r9 CLEAR BASELINE` when installed and has no
   `SDF optics` or `frost:` lines during normal navigation.

## Current beta: **0.1-dev-r8 — optically transparent native layer isolation**

Follow-up to r7's real device screenshots and log from 2026-10-21.
**Navigation works**, including drag-to-select, DM inbox, own profile and
hiding overlays on someone else's profile. But the visual shell still appears
**solid white/milky** over both bright posts and Reels even though r7's
SDF pipeline reports **at most ~5% center opacity** and its glass surface
paint does not draw a solid background.

### Root-cause mitigation

The Instagram 452 tab row itself is a custom View hierarchy: clearing the
`View.background` cannot stop an Instagram child View from painting an
opaque body from its own `onDraw` implementation. Earlier r7 kept the
original View graphics drawn **above** our transparent material, so its
opaque selected/updated surfaces could cover the glass again after scrolling.

**r8 deliberately separates native hit targets from native visuals.**

- The genuine five-tab Instagram hierarchy (four buttons in the main pill,
  the fifth Search split into its own circle) stays attached with original
  listeners and full touch geometry, but the main native row is given
  `alpha=0` *visually*.
- A new **`DevNavGlyphs`** draws four crisp, independent Home / Reels /
  DM-send / Profile icons above the truly transparent optical shell. It
  attempts to use the existing native Profile avatar image if obtainable,
  otherwise uses a generic profile outline. Its dark contour preserves
  icon readability over bright feeds while keeping a white iOS-style glyph.
  The glyph view has `clickable=false`, so the original invisible native
  buttons still receive taps. The slide-to-select parent is unchanged.
- Search retains its genuine native transparent click target; the
  independent search magnifier now has a dark outline too.
- There is NO opaque glass-body color. The SDF center and rim effects remain
  low-opacity. Original tab `alpha` is saved and restored on rollback /
  Activity teardown; this build also stops destructive clearing of child
  backgrounds on every half-second screen poll.
- Own profile and DM inbox visible; opened DM, other profiles and Story
  hidden — the r7 routing contract is preserved.
- Reels can attempt asynchronous SurfaceView PixelCopy; when unavailable
  it falls back to **genuinely clear material plus optical rim**, not a
  fabricated solid photo.

This is an experimental step toward the reference concept, **not verified
pixel-identical iOS Liquid Glass**. Instagram's native tab view may be
accessibility-sensitive to alpha=0; test click/long-press/accessibility
and force-stop/disable LSPosed if anything stops working.

### Device test

1. Install the latest successful r8 `Build LSPosed APK` artifact,
   `app-debug.apk`; scope Instagram only in LSPosed and force-stop Instagram.
2. Scroll a bright Home post; check **actual underlying post pixels are
   visible through BOTH glass shells** and not milky white.
3. Open Reels and scroll through two videos; the top and bottom navigation
   should not become a white slab. Original video remains responsive.
4. Tap Home, Reels, the DM button, Profile and separate Search. Drag the
   active selection across the main capsule. Check avatar/icon legibility.
5. Confirm own profile + inbox keep the pill, other people's profiles +
   opened DM conversations + Story hide it. Upload screenshots and
   `Download/IGGlass-0.1-dev-*.txt` with any deviations.

## Current experimental build: **0.1-dev-r7 — clear glass on Reels + smart pages**

Target: Instagram 452.0.0.48.64, Android 16, LSPosed.

**Design contract from the user's device test:** do NOT adapt the glass body's
color to Android or Instagram night/light themes, and do NOT let the frosted
sample turn the entire pill white during scroll. The main and Search shells
have **zero-opacity base fill**. Only a narrow optical highlight/rim, movable
active lens, touch ripple and a deliberately faint SDF refraction band are
allowed. Even with a very bright post, SDF interior contribution is capped
to roughly 5% and edge contribution to 29% of the sampled material. If the
background sample is untrustworthy, zero image contribution is the fallback.

**Reels video:** tries a small asynchronous Android `PixelCopy` from the
actual `SurfaceView` (not a screenshot of our overlay); captures the union
strip behind both pills at 22% scale, at most around 0.8 snapshots/second.
Reels pixels can then enter the MIT-licensed SDF refraction shader without
painting over the video or blocking 60fps playback. If no matching
`SurfaceView` is found, or PixelCopy fails, the body stays clear and the
specular rim plus animated active lens stay visible. Software fallback
snapshots of Reels are **disabled**, preventing stale white/black frames.

### Where the custom navbar appears

| Screen | Floating glass navbar |
|---|---|
| Home feed | **Visible**, drag-to-select |
| Reels (including playing video) | **Visible**, drag-to-select |
| Search / Explore | **Visible** |
| Your **own** Instagram profile | **Visible**, drag-to-select |
| Direct Message **inbox / conversation-selection list** | **Visible**, drag-to-select |
| An **opened DM chat/conversation** | **Hidden** |
| **Other people's profiles** | **Hidden** |
| **Stories / statuses** | **Hidden** |

The visibility detector uses several **public UI labels and view resource
IDs together**; it never reads the contents of EditText messages, and no
usernames/messages are written to log files. Instagram 452 does not expose
the standard AndroidX fragment manager here, so screen detection is
best-effort. If a detail screen is misclassified, share a screenshot and
the automatically generated Downloads log; the log now contains transitions
such as `page routing HOME_OR_REELS -> DIRECT_INBOX` and
`screen=hidden:dm-conversation-open`.

**Installation:** download the APK from the *latest successful*
`Build LSPosed APK` GitHub Actions run, install/update `app-debug.apk`,
enable Instagram in LSPosed, force-stop Instagram, then reopen.
If any touch behavior is broken, disable only the LSPosed module and restart
Instagram; do not clear or uninstall the Instagram app.

**Limits:** true iOS-style real-time compositor blur is not exposed to
LSPosed. PixelCopy may fail on protected video surfaces, and the rendering
is a translucent Android approximation. Successful Gradle compilation does
not confirm the physical device's visual appearance.

## Current experimental build: **0.1-dev-r6 SDF optics (MIT adapted)**

Target: Instagram **452.0.0.48.64**, Android **16**, LSPosed.

The user supplied three open-source shader libraries after r5 still rendered
black/white flat pills:
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass):
  high-quality Compose Multiplatform, not immediately compatible with a plain
  Android View injection.
- [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android):
  native Android View implementation with SDF optics and a native/Kotlin
  rendering stack. A promising alternative if this small port is inadequate.
- [QmDeve/AndroidLiquidGlassView](https://github.com/QmDeve/AndroidLiquidGlassView):
  compact Java/Android View AGSL implementation with rounded-rectangle signed
  distance fields, true edge pixel displacement, and chromatic dispersion.

### Actual integration in r6

The module now **adapts the QmDeve MIT shader source** instead of approximating
physical refraction with simple diagonal UV offsets. The new
`DevRefractiveBackdrop.java` embeds the SDF and multi-channel dispersion
algorithms from `core/src/main/res/raw/liquidglass_effect.agsl`.
The upstream copyright and license are preserved in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). The Android 16
`RuntimeShader` uses a low-resolution background `BitmapShader` as its input.

The appearance is **ONE fixed NEUTRAL CLEAR material**, never tied to the
Instagram or Android day/night settings:
- Low-opacity clear silver capsule with a reflective white rim.
- Lighter but translucent selection lens that follows horizontal drags.
- SDF-based physical distortion concentrated near rounded edges, with
  chromatic dispersion only when a reliable captured background exists.
- Separated Search button retaining the original Instagram click target.
- Stable original native navigation and Android content margin fixes.
- Original MediaStore auto-diagnostics in Downloads.

### Why the screenshot may still differ from iOS

Unlike full iOS compositor access, this LSPosed module can only refract
content it successfully samples. `SurfaceView` video may be **invisible to
`View.draw(Canvas)`**. Backgrounds with very low detail, or blank white/black
captures, now disable the SDF sample rather than covering the bar with an
incorrect solid color. The reflective rim and fixed clear material remain.
The snapshot cache is updated when scrolling/interaction settles, not on
every video frame. Therefore the optics are more physically based **but not
a guarantee of 1:1 iOS Liquid Glass over real-time Reels video**.

### Test sequence

1. Download **0.1-dev-r6-real-sdf-optics** successful `Build LSPosed APK`
   artifact, install `app-debug.apk`, scope Instagram only, force-stop it.
2. Check white/bright photos on Home AND Reels video backgrounds. The
   capsule and separate Search should not turn into black/white blocks.
3. Drag across the four main native tabs and try Search. Report if a native
   tab stops responding (disable LSPosed module if unusable).
4. Check Story/status, profile and Direct inbox. Their auto-hide remains
   experimental on Instagram's proprietary navigation implementation.
5. Upload the latest `Download/IGGlass-0.1-dev-*.txt` and screenshots.
   Critical new lines: `SDF optical pipeline loaded`, `SDF refraction+
   dispersion uniform setup`, `SDF optics luma...`, or `SDF shader failed`.

## Current test build: **0.1-dev-r5 glass material fix**

Device feedback from the r4 screenshots and Downloads log: the home and
Reels four-tab bar is almost opaque black, the separately relocated Search
button turns **solid white**, and both glass controls incorrectly stay visible
over the Direct inbox and Instagram profile. The r4 log confirms functioning
180px/60px insets and native drag tab activation, but does not prove the
background visually resembles iOS Liquid Glass.

This revision targets **those specific regressions**, rather than rewriting
native tab navigation again:

- Preserve **all original Instagram tab/click listeners** while clearing
  full-height opaque backgrounds carried by individual tab surfaces. Earlier
  builds cleared only the outer tab row, potentially leaving another opaque
  native layer *above* the custom glass.
- Keep the original Search view physically present as a **transparent touch
  target**, and render a crisp, nonclickable magnifier glyph on top of the
  circular glass button. This prevents Instagram's native white Search-state
  background from washing out the separate glass circle. The original
  view alpha is restored on safe module teardown.
- Lighten both *procedural* glass layers toward the user's reference:
  translucent neutral silver/grey, double-reflection rim, glossy top lip,
  moving active lens, cached background UV refraction and GPU touch ripple.
  Never turn the complete bar into a flat opaque white rectangle.
- Besides exact, visible Instagram resource-ID checks, detect **Direct inbox**
  using multiple fixed local UI labels (e.g. Utama + Permintaan) and
  **Profile** using multiple labels (e.g. Edit profil + Bagikan profil).
  Long/private text is not inspected, and no chat contents or names are
  written to logs. Both pills should be hidden on detected pages.
- The successful v0.8 **180px native tab reservation fix** and guarded 60px
  root app-owned gap correction are unchanged. Snapshot capture remains
  throttled with a slow-device fallback, and automatic debugging continues
  to `Download/IGGlass-0.1-dev-*.txt`.

### Verify on Instagram 452 / Android 16

1. Install **only the latest successful v0.1-dev-r5** GitHub Actions debug
   APK, enable LSPosed scope for Instagram and force-stop/reopen Instagram.
2. On bright Home images and dark Reels verify the four-button pill looks
   smoky translucent glass, not solid black. The Search circle should no
   longer be a white disk and should still **open Search normally**.
3. Normal taps, badge/profile avatar, Reels, swipe-to-select the active
   lens, and scroll/DM gestures must remain operational.
4. Open **Direct inbox**, **your profile**, **another user's profile** and
   **Story/status**. On detected detail pages both custom glass pills should
   disappear and reappear after Back. If any remain, include screenshots
   and logs for targeted detection improvements.
5. In Files > Downloads, upload the latest
   `IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt`. Useful lines include
   `glass: removed native ... panel`, `screen signature`, `screen=hidden`,
   `frost:`, and `gesture:`.

**Limit:** Liquid Glass here is an approximation with cached blurred/reflected
content. It is not pixel-perfect iOS glass, and Android's real-time
SurfaceView rendering is not universally capturable. GitHub Actions success
confirms compilation, *not* the look, frame rate or correct screen detection.
If navigation is broken, disable this LSPosed module and force-stop Instagram;
the last phone-confirmed black-strip fix was v0.8.

## Current test build: **0.1-dev-r4 refractive glass concept**

This revision is guided directly by the supplied Instagram screenshots and
the **translucent silver iOS 26 / Dribbble concept image**. It does not
reproduce a proprietary Apple shader or asset; it implements an independent
Android approximation using cached background imagery and runtime shaders.

### Why v0.1-dev-r3 looked wrong

- The `ImageView` at the bottom of each pill used a high-alpha bitmap of
  Instagram content. A bright frame could paint the **entire search circle
  white**, hiding the search icon. The pills changed between nearly black and
  nearly white based on the content, instead of looking like one coherent
  silver glass material.
- The screen samples themselves completed in ~0–5ms in the supplied logs,
  and drag-to-select events reported handled native clicks. This isolated
  the immediate problem to the **visual composition and contrast**, not
  an app-wide download or broken tab event handler.
- Actual iOS compositor backdrop blur/refraction is not exposed to LSPosed
  inside another Android app. There are limitations with SurfaceView video
  content even when CPU snapshot sampling succeeds.

### r4 visual layers, bottom to top

1. A **neutral smoke/silver translucent capsule** with a consistent palette
   shared by the four-tab pill and separate circular Search.
2. A **texture-aware, low-alpha refracted content snapshot**. Android AGSL
   bends the cached sample near the capsule edge; a small GPU RenderEffect
   softens only this background. Bright or uniform backgrounds contribute
   much less opacity to prevent white-out. A slow capture disables refresh.
3. A light glossy film and a GPU animated shimmer/ripple on touch.
4. A newly implemented **white-blue specular rim** with a high-gloss upper lip,
   plus the animated oval selection lens.
5. The **real Instagram tab icon Views on top**, retaining Instagram's native
   tab behavior and badges.

Additional behavior: 52dp bottom clearance, native horizontal drag-to-select
on the main pill, the separate right Search circle, targeted 180px feed-margin
fix and guarded 60px app-owned root gap fix. The floating overlays also
respect when Instagram hides its original navigation parent (for Story
and other screens). Existing fragment/view-ID detection remains experimental.

### Test and log collection

1. Install the latest successful Build LSPosed APK for version
   `0.1-dev-r4-refractive-glass`. Keep LSPosed scope to Instagram; force-stop
   before reopening.
2. Compare on **Home (bright feed/photo)** and **Reels (dark video)**:
   both pills should stay smoky/silver and icons legible, never turn opaque
   white. Compare the selected lens and glossy edge to the concept.
3. Check Search (circle at right), swipe the selected lens across the four
   native buttons, normal taps, long presses and scroll/DM gestures.
4. Open Story/status and profile detail; both shells should hide and return
   on navigation back. If they don't, report the precise screen.
5. Open **Downloads** and attach `IGGlass-0.1-dev-*.txt` after reproducing.
   In addition to previous entries, r4 logs
   `glass material luma=... texture=... blend=...`,
   `glass refraction fallback` or `refractive draw fallback` when AGSL
   cannot render, and `screen=hidden:instagram-parent-hidden` when relevant.

### Limits

This is **not pixel-identical iOS 26 Liquid Glass**, and it is not real-time
compositor access to Instagram's SurfaceView/Reels. Snapshot refraction is a
best-effort optical approximation. Successful GitHub Actions compilation
does not verify visual output or touch behavior on the user's phone.

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
