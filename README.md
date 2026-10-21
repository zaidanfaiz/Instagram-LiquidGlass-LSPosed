# Instagram Liquid Glass — LSPosed

Experimental LSPosed module targeting **Android 16 / Instagram 452.0.0.48.64**. This project is not affiliated with Meta or Instagram.

## v0.5 — compact floating glass (beta)

Compared to v0.4:
- The floating pill now has **16 dp** side insets and **12 dp** bottom offset to avoid the excessive empty area below the navigation in screenshots.
- A `Choreographer` vsync callback responds to finger motion and a `ViewTreeObserver.OnScrollChangedListener` responds to flings/scroll movement.
- Sampling updates at most **12 frames per second during interaction**, replacing the old fixed 900ms delay. At rest it refreshes about every 550ms to detect newly loaded feed images.
- During rapid scrolling, expensive fallback sampling of the strip above the bar is skipped; the app can remain more responsive. When idle, the fallback still runs if the real backdrop is blank.
- The screenshot sampler excludes the glass overlay when drawing underlying content.
- Preserves Instagram's own tab navigation and touch handlers.

**Known limitations:** The glass is a blurred sample of the underlying Android hierarchy plus a translucent tint, not Apple's native real-time physical refraction shader. Certain hardware-accelerated video/SurfaceView content may render blank in offscreen captures. If Instagram itself reserves bottom space, the feed may still not render underneath the pill. Do not mistake the 52dp offset in v0.4 for Android screen scaling. A loading spinner in a post may be caused by Instagram/network, not solely by the module. Exact integration requires hierarchy logs from the device and more app-specific work. This experimental module may be slower or glitch on some phones. A successful CI build does NOT prove it works in Instagram.

## Research references

- [liquid-glass-react](https://github.com/rdev/liquid-glass-react) — React browser-based refractive shader and touch effects
- [liquidGL](https://github.com/naughtyduk/liquidGL) — browser-based GPU glass with scroll syncing
- [Prismal](https://github.com/styropyr0/Prismal) — native Android GL shader, backdrop sampling and vsync interaction; future direction for a true refractive effect

These projects are **design/architecture references**. Their shader code has not been copied into this source.

## Build without a computer

1. Open [GitHub Actions](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions).
2. Open the newest **successful** run of **Build LSPosed APK**.
3. Download the `Instagram-LiquidGlass-LSPosed-debug` artifact and extract the `app-debug.apk`.
4. Install the APK; enable the module in LSPosed with **Instagram only** as scope.
5. Force-stop Instagram and reopen it to test scrolling and tapping all tabs.

If debug APK signature mismatch prevents updating, uninstall **only the module app**, not Instagram, install the new one, and re-enable the module in LSPosed.

## Recover from issues

If Instagram freezes, crashes, nav buttons stop responding, or the feed stops scrolling, disable the module in LSPosed, force-stop Instagram and reopen. Report your Android/Instagram version, screenshot and relevant LSPosed log for debugging.

## Development

The code uses legacy Xposed API 82, Java 17, Android SDK 35, Gradle 8.9, and AGP 8.7.3.

[Main Java hook](app/src/main/java/dev/zaidan/igglass/InstagramGlassHook.java) · [GitHub Actions workflow](.github/workflows/build-apk.yml)
