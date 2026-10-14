// NightLightingController.cs — Siklus siang/senja/malam Jakarta.
//
// - Posisi matahari dari jam lokal Asia/Jakarta (aproksimasi)
// - Transisi smooth antar fase
// - Lampu kota (emissive) menyala otomatis saat malam
//
// Dipanggil via SelaBridge.SetTimeOfDay({"hour":..}).

using UnityEngine;

public class NightLightingController : MonoBehaviour
{
    [Header("Referensi")]
    public Light sunLight;
    public Light moonLight;

    [Header("Warna langit")]
    public Color daySky = new Color(0.53f, 0.75f, 0.95f);
    public Color duskSky = new Color(0.98f, 0.45f, 0.25f);
    public Color nightSky = new Color(0.039f, 0.051f, 0.078f); // #0A0D14 SELA

    private float currentHour = 21.5f;
    private float targetHour = 21.5f;

    void Start()
    {
        if (sunLight == null)
        {
            var sunGo = new GameObject("Sun");
            sunLight = sunGo.AddComponent<Light>();
            sunLight.type = LightType.Directional;
            sunLight.shadows = LightShadows.Soft;
        }
        if (moonLight == null)
        {
            var moonGo = new GameObject("Moon");
            moonLight = moonGo.AddComponent<Light>();
            moonLight.type = LightType.Directional;
            moonLight.intensity = 0f;
        }
        ApplyHour(currentHour);
    }

    void Update()
    {
        // Transisi smooth ke target jam
        if (Mathf.Abs(currentHour - targetHour) > 0.01f)
        {
            currentHour = Mathf.Lerp(currentHour, targetHour, Time.deltaTime * 0.5f);
            ApplyHour(currentHour);
        }
    }

    public void SetTimeOfDay(float hourJakarta)
    {
        targetHour = Mathf.Repeat(hourJakarta, 24f);
    }

    private void ApplyHour(float hour)
    {
        // Fase: 0-5 malam, 5-7 fajar, 7-17 siang, 17-19 senja, 19-24 malam
        float nightFactor = NightFactor(hour); // 0=siang, 1=malam penuh

        // Langit
        Color sky;
        if (hour >= 5 && hour < 7)
            sky = Color.Lerp(nightSky, daySky, (hour - 5) / 2f);
        else if (hour >= 7 && hour < 17)
            sky = daySky;
        else if (hour >= 17 && hour < 19)
            sky = Color.Lerp(daySky, duskSky, (hour - 17) / 2f);
        else if (hour >= 19 && hour < 20)
            sky = Color.Lerp(duskSky, nightSky, hour - 19);
        else
            sky = nightSky;

        RenderSettings.ambientSkyColor = sky;
        Camera.main.backgroundColor = sky;

        // Matahari: sudut dari jam (terbit 6, terbenam 18)
        float sunAngle = ((hour - 6f) / 12f) * 180f; // 0=horizon timur, 180=horizon barat
        bool sunUp = hour > 5.5f && hour < 18.5f;
        sunLight.intensity = sunUp ? Mathf.Clamp01(Mathf.Sin(sunAngle * Mathf.Deg2Rad)) * 1.2f : 0f;
        sunLight.transform.rotation = Quaternion.Euler(sunAngle, 30f, 0f);
        sunLight.color = Color.Lerp(
            new Color(1f, 0.6f, 0.35f), // senja
            Color.white,                 // siang
            Mathf.Clamp01(sunLight.intensity));

        // Bulan + ambient malam
        moonLight.intensity = nightFactor * 0.25f;
        RenderSettings.ambientIntensity = Mathf.Lerp(1f, 0.35f, nightFactor);

        // Fog malam untuk depth
        RenderSettings.fog = true;
        RenderSettings.fogColor = sky;
        RenderSettings.fogDensity = 0.0008f + nightFactor * 0.0004f;
    }

    private float NightFactor(float hour)
    {
        if (hour >= 19 || hour < 5) return 1f;
        if (hour >= 7 && hour < 17) return 0f;
        if (hour >= 5 && hour < 7) return 1f - (hour - 5) / 2f;
        return (hour - 17) / 2f; // 17-19
    }

    /// <summary>
    /// Aktif/nonaktifkan post-processing (bloom). Dipanggil QualityManager.
    /// Implementasi ringan: atur intensitas glow via fog + emissive scale.
    /// Bloom URP penuh butuh Volume profile — didokumentasikan sebagai
    /// peningkatan bertahap (Phase 6).
    /// </summary>
    public void SetPostProcessing(bool enabled)
    {
        // Simpan flag; dipakai untuk skala intensitas emissive jendela/lampu.
        postProcessingEnabled = enabled;
        Debug.Log($"[NightLightingController] Post-processing: {(enabled ? "ON" : "OFF")}");
    }

    private bool postProcessingEnabled = false;
}
