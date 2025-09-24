plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    // iOS targets for full multiplatform support
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/commonMain/kotlin")
            
            dependencies {
                // Core coroutines and serialization
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.uuid)
                
                // Networking with Ktor
                implementation(libs.bundles.ktor.client)
                
                // Database
                implementation(libs.bundles.sqldelight)
                
                // Dependency injection
                implementation(libs.koin.core)
                
                // Glass morphism and blur effects
                implementation(libs.haze)
                
            }
        }
        
        val androidMain by getting {
            
            dependencies {
                // Platform-specific database driver
                implementation(libs.sqldelight.android.driver)
                
                // Android-specific HTTP client
                implementation(libs.ktor.client.android)
                implementation(libs.ktor.client.auth)
                
                // AndroidX Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.viewmodel)
                
                // Security libraries for Android Keystore and encryption
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("androidx.security:security-identity-credential:1.0.0-alpha03")
                
                // AI and ML libraries - updated versions for better compatibility
                implementation(libs.bundles.ai.ml)
                implementation(libs.tensorflow.lite.gpu)
                implementation(libs.tensorflow.lite.support)
                
                // Firebase AI and Vertex AI for Gemini integration
                implementation(libs.firebase.vertexai)
                implementation(libs.generativeai)
                
                // AWS SDK for cloud services (Android only)
                implementation(libs.aws.sdk.cognitoidentityprovider)
                implementation(libs.aws.sdk.s3)
                
                // Image processing utilities
                implementation(libs.exifinterface)
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                // Android-specific test dependencies that support mockk
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.robolectric)
            }
        }
        
        val androidInstrumentedTest by getting {
            dependencies {
                // Android instrumentation test dependencies
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:rules:1.5.0")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            
            dependencies {
                // Platform-specific database driver
                implementation(libs.sqldelight.native.driver)
                
                // iOS-specific HTTP client
                implementation(libs.ktor.client.darwin)
            }
        }
        
        val commonTest by getting {
            dependencies {
                // Use only KMP-compatible test dependencies
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.hazardhawk.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

sqldelight {
    databases {
        create("HazardHawkDatabase") {
            packageName.set("com.hazardhawk.database")
            srcDirs.from("src/commonMain/sqldelight")
        }
    }
}