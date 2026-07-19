plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.farsitel.bazaar.bazaarInstallReferrerTest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.farsitel.bazaar.bazaarInstallReferrerTest"
        minSdk = 21
        targetSdk = 34
        versionCode = 16
        versionName = "1.6-referrer"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose =  true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.5"
    }

    signingConfigs {
        create("SigningConfig") {
            keyAlias = "key0"
            storeFile = rootProject.file("app/bazaarUpdater.jks")
            keyPassword = "testtest"
            storePassword = "testtest"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs["SigningConfig"]
            isMinifyEnabled = false
        }
        debug {
            signingConfig = signingConfigs["SigningConfig"]
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(project(":updater"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.bazaar.referrer.sdk)
}