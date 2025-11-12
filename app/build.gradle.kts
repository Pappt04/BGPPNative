import com.android.build.api.dsl.Packaging
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android.buildFeatures.buildConfig = true


android {
    namespace = "st.misa.bgpp_native"
    compileSdk = 36

    defaultConfig {
        applicationId = "st.misa.bgpp_native"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val thunderforestKey = localProperties.getProperty("thunderforest.apiKey") ?: ""
        buildConfigField("String", "THUNDERFOREST_API_KEY", "\"$thunderforestKey\"")

        val geoapifyKey = localProperties.getProperty("geoapify.apiKey") ?: ""
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            packaging {
                jniLibs {
                    useLegacyPackaging = false
                }
                resources {
                    excludes += listOf(
                        "META-INF/*.kotlin_module",
                        "**/*.txt",
                        "**/*.md",
                        "**/*.so.debug",
                        "**/*.sym",
                        "**/*.json",
                        // native stuff
                        "META-INF/native/**",
                        "org/sqlite/native/**",
                        "org/sqlite/native/Windows/**",
                        "org/sqlite/native/Mac/**",
                        "org/sqlite/native/Linux/**",
                        "**/*.dll",
                        "**/*.dylib",
                        "**/*.jnilib"
                    )
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    // Ensure unwanted ABIs aren't included
    fun Packaging.() {
        jniLibs {
            excludes += listOf("**/x86/**", "**/x86_64/**")
        }
    }

    // Optional per-ABI APKs (good if you’re not using AAB)
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {

    implementation("org.jetbrains:annotations:23.0.0")
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
    }


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.destinations.core)
    implementation(libs.compose.destinations.animations)



    implementation(libs.bundles.koin)

    implementation(libs.bundles.ktor)

    implementation(libs.bundles.room)
    implementation(libs.material3)
    ksp(libs.androidx.room.compiler)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.maplibreAndroid)
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)





}
