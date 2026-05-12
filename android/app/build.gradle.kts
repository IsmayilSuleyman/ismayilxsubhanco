plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Edit these two values after deploying the Apps Script web app and
// publishing the static site, then rebuild the APK.
val webAppUrl = providers.gradleProperty("WEB_APP_URL").orElse("https://script.google.com/macros/s/AKfycbzcErntGIcTldT1Y0tAksrcTFm3fZ82HtveNFUTvdAzAQMkqvGAsBZMndsGVVWuEmr-Lw/exec").get()
val sheetCsvUrl = providers.gradleProperty("SHEET_CSV_URL").orElse("https://docs.google.com/spreadsheets/d/e/2PACX-1vSVqbzTJJHPgIXlNKLg50CVSemInyOHA-yzsO2pceUVoKtmFqXSa__3j8xiGKqjo3Zb_NFePONk5a9b/pub?output=csv").get()

android {
    namespace = "com.subhanismayil.budget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.subhanismayil.budget"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "WEB_APP_URL", "\"$webAppUrl\"")
        buildConfigField("String", "SHEET_CSV_URL", "\"$sheetCsvUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
