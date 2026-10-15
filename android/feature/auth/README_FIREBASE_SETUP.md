# Setup Firebase untuk Login Google SELA

**Status:** Belum dikonfigurasi. Kode login sudah siap, tapi butuh langkah di bawah ini agar login Google berfungsi.

**PENTING:** Tanpa langkah ini, tombol login akan menampilkan pesan "Konfigurasi Firebase belum lengkap" — BUKAN login palsu.

---

## Yang perlu Bayu lakukan

### 1. Buat project Firebase
1. Buka https://console.firebase.google.com
2. Klik "Add project" / "Tambah project"
3. Nama project bebas (misal: "SELA")
4. Ikuti wizard sampai selesai

### 2. Tambahkan aplikasi Android (DEBUG)
1. Di Firebase Console → Project Overview → klik ikon Android
2. **Package name:** `id.sela.debug`
3. **App nickname:** SELA Debug (bebas)
4. **SHA-1 debug:** Jalankan perintah ini di HP/PC yang punya keystore debug:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   Salin nilai **SHA1**. (SHA-256 juga disalin jika ada kolomnya)
5. Klik "Register app"

### 3. Tambahkan aplikasi Android (RELEASE)
1. Klik "Add app" → Android lagi
2. **Package name:** `id.sela`
3. **SHA-1/SHA-256 release:** Dari keystore yang dipakai untuk sign APK release.
   Jika belum punya, buat dulu lalu daftarkan SHA-nya di sini.
4. Klik "Register app"

### 4. Download google-services.json
1. Di Project Settings → Your apps → download **google-services.json**
2. Letakkan di: `android/app/google-services.json`
   (JANGAN commit ke repo publik — file ini berisi konfigurasi project Firebase)

### 5. Aktifkan Authentication
1. Di Firebase Console → Build → Authentication → Sign-in method
2. Aktifkan **Google** → Save

### 6. Ambil Web Client ID
1. Di Firebase Console → Project Settings → General
2. Cari bagian "Web API Key" — BUKAN ini.
3. Yang dibutuhkan: **Web OAuth Client ID**
   - Buka: https://console.cloud.google.com/apis/credentials
   - Pilih project Firebase yang sama
   - Cari "OAuth 2.0 Client IDs" → yang tipe **Web application** (auto-created by Google Service)
   - Salin **Client ID** (format: `xxxx.apps.googleusercontent.com`)

### 7. Isi Web Client ID ke project
1. Buka file `android/local.properties` (buat jika belum ada)
2. Tambahkan baris:
   ```
   google.webClientId=xxxx.apps.googleusercontent.com
   ```
   (Ganti xxxx dengan Client ID asli dari langkah 6)

### 8. Tambahkan google-services plugin
Di `android/app/build.gradle.kts`, tambahkan di bagian plugins:
```kotlin
plugins {
    ...
    id("com.google.gms.google-services")
}
```
Dan di `android/settings.gradle.kts` bagian pluginManagement, pastikan ada `google()` di repositories (sudah ada).

---

## Verifikasi

Setelah semua langkah di atas:
1. Build APK debug via GitHub Actions
2. Install di HP
3. Buka app → layar welcome → "Mulai dengan Google"
4. Pilih akun Google → harus masuk ke Home SELA
5. Tutup app, buka lagi → langsung ke Home (session persisten)
6. Settings → Akun → Keluar → kembali ke layar login

## Troubleshooting

| Gejala | Penyebab | Solusi |
|--------|----------|--------|
| "Konfigurasi Firebase belum lengkap" | `google.webClientId` kosong di local.properties | Isi langkah 7 |
| "Developer error" / code 10 | SHA-1 belum didaftarkan | Daftarkan SHA-1 di langkah 2/3 |
| "Network error" | Tidak ada internet | Cek koneksi |
| Login dibatalkan | User menutup dialog Google | Normal, coba lagi |

## Catatan keamanan

- `google-services.json` JANGAN di-commit ke repo publik
- `local.properties` sudah di `.gitignore` (jangan diubah)
- Web Client ID BUKAN secret (aman di BuildConfig), tapi tetap jangan disebar
- TIDAK ada client secret yang disimpan di app
