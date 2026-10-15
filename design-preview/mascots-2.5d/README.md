# Maskot 2.5D SELA — "Sela"

## Karakter

Makhluk organik original: tubuh clay/jelly pastel biru, perut mint dengan motif
jejak rute (titik-titik seperti jalur bus), telinga lavender. Soft studio lighting,
ambient occlusion, contact shadow.

## Varian ekspresi (5 render, WebP ~44KB total di APK)

| File | Ekspresi | Dipakai untuk |
|---|---|---|
| `sela_mascot_idle.png` | Netral, senyum lembut | Idle, WaitingBus |
| `sela_mascot_happy.png` | Senyum lebar, mata ceria | Happy, Welcome, RouteFound, BusApproaching, Success |
| `sela_mascot_thinking.png` | Melirik, berpikir | Thinking, Searching |
| `sela_mascot_sleepy.png` | Mata terpejam, tenang | Sleeping, Offline, Empty |
| `sela_mascot_sad.png` | Kecewa lembut | Error |

## 10 animasi (Compose procedural di `MascotView.kt`)

| # | State | Render | Animasi |
|---|---|---|---|
| 1 | Idle | idle | Floating lembut (2.3s) |
| 2 | Welcome | happy | Bounce ceria + floating |
| 3 | Searching | thinking | Tilt oleng + floating |
| 4 | RouteFound | happy | Happy bounce |
| 5 | WaitingBus | idle | Sway + floating |
| 6 | BusApproaching | happy | Excited bounce cepat (0.45s) |
| 7 | Offline | sleepy | Float lambat (3.4s) |
| 8 | Error | sad | Shake halus + floating |
| 9 | Success | happy | Celebratory bounce (0.5s) |
| 10 | Empty | sleepy | Calm float lambat |

Semua animasi menghormati `ANIMATOR_DURATION_SCALE` sistem (mati jika user
menonaktifkan animasi).

## Pipeline (jujur)

Render dibuat via AI image generation (bukan Blender). Kualitas visual setara
referensi 3D-rendered. Jika Bayu menginginkan pipeline Blender asli (model +
rig + render), itu menjadi gap untuk Codex.

## Integrasi

- `core/designsystem/.../MascotView.kt`: `SelaMascot(expression, ...)` — API sama
  seperti sebelumnya, semua pemakaian existing otomatis memakai render baru.
- Aset: `app/src/main/res/drawable-xxhdpi/sela_mascot_*.webp`
