package com.smjcco.wxpusher.web.update

import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

object WebBundleManager {
    private const val TAG = "WebBundleManager"
    private const val BUNDLE_NAME = "web_bundle.zip"
    private const val WEB_FOLDER = "web"
    private const val TEMP_FOLDER = "web_temp"
    private const val WEB_VERSION_KEY = "web_version"
    private const val NEED_APPLY_UPDATE_KEY = "need_apply_update"

    private lateinit var webDir: File
    private lateinit var tempDir: File

    fun init() {
        val context = ApplicationUtils.application
        webDir = File(context.filesDir, WEB_FOLDER)
        tempDir = File(context.filesDir, TEMP_FOLDER)

        // 确保目录存在
        webDir.mkdirs()
        tempDir.mkdirs()

        // 首次运行时从assets解压初始网页文件
        if (!isWebFilesExists()) {
            extractAssetsBundle()
        }

        // 检查更新
        checkUpdate()
    }

    fun getWebFileDir(): File {
        Log.d(TAG, "Web目录: ${webDir.absolutePath}")
        Log.d(TAG, "Web目录是否存在: ${webDir.exists()}")
        Log.d(TAG, "Web目录文件列表: ${webDir.list()?.joinToString()}")
        return webDir
    }

    private fun isWebFilesExists(): Boolean {
        return webDir.exists() && webDir.list()?.isNotEmpty() == true
    }

    private fun extractAssetsBundle() {
        try {
            val input = ApplicationUtils.application.assets.open(BUNDLE_NAME)
            ZipInputStream(input).use { zip ->
                extractZipTo(zip, webDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解压assets中的bundle失败", e)
        }
    }

    private fun checkUpdate() {
        WxPusherUtils.getIoScopeScope().launch {
            try {
                val serverVersion = URL("${WxPusherConfig.WebUrl}/version.txt").readText()
                val localVersion = SaveUtils.getByKey(WEB_VERSION_KEY) ?: "0"
                if (serverVersion > localVersion) {
                    Log.d(TAG, "检查到新版本，开始下载,version=${serverVersion}")
                    downloadNewBundle(serverVersion)
                } else {
                    Log.d(TAG, "无新版本，跳过更新")
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败", e)
            }
        }
    }

    private fun downloadNewBundle(serverVersion: String) {
        WxPusherUtils.getIoScopeScope().launch {
            try {
                val url = URL("${WxPusherConfig.WebUrl}/web_bundle.zip")
                val connection = url.openConnection()
                // 清空临时目录
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                // 下载并解压到临时目录
                val tempFile = File(tempDir, BUNDLE_NAME)
                FileOutputStream(tempFile).use { output ->
                    connection.getInputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "新版本下载完成,version=${serverVersion}")
                ZipInputStream(tempFile.inputStream()).use { zip ->
                    extractZipTo(zip, tempDir)
                }
                Log.d(TAG, "新版本解压完成,version=${serverVersion}")
                // 标记有新版本可用，并且标记本地版本
                SaveUtils.setKeyValue(WEB_VERSION_KEY, serverVersion)
                SaveUtils.setKeyValue(NEED_APPLY_UPDATE_KEY, "true")
            } catch (e: Exception) {
                Log.e(TAG, "下载新bundle失败", e)
            }
        }
    }

    fun applyUpdateIfAvailable() {
        if (SaveUtils.getByKey(NEED_APPLY_UPDATE_KEY) == "true") {
            try {
                Log.d(TAG, "删除老版本")
                // 删除旧文件
                webDir.deleteRecursively()
                webDir.mkdirs()

                // 移动新文件
                tempDir.listFiles()?.forEach { file ->
                    file.copyRecursively(File(webDir, file.name), true)
                }

                // 清理
                tempDir.deleteRecursively()
                SaveUtils.setKeyValue(NEED_APPLY_UPDATE_KEY, "false")
                Log.d(TAG, "应用新版本完成")
            } catch (e: Exception) {
                //更新失败，重置一下版本号，下次启动会再次更新
                SaveUtils.setKeyValue(WEB_VERSION_KEY, "0")
                Log.e(TAG, "应用更新失败", e)
            }
        }
    }

    private fun extractZipTo(zip: ZipInputStream, destDir: File) {
        var entry = zip.nextEntry
        while (entry != null) {
            val file = File(destDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { output ->
                    zip.copyTo(output)
                }
                // 确保文件可读
                file.setReadable(true, false)
            }
            entry = zip.nextEntry
        }
    }
}