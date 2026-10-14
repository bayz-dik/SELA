// SelaCiBuild.cs — Build entry point untuk CI (GitHub Actions + game-ci).
//
// Dipakai sebagai `buildMethod` di workflow:
//   with:
//     buildMethod: SelaCiBuild.BuildAndroidLibrary
//
// Alur:
//   1. Regenerasi scene Jakarta3D via SceneBootstrapper (selalu sinkron dengan script)
//   2. Export unityLibrary via UalExportBuilder
//
// Ini lebih robust daripada mengandalkan scene .unity yang di-commit,
// karena scene selalu dibangun ulang dari kode terbaru.

using UnityEngine;
using UnityEditor;

public static class SelaCiBuild
{
    public static void BuildAndroidLibrary()
    {
        Debug.Log("[SelaCiBuild] Regenerasi scene Jakarta3D...");
        try
        {
            SceneBootstrapper.BuildScene();
        }
        catch (System.Exception e)
        {
            Debug.LogError("[SelaCiBuild] SceneBootstrapper gagal: " + e);
            EditorApplication.Exit(1);
            return;
        }

        Debug.Log("[SelaCiBuild] Scene OK. Lanjut export unityLibrary...");
        UalExportBuilder.ExportAndroidLibrary();
    }
}
