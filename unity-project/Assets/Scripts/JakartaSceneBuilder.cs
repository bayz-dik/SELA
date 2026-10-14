// JakartaSceneBuilder.cs — Membangun distrik Jakarta 3D prosedural.
//
// Dibuat untuk dijalankan sekali saat scene dimuat (atau via Editor menu).
// Menggunakan data GTFS (halte/rute) dari SELA + footprint gedung prosedural
// yang deterministik (seeded) agar konsisten antar sesi.
//
// Arsitektur:
// - JakartaSceneBuilder (ini): orkestrasi, membaca config JSON dari StreamingAssets
// - BuildingGenerator: gedung extrude + jendela malam
// - StreetlightPlacer: lampu jalan di sepanjang koridor
// - NightLightingController: siklus siang/senja/malam
//
// Data flow: Kotlin -> SelaBridge.SetStops/SetRoutes -> MapController ->
//   JakartaSceneBuilder.BuildDistrict()

using UnityEngine;
using System.Collections.Generic;

public class JakartaSceneBuilder : MonoBehaviour
{
    public static JakartaSceneBuilder Instance { get; private set; }

    [Header("Referensi")]
    public BuildingGenerator buildingGenerator;
    public StreetlightPlacer streetlightPlacer;
    public NightLightingController lightingController;

    [Header("Config")]
    [Tooltip("Radius distrik dalam meter dari origin")]
    public float districtRadius = 1500f;
    [Tooltip("Seed untuk generasi prosedural deterministik")]
    public int citySeed = 42;

    private bool built;

    void Awake()
    {
        Instance = this;
    }

    void Start()
    {
        // Auto-build dengan config default jika belum ada data dari Kotlin.
        // Ini memungkinkan preview di Editor tanpa app Android.
        if (!built)
        {
            BuildPreviewDistrict();
        }
    }

    /// <summary>
    /// Build distrik preview untuk testing di Editor (tanpa data Kotlin).
    /// Menggunakan grid jalan sintetis + gedung prosedural.
    /// </summary>
    public void BuildPreviewDistrict()
    {
        if (built) return;
        built = true;

        var rng = new System.Random(citySeed);
        Debug.Log("[JakartaSceneBuilder] Membangun distrik preview Jakarta...");

        // 1. Tanah dasar (dark asphalt, sesuai tema SELA Malam Kota)
        CreateGround();

        // 2. Grid jalan arterial (pola Jakarta: Sudirman-Thamrin axis)
        var roads = GenerateRoadGrid(rng);
        RoadMeshBuilder.Instance?.BuildRoads(roads);

        // 3. Gedung di blok-blok antar jalan
        buildingGenerator?.GenerateDistrict(rng, districtRadius);

        // 4. Lampu jalan di sepanjang arterial
        streetlightPlacer?.PlaceAlongRoads(roads, rng);

        // 5. Lighting default: malam (signature look SELA)
        lightingController?.SetTimeOfDay(21.5f);

        Debug.Log("[JakartaSceneBuilder] Distrik selesai.");
    }

