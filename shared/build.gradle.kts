import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

// 读取local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    // 定义一个配置函数
    fun KotlinNativeTarget.iosTargetConfig() {
        compilations.getByName("main") {
            val iosShellBridge by cinterops.creating {
                defFile(project.file("src/iosMain/cinterop/KtSwiftBridge.def"))
                includeDirs(project.rootProject.file("iosApp/wxpusher/KtSwiftBridge"))
            }
        }
    }

    // 应用配置到各个目标
    iosX64() {
        iosTargetConfig()
    }
    iosArm64() {
        iosTargetConfig()
    }
    iosSimulatorArm64() {
        iosTargetConfig()
    }

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "13.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
        pod("Toaster") {
            version = "2.3.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation("com.aliyun.openservices:aliyun-log-android-sdk:2.6.13")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.smjcco.wxpusher.kmp"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        buildConfigField("String", "ALIYUN_SLS_ACCESS_KEY_ID", "\"${localProperties.getProperty("aliyun.sls.accessKeyId", "")}\"")
        buildConfigField("String", "ALIYUN_SLS_ACCESS_KEY_SECRET", "\"${localProperties.getProperty("aliyun.sls.accessKeySecret", "")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}