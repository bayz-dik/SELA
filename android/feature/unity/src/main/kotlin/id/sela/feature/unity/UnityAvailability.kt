package id.sela.feature.unity

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.os.Build

/**
 * Deteksi kemampuan perangkat untuk Unity 3D (§V3 addendum: adaptive quality).
 *
 * 3D Unity hanya ditawarkan jika perangkat memenuhi syarat minimum.
 * Jika tidak, SELA tetap jalan penuh dengan MapLibre 2D.
 */
object UnityAvailability {

    /** True jika unityLibrary ada di APK (Unity export sudah di-drop-in). */
    fun isUnityLibraryPresent(): Boolean {
        return try {
            Class.forName("com.unity3d.player.UnityPlayer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** True jika GPU mendukung OpenGL ES 3.0+ (syarat Unity modern). */
    fun supportsGles3(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info: ConfigurationInfo = am.deviceConfigurationInfo
        return info.reqGlEsVersion >= 0x30000
    }

    /** Estimasi RAM dalam GB. */
    fun ramGb(context: Context): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem / 1e9
    }

    /** True jika arsitektur CPU didukung Unity (ARM64). */
    fun isSupportedAbi(): Boolean {
        return Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
    }

    /**
     * Keputusan akhir: bolehkan tombol 3D Unity?
     * Syarat: unityLibrary ada + GLES3 + ARM64.
     * RAM < 3GB tetap boleh tapi Unity memakai tier grafis rendah (diatur di sisi Unity).
     */
    fun is3dSupported(context: Context): Boolean {
        return isUnityLibraryPresent() && supportsGles3(context) && isSupportedAbi()
    }

    /** Alasan kenapa 3D tidak tersedia (untuk pesan ke user). */
    fun unsupportedReason(context: Context): String {
        return when {
            !isUnityLibraryPresent() -> "Modul 3D belum terpasang di build ini."
            !isSupportedAbi() -> "3D butuh perangkat ARM64."
            !supportsGles3(context) -> "3D butuh GPU OpenGL ES 3.0+."
            else -> "Perangkat tidak mendukung 3D."
        }
    }
}
