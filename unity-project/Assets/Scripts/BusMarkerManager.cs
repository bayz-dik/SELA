// BusMarkerManager.cs — Marker 3D bus Transjakarta.
//
// - Bus dirender sebagai box memanjang dengan warna koridor
// - Interpolasi posisi smooth antar update MQTT
// - Rotasi mengikuti heading
// - Pooling: reuse GameObject, tidak instantiate per update

using UnityEngine;
using System.Collections.Generic;

public class BusMarkerManager : MonoBehaviour
{
    [Header("Config")]
    public float busLength = 12f;   // bus gandeng Transjakarta ~18m, biasa 12m
    public float busWidth = 2.5f;
    public float busHeight = 3f;

    private Dictionary<string, BusMarker> markers = new Dictionary<string, BusMarker>();
    private MapController mapController;

    // Warna koridor SELA: BRT merah, JakLingko biru, biasa hijau
    private static readonly Dictionary<string, Color> CorridorColors =
        new Dictionary<string, Color>
        {
            { "BRT", new Color(1f, 0.36f, 0.36f) },       // #FF5D5D
            { "JakLingko", new Color(0.22f, 0.74f, 0.97f) }, // #38BDF8
            { "default", new Color(0.20f, 0.83f, 0.60f) },   // #34D399
        };

    void Start()
    {
        mapController = MapController.Instance;
    }

    public void UpdateBuses(SelaBridge.BusDto[] buses)
    {
        if (buses == null) return;
        var seen = new HashSet<string>();

        foreach (var bus in buses)
        {
            seen.Add(bus.id);
            if (!markers.TryGetValue(bus.id, out var marker))
            {
                marker = CreateMarker(bus);
                markers[bus.id] = marker;
            }
            marker.SetTarget(bus, mapController);
        }

        // Hapus bus yang sudah tidak ada di feed
        var toRemove = new List<string>();
        foreach (var id in markers.Keys)
            if (!seen.Contains(id)) toRemove.Add(id);
        foreach (var id in toRemove)
        {
            Destroy(markers[id].gameObject);
            markers.Remove(id);
        }
    }

    private BusMarker CreateMarker(SelaBridge.BusDto bus)
    {
        var go = new GameObject($"Bus_{bus.id}");
        var marker = go.AddComponent<BusMarker>();
        marker.Configure(busLength, busWidth, busHeight, ColorFor(bus.routeId));
        return marker;
    }

    private Color ColorFor(string routeId)
    {
        // Klasifikasi terpusat (CorridorClassifier) — konsisten dengan RouteRenderer.
        if (CorridorClassifier.IsBrt(routeId)) return CorridorColors["BRT"];
        if (CorridorClassifier.IsJakLingko(routeId)) return CorridorColors["JakLingko"];
        return CorridorColors["default"];
    }
}

/// <summary>Marker individual dengan interpolasi smooth.</summary>
public class BusMarker : MonoBehaviour
{
    private Vector3 targetPos;
    private float targetHeading;
    private bool hasTarget;

    // Material shared per warna koridor: dibuat sekali, dipakai semua bus.
    // (Tidak lagi `new Material` per bus — sebelumnya bocor saat bus dihapus.)
    private static readonly Dictionary<Color, Material> SharedBodyMaterials =
        new Dictionary<Color, Material>();
    private static Material sharedGlassMaterial;

    private static Material BodyMaterial(Color color)
    {
        if (!SharedBodyMaterials.TryGetValue(color, out var mat) || mat == null)
        {
            mat = new Material(Shader.Find("Universal Render Pipeline/Lit"));
            mat.color = color;
            mat.SetFloat("_Smoothness", 0.6f);
            // Emissive ringan agar terlihat di malam hari
            mat.SetColor("_EmissionColor", color * 0.35f);
            mat.EnableKeyword("_EMISSION");
            SharedBodyMaterials[color] = mat;
        }
        return mat;
    }

    private static Material GlassMaterial()
    {
        if (sharedGlassMaterial == null)
        {
            sharedGlassMaterial = new Material(Shader.Find("Universal Render Pipeline/Lit"));
            sharedGlassMaterial.color = new Color(0.05f, 0.07f, 0.10f);
            sharedGlassMaterial.SetFloat("_Smoothness", 0.9f);
        }
        return sharedGlassMaterial;
    }

    public void Configure(float length, float width, float height, Color color)
    {
        var body = GameObject.CreatePrimitive(PrimitiveType.Cube);
        body.name = "Body";
        body.transform.SetParent(transform);
        body.transform.localPosition = new Vector3(0, height / 2 + 0.5f, 0);
        body.transform.localScale = new Vector3(width, height, length);
        // NB: GetComponent(string) agar kompilasi tidak butuh modul Physics.
        Destroy(body.GetComponent("Collider"));

        // sharedMaterial (bukan .material) agar tidak duplikat per bus.
        body.GetComponent<Renderer>().sharedMaterial = BodyMaterial(color);

        // Kaca depan (strip gelap)
        var windshield = GameObject.CreatePrimitive(PrimitiveType.Cube);
        windshield.name = "Windshield";
        windshield.transform.SetParent(transform);
        windshield.transform.localPosition = new Vector3(0, height / 2 + 1.2f, length / 2 - 0.1f);
        windshield.transform.localScale = new Vector3(width * 0.9f, 1f, 0.2f);
        Destroy(windshield.GetComponent("Collider"));
        windshield.GetComponent<Renderer>().sharedMaterial = GlassMaterial();
    }

    public void SetTarget(SelaBridge.BusDto bus, MapController mc)
    {
        if (mc == null) return;
        targetPos = mc.LatLonToWorld(bus.lat, bus.lon);
        targetPos.y = 0.5f;
        targetHeading = bus.heading;
        hasTarget = true;
        if (transform.position == Vector3.zero)
            transform.position = targetPos; // spawn langsung tanpa lerp pertama
    }

    void Update()
    {
        if (!hasTarget) return;
        // Interpolasi posisi (smooth, ~5 detik untuk menempuh jarak update)
        transform.position = Vector3.Lerp(transform.position, targetPos, Time.deltaTime * 2f);
        // Rotasi smooth ke heading
        float targetY = targetHeading;
        float currentY = transform.eulerAngles.y;
        float newY = Mathf.LerpAngle(currentY, targetY, Time.deltaTime * 3f);
        transform.rotation = Quaternion.Euler(0, newY, 0);
    }
}
