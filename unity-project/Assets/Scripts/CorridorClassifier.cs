// CorridorClassifier.cs — SATU sumber kebenaran klasifikasi koridor TJ.
//
// Dipakai BusMarkerManager (warna marker bus) dan RouteRenderer (warna polyline)
// agar klasifikasi konsisten. Sebelumnya: "4D" hijau di satu file, merah di file lain.

using UnityEngine;

public static class CorridorClassifier
{
    /// <summary>
    /// True untuk koridor BRT Transjakarta: angka 1-13, opsional diikuti
    /// sufiks huruf (mis. "4D", "1N", "9C"). Bukan cuma StartsWith("1") dsb.
    /// </summary>
    public static bool IsBrt(string routeId)
    {
        if (string.IsNullOrEmpty(routeId)) return false;
        string id = routeId.Trim().ToLowerInvariant();

        // Bentuk eksplisit: "brt...", "koridor 4", "koridor4"
        if (id.StartsWith("brt")) return true;
        if (id.StartsWith("koridor"))
        {
            string rest = id.Substring("koridor".Length).Trim();
            if (int.TryParse(rest, out int k)) return k >= 1 && k <= 13;
            return false;
        }

        // Bentuk angka + sufiks huruf opsional: "4", "4D", "12a"
        int i = 0;
        while (i < id.Length && char.IsDigit(id[i])) i++;
        if (i == 0) return false;
        if (!int.TryParse(id.Substring(0, i), out int n)) return false;
        if (n < 1 || n > 13) return false;
        // Sisa harus huruf saja (atau kosong) — "4D" ya, "4X5" bukan
        for (int j = i; j < id.Length; j++)
            if (!char.IsLetter(id[j])) return false;
        return true;
    }

    /// <summary>True untuk JakLingko: ID diawali "JAK" atau mengandung "jaklingko"
    /// (case-insensitive).</summary>
    public static bool IsJakLingko(string routeId)
    {
        if (string.IsNullOrEmpty(routeId)) return false;
        return routeId.StartsWith("JAK", System.StringComparison.OrdinalIgnoreCase) ||
            routeId.ToLowerInvariant().Contains("jaklingko");
    }
}
