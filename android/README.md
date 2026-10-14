# SELA Android — Phase 4

Native Kotlin / Jetpack Compose / Material 3 / MapLibre. No WebView.

## Build

Toolchain: JDK 21, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26, build-tools 34.0.0.

In this sandbox:

    cd /home/hatch/workspace/sela/apps/android
    bash scripts/local-gradle.sh test
    bash scripts/local-gradle.sh :app:assembleDebug

The launcher sets JDK 21, SDK, Gradle cache and IPv4 for child JVMs, then runs Gradle 8.9. Start python3 scripts/artifact-bridge.py separately if port 8766 is unavailable. scripts/build-local.sh is an optional bridge bootstrapper and delegates to this same launcher. Downloads are HTTPS curl requests to official Google Maven, Maven Central, and Gradle Plugin Portal; proxy credentials remain in the environment, not the project.

Do not invoke ./gradlew directly in this sandbox; use scripts/local-gradle.sh for every Gradle task.

Outside this sandbox use JDK 21, install SDK 35/build-tools 34.0.0, set sdk.dir in local.properties, and use ./gradlew normally. Without SELA_MAVEN_BRIDGE, repositories use their normal HTTPS origins.

SDK manager fallback for this proxy environment:

    python scripts/sdk-proxy.py
    # separate terminal, after source scripts/local-env.sh:
    yes | sdkmanager --sdk_root="$ANDROID_HOME" --no_https --proxy=http --proxy_host=127.0.0.1 --proxy_port=8767 --licenses
    sdkmanager --sdk_root="$ANDROID_HOME" --no_https --proxy=http --proxy_host=127.0.0.1 --proxy_port=8767 'platforms;android-35' 'build-tools;34.0.0'

--no_https applies only to the local SDK-manager hop; the proxy fetches the official upstream using verified HTTPS through curl.

## Configuration

Default backend: http://10.0.2.2:8080 (Android emulator's host). Override at build time with -Psela.apiBaseUrl=https://your-server or through Pengaturan. Settings are stored in DataStore and apply to REST and the next WebSocket session; the activity restarts its foreground realtime session when the endpoint changes. A physical device needs a reachable backend address, not 10.0.2.2.

Cleartext HTTP is enabled for the explicit development backend requirement. Choose HTTPS for production and tighten cleartext policy before distributing a production release. No signing keys or credentials are included.

## Modules

- app: Hilt Application, MainActivity, SelaScaffold, NavHost, manifest and theme.
- core:model: domain entities, repository/use-case interface, monotonic vehicle reconciler.
- core:database: Room atomic snapshots, metadata, favorites.
- core:network: Retrofit DTOs/pagination, WebSocket, offline-first repository, DataStore, Hilt bindings.
- core:designsystem: theme, semantic palette, spacing, radii and elevation.
- core:location: one cancellable foreground location request.
- feature:map: StateFlow ViewModel, native MapLibre map, controls and sheets.
- feature:search: locale-stable local search.
- feature:settings: endpoint preference editor and privacy/data explanations.

core:common is unnecessary; the domain is shared through core:model. Empty historical feature directories are not Gradle modules; route/stop/vehicle detail functionality lives in feature:map and has not been removed.

## Data behavior

Room cache is emitted before network refresh. Every page is validated before static snapshot commit; inconsistent versions do not replace the cache. Snapshot metadata includes datasetVersion and sync time. Initial REST snapshot finishes before realtime subscription; acknowledgements/reconnects request another vehicle snapshot, with periodic polling fallback. Each socket subscribes to at most 20 routes, preserving coverage across all routes. Ack alone is not treated as proof of a live upstream. Vehicles stay ephemeral; stale status begins at 90 seconds, and stale vehicles are excluded from markers/list.

Backend serviceType can be null. The DTO keeps its existing BUS fallback; displayServiceType infers the same code categories as the web map without presenting that inference as source metadata. BRT red, JAK blue, other buses lime. Missing route geometry, schedules and arrivals are explicitly unavailable, never synthesized. OpenFreeMap tiles require network; offline support covers transit snapshots/favorites, not guaranteed offline basemap tiles.

Route/stop/vehicle selection and camera coordinates are saved via SavedStateHandle. Location permission is requested only after the user taps Lokasi saya and confirms the rationale. No background permission/service, location upload, or continuous tracking.

## Verification scope

Unit tests cover reconciler ordering/staleness, backend wire DTOs and pagination, offline/online repository behavior, cancellation, initial REST-before-WS ordering, ack refetch, real Retrofit/OkHttp transports against MockWebServer, and ViewModel selection/filter/camera restoration. See PROGRESS_ANDROID.md at the repository root for final executed results.

Device/emulator smoke checks for Phase 5: launch, pinch/pan map, select and restore route on rotation, allow/deny location, revoke permissions, font scaling 200%, TalkBack lists, airplane-mode cached search, endpoint editing, and real upstream reconnect. A successful unit-test/APK build is not a claim that these device checks or live MQTT have been exercised.
