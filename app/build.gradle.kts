plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.antoniegil.astronia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.antoniegil.astronia"
        minSdk = 24
        targetSdk = 36
        versionCode = 102
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }
    
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (signingConfigs.findByName("release")?.storeFile?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["appAuthRedirectScheme"] = "com.antoniegil.astronia"
            ndk {
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
            }
        }
        debug {
            if (signingConfigs.findByName("release")?.storeFile?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Astronia Debug")
            manifestPlaceholders["appAuthRedirectScheme"] = "com.antoniegil.astronia.debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        jvmToolchain(17)
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs.useLegacyPackaging = true
    }
    
    androidResources { 
        generateLocaleConfig = true 
    }
    
    bundle {
        language {
            enableSplit = false
        }
    }
    
    lint { 
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation", "MissingQuantity", "ChromeOsAbiSupport"))
        abortOnError = false
    }
    
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName = output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier ?: "universal"
                val baseVersionCode = output.versionCode.orNull ?: 0
                output.versionCode.set(baseVersionCode * 100 + when(abiName) {
                    "arm64-v8a" -> 4
                    "armeabi-v7a" -> 3
                    "x86_64" -> 2
                    "x86" -> 1
                    "universal" -> 0
                    else -> 0
                })
            }
        }
    }
    
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Astronia-${defaultConfig.versionName}-${name}.apk"
        }
    }
}

dependencies {
    // Core
    implementation(libs.bundles.core)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidxCompose)
    
    // Accompanist
    implementation(libs.bundles.accompanist)
    
    // Image loading
    implementation(libs.coil.kt.compose)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    
    // Network
    implementation(libs.okhttp)
    
    // Storage
    implementation(libs.mmkv)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // DateTime
    implementation(libs.kotlinx.datetime)
    
    implementation(libs.bundles.media3)

    
    // Testing
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary)
}
