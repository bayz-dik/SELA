// RoadMeshBuilder.cs — Membangun mesh jalan dari segmen.
//
// Setiap segmen jalan dirender sebagai quad datar dengan:
// - Warna aspal gelap (#1A1D24)
// - Garis tengah kuning untuk arterial (marka jalan)
// - Kasing (outline) lebih terang di tepi
//
// Menggunakan satu mesh gabungan per kategori untuk draw call minimal.

using UnityEngine;
using System.Collections.Generic;

public class RoadMeshBuilder : MonoBehaviour
{
    public static RoadMeshBuilder Instance { get; private set; }

    [Header("Material")]
    public Material roadMaterial;
    public Material markingMaterial; // garis marka

    // Root hasil build terakhir — dihapus (beserta mesh-nya) sebelum rebuild
    // agar tidak dobel dan tidak bocor memori.
    private GameObject roadRoot;

    void Awake()
    {
        Instance = this;
        if (roadMaterial == null)
        {
            roadMaterial = new Material(Shader.Find("Universal Render Pipeline/Lit"));
            roadMaterial.color = new Color(0.10f, 0.11f, 0.14f, 1f); // aspal gelap
            roadMaterial.SetFloat("_Smoothness", 0.4f);
        }
        if (markingMaterial == null)
        {
            markingMaterial = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
            // Kuning volt SELA #CDF138 untuk marka arterial
            markingMaterial.color = new Color(0.80f, 0.95f, 0.22f, 1f);
        }
    }

    public void BuildRoads(List<JakartaSceneBuilder.RoadSegment> segments)
    {
        BuildRoadsInternal(segments);
    }

    /// <summary>
    /// Overload: bangun jalan dari polyline rute nyata (GTFS shapes).
    /// Setiap polyline dipecah jadi segmen-segmen.
    /// </summary>
    public void BuildRoads(List<List<Vector3>> polylines, bool isArterial)
    {
        var segments = new List<JakartaSceneBuilder.RoadSegment>();
        foreach (var line in polylines)
        {
            for (int i = 0; i + 1 < line.Count; i++)
            {
                segments.Add(new JakartaSceneBuilder.RoadSegment
                {
                    start = line[i],
                    end = line[i + 1],
                    width = isArterial ? 16f : 10f,
                    isArterial = isArterial,
                });
            }
        }
        BuildRoadsInternal(segments);
    }

    private void BuildRoadsInternal(List<JakartaSceneBuilder.RoadSegment> segments)
    {
        ClearRoads(); // hapus build lama: cegah geometri dobel + mesh leak

        var roadGo = new GameObject("Roads");
        roadGo.transform.SetParent(transform);
        roadRoot = roadGo;

        var verts = new List<Vector3>();
        var tris = new List<int>();
        var uvs = new List<Vector2>();

        var markVerts = new List<Vector3>();
        var markTris = new List<int>();

        foreach (var seg in segments)
        {
            AddRoadQuad(seg, verts, tris, uvs);
            if (seg.isArterial)
                AddCenterLine(seg, markVerts, markTris);
        }

        // Mesh jalan
        var roadMesh = new Mesh();
        roadMesh.SetVertices(verts);
        roadMesh.SetTriangles(tris, 0);
        roadMesh.SetUVs(0, uvs);
        roadMesh.RecalculateNormals();
        var rf = roadGo.AddComponent<MeshFilter>();
        rf.mesh = roadMesh;
        var rr = roadGo.AddComponent<MeshRenderer>();
        rr.material = roadMaterial;

        // Mesh marka
        if (markVerts.Count > 0)
        {
            var markGo = new GameObject("RoadMarkings");
            markGo.transform.SetParent(roadGo.transform);
            var markMesh = new Mesh();
            markMesh.SetVertices(markVerts);
            markMesh.SetTriangles(markTris, 0);
            markMesh.RecalculateNormals();
            markGo.AddComponent<MeshFilter>().mesh = markMesh;
            markGo.AddComponent<MeshRenderer>().material = markingMaterial;
        }

        Debug.Log($"[RoadMeshBuilder] {segments.Count} segmen jalan dibangun.");
    }

    /// <summary>
    /// Hapus hasil build sebelumnya beserta mesh yang dibuat
    /// (Mesh hasil `new Mesh()` tidak ikut hancur saat GameObject di-Destroy).
    /// </summary>
    public void ClearRoads()
    {
        if (roadRoot == null) return;
        foreach (var mf in roadRoot.GetComponentsInChildren<MeshFilter>())
        {
            if (mf != null && mf.sharedMesh != null)
                Destroy(mf.sharedMesh);
        }
        Destroy(roadRoot);
        roadRoot = null;
    }

    private void AddRoadQuad(JakartaSceneBuilder.RoadSegment seg,
        List<Vector3> verts, List<int> tris, List<Vector2> uvs)
    {
        Vector3 dir = (seg.end - seg.start).normalized;
        Vector3 perp = new Vector3(-dir.z, 0, dir.x) * (seg.width / 2f);
        float y = 0.02f;

        int base_idx = verts.Count;
        verts.Add(seg.start - perp + Vector3.up * y);
        verts.Add(seg.start + perp + Vector3.up * y);
        verts.Add(seg.end - perp + Vector3.up * y);
        verts.Add(seg.end + perp + Vector3.up * y);

        // Dua segitiga (winding CCW dari atas)
        tris.Add(base_idx); tris.Add(base_idx + 2); tris.Add(base_idx + 1);
        tris.Add(base_idx + 1); tris.Add(base_idx + 2); tris.Add(base_idx + 3);

        uvs.Add(new Vector2(0, 0)); uvs.Add(new Vector2(1, 0));
        uvs.Add(new Vector2(0, 1)); uvs.Add(new Vector2(1, 1));
    }

    private void AddCenterLine(JakartaSceneBuilder.RoadSegment seg,
        List<Vector3> verts, List<int> tris)
    {
        Vector3 dir = (seg.end - seg.start).normalized;
        Vector3 perp = new Vector3(-dir.z, 0, dir.x) * 0.3f; // garis 60cm
        float y = 0.035f; // sedikit di atas jalan

        // Garis putus-putus: segmen 3m, gap 3m
        float totalLen = Vector3.Distance(seg.start, seg.end);
        float dashLen = 3f, gapLen = 3f;
        float t = 0f;
        while (t < totalLen)
        {
            float tEnd = Mathf.Min(t + dashLen, totalLen);
            Vector3 p0 = seg.start + dir * t;
            Vector3 p1 = seg.start + dir * tEnd;

            int b = verts.Count;
            verts.Add(p0 - perp + Vector3.up * y);
            verts.Add(p0 + perp + Vector3.up * y);
            verts.Add(p1 - perp + Vector3.up * y);
            verts.Add(p1 + perp + Vector3.up * y);
            tris.Add(b); tris.Add(b + 2); tris.Add(b + 1);
            tris.Add(b + 1); tris.Add(b + 2); tris.Add(b + 3);

            t += dashLen + gapLen;
        }
    }
}
