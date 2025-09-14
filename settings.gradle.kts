rootProject.name = "WxPusher-App"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        //华为推送
        maven {
            setUrl("https://developer.huawei.com/repo/")
        }
        //荣耀推送
        maven {
            setUrl("https://developer.hihonor.com/repo/")
        }
    }

}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }



        mavenCentral()

        //华为推送
        maven {
            setUrl("https://developer.huawei.com/repo/")
        }
        //荣耀推送
        maven {
            setUrl("https://developer.hihonor.com/repo/")
        }

        maven {
            setUrl("https://tencent-tds-maven.pkg.coding.net/repository/shiply/repo")
        }
    }
}

//include(":androidApp")
include(":androidAppV2")
include(":shared")