plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

/**
 * Standalone, self-instrumenting E2E harness that drives the whole device with
 * UiAutomator (Metrix tracking link -> Cafebazaar / web -> install ->
 * open the referrer test app -> assert the referrer shown on screen).
 *
 * It is intentionally a separate module from `:app`: the referrer-attribution
 * flow requires a *fresh* install of `com.farsitel.bazaar.bazaarInstallReferrerTest`
 * from Bazaar, and an instrumentation test that targets that same package can not
 * uninstall/reinstall its own app-under-test. This module targets its own package
 * instead, so it can freely uninstall, install and inspect the referrer app.
 */
android {
    namespace = "com.farsitel.bazaar.referrere2e"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.farsitel.bazaar.referrere2e"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.uiautomator)
}
