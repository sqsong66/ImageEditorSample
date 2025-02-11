plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.kotlinParcelize)
}

android {
    namespace = "com.example.customviewsample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.customviewsample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":nativelib"))
    implementation(project(":OpenGLLib"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":renderscript-toolkit"))
    implementation(project(":jnilibrary"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // Gson
    implementation(libs.gson)

    implementation(libs.mlkit.segmentation)
    implementation(libs.removebg)

    // Svg
    implementation(libs.svg)
}