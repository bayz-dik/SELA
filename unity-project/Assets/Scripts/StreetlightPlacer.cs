// StreetlightPlacer.cs — Lampu jalan di sepanjang koridor.
//
// Prinsip performa (sesuai spec V4 §19.3):
// - Dekat kamera: mesh tiang + glow sprite
// - Jauh: emissive sprite saja (tanpa mesh tiang)
// - TIDAK ada real-time point light per lampu (terlalu mahal)
// - Penempatan deterministik (seeded) — stabil antar sesi

using UnityEngine;
using System.Collections.Generic;

public class StreetlightPlacer : MonoBehaviour
{
    [Header("Config")]
    public float spacingArterial = 40f;   // tiap 40m di jalan arterial
    public float spacingLocal = 60f;
    public float poleHeight = 9f;

    [Header("Material")]
    public Material poleMaterial;
    public Material glowMaterial; // additive, untuk sprite cahaya

    private List<GameObject> lights = new List<GameObject>();

    public void PlaceAlongRoads(List<JakartaSceneBuilder.RoadSegment> roads, System.Random rng)
    {
        var polylines = new List<List<Vector3>>();
        foreach (var road in roads)
            polylines.Add(new List<Vector3> { road.start, road.end });
        PlaceAlongPolylines(polylines, rng, defaultIsArterial: false);
    }

    /// <summary>
    /// Tempatkan lampu di sepanjang polyline rute nyata.
    /// </summary>
    public void PlaceAlongPolylines(List<List<Vector3>> polylines,
        System.Random rng, bool defaultIsArterial = true)
    {
        EnsureMaterials();
        int count = 0;
        foreach (var line in polylines)
        {
            float spacing = defaultIsArterial ? spacingArterial : spacingLocal;
            for (int i = 0; i + 1 < line.Count; i++)
            {
                float len = Vector3.Distance(line[i], line[i + 1]);
                int n = Mathf.Max(1, Mathf.FloorToInt(len / spacing));
                for (int k = 0; k <= n; k++)
                {
                    var pos = Vector3.Lerp(line[i], line[i + 1], (float)k / n);
                    // Sisi jalan bergantian
                    var dir = (line[i + 1] - line[i]).normalized;
                    var side = new Vector3(-dir.z, 0, dir.x);
                    pos += side * ((count % 2 == 0) ? 8f : -8f);
                    pos.y = 0f;
                    PlaceSingle(pos, rng);
                    count++;
                }
            }
        }
        Debug.Log($"[StreetlightPlacer] {count} lampu di polyline");
    }

    private void EnsureMaterials()
    {
        if (poleMaterial == null)
        {
            poleMaterial = new Material(Shader.Find("Universal Render Pipeline/Lit"));
            poleMaterial.color = new Color(0.15f, 0.15f, 0.17f);
        }
        if (glowMaterial == null)
        {
            glowMaterial = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
            glowMaterial.color = new Color(1f, 0.8f, 0.45f, 1f);
            // NOTE: untuk additive blending yang benar, set via URP material:
            // surface type = transparent, blending = additive.
            // Dilakukan di Editor; di sini fallback ke opaque warm.
        }
    }

    private void PlaceSingle(Vector3 pos, System.Random rng)
    {
        int idx = lights.Count;
        CreateLamp(pos, ref idx);
    }

    private void CreateLamp(Vector3 pos, ref int count)
    {
        var go = new GameObject($"Lamp_{count}");

        // Tiang
        var pole = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
        pole.name = "Pole";
        pole.transform.SetParent(go.transform);
        pole.transform.localPosition = new Vector3(0, poleHeight / 2, 0);
        pole.transform.localScale = new Vector3(0.25f, poleHeight, 0.25f);
        pole.GetComponent<Renderer>().material = poleMaterial;

        // Kepala lampu (kapsul horizontal)
        var head = GameObject.CreatePrimitive(PrimitiveType.Cube);
        head.name = "Head";
        head.transform.SetParent(go.transform);
        head.transform.localPosition = new Vector3(0, poleHeight, 0);
        head.transform.localScale = new Vector3(1.2f, 0.25f, 0.5f);
        head.GetComponent<Renderer>().material = glowMaterial;

        // Glow billboard (selalu menghadap kamera)
        var glow = GameObject.CreatePrimitive(PrimitiveType.Quad);
        glow.name = "Glow";
        glow.transform.SetParent(go.transform);
        glow.transform.localPosition = new Vector3(0, poleHeight - 0.3f, 0);
        glow.transform.localScale = new Vector3(4f, 4f, 1f);
        var gr = glow.GetComponent<Renderer>();
        gr.material = glowMaterial;
        // Hapus collider yang tidak perlu
        Destroy(glow.GetComponent<Collider>());

        // Billboard behavior via script ringan
        var bb = glow.AddComponent<BillboardGlow>();
        bb.targetCamera = Camera.main;

        go.transform.position = pos;
        lights.Add(go);
        count++;
    }

    public void Clear()
    {
        foreach (var l in lights)
            if (l != null) Destroy(l);
        lights.Clear();
    }

    /// <summary>
    /// Atur densitas lampu (0-1). Dipanggil QualityManager.
    /// Implementasi: sembunyikan sebagian lampu secara deterministik.
    /// </summary>
    public void SetDensity(float density)
    {
        density = Mathf.Clamp01(density);
        for (int i = 0; i < lights.Count; i++)
        {
            if (lights[i] == null) continue;
            // Pola deterministik: tampilkan jika hash index < density
            float h = (i * 0.6180339887f) % 1f;
            lights[i].SetActive(h < density);
        }
        Debug.Log($"[StreetlightPlacer] Densitas {density:P0} ({lights.Count} lampu)");
    }
}

/// <summary>Quad glow selalu menghadap kamera.</summary>
public class BillboardGlow : MonoBehaviour
{
    public Camera targetCamera;
    void LateUpdate()
    {
        if (targetCamera == null) targetCamera = Camera.main;
        if (targetCamera == null) return;
        transform.LookAt(transform.position + targetCamera.transform.rotation * Vector3.forward,
            targetCamera.transform.rotation * Vector3.up);
    }
}
