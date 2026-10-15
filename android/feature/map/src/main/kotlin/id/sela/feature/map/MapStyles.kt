package id.sela.feature.map

/** Style peta gelap OpenFreeMap — default, selaras tema Malam Kota. */
internal const val SELA_MAP_STYLE = "https://tiles.openfreemap.org/styles/dark"

/**
 * B24: style terang OpenFreeMap — FALLBACK DARURAT bila style gelap gagal
 * dimuat (jaringan macet, style corrupt, dsb). Tidak dipakai saat peta sehat;
 * hanya muncul agar user tidak menatap peta hitam misterius.
 */
internal const val SELA_BRIGHT_STYLE = "https://tiles.openfreemap.org/styles/bright"

/**
 * B20: style satelit — tile raster gratis ESRI World Imagery.
 * MapLibre butuh objek style penuh (bukan tile URL mentah), jadi dibungkus
 * JSON inline. Atribusi Esri tampil via uiSettings.isAttributionEnabled.
 * URL tile terverifikasi 2026-10-15 (HTTP 200, byte gambar valid).
 */
internal const val SELA_SATELLITE_STYLE = """
{
  "version": 8,
  "name": "SELA Satelit",
  "sources": {
    "esri-imagery": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256,
      "maxzoom": 19,
      "attribution": "Imagery © Esri, Maxar, Earthstar Geographics"
    }
  },
  "layers": [
    { "id": "esri-imagery", "type": "raster", "source": "esri-imagery", "minzoom": 0, "maxzoom": 22 }
  ]
}
"""

/**
 * B24: rantai percobaan style berlapis.
 * - Style URL vektor (gelap): [diminta, terang] — gelap dulu, terang cadangan.
 * - JSON inline (satelit): [satelit] — tanpa fallback, langsung pesan error bila gagal.
 */
internal fun styleFallbackChain(requested: String): List<String> =
    if (requested.trimStart().startsWith("{")) listOf(requested)
    else (listOf(requested) + SELA_BRIGHT_STYLE).distinct()
