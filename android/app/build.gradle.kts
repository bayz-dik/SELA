plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android {
    namespace = "id.sela"
    compileSdk = 35
    defaultConfig { minSdk = 26; targetSdk = 35; applicationId = "id.sela"; versionCode = 1; versionName = "0.6.0" }
    signingConfigs {
        create("release") {
            storeFile = file("sela-release.keystore")
            storePassword = providers.gradleProperty("sela.keystore.password").getOrElse("")
            keyAlias = providers.gradleProperty("sela.key.alias").getOrElse("sela")
            keyPassword = providers.gradleProperty("sela.key.password").getOrElse("")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
    buildFeatures { compose = true; buildConfig = true }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:map"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:unity"))
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("javax.inject:javax.inject:1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("com.google.dagger:hilt-android:2.52")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    // Tanya Sela: ChatGPT OAuth (Custom Tabs) + penyimpanan token terenkripsi
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.security:security-crypto:1.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.json:json:20240303")
}
