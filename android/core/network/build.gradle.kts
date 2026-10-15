plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android {
    namespace = "id.sela.network"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
    buildFeatures { compose = false; buildConfig = true }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("javax.inject:javax.inject:1")
    implementation("com.google.dagger:hilt-android:2.52")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.maplibre.gl:android-sdk:11.5.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
}
