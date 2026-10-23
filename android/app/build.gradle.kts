import java.util.Properties
import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releasePropsFile = rootProject.file("keys/release.properties")
val releaseProps = Properties().apply {
    if (releasePropsFile.exists()) releasePropsFile.inputStream().use(::load)
}

// Firebase is enabled as soon as this app has its own config downloaded from Firebase Console.
// Keep debug builds usable before then; the Qvacell google-services.json belongs to another app.
if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
    // Disabled pending delivery from Cuba without VPN; see ../ARCHITECTURE.md.
    // apply(plugin = "com.google.firebase.crashlytics")
    // apply(plugin = "com.google.firebase.firebase-perf")
}

android {
    namespace = "com.llamacon99"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.llamacon99"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseProps.getProperty("storeFile", "keys/release.jks"))
            storePassword = releaseProps.getProperty("storePassword")
            keyAlias = releaseProps.getProperty("keyAlias", "llamacon99")
            keyPassword = releaseProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releasePropsFile.exists()) {
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

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

android.applicationVariants.all {
    val variant = this
    outputs.all {
        val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
        val suffix = if (variant.buildType.name == "release") "" else "-debug"
        output?.outputFileName = "llamacon99-v${variant.versionName}$suffix.apk"
    }
}

abstract class NameBundleTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputBundle: RegularFileProperty

    @get:OutputFile
    abstract val outputBundle: RegularFileProperty

    @TaskAction
    fun copyBundle() {
        val destination = outputBundle.get().asFile
        destination.parentFile.mkdirs()
        inputBundle.get().asFile.copyTo(destination, overwrite = true)
    }
}

// APK outputFileName does not apply to App Bundles. Transform the final bundle so
// Gradle and Android Studio both see the versioned file, preserving its signature.
androidComponents {
    onVariants { variant ->
        val suffix = if (variant.buildType == "release") "" else "-${variant.name}"
        val version = variant.outputs.single().versionName.get()
        val nameBundle = tasks.register<NameBundleTask>("name${variant.name.replaceFirstChar { it.uppercaseChar() }}Bundle")
        variant.artifacts.use(nameBundle)
            .wiredWithFiles(NameBundleTask::inputBundle, NameBundleTask::outputBundle)
            .withName("llamacon99-v$version$suffix.aab")
            .toTransform(SingleArtifact.BUNDLE)
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // Disabled pending delivery from Cuba without VPN; see ../ARCHITECTURE.md.
    // implementation(libs.firebase.crashlytics)
    // implementation(libs.firebase.perf)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
