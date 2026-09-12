plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android.plugin)
    alias(libs.plugins.kotlin.symbol.processing)
}

android {
    namespace = "com.app.mykios"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.mykios"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "edition"
    productFlavors {
        create("free") {
            dimension = "edition"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            buildConfigField("boolean", "MYKIOS_PRO_EDITION", "false")
        }
        create("pro") {
            dimension = "edition"
            buildConfigField("boolean", "MYKIOS_PRO_EDITION", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// KSP tracing note:
// Room schema export is temporarily disabled here to isolate the
// JsonDecodingException/EOF failure seen during kspFreeReleaseKotlin.
// The committed v5 schema remains in app/schemas and will be re-enabled
// after the processor itself is proven healthy.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ZXing for Barcodes
    implementation(libs.zxingCore)
    implementation(libs.zxingAndroidEmbedded)

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // WorkManager for Notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Lottie Animations
    implementation(libs.lottie)

    // Excel Support (Apache POI)
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Google Drive & Auth
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.api-client:google-api-client-android:1.32.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
