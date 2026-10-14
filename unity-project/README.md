# SELA 3D — Unity Project

Proyek Unity untuk mode 3D SELA (Unity as a Library).

## Struktur

```
unity-project/
├── Assets/
│   ├── Scenes/           # Jakarta3D.unity (dibuat via SceneBootstrapper)
│   ├── Scripts/          # C# gameplay
│   │   ├── SelaBridge.cs         # Jembatan Kotlin ↔ Unity
│   │   ├── MapController.cs      # Kamera + koordinat + orkestrasi
│   │   ├── JakartaSceneBuilder.cs # Bangun kota prosedural
│   │   ├── BuildingGenerator.cs  # Gedung + jendela malam
│   │   ├── RoadMeshBuilder.cs    # Mesh jalan + marka
│   │   ├── StreetlightPlacer.cs  # Lampu jalan
│   │   ├── NightLightingController.cs # Siklus siang/malam
│   │   └── BusMarkerManager.cs   # Marker bus 3D
│   ├── Editor/
│   │   ├── SceneBootstrapper.cs  # Buat scene via menu/batch
│   │   └── UalExportBuilder.cs   # Export unityLibrary via batch
│   └── Settings/         # URP asset, volume profile (dibuat otomatis)
├── Packages/
│   └── manifest.json     # URP 17.0.3, Input System, Pipeline
├── ProjectSettings/
│   └── ProjectVersion.txt # Unity 6000.0.47f1 (Unity 6 LTS terbaru)
└── .github/workflows/
    └── unity-ual.yml      # CI: build unityLibrary di GitHub Actions
```

## Cara build (di mesin dengan Unity Editor)

### Opsi A — Manual (GUI)

1. Unity Hub → Add project from disk → pilih folder ini
2. Tunggu import selesai
3. Menu **SELA > Build Jakarta3D Scene**
4. **File > Build Settings > Android**:
   - Centang **Export Project** ✅
   - Scripting Backend: IL2CPP, ARM64, minSdk 26
5. Klik **Export** → pilih `~/workspace/sela/apps/android/unity-export/`

### Opsi B — Batch mode (tanpa GUI)

```bash
Unity -batchmode -nographics \
  -projectPath /path/to/unity-project \
  -executeMethod SceneBootstrapper.BuildSceneHeadless \
  -logFile scene-build.log -quit

Unity -batchmode -nographics \
  -projectPath /path/to/unity-project \
  -executeMethod UalExportBuilder.ExportAndroidLibrary \
  -exportPath /path/to/unity-export \
  -logFile ual-export.log -quit
```

### Opsi C — GitHub Actions (cloud, gratis)

Lihat `.github/workflows/unity-ual.yml`. Butuh secrets:
- `UNITY_LICENSE` — isi file `.ulf`
- `UNITY_EMAIL`, `UNITY_PASSWORD`

## Integrasi ke SELA Android

Hasil export (`unity-export/unityLibrary/`) di-drop ke:
```
~/workspace/sela/apps/android/unity-export/
```

`settings.gradle.kts` otomatis include `:unityLibrary` jika folder ada.
Tanpa folder tersebut, app build normal dengan fallback
"Modul 3D belum terpasang".

## Versi

- **Unity:** 6000.0.47f1 (Unity 6 LTS terbaru)
- **Render Pipeline:** URP 17.0.3
- **Scripting:** C# 9, IL2CPP, ARM64
- **minSdk:** 26 (Android 8.0)
