package id.sela.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Manajer SOS darurat (fitur #21, #59, #123).
 *
 * Batasan jujur — tanpa izin SMS (sengaja tidak diminta):
 * - Pesan SOS dikirim lewat WhatsApp (intent wa.me) → butuh WhatsApp terpasang
 *   DAN koneksi internet. Bukan SMS, bukan panggilan otomatis.
 * - "Hubungi 112" hanya membuka dialer (ACTION_DIAL) — pengguna tetap yang
 *   menekan tombol panggil. Android tidak mengizinkan panggilan otomatis
 *   tanpa izin CALL_PHONE yang berbahaya.
 */
object SosManager {
    /**
     * Normalisasi nomor HP Indonesia ke format internasional tanpa "+":
     * "0812…" → "62812…", "+62812…" → "62812…".
     */
    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("62") -> digits
            digits.isNotEmpty() -> "62$digits"
            else -> ""
        }
    }

    /** Susun pesan SOS Bahasa Indonesia dengan tautan lokasi Google Maps. */
    fun sosMessage(contactName: String, lat: Double, lon: Double, routeInfo: String): String {
        val mapsLink = "https://maps.google.com/?q=$lat,$lon"
        return buildString {
            append("DARURAT! Saya butuh bantuan")
            if (contactName.isNotBlank()) append(" — $contactName")
            append(".\n")
            if (routeInfo.isNotBlank()) append("Naik $routeInfo.\n")
            append("Lokasi terakhir saya: $mapsLink\n")
            append("Dikirim otomatis dari aplikasi SELA.")
        }
    }

    /**
     * Buka WhatsApp ke [phone] dengan [message] terisi. Melempar bila WhatsApp
     * tidak bisa menangani intent (tangkap di UI).
     */
    fun sendSosViaWhatsApp(context: Context, phone: String, message: String) {
        val normalized = normalizePhone(phone)
        require(normalized.isNotBlank()) { "Nomor kontak darurat kosong" }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$normalized?text=$encoded"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    /** Buka dialer ke 112 (layanan darurat Indonesia). */
    fun call112(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }
}
