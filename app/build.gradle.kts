plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.firebase-perf")
    id("com.google.firebase.crashlytics")
}

android {
    signingConfigs {
        create("release") {
        }
    }
    namespace = "com.chat.sms_text.messages"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chat.sms_text.messages"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resConfigs(
            "en",
            "ar",
            "da",
            "de",
            "es",
            "et",
            "fi",
            "fr",
            "hi",
            "id",
            "it",
            "ja",
            "ko",
            "nl",
            "no",
            "pt",
            "ru",
            "sv",
            "sw",
            "ta",
            "te",
            "tl",
            "tr",
            "vi",
            "zh"
        )
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    kapt {
        correctErrorTypes = true
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":kalendarview"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.lottie)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)
    implementation(libs.library)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.emoji.google)
    implementation(libs.glide.transformations)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.firebase.database)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.perf)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.shimmer)
    implementation(libs.fastscroll)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.startup.runtime)
}