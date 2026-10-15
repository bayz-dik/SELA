# SELA — Design System V5

**Product:** SELA, aplikasi transportasi publik Jakarta, **native Android**  
**Stack:** Kotlin · Jetpack Compose · MapLibre Native · MQTT TransJakarta langsung · GTFS lokal  
**Status dokumen:** spesifikasi visual dan implementasi V5, 15 Oktober 2026  
**Tujuan:** menjaga informasi perjalanan tetap jelas sambil memberikan identitas original, playful, soft 2.5D, dan premium.

> **Perbedaan dokumen vs implementasi:** Dokumen ini memuat arahan lengkap produk. Tidak semua detail pada dokumen telah diimplementasikan di source V5. Cek `QA_V5.md` untuk batasan yang belum terverifikasi.

## 1. Product point of view

SELA bukan dashboard BI, chatbot serbaguna, atau replika aplikasi wellness. SELA adalah **pendamping transit kota** yang menerjemahkan jaringan bus Jakarta menjadi keputusan kecil yang mudah: *bus mana, ke halte mana, rute apa, dan bagaimana tetap bergerak saat offline.*

Tiga janji desain:

1. **Beri konteks, bukan dekorasi.** Apa manfaat informasi ini terhadap perjalanan?
2. **Jujur soal data.** `LIVE` adalah status broker; bukan bukti bus dekat. ETA adalah estimasi lokal berbasis jarak/kecepatan, bukan jadwal resmi. Jika tidak ada data, tampilkan kosong atau menunggu, bukan angka buatan.
3. **Hidup tanpa berisik.** Ilustrasi dan animasi menghangatkan pengalaman, tetapi tidak menghalangi tombol peta atau readability.

### Anti-slop design read

