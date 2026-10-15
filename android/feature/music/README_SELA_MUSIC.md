# SELA Music — Dokumentasi Teknis

Implementasi full fitur musik di SELA V5: katalog YouTube Music, streaming audio,
player background, dan pustaka lokal. Dikerjakan end-to-end dalam satu sesi.

## Arsitektur

```
:feature:music
├── moe.rukamori.archivetune.innertube/   # Port dari rukamori/core (GPL-3.0), 101 files
│   ├── YouTube.kt                        # Singleton API: search, home, artist, album, playlist, player
│   ├── InnerTube.kt                      # HTTP client (Ktor + OkHttp)
│   ├── pages/NewPipe.kt                  # NewPipeUtils.getStreamUrl (decipher signature)
│   └── models/ + pages/                  # Model respons YouTube Music
├── id.sela.feature.music/
│   ├── data/
│   │   ├── MusicModels.kt                # SelaSong, SelaArtist, SelaAlbum, SelaPlaylist
│   │   ├── MusicMappers.kt               # YTItem -> model SELA
│   │   ├── MusicRepository.kt            # Akses katalog (IO dispatcher, Result)
│   │   └── MusicDatabase.kt              # Room: favorit, playlist, history, search history
│   ├── playback/
│   │   ├── StreamResolver.kt             # videoId -> URL audio (cache + re-resolve)
│   │   ├── MusicPlaybackService.kt       # MediaLibraryService + ExoPlayer
│   │   └── SelaMusicController.kt        # API untuk UI (StateFlow)
│   ├── di/
│   │   ├── MusicModule.kt                # Hilt SingletonComponent
│   │   └── MusicControllerEntryPoint.kt   # Akses controller dari :app
│   └── ui/
│       ├── MusicTabScreen.kt             # Tab Musik (nav internal)
│       ├── MusicHomeScreen.kt            # Rekomendasi
│       ├── MusicSearchScreen.kt          # Cari + saran + riwayat
│       ├── MusicLibraryScreen.kt         # Favorit, playlist, terakhir diputar
│       ├── MusicDetailScreens.kt         # Detail artis/album/playlist
│       ├── NowPlayingScreen.kt           # Full-screen player + antrean
│       ├── MiniPlayer.kt                 # Mini-player mengambang
│       └── MusicComponents.kt            # Komponen shared
```

## Alur playback

1. User ketuk lagu -> `SelaMusicController.playQueue()`
2. `StreamResolver.resolve(videoId)`:
   - Cek cache (URL kedaluwarsa < 60 detik -> resolve ulang)
   - `YouTube.player(videoId, ANDROID_MUSIC)` -> fallback `WEB_REMIX`
   - Pilih adaptive format audio bitrate tertinggi
   - `NewPipeUtils.getStreamUrl()` -> decipher signature jika perlu
3. ExoPlayer memutar URL via OkHttpDataSource
4. Error playback -> invalidate cache -> resolve ulang -> retry sekali otomatis
5. MediaSession -> notifikasi + lock screen + Bluetooth controls otomatis

## Integrasi SELA

- **Navigasi**: tab "Musik" ditambahkan ke `SelaFloatingDock` (6 item). Route `music` -> `MusicTabScreen`.
- **Mini-player global**: di `SelaScaffold.bottomBar`, di atas dock, muncul saat ada lagu
  (disembunyikan di tab Musik karena tab Musik punya sub-nav sendiri).
- **Now Playing**: bottom sheet penuh di dalam `MusicTabScreen`.
- **Background**: `MusicPlaybackService` foreground service (`mediaPlayback`).
  Tetap hidup saat peta MapLibre dibuka / tab berganti / layar dikunci.
- **MQTT/GTFS**: tidak disentuh. Playback dan data transit jalan independen.
- **Tema**: `SelaPastel` (Pagi Cerah) di semua UI musik.

## Dependensi baru

| Library | Versi | Kegunaan |
|---|---|---|
| media3-exoplayer/session/datasource-okhttp | 1.7.1 | Playback + MediaSession |
| ktor-client-core/okhttp/content-negotiation | 3.1.3 | InnerTube HTTP |
| ktor-serialization-kotlinx-json | 3.1.3 | JSON |
| ktor-client-encoding | 3.1.3 | Brotli/gzip |
| okhttp / okhttp-dnsoverhttps | 4.12.0 | HTTP engine |
| NewPipeExtractor (JitPack) | v0.24.5 | Decipher signature |
| re2j | 1.8 | Regex (innertube) |
| brotli dec | 0.1.2 | Dekompresi (innertube) |
| kotlinx-serialization-json | 1.8.1 | JSON models |
| coil3 coil-compose | 3.1.0 | Artwork |
| room-ktx | 2.6.1 | Database reaktif |

Repo JitPack ditambahkan di `settings.gradle.kts` (khusus NewPipeExtractor).

## Batasan arsitektur (perintah Bayu 2026-10-16 — FINAL, jangan diubah)

