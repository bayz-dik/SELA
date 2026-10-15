// MapController.cs — Kamera + koordinat + orkestrasi scene SELA 3D.
//
// Tanggung jawab:
// - Transformasi WGS84 <-> dunia Unity (origin rebasing)
// - Mode kamera: free (gesture) dan follow (GPS)
// - Menerima data halte/rute/bus dari SelaBridge dan meneruskan ke manager
//
// Komponen scene (di-assign di Editor atau dicari otomatis):
// - JakartaSceneBuilder: bangun kota
// - BusMarkerManager: marker bus 3D
// - NightLightingController: siang/malam

using UnityEngine;

public class MapController : MonoBehaviour
{
    public static MapController Instance { get; private set; }

    [Header("Kamera")]
    public Camera mapCamera;
    public float minZoom = 10f;
    public float maxZoom = 19f;

    [Header("Komponen Scene (auto-cari jika kosong)")]
    public JakartaSceneBuilder sceneBuilder;
    public BusMarkerManager busManager;
    public NightLightingController lighting;
    public StopMarkerManager stopMarkers;
    public RouteRenderer routeRenderer;
    public GpsFilter gpsFilter;
    public QualityManager quality;

    // Origin rebasing: titik lat/lon yang dipetakan ke (0,0,0) dunia Unity.
    private double originLat = -6.2088; // Jakarta default: Monas
    private double originLon = 106.8456;
    private bool originSet = true; // default Jakarta agar preview langsung jalan

    private string cameraMode = "free"; // free | follow

    // State kamera untuk easing
    private Vector3 camTargetPos;
    private float camTargetZoom = 15f;
    private float camTargetBearing;
    private float camTargetPitch = 60f;
    private bool camAnimating;

    void Awake()
    {
        Instance = this;
        if (mapCamera == null) mapCamera = Camera.main;

        // Auto-cari komponen jika belum di-assign
        if (sceneBuilder == null) sceneBuilder = FindObjectOfType<JakartaSceneBuilder>();
        if (busManager == null) busManager = FindObjectOfType<BusMarkerManager>();
        if (lighting == null) lighting = FindObjectOfType<NightLightingController>();
        if (stopMarkers == null) stopMarkers = FindObjectOfType<StopMarkerManager>();
        if (routeRenderer == null) routeRenderer = FindObjectOfType<RouteRenderer>();
        if (gpsFilter == null) gpsFilter = FindObjectOfType<GpsFilter>();
        if (quality == null) quality = FindObjectOfType<QualityManager>();
    }

    void Start()
    {
        // Pastikan scene dibangun
        if (sceneBuilder != null && !IsSceneBuilt())
        {
            sceneBuilder.BuildPreviewDistrict();
        }
        ApplyCameraState(instant: true);
    }

    void Update()
    {
        // Easing kamera menuju target
        if (camAnimating && mapCamera != null)
        {
            mapCamera.transform.position = Vector3.Lerp(
                mapCamera.transform.position, camTargetPos, Time.deltaTime * 3f);
            var curRot = mapCamera.transform.rotation.eulerAngles;
            float newX = Mathf.LerpAngle(curRot.x, camTargetPitch, Time.deltaTime * 3f);
            float newY = Mathf.LerpAngle(curRot.y, camTargetBearing, Time.deltaTime * 3f);
            mapCamera.transform.rotation = Quaternion.Euler(newX, newY, 0);

            if (Vector3.Distance(mapCamera.transform.position, camTargetPos) < 0.5f)
                camAnimating = false;
        }
    }

    private bool IsSceneBuilt()
    {
        return GameObject.Find("Ground_Jakarta") != null;
    }

    // ---------- API dari SelaBridge ----------

    public void SetOrigin(double lat, double lon)
    {
        originLat = lat; originLon = lon; originSet = true;
    }

    public void ApplyCamera(SelaBridge.CameraMsg cam)
    {
        if (!originSet) SetOrigin(cam.lat, cam.lon);
        cameraMode = string.IsNullOrEmpty(cam.mode) ? "free" : cam.mode;

        var world = LatLonToWorld(cam.lat, cam.lon);
        float zoom = Mathf.Clamp(cam.zoom, minZoom, maxZoom);

        // Konversi zoom (level peta) ke tinggi kamera
        // zoom 19 ~ 50m, zoom 10 ~ 5000m (aproksimasi eksponensial)
        float height = Mathf.Pow(2f, 19f - zoom) * 50f;

        camTargetPos = new Vector3(world.x, height, world.z + height * 0.4f);
        camTargetZoom = zoom;
        camTargetBearing = cam.bearing;
        camTargetPitch = Mathf.Clamp(cam.pitch, 0f, 75f);
        camAnimating = true;

        // Beri tahu StopMarkerManager agar clustering mengikuti zoom baru
        if (stopMarkers != null) stopMarkers.SetZoom(zoom);

        Debug.Log($"[MapController] Kamera -> lat={cam.lat:F5} lon={cam.lon:F5} " +
                  $"zoom={zoom} bearing={cam.bearing} pitch={cam.pitch} mode={cameraMode}");
    }