Repo publik [`anti-slop`](https://github.com/miqdadbadjuber/anti-slop) dipakai sebagai **filter kualitas**, bukan pemasok gaya. Arah estetika justru berasal dari tujuh gambar referensi yang diberikan pengguna dan keputusan produk SELA sendiri. Prinsip operasional: purpose test untuk setiap gradien/kartu/efek, hindari copy generik, buat hirarki jelas, jaga kontrol sentuh, dan jangan menyamarkan mock sebagai data nyata. Tidak ada paket antislop yang ditambahkan sebagai dependency Android.

## 2. Referensi visual dan penerapannya

| Referensi | Ambil | Jangan tiru secara literal |
|---|---|---|
| Vitamin app biru | ruang kosong, gradien biru seperti kabut, tokoh lucu dengan mata ekspresif | susunan produk vitamin, copy kesehatan |
| Recenter hijau | tubuh organik jelly, highlight, maskot 2.5D, suasana hening | logo, karakter, bentuk identik |
| Recenter card-stack | kartu masuk berlapis, sinyal warna mint-krem-lavender, typografi editorial | satu layar onboarding yang persis sama |
| Nexus EdTech | hero berilustrasi, kartu bento, ritme visual positif | ilustrasi objek plush yang identik |
| Paypet | navigation dock floating + kartu grid asimetris | komposisi pixel-perfect dari sumber |
| Duolingo journey | gagasan navigasi progres, titik rute | peta gunung; tidak relevan dengan transit real |
| Mobile wellness | input sederhana, microcopy jelas, pilihan dalam kartu | pertanyaan wellness atau CTA copy aslinya |

## 3. Identitas SELA

- **Suasana:** empatik, energik secukupnya, taktis, optimistis, aman.
- **Ekspresi visual:** pastel 2.5D, kartu dengan bayangan halus, area peta praktis, teks kontras tinggi.
- **Metafora:** jalur perjalanan, node halte, perjalanan dari titik ke titik, bukan hanya bus.
- **Maskot:** Sela, makhluk organik asli dengan tubuh biru/mint/lavender dan jejak rute kecil di bagian bawah tubuh.
- **Pilihan bahasa:** Bahasa Indonesia singkat. Contoh: “Kita susun perjalananmu dulu”, “Rute lokal tetap tersedia”, “Bus diperkirakan dekat”.

## 4. Foundation tokens

| Token | Nilai | Tujuan |
|---|---|---|
| Background | `#FDFBF7` | Off-white agar kartu terasa ringan |
| Ink | `#2A2A3A` | Headline dan kontrol |
| Ink soft | `#55556A` | Detail, label sekunder |
| Blue / soft / deep | `#A8C8F0` / `#DCE9FB` / `#4E7CC4` | Info, pencarian, rute |
| Lavender / soft / deep | `#C8B8F0` / `#E7DFFB` / `#8266CC` | Identitas AI dan navigasi |
| Mint / soft / deep | `#A8E8C8` / `#D9F5E5` / `#3D9E6B` | Status live, keberhasilan |
| Peach | `#FFD9B3` | Peringatan ringan dan bantuan |
| Pink | `#F4B8D0` | Pipi maskot dan highlight |
| Danger | `#E8696B` | Kegagalan, tanpa alarm palsu |

**Tipografi:** hirarki sistem Compose Material3 + penegasan tipografis pada hero: ukuran judul besar dengan berat ExtraBold/Black, body 14–16sp, label 11–13sp. Jangan menggunakan lebih dari dua karakter display yang bersaing pada satu layar.

**Corner scale:** pill 100dp; hero 34–36dp; kartu 24–29dp; tile angka 18–24dp; bidang teks 18dp. **Spacing:** base grid 4dp, layout 8/12/16/20/24dp, margin layar 16–20dp. **Touch target:** kontrol utama setidaknya ~48dp.

**Surface layering:** warm white body → pastel ambient haze → satu hero bertint → putih untuk informasi utilitas → map menjaga saturasi rendah. Jangan meletakkan glassmorphism di setiap elemen.

## 5. SELA original mascot — 2.5D

**Implementation of record:** `core/designsystem/.../MascotView.kt` (Compose Canvas, tidak memerlukan Rive/Unity/WebView/asset jaringan). Sumber SVG 10 ekspresi pada `design-preview/mascots` hanya untuk referensi dan eksplorasi editorial; source animasi Android adalah Kotlin. `SelaMascotExpression` mempertahankan `Happy`, `Thinking`, `Sleeping` sebagai alias agar komponen lama tidak rusak.

| State | Pemicu | Gerakan | Prioritas |
|---|---|---|---|
| Idle | beranda, menunggu input | float, breathing, blink | rendah |
| Welcome | onboarding, sapaan, asisten | melambai, bounce kecil | sedang |
| Searching | planner belum selesai, loading data | mata melirik, alis berpikir | sedang |
| RouteFound | ada hasil perencanaan GTFS | happy bounce | sedang |
| WaitingBus | halte belum mendapat update bus | mata melirik, aksen jam | rendah |
| BusApproaching | **ETA lokal di halte terpilih sekitar ≤3 menit**, bukan sekadar broker LIVE | antusias, sparkle kecil | sedang |
| Offline | status broker offline | gerakan tenang, mengantuk | rendah |
| Error | koneksi gagal / penyedia data error | muka kecewa lembut | rendah |
| Success | aksi / hasil berhasil | bounce & sparkle | sedang |
| Empty | hasil pencarian kosong | menunggu netral | rendah |

**Motion:** idle sekitar 2.3 detik, breathing 2.1 detik, blink 2.6 detik dengan jeda alami, wave 0.9 detik, glance 1.8 detik, bounce 1.2 detik. Nilai ini bersifat rekomendasi; implementasi di source menggunakan `rememberInfiniteTransition`. Maskot menjadi statis bila pengguna mematikan `ANIMATOR_DURATION_SCALE` atau ketika `animate=false`.

**Skala visual:** 110dp di home hero, 52–76dp pada informasi perjalanan, 36dp di gelembung chat, sekitar 40dp di status peta. Di peta aktif, animasi sekecil mungkin dan tidak boleh mengganggu kontrol MapLibre.

## 6. Komponen UI

| Komponen / keluarga | Implementasi | Prinsip |
|---|---|---|
| `SelaMascot` | Compose Canvas | 10 state; asli SELA |
| `SelaHeroPanel` | `SelaV2Components.kt` | headline besar, CTA kuat, maskot soft |
| `SelaCompanionHeroCard` | `SelaTransitCompanion.kt` | status konteks perjalanan, bukan hiasan |
| `SelaHeroStatChip` | `SelaTransitCompanion.kt` | statistik tiga kolom responsif |
| `SelaMetricTile` | `SelaV2Components.kt` | angka real dari state, tile pastel |
| `SelaTransitLine` | `SelaV2Components.kt` | route/stop list clickable |
| `SelaLiveBusCard` | `SelaLiveTransitCards.kt` | data bus tidak stale, tanpa ETA palsu |
| `SelaFloatingDock` | `SelaV2Components.kt` | tab utama lima item |
| `PastelCard`, `PastelBottomSheet` | designsystem bawaan | reusable, bentuk organik |
| `PastelSearchBar`, `PastelRouteBadge` | designsystem bawaan | utilitarian, state jelas |
| `PastelLoadingState`, `PastelEmptyState`, `PastelErrorState` | `PastelStates.kt` | respons ramah + aksesibel |
| `PastelPulseDot`, `ConnectionPill` | designsystem/feature map | status koneksi hanya dari state |
| `ArrivalTimelineRow`, `StopDetailSheet` | feature/map/DetailSheets | ETA kasar berlabel jelas |
| `MapDock`, `ServiceLegend` | feature/map/MapScreen | akses ke daftar rute, halte, favorit, bus |
| `BentoGrid`, `PastelStatCell` | designsystem bawaan | konteks statistik, tidak dekorasi murni |

## 7. Rancangan tiap halaman

### Beranda (`SelaHomeScreen.kt`)

- Wordmark “SELA”, subheading “Teman transit Jakarta”, serta pill koneksi.
- Hero biru-lavender dengan judul editorial, maskot 2.5D, CTA buka peta.
- Bento statistik dua kolom: **bus live** dari `vehicles.filter(!isStale)`; **jumlah rute** dari data GTFS yang telah dimuat.
- Quick actions Cari Rute dan Tanya Sela.
- Daftar rute dan halte dari repository nyata serta blok favorit bila tersedia.
- Riwayat di bagian bawah tanpa membuat angka pengalaman fiktif.

### Peta (`MapScreen.kt`)

- MapLibre tetap layer dominan.
- Top search, status koneksi, filter layanan, dan controls peta lama dipertahankan.
- Maskot besar **tidak** menutupi peta; hanya status strip mini saat offline, error, connecting, atau menunggu posisi armada.
- Bottom toolbar terkait peta tetap punya aksi rute, halte, favorit, bus, lokasi.
- Saat bus dipilih: `SelaLiveBusCard` memakai data kendaraan aktual; tidak mengarang ETA/kecepatan.

### Pencarian rute (`TripPlannerScreen.kt`)

- Companion card berubah menurut state input, ditemukan, atau kosong.
- Asal/tujuan tetap memakai `StopPicker` dan saran dari GTFS lokal.
- Kartu hasil rute dan legs tetap menggunakan `TripPlanner` bawaan; tidak mengklaim durasi atau keberangkatan real-time.

### Tanya Sela (`KonduktorScreen.kt`)

- Sapaan dari karakter 2.5D, composer chat dan metode online/offline tetap bekerja sesuai implementasi existing.
- Searching state pada balon berpikir.
- Tidak menambahkan klaim AI selalu aktif atau akses akun otomatis.

### Lainnya / Pengaturan (`SettingsScreen.kt`)

- Tampilan konsisten dengan aurora backdrop dan mascot idle.
- Fitur lama (GTFS, offline, accessibility, AR, SOS, riwayat) tetap tersedia.
- Opsi advanced tidak dihilangkan demi estetika.

### Pendukung

- Riwayat: kartu statistik, lencana, nilai jejak karbon sebagaimana kode existing, maskot sukses, latar selaras.
- Offline map: status unduhan nyata, maskot welcome/empty, bukan peta tersedia palsu.
- SOS/Mesh: tone lebih tenang, utilitas aman didahulukan daripada animasi.
- AR: fungsionalitas lama dipertahankan, belum dirombak kamera secara menyeluruh karena butuh pengujian perangkat.

## 8. Data contracts

**MapLibre:** jangan ubah camera state, 3D buildings, marker selection, map style, bearing, atau follow mode saat hanya mengubah visual. **MQTT:** status `LIVE/CONNECTING/RECONNECTING/OFFLINE/ERROR` berasal dari `MapUiState.connectionStatus`; bukan dari mock. **GTFS:** daftar rute, halte, dan planner dari repository lokal. **ETA:** hanya melalui `EtaPredictor.estimate(stop, vehicles)` dan harus disebut **estimasi lokal**. **Favorit/history:** dari state asli.

Pernyataan microcopy yang aman:

- “Terhubung, menunggu posisi bus.” ✅
- “24 bus terpantau” hanya kalau `count(!isStale)` = 24. ✅
- “Bus akan tiba 2 menit lagi” tanpa label estimasi. ❌
- “Bus sudah mendekat” saat hanya broker LIVE tanpa konteks halte. ❌
- “Semua data live akurat 100%.” ❌

## 9. Aksesibilitas, performa, dan skala

- Gunakan semantik selected pada tab dan live region pada status.
- `ANIMATOR_DURATION_SCALE = 0` mematikan animasi maskot pada recomposition baru; `animate=false` membangun cabang statis tanpa infinite transition.
- Status dipahami lewat **teks + warna**, bukan warna saja.
- Bungkus teks panjang, gunakan ellipsis seperlunya; label pada tab aktif tetap terlihat.
- Minimum Android SDK 26 pada proyek; aplikasi memakai Jetpack Compose sesuai versi dependency yang tersedia.
- Kurangi maskot di atas MapLibre untuk menahan overdraw; tidak memakai Unity atau Rive runtime.
- Jangan meletakkan data MQTT di loop animasi; alirannya tetap `MapViewModel`.

## 10. Deliverables dan verifikasi

- `android/`: source lengkap yang dipertahankan, dengan pembaruan V5.
- `design-preview/`: screenshot simulasi, preview HTML interaktif, PNG 10 ekspresi, sumber SVG, dan tujuh foto referensi.
- `DESIGN.md`: spesifikasi ini.
- `QA_V5.md`: hasil pemeriksaan serta limitasi build.
- `CHANGES_V5.md`: daftar perubahan per modul.

**Build status:** Gradle wrapper mencoba mengunduh Gradle 8.9 namun gagal DNS pada `services.gradle.org`, sehingga APK belum dibangun dan kompatibilitas Android belum tervalidasi. Source tetap perlu diuji di Android Studio dengan SDK 35 dan JDK 21 atau GitHub Actions Android. Preview HTML telah diuji interaksi pada Chromium; **itu bukan bukti build Android**.

## 11. Acceptance checklist untuk iterasi berikutnya

- [ ] `./gradlew :app:assembleDebug` sukses di Android SDK 35.
- [ ] UI snapshot dari emulator Android 360x800, 412x915, landscape, font scale 1.35.
- [ ] Tidak ada UI crash saat MQTT offline, connecting, reconnect, error, dan live.
- [ ] Peta 2D/3D, layer style, lokasi, pan, zoom, AR/SOS tidak rusak.
- [ ] GTFS planner bisa memilih dua halte dan menunjukkan rute/empty state yang benar.
- [ ] Kelima tab tidak nested-dock overlap pada layar kecil.
- [ ] Maskot beranimasi tidak menyebabkan drop frame saat map + MQTT live.
- [ ] Pengaturan reduced motion/animator-scale tersimulasikan.
- [ ] Screen reader membaca status dan nama tab tanpa label duplikat.
- [ ] Review visual ulang terhadap 7 foto sumber.

## 12. Delivery audit bergaya anti-slop

| Gate | Kondisi | Catatan |
|---|---|---|
| Design direction sebelum build | PASS | 7 referensi + design tokens SELA tersedia |
| UI bukan template generik | PASS konseptual | Maskot original dan konteks transit nyata, komposisi berbeda per layar |
| Data dan copy bisa dipertanggungjawabkan | PASS di alur baru | Tidak ada angka ETA palsu di runtime baru; preview diberi label demo |
| Konsistensi visual | PARTIAL | Main tabs, history, settings, offline, SOS/mesh; AR belum dibentuk ulang |
| Uji interaktif preview | PASS | Chromium tanpa error JS, tab/route interactions bekerja |
| Kompilasi Android | BLOCKED | Gradle 8.9 tidak dapat diunduh di environment ini |
| Accessibility/performance device | NOT VERIFIED | Perlu emulator/perangkat sungguhan |
| Production-ready / shippable | FAIL | Jangan rilis APK sebelum build dan regression test |

**Priority:** compile test di environment Android, koreksi lint/error yang muncul, screenshot emulator, penyesuaian safe-area dan desain untuk 320dp/412dp.
