# SELA V5 — QA results

Tanggal: 2026-10-15

## Environment

- JDK 21 tersedia.
- `android/gradlew` tersedia, wrapper Gradle 8.9.
- Android SDK tidak tersedia dalam runtime ini.
- Gradle distribution tidak cached.

## Build attempt

Perintah: `./gradlew --offline :app:assembleDebug`

Hasil: **BLOCKED** sebelum konfigurasi proyek karena wrapper gagal mengambil `https://services.gradle.org/distributions/gradle-8.9-bin.zip` (`java.net.UnknownHostException`). Ini bukan bukti sukses maupun gagal compile source Kotlin.

## Verified

- Semua file baru ada pada lokasi yang sesuai modul Gradle.
- Integrasi state `MapViewModel` di Home/Map tetap mengonsumsi state repository existing.
- Source UI Android tidak menambahkan dependency Unity, Flutter, React Native, Rive, atau backend.
- Preview HTML berhasil dimuat di Chromium tanpa `pageerror` pada viewport 375×812 dan 1440×900.
- Lima tab tampil dalam galeri. 14 pemeriksaan interaksi preview berhasil (lihat `design-preview/qa_preview.json`).
- Tombol tab Rute menampilkan form dan tombol Cari perjalanan memunculkan hasil contoh.
- 10 state maskot digambar di SVG preview dan exported assets.

## Requires Android QA

1. Gradle sync dan `:app:assembleDebug` (SDK 35, JDK 21).
2. Manual test pada perangkat/emulator Android 26–35, portrait/landscape.
3. Test ukuran 320dp/360dp/412dp, font scale 1.35, screen reader.
4. Tes MQTT status offline/live dan jumlah bus nyata; tidak mengasumsikan akses broker selalu terbuka.
5. GTFS planner; map interactions pan/zoom/search/3D and selected route sheet.
6. Maskot saat tab switching dan saat estimasi ETA lokal <=3 menit; cek jank/performa.
7. Test navigation back stack, shortcuts, SOS/mesh/AR after start destination changed to home.

## Known limitations

- State animasi `Welcome`, `Success`, dan `BusApproaching` memakai gerak loop saat aktif; bila akan di-trigger pada aksi satu kali, tahap berikutnya sebaiknya mengganti dengan animasi one-shot per event.
- Preview HTML/SVG character adalah render konseptual independen dari Compose Canvas, bukan exact parity.
- Tidak ada APK, screenshot emulator, atau klaim QA perangkat.
- Map overlay/bottom dock perlu cek berbagai safe areas dalam Android nyata.
