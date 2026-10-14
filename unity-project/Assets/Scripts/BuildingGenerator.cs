// BuildingGenerator.cs — Gedung extrude prosedural dengan jendela malam.
//
// Setiap gedung:
// - Footprint kotak dengan variasi ukuran/rotasi (seeded, deterministik)
// - Tinggi dari distribusi realistis Jakarta (ruko 2-4 lantai, apartemen 20-40, office 10-30)
// - Jendela emissive via shader (bukan point light per jendela — hemat GPU)
// - Pola jendela menyala deterministik per gedung (hash dari posisi)
//
// Performance: satu material shared + GPU instancing untuk jendela.

using UnityEngine;
using System.Collections.Generic;

public class BuildingGenerator : MonoBehaviour
{
    [Header("Material")]
    public Material buildingMaterial; // URP/Lit, akan di-setup otomatis jika null

    [Header("Distribusi tinggi (meter)")]
    public float shophouseChance = 0.45f;   // ruko 8-15m
    public float apartmentChance = 0.30f;   // apartemen 60-120m
    public float officeChance = 0.25f;      // kantor 40-100m

    private List<GameObject> buildings = new List<GameObject>();

    /// <summary>
    /// Generate gedung di blok-blok dalam radius. Dipanggil sekali.
    /// </summary>
    public void GenerateDistrict(System.Random rng, float radius)
    {
        if (buildingMaterial == null)
        {
            buildingMaterial = new Material(Shader.Find("Universal Render Pipeline/Lit"));
            buildingMaterial.color = new Color(0.08f, 0.09f, 0.12f, 1f);
            buildingMaterial.SetFloat("_Smoothness", 0.3f);
        }

        float blockSize = 300f; // sama dengan spacing jalan
        float roadHalf = 12f;   // setengah lebar jalan arterial

        int count = 0;
        for (float bx = -radius; bx < radius; bx += blockSize)
        {
            for (float bz = -radius; bz < radius; bz += blockSize)
            {
                // Lewati blok yang terlalu dekat ke jalan arterial (tengah)
                if (Mathf.Abs(bx + blockSize / 2) < 60f && Mathf.Abs(bz + blockSize / 2) < 60f)
                    continue;

                GenerateBlock(rng, bx + roadHalf, bz + roadHalf,
                    blockSize - roadHalf * 2, ref count);
            }
        }
        Debug.Log($"[BuildingGenerator] {count} gedung dibuat.");
    }

    /// <summary>
    /// Tandai area di sekitar halte sebagai zona komersial:
    /// gedung di sekitarnya cenderung lebih tinggi (ruko/kantor).
    /// Dipanggil JakartaSceneBuilder.BuildFromRealData.
    /// </summary>
    public void MarkCommercialAnchor(Vector3 worldPos, float radius)
    {
        commercialAnchors.Add((worldPos, radius));
    }

    private readonly List<(Vector3 pos, float radius)> commercialAnchors =
        new List<(Vector3 pos, float radius)>();

    /// <summary>
    /// Cek apakah posisi dekat anchor komersial (untuk boost tinggi gedung).
    /// </summary>
    private bool NearCommercial(Vector3 pos)
    {
        foreach (var a in commercialAnchors)
        {
            float dx = pos.x - a.pos.x, dz = pos.z - a.pos.z;
            if (dx * dx + dz * dz < a.radius * a.radius) return true;
        }
        return false;
    }

    private void GenerateBlock(System.Random rng, float x0, float z0, float size, ref int count)
    {
        // 2-5 gedung per blok
        int n = rng.Next(2, 6);
        for (int i = 0; i < n; i++)
        {
            float w = (float)(rng.NextDouble() * 30 + 15);  // 15-45m
            float d = (float)(rng.NextDouble() * 30 + 15);
            float x = x0 + (float)rng.NextDouble() * (size - w);
            float z = z0 + (float)rng.NextDouble() * (size - d);

            float h = PickHeight(rng);
            // Boost komersial: di dekat halte, gedung cenderung lebih tinggi
            if (NearCommercial(new Vector3(x + w / 2, 0, z + d / 2)))
                h = Mathf.Max(h, 25f + (float)rng.NextDouble() * 40f);
            CreateBuilding(x, z, w, d, h, rng, ref count);
        }
    }

    private float PickHeight(System.Random rng)
    {
        double r = rng.NextDouble();
        if (r < shophouseChance)
            return (float)(rng.NextDouble() * 7 + 8);      // 8-15m
        if (r < shophouseChance + apartmentChance)
            return (float)(rng.NextDouble() * 60 + 60);    // 60-120m
        return (float)(rng.NextDouble() * 60 + 40);        // 40-100m
    }

    private void CreateBuilding(float x, float z, float w, float d, float h,
        System.Random rng, ref int count)
    {
        var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
        go.name = $"Building_{count}";
        go.transform.position = new Vector3(x + w / 2, h / 2, z + d / 2);
        go.transform.localScale = new Vector3(w, h, d);
        go.GetComponent<Renderer>().material = buildingMaterial;

        // Jendela malam: tambahkan emissive via material property block
        // (deterministik dari hash posisi — pola stabil antar sesi)
        int hash = (x.GetHashCode() * 397) ^ z.GetHashCode();
        var blockRng = new System.Random(hash);
        float litRatio = (float)(blockRng.NextDouble() * 0.5 + 0.2); // 20-70% menyala

        var mpb = new MaterialPropertyBlock();
        // Encode lit ratio ke emission — shader custom bisa pakai ini.
        // Untuk URP/Lit standar, kita tint emission color.
        mpb.SetColor("_EmissionColor", new Color(1.0f, 0.75f, 0.35f) * litRatio * 0.6f);
        go.GetComponent<Renderer>().SetPropertyBlock(mpb);

        // Simpan metadata untuk LOD / window detail di masa depan
        var meta = go.AddComponent<BuildingMeta>();
        meta.height = h;
        meta.litRatio = litRatio;
        meta.seed = hash;

        buildings.Add(go);
        count++;
    }

    /// <summary>Hapus semua gedung (untuk rebuild).</summary>
    public void Clear()
    {
        foreach (var b in buildings)
            if (b != null) Destroy(b);
        buildings.Clear();
    }
}

/// <summary>Metadata per gedung untuk LOD dan detail jendela.</summary>
public class BuildingMeta : MonoBehaviour
{
    public float height;
    public float litRatio;
    public int seed;
}