- **API langsung**: musik memakai InnerTube (YouTube Music) langsung dari aplikasi.
  TIDAK ada server perantara/proxy/backend dalam bentuk apapun.
- **Tanpa iklan**: tidak ada iklan dalam bentuk apapun di fitur musik.
- **Gratis semua**: tidak ada layanan berbayar, tidak ada API key berbayar.
- **Standalone**: arsitektur direct API + Room lokal + Media3 adalah final.
- Usulan yang melanggar batasan di atas DITOLAK.

## Lisensi & evaluasi

- **Kode InnerTube**: port dari `github.com/rukamori/core`, lisensi **GPL-3.0**.
  Header atribusi dipertahankan di semua 101 file. Tidak ada file yang diubah
  selain penyalinan (package tetap `moe.rukamori.archivetune.innertube`).
- **Implikasi**: jika kode ini di-commit ke repo publik, keseluruhan karya turunan
  tunduk pada GPL-3.0 (wajib menyediakan source). Untuk penggunaan pribadi
  tanpa distribusi, tidak ada kewajiban tambahan.
- **Rekomendasi**: (a) repo SELA dijadikan privat, atau (b) tambahkan file LICENSE
  GPL-3.0 di repo, atau (c) pisahkan modul musik ke repo privat.
- **Ketentuan layanan**: InnerTube adalah API tidak resmi YouTube Music.
  Akses dapat berubah/diblokir sewaktu-waktu tanpa pemberitahuan. Tidak ada
  jaminan ketersediaan jangka panjang. Gunakan untuk pribadi, bukan komersial.

## Status pengujian (jujur)

| Pengujian | Status |
|---|---|
| Kompilasi Gradle | **BELUM** — VM tidak cukup RAM untuk Gradle daemon |
| Review kode manual | Selesai — struktur, import, API innertube diverifikasi |
| Playback lagu sungguhan | **BELUM** — butuh device/emulator |
| Search/metadata | **BELUM** — butuh runtime + internet |
| Background/lock screen | **BELUM** — butuh device |
| Bluetooth/headset | **BELUM** — butuh device |
| MapLibre + MQTT saat musik | **BELUM** — butuh device |
| Offline/error handling | Kode ada, **belum diuji runtime** |
| Memory/perf | **BELUM** — butuh profiling di device |

**Jangan klaim fitur bekerja sebelum diuji di device.** Build via GitHub Actions
direkomendasikan sebelum instal.

## Perbaikan Codex (2026-10-16)

- **Compile error**: `getOrDefault(null)` → `getOrNull()`; tambah import
  `SharingStarted`/`stateIn` yang hilang di MusicViewModel.
- **(b) Queue URL**: `ensureResolved()` di `onMediaItemTransition` — item pending
  di-resolve otomatis saat transisi; `preResolveNext()` pre-resolve lagu berikut.
- **(c) Progress ticker**: coroutine 1-detik di SelaMusicController, jalan saat
  `isPlaying`, berhenti saat pause.
- **(d) Autoplay**: `onPlaybackStateChanged(STATE_ENDED)` → `triggerAutoplay()`
  memakai `musicRepository.relatedSongs()` (5 lagu, di luar repeat mode).
- **(e) Audio cache**: `SimpleCache` 200MB LRU via `CacheDataSource` di
  MusicPlaybackService (cache sementara, bukan download offline permanen).
- **(f) Dark mode**: TIDAK diimplementasikan — V5 "Pagi Cerah" adalah desain
  light-only sesuai brief. Dark mode butuh keputusan desain Bayu.

## Masalah yang belum terselesaikan

1. **Kompilasi belum terverifikasi** — butuh `Build SELA Android APK` di GitHub Actions.
   (JANGAN trigger — Bayu yang perintahkan.)
2. **poToken/BotGuard**: untuk beberapa konten, YouTube meminta poToken yang
   dihasilkan via BotGuard (butuh WebView + challenge). Implementasi dasar
   (`resolvePlayerPoToken`) ada di port, tapi tanpa uji runtime tidak jelas
   seberapa sering dibutuhkan.
3. **NewPipeExtractor via JitPack**: build pertama akan download dari JitPack;
   jika JitPack lambat/down, build bisa gagal. Alternatif: mirror ke Maven Central
   atau vendor JAR lokal.
4. **Coil3 3.1.0**: versi ini diasumsikan ada; jika tidak, turunkan ke 3.0.x.
5. **Mini-player di tab non-musik**: saat ini disembunyikan di tab Musik agar tidak
   dobel dengan sub-nav. Perilaku ini bisa disesuaikan.
6. **Detail artis**: hanya menampilkan info dasar; daftar lagu artis memakai
   fallback (belum ada endpoint khusus top-songs di port ini).
7. **Cache audio**: belum ada cache file audio offline; hanya cache URL di memori.
   Download offline belum diimplementasikan (butuh izin storage + worker).
8. **Lirik**: belum diimplementasikan.
9. **Equalizer**: belum diimplementasikan.
10. **Android Auto**: belum diimplementasikan.