    public void SetStops(SelaBridge.StopDto[] stops)
    {
        Debug.Log($"[MapController] {stops?.Length ?? 0} halte diterima");
        if (stopMarkers != null)
            stopMarkers.SetStops(stops, camTargetZoom);
    }

    public void SetRoutes(SelaBridge.RouteDto[] routes)
    {
        Debug.Log($"[MapController] {routes?.Length ?? 0} rute diterima");
        if (routeRenderer != null)
            routeRenderer.SetRoutes(routes);
    }

    public void SetBuses(SelaBridge.BusDto[] buses)
    {
        if (busManager != null)
            busManager.UpdateBuses(buses);
        else
            Debug.Log($"[MapController] {buses?.Length ?? 0} bus diterima (tanpa manager)");
    }

    public void UpdateGps(SelaBridge.GpsMsg gps)
    {
        if (cameraMode != "follow") return;

        // Filter GPS dulu (anti-jitter, anti-teleport) — spec §21
        double fLat = gps.lat, fLon = gps.lon;
        float fBearing = gps.bearing;
        if (gpsFilter != null)
        {
            var f = gpsFilter.Process(gps.lat, gps.lon, gps.accuracy,
                gps.bearing, -1f, 0f);
            if (!f.valid) return; // GPS buruk — jangan gerakkan kamera
            fLat = f.lat; fLon = f.lon; fBearing = f.bearing;
        }

        var world = LatLonToWorld(fLat, fLon);
        // Follow: kamera di atas-belakang posisi GPS
        float height = Mathf.Pow(2f, 19f - camTargetZoom) * 50f;
        camTargetPos = new Vector3(world.x, height, world.z + height * 0.4f);
        camTargetBearing = fBearing;
        camAnimating = true;
    }

    public void SetTimeOfDay(float hourJakarta)
    {
        if (lighting != null)
            lighting.SetTimeOfDay(hourJakarta);
    }

    // ---------- Kontrol kamera untuk UI ----------

    public void RecenterToGps(double lat, double lon)
    {
        var world = LatLonToWorld(lat, lon);
        float height = 300f;
        camTargetPos = new Vector3(world.x, height, world.z + height * 0.4f);
        camTargetPitch = 60f;
        camTargetBearing = 0f;
        camAnimating = true;
    }

    public void SetCameraMode(string mode)
    {
        cameraMode = mode;
        Debug.Log($"[MapController] Mode kamera: {mode}");
    }

    // ---------- Transformasi koordinat ----------

    /// <summary>
    /// WGS84 -> meter lokal terhadap origin (equirectangular, cukup untuk kota).
    /// X = timur, Z = selatan (konvensi Unity: -Z utara).
    /// </summary>
    public Vector3 LatLonToWorld(double lat, double lon)
    {
        const double R = 6371000.0;
        double dLat = Mathf.Deg2Rad * (float)(lat - originLat);
        double dLon = Mathf.Deg2Rad * (float)(lon - originLon);
        double x = R * dLon * System.Math.Cos(Mathf.Deg2Rad * (float)originLat);
        double z = -R * dLat; // utara = -Z
        return new Vector3((float)x, 0f, (float)z);
    }

    public (double lat, double lon) WorldToLatLon(Vector3 world)
    {
        const double R = 6371000.0;
        double dLat = -world.z / R;
        double dLon = world.x / (R * System.Math.Cos(Mathf.Deg2Rad * (float)originLat));
        return (originLat + dLat * Mathf.Rad2Deg, originLon + dLon * Mathf.Rad2Deg);
    }

    private void ApplyCameraState(bool instant)
    {
        if (mapCamera == null) return;
        // Posisi awal: di atas Monas, pitch 60° (signature 3D)
        camTargetPos = new Vector3(0, 800f, 320f);
        camTargetPitch = 60f;
        camTargetBearing = 0f;
        if (instant)
        {
            mapCamera.transform.position = camTargetPos;
            mapCamera.transform.rotation = Quaternion.Euler(camTargetPitch, camTargetBearing, 0);
        }
        else camAnimating = true;
    }
}
