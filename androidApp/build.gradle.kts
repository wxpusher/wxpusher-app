import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
//    alias(libs.plugins.huawei.agconnect)
    //华为推送
    id("com.huawei.agconnect")
    //荣耀推送
    id("com.hihonor.mcs.asplugin")

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
            implementation(libs.androidx.core.ktx)
            implementation("androidx.work:work-runtime-ktx:2.10.0")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.google.code.gson:gson:2.10.1")
            implementation("androidx.fragment:fragment:1.7.0")
            implementation("androidx.appcompat:appcompat:1.7.0")
//            implementation("androidx.webkit:webkit:1.9.0")
            implementation("com.google.android.material:material:1.12.0")
            implementation("commons-codec:commons-codec:1.6")

            //腾讯shiply https://shiply.tds.qq.com/docs/doc?id=4008331373
            implementation("com.tencent.shiply:upgrade:2.2.1-RC01") {
                exclude(group = "androidx.appcompat", module = "appcompat")
                exclude(group = "androidx.fragment", module = "fragment")
            }
            implementation("com.tencent.shiply:upgrade-ui:2.2.1-RC01") {
                exclude(group = "com.tencent.shiply", module = "upgrade")
            }
            //华为推送
            implementation(libs.huawei.push)
            implementation("com.hihonor.mcs:push:8.0.12.307")
            //SmartRefreshLayout下拉刷新
            implementation("io.github.scwang90:refresh-layout-kernel:2.1.0")
            implementation("io.github.scwang90:refresh-header-classics:2.1.0")
            implementation("io.github.scwang90:refresh-footer-classics:2.1.0")
        }
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
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
        versionCode = 10520
        versionName = "1.5.20"
        //指定产物名称
        setProperty("archivesBaseName", "wxpusher-app-v$versionName")

        ndk {
            // 只保留ARM架构，去掉x86和x86_64，减小包大小
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 签名配置必须在buildTypes之前定义
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.jks")
            storePassword = "smjcco"
            keyAlias = "smjcco"
            keyPassword = "smjcco"
        }
        // Release 签名配置 - 从 secrets/android/key.properties 读取
        val secretsDir = rootProject.file("secrets/android")
        val keyPropertiesFile = File(secretsDir, "key.properties")
        if (keyPropertiesFile.exists()) {
            create("release") {
                val keyProperties = Properties()
                keyProperties.load(keyPropertiesFile.inputStream())
                storeFile = File(secretsDir, keyProperties.getProperty("storeFile"))
                storePassword = keyProperties.getProperty("storePassword")
                keyAlias = keyProperties.getProperty("keyAlias")
                keyPassword = keyProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            // true - 打开混淆
            isMinifyEnabled = true
            // true - 打开资源压缩
            isShrinkResources = true
            // 指定ProGuard规则文件
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 使用 release 签名配置（如果存在）
            signingConfig = try {
                signingConfigs.getByName("release")
            } catch (_: UnknownDomainObjectException) {
                null
            }
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
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
//    implementation(libs.androidx.core)
    // AndroidX, The Basics
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.preference:preference-ktx:1.2.0")
    //SmartRefreshLayout
    implementation("io.github.scwang90:refresh-layout-kernel:3.0.0-alpha")
    implementation("io.github.scwang90:refresh-header-classics:3.0.0-alpha")

    implementation("com.google.zxing:core:3.3.3")
    //微信sdk
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.34")

}

