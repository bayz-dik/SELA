# SELA V5 — native Android redesign pack

**Start with:** `android/WORK_WITH_V5.md` and `android/DESIGN.md`.

- `android/`: complete SELA Android Gradle project (Kotlin/Compose/MapLibre/MQTT/GTFS) modified for V5.
- `.github/workflows/android-debug-v5.yml`: optional debug build workflow; not yet executed.
- `design-preview/SELA_V5_INTERACTIVE_PREVIEW.html`: fully local, self-contained HTML concept; no network necessary.
- `design-preview/SELA_V5_GALLERY.png`: five-screen concept sheet.
- `design-preview/SELA_V5_MASCOT_STATES.png`: all ten original mascot states.
- `design-preview/mascots/*.svg`: static, editable vector references for mascot concepts.
- `design-preview/references`: seven original reference photos supplied by the user.

**Important:** Android app was not compiled or installed in this environment. The Gradle wrapper failed to download Gradle 8.9 because DNS/network access was unavailable. The HTML mock is illustrative rather than an Android screenshot.

**No legacy Unity:** the old `unity-project/` from source is intentionally not included because this handoff is native Android only.
