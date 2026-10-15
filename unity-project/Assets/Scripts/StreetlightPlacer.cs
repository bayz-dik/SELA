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
    private Camera cachedMainCam;

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
        }
        // Setup additive blending via kode: glow harus transparan-additive,
        // bukan kotak kuning opaque. Berlaku juga untuk material dari Inspector.
        SetupAdditiveGlow(glowMaterial);
    }

    /// <summary>
    /// Ubah material glow menjadi additive: SrcAlpha + One, tanpa Z-write,
    /// render queue transparan. Tanpa ini glow tampil sebagai quad kuning solid.
    /// </summary>
    private static void SetupAdditiveGlow(Material mat)
    {
        if (mat == null) return;
        mat.SetFloat("_Surface", 1f); // URP: 0=opaque, 1=transparent
        mat.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
        mat.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.One);
        mat.SetInt("_ZWrite", 0);
        mat.DisableKeyword("_ALPHATEST_ON");
        mat.DisableKeyword("_ALPHAPREMULTIPLY_ON");
        mat.renderQueue = (int)UnityEngine.Rendering.RenderQueue.Transparent;
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
        // sharedMaterial: .material akan menduplikasi material per lampu (2500+ instance).
        pole.GetComponent<Renderer>().sharedMaterial = poleMaterial;

        // Kepala lampu (kapsul horizontal)
        var head = GameObject.CreatePrimitive(PrimitiveType.Cube);
        head.name = "Head";
        head.transform.SetParent(go.transform);
        head.transform.localPosition = new Vector3(0, poleHeight, 0);
        head.transform.localScale = new Vector3(1.2f, 0.25f, 0.5f);
        head.GetComponent<Renderer>().sharedMaterial = glowMaterial;

        // Glow billboard (selalu menghadap kamera)
        var glow = GameObject.CreatePrimitive(PrimitiveType.Quad);
        glow.name = "Glow";
        glow.transform.SetParent(go.transform);
        glow.transform.localPosition = new Vector3(0, poleHeight - 0.3f, 0);
        glow.transform.localScale = new Vector3(4f, 4f, 1f);
        var gr = glow.GetComponent<Renderer>();
        gr.sharedMaterial = glowMaterial;
        // Hapus collider yang tidak perlu.
        // NB: pakai GetComponent(string) agar kompilasi tidak butuh modul Physics.
        Destroy(glow.GetComponent("Collider"));

        // Billboard glow: didaftarkan ke BillboardGlowManager yang mengupdate
        // SEMUA glow dalam satu loop per frame (bukan 2500+ LateUpdate individu).
        var bb = glow.AddComponent<BillboardGlow>();
        if (cachedMainCam == null) cachedMainCam = Camera.main; // cache sekali
        bb.targetCamera = cachedMainCam;
        BillboardGlowManager.Ensure(gameObject);

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

/// <summary>
/// Penanda quad glow agar dirotasi menghadap kamera oleh BillboardGlowManager.
/// Mendaftarkan diri ke daftar statis; TIDAK punya LateUpdate sendiri
/// (sebelumnya 2500+ LateUpdate individual per frame).
/// </summary>
public class BillboardGlow : MonoBehaviour
{
    public Camera targetCamera;

    internal static readonly System.Collections.Generic.List<BillboardGlow> all =
        new System.Collections.Generic.List<BillboardGlow>();

    void OnEnable()
    {
        if (!all.Contains(this)) all.Add(this);
    }

    void OnDisable()
    {
        all.Remove(this);
    }
}

/// <summary>
/// Satu-satunya LateUpdate untuk semua glow: iterasi daftar BillboardGlow
/// dalam satu loop, kamera di-cache sekali per frame.
/// </summary>
public class BillboardGlowManager : MonoBehaviour
{
    private Camera cachedCam;

    /// <summary>Pastikan satu manager ada di GameObject induk.</summary>
    public static void Ensure(GameObject host)
    {
        if (host.GetComponent<BillboardGlowManager>() == null)
            host.AddComponent<BillboardGlowManager>();
    }

    void LateUpdate()
    {
        if (BillboardGlow.all.Count == 0) return;
        if (cachedCam == null) cachedCam = Camera.main;
        if (cachedCam == null) return;

        var camRot = cachedCam.transform.rotation;
        var camFwd = camRot * Vector3.forward;
        var camUp = camRot * Vector3.up;

        foreach (var b in BillboardGlow.all)
        {
            if (b == null || !b.isActiveAndEnabled) continue;
            var t = b.transform;
            if (b.targetCamera != null && b.targetCamera != cachedCam)
            {
                // Glow dengan kamera target eksplisit yang berbeda
                var r = b.targetCamera.transform.rotation;
                t.LookAt(t.position + r * Vector3.forward, r * Vector3.up);
            }
            else
            {
                t.LookAt(t.position + camFwd, camUp);
            }
        }
    }
}
