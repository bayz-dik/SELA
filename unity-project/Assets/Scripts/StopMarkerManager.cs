// StopMarkerManager.cs — Marker halte 3D untuk SELA Unity.
//
// Spesifikasi (V4 §8, §20):
// - Billboard pin: selalu menghadap kamera, anchor di posisi halte
// - Label nama halte via TextMeshPro (fallback: TextMesh)
// - Selected state: pin membesar + ring highlight
// - Tap (raycast) -> SelaBridge.NotifyStopSelected(stopId)
// - Z-order: pin digambar di atas gedung (depth test off untuk pin, on untuk label)
// - Clustering: di zoom rendah, gabungkan halte berdekatan (sederhana: grid cell)
//
// Data flow: SelaBridge.SetStops(json) -> MapController.SetStops -> sini.

using UnityEngine;
using System.Collections.Generic;

public class StopMarkerManager : MonoBehaviour
{
    [Header("Visual")]
    public Color pinColor = new Color(0.804f, 0.945f, 0.220f, 1f); // volt #CDF138
    public Color pinSelectedColor = Color.white;
    public float pinSize = 6f;
    public float pinSelectedSize = 9f;
    public float labelHeight = 14f;

    [Header("Clustering")]
    public float clusterCellMeters = 120f; // ukuran sel grid untuk clustering
    public float clusterMinZoom = 13.5f;   // di bawah zoom ini, cluster aktif

    private class Marker
    {
        public string id;
        public string name;
        public Vector3 worldPos;
        public GameObject root;
        public GameObject pin;
        public GameObject label;
        public bool selected;
    }

    private readonly List<Marker> markers = new List<Marker>();
    private readonly Dictionary<string, Marker> byId = new Dictionary<string, Marker>();
    private MapController map;
    private string selectedId;
    private float currentZoom = 15f;

    void Awake()
    {
        map = GetComponent<MapController>();
        if (map == null) map = FindObjectOfType<MapController>();
    }

    /// <summary>
    /// Bangun ulang semua marker dari data halte. Dipanggil dari MapController.
    /// </summary>
    public void SetStops(SelaBridge.StopDto[] stops, float zoom)
    {
        currentZoom = zoom;
        Clear();
        if (stops == null || stops.Length == 0 || map == null) return;

        if (zoom < clusterMinZoom)
            BuildClustered(stops);
        else
            BuildIndividual(stops);
    }

    public void SetZoom(float zoom)
    {
        // Rebuild hanya jika melewati threshold clustering
        bool wasClustered = currentZoom < clusterMinZoom;
        bool nowClustered = zoom < clusterMinZoom;
        currentZoom = zoom;
        if (wasClustered != nowClustered && markers.Count > 0)
        {
            // Simpan data lalu rebuild — murah karena jumlah marker terbatas
            var dtos = new List<SelaBridge.StopDto>();
            foreach (var m in markers)
                dtos.Add(new SelaBridge.StopDto { id = m.id, name = m.name });
            // worldPos -> lat/lon dibalik untuk rebuild akurat
            RebuildFromMarkers(dtos, nowClustered);
        }
    }

    public void SelectStop(string stopId)
    {
        selectedId = stopId;
        foreach (var m in markers)
        {
            bool sel = m.id == stopId;
            if (m.selected != sel)
            {
                m.selected = sel;
                ApplyVisual(m);
            }
        }
    }

    public void ClearSelection() => SelectStop(null);

    void Update()
    {
        // Billboard: pin selalu menghadap kamera
        var cam = map != null ? map.mapCamera : Camera.main;
        if (cam == null) return;
        var camRot = cam.transform.rotation;
        foreach (var m in markers)
        {
            if (m.pin != null) m.pin.transform.rotation = camRot;
            if (m.label != null) m.label.transform.rotation = camRot;
        }

        // Tap detection (touch + mouse untuk editor)
        if (Input.GetMouseButtonDown(0) && cam != null)
        {
            var ray = cam.ScreenPointToRay(Input.mousePosition);
            if (Physics.Raycast(ray, out var hit, 5000f))
            {
                var marker = hit.collider.GetComponentInParent<MarkerRef>();
                if (marker != null && !string.IsNullOrEmpty(marker.stopId))
                {
                    SelectStop(marker.stopId);
                    FindObjectOfType<SelaBridge>()?.NotifyStopSelected(marker.stopId);
                }
            }
        }
    }

    // ---------- pembangunan ----------

    private void BuildIndividual(SelaBridge.StopDto[] stops)
    {
        foreach (var s in stops)
        {
            var world = map.LatLonToWorld(s.lat, s.lon);
            world.y = 2f; // sedikit di atas tanah
            CreateMarker(s.id, s.name ?? s.id, world, 1);
        }
    }

