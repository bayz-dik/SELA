package id.sela.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.selaPrefs2 by preferencesDataStore(name = "sela_prefs2")

/**
 * Preferensi SELA batch-2, disimpan di DataStore terpisah ("sela_prefs2").
 *
 * Sengaja file terpisah dari [TransitPreferences] supaya batch yang berbeda
 * tidak berebut edit file yang sama. Semua tersimpan lokal di perangkat,
 * tidak dikirim ke mana pun.
 */
@Singleton
class SelaPrefs @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        /** Mode tampilan: "normal" | "tunanetra" | "lansia" | "anak" (fitur #66/#121/#122). */
        const val MODE_NORMAL = "normal"
        const val MODE_TUNANETRA = "tunanetra"
        const val MODE_LANSIA = "lansia"
        const val MODE_ANAK = "anak"
    }

    private val uiModeKey = stringPreferencesKey("ui_mode")
    private val emergencyContactKey = stringPreferencesKey("emergency_contact")

    private val data = context.selaPrefs2.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val uiMode = data.map { it[uiModeKey] ?: MODE_NORMAL }.distinctUntilChanged()

    /** Nomor WhatsApp kontak darurat (format bebas, dinormalisasi saat dipakai). */
    val emergencyContact = data.map { it[emergencyContactKey].orEmpty() }.distinctUntilChanged()

    suspend fun setUiMode(mode: String) {
        val safe = when (mode) {
            MODE_TUNANETRA, MODE_LANSIA, MODE_ANAK -> mode
            else -> MODE_NORMAL
        }
        context.selaPrefs2.edit { it[uiModeKey] = safe }
    }

    suspend fun setEmergencyContact(phone: String) {
        context.selaPrefs2.edit { it[emergencyContactKey] = phone.trim() }
    }
}
