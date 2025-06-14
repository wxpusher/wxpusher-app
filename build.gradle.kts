buildscript {
    dependencies {

        classpath("com.android.tools.build:gradle:8.5.2")
        //荣耀AGP插件
        classpath("com.hihonor.mcs:asplugin:2.0.1.300")
        //华为AGP插件
        classpath("com.huawei.agconnect:agcp:1.9.1.301")
    }
}
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
//    alias(libs.plugins.androidApplication) apply false
//    alias(libs.plugins.androidLibrary) apply false
//    alias(libs.plugins.composeMultiplatform) apply false
//    alias(libs.plugins.composeCompiler) apply false
//    alias(libs.plugins.kotlinMultiplatform) apply false

    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}