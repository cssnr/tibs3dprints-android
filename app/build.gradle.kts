plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "org.cssnr.tibs3dprints"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.cssnr.tibs3dprints"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["firebaseAnalyticsDeactivated"] = false // enabled
        manifestPlaceholders["firebaseCrashlyticsEnabled"] = true // enabled
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            //applicationIdSuffix = ".dev"
            //versionNameSuffix = "-dev"
            manifestPlaceholders["firebaseAnalyticsDeactivated"] = true // disabled
            manifestPlaceholders["firebaseCrashlyticsEnabled"] = false // disabled
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.glide)
    implementation(libs.okhttp.integration)
    implementation(libs.rssparser)
    //implementation(libs.androidyoutubeplayer)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.glide.compiler)
    ksp(libs.moshi.kotlin.codegen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

//afterEvaluate {
//    if (project.hasProperty("android.injected.signing.store.file")) {
//        println("store.file: ${project.property("android.injected.signing.store.file")}")
//    }
//    if (project.hasProperty("android.injected.signing.store.password")) {
//        println("store.password: ${project.property("android.injected.signing.store.password")}")
//    }
//    if (project.hasProperty("android.injected.signing.key.alias")) {
//        println("key.alias: ${project.property("android.injected.signing.key.alias")}")
//    }
//    if (project.hasProperty("android.injected.signing.key.password")) {
//        println("key.password: ${project.property("android.injected.signing.key.password")}")
//    }
//}
