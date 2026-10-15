package id.sela.feature.map

/**
 * Mode kamera peta — state machine §7/§21 spec 3D Ultra V2.0.
 *
 * - [MAP_2D]: peta 2D original, tidak berubah (pitch 0, tilt gesture mati).
 * - [MAP_3D_FREE]: 3D dengan kamera bebas (pan/pinch/rotate/tilt ala game).
 * - [MAP_3D_FOLLOW]: 3D mengikuti GPS; gesture manual membatalkan ke FREE.
 *
 * Batasan jujur: pitch maksimum 60° (batas MapLibre 11.x), bukan 6-DOF bebas.
 */
enum class MapMode {
    MAP_2D,
    MAP_3D_FREE,
    MAP_3D_FOLLOW,
}

val MapMode.is3d: Boolean get() = this != MapMode.MAP_2D

/** Pitch awal saat masuk 3D — kandidat 60° per §7 spec (batas SDK). */
const val MAP_3D_DEFAULT_TILT = 60.0

/** Zoom minimal yang nyaman untuk Follow agar gedung 3D terlihat. */
const val MAP_3D_FOLLOW_MIN_ZOOM = 15.0
