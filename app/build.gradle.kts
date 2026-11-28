import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)               // genera el código de Room
    alias(libs.plugins.google.services)   // Actividad 4: lee app/google-services.json (Firebase)
}

// ---------------------------------------------------------------------------
// Archivos de configuración local (NO se suben al repositorio, ver .gitignore)
// ---------------------------------------------------------------------------
fun leerPropiedades(nombre: String) = Properties().apply {
    val archivo = rootProject.file(nombre)
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// Actividad 3: servicio REST alternativo (servidor simulado). Ver api.properties.example
val apiProps = leerPropiedades("api.properties")

// Actividad 4: credenciales de firma. Ver keystore.properties.example
val firmaProps = leerPropiedades("keystore.properties")
val hayFirma = firmaProps.getProperty("storeFile") != null

android {
    namespace = "com.example.recordatorios"
    compileSdk = 35

    defaultConfig {
        // Actividad 4: id propio (com.example no se acepta para distribuir)
        applicationId = "com.amemarqui.recordatorios"
        minSdk = 26
        targetSdk = 35

        // Versión candidata para testers: subir versionCode en CADA entrega
        versionCode = 4
        versionName = "4.0.0-rc1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL",
            "\"${apiProps.getProperty("api.baseUrl", "https://servidor-simulado.local/api/")}\"")
        buildConfigField("boolean", "USAR_SERVIDOR_SIMULADO",
            apiProps.getProperty("api.usarServidorSimulado", "true"))
        // Fuente remota de la sincronización: "firestore" (Actividad 4) o "rest" (Actividad 3)
        buildConfigField("String", "FUENTE_REMOTA", "\"firestore\"")
    }

    // Firma de la versión release: las contraseñas se leen de keystore.properties
    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = rootProject.file(firmaProps.getProperty("storeFile"))
                storePassword = firmaProps.getProperty("storePassword")
                keyAlias = firmaProps.getProperty("keyAlias")
                keyPassword = firmaProps.getProperty("keyPassword")
            }
        }
    }

    // Ambientes separados de forma estable (no hay que tocar nada antes de compilar)
    flavorDimensions += "ambiente"
    productFlavors {
        create("dev") {
            dimension = "ambiente"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Recordatorios DEV")
            buildConfigField("String", "AMBIENTE", "\"desarrollo\"")
        }
        create("prod") {
            dimension = "ambiente"
            resValue("string", "app_name", "Recordatorios")
            buildConfigField("String", "AMBIENTE", "\"produccion\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    // Actividad 2: ViewModel, StateFlow con ciclo de vida y navegación
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Actividad 3: persistencia local (Room + DataStore) y servicio REST (Retrofit + OkHttp)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Actividad 4: Firebase (Authentication + Cloud Firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)   // .await() sobre las Task de Firebase

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
