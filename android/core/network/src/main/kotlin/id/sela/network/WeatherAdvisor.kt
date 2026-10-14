package id.sela.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Saran cuaca untuk komuter (fitur #69).
 *
 * Sumber: Open-Meteo (https://open-meteo.com) — gratis, tanpa API key.
 * Lokasi tetap: Jakarta (-6.2, 106.8).
 *
 * KDoc jujur: BUTUH INTERNET. Bila offline atau request gagal,
 * [advise] mengembalikan null (UI tidak menampilkan apa-apa, bukan error).
 * Ini saran kasar berbasis probabilitas hujan per jam, bukan prakiraan resmi.
 */
@Singleton
class WeatherAdvisor @Inject constructor() {

    data class Advice(val text: String, val rainProbability: Int)

    suspend fun advise(): Advice? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=-6.2&longitude=106.8" +
                    "&hourly=precipitation_probability&forecast_days=1&timezone=Asia%2FJakarta",
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                val probs = JSONObject(body)
                    .getJSONObject("hourly")
                    .getJSONArray("precipitation_probability")
                val nextHours = (0 until minOf(3, probs.length())).map { probs.getInt(it) }
                val maxProb = nextHours.maxOrNull() ?: return@runCatching null
                val text = when {
                    maxProb >= 70 -> "Hujan $maxProb% dalam 1–3 jam ke depan — berangkat naik bus sekarang biar nggak kehujanan."
                    maxProb >= 40 -> "Potensi hujan $maxProb% — bawa payung kalau mau jalan ke halte."
                    else -> return@runCatching null
                }
                Advice(text, maxProb)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}
