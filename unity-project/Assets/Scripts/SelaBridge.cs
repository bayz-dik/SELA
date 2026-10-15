// SelaBridge.cs — Jembatan pesan Unity ↔ Kotlin (SELA 3D Ultra V4.0).
//
// Tempelkan script ini pada GameObject bernama "SelaBridge" di scene utama.
// Kotlin memanggil method-method di bawah via UnityPlayer.UnitySendMessage.
// Balasan ke Kotlin via reflection ke id.sela.feature.unity.UnityBridge
// static methods (nativeOnUnityReady / nativeOnStopSelected / nativeOnCameraChanged).
// Reflection murni: TANPA referensi compile-time ke modul Android JNI
// (modul tsb tidak terinstall di semua environment build, mis. CI game-ci).

using UnityEngine;

public class SelaBridge : MonoBehaviour
{
    private bool readySent;

    void Start()
    {
        // Beri tahu Kotlin bahwa engine selesai boot.
        if (Application.platform == RuntimePlatform.Android)
        {
            CallKotlinStatic("nativeOnUnityReady");
            readySent = true;
        }
        else
        {
            Debug.Log("[SelaBridge] Berjalan di Editor — mode standalone.");
        }
    }

    // ---------- Kotlin -> Unity (via UnitySendMessage) ----------

    // {"lat":..,"lon":..,"zoom":..,"bearing":..,"pitch":..,"mode":"free|follow"}
    public void SetCamera(string json)
    {
        var cam = JsonUtility.FromJson<CameraMsg>(json);
        MapController.Instance?.ApplyCamera(cam);
    }

    // {"stops":[{id,name,lat,lon}]}
    public void SetStops(string json)
    {
        var data = JsonUtility.FromJson<StopsMsg>(json);
        MapController.Instance?.SetStops(data.stops);
    }

    // {"routes":[{id,color,points:[{lat,lon},...]}]}
    // NB: points memakai array objek (bukan array-of-array) karena
    // JsonUtility tidak mendukung jagged array (double[][]).
    public void SetRoutes(string json)
    {
        var data = JsonUtility.FromJson<RoutesMsg>(json);
        MapController.Instance?.SetRoutes(data.routes);
    }

    // {"buses":[{id,routeId,lat,lon,heading}]}
    public void SetBuses(string json)
    {
        var data = JsonUtility.FromJson<BusesMsg>(json);
        MapController.Instance?.SetBuses(data.buses);
    }

    // {"lat":..,"lon":..,"accuracy":..,"bearing":..}
    public void SetGps(string json)
    {
        var gps = JsonUtility.FromJson<GpsMsg>(json);
        MapController.Instance?.UpdateGps(gps);
    }

    // {"hour":..} — jam lokal Asia/Jakarta (0-24)
    public void SetTimeOfDay(string json)
    {
        var t = JsonUtility.FromJson<TimeMsg>(json);
        MapController.Instance?.SetTimeOfDay(t.hour);
    }

    // {"tier":"low|medium|high|ultra|auto"} — quality tier
    public void SetQualityTier(string json)
    {
        var t = JsonUtility.FromJson<TierMsg>(json);
        var qm = QualityManager.Instance;
        if (qm == null) qm = FindObjectOfType<QualityManager>();
        qm?.SetTierFromString(t?.tier ?? "auto");
    }

    // {"stopId":"..."} — pilih halte (dari Kotlin, mis. tap di list)
    public void SelectStop(string json)
    {
        var s = JsonUtility.FromJson<SelectMsg>(json);
        MapController.Instance?.stopMarkers?.SelectStop(s?.id);
    }

    // {"routeId":"..."} — pilih rute
    public void SelectRoute(string json)
    {
        var s = JsonUtility.FromJson<SelectMsg>(json);
        MapController.Instance?.routeRenderer?.SelectRoute(s?.id);
    }

    // ---------- Unity -> Kotlin ----------

    public void NotifyStopSelected(string stopId)
    {
        CallKotlin("nativeOnStopSelected", stopId);
    }

    public void NotifyCameraChanged(double lat, double lon, float zoom, float bearing, float pitch)
    {
        if (Application.platform != RuntimePlatform.Android) return;
        CallKotlinStatic("nativeOnCameraChanged", lat, lon, zoom, bearing, pitch);
    }

    private void CallKotlin(string method, string arg)
    {
        if (Application.platform != RuntimePlatform.Android) return;
        CallKotlinStatic(method, arg);
    }

    // Panggil static method Kotlin via reflection murni.
    //
    // Tidak ada referensi compile-time ke AndroidJavaClass / AndroidJavaObject:
    // modul Android JNI tidak terinstall di semua environment build (mis. CI),
    // jadi type di-resolve saat runtime saja. Di luar Android (atau jika modul
    // tidak ada), GetType -> null dan pemanggilan di-skip dengan aman.
    private void CallKotlinStatic(string method, params object[] args)
    {
        try
        {
            var ajcType = System.Type.GetType(
                "UnityEngine.AndroidJavaClass, UnityEngine.AndroidJNIModule");
            if (ajcType == null)
            {
                Debug.LogWarning("[SelaBridge] Modul Android JNI tidak tersedia; lewati " + method + ".");
                return;
            }
            var cls = System.Activator.CreateInstance(
                ajcType, new object[] { "id.sela.feature.unity.UnityBridge" });
            try
            {
                var callStatic = ajcType.GetMethod(
                    "CallStatic", new System.Type[] { typeof(string), typeof(object[]) });
                if (callStatic == null)
                {
                    Debug.LogWarning("[SelaBridge] CallStatic tidak ditemukan.");
                    return;
                }
                callStatic.Invoke(cls, new object[] { method, args });
            }
            finally
            {
                (cls as System.IDisposable)?.Dispose();
            }
        }
        catch (System.Exception e)
        {
            Debug.LogWarning("[SelaBridge] " + method + " gagal: " + e.Message);
        }
    }

    // ---------- DTO (harus cocok dengan JSON dari Kotlin) ----------

    [System.Serializable] public class CameraMsg
    { public double lat; public double lon; public float zoom; public float bearing; public float pitch; public string mode; }

    [System.Serializable] public class StopDto
    { public string id; public string name; public double lat; public double lon; }
    [System.Serializable] public class StopsMsg { public StopDto[] stops; }

    [System.Serializable] public class RoutePointDto
    { public double lat; public double lon; }
    [System.Serializable] public class RouteDto
    { public string id; public string color; public RoutePointDto[] points; }
    [System.Serializable] public class RoutesMsg { public RouteDto[] routes; }

    [System.Serializable] public class BusDto
    { public string id; public string routeId; public double lat; public double lon; public float heading; }
    [System.Serializable] public class BusesMsg { public BusDto[] buses; }

    [System.Serializable] public class GpsMsg
    { public double lat; public double lon; public float accuracy; public float bearing; }

    [System.Serializable] public class TimeMsg { public float hour; }

    [System.Serializable] public class TierMsg { public string tier; }

    [System.Serializable] public class SelectMsg { public string id; }
}
