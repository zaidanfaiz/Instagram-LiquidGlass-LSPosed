# README Media Slots

This folder is reserved for **original screenshots and recordings supplied by the repository maintainer**. The public README already has empty, styled placeholders. **There are intentionally no sample screenshots or GIFs in this directory.**

## File names to upload

| Slot in README | Upload your file here | Format / suggested dimensions |
| --- | --- | --- |
| Cover / hero | `assets/readme/cover.png` | PNG, 1600 × 900 (16:9) |
| Animated navigation | `assets/readme/navigation-demo.gif` | GIF, around 900 × 500 |
| Animated Reels blur | `assets/readme/reels-blur.gif` | GIF, around 900 × 500 |
| Home | `assets/readme/screenshots/home.png` | PNG, portrait 9:16 |
| Reels | `assets/readme/screenshots/reels.png` | PNG, portrait 9:16 |
| Direct inbox | `assets/readme/screenshots/dm.png` | PNG, portrait 9:16 |
| Own Profile | `assets/readme/screenshots/profile.png` | PNG, portrait 9:16 |
| Search | `assets/readme/screenshots/search.png` | PNG, portrait 9:16 |

*You can use WebP instead of PNG, but update the extension in both README files. The example dimensions are suggestions, not hard requirements.*

## How to activate the images

1. Upload your pictures/GIFs to the exact paths above via GitHub's **Add file → Upload files**, or commit them normally.
2. Open [README.md](../../README.md) and [README_ID.md](../../README_ID.md). Find the sections labeled `MEDIA SLOT 01/02/03` or `SLOT MEDIA 01/02/03`.
3. Each HTML comment already contains the **full image layout ready to use**. Copy the markup out of the comment (or remove the comment delimiters) and delete the visible text-only placeholder table for that section.
4. Preview the rendered Markdown, confirm your original media appears, and commit the README edits.

### Example: hero

Replace the cover placeholder table with:

```html
<p align="center">
  <img src="assets/readme/cover.png"
       alt="Instagram Liquid Glass preview on POCO F7 Ultra"
       width="100%">
</p>
```

### Example: animated demo

Replace the first animation slot table with:

```html
<table>
  <tr>
    <td align="center"><img src="assets/readme/navigation-demo.gif" width="100%" alt="Animated glass navigation"></td>
    <td align="center"><img src="assets/readme/reels-blur.gif" width="100%" alt="Animated Reels video blur"></td>
  </tr>
</table>
```

The GitHub README header also uses an **animated typing banner** supplied by `readme-typing-svg.demolab.com`. That banner works independently from the optional screenshots/GIFs. If the external banner service becomes unavailable, the project title, description, navigation and all other content remain readable.

### Before publishing screenshots

- Use the real **POCO F7 Ultra** device capture where possible. Do not use images from other phones and label them as device tests.
- Hide or crop personal usernames, profile details, message previews, account IDs and notifications.
- Avoid uploading third-party artwork, proprietary Instagram source APKs or personal chat contents.
- Optimize GIF size to keep the README fast, especially for visitors on mobile networks.
- The repository remains **private** until the maintainer decides otherwise.
