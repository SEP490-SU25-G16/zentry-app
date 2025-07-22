plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.fpt.zentryapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "vn.edu.fpt.zentryapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
    
    // Thêm cấu hình aaptOptions để tránh nén file tflite
    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    implementation ("com.github.ChaosLeong:PinView:1.4.3")
    implementation (libs.navigation.fragment)
    implementation (libs.navigation.ui)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Face ID dependencies - sử dụng phiên bản từ OnDevice-Face-Recognition-Android
    implementation("com.google.ai.edge.litert:litert:1.1.2")
    implementation("com.google.ai.edge.litert:litert-gpu:1.1.2")
    implementation("com.google.ai.edge.litert:litert-gpu-api:1.1.2")
    implementation("com.google.ai.edge.litert:litert-support:1.1.2")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    
    // CameraX
    val cameraxVersion = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // OkHttp3
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    
    // Retrofit2
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}