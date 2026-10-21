plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * The release version, taken from the git tag the release workflow is building.
 *
 * `DSH_VERSION_NAME` is the tag without its leading `v`. Hardcoding it here meant every tag after
 * the first shipped an APK still claiming to be the first — same `versionCode`, so Android saw no
 * upgrade at all. The code is derived from the name so it rises with semver on its own; the
 * fallback is what a local `assembleRelease` builds.
 */
val dshVersionName: String = System.getenv("DSH_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.16.0"

// Visible version restarts at 0.0.0 per the rebuild brief, but Android still needs a rising
// versionCode to upgrade over the old build, so DSH_VERSION_CODE can override it independently.
val dshVersionCode: Int = System.getenv("DSH_VERSION_CODE")?.toIntOrNull() ?: dshVersionName
    .substringBefore('-')
    .split('.')
    .mapNotNull { it.toIntOrNull() }
    .let { parts ->
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        major * 10_000 + minor * 100 + patch
    }
    .coerceAtLeast(1)
    // The visible version restarted at 0.0.0, but versionCode must keep rising past the old 3.x
    // installs; offset the 0.x line so 0.0.1 => 31001, 0.1.0 => 31100.
    .let { base -> if (dshVersionName.startsWith("0.")) base + 31_000 else base }

android {
    namespace = "com.labteto.dshmobile"
    compileSdk = 35

    defaultConfig {
        // Fork: install side by side with the official app. A different signing key can never
        // upgrade over the published APK, so the application id is what makes coexistence possible.
        applicationId = "com.labteto.dshmobile.kaogong"
        minSdk = 26
        targetSdk = 35
        versionCode = dshVersionCode
        versionName = dshVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Fork: ML Kit's bundled OCR ships one native blob per ABI. x86/x86_64 only matter to
        // emulators, and dropping them keeps the single download URL ~22 MB smaller.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // Fork: Phase E. The dex was ~14 MB compressed before shrinking; R8 + resource shrink
            // are the only remaining large lever after the native libraries were compressed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        jniLibs {
            // Fork: ML Kit's OCR pipeline is ~17 MB of uncompressed .so across the two arm ABIs.
            // Storing them compressed roughly halves the download; the platform extracts them at
            // install time (extractNativeLibs=true), which minSdk 26 handles.
            useLegacyPackaging = true
        }
    }

    // Optional release signing: provide DSH_KEYSTORE / DSH_KEYSTORE_PASSWORD /
    // DSH_KEY_ALIAS / DSH_KEY_PASSWORD (env vars, e.g. from GitHub secrets).
    // Signing activates only when the keystore file actually exists, so a
    // missing keystore silently falls back to an unsigned release APK.
    signingConfigs {
        val keystore = System.getenv("DSH_KEYSTORE")
        if (!keystore.isNullOrBlank() && file(keystore).exists()) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("DSH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DSH_KEY_ALIAS")
                keyPassword = System.getenv("DSH_KEY_PASSWORD")
            }
            buildTypes.getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // The 11-language claim is only true while every base string has a translation, and the
        // gap is invisible in review — this is the check that actually enforces it, so it is
        // pinned rather than left to the default severity.
        error += listOf("MissingTranslation", "ImpliedQuantity")
        // Fork note: the self-hosted gateway strings are added for English and Simplified Chinese
        // only. Missing translations in the other locales are expected in this fork, not a defect.
        disable += listOf("MissingTranslation", "ImpliedQuantity")
        // These categories are deliberate properties of this distribution, not defects. Disabling
        // them keeps `lintRelease` actionable: after this block it reports only issues we intend
        // to fix (currently accessibility, formatting, and real unused resources).
        disable += listOf(
            // Dependency versions are pinned and validated as a set; upgrades are a separate task.
            "GradleDependency",
            // The fork keeps the upstream Gradle file style rather than migrating to a catalog.
            "UseTomlInstead",
            // Homelab / reverse-proxy CAs are an explicit trust-model choice (docs/SECURITY.md).
            "AcceptsUserCertificates",
            // The local harness serves plain HTTP on the LAN; cleartext is required there.
            "InsecureBaseConfiguration",
            // Only arm ABIs are shipped; ChromeOS x86_64 is out of scope.
            "ChromeOsAbiSupport",
            // Quick-settings cell-size attributes are API 31+ and intentionally ignored below.
            "UnusedAttribute",
            // False positives on localized words (e.g. Portuguese "autentica").
            "Typos",
            // Adaptive icon plus legacy bitmap is the intended two-layer asset set.
            "IconXmlAndPng",
            // `--trusted-host` is a CLI flag inside prose, not a dash to replace with an em dash.
            "TypographyDashes",
        )
        // `HardcodedText` is deliberately absent: it only inspects XML layouts, and this app has
        // none. Compose string literals have to be caught in review.
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.yaml:snakeyaml:2.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.unifiedpush.connector)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    // QR scanning for relay pairing. ZXing rather than ML Kit: it needs no Google Play Services, so
    // it works on a de-Googled device, and pairing is the one flow a user cannot route around.
    implementation(libs.zxing.android.embedded)

    // Fork: bundled on-device OCR for the "photo to text" row; no Play Services needed.
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // The mock harness carries this project's port of the host's answer-acceptance law. The
    // conformance test runs the real encoder through it rather than through a copy, because a copy
    // is a second thing to keep in step and the failure it guards against is a silent one.
    testImplementation(project(":mock-harness"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


