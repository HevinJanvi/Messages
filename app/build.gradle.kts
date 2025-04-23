plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlin-android")
    id("com.google.firebase.crashlytics")
}

android {
    signingConfigs {
        create("release") {
            storeFile = file("D:\\JanviAndroidProjects\\Messages\\app\\demo_jks.jks")
            storePassword = "demo_jks"
            keyAlias = "demo_jks"
            keyPassword = "demo_jks"
        }
    }
    namespace = "com.test.messages.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.test.messages.demo"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable =true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    viewBinding {
        enable = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.google.android.material:material:1.10.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("io.github.cymchad:BaseRecyclerViewAdapterHelper4:4.1.0")

    implementation("com.google.dagger:hilt-android:2.46")
    kapt("com.google.dagger:hilt-compiler:2.46")

//    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("com.klinkerapps:android-smsmms:5.2.6")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.51")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.makeramen:roundedimageview:2.3.0")

    implementation ("androidx.room:room-ktx:2.6.1")
    kapt ("androidx.room:room-compiler:2.6.1")

    implementation ("com.google.code.gson:gson:2.11.0")
    implementation("org.greenrobot:eventbus:3.1.1")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
//    implementation ("android.arch.lifecycle:extensions:1.1.1")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation ("it.xabaras.android:recyclerview-swipedecorator:1.4")

    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

//    implementation ("com.intuit.sdp:sdp-android:1.1.0")
//    implementation ("com.intuit.ssp:ssp-android:1.1.0")



}