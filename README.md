# Instagram Liquid Glass — LSPosed

Experimental module for Instagram Android (targeted: 452.0.0.48.64) on Android 16. This is **not affiliated with Instagram**.

## v0.3 — Frosted floating navigation beta

- Reuses Instagram's original clickable navigation controls.
- Moves the tab bar into a rounded floating pill (14dp inset from side edges and bottom).
- Samples a small part of the application window behind the bar every 900 ms, blurs the image with Android RenderEffect, then overlays a low-opacity gradient and bright edge stroke.
- Tries sampling the strip above the bar when Instagram draws a blank bottom spacer.
- Falls back to a simple inset if Instagram's private view hierarchy cannot be safely rearranged.

**Limitations:** This is a screenshot-sampling approximation, **not actual iOS Liquid Glass**. The background blur may update with a delay. Hardware-rendered content that cannot be captured via a software Canvas may remain dark. Some screens may not support the relocation; Instagram 452 may behave differently on other devices. The module is experimental and has not been run on your phone.

## Download/build APK on your phone

1. Go to [GitHub Actions](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions).
2. Open the newest successful **Build LSPosed APK** run.
3. Under **Artifacts**, download **Instagram-LiquidGlass-LSPosed-debug**.
4. Extract the ZIP, install `app-debug.apk`.
5. Enable the module in LSPosed, and set **Instagram** as its only scope.
6. Force-stop Instagram, reopen it, and test all five tabs.

GitHub Actions generates a new debug signing key on some runs. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall only the **module app**, reinstall the new debug APK, and enable it again. Do not uninstall Instagram for the signature mismatch.

## Recovery if Instagram UI glitches

Disable the module in LSPosed, force-stop Instagram, and reopen Instagram. If the UI still breaks, reboot the phone with the module disabled. Send an LSPosed log and a screenshot for compatibility debugging.

## Implementation notes

- Legacy Xposed API 82; `com.instagram.android` is the only hooked package.
- Compile SDK 35, Java 17, Gradle 8.9 / AGP 8.7.3.
- The CI test confirms source compiles into an APK, **not** that Instagram's UI layout is compatible.

Source: [InstagramGlassHook.java](app/src/main/java/dev/zaidan/igglass/InstagramGlassHook.java).
