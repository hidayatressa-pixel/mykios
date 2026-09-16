plugins {
    alias(libs.plugins.android.application)
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
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    buildFeatures { buildConfig = true }

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
}

// AGP 9 provides built-in Kotlin support. Do not apply org.jetbrains.kotlin.android
// or use android.kotlinOptions; doing so registers the Kotlin extension twice.

// Room schema export is intentionally disabled until the existing v5 schema
// and processor state are regenerated cleanly. Normal Room code generation stays enabled.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.zxingCore)
    implementation(libs.zxingAndroidEmbedded)

    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.lottie)

    implementation("org.apache.poi:poi-ooxml:5.2.5")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.api-client:google-api-client-android:1.32.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
