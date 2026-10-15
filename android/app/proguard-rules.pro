# SELA ProGuard rules
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep Retrofit interfaces
-keep interface * { *; }

# Keep MapLibre
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Keep Gson-parsed GTFS model classes (field names must match JSON keys)
-keep class id.sela.network.GtfsJsonParser { *; }
-keep class id.sela.network.RawRoute { *; }
-keep class id.sela.network.RawStop { *; }
-keepclassmembers class id.sela.network.RawRoute { *; }
-keepclassmembers class id.sela.network.RawStop { *; }
# Gson generic type tokens
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# Suppress error_prone annotations warnings (compile-time only)
-dontwarn javax.lang.model.element.Modifier
-dontwarn com.google.errorprone.annotations.**

# Keep Paho MQTT (uses reflection for WebSocket)
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.**

# Rhino JavaScript (NewPipe signature deciphering) — java.beans tidak dipakai di Android
-dontwarn java.beans.**
-dontwarn javax.swing.**
-dontwarn org.mozilla.javascript.tools.**

# Jsoup — jspecify annotations tidak dibutuhkan saat runtime
-dontwarn org.jspecify.annotations.**

# InnerTube — jangan obfuscate model serialization
-keep class moe.rukamori.archivetune.innertube.** { *; }

# Media3 session
-keep class androidx.media3.session.** { *; }
