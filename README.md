# Instagram Navigation Layout Diagnostic (LSPosed)

**This branch is for diagnosis only. It does not style, move, hide or modify any Instagram view.** It records Android UI structure for the bottom 300dp of the Instagram screen to help locate the opaque rectangle behind the floating navbar.

Targets `com.instagram.android` — Android 16 / Instagram 452.0.0.48.64.

## Steps on your phone

1. Open [GitHub Actions](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml), choose the successful run **on branch `diagnostic/nav-hierarchy`**, download `IG-Navbar-Diagnostic-debug` from Artifacts and extract `app-debug.apk`.
2. **Disable the current IG Liquid Glass v0.6 module** in LSPosed so that Instagram has its normal native navigation. This lets us inspect the original hierarchy rather than the modified layout.
3. Install **IG Navbar Diagnostic**. Enable it in LSPosed and select **Instagram** as its only scope.
4. Force-stop Instagram and reopen it. Stay on the Home tab for at least 5 seconds. The diagnostic logs its structural layout twice.
5. In LSPosed, open Logs → verbose/module logs, export logs and **share only lines containing `IGNAV`** in this chat. A screenshot of the same page also helps.

The logged data includes only view class names, static Android resource names, bounds, background drawable types, parent names and layout parameters. It deliberately avoids usernames, captions, view texts, contents and screenshots. Nevertheless, inspect any logs before sharing.

Once we see the runtime hierarchy, we can implement a targeted hook in the main production branch without blindly hiding views.

## Troubleshooting

If you don't see any `IGNAV` lines, ensure the diagnostic module is enabled and Instagram was force-stopped/reopened. Verify the scope includes `com.instagram.android`. Try opening the Home tab again and wait 5 seconds.

This module uses the legacy Xposed API and requires a functioning LSPosed installation.
