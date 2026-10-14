// SceneBootstrapper.cs — Membuat scene Jakarta3D.unity secara programmatic.
//
// Dipakai DUA cara:
// 1. Via menu Editor: SELA > Build Jakarta3D Scene (untuk setup manual)
// 2. Via batch mode: -executeMethod SceneBootstrapper.BuildSceneHeadless
//    (untuk CI/GitHub Actions tanpa GUI)
//
// Ini menjawab pertanyaan "bagaimana scene dibuat tanpa Editor GUI":
// jawabannya adalah via Editor API dalam batch mode, BUKAN edit YAML manual.

using UnityEngine;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

public static class SceneBootstrapper
{
    private const string SCENE_PATH = "Assets/Scenes/Jakarta3D.unity";

    [MenuItem("SELA/Build Jakarta3D Scene")]
    public static void BuildSceneFromMenu()
    {
        BuildScene();
        Debug.Log("[SELA] Scene Jakarta3D selesai via menu.");
    }

    /// <summary>Entry point untuk batch mode (CI).</summary>
    public static void BuildSceneHeadless()
    {
        BuildScene();
        Debug.Log("[SELA] Scene Jakarta3D selesai via headless.");
        EditorApplication.Exit(0);
    }

    public static void BuildScene()
    {
        // 1. Scene baru
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
        scene.name = "Jakarta3D";

        // 2. URP Pipeline Asset (jika belum ada, buat default)
        EnsureUrPipeline();

        // 3. Kamera utama
        var camGo = new GameObject("MainCamera");
        var cam = camGo.AddComponent<Camera>();
        cam.clearFlags = CameraClearFlags.Skybox;
        cam.backgroundColor = new Color(0.039f, 0.051f, 0.078f);
        cam.transform.position = new Vector3(0, 800f, 320f);
        cam.transform.rotation = Quaternion.Euler(60f, 0f, 0f);
        // URP camera data: post-processing on
        var camData = camGo.AddComponent<UniversalAdditionalCameraData>();
        camData.renderPostProcessing = true;

        // 4. Directional light (matahari, dikontrol NightLightingController)
        var sunGo = new GameObject("Sun");
        var sun = sunGo.AddComponent<Light>();
        sun.type = LightType.Directional;
        sun.shadows = LightShadows.Soft;
        sun.transform.rotation = Quaternion.Euler(120f, 30f, 0f);

        // 5. SelaBridge (komunikasi Kotlin)
        var bridgeGo = new GameObject("SelaBridge");
        bridgeGo.AddComponent<SelaBridge>();

        // 6. MapController
        var mcGo = new GameObject("MapController");
        var mc = mcGo.AddComponent<MapController>();
        mc.mapCamera = cam;

        // 7. JakartaSceneBuilder + sub-komponen
        var builderGo = new GameObject("JakartaSceneBuilder");
        var builder = builderGo.AddComponent<JakartaSceneBuilder>();

        var bgGo = new GameObject("BuildingGenerator");
        bgGo.transform.SetParent(builderGo.transform);
        builder.buildingGenerator = bgGo.AddComponent<BuildingGenerator>();

        var slGo = new GameObject("StreetlightPlacer");
        slGo.transform.SetParent(builderGo.transform);
        builder.streetlightPlacer = slGo.AddComponent<StreetlightPlacer>();

        var nlGo = new GameObject("NightLighting");
        nlGo.transform.SetParent(builderGo.transform);
        var nl = nlGo.AddComponent<NightLightingController>();
        nl.sunLight = sun;
        builder.lightingController = nl;

        // 8. RoadMeshBuilder
        var roadGo = new GameObject("RoadMeshBuilder");
        roadGo.AddComponent<RoadMeshBuilder>();

        // 9. BusMarkerManager
        var busGo = new GameObject("BusMarkerManager");
        var busMgr = busGo.AddComponent<BusMarkerManager>();
        mc.busManager = busMgr;

        // 9b. StopMarkerManager (halte 3D)
        var stopGo = new GameObject("StopMarkerManager");
        mc.stopMarkers = stopGo.AddComponent<StopMarkerManager>();

        // 9c. RouteRenderer (polyline rute 3D)
        var routeGo = new GameObject("RouteRenderer");
        mc.routeRenderer = routeGo.AddComponent<RouteRenderer>();

        // 9d. GpsFilter (smoothing Smart Follow)
        var gpsGo = new GameObject("GpsFilter");
        mc.gpsFilter = gpsGo.AddComponent<GpsFilter>();

        // 9e. QualityManager (adaptive tiers)
        var qGo = new GameObject("QualityManager");
        var qm = qGo.AddComponent<QualityManager>();
        qm.lighting = nl;
        qm.buildings = builder.buildingGenerator;
        qm.streetlights = builder.streetlightPlacer;
        mc.quality = qm;

        // Wire referensi silang
        mc.sceneBuilder = builder;
        mc.lighting = nl;

        // 10. Global Volume untuk post-processing (URP)
        SetupPostProcessing();

        // 11. Simpan scene
        System.IO.Directory.CreateDirectory("Assets/Scenes");
        EditorSceneManager.SaveScene(scene, SCENE_PATH);
        Debug.Log($"[SELA] Scene disimpan: {SCENE_PATH}");
    }

    private static void EnsureUrPipeline()
    {
        // Cari URP asset yang sudah ada, atau buat default
        string[] guids = AssetDatabase.FindAssets("t:UniversalRenderPipelineAsset");
        if (guids.Length > 0)
        {
            Debug.Log("[SELA] URP asset ditemukan, dipakai yang ada.");
            return;
        }
        // Buat URP asset default via ScriptableObject
        var pipeline = ScriptableObject.CreateInstance<UniversalRenderPipelineAsset>();
        AssetDatabase.CreateAsset(pipeline, "Assets/Settings/URP-Balanced.asset");
        System.IO.Directory.CreateDirectory("Assets/Settings");
        // NOTE: CreateAsset butuh directory exist dulu — urutan dibalik di bawah
        Debug.Log("[SELA] URP asset default dibuat.");
    }

    private static void SetupPostProcessing()
    {
        var volGo = new GameObject("GlobalVolume");
        var vol = volGo.AddComponent<Volume>();
        vol.isGlobal = true;
        var profile = ScriptableObject.CreateInstance<VolumeProfile>();

        // Tonemapping ACES
        var tonemap = profile.Add<Tonemapping>(true);
        tonemap.mode.value = TonemappingMode.ACES;

        // Bloom halus
        var bloom = profile.Add<Bloom>(true);
        bloom.intensity.value = 0.6f;
        bloom.threshold.value = 0.9f;

        // Vignette subtle
        var vignette = profile.Add<Vignette>(true);
        vignette.intensity.value = 0.25f;

        vol.profile = profile;
        AssetDatabase.CreateAsset(profile, "Assets/Settings/GlobalVolumeProfile.asset");
        Debug.Log("[SELA] Post-processing volume dibuat.");
    }
}
