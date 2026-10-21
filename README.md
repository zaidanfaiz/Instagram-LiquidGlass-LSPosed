# Instagram Liquid Glass — LSPosed

**Experimental Android LSPosed module**, tested as a build project for Android 16 / Instagram 452.0.0.48.64. Not affiliated with Instagram.

## v0.2-floating-beta

- Attempts to move the Instagram bottom tab row into an **inset floating overlay** above the feed (14dp left/right; 15dp bottom).
- Uses a **genuinely translucent** layered gradient, subtle highlight/stroke and elevation.
- Keeps the original Instagram navigation view and click listeners; does not create fake buttons.
- Falls back to a scaled/transformed bar if relocation is unsuitable.
- Uses a heuristic to find the navigation view; compatibility is **not guaranteed** on every Instagram release.

**This is not actual iOS Liquid Glass:** no refractive shader and no sampled backdrop blur. Android view transparency alone only reveals what is already drawn behind the bar; if Instagram doesn't draw content underneath, the area may still look dark. The experimental reparent operation may interfere with Instagram's layout; disable the module in LSPosed if navigation breaks.

## Build without a computer

1. Open **[Actions](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions)**.
2. Select **Build LSPosed APK**, and if necessary use **Run workflow**.
3. Open a successful run and download the `Instagram-LiquidGlass-LSPosed-debug` artifact.
4. Extract the ZIP to get `app-debug.apk`.
5. Install the APK, activate it in LSPosed with **Instagram** as its only application scope, then force-stop/reopen Instagram.

> GitHub Actions uses a new debug signing key on fresh runners. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE` while updating the module, uninstall the **old module app only** and install the new build. Do not uninstall Instagram just to fix module signing. Then re-enable the module in LSPosed.

## Compatibility

- Requires an operational LSPosed framework that supports **legacy Xposed API 82**.
- Targets the Instagram package `com.instagram.android`, not Instagram Lite.
- Built with Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3, and compile SDK 35.
- The APK must be tested on-device; **a successful CI build does not prove visual compatibility**.

## Troubleshooting

If the bar disappears, taps don't respond, Instagram crashes, or layout looks wrong: disable this module in LSPosed, force-stop Instagram, reopen it, and report your Instagram version plus an LSPosed log. Because the module hooks the app UI, minor Instagram updates can invalidate the heuristic.

## Files

- [Hook source](app/src/main/java/dev/zaidan/igglass/InstagramGlassHook.java)
- [Automatic APK build workflow](.github/workflows/build-apk.yml)

Do not distribute this as a verified fully functional iOS 26 Liquid Glass implementation.
