plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.canorecoapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.canorecoapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.places)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    val nav_version = "2.7.2"
    implementation ("com.itextpdf:itext7-core:7.1.15")
    implementation (platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
    implementation ("androidx.fragment:fragment-ktx:1.8.1")
    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    //circular
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.27")
    // dependency for circular ImageView
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    //karumi to access other component of mobile
    implementation ("com.karumi:dexter:6.2.2")
    implementation ("com.itextpdf:kernel:7.1.15")
    implementation ("com.itextpdf:layout:7.1.15")
    //glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")
    //stepview
    implementation ("com.github.shuhart:stepview:1.5.1")

    //coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    //Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.6.2")
    implementation ("com.squareup.retrofit2:converter-gson:2.6.0")

    implementation ("pub.devrel:easypermissions:3.0.0")


    implementation( "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    implementation ("com.sun.mail:javax.mail:1.6.2")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("com.google.code.gson:gson:2.8.8")


    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.firebase:firebase-messaging-ktx:23.3.1")

    implementation ("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation ("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation ("com.paytm.appinvokesdk:appinvokesdk:1.6.12") // Updated version

    implementation ("com.facebook.shimmer:shimmer:0.1.0@aar")
    implementation ("com.github.f0ris.sweetalert:library:1.6.2")

}