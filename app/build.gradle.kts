plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.cleancache"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cleancache"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {
    implementation(libs.bundles.android.base)
    
    // ДОБАВЛЕНО: Для работы с EXIF метаданными фотографий
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    
    // ДОБАВЛЕНО: Для работы с геолокацией (опционально, можно использовать стандартный LocationManager)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    testImplementation(libs.junit)
}
