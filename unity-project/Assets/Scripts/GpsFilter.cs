// GpsFilter.cs — Filter GPS untuk Smart Follow SELA 3D.
//
// Spesifikasi (V4 §21 "GPS follow details"):
// - Pisahkan akurasi posisi, course-over-ground, dan kompas device
// - Blend arah pada threshold kecepatan; tahan heading terakhir saat diam
// - Jangan teleport kamera saat GPS buruk/stale
// - Update GPS tidak diikat ke frame rate (dipanggil per event)
// - Interpolasi kamera smooth per frame tanpa antrean animateCamera
//
// Data flow: SelaBridge.SetGps(json) -> MapController.UpdateGps -> sini.

using UnityEngine;

public class GpsFilter : MonoBehaviour
{
    [Header("Threshold")]
    [Tooltip("Kecepatan minimum (m/s) untuk memakai course-over-ground")]
    public float speedThreshold = 1.5f;
    [Tooltip("Akurasi maksimum (meter) agar GPS dianggap valid")]
    public float maxAccuracy = 50f;
    [Tooltip("Umur maksimum fix (detik) sebelum dianggap stale")]
    public float maxAgeSec = 10f;
    [Tooltip("Jarak maksimum perpindahan wajar per update (meter)")]
    public float maxJumpMeters = 200f;

    [Header("Smoothing")]
    [Range(0f, 1f)] public float positionLerp = 0.35f;
    [Range(0f, 1f)] public float bearingLerp = 0.25f;

    public struct FilteredGps
    {
        public double lat;
        public double lon;
        public float bearing;   // arah yang sudah difilter
        public float speed;     // m/s (estimasi)
        public bool valid;
        public bool stationary;
    }

    private FilteredGps last;
    private double lastUpdateTime;
    private bool hasLast;

    // Jendela sampel untuk estimasi kecepatan: (waktu, lat, lon) fix yang diterima.
    // Menggantikan threshold kaku `dt > 0.5s` — update GPS > 2Hz sebelumnya
    // selalu dianggap diam karena kecepatan tak pernah dihitung ulang.
    private readonly System.Collections.Generic.List<(double t, double lat, double lon)> samples =
        new System.Collections.Generic.List<(double t, double lat, double lon)>();
    private const double SpeedWindowSec = 3.0;
    private const double MinWindowSpanSec = 0.05;

    /// <summary>
    /// Proses satu fix GPS mentah. Kembalikan hasil filter.
    /// Dipanggil dari MapController.UpdateGps.
    /// </summary>
    public FilteredGps Process(double lat, double lon, float accuracy,
        float bearing, float courseOverGround, float speed)
    {
        var now = Time.realtimeSinceStartup;
        var result = new FilteredGps { valid = false };

        // 1. Validasi akurasi
        if (accuracy > maxAccuracy || accuracy <= 0f)
        {
            Debug.Log($"[GpsFilter] Tolak: akurasi {accuracy}m > {maxAccuracy}m");
            return last; // kembalikan fix terakhir yang valid
        }

        // 2. Validasi lompatan (anti-teleport)
        if (hasLast)
        {
            double dist = HaversineMeters(last.lat, last.lon, lat, lon);
            double dt = now - lastUpdateTime;
            if (dt > 0 && dist / dt > 83.3) // > 300 km/h = tidak wajar
            {
                Debug.Log($"[GpsFilter] Tolak: lompatan {dist:F0}m dalam {dt:F1}s");
                return last;
            }
            if (dist > maxJumpMeters && dt < 2.0)
            {
                Debug.Log($"[GpsFilter] Tolak: jump {dist:F0}m terlalu cepat");
                return last;
            }
        }

        // 3. Estimasi kecepatan dari jendela waktu (rata-rata beberapa sampel
        // terakhir), bukan threshold kaku. Update cepat (>2Hz) tetap terhitung.
        // Konvensi: speed <= 0 berarti "tidak disediakan" (MapController
        // mengirim 0) — maka estimasi dari perpindahan.
        float estSpeed = speed;
        if (speed <= 0f)
        {
            // Tambahkan sampel saat ini lalu pangkas di luar jendela
            samples.Add((now, lat, lon));
            while (samples.Count > 0 && now - samples[0].t > SpeedWindowSec)
                samples.RemoveAt(0);

            if (samples.Count >= 2)
            {
                var first = samples[0];
                var lastS = samples[samples.Count - 1];
                double span = lastS.t - first.t;
                if (span >= MinWindowSpanSec)
                {
                    double dist = HaversineMeters(first.lat, first.lon, lastS.lat, lastS.lon);
                    estSpeed = (float)(dist / span);
                }
                else if (hasLast)
                {
                    estSpeed = last.speed; // jendela terlalu pendek — tahan estimasi lama
                }
                else
                {
                    estSpeed = 0f;
                }
            }
            else if (hasLast)
            {
                estSpeed = last.speed;
            }
            else
            {
                estSpeed = 0f;
            }
        }

        // 4. Pilih bearing: course-over-ground saat bergerak, tahan saat diam
        float useBearing;
        bool stationary = estSpeed < speedThreshold;
        if (stationary)
        {
            // Tahan heading terakhir — jangan osilasi saat diam (§21)
            useBearing = hasLast ? last.bearing : bearing;
        }
        else
        {
            // Blend course-over-ground dengan kompas
            float cog = courseOverGround >= 0f ? courseOverGround : bearing;
            useBearing = hasLast
                ? Mathf.LerpAngle(last.bearing, cog, bearingLerp)
                : cog;
        }

        // 5. Smooth posisi
        double fLat = lat, fLon = lon;
        if (hasLast)
        {
            fLat = last.lat + (lat - last.lat) * positionLerp;
            fLon = last.lon + (lon - last.lon) * positionLerp;
        }

        result = new FilteredGps
        {
            lat = fLat,
            lon = fLon,
            bearing = useBearing,
            speed = estSpeed,
            valid = true,
            stationary = stationary,
        };

        last = result;
        lastUpdateTime = now;
        hasLast = true;
        return result;
    }

    /// <summary>
    /// Cek apakah fix terakhir sudah stale.
    /// </summary>
    public bool IsStale()
    {
        if (!hasLast) return true;
        return (Time.realtimeSinceStartup - lastUpdateTime) > maxAgeSec;
    }

    public FilteredGps LastValid() => last;

    private static double HaversineMeters(double lat1, double lon1,
        double lat2, double lon2)
    {
        const double R = 6371000.0;
        double dLat = (lat2 - lat1) * Mathf.Deg2Rad;
        double dLon = (lon2 - lon1) * Mathf.Deg2Rad;
        double a = System.Math.Sin(dLat / 2) * System.Math.Sin(dLat / 2) +
                   System.Math.Cos(lat1 * Mathf.Deg2Rad) *
                   System.Math.Cos(lat2 * Mathf.Deg2Rad) *
                   System.Math.Sin(dLon / 2) * System.Math.Sin(dLon / 2);
        return 2 * R * System.Math.Asin(System.Math.Sqrt(a));
    }
}