    /// <summary>
    /// Build dari data nyata (dipanggil setelah SelaBridge menerima stops/routes).
    /// Rute Transjakarta dipakai sebagai arterial utama; halte sebagai anchor
    /// gedung komersial di sekitarnya.
    /// </summary>
    public void BuildFromRealData(
        List<SelaBridge.StopDto> stops,
        List<SelaBridge.RouteDto> routes)
    {
        var map = MapController.Instance;
        if (map == null) { if (!built) BuildPreviewDistrict(); return; }

        var rng = new System.Random(citySeed);

        // 1. Tanah dasar
        if (!built) CreateGround();

        // 2. Arterial dari rute Transjakarta nyata (geometri GTFS shapes)
        var arterials = new List<List<Vector3>>();
        if (routes != null)
        {
            foreach (var r in routes)
            {
                if (r.points == null || r.points.Length < 2) continue;
                var line = new List<Vector3>();
                foreach (var pt in r.points)
                {
                    if (pt == null || pt.Length < 2) continue;
                    var w = map.LatLonToWorld(pt[0], pt[1]);
                    // Filter ke dalam radius distrik
                    if (w.magnitude < districtRadius * 1.5f) line.Add(w);
                }
                if (line.Count >= 2) arterials.Add(line);
            }
        }
        Debug.Log($"[JakartaSceneBuilder] {arterials.Count} arterial dari rute TJ");

        // 3. Grid jalan lokal prosedural sebagai pengisi antar arterial
        var localRoads = GenerateRoadGrid(rng);

        // 4. Bangun mesh: arterial dulu (lebih lebar), lalu lokal
        var roadBuilder = RoadMeshBuilder.Instance;
        if (roadBuilder != null)
        {
            roadBuilder.BuildRoads(arterials, isArterial: true);
            roadBuilder.BuildRoads(
                localRoads.ConvertAll(l => l), isArterial: false);
        }

        // 5. Gedung: blok komersial di dekat halte, residensial di sela
        buildingGenerator?.GenerateDistrict(rng, districtRadius);
        if (stops != null && buildingGenerator != null)
        {
            foreach (var s in stops)
            {
                var w = map.LatLonToWorld(s.lat, s.lon);
                if (w.magnitude < districtRadius)
                    buildingGenerator.MarkCommercialAnchor(w, 150f);
            }
        }

        // 6. Lampu di sepanjang arterial nyata
        if (arterials.Count > 0)
            streetlightPlacer?.PlaceAlongPolylines(arterials, rng);
        else
            streetlightPlacer?.PlaceAlongRoads(
                localRoads.ConvertAll(l => l), rng);

        // 7. Lighting malam default
        lightingController?.SetTimeOfDay(21.5f);

        built = true;
        Debug.Log($"[JakartaSceneBuilder] Build dari data nyata selesai: " +
                  $"{stops?.Count ?? 0} halte, {routes?.Count ?? 0} rute");
    }

    private void CreateGround()
    {
        var ground = GameObject.CreatePrimitive(PrimitiveType.Plane);
        ground.name = "Ground_Jakarta";
        float size = districtRadius * 2.5f;
        ground.transform.localScale = new Vector3(size / 10f, 1f, size / 10f);
        ground.transform.position = Vector3.zero;

        var mat = new Material(Shader.Find("Universal Render Pipeline/Lit"));
        // #0A0D14 — SELA Malam Kota background
        mat.color = new Color(0.039f, 0.051f, 0.078f, 1f);
        mat.SetFloat("_Smoothness", 0.1f);
        ground.GetComponent<Renderer>().material = mat;
    }

    private List<RoadSegment> GenerateRoadGrid(System.Random rng)
    {
        var roads = new List<RoadSegment>();
        float spacing = 300f; // blok Jakarta ~300m
        float half = districtRadius;

        // Jalan horizontal (timur-barat)
        for (float z = -half; z <= half; z += spacing)
        {
            bool arterial = Mathf.Abs(z) < spacing * 0.5f;
            roads.Add(new RoadSegment
            {
                start = new Vector3(-half, 0.02f, z),
                end = new Vector3(half, 0.02f, z),
                width = arterial ? 24f : 12f,
                isArterial = arterial,
            });
        }
        // Jalan vertikal (utara-selatan)
        for (float x = -half; x <= half; x += spacing)
        {
            bool arterial = Mathf.Abs(x) < spacing * 0.5f;
            roads.Add(new RoadSegment
            {
                start = new Vector3(x, 0.02f, -half),
                end = new Vector3(x, 0.02f, half),
                width = arterial ? 24f : 12f,
                isArterial = arterial,
            });
        }
        return roads;
    }

    [System.Serializable]
    public class RoadSegment
    {
        public Vector3 start;
        public Vector3 end;
        public float width = 12f;
        public bool isArterial;
    }
}
