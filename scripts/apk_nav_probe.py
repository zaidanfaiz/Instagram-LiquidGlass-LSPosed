#!/usr/bin/env python3
"""Read-only reconnaissance for an Android APK. Never uploads bundled APK data.

Outputs bounded navigation-related metadata and resource snippets. It does not
assert that a resource name identifies a live View or LSPosed hook target.
"""
import argparse
import collections
import os
import re
import sys
import tempfile
import zipfile
from pathlib import Path

NAV = re.compile(
    r"bottom.{0,7}nav|bottom.{0,7}bar|bottom.{0,7}tab|tab.{0,7}bar|"
    r"navigation.{0,7}bar|tab.{0,7}strip|"
    r"feed.{0,7}tab|main.{0,7}tab|"
    r"nav.{0,7}container|nav.{0,7}background|"
    r"edge.{0,7}edge|system.{0,7}bar|"
    r"navigation_bar|bottom_navigation|bottom_tabs|tab_bar",
    re.I,
)
ATTR = re.compile(
    r"(?:android:)?(?:background|foreground|layout_height|layout_width|"
    r"paddingBottom|layout_marginBottom|fitsSystemWindows|"
    r"clipChildren|clipToPadding)\s*=", re.I
)
MAX_RESOURCE_FILES = 9000
MAX_MATCHES = 400

def apk_root(original: Path, scratch: Path):
    with zipfile.ZipFile(original) as archive:
        entries = archive.namelist()
        if "AndroidManifest.xml" in entries and any(
            name.startswith("classes") and name.endswith(".dex") for name in entries
        ):
            return original, "standalone APK"
        nested = [name for name in entries if name.endswith(".apk")]
        if not nested:
            raise RuntimeError("File is not an Android APK or a bundle containing APKs.")
        preferred = next((n for n in nested if Path(n).name == "base.apk"),
                         next((n for n in nested if "base" in n.lower()), nested[0]))
        target = scratch / "extracted-base.apk"
        with archive.open(preferred) as source, open(target, "wb") as dest:
            import shutil
            shutil.copyfileobj(source, dest, 4 * 1024 * 1024)
        return target, "nested APK " + preferred

def scan_zip(apk: Path, output: Path, source_label: str):
    lines = [
        "# Instagram APK static navigation reconnaissance",
        "",
        f"Container: {source_label}",
        f"Inner APK size: {apk.stat().st_size:,} bytes",
        "These are *candidate identifiers*, not confirmed navigation hooks.",
        "",
    ]
    with zipfile.ZipFile(apk) as z:
        infos = z.infolist()
        bad = z.testzip() if len(infos) < 100 else None
        lines += [
            f"ZIP entries: {len(infos)}",
            f"First ZIP integrity error: {bad or 'none found (or not tested)'}",
        ]
        dex = [i for i in infos if re.fullmatch(r"classes(?:\d+)?\.dex", i.filename)]
        lines += [
            f"DEX files: {len(dex)}",
            "DEX uncompressed sizes: " + ", ".join(f"{i.filename}={i.file_size:,}" for i in dex[:30]),
            "",
            "## Navigation-named archive entries",
        ]
        entries = [i.filename for i in infos if NAV.search(i.filename)]
        lines += entries[:MAX_MATCHES] or ["No navigation-named entries found."]
        lines += ["", "## DEX string fragments (first 400, unique)"]
        # Scan raw DEX bytes in small chunks instead of unpacking entire dex trees.
        needle = re.compile(rb"(?i)(?:bottom.{0,6}nav|bottom.{0,6}bar|bottom.{0,6}tab|"
                            rb"tab.{0,6}bar|navigation.{0,6}bar|"
                            rb"nav.{0,6}container|edge.to.edge)")
        found = set()
        for info in dex:
            if len(found) >= MAX_MATCHES:
                break
            with z.open(info) as fh:
                overlap = b""
                while len(found) < MAX_MATCHES:
                    block = fh.read(1024 * 1024)
                    if not block:
                        break
                    data = overlap + block
                    for match in needle.finditer(data):
                        segment = data[max(0, match.start()-100):min(len(data), match.end()+120)]
                        segment = "".join(
                            chr(c) if 32 <= c < 127 else " " for c in segment
                        )
                        segment = re.sub(r"\s+", " ", segment).strip()[:200]
                        if len(segment) > 8:
                            found.add(f"[{info.filename}] {segment}")
                    overlap = data[-256:]
        lines += sorted(found)[:MAX_MATCHES] or ["No readable DEX candidate strings found."]
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")

def scan_decoded(decoded: Path, out: Path):
    lines = ["# Decompiled Android resources: navigation candidates", ""]
    manifest = decoded / "AndroidManifest.xml"
    if manifest.exists():
        text = manifest.read_text(encoding="utf-8", errors="replace")
        lines += ["## Manifest hints"]
        lines += [x.strip()[:350] for x in text.splitlines()
                  if NAV.search(x)][:80] or ["No matching manifest lines."]
    candidates = []
    resource_root = decoded / "res"
    if resource_root.exists():
        paths = list(resource_root.rglob("*.xml"))[:MAX_RESOURCE_FILES]
        for file in paths:
            rel = str(file.relative_to(decoded))
            by_name = bool(NAV.search(rel))
            if not by_name and not (
                file.parent.name.startswith(("layout", "values", "drawable"))
            ):
                continue
            data = file.read_text(encoding="utf-8", errors="replace")
            interesting = [line.strip()[:350] for line in data.splitlines()
                           if NAV.search(line)]
            if not interesting and not by_name:
                continue
            if by_name:
                attrs = [line.strip()[:350] for line in data.splitlines()
                         if ATTR.search(line)][:10]
                interesting = interesting[:12] + attrs
            candidates.append((rel, interesting))
    lines += ["", f"Matching resource files: {len(candidates)}", ""]
    for rel, matching_lines in candidates[:200]:
        lines.append(f"### {rel}")
        lines += [f"- {x}" for x in matching_lines[:18]]
        lines.append("")
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--apk", required=True)
    p.add_argument("--output-dir", required=True)
    p.add_argument("--decoded")
    a = p.parse_args()
    out = Path(a.output_dir)
    out.mkdir(parents=True, exist_ok=True)
    if a.decoded:
        scan_decoded(Path(a.decoded), out / "decoded-candidates.md")
        return
    with tempfile.TemporaryDirectory() as temp:
        apk, label = apk_root(Path(a.apk), Path(temp))
        scan_zip(apk, out / "apk-dex-candidates.md", label)
        # Keep the extracted base APK only for the next job step, never artifact it.
        if apk != Path(a.apk):
            import shutil
            shutil.copyfile(apk, out.parent / "base-for-apktool.apk")
        else:
            (out.parent / "base-for-apktool.apk").symlink_to(Path(a.apk).resolve())

if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"APK reconnaissance failed: {exc}", file=sys.stderr)
        raise
