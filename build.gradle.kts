// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jetbrains.kotlin.kapt") version "2.3.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}