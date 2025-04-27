import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(compose.preview)
            implementation(libs.androidx.core.ktx)
            implementation("androidx.work:work-runtime-ktx:2.10.0")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.google.code.gson:gson:2.10.1")
            implementation("androidx.fragment:fragment:1.7.0")
            implementation("androidx.appcompat:appcompat:1.7.0")
//            implementation("androidx.webkit:webkit:1.9.0")
            implementation("com.google.android.material:material:1.12.0")



            //腾讯shiply https://shiply.tds.qq.com/docs/doc?id=4008331373
            implementation("com.tencent.shiply:upgrade:2.2.1-RC01"){
                exclude(group="androidx.appcompat", module = "appcompat")
                exclude(group="androidx.fragment", module = "fragment")
            }
            implementation("com.tencent.shiply:upgrade-ui:2.2.1-RC01"){
                exclude(group="com.tencent.shiply", module = "upgrade")
            }

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(projects.shared)
        }
    }
}

android {
    namespace = "com.smjcco.wxpusher"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.smjcco.wxpusher"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.0.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // true - 打开混淆
            isMinifyEnabled = true
            // true - 打开资源压缩
            isShrinkResources = true
            // 指定ProGuard规则文件
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 定义flavor维度
    flavorDimensions.add("env")

    // 配置产品风格
    productFlavors {
        create("offline") {
            dimension = "env"
//            applicationIdSuffix = ".test"
            versionNameSuffix = ".test"
        }
        create("prod") {
            dimension = "env"
        }
    }

    sourceSets {
        getByName("offline") {
            manifest.srcFile("src/androidOffline/AndroidManifest.xml")
        }
    }
}

dependencies {
    implementation(fileTree("libs") {
        include("*.jar")
        include("*.aar")
    })
    implementation(libs.androidx.core)
    debugImplementation(compose.uiTooling)
}

