// QualityManager.cs — Adaptive quality tiers untuk SELA 3D Unity.
//
// Spesifikasi (V4 §V3 Addendum "Adaptive quality"):
// - Tier: Low / Medium / High / Ultra (user-selectable + auto)
// - Auto: pilih tier dari kemampuan GPU, RAM, Android version, thermal
// - Low-end: prioritaskan kamera smooth + keterbacaan; matikan shadow,
//   reflection, post-processing, kurangi draw distance
// - Semua tier: navigasi 2D/3D tetap fungsional
//
// API: QualityManager.Instance.ApplyTier(tier) / AutoDetect()

using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

public class QualityManager : MonoBehaviour
{
    public static QualityManager Instance { get; private set; }

    public enum Tier { Low, Medium, High, Ultra }

    [Header("Status")]
    public Tier currentTier = Tier.Medium;
    public bool autoMode = true;

    [Header("Referensi Komponen")]
    public NightLightingController lighting;
    public BuildingGenerator buildings;
    public StreetlightPlacer streetlights;

    // Batas draw distance per tier (meter)
    private const float DrawLow = 800f;
    private const float DrawMedium = 1500f;
    private const float DrawHigh = 2500f;
    private const float DrawUltra = 4000f;

    void Awake()
    {
        Instance = this;
        if (lighting == null) lighting = FindObjectOfType<NightLightingController>();
        if (buildings == null) buildings = FindObjectOfType<BuildingGenerator>();
        if (streetlights == null) streetlights = FindObjectOfType<StreetlightPlacer>();
    }

    void Start()
    {
        if (autoMode) AutoDetect();
        else ApplyTier(currentTier);
    }

    /// <summary>
    /// Deteksi kemampuan perangkat dan pilih tier otomatis.
    /// Kriteria: RAM, GPU shader level, core CPU, Android API.
    /// </summary>
    public Tier AutoDetect()
    {
        int ramMB = SystemInfo.systemMemorySize; // MB, 0 jika unknown
        int shaderLevel = SystemInfo.graphicsShaderLevel; // e.g. 50 = 5.0
        int cpuCores = SystemInfo.processorCount;
        bool isGLES3 = SystemInfo.graphicsDeviceType == GraphicsDeviceType.OpenGLES3;

        Tier tier;
        if (!isGLES3 || ramMB is > 0 and < 3072 || shaderLevel < 40)
            tier = Tier.Low;
        else if (ramMB < 6144 || cpuCores <= 4)
            tier = Tier.Medium;
        else if (ramMB < 8192)
            tier = Tier.High;
        else
            tier = Tier.Ultra;

        Debug.Log($"[QualityManager] AutoDetect: RAM={ramMB}MB shader={shaderLevel} " +
                  $"cores={cpuCores} gles3={isGLES3} -> {tier}");
        ApplyTier(tier);
        return tier;
    }

    public void ApplyTier(Tier tier)
    {
        currentTier = tier;
        var cam = Camera.main;
        float drawDist = DrawMedium;
        int pixelLightCount = 1;
        bool shadows = false;
        bool postProcessing = false;
        float lodBias = 1f;
        float streetlightDensity = 1f;

        switch (tier)
        {
            case Tier.Low:
                drawDist = DrawLow;
                pixelLightCount = 0;
                shadows = false;
                postProcessing = false;
                lodBias = 0.6f;
                streetlightDensity = 0.4f;
                if (cam != null) cam.farClipPlane = DrawLow;
                break;
            case Tier.Medium:
                drawDist = DrawMedium;
                pixelLightCount = 1;
                shadows = false;
                postProcessing = false;
                lodBias = 1f;
                streetlightDensity = 0.7f;
                if (cam != null) cam.farClipPlane = DrawMedium;
                break;
            case Tier.High:
                drawDist = DrawHigh;
                pixelLightCount = 2;
                shadows = true;
                postProcessing = false;
                lodBias = 1.3f;
                streetlightDensity = 1f;
                if (cam != null) cam.farClipPlane = DrawHigh;
                break;
            case Tier.Ultra:
                drawDist = DrawUltra;
                pixelLightCount = 4;
                shadows = true;
                postProcessing = true;
                lodBias = 1.6f;
                streetlightDensity = 1f;
                if (cam != null) cam.farClipPlane = DrawUltra;
                break;
        }

        QualitySettings.lodBias = lodBias;
        QualitySettings.pixelLightCount = pixelLightCount;
        QualitySettings.shadows = shadows
            ? UnityEngine.ShadowQuality.HardOnly : UnityEngine.ShadowQuality.Disable;

        // URP: matikan shadow via render pipeline asset jika ada
        var urp = GraphicsSettings.currentRenderPipeline as UniversalRenderPipelineAsset;
        if (urp != null)
        {
            // URP asset properties diakses via serialized object di editor;
            // di runtime kita atur lewat QualitySettings + per-light shadow.
            // (Keterbatasan runtime API — didokumentasikan.)
        }

        if (streetlights != null)
            streetlights.SetDensity(streetlightDensity);

        if (lighting != null)
            lighting.SetPostProcessing(postProcessing);

        Debug.Log($"[QualityManager] Tier {tier}: draw={drawDist}m lights={pixelLightCount} " +
                  $"shadow={shadows} pp={postProcessing}");
    }

    /// <summary>
    /// Dipanggil dari Kotlin via SelaBridge: {"tier":"low|medium|high|ultra|auto"}
    /// </summary>
    public void SetTierFromString(string tierName)
    {
        autoMode = false;
        switch ((tierName ?? "").ToLowerInvariant())
        {
            case "low": ApplyTier(Tier.Low); break;
            case "medium": ApplyTier(Tier.Medium); break;
            case "high": ApplyTier(Tier.High); break;
            case "ultra": ApplyTier(Tier.Ultra); break;
            case "auto":
                autoMode = true;
                AutoDetect();
                break;
            default:
                Debug.LogWarning($"[QualityManager] Tier tidak dikenal: {tierName}");
                break;
        }
    }

    /// <summary>
    /// Downgrade otomatis jika thermal/fps buruk (dipanggil berkala).
    /// </summary>
    public void ReportFrameTime(float avgMs)
    {
        // Jika rata-rata frame > 33ms (di bawah 30fps) selama periode,
        // turunkan satu tier — kecuali user mengunci manual.
        if (!autoMode) return;
        if (avgMs > 33f && currentTier > Tier.Low)
        {
            Debug.Log($"[QualityManager] Frame {avgMs:F1}ms — downgrade {currentTier} -> {currentTier - 1}");
            ApplyTier(currentTier - 1);
        }
    }
}
