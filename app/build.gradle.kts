plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.cascade.attentiontwin"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.cascade.attentiontwin"
        minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}

tasks.register("checkNoContentReads") {
    doLast {
        val banned = listOf("EXTRA_TITLE","EXTRA_TEXT","EXTRA_BIG_TEXT","EXTRA_MESSAGES","EXTRA_SUB_TEXT",
            "EXTRA_INFO_TEXT","EXTRA_SUMMARY_TEXT","EXTRA_TEXT_LINES","tickerText")
        val hits = fileTree("src/main").matching { include("**/*.kt","**/*.java") }.files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, l -> if (banned.any { b -> l.contains(b) }) "${f.name}:${i+1}: $l" else null }
        }
        if (hits.isNotEmpty()) throw GradleException("Notification content access found:\n" + hits.joinToString("\n"))
    }
}
tasks.named("preBuild") { dependsOn("checkNoContentReads") }
