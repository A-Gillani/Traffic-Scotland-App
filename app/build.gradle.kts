
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val trafficClientId = localProperties.getProperty("TRAFFIC_CLIENT_ID", "")
val trafficClientKey = localProperties.getProperty("TRAFFIC_CLIENT_KEY", "")


android {
    namespace = "org.me.gcu.gillani_muhammad_s2364257"
    compileSdk {
        version = release(37)
    }


    buildFeatures {
        buildConfig = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true


    defaultConfig {
        applicationId = "org.me.gcu.gillani_muhammad_s2364257"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"


        buildConfigField("String", "TRAFFIC_CLIENT_ID",
            "\"${trafficClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        buildConfigField("String", "TRAFFIC_CLIENT_KEY",
            "\"${trafficClientKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    testImplementation(libs.junit)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

}
