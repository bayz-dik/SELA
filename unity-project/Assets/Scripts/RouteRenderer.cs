// RouteRenderer.cs — Polyline rute Transjakarta 3D untuk SELA Unity.
//
// Spesifikasi (V4 §8, §20):
// - Rute digambar sebagai garis di atas jalan (elevasi +3m agar tidak z-fight)
// - Warna koridor SELA: BRT merah (#FF5D5D), JakLingko biru (#38BDF8),
//   Bus Biasa hijau (#34D399), default volt (#CDF138)
// - Lebar garis proporsional zoom; casing gelap di bawah untuk kontras
// - Selected route: lebih tebal + glow
// - Efisien: satu LineRenderer per rute (jumlah rute aktif terbatas)
//
// Data flow: SelaBridge.SetRoutes(json) -> MapController.SetRoutes -> sini.

using UnityEngine;
using System.Collections.Generic;

public class RouteRenderer : MonoBehaviour
{
    [Header("Visual")]
    public float lineWidth = 4f;
    public float selectedLineWidth = 7f;
    public float elevationAboveRoad = 3f;
    public Color casingColor = new Color(0.04f, 0.05f, 0.08f, 0.9f);

    // Warna koridor SELA (sinkron dengan 2D MapLibre)
    private static readonly Dictionary<string, Color> CorridorColors =
        new Dictionary<string, Color>
        {
            { "brt", new Color(1f, 0.365f, 0.365f) },      // #FF5D5D
            { "jaklingko", new Color(0.22f, 0.741f, 0.973f) }, // #38BDF8
            { "bus", new Color(0.204f, 0.827f, 0.6f) },    // #34D399
        };
    private static readonly Color DefaultColor =
        new Color(0.804f, 0.945f, 0.220f); // volt #CDF138

    private class RouteLine
    {
        public string id;
        public GameObject root;
        public LineRenderer casing;
        public LineRenderer line;
        public bool selected;
    }

    private readonly List<RouteLine> routes = new List<RouteLine>();
    private MapController map;
    private string selectedId;

    // Material shared per (warna, renderQueue): dibuat sekali, dipakai semua rute.
    // (Sebelumnya `new Material` per garis — bocor setiap rebuild rute.)
    private static readonly Dictionary<string, Material> SharedLineMaterials =
        new Dictionary<string, Material>();

    void Awake()
    {
        map = GetComponent<MapController>();
        if (map == null) map = FindObjectOfType<MapController>();
    }

    /// <summary>
    /// Bangun ulang semua polyline rute. Dipanggil dari MapController.
    /// </summary>
    public void SetRoutes(SelaBridge.RouteDto[] routeDtos)
    {
        Clear();
        if (routeDtos == null || routeDtos.Length == 0 || map == null) return;

        foreach (var r in routeDtos)
        {
            if (r.points == null || r.points.Length < 2) continue;
            CreateRouteLine(r);
        }
    }

    public void SelectRoute(string routeId)
    {
        selectedId = routeId;
        foreach (var r in routes)
        {
            bool sel = r.id == routeId;
            if (r.selected != sel)
            {
                r.selected = sel;
                r.line.widthMultiplier = sel ? selectedLineWidth : lineWidth;
                r.casing.widthMultiplier = sel ? selectedLineWidth + 3f : lineWidth + 3f;
            }
        }
    }

    public void ClearSelection() => SelectRoute(null);

    private void CreateRouteLine(SelaBridge.RouteDto dto)
    {
        var root = new GameObject("Route_" + dto.id);
        root.transform.SetParent(transform, false);

        var positions = new Vector3[dto.points.Length];
        for (int i = 0; i < dto.points.Length; i++)
        {
            var pt = dto.points[i];
            if (pt == null) continue;
            var world = map.LatLonToWorld(pt.lat, pt.lon);
            world.y = elevationAboveRoad;
            positions[i] = world;
        }

        Color routeColor = ResolveColor(dto);

        // Casing: garis gelap lebih lebar di bawah (kontras di atas jalan)
        var casing = CreateLine(root, "Casing", positions, casingColor,
            lineWidth + 3f, 2998);
        // Line: garis warna koridor di atas casing
        var line = CreateLine(root, "Line", positions, routeColor,
            lineWidth, 2999);

        routes.Add(new RouteLine
        {
            id = dto.id,
            root = root,
            casing = casing,
            line = line,
            selected = dto.id == selectedId,
        });
    }

    private LineRenderer CreateLine(GameObject parent, string name,
        Vector3[] positions, Color color, float width, int renderQueue)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent.transform, false);
        var lr = go.AddComponent<LineRenderer>();
        lr.positionCount = positions.Length;
        lr.SetPositions(positions);
        lr.widthMultiplier = width;
        lr.useWorldSpace = false; // posisi relatif ke root (sudah world)
        // Koreksi: karena parent di origin, pakai world space true lebih aman
        lr.useWorldSpace = true;
        lr.numCornerVertices = 4;
        lr.numCapVertices = 4;
        lr.material = MakeLineMaterial(color, renderQueue);
        lr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
        lr.receiveShadows = false;
        return lr;
    }

    private Material MakeLineMaterial(Color color, int renderQueue)
    {
        string key = ColorUtility.ToHtmlStringRGBA(color) + ":" + renderQueue;
        if (!SharedLineMaterials.TryGetValue(key, out var mat) || mat == null)
        {
            mat = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
            mat.color = color;
            // Selalu di atas jalan/gedung agar rute terbaca (§8)
            mat.SetInt("_ZTest", (int)UnityEngine.Rendering.CompareFunction.Always);
            mat.renderQueue = renderQueue;
            SharedLineMaterials[key] = mat;
        }
        return mat;
    }

    private static Color ResolveColor(SelaBridge.RouteDto dto)
    {
        if (!string.IsNullOrEmpty(dto.color))
        {
            if (ColorUtility.TryParseHtmlString(dto.color, out var parsed))
                return parsed;
        }
        // Fallback: klasifikasi terpusat — konsisten dengan BusMarkerManager.
        string id = dto.id ?? "";
        if (CorridorClassifier.IsJakLingko(id)) return CorridorColors["jaklingko"];
        if (CorridorClassifier.IsBrt(id)) return CorridorColors["brt"];
        return DefaultColor;
    }

    public void Clear()
    {
        foreach (var r in routes)
            if (r.root != null) Destroy(r.root);
        routes.Clear();
    }
}
