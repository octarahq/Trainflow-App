plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    wasmJs {
        moduleName = "trainflowApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "trainflowApp.js"
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                
                implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta02")
                implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta02")
                
                implementation("io.ktor:ktor-client-core:3.0.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("androidx.activity:activity-compose:1.9.1")
                
                implementation("org.maplibre.gl:android-sdk:11.5.0")
                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
                implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
                implementation("androidx.exifinterface:exifinterface:1.3.7")
                implementation("androidx.work:work-runtime-ktx:2.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
                
                implementation("io.ktor:ktor-client-okhttp:3.0.0")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:3.0.0")
            }
        }
    }
}

android {
    namespace = "com.octarahq.trainflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.octarahq.trainflow"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.2"
        buildConfigField("String", "BASE_URL", "\"https://apitrainflow.orionhost.app\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            storeFile = System.getenv("KEYSTORE_FILE")?.let { file(it) } ?: file("dummy.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "full"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

afterEvaluate {
    tasks.configureEach {
        if (name == "installDebug") {
            doLast {
                exec {
                    commandLine("adb", "shell", "am", "start", "-n", "com.octarahq.trainflow/.MainActivity")
                }
            }
        }
    }
}