    private void BuildClustered(SelaBridge.StopDto[] stops)
    {
        // Grid clustering sederhana: gabungkan halte dalam satu sel
        var cells = new Dictionary<(int, int), List<SelaBridge.StopDto>>();
        foreach (var s in stops)
        {
            var world = map.LatLonToWorld(s.lat, s.lon);
            int cx = Mathf.FloorToInt(world.x / clusterCellMeters);
            int cz = Mathf.FloorToInt(world.z / clusterCellMeters);
            var key = (cx, cz);
            if (!cells.TryGetValue(key, out var list))
            {
                list = new List<SelaBridge.StopDto>();
                cells[key] = list;
            }
            list.Add(s);
        }

        foreach (var kv in cells)
        {
            var list = kv.Value;
            if (list.Count == 1)
            {
                var s = list[0];
                var world = map.LatLonToWorld(s.lat, s.lon);
                world.y = 2f;
                CreateMarker(s.id, s.name ?? s.id, world, 1);
            }
            else
            {
                // Cluster: posisi rata-rata, label "N halte"
                double sx = 0, sz = 0;
                foreach (var s in list)
                {
                    var w = map.LatLonToWorld(s.lat, s.lon);
                    sx += w.x; sz += w.z;
                }
                var avg = new Vector3((float)(sx / list.Count), 2f, (float)(sz / list.Count));
                CreateMarker("cluster:" + kv.Key, list.Count + " halte", avg, list.Count);
            }
        }
    }

    private void RebuildFromMarkers(List<SelaBridge.StopDto> dtos, bool clustered)
    {
        // Rebuild sederhana: clear lalu buat ulang dari posisi tersimpan
        var saved = new List<(string id, string name, Vector3 pos)>();
        foreach (var m in markers) saved.Add((m.id, m.name, m.worldPos));
        Clear();
        foreach (var s in saved)
            CreateMarker(s.id, s.name, s.pos, 1);
    }

    private void CreateMarker(string id, string name, Vector3 worldPos, int count)
    {
        var root = new GameObject("Stop_" + id);
        root.transform.position = worldPos;
        root.transform.SetParent(transform, false);

        // Pin: sphere pipih + collider untuk tap
        var pin = GameObject.CreatePrimitive(PrimitiveType.Sphere);
        pin.name = "Pin";
        pin.transform.SetParent(root.transform, false);
        pin.transform.localScale = new Vector3(pinSize, pinSize * 0.6f, pinSize);
        pin.transform.localPosition = new Vector3(0, pinSize * 0.3f, 0);
        var pinRend = pin.GetComponent<Renderer>();
        var pinMat = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
        pinMat.color = pinColor;
        // Depth test off agar pin selalu terlihat di atas gedung (§8: readability)
        pinMat.SetInt("_ZTest", (int)UnityEngine.Rendering.CompareFunction.Always);
        pinMat.renderQueue = 3000;
        pinRend.material = pinMat;
        pinRend.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;

        var markerRef = pin.AddComponent<MarkerRef>();
        markerRef.stopId = id.StartsWith("cluster:") ? null : id;

        // Label: TextMesh di atas pin
        var labelObj = new GameObject("Label");
        labelObj.transform.SetParent(root.transform, false);
        labelObj.transform.localPosition = new Vector3(0, labelHeight, 0);
        var textMesh = labelObj.AddComponent<TextMesh>();
        textMesh.text = count > 1 ? name : name;
        textMesh.fontSize = 48;
        textMesh.characterSize = 0.12f;
        textMesh.anchor = TextAnchor.MiddleCenter;
        textMesh.alignment = TextAlignment.Center;
        textMesh.color = Color.white;
        var labelRend = labelObj.GetComponent<Renderer>();
        var labelMat = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
        labelMat.color = new Color(0.05f, 0.05f, 0.08f, 0.75f); // background gelap
        labelMat.SetInt("_ZTest", (int)UnityEngine.Rendering.CompareFunction.Always);
        labelMat.renderQueue = 3001;
        labelRend.material = labelMat;

        var marker = new Marker
        {
            id = id,
            name = name,
            worldPos = worldPos,
            root = root,
            pin = pin,
            label = labelObj,
            selected = id == selectedId,
        };
        markers.Add(marker);
        if (!id.StartsWith("cluster:")) byId[id] = marker;
        ApplyVisual(marker);
    }

    private void ApplyVisual(Marker m)
    {
        if (m.pin == null) return;
        float size = m.selected ? pinSelectedSize : pinSize;
        m.pin.transform.localScale = new Vector3(size, size * 0.6f, size);
        var rend = m.pin.GetComponent<Renderer>();
        if (rend != null) rend.material.color = m.selected ? pinSelectedColor : pinColor;
    }

    public void Clear()
    {
        foreach (var m in markers)
            if (m.root != null) Destroy(m.root);
        markers.Clear();
        byId.Clear();
    }

    // Komponen penanda untuk raycast -> stopId
    private class MarkerRef : MonoBehaviour
    {
        public string stopId;
    }
}
