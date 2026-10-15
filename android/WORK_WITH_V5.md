# Cara membuka SELA V5

1. Ekstrak ZIP.
2. Buka **folder `android/`** di Android Studio versi terbaru yang mendukung AGP dan Compose BOM project.
3. Pastikan **JDK 21** dan **Android SDK 35** terpasang.
4. Jalankan Gradle sync: `./gradlew :app:assembleDebug`.
5. Lakukan regression test fitur map, GTFS, MQTT, permission, SOS, AR, dan lainnya.
6. Mulai review visual di `design-preview/SELA_V5_INTERACTIVE_PREVIEW.html` dan PNG gallery.

Tidak perlu membuka project Unity atau menjalankan backend. Sumber kode V5 sengaja mempertahankan seluruh modul bisnis Android native.

**Peringatan:** Build di sesi pembuat file tidak dapat diselesaikan karena distribusi Gradle tidak tersedia; lihat `QA_V5.md`.

Alternatif: upload isi paket ke repo GitHub milikmu, lalu jalankan workflow `.github/workflows/android-debug-v5.yml` untuk build APK debug tanpa secret keystore release. Workflow ini belum dieksekusi pada sesi ini.
