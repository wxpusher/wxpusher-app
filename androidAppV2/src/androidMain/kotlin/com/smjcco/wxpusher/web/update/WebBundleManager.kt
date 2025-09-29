package com.smjcco.wxpusher.web.update

import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object WebBundleManager {
    private const val TAG = "WebBundleManager"

    //web数据的bundle文件
    private const val BUNDLE_NAME = "web_bundle.zip"

    //webview浏览器访问的目录
    private const val WEB_FOLDER = "web"

    //更新数据的临时目录
    private const val TEMP_FOLDER = "web_update_temp"

    //存放版本号的文件名
    private const val VERSION_FILE = "version.txt"

    //是否需要apply更新的标记，主要用于网络更新bundle以后，在下一次启动应用更新（从web_temp复制到web）
    private const val NEED_APPLY_UPDATE_KEY = "need_apply_update"

    //正式的工作目录
    private lateinit var webDir: File

    //升级的时候的临时目录
    private lateinit var tempDir: File

    fun init() {
        val context = ApplicationUtils.getApplication()
        webDir = File(context.filesDir, WEB_FOLDER)
        tempDir = File(context.filesDir, TEMP_FOLDER)

        // 确保目录存在
        webDir.mkdirs()
        tempDir.mkdirs()
        //检查内置包是否更新（用于host APP升级以后，可能需要释放内置包）
        checkIfNeedUnzipInsetZip()

        // 检查更新
        checkUpdate()
    }

    /**
     * 检查是否需要释放内置包
     */
    private fun checkIfNeedUnzipInsetZip() {
        val nowVersion = getNowVersion()
        val insetZipVersion =
            getVersionFromZip(ApplicationUtils.getApplication().assets.open(BUNDLE_NAME))

        if (isVersionGreater(insetZipVersion, nowVersion)) {
            WxPusherLog.i(TAG, "内置包更新，需要重新解压")
            ApplicationUtils.getApplication().assets.open(BUNDLE_NAME).use {
                extractZipTempDir(it)
                applyUpdateIfAvailable()
            }
        }
    }

    private fun isVersionGreater(newVersion: String, oldVersion: String): Boolean {
        val newParts = newVersion.split(".").map { it.toInt() }
        val oldParts = oldVersion.split(".").map { it.toInt() }

        for (i in 0 until Math.max(newParts.size, oldParts.size)) {
            val newPart = newParts.getOrNull(i) ?: 0
            val oldPart = oldParts.getOrNull(i) ?: 0
            if (newPart > oldPart) return true
            if (newPart < oldPart) return false
        }
        return false
    }

    /**
     * 读取zip的版本号
     */
    private fun getVersionFromZip(input: InputStream): String {
        var version = "0"
        ZipInputStream(input).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == VERSION_FILE) {
                    version = zipInputStream.bufferedReader().readText().trim()
                    break
                }
                entry = zipInputStream.nextEntry
            }
        }
        return version
    }

    /**
     * 获取当前正在使用的版本
     */
    private fun getNowVersion(): String {
        try {
            val text = File(webDir, VERSION_FILE).readText()
            if (text.isEmpty()) {
                return "0.0.0"
            }
            return text
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return "0.0.0"
    }

    fun getWebFileDir(): File {
        WxPusherLog.i(TAG, "Web目录: ${webDir.absolutePath}")
        WxPusherLog.i(TAG, "Web目录是否存在: ${webDir.exists()}")
        WxPusherLog.i(TAG, "Web目录文件列表: ${webDir.list()?.joinToString()}")
        return webDir
    }

    private fun checkUpdate() {
        WxPusherUtils.getIoScopeScope().launch {
            try {
//                //根据功能版本号去查询新的bundle，避免接口不兼容的问题，如果修改了js桥，就必须修改功能版本号
//                val mainVersion =
//                    WxPusherUtils.getVersionName().split(".").take(2).joinToString(".")
//                val serverVersion =
//                    URL("${WxPusherConfig.WebUrl}/${mainVersion}_${VERSION_FILE}").readText()
//                val localVersion = getNowVersion()
//                if (isVersionGreater(serverVersion, localVersion)) {
//                    WxPusherLog.i(TAG, "检查到新版本，开始下载,version=${serverVersion}")
//                    downloadNewBundle(serverVersion)
//                } else {
//                    WxPusherLog.i(TAG, "无新版本，跳过更新")
//                }
            } catch (e: Exception) {
                WxPusherLog.i(TAG, "检查更新失败")
            }
        }
    }

    private fun downloadNewBundle(serverVersion: String) {
        WxPusherUtils.getIoScopeScope().launch {
            try {
//                val url = URL("${WxPusherConfig.WebUrl}/web_bundle.zip")
//                val connection = url.openConnection()
//                // 清空临时目录
//                tempDir.deleteRecursively()
//                tempDir.mkdirs()
//
//                // 下载并解压到临时目录
//                val bundleZip = File(ApplicationUtils.getApplication().filesDir, BUNDLE_NAME)
//                if (bundleZip.exists()) {
//                    bundleZip.delete()
//                }
//                FileOutputStream(bundleZip).use { output ->
//                    connection.getInputStream().use { input ->
//                        input.copyTo(output)
//                    }
//                }
//                WxPusherLog.i(TAG, "新版本下载完成,version=${serverVersion}")
//                bundleZip.inputStream().use {
//                    extractZipTempDir(it)
//                }
//                bundleZip.delete()
//                WxPusherLog.i(TAG, "新版本解压完成,version=${serverVersion}")
            } catch (e: Exception) {
                WxPusherLog.w(TAG, "下载新bundle失败", e)
            }
        }
    }

    /**
     * 在加载的时候，再去复制文件，避免解压失败 ，无法进入
     */
    fun applyUpdateIfAvailable() {
        if (WxpSaveService.get(NEED_APPLY_UPDATE_KEY, "false") == "true") {
            try {
                WxPusherLog.i(TAG, "删除老版本")
                // 删除旧文件
                webDir.deleteRecursively()
                webDir.mkdirs()

                // 移动新文件
                tempDir.listFiles()?.forEach { file ->
                    file.copyRecursively(File(webDir, file.name), true)
                }

                // 清理
                tempDir.deleteRecursively()
                WxpSaveService.set(NEED_APPLY_UPDATE_KEY, "false")
                WxPusherLog.i(TAG, "应用新版本完成")
            } catch (e: Exception) {
                //更新失败，重置一下版本号，下次启动会再次更新
                WxPusherLog.w(TAG, "应用更新失败", e)
            }
        }
    }

    /**
     * 解压释放到临时目录，后面调用 applyUpdateIfAvailable 即可生效
     */
    private fun extractZipTempDir(input: InputStream) {
        WxPusherLog.i(TAG, "extractZipTempDir: 开始解压BundleZip")
        tempDir.deleteRecursively()
        tempDir.mkdirs()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
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
        WxpSaveService.set(NEED_APPLY_UPDATE_KEY, "true")
        WxPusherLog.i(TAG, "extractZipTempDir: 完成解压BundleZip")
    }
}