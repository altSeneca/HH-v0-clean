plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.hazardhawk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hazardhawk"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.hazardhawk.com\"")
            buildConfigField("boolean", "DEBUG_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_MONITORING", "true")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.hazardhawk.com\"")
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_MONITORING", "true")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    
    // Simplified build configuration - only debug and release variants
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xsuppress-version-warnings"
        )
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude AI models from APK to reduce size - download dynamically
            excludes += "assets/models/*.tflite"
            excludes += "assets/models/*.onnx"
            excludes += "assets/models/*.pt"
            excludes += "assets/*.tflite"
            excludes += "assets/*.onnx"
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }
}

koverReport {
    filters {
        excludes {
            classes("*BuildConfig", "*.R", "*.R$*", "*.*Test*", "*.test.*", "android.*")
            annotatedBy("androidx.compose.runtime.Composable")
        }
    }
    defaults {
        html {
            onCheck = true
        }
        xml {
            onCheck = true
        }
    }
}

dependencies {
    // Shared module dependency - contains database models and business logic
    implementation(project(":shared"))
    
    // Compose BOM ensures all Compose libraries use compatible versions
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.compose.activity)
    
    // AndroidX core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.navigation.compose)
    
    // CameraX dependencies
    implementation(libs.bundles.camerax)
    
    // Image loading and processing - Coil 3.0
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.exifinterface)
    
    // UI utilities
    implementation(libs.permissions)
    implementation(libs.zoomable)
    implementation(libs.haze) // Glass morphism effects
    implementation(libs.haze.materials) // Materials API for glass effects
    
    // Location services
    implementation(libs.play.services.location)

    // AR functionality - ARCore for construction safety monitoring
    implementation("com.google.ar:core:1.45.0")  // Updated to latest stable version
    // implementation("com.google.ar.sceneform:core:1.17.1")  // Skip Sceneform to avoid conflicts
    
    // Background work and file operations
    implementation(libs.work.runtime)
    implementation(libs.documentfile)
    
    // Data handling and serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Secure storage - EncryptedSharedPreferences
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.datetime)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)
    
    // PDF generation
    implementation(libs.itext.core)
    implementation(libs.itext.html2pdf)
    
    // QR Code generation
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)
    
    // Security
    implementation(libs.security.crypto)
    implementation(libs.security.crypto.ktx)
    
    // Performance monitoring
    implementation(libs.compose.runtime.tracing)
    
    // Local AI inference (temporarily disabled for APK size)
    // implementation(libs.onnxruntime.android)
    
    // TensorFlow Lite for on-device AI processing (models downloaded post-install)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support)
    
    // Note: LiteRT dependencies not yet publicly available - will use TensorFlow Lite
    
    // Cloud-based AI integration (Gemini Vision)
    implementation(libs.firebase.vertexai)
    implementation(libs.generativeai)
    
    // Dependency injection
    implementation(libs.bundles.koin)
    
    // Testing dependencies
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    
    // Android instrumented testing
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.benchmark.junit4)
    
    // Debug implementations
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}