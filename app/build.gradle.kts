plugins {
    id("com.android.application") version "8.3.0"
    id("org.jetbrains.kotlin.android") version "1.9.20"
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // ДОБАВЛЕНО: Для работы с EXIF метаданными фотографий
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    
    testImplementation("junit:junit:4.13.2")
}
