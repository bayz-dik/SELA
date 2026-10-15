# SELA V5 — Changelog

## Versus original SELA

- Ditambah `SelaHomeScreen.kt` sebagai tab Beranda baru dengan isi berdasarkan MapViewModel: koneksi, bus live, rute, halte, favorit, dan shortcut.
- Ditambah `SelaV2Components.kt` berisi hero, aurora background, metric tiles, route rows, live status, dan floating 5-tab navigation.
- `MainActivity.kt` memakai lima tab: Beranda, Peta, Rute, Tanya, Lainnya. Semua destination lama tetap ada (history, offline map, SOS, mesh, AR).
- `MascotView.kt`: maskot 2.5D original, 10 state, blink, eye glance, wave, bounce, sparkles, breathing, static reduced-motion branch. API legacy tiga state tetap didukung.
- `SelaTransitCompanion.kt`: reusable companion card dan stat chip.
- `SelaLiveTransitCards.kt`: vehicle live card tanpa ETA buatan.
- `MapScreen.kt`: mini status companion hanya ketika dibutuhkan, bus rows baru, peta tetap dominan. Global nav dan map tool dock memiliki fungsi berbeda.
- `DetailSheets.kt`: maskot waiting saat belum ada arrival, maskot mendekat hanya ketika estimasi lokal ETA ≤3 menit, dengan disclaimer.
- `TripPlannerScreen.kt`: hero kontekstual untuk input, rute ditemukan, hasil kosong.
- `PastelStates.kt`: loading dan empty state 10-state compatible.
- `KonduktorScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`, `OfflineMapScreen.kt`, `SosScreen.kt`, `MeshScreen.kt`: konsistensi aura/ekspresi.

## Tidak diubah

- Implementasi MQTT TransJakarta, broker, schema vehicle.
- GTFS parsing, routing, repository.
- MapLibre renderer dan logika kamera 2D/3D.
- Hilt DI, storage, location permissions, SOS, AR dan mesh business logic.

## Batasan

Belum lulus Gradle compilation/Android emulator. Preview HTML hanya representasi desain dengan data contoh. Legacy Unity project tidak disertakan dalam ZIP native Android V5, namun tetap ada pada source asli pengguna.
