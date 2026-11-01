plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 当前 MainActivity 用的是传统 View，不用 Compose，可以保留也可以删
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.dafeizhushou"   // ← 改成和 MainActivity 的 package 一致
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dafei"   // 已发布过就保持不变
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.4"
    }

    // 不用 Compose，可删
    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        checkReleaseBuilds = false  // 关闭 Release 构建时的 Lint 检查
        abortOnError = false        // 即使有 Lint 错误也不终止构建
    }
}

dependencies {
    // ★ 必须添加，解决所有 appcompat / AppCompatActivity 连锁错误
    implementation("androidx.appcompat:appcompat:1.7.1")  // 当前最新稳定版

    implementation("androidx.core:core-ktx:1.12.0")

    // 美恒通 SDK，保留你原来的依赖方式即可
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // Compose BOM (推荐使用最新稳定版)
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    // 基础 UI 和 Material3 设计库
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Activity Compose 集成 (用于 setContent)
    implementation("androidx.activity:activity-compose:1.8.2")

    // 可选：Compose 工具链 (用于预览等)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("com.google.code.gson:gson:2.10.1")
}

//dependencies {
//    // ★ 新增：AppCompat，解决 Unresolved reference 'appcompat'
//    implementation("androidx.appcompat:appcompat:1.6.1")
//
//    implementation("androidx.core:core-ktx:1.12.0")
//
//    // ★ 美恒通 SDK（如果 SDK 是 libs 下的 jar/aar，必须要这行，AGP 8.x 不再自动包含 libs）
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
//
//    // 下面这些当前代码完全没用，可删可留（不影响编译）
//    // implementation("androidx.activity:activity-compose:1.8.2")
//    // implementation(platform("androidx.compose:compose-bom:2024.02.00"))
//    // implementation("androidx.compose.ui:ui")
//    // implementation("androidx.compose.material3:material3")
//    // implementation("com.google.code.gson:gson:2.10.1")
//}