// UalExportBuilder.cs — Export Unity as a Library via batch mode.
//
// Cara pakai (di mesin dengan Unity Editor):
//   Unity -batchmode -nographics -projectPath <path> \
//     -executeMethod UalExportBuilder.ExportAndroidLibrary \
//     -logFile ual-export.log -quit
//
// Menghasilkan modul Gradle `unityLibrary` di folder yang ditentukan
// via argumen -exportPath (default: ../unity-export).
//
// Berdasarkan riset: EditorUserBuildSettings.exportAsGoogleAndroidProject = true
// adalah kunci untuk UAL (bukan APK).

using UnityEngine;
using UnityEditor;
using UnityEditor.Build.Reporting;
using System.IO;
using System.Linq;

public static class UalExportBuilder
{
    public static void ExportAndroidLibrary()
    {
        string exportPath = GetArg("-exportPath");
        if (string.IsNullOrEmpty(exportPath))
        {
            // Default: ../unity-export relatif dari project
            exportPath = Path.GetFullPath(Path.Combine(Application.dataPath, "..", "..", "unity-export"));
        }
        Debug.Log($"[UalExport] Target export: {exportPath}");

        // 1. Switch ke platform Android
        Debug.Log("[UalExport] Switch build target ke Android...");
        if (!EditorUserBuildSettings.SwitchActiveBuildTarget(
            BuildTargetGroup.Android, BuildTarget.Android))
        {
            Fail("Gagal switch ke Android. Pastikan modul Android terinstall.");
            return;
        }

        // 2. Konfigurasi UAL
        Debug.Log("[UalExport] Konfigurasi Unity as a Library...");
        EditorUserBuildSettings.exportAsGoogleAndroidProject = true;
        EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;

        // Player settings
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures =
            AndroidArchitecture.ARM64; // ARM64 saja (hemat ukuran)
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
        PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
        // Package name bebas — bukan launcher (SELA hapus via manifest merge)
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "id.sela.unity3d");
        PlayerSettings.companyName = "SELA";
        PlayerSettings.productName = "SELA 3D";

        // Graphics API: OpenGL ES 3 (kompatibilitas maksimal Android)
        PlayerSettings.SetGraphicsAPIs(BuildTarget.Android,
            new[] { UnityEngine.Rendering.GraphicsDeviceType.OpenGLES3 });

        // 3. Pastikan scene utama ada di build
        var scenes = EditorBuildSettings.scenes;
        if (scenes.Length == 0 || !scenes.Any(s => s.enabled))
        {
            // Cari scene Jakarta3D
            string[] guids = AssetDatabase.FindAssets("t:Scene Jakarta3D");
            if (guids.Length == 0)
            {
                Fail("Scene Jakarta3D.unity tidak ditemukan di Assets/Scenes/.");
                return;
            }
            string scenePath = AssetDatabase.GUIDToAssetPath(guids[0]);
            EditorBuildSettings.scenes = new[]
            {
                new EditorBuildSettingsScene(scenePath, true)
            };
            Debug.Log($"[UalExport] Scene ditambahkan: {scenePath}");
        }

        // 4. Validasi script penting ada
        ValidateScript("SelaBridge");
        ValidateScript("MapController");
        ValidateScript("JakartaSceneBuilder");

        // 5. Build
        Directory.CreateDirectory(exportPath);
        var options = new BuildPlayerOptions
        {
            scenes = EditorBuildSettings.scenes
                .Where(s => s.enabled).Select(s => s.path).ToArray(),
            locationPathName = exportPath,
            target = BuildTarget.Android,
            targetGroup = BuildTargetGroup.Android,
            options = BuildOptions.None,
        };

        Debug.Log("[UalExport] Memulai build...");
        BuildReport report = BuildPipeline.BuildPlayer(options);
        BuildSummary summary = report.summary;

        if (summary.result == BuildResult.Succeeded)
        {
            Debug.Log($"[UalExport] SUKSES. unityLibrary ada di: {exportPath}/unityLibrary");
            Debug.Log($"[UalExport] Ukuran: {summary.totalSize / 1024 / 1024} MB");
            EditorApplication.Exit(0);
        }
        else
        {
            Fail($"Build gagal: {summary.result}. Cek log untuk detail.");
        }
    }

    private static void ValidateScript(string className)
    {
        // Cari via reflection — pastikan class tercompile
        var type = System.AppDomain.CurrentDomain.GetAssemblies()
            .SelectMany(a => { try { return a.GetTypes(); } catch { return new System.Type[0]; } })
            .FirstOrDefault(t => t.Name == className);
        if (type == null)
            Debug.LogWarning($"[UalExport] Class {className} tidak ditemukan (mungkin belum compile).");
        else
            Debug.Log($"[UalExport] OK: {className}");
    }

    private static string GetArg(string name)
    {
        string[] args = System.Environment.GetCommandLineArgs();
        for (int i = 0; i < args.Length - 1; i++)
            if (args[i] == name) return args[i + 1];
        return null;
    }

    private static void Fail(string message)
    {
        Debug.LogError("[UalExport] GAGAL: " + message);
        EditorApplication.Exit(1);
    }
}
