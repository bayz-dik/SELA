package id.sela.assistant

/**
 * Konteks opsional yang memperkaya jawaban kondektur.
 *
 * @param altRoutes daftar nama rute alternatif yang bisa disarankan saat bus telat.
 * @param nearestStopName nama halte terdekat pengguna, kalau diketahui.
 * @param vehicles daftar kendaraan live dengan posisi, untuk query jarak.
 * @param userLat latitude pengguna, untuk hitung jarak.
 * @param userLon longitude pengguna, untuk hitung jarak.
 */
data class KonduktorCtx(
    val altRoutes: List<String> = emptyList(),
    val nearestStopName: String? = null,
    val vehicles: List<VehicleDistance> = emptyList(),
    val userLat: Double? = null,
    val userLon: Double? = null,
)

/**
 * Info jarak kendaraan untuk query kondektur.
 */
data class VehicleDistance(
    val id: String,
    val routeId: String,
    val lat: Double,
    val lon: Double,
)

/**
 * "AI" kondektur SELA — tanda kutipnya disengaja.
 *
 * Ini rule-based murni (pencocokan kata kunci, tanpa model bahasa, tanpa jaringan,
 * tanpa import Android sehingga bisa di-unit-test di JVM biasa). Jawabannya
 * template Bahasa Indonesia, bukan pemahaman bahasa beneran.
 *
 * Prioritas pencocokan: kata kunci spesifik dulu (telat → rute → macet → halte),
 * lalu sapaan, terakhir jawaban default.
 */
object KonduktorAi {
    /**
     * Balas pesan pengguna berdasarkan kata kunci.
     *
     * @param input teks mentah dari pengguna (akan di-lowercase dulu).
     * @param ctx konteks opsional (rute alternatif / halte terdekat).
     */
    fun reply(input: String, ctx: KonduktorCtx = KonduktorCtx()): String {
        val q = input.lowercase()
        // Pecah jadi kata supaya "lama" tidak cocok di dalam "selamat".
        val words = q.split(Regex("[^a-z]+")).filter { it.isNotBlank() }.toSet()
        fun has(vararg keywords: String) = keywords.any { it in words || it in q && it.contains(" ") }

        // 1. Kata kunci spesifik — dicek sebelum sapaan.
        // Query jarak: "bus 1A berapa meter lagi?" / "dimana bus koridor 1?"
        if (has("meter", "jarak", "berapa jauh", "dimana", "di mana", "posisi")) {
            return answerDistance(q, ctx)
        }
        if (has("telat", "lama", "kapan datang", "kapan")) {
            val alt = if (ctx.altRoutes.isNotEmpty()) {
                "coba cek rute alternatif: ${ctx.altRoutes.joinToString(", ")}. "
            } else {
                ""
            }
            return "Sabar ya... $alt" + "Lihat ETA live di peta biar pasti."
        }
        if (has("rute", "koridor", "jurusan")) {
            return "Aku tahu koridor TransJakarta 1–13 plus JakLingko. " +
                "Pilih halte di peta buat lihat rute yang lewat."
        }
        if (has("macet")) {
            return "Macet emang nyebelin. Coba berangkat 15 menit lebih awal besok, " +
                "atau cek koridor lain."
        }
        if (has("halte")) {
            val near = ctx.nearestStopName
            return if (near != null) {
                "Halte terdekatmu: $near. Buka detailnya buat lihat ETA."
            } else {
                "Pilih halte di peta buat lihat ETA live."
            }
        }

        // 2. Sapaan.
        if (has("halo", "hai", "pagi", "siang", "sore", "malam")) {
            return "Halo! Mau ke mana hari ini? Tanya aja soal bus atau halte."
        }

        // 3. Default.
        return "Aku versi offline — cuma bisa jawab soal rute, halte, dan ETA. " +
            "Coba tanya 'bus koridor 1 kapan datang?'"
    }

    /**
     * Jawab query jarak: "bus 1A berapa meter lagi?"
     * Cari bus yang cocok dari ctx.vehicles, hitung jarak Haversine ke user.
     */
    private fun answerDistance(q: String, ctx: KonduktorCtx): String {
        val userLat = ctx.userLat
        val userLon = ctx.userLon
        if (userLat == null || userLon == null) {
            return "Aktifkan lokasi dulu biar aku bisa hitung jarak bus ke kamu."
        }
        if (ctx.vehicles.isEmpty()) {
            return "Belum ada data bus live. Tunggu sebentar sampai koneksi MQTT nyambung."
        }

        // Cari kode bus/rute dari query (misal "1A", "koridor 1", "jak 46").
        // B34: cocokkan sebagai kata UTUH (word boundary) supaya "koridor 10"
        // tidak cocok dengan rute "1".
        val queryUpper = q.uppercase()
        val matched = ctx.vehicles.filter { v ->
            containsWholeToken(queryUpper, v.id.uppercase()) ||
                containsWholeToken(queryUpper, v.routeId.uppercase())
        }

        val candidates = if (matched.isNotEmpty()) matched else ctx.vehicles
        // Ambil yang terdekat
        val nearest = candidates.minByOrNull { v ->
            haversineMeters(userLat, userLon, v.lat, v.lon)
        } ?: return "Nggak ketemu bus yang cocok."

        val distM = haversineMeters(userLat, userLon, nearest.lat, nearest.lon)
        val distText = if (distM < 1000) {
            "${distM.toInt()} meter"
        } else {
            "%.1f km".format(distM / 1000)
        }
        return "Bus ${nearest.id} (rute ${nearest.routeId}) sekitar $distText dari kamu."
    }

    /**
     * B34 — cocokkan [token] sebagai kata utuh di [query] (word boundary di
     * kedua sisi). Mencegah "10" cocok dengan rute "1", atau "21" cocok
     * dengan bus "1".
     */
    private fun containsWholeToken(query: String, token: String): Boolean {
        if (token.isBlank()) return false
        return Regex("\\b${Regex.escape(token)}\\b").containsMatchIn(query)
    }

    /**
     * Jarak Haversine dalam meter antara dua titik koordinat.
     */
    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // radius bumi dalam meter
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
